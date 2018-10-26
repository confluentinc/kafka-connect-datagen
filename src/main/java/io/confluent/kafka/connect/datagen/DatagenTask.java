/**
 * Copyright © 2018 Yeva Byzek (yeva@confluent.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.confluent.kafka.connect.datagen;


import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.config.types.Password;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import org.apache.avro.Schema;

import io.confluent.avro.random.generator.Generator;
import io.confluent.connect.avro.AvroData;
import org.apache.avro.generic.GenericData.Record;
import org.apache.avro.generic.GenericRecord;
import io.confluent.kafka.connect.datagen.GenericRow;

import java.io.IOException;
import java.io.File;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class DatagenTask extends SourceTask {

  static final Logger log = LoggerFactory.getLogger(DatagenTask.class);

  private static final Schema KEY_SCHEMA = Schema.STRING_SCHEMA;
  private DatagenConnectorConfig config;

  private String topic;
  private Long interval;
  private Integer iterations;
  private String schemaFilename;
  private String schemaKeyfield;

  private Integer count = 0;

  private BlockingQueue<SourceRecord> queue = null;

  @Override
  public String version() {
    return VersionUtil.getVersion();
  }

  @Override
  public void start(Map<String, String> props) {
    config = new DatagenConnectorConfig(props);
    topic = config.getKafkaTopic();
    interval = config.getInterval();
    iterations = config.getIterations();
    schemaFilename = config.getSchemaFilename();
    schemaKeyfield = config.getSchemaKeyfield();
  }

  @Override
  public List<SourceRecord> poll() throws InterruptedException {

    try {
      Thread.sleep((long) (interval * Math.random()));
    } catch (InterruptedException e) {
      // Ignore the exception.
    }

    final List<SourceRecord> records = new ArrayList<>();

    Generator generator = null;

    //String schemaString = "{\"type\":\"record\",\"name\":\"Payment\",\"namespace\":\"io.confluent.examples.clients.basicavro\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"},{\"name\":\"amount\",\"type\":\"double\"}]}";
    //generator = new Generator(schemaString, new Random());
    final File schemaFile = new File(schemaFilename);
    try {
      generator = new Generator(schemaFile, new Random());
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Value
    org.apache.avro.Schema avroSchema = generator.schema();
    final AvroData avroData = new AvroData(1);
    org.apache.kafka.connect.data.Schema ksqlSchema = avroData.toConnectSchema(avroSchema);

    Map<String, ?> srcPartition = Collections.emptyMap();
    Map<String, ?> srcOffset = Collections.emptyMap();

    final Object generatedObject = generator.generate();

    if (!(generatedObject instanceof GenericRecord)) {
      throw new RuntimeException(String.format(
          "Expected Avro Random Generator to return instance of GenericRecord, found %s instead",
          generatedObject.getClass().getName()
      ));
    }
    final GenericRecord randomAvroMessage = (GenericRecord) generatedObject;

    final List<Object> genericRowValues = new ArrayList<>();

    for (org.apache.avro.Schema.Field field : avroSchema.getFields()) {

      final Object value = randomAvroMessage.get(field.name());
      if (value instanceof Record) {
        final Record record = (Record) value;
        final Object ksqlValue = avroData.toConnectData(record.getSchema(), record).value();
        genericRowValues.add(
            getOptionalValue(ksqlSchema.field(field.name()).schema(), ksqlValue));
      } else {
        genericRowValues.add(value);
      }

    }

    // Key
    String keyString = "";
    if (!schemaKeyfield.isEmpty()) {
      keyString = avroData.toConnectData(
                  randomAvroMessage.getSchema().getField(schemaKeyfield).schema(),
                  randomAvroMessage.get(schemaKeyfield)).value().toString();
    }

    // Value
    final org.apache.kafka.connect.data.Schema messageSchema = avroData.toConnectSchema(avroSchema);
    final Object messageValue = avroData.toConnectData(avroSchema, randomAvroMessage).value();

    records.add(
              new SourceRecord(
                  srcPartition,
                  srcOffset,
                  topic,
                  KEY_SCHEMA,
                  keyString,
                  messageSchema,
                  messageValue
              ));

    count = count + 1;
    if (count > iterations) {
      this.stop();
    }

    return records;

  }

  @Override
  public void stop() {
  }

  private org.apache.kafka.connect.data.Schema getOptionalSchema(
      final org.apache.kafka.connect.data.Schema schema) {
    switch (schema.type()) {
      case BOOLEAN:
        return org.apache.kafka.connect.data.Schema.OPTIONAL_BOOLEAN_SCHEMA;
      case INT32:
        return org.apache.kafka.connect.data.Schema.OPTIONAL_INT32_SCHEMA;
      case INT64:
        return org.apache.kafka.connect.data.Schema.OPTIONAL_INT64_SCHEMA;
      case FLOAT64:
        return org.apache.kafka.connect.data.Schema.OPTIONAL_FLOAT64_SCHEMA;
      case STRING:
        return org.apache.kafka.connect.data.Schema.OPTIONAL_STRING_SCHEMA;
      case ARRAY:
        return SchemaBuilder.array(getOptionalSchema(schema.valueSchema())).optional().build();
      case MAP:
        return SchemaBuilder.map(
            getOptionalSchema(schema.keySchema()),
            getOptionalSchema(schema.valueSchema()))
            .optional().build();
      case STRUCT:
        final SchemaBuilder schemaBuilder = SchemaBuilder.struct();
        for (Field field : schema.fields()) {
          schemaBuilder.field(field.name(), getOptionalSchema(field.schema()));
        }
        return schemaBuilder.optional().build();
      default:
        throw new ConnectException("Unsupported type: " + schema);
    }
  }

  private Object getOptionalValue(
      final org.apache.kafka.connect.data.Schema schema,
      final Object value) {
    switch (schema.type()) {
      case BOOLEAN:
      case INT32:
      case INT64:
      case FLOAT64:
      case STRING:
        return value;
      case ARRAY:
        final List<?> list = (List<?>) value;
        return list.stream().map(listItem -> getOptionalValue(schema.valueSchema(), listItem))
            .collect(Collectors.toList());
      case MAP:
        final Map<?, ?> map = (Map<?, ?>) value;
        return map.entrySet().stream()
            .collect(Collectors.toMap(
                k -> getOptionalValue(schema.keySchema(), k),
                v -> getOptionalValue(schema.valueSchema(), v)
            ));
      case STRUCT:
        final Struct struct = (Struct) value;
        final Struct optionalStruct = new Struct(getOptionalSchema(schema));
        for (Field field : schema.fields()) {
          optionalStruct
              .put(field.name(), getOptionalValue(field.schema(), struct.get(field.name())));
        }
        return optionalStruct;

      default:
        throw new ConnectException("Invalid value schema: " + schema + ", value = " + value);
    }
  }

}
