# ![Alpaca](https://cloud.githubusercontent.com/assets/2371345/15409648/16c140b4-1dec-11e6-81d9-41929bc83b1f.png) Alpaca
[![Build Status](https://github.com/islandora/Alpaca/actions/workflows/build-1.x.yml/badge.svg)](https://github.com/Islandora/Alpaca/actions)
[![Contribution Guidelines](http://img.shields.io/badge/CONTRIBUTING-Guidelines-blue.svg)](./CONTRIBUTING.md)
[![LICENSE](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)](./LICENSE)
[![codecov](https://codecov.io/gh/Islandora/Alpaca/branch/1.x/graphs/badge.svg)](https://codecov.io/gh/Islandora/Alpaca)

## Introduction

Event-driven middleware based on [Apache Camel](http://camel.apache.org/) that synchronizes a Fedora repository with a Drupal instance.

## Requirements

This project requires Java 11 and can be built with [Gradle](https://gradle.org).

To build and test locally, clone this repository and then change into the Alpaca directory.
Next run `./gradlew clean build shadowJar`.

The main executable jar is available in the `islandora-alpaca-app/build/libs` directory, with the classifier `-all`.

ie.
> islandora-alpaca-app/build/libs/islandora-alpaca-app-2.0.0-all.jar

## Configuration

Alpaca is made up of several services, each of these can be enabled or disabled individually.

Alpaca takes an external file to configure its behaviour.

Look at the [`example.properties`](example.properties) file to see some example settings.

The properties are:

```
# Common options
error.maxRedeliveries=4
```
This defines how many times to retry a message before failing completely.

There are also common ActiveMQ properties to setup the connection.

```
# ActiveMQ options
jms.brokerUrl=tcp://localhost:61616
```

This defines the url to the ActiveMQ broker.

```
jms.username=
jms.password=
```
This defines the login credentials (if required)

```
jms.connections=10
```
This defines the pool of connections to the ActiveMQ instance.

```
jms.concurrent-consumers=1
```
This defines how many messages to process simultaneously.

### islandora-indexing-fcrepo

This service manages a Drupal node into a corresponding Fedora resource.

It's properties are:

```
# Fcrepo indexer options
fcrepo.indexer.enabled=true
```

This defines whether the Fedora indexer is enabled or not.

```
fcrepo.indexer.node=queue:islandora-indexing-fcrepo-content
fcrepo.indexer.delete=queue:islandora-indexing-fcrepo-delete
fcrepo.indexer.media=queue:islandora-indexing-fcrepo-media
fcrepo.indexer.external=queue:islandora-indexing-fcrepo-file-external
```

These define the various queues to listen on for the indexing/deletion
messages. The part after `queue:` should match your Islandora instance "Actions".

```
fcrepo.indexer.milliner.baseUrl=http://localhost:8000/milliner
```
This defines the location of your Milliner microservice.

```
fcrepo.indexer.concurrent-consumers=1
fcrepo.indexer.max-concurrent-consumers=1
```
These define the default number of concurrent consumers and maximum number of concurrent
consumers working off your ActiveMQ instance.
A value of `-1` means no setting is applied.

```
fcrepo.indexer.async-consumer=true
```

This property allows the concurrent consumers to process concurrently; otherwise, the consumers will wait to the previous message has been processed before executing.

### islandora-indexing-triplestore

This service indexes the Drupal node into the configured triplestore

It's properties are:

```
# Triplestore indexer options
triplestore.indexer.enabled=false
```

This defines whether the Triplestore indexer is enabled or not.

```
triplestore.index.stream=queue:islandora-indexing-triplestore-index
triplestore.delete.stream=queue:islandora-indexing-triplestore-delete
```

These define the various queues to listen on for the indexing/deletion
messages. The part after `queue:` should match your Islandora instance "Actions".

```
triplestore.baseUrl=http://localhost:8080/bigdata/namespace/kb/sparql
```

This defines the location of your triplestore's SPARQL update endpoint.

```
triplestore.indexer.concurrent-consumers=1
triplestore.indexer.max-concurrent-consumers=1
```

These define the default number of concurrent consumers and maximum number of concurrent
consumers working off your ActiveMQ instance.
A value of `-1` means no setting is applied.


```
triplestore.indexer.async-consumer=true
```

This property allows the concurrent consumers to process concurrently; otherwise, the consumers will wait to the previous message has been processed before executing.

### islandora-connector-derivative

This service is used to configure an external microservice. This service will deploy multiple copies of its routes
with different configured inputs and outputs based on properties.

The routes to be configured are defined with the property `derivative.systems.installed` which expects
a comma separated list. Each item in the list defines a new route and must also define 3 additional properties.

```
derivative.<item>.enabled=true
```

This defines if the `item` service is enabled.

```
derivative.<item>.in.stream=queue:islandora-item-connector.index
```

This is the input queue for the derivative microservice.
The part after `queue:` should match your Islandora instance "Actions".

```
derivative.<item>.service.url=http://example.org/derivative/convert
```

This is the microservice URL to process the request.

```
derivative.<item>.concurrent-consumers=1
derivative.<item>.max-concurrent-consumers=1
```

These define the default number of concurrent consumers and maximum number of concurrent
consumers working off your ActiveMQ instance.
A value of `-1` means no setting is applied.


```
derivative.<item>.async-consumer=true
```

This property allows the concurrent consumers to process concurrently; otherwise, the consumers will wait to the previous message has been processed before executing.

For example, with two services defined (houdini and crayfits) my configuration would have

```
derivative.systems.installed=houdini,fits

derivative.houdini.enabled=true
derivative.houdini.in.stream=queue:islandora-connector-houdini
derivative.houdini.service.url=http://127.0.0.1:8000/houdini/convert
derivative.houdini.concurrent-consumers=1
derivative.houdini.max-concurrent-consumers=4
derivative.houdini.async-consumer=true

derivative.fits.enabled=true
derivative.fits.in.stream=queue:islandora-connector-fits
derivative.fits.service.url=http://127.0.0.1:8000/crayfits
derivative.fits.concurrent-consumers=2
derivative.fits.max-concurrent-consumers=2
derivative.fits.async-consumer=false
```

### Customizing HTTP client timeouts

You can alter the HTTP client from the defaults for its request, connection and socket timeouts.
To do this you want to enable the request configurer.

```shell
request.configurer.enabled=true
```

Then set the next 3 timeouts (measured in milliseconds) to the desired timeout.

```shell
request.timeout=-1
connection.timeout=-1
socket.timeout=-1
```

The default for all three is `-1` which indicates no timeout.

## Deploying/Running

You can see the options by passing the `-h|--help` flag

```shell
> java -jar  islandora-alpaca-app/build/libs/islandora-alpaca-app-2.0.0-all.jar -h
Usage: alpaca [-hV] [-c=<configurationFilePath>]
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
  -c, --config=<configurationFilePath>
                  The path to the configuration file
```

Using the `-V|--version` flag will just return the current version of the application.

```shell
> java -jar  islandora-alpaca-app/build/libs/islandora-alpaca-app-2.0.0-all.jar -v
2.0.0
```

To start Alpaca you would pass the external property file with the `-c|--config` flag.

For example if you are using an external properties file located at `/opt/my.properties`,
you would run:

```shell
java -jar islandora-alpaca-app-2.0.0-all.jar -c /opt/my.properties
```

## Debugging/Troubleshooting

Logging is done to the console, and defaults to the INFO level. To get more verbose logging you
can use the Java property `islandora.alpaca.log`

i.e.

```shell
java -Dislandora.alpaca.log=DEBUG -jar islandora-alpaca-app-2.0.0-all.jar -c /opt/my.properties
```

## Documentation

Further documentation for this module is available on the [Islandora documentation site](https://islandora.github.io/documentation/).

## Troubleshooting/Issues

Having problems or solved a problem? Check out the Islandora google groups for a solution.

* [Islandora Group](https://groups.google.com/forum/?hl=en&fromgroups#!forum/islandora) * [Islandora Dev Group](https://groups.google.com/forum/?hl=en&fromgroups#!forum/islandora-dev)

## Current Maintainers

* [Jared Whiklo](https://github.com/whikloj)

## Sponsors

* Discoverygarden
* LYRASIS
* York University
* McMaster University
* University of Prince Edward Island
* University of Manitoba
* University of Limerick
* Simon Fraser University
* PALS

## Development

If you would like to contribute, please get involved by attending our weekly [Tech Call](https://github.com/Islandora/documentation/wiki). We love to hear from you!

If you would like to contribute code to the project, you need to be covered by an Islandora Foundation [Contributor License Agreement](http://islandora.ca/sites/default/files/islandora_cla.pdf) or [Corporate Contributor License Agreement](http://islandora.ca/sites/default/files/islandora_ccla.pdf). Please see the [Contributors](http://islandora.ca/resources/contributors) pages on Islandora.ca for more information.

We recommend using the [islandora-playbook](https://github.com/Islandora-Devops/islandora-playbook) to get started.

## Licensing
[MIT](/License)
