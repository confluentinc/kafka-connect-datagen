FROM confluentinc/cp-kafka-connect-base:5.0.0

ENV CONNECT_PLUGIN_PATH="/usr/share/java,/usr/share/confluent-hub-components"

COPY target/components/packages/confluentinc-kafka-connect-datagen-5.0.0.zip /tmp/confluentinc-kafka-connect-datagen-5.0.0.zip 

RUN  confluent-hub install --no-prompt /tmp/confluentinc-kafka-connect-datagen-5.0.0.zip
