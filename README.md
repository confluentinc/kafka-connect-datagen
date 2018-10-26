# kafka-connect-datagen

```bash
confluent destroy
mvn clean compile package
mkdir $CONFLUENT_HOME/share/java/kafka-connect-datagen
yes | cp -f target/kafka-connect-datagen-5.0.0.jar $CONFLUENT_HOME/share/java/kafka-connect-datagen/.
confluent start connect
confluent config datagen -d ./connector_datagen.config

```
