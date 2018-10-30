# Overview

`kafka-connect-datagen` is a Kafka Connect connector for generating mock data.
It is available in [Confluent Hub](https://www.confluent.io/connector/kafka-connect-datagen/).
It is not suitable for production.

# Confluent Platform running on local install

## Install connector from Confluent Hub

You may install the `kafka-connect-datagen` connector from [Confluent Hub](https://www.confluent.io/connector/kafka-connect-datagen/).

```bash
confluent-hub install --no-prompt confluentinc/kafka-connect-datagen:0.1.0
```

## Build connector from latest code

Alternatively, you may build and install the `kafka-connect-datagen` connector from latest code.

```bash
git checkout v0.1.0
mvn clean compile package
confluent-hub install --no-prompt target/components/packages/confluentinc-kafka-connect-datagen-0.1.0.zip
```

## Run connector in local install

Here is an example of how to run the `kafka-connect-datagen` on a local install:

```bash
confluent destroy
confluent start connect
sleep 15
confluent config datagen -d ./connector_datagen.config
#confluent config datagen -d ./connector_datagen.custom.config
sleep 5
confluent status connectors
confluent consume test1 --value-format avro --max-messages 5 --property print.key=true --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer --from-beginning
```

# Confluent Platform running in Docker

## Install connector from Confluent Hub

You may install the `kafka-connect-datagen` connector from [Confluent Hub](https://www.confluent.io/connector/kafka-connect-datagen/).

```bash
docker build . -f Dockerfile-confluenthub -t confluentinc/kafka-connect-datagen:0.1.0
```

## Build connector from latest code

Alternatively, you may build and install the `kafka-connect-datagen` connector from latest code.

```bash
git checkout v0.1.0
mvn clean compile package
docker build . -f Dockerfile-local -t confluentinc/kafka-connect-datagen:0.1.0
```

## Run connector in Docker Compose

Here is an example of how to run the `kafka-connect-datagen` with Docker Compose:

```bash
docker-compose down --remove-orphans
docker-compose up -d --build
sleep 30
./submit_datagen_config.sh
sleep 5
docker-compose exec connect kafka-console-consumer --topic test1 --bootstrap-server kafka:29092  --property print.key=true --max-messages 5 --from-beginning
```
