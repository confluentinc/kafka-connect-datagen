# kafka-connect-datagen

`kafka-connect-datagen` is a Kafka Connect connector for generating mock data, not suitable for production

# Confluent Platform running on local install

## Install connector from Confluent Hub

```bash
confluent-hub install --no-prompt confluentinc/kafka-connect-datagen:0.1.0
```

## Build connector from latest code

```bash
git checkout v0.1.0
mvn clean compile package
confluent-hub install --no-prompt target/components/packages/confluentinc-kafka-connect-datagen-0.1.0.zip
```

## Run connector in local install

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

```bash
docker build Dockerfile-confluenthub -t confluentinc/kafka-connect-datagen:0.1.0
```

## Build connector from latest code

```bash
git checkout v0.1.0
mvn clean compile package
docker build Dockerfile-local -t confluentinc/kafka-connect-datagen:0.1.0
```

## Run connector in Docker Compose

```bash
docker-compose down --remove-orphans
docker-compose up -d --build
sleep 30
./submit_datagen_config.sh
sleep 5
docker-compose exec connect kafka-console-consumer --topic test1 --bootstrap-server kafka:29092  --property print.key=true --max-messages 5 --from-beginning
```
