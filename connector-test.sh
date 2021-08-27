#!/bin/bash

confluent local destroy
mvn clean package || exit 1
rm -fr $CONFLUENT_HOME/share/confluent-hub-components/confluentinc-kafka-connect-datagen
confluent-hub install --no-prompt target/components/packages/confluentinc-kafka-connect-datagen-0.5.1-SNAPSHOT.zip
confluent local services connect start
sleep 10

confluent local services connect status
confluent local services connect connector config datagen-credit-cards --config config/connector_credit_cards.config
confluent local services connect connector config datagen-stores --config config/connector_stores.config
confluent local services connect connector config datagen-transactions --config config/connector_transactions.config
confluent local services connect connector config datagen-purchases --config config/connector_purchases.config
confluent local services connect connector config datagen-products --config config/connector_product.config
confluent local services connect connector status

echo
echo "credit-cards:"
confluent local services kafka consume credit-cards --max-messages 5 --property print.key=true --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer --from-beginning
echo
echo "stores:"
confluent local services kafka consume stores --max-messages 5 --property print.key=true --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer --from-beginning
echo
echo "transactions:"
confluent local services kafka consume transactions --max-messages 5 --property print.key=true --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer --from-beginning
echo
echo "purchases:"
confluent local services kafka consume purchases --max-messages 5 --property print.key=true --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer --from-beginning
echo
echo "products:"
confluent local services kafka consume products --max-messages 5 --property print.key=true --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer --from-beginning
confluent local destroy
