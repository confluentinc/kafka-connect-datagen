FROM confluentinc/cp-kafka-connect-base:5.0.0

COPY target/components/packages/confluentinc-kafka-connect-datagen-5.0.0.zip /tmp/confluentinc-kafka-connect-datagen-5.0.0.zip 

RUN  confluent-hub install --no-prompt /tmp/confluentinc-kafka-connect-datagen-5.0.0.zip
