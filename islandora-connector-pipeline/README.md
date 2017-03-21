# Islandora Connector Pipeline

## Description

This service takes an incoming message and routes through the queues specified in the 'IslandoraPipelineRecipients' header as a comma separated list sequentially.  

## Configuration

Once deployed, the incoming message stream can be configured by editing the `ca.islandora.alpaca.connector.pipeline.cfg` file in your Karaf installation's `etc` directory.

    input.stream=broker:queue:islandora-connector-pipeline

For example, by changing `broker` to `activemq`, this service will attempt read messages from the ActiveMQ queue `islandora-connector-pipeline`.  In theory, any broker technology supported by Camel should be supported, though ActiveMQ is the only one tested.

## More Information

For more information, please visit the [Apache Camel](http://camel.apache.org) documentation.
