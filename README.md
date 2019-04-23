# Table of Contents

- [Overview](#overview)
- [Versions](#versions)
- [Install and Run](#install-and-run)
- [Configuration](#configuration)
- [Confusion about schemas and Avro](#confusion-about-schemas-and-avro)


# Overview

`kafka-connect-datagen` is a Kafka Connect connector for generating mock data.
It is available in [Confluent Hub](https://www.confluent.io/connector/kafka-connect-datagen/).
It is not suitable for production.

# Versions

There are multiple [released versions](https://github.com/confluentinc/kafka-connect-datagen/releases) of this connector, starting with `0.1.0`.
The instructions below use version `0.1.1` as an example, but you can substitute any of the other released versions.
In fact, unless specified otherwise, we recommend using the latest released version to get all of the features and bug fixes.

# Install and Run

## Confluent Platform running on local install

### Install connector from Confluent Hub

You may install the `kafka-connect-datagen` connector from [Confluent Hub](https://www.confluent.io/connector/kafka-connect-datagen/).

```bash
confluent-hub install confluentinc/kafka-connect-datagen:0.1.1
```

for a the `0.1.1` version of the connector (you can use any released version), or

```bash
confluent-hub install confluentinc/kafka-connect-datagen:latest
```

for the latest released version of the connector.


### Build connector from latest code

Alternatively, you may build and install the `kafka-connect-datagen` connector from latest code.
Here we use `v0.1.1` to reference the git tag for the `0.1.1` version, but the same pattern works for all released versions.

```bash
git checkout v0.1.1
mvn clean package
confluent-hub install target/components/packages/confluentinc-kafka-connect-datagen-0.1.1.zip
```

### Run connector in local install

Here is an example of how to run the `kafka-connect-datagen` on a local install:

```bash
confluent start connect
confluent config datagen-pageviews -d config/connector_pageviews.config
confluent status connectors
confluent consume test1 --value-format avro --max-messages 5 --property print.key=true --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer --from-beginning
```

## Confluent Platform running in Docker

This project provides several Dockerfiles that you can use to create Docker images with this connector.
The Dockerfiles differ slightly with each release, so be sure the connector version in the Dockerfile matches the version you want to use.

### Install connector from Confluent Hub

You may install into your Docker image the `kafka-connect-datagen` connector from [Confluent Hub](https://www.confluent.io/connector/kafka-connect-datagen/).
The following command builds the image using the `Dockerfile-confluenthub` specification and tags that image with `confluentinc/kafka-connect-datagen:0.1.1` (be sure to use the correct datagen connector version in the label).

```bash
docker build . -f Dockerfile-confluenthub -t confluentinc/kafka-connect-datagen:0.1.1
```

### Build connector from latest code

Alternatively, you may build and install the `kafka-connect-datagen` connector from latest code.
Here we use `v0.1.1` to reference the git tag for the `0.1.1` version, but the same pattern works for all released versions.
Be sure to use the same version in the Docker image tag (e.g., `confluentinc/kafka-connect-datagen:0.1.1`) that you checked out (e.g., `v0.1.1`).

```bash
git checkout v0.1.1
mvn clean package
docker build . -f Dockerfile-local -t confluentinc/kafka-connect-datagen:0.1.1
```

### Run connector in Docker Compose

Here is an example of how to run the `kafka-connect-datagen` with Docker Compose.
If you used a different Docker image tag, be sure to use that here instead of `confluentinc/kafka-connect-datagen:0.1.1`.

```bash
docker-compose up -d --build
curl -X POST -H "Content-Type: application/json" --data @config/connector_pageviews.config http://localhost:8083/connectors
docker-compose exec connect kafka-console-consumer --topic pageviews --bootstrap-server kafka:29092  --property print.key=true --max-messages 5 --from-beginning
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
