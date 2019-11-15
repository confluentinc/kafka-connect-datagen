# Table of Contents

- [Overview](#overview)
- [Versions](#versions)
- [Usage](#usage)
- [Configuration](#configuration)
- [Confusion about schemas and Avro](#confusion-about-schemas-and-avro)
- [Publishing](#publishing-docker-images)

# Overview

`kafka-connect-datagen` is a [Kafka Connect](https://docs.confluent.io/current/connect/index.html) connector for generating mock data for testing and is not suitable for production scenarios.  It is available in [Confluent Hub](https://www.confluent.io/connector/kafka-connect-datagen/).

# Versions

There are multiple [released versions](https://github.com/confluentinc/kafka-connect-datagen/releases) of this connector, starting with `0.1.0`.
The instructions below use version `0.1.6` as an example, but you can substitute any of the other released versions.
In fact, unless specified otherwise, we recommend using the latest released version to get all of the features and bug fixes.

# Usage

You can choose to install a released version of the `kafka-connect-datagen` from [Confluent Hub](https://www.confluent.io/connector/kafka-connect-datagen/) or build it from source.  For running the connector you can choose a local [Confluent Platform Installation](https://docs.confluent.io/current/quickstart/index.html) or in a Docker container.

## Install the connector from Confluent Hub to a local Confluent Platform

Using the [Confluent Hub Client](https://docs.confluent.io/current/connect/managing/confluent-hub/client.html) you may install the `kafka-connect-datagen` connector from [Confluent Hub](https://www.confluent.io/connector/kafka-connect-datagen/).

To install a specific release version you can run: 

```bash
confluent-hub install confluentinc/kafka-connect-datagen:0.1.6
```

or to install the latest released version:

```bash
confluent-hub install confluentinc/kafka-connect-datagen:latest
```

Here is an example of how to run the `kafka-connect-datagen` on a local Confluent Platform after it's been installed.  [Configuration](#configuration) details are provided below.

```bash
confluent local start connect
confluent local config datagen-pageviews -- -d config/connector_pageviews.config
confluent local status connectors
confluent local consume test1 --value-format avro --max-messages 5 --property print.key=true --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer --from-beginning
```

## Install the connector from Confluent Hub into a [Kafka Connect](https://docs.confluent.io/current/connect/index.html) based Docker image

A Docker image based on Kafka Connect with the `kafka-connect-datagen` plugin is already available in [Dockerhub](https://hub.docker.com/r/cnfldemos/kafka-connect-datagen), and it is ready for you to use.

If you want to build a local copy of the Docker image with `kafka-connect-datagen`, this project provides a [Dockerfile](Dockerfile) that you can reference.

You can create a Docker image packaged with the locally built source by running:
```bash
make build-docker-from-local
```

This will build the connector from source and create a local image with an aggregate version number.  The aggregate version number is the kafka-connect-datagen connector version number and the Confluent Platform version number separated with a `-`.   The local kafka-connect-datagen version number is defined in the `pom.xml` file, and the Confluent Platform version defined in the [Makefile](Makfile).  An example of the aggregate version number might be: `0.1.6-5.3.1`.

Alternatively, you can install the `kafka-connect-datagen` connector from [Confluent Hub](https://www.confluent.io/connector/kafka-connect-datagen/) into a Docker image by running:
```bash
make build-docker-from-released
```

The [Makefile](Makefile) contains some default variables that affect the version numbers of both the installed `kafka-connect-datagen` as well as the base Confluent Platform version.  The variables are located near the top of the [Makefile](Makefile) with the following names and current default values:

```bash
CP_VERSION ?= 5.3.1
KAFKA_CONNECT_DATAGEN_VERSION ?= 0.1.6
```
These values can be overriden with variable declarations before the `make` command.  For example:
```bash
KAFKA_CONNECT_DATAGEN_VERSION=0.1.4 make build-docker-from-released
```

### Run connector in Docker Compose

Here is an example of how to run the `kafka-connect-datagen` with the provided [docker-compose.yml](docker-compose.yml) file.  If you wish to use a different Docker image tag, be sure to modify appropriately in the [docker-compose.yml](docker-compose.yml)` file.

```bash
docker-compose up -d --build
curl -X POST -H "Content-Type: application/json" --data @config/connector_pageviews.config http://localhost:8083/connectors
docker-compose exec connect kafka-console-consumer --topic pageviews --bootstrap-server kafka:29092  --property print.key=true --max-messages 5 --from-beginning
```
## Building

To build the `kafka-connect-datagen` connector from latest code and install it to a local Confluent Platform, you can perform the following (here we use `v0.1.6` to reference the git tag for the `0.1.6` version, but the same pattern works for all released versions).

```bash
git checkout v0.1.6
make package
confluent-hub install target/components/packages/confluentinc-kafka-connect-datagen-0.1.6.zip
```

# Configuration

## Generic Kafka Connect Parameters

See all Kafka Connect [configuration parameters](https://docs.confluent.io/current/connect/managing/configuring.html).

## Connector-specific Parameters

See `kafka-connect-datagen` [configuration parameters](https://github.com/confluentinc/kafka-connect-datagen/blob/master/src/main/java/io/confluent/kafka/connect/datagen/DatagenConnectorConfig.java) and their defaults.

## Use a bundled schema specifications

There are a few quickstart schema specifications bundled with `kafka-connect-datagen`, and they are listed in this [directory](https://github.com/confluentinc/kafka-connect-datagen/tree/master/src/main/resources).
To use one of these bundled schema, refer to [this mapping](https://github.com/confluentinc/kafka-connect-datagen/blob/master/src/main/java/io/confluent/kafka/connect/datagen/DatagenTask.java#L66-L73) and in the configuration file, set the parameter `quickstart` to the associated name.
For example:

```bash
...
"quickstart": "users",
...
```

## Define a new schema specification

You can also define your own schema specifications if you want to customize the fields and their values to be more domain specific or to match what your application is expecting.
Under the hood, `kafka-connect-datagen` uses [Avro Random Generator](https://github.com/confluentinc/avro-random-generator), so the only constraint in writing your own schema specification is that it is compatible with Avro Random Generator.
To define your own schema:

1. Create your own schema file `/path/to/your_schema.avsc` that is compatible with [Avro Random Generator](https://github.com/confluentinc/avro-random-generator)
2. In the connector configuration, remove the configuration parameter `quickstart` and add the parameters `schema.filename` (which should be the absolute path) and `schema.keyfield`:

```bash
...
"schema.filename": "/path/to/your_schema.avsc",
"schema.keyfield": "<field representing the key>",
...
```

# Confusion about schemas and Avro

To define the set of "rules" for the mock data, `kafka-connect-datagen` uses [Avro Random Generator](https://github.com/confluentinc/avro-random-generator).
The configuration parameters `quickstart` or `schema.filename` specify the Avro schema, or the set of "rules", which declares a list of primitives or more complex data types, length of data, and other properties about the generated mock data.
Examples of these schema files are listed in this [directory](https://github.com/confluentinc/kafka-connect-datagen/tree/master/src/main/resources).

Do not confuse the above terminology with `Avro` and `schemas` used in a different context as described below.
The Avro schemas for generating mock data are independent of (1) the format of the data produced to Kafka and (2) the schema in Confluent Schema Registry.

1. The format of data produced to Kafka may or may not be Avro.
To define the format of the data produced to Kafka, you must set the format type in your connector configuration.
The connector configuration parameters can be defined for the key or value.
For example, to produce messages to Kafka where the message value format is Avro, set the `value.converter` and `value.converter.schema.registry.url` parameters:

```bash
...
"value.converter": "io.confluent.connect.avro.AvroConverter",
"value.converter.schema.registry.url": "http://localhost:8081",
...
```

Or to produce messages to Kafka where the message value format is JSON, set the `value.converter` parameter:

```bash
...
"value.converter": "org.apache.kafka.connect.json.JsonConverter",
...
```

2. The schema in Confluent Schema Registry declares the record fields and their types, and is used by Kafka clients when they are configured to produce or consume Avro data.
As an example, consider the following "rule" in the schema specification to generate a field `userid`:

```bash
...
{"name": "userid", "type": {
    "type": "string",
    "arg.properties": {
        "regex": "User_[1-9]{0,1}"
    }
}},
...
```

If you are using Avro format for producing data to Kafka, here is the corresponding field in the registered schema in Confluent Schema Registry:

```bash
{"name": "userid", "type": ["null", "string"], "default": null},
```

If you are not using Avro format for producing data to Kafka, there will be no schema in Confluent Schema Registry.

# Publishing Docker Images

*Note: The following instructions are only relevant if you are an administrator of this repository and have push access to the https://hub.docker.com/r/cnfldemos/kafka-connect-datagen/ repository.  The local Docker daemon must be logged into a proper Docker Hub account.*

To release new versions of the Docker images to Dockerhub (https://hub.docker.com/r/cnfldemos/kafka-connect-datagen/ & https://hub.docker.com/r/cnfldemos/cp-server-connect-operator-with-datagen) use the respective targets in the [Makefile](Makefile).

The [Makefile](Makefile) contains some default variables that affect the version numbers of both the installed `kafka-connect-datagen` as well as the base Confluent Platform version.  The variables are located near the top of the [Makefile](Makefile) with the following names and current default values:

```bash
CP_VERSION ?= 5.3.1
KAFKA_CONNECT_DATAGEN_VERSION ?= 0.1.6
OPERATOR_VERSION ?= 0 # Operator is a 'rev' version appended at the end of the CP version, like so: 5.3.1.0
```

To publish the https://hub.docker.com/r/cnfldemos/kafka-connect-datagen/ image:
```bash
make publish-cp-kafka-connect-confluenthub
```

and to override the CP Version of the `kafka-connect-datagen` version you can run something similar to:
```bash
CP_VERSION=5.3.0 KAFKA_CONNECT_DATAGEN_VERSION=0.1.4 make publish-cp-kafka-connect-confluenthub
```

to override the CP Version and the Operator version, which may happen if Operator releases a patch version, you could run something similar to:
```bash
CP_VERSION=5.3.0 OPERATOR_VERSION=1 KAFKA_CONNECT_DATAGEN_VERSION=0.1.4 make publish-cp-server-connect-operator-confluenthub
```
which would result in a docker image tagged as: `cp-server-connect-operator-with-datagen:0.1.4-5.3.0.1`

To publish the https://hub.docker.com/r/cnfldemos/cp-server-connect-operator-with-datagen image:
```bash
make publish-cp-server-connect-operator-confluenthub
```
