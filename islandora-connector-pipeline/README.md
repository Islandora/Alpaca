# Islandora Connector Broadcast

## Description

This service takes an incoming message and broadcasts it to a list of queues/topics.  The list of recipients is specified in the `IslandoraBroadcastRecipients` header as a comma separated list.

## Configuration

Once deployed, the incoming message stream can be configured by editing the `ca.islandora.alpaca.connector.broadcast.cfg` file in your Karaf installation's `etc` directory.

    input.stream=broker:queue:islandora-connector-broadcast

For example, by changing `broker` to `activemq`, this service will attempt read messages from the ActiveMQ queue `islandora-connector-broadcast`.  In theory, any broker technology supported by Camel should be supported, though ActiveMQ is the only one tested.

## More Information

For more information, please visit the [Apache Camel](http://camel.apache.org) documentation.
