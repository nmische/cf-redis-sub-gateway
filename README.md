#ColdFusion Redis Sub Gateway#

This is a ColdFusion event gateway that allows you to subscribe to a [Redis](http://redis.io/) pub/sub channel. This implementation is based on the [Jedis](https://github.com/xetorthio/jedis) Redis client.

## Installing the Gateway ##

* Downlaod the `cf-redis-sub-gateway-X.X.zip` package from [Github](http://github.com/nmische/cf-redis-sub-gateway/downloads).
* Extract the archive and copy the `cf-redis-sub-gateway-X.X.jar`, and the `jedis-2.1.0.jar` to the ColdFusion classpath.
    * The best place to put these jars is in the `{cfusion.root}/gateway/lib` folder. For example `C:\ColdFusion10\cfusion\gateway\lib` or `/Applications/ColdFusion10/cfusion/gateway`
* Restart ColdFusion, if necessary.
* Log in to the ColdFusion administrator and navigate to the Event Gateways > Gateway Types page.
* Under Add / Edit ColdFusion Event Gateway Types enter the following:
    * Type Name: RedisSub
    * Description: Handles messages from a Redis pub/sub channel 
    * Java Class: com.newschuyl.redisgateway.RedisSubGateway
    * Startup Timeout: 30 seconds
    * Stop on Startup Timeout: checked

## Configuring the Gateway ##

* Log in to the ColdFusion administrator and navigate to the Event Gateways > Gateway Instances page.
* Under Add / Edit ColdFusion Event Gateway Instances enter the following:
    * Gateway ID: An name for your gateway instance.
    * Gateway Type: RedisSub - Handles messages from a Redis Pub/Sub channel 
    * CFC Path: Path to the CFC listner. For more about the listener CFC see Handling Incoming Messages.
    * Configuration File: The path to the (optional) configuration file. If not provided the gateway will subscribe to the `cfredis` channel on a local Redis server (127.0.0.1:6379). Below is a sample configuration file.

```
# host - The redis host, default is localhost
host=127.0.0.1
# port - The redis port, default is 6379
port=6379
# auth - The redis server password, if the server has requirepass set
auth=foobared
# channel - The channel to subscribe to, the default is cfredis
channel=foo.bar
```

## Handling Incoming Messages ##

### Events ###

The CFC listener must implement the following method:

* onIncomingMessage(event) - fires when a message is received on the pub/sub channel. The event has the following fields:
    * CfcMethod: Listener CFC method, onIncomingMessage in this case
    * Data.CHANNEL: The name of the Redis pub/sub channel
    * Data.MESSAGE: Message contents
    * GatewayType: Always "RedisSub"
    * OriginatorID: Always "RedisSubGateway"
