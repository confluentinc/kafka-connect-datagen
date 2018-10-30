# kafka-connect-datagen

`kafka-connect-datagen` is a Kafka Connect connector for generating mock data, not suitable for production

## Local

```bash
git checkout v0.1.0
confluent destroy
mvn clean compile package
confluent-hub install --no-prompt target/components/packages/confluentinc-kafka-connect-datagen-0.1.0.zip
confluent start connect
sleep 15
confluent config datagen -d ./connector_datagen.config
#confluent config datagen -d ./connector_datagen.custom.config
sleep 5
confluent status connectors
confluent consume test1 --value-format avro --max-messages 5 --property print.key=true --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer --from-beginning

```


## Docker

```bash
git checkout v0.1.0
docker-compose down --remove-orphans
mvn clean compile package
docker-compose up -d --build
sleep 30
./submit_datagen_config.sh
sleep 5
docker-compose exec connect kafka-console-consumer --topic test1 --bootstrap-server kafka:29092  --property print.key=true --max-messages 5 --from-beginning
```
