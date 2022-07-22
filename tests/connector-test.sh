#!/bin/bash
set -e

confluent local destroy || true
mvn -f ../pom.xml clean package || exit 1
rm -fr $CONFLUENT_HOME/share/confluent-hub-components/confluentinc-kafka-connect-datagen
confluent-hub install --no-prompt ../target/components/packages/confluentinc-kafka-connect-datagen-*.zip
confluent local services connect start
sleep 10

confluent local services connect status

connectors="credit_cards stores transactions purchases inventory product campaign_finance"
connectors="campaign_finance"

for connector in $connectors; do
    confluent local services connect connector config datagen-$connector --config ../config/connector_${connector}.config
done

confluent local services connect connector status
echo

for connector in $connectors; do
    echo
    echo $connector:
    confluent local services kafka consume $connector --max-messages 10 --property print.key=true --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer --from-beginning
done

confluent local destroy
