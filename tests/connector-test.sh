#!/bin/bash
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"

confluent local destroy || true
mvn -f ${DIR}/../pom.xml clean package || exit 1
rm -fr $CONFLUENT_HOME/share/confluent-hub-components/confluentinc-kafka-connect-datagen
confluent-hub install --no-prompt ${DIR}/../target/components/packages/confluentinc-kafka-connect-datagen-*.zip
confluent local services connect start
sleep 10

confluent local services connect status

connectors="credit_cards stores transactions purchases inventory product campaign_finance"

for connector in $connectors; do
    confluent local services connect connector config datagen-$connector --config ${DIR}/../config/connector_${connector}.config
done

confluent local services connect connector status
echo

for connector in $connectors; do
    echo
    echo $connector:
    confluent local services kafka consume $connector --max-messages 10 --property print.key=true --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer --from-beginning
done

confluent local destroy
