package com.newschuyl.redisgateway;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;

import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Jedis;

import coldfusion.eventgateway.CFEvent;
import coldfusion.eventgateway.Gateway;
import coldfusion.eventgateway.GatewayHelper;
import coldfusion.eventgateway.GatewayServices;
import coldfusion.eventgateway.Logger;

public class RedisSubGateway extends JedisPubSub implements Gateway {

	// The handle to the CF gateway service
	protected GatewayServices gatewayService;

	// ID provided by EventService
	protected String gatewayID;

	// Listener CFC paths for our events
	protected String[] listeners;

	// Path to my configuration file
	protected String config;

	// The thread that is running the client
	protected Thread clientThread;

	// The current status
	protected int status = STOPPED;

	// The jedis client
	protected Jedis jedis;

	// Redis defaults
	public static final String DEFAULT_HOST = "127.0.0.1";
	public static final int DEFAULT_PORT = 6379;
	public static final String DEFAULT_CHANNEL = "cfredis";

	// Redis settings
	protected String host = DEFAULT_HOST;
	protected int port = DEFAULT_PORT;
	protected String channel = DEFAULT_CHANNEL;
	protected String auth;

	// The gateway logger
	private Logger log;

	public RedisSubGateway(String gatewayID, String config) {
		this.gatewayID = gatewayID;
		this.config = config;
		this.gatewayService = GatewayServices.getGatewayServices();

		// log things to redis-gateway.log in the CF log directory
		log = gatewayService.getLogger("redis-gateway");

		try {
			FileInputStream pFile = new FileInputStream(config);
			Properties p = new Properties();
			p.load(pFile);
			pFile.close();
			if (p.containsKey("host"))
				host = p.getProperty("host");
			if (p.containsKey("port"))
				port = Integer.parseInt(p.getProperty("port"));
			if (p.containsKey("auth"))
				auth = p.getProperty("auth");
			if (p.containsKey("channel"))
				channel = p.getProperty("channel");
		} catch (IOException e) {
			// do nothing. use default value for port.
			log.warn("RedisSubGateway(" + gatewayID
					+ ") Unable to read configuration file '" + config + "': "
					+ e.toString(), e);
		}

		log.info("RedisSubGateway(" + gatewayID + ") configured for " + host
				+ ":" + port + "::" + channel + ".");

	}

	@Override
	public String getGatewayID() {
		return gatewayID;
	}

	@Override
	public GatewayHelper getHelper() {
		return null;
	}

	@Override
	public int getStatus() {
		return status;
	}

	@Override
	public String outgoingMessage(CFEvent event) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void restart() {
		stop();
		start();
	}

	@Override
	public void setCFCListeners(String[] listeners) {
		this.listeners = listeners;
	}

	@Override
	public void setGatewayID(String id) {
		gatewayID = id;
	}

	@Override
	public void start() {

		status = STARTING;

		clientThread = new Thread(new Runnable() {
			public void run() {
				startJedis();
			}
		});
		clientThread.start();

		status = RUNNING;

	}

	protected void startJedis() {

		jedis = new Jedis(host, port, 500);
		jedis.connect();
		if (auth != null)
			jedis.auth("foobared");
		jedis.configSet("timeout", "300");
		jedis.flushAll();
		jedis.subscribe(this, channel);

	}

	@Override
	public void stop() {
		status = STOPPING;
		this.unsubscribe();
		jedis.disconnect();
		status = STOPPED;
	}

	@Override
	public void onMessage(String channel, String message) {

		log.info("RedisSubGateway(" + gatewayID
				+ ") Message received. Message was: '"
				+ message.substring(0, Math.min(20, message.length())) + "'");

		for (String path : listeners) {

			CFEvent event = new CFEvent(gatewayID);

			Hashtable<String, Object> mydata = new Hashtable<String, Object>();
			mydata.put("CHANNEL", channel);
			mydata.put("MESSAGE", message);

			event.setData(mydata);
			event.setGatewayType("RedisSub");
			event.setOriginatorID("RedisSubGateway");
			event.setCfcMethod("onIncomingMessage");
			event.setCfcTimeOut(10);
			if (path != null) {
				event.setCfcPath(path);
			}

			boolean sent = gatewayService.addEvent(event);
			if (!sent) {
				log.error("RedisSubGateway("
						+ gatewayID
						+ ") Unable to put message on event queue. Message not sent from "
						+ gatewayID + ".  Message was: '"
						+ message.substring(0, Math.min(20, message.length()))
						+ "'");
			}
		}
	}

	@Override
	public void onPMessage(String pattern, String channel, String message) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPSubscribe(String pattern, int subscribedChannels) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPUnsubscribe(String pattern, int subscribedChannels) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSubscribe(String channel, int subscribedChannels) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUnsubscribe(String channel, int subscribedChannels) {
		// TODO Auto-generated method stub

	}

}
