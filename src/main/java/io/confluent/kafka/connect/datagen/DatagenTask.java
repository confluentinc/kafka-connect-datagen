/*
 * Copyright 2018 Confluent Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.confluent.kafka.connect.datagen;

import io.confluent.avro.random.generator.Generator;
import io.confluent.connect.avro.AvroData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.header.ConnectHeaders;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatagenTask extends SourceTask {

  static final Logger log = LoggerFactory.getLogger(DatagenTask.class);

  private static final Schema DEFAULT_KEY_SCHEMA = Schema.OPTIONAL_STRING_SCHEMA;
  public static final String TASK_ID = "task.id";
  public static final String TASK_GENERATION = "task.generation";
  public static final String CURRENT_ITERATION = "current.iteration";
  public static final String RANDOM_SEED = "random.seed";

  private DatagenConnectorConfig config;
  private String topic;
  private long maxInterval;
  private long staticInterval;
  private long interval;
  private int maxRecords;
  private long count = 0L;
  private String schemaKeyField;
  private Generator generator;
  private org.apache.avro.Schema avroSchema;
  private org.apache.kafka.connect.data.Schema ksqlSchema;
  private AvroData avroData;
  private int taskId;
  private Map<String, Object> sourcePartition;
  private long taskGeneration;
  private final Random random = new Random();

  @Override
  public String version() {
    return VersionUtil.getVersion();
  }

  @Override
  public void start(Map<String, String> props) {
    config = new DatagenConnectorConfig(props);
    topic = config.getKafkaTopic();
    maxInterval = config.getMaxInterval();
    staticInterval = config.getStaticInterval();
    maxRecords = config.getIterations();
    schemaKeyField = config.getSchemaKeyfield();
    taskGeneration = 0;
    taskId = Integer.parseInt(props.get(TASK_ID));
    sourcePartition = Collections.singletonMap(TASK_ID, taskId);

    if (config.getRandomSeed() != null) {
      random.setSeed(config.getRandomSeed());
      // Each task will now deterministically advance it's random source
      // This makes it such that each task will generate different data
      for (int i = 0; i < taskId; i++) {
        random.setSeed(random.nextLong());
      }
    }

    Map<String, Object> offset = context.offsetStorageReader().offset(sourcePartition);
    if (offset != null) {
      //  The offset as it is stored contains our next state, so restore it as-is.
      taskGeneration = ((Long) offset.get(TASK_GENERATION)).intValue();
      count = ((Long) offset.get(CURRENT_ITERATION));
      random.setSeed((Long) offset.get(RANDOM_SEED));
    }

    avroSchema = config.getSchema();

    Generator.Builder generatorBuilder = new Generator.Builder()
        .random(random)
        .generation(count)
        .schema(avroSchema);

    generator = generatorBuilder.build();
    avroData = new AvroData(1);
    ksqlSchema = avroData.toConnectSchema(avroSchema);
  }

  @Override
  public List<SourceRecord> poll() throws InterruptedException {

    if (staticInterval > 0) {
      interval = staticInterval;
    } else if (maxInterval > 0) {
      interval = (long) (maxInterval * Math.random());
    }
    try {
      Thread.sleep(interval);
    } catch (InterruptedException e) {
      Thread.interrupted();
      return null;
    }

    final Object generatedObject = generator.generate();
    if (!(generatedObject instanceof GenericRecord)) {
      throw new RuntimeException(String.format(
          "Expected Avro Random Generator to return instance of GenericRecord, found %s instead",
          generatedObject.getClass().getName()
      ));
    }
    final GenericRecord randomAvroMessage = (GenericRecord) generatedObject;

    // Key
    SchemaAndValue key = new SchemaAndValue(DEFAULT_KEY_SCHEMA, null);
    if (!schemaKeyField.isEmpty()) {
      key = avroData.toConnectData(
          randomAvroMessage.getSchema().getField(schemaKeyField).schema(),
          randomAvroMessage.get(schemaKeyField)
      );
    }

    // Value
    final org.apache.kafka.connect.data.Schema messageSchema = avroData.toConnectSchema(avroSchema);
    final Object messageValue = avroData.toConnectData(avroSchema, randomAvroMessage).value();

    if (maxRecords > 0 && count >= maxRecords) {
      throw new ConnectException(
          String.format("Stopping connector: generated the configured %d number of messages", count)
      );
    }

    // Re-seed the random each time so that we can save the seed to the source offsets.
    long seed = random.nextLong();
    random.setSeed(seed);

    // The source offsets will be the values that the next task lifetime will restore from
    // Essentially, the "next" state of the connector after this loop completes
    Map<String, Object> sourceOffset = new HashMap<>();
    // The next lifetime will be a member of the next generation.
    sourceOffset.put(TASK_GENERATION, taskGeneration + 1);
    // We will have produced this record
    sourceOffset.put(CURRENT_ITERATION, count + 1);
    // This is the seed that we just re-seeded for our own next iteration.
    sourceOffset.put(RANDOM_SEED, seed);

    final ConnectHeaders headers = new ConnectHeaders();
    headers.addLong(TASK_GENERATION, taskGeneration);
    headers.addLong(TASK_ID, taskId);
    headers.addLong(CURRENT_ITERATION, count);

    final List<SourceRecord> records = new ArrayList<>();
    SourceRecord record = new SourceRecord(
        sourcePartition,
        sourceOffset,
        topic,
        null,
        key.schema(),
        key.value(),
        messageSchema,
        messageValue,
        null,
        headers
    );
    records.add(record);
    count += records.size();
    return records;
  }

  @Override
  public void stop() {
  }
}
