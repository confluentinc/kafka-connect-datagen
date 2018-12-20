# Overview

`kafka-connect-datagen` is a Kafka Connect connector for generating mock data.
It is available in [Confluent Hub](https://www.confluent.io/connector/kafka-connect-datagen/).
It is not suitable for production.

# Versions

There are multiple [released versions](https://github.com/confluentinc/kafka-connect-datagen/releases) of this connector, starting with `0.1.0`.
The instructions below use version `0.1.1` as an example, but you can substitute any of the other released versions.
In fact, unless specified otherwise, we recommend using the latest released version to get all of the features and bug fixes.

# Confluent Platform running on local install

## Install connector from Confluent Hub

You may install the `kafka-connect-datagen` connector from [Confluent Hub](https://www.confluent.io/connector/kafka-connect-datagen/).

```bash
confluent-hub install confluentinc/kafka-connect-datagen:0.1.1
```

for a the `0.1.1` version of the connector (you can use any released version), or

```bash
confluent-hub install confluentinc/kafka-connect-datagen:latest
```

for the latest released version of the connector.


## Build connector from latest code

Alternatively, you may build and install the `kafka-connect-datagen` connector from latest code.
Here we use `v0.1.1` to reference the git tag for the `0.1.1` version, but the same pattern works for all released versions.

```bash
git checkout v0.1.1
mvn clean package
confluent-hub install target/components/packages/confluentinc-kafka-connect-datagen-0.1.1.zip
```

## Run connector in local install

Here is an example of how to run the `kafka-connect-datagen` on a local install:

```bash
confluent start connect
confluent config datagen-pageviews -d config/connector_pageviews.config
confluent status connectors
confluent consume test1 --value-format avro --max-messages 5 --property print.key=true --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer --from-beginning
```

# Confluent Platform running in Docker

This project provides several Dockerfiles that you can use to create Docker images with this connector.
The Dockerfiles differ slightly with each release, so be sure the connector version in the Dockerfile matches the version you want to use.

## Install connector from Confluent Hub

You may install into your Docker image the `kafka-connect-datagen` connector from [Confluent Hub](https://www.confluent.io/connector/kafka-connect-datagen/).
The following command builds the image using the `Dockerfile-confluenthub` specification and tags that image with `confluentinc/kafka-connect-datagen:0.1.1` (be sure to use the correct datagen connector version in the label).

```bash
docker build . -f Dockerfile-confluenthub -t confluentinc/kafka-connect-datagen:0.1.1
```

## Build connector from latest code

Alternatively, you may build and install the `kafka-connect-datagen` connector from latest code.
Here we use `v0.1.1` to reference the git tag for the `0.1.1` version, but the same pattern works for all released versions.
Be sure to use the same version in the Docker image tag (e.g., `confluentinc/kafka-connect-datagen:0.1.1`) that you checked out (e.g., `v0.1.1`).

```bash
git checkout v0.1.1
mvn clean package
docker build . -f Dockerfile-local -t confluentinc/kafka-connect-datagen:0.1.1
```

## Run connector in Docker Compose

Here is an example of how to run the `kafka-connect-datagen` with Docker Compose.
If you used a different Docker image tag, be sure to use that here instead of `confluentinc/kafka-connect-datagen:0.1.1`.

```bash
docker-compose up -d --build
curl -X POST -H "Content-Type: application/json" --data @config/connector_pageviews.config http://localhost:8083/connectors
docker-compose exec connect kafka-console-consumer --topic pageviews --bootstrap-server kafka:29092  --property print.key=true --max-messages 5 --from-beginning
```

# Configuration

See `kafka-connect-datagen` [configuration parameters](https://github.com/confluentinc/kafka-connect-datagen/blob/master/src/main/java/io/confluent/kafka/connect/datagen/DatagenConnectorConfig.java) and their defaults.

# Schemas for Random Data

Pre-defined schemas are listed in this [directory](https://github.com/confluentinc/kafka-connect-datagen/tree/master/src/main/resources).
To use a pre-defined schema, refer to [this mapping](https://github.com/confluentinc/kafka-connect-datagen/blob/master/src/main/java/io/confluent/kafka/connect/datagen/DatagenTask.java#L66-L73) and set the parameter `quickstart` to the associated name.

This connector uses [Avro Random Generator](https://github.com/confluentinc/avro-random-generator), so you may also define your own schema accordingly.
Use the pre-defined schemas as reference examples.
To define your own schema, create the schema file and then set the configuration parameters `schema.filename` and `schema.keyfield`.
