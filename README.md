# kafka-connect-datagen

```bash
confluent destroy
mvn clean compile package
yes | cp -f target/kafka-connect-datagen-5.0.0.jar $CONFLUENT_HOME/share/java/.
confluent start
confluent config datagen -d ./connector_datagen.config
```
