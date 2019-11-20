/**
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
 **/

package io.confluent.kafka.connect.datagen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import io.confluent.avro.random.generator.Generator;
import io.confluent.connect.avro.AvroData;

import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.source.SourceRecord;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DatagenTaskTest {

  private static final String TOPIC = "my-topic";
  private static final int NUM_MESSAGES = 100;
  private static final int MAX_INTERVAL_MS = 0;

  private static final AvroData AVRO_DATA = new AvroData(20);

  private Map<String, String> config;
  private DatagenTask task;
  private List<SourceRecord> records;
  private Schema expectedValueConnectSchema;
  private Schema expectedKeyConnectSchema;

  @Before
  public void setUp() throws Exception {
    config = new HashMap<>();
    records = new ArrayList<>();
  }

  @After
  public void tearDown() throws Exception {
    task.stop();
    task = null;
  }

  @Test
  public void shouldGenerateFilesForClickstreamCodesQuickstart() throws Exception {
    generateAndValidateRecordsFor(DatagenTask.Quickstart.CLICKSTREAM_CODES);
  }

  @Test
  public void shouldGenerateFilesForClickstreamUsersQuickstart() throws Exception {
    generateAndValidateRecordsFor(DatagenTask.Quickstart.CLICKSTREAM_USERS);
  }

  @Test
  public void shouldGenerateFilesForClickstreamQuickstart() throws Exception {
    generateAndValidateRecordsFor(DatagenTask.Quickstart.CLICKSTREAM);
  }

  @Test
  public void shouldGenerateFilesForOrdersQuickstart() throws Exception {
    generateAndValidateRecordsFor(DatagenTask.Quickstart.ORDERS);
  }

  @Test
  public void shouldGenerateFilesForRatingsQuickstart() throws Exception {
    generateAndValidateRecordsFor(DatagenTask.Quickstart.RATINGS);
  }

  @Test
  public void shouldGenerateFilesForUsersQuickstart() throws Exception {
    generateAndValidateRecordsFor(DatagenTask.Quickstart.USERS);
  }

  @Test
  public void shouldGenerateFilesForUsers2Quickstart() throws Exception {
    generateAndValidateRecordsFor(DatagenTask.Quickstart.USERS_);
  }

  @Test
  public void shouldGenerateFilesForPageviewsQuickstart() throws Exception {
    generateAndValidateRecordsFor(DatagenTask.Quickstart.PAGEVIEWS);
  }

  @Test
  public void shouldGenerateFilesForStockTradesQuickstart() throws Exception {
    generateAndValidateRecordsFor(DatagenTask.Quickstart.STOCK_TRADES);
  }

  @Test
  public void shouldFailToGenerateMoreRecordsThanSpecified() throws Exception {
    // Generate the expected number of records
    createTaskWith(DatagenTask.Quickstart.USERS);
    generateRecords();
    assertRecordsMatchSchemas();

    // Attempt to get another batch of records, but the task is expected to fail
    try {
      task.poll();
      fail("Expected poll to fail");
    } catch (ConnectException e) {
      // expected
    }
  }

  private void generateAndValidateRecordsFor(DatagenTask.Quickstart quickstart) throws Exception {
    createTaskWith(quickstart);
    generateRecords();
    assertRecordsMatchSchemas();

    // Do the same thing with schema file
    createTaskWithSchema(quickstart.getSchemaFilename(), quickstart.getSchemaKeyField());
    generateRecords();
    assertRecordsMatchSchemas();
  }

  private void generateRecords() throws Exception {
    records.clear();
    while (records.size() < NUM_MESSAGES) {
      List<SourceRecord> newRecords = task.poll();
      assertNotNull(newRecords);
      assertEquals(1, newRecords.size());
      records.addAll(newRecords);
    }
  }

  private void assertRecordsMatchSchemas() {
    for (SourceRecord record : records) {
      // Check the key
      assertEquals(expectedKeyConnectSchema, record.keySchema());
      if (expectedKeyConnectSchema != null) {
        assertTrue(isConnectInstance(record.key(), expectedKeyConnectSchema));
      }

      // Check the value
      assertEquals(expectedValueConnectSchema, record.valueSchema());
      if (expectedValueConnectSchema != null) {
        assertTrue(isConnectInstance(record.value(), expectedValueConnectSchema));
      }
    }
  }

  private boolean isConnectInstance(Object value, Schema expected) {
    if (expected.isOptional() && value == null) {
      return true;
    }
    switch (expected.type()) {
      case BOOLEAN:
        return value instanceof Boolean;
      case BYTES:
        return value instanceof byte[];
      case INT8:
        return value instanceof Byte;
      case INT16:
        return value instanceof Short;
      case INT32:
        return value instanceof Integer;
      case INT64:
        return value instanceof Long;
      case FLOAT32:
        return value instanceof Float;
      case FLOAT64:
        return value instanceof Double;
      case STRING:
        return value instanceof String;
      case ARRAY:
        return value instanceof List;
      case MAP:
        return value instanceof Map;
      case STRUCT:
        if (value instanceof Struct) {
          Struct struct = (Struct) value;
          for (Field field : expected.fields()) {
            Object fieldValue = struct.get(field.name());
            if (!isConnectInstance(fieldValue, field.schema())) {
              return false;
            }
          }
          return true;
        }
        return false;
      default:
        throw new IllegalArgumentException("Unexpected enum schema");
    }
  }

  private void createTaskWith(DatagenTask.Quickstart quickstart) {
    config.put(
        DatagenConnectorConfig.QUICKSTART_CONF,
        quickstart.name().toLowerCase(Locale.getDefault())
    );
    createTask();
    loadKeyAndValueSchemas(quickstart.getSchemaFilename(), quickstart.getSchemaKeyField());
  }

  private void createTaskWithSchema(String schemaResourceFilename, String idFieldName) {
    config.put(DatagenConnectorConfig.SCHEMA_FILENAME_CONF, schemaResourceFilename);
    config.put(DatagenConnectorConfig.SCHEMA_KEYFIELD_CONF, idFieldName);
    createTask();
    loadKeyAndValueSchemas(schemaResourceFilename, idFieldName);
  }

  private void createTask() {
    config.putIfAbsent(DatagenConnectorConfig.KAFKA_TOPIC_CONF, TOPIC);
    config.putIfAbsent(DatagenConnectorConfig.ITERATIONS_CONF, Integer.toString(NUM_MESSAGES));
    config.putIfAbsent(DatagenConnectorConfig.MAXINTERVAL_CONF, Integer.toString(MAX_INTERVAL_MS));

    task = new DatagenTask();
    task.start(config);
  }

  private void loadKeyAndValueSchemas(String schemaResourceFilename, String idFieldName) {
    org.apache.avro.Schema expectedValueAvroSchema = loadAvroSchema(schemaResourceFilename);
    expectedValueConnectSchema = AVRO_DATA.toConnectSchema(expectedValueAvroSchema);

    if (idFieldName != null) {
      // Check that the Avro schema has the named field
      org.apache.avro.Schema expectedKeyAvroSchema = expectedValueAvroSchema.getField(idFieldName).schema();
      assertNotNull(expectedKeyAvroSchema);
      expectedKeyConnectSchema = AVRO_DATA.toConnectSchema(expectedKeyAvroSchema);
    }

    // Right now, Datagen always uses non-null strings for the key!
    expectedKeyConnectSchema = Schema.STRING_SCHEMA;
  }

  private org.apache.avro.Schema loadAvroSchema(String schemaFilename) {
    try {
      Generator generator = new Generator(
          getClass().getClassLoader().getResourceAsStream(schemaFilename),
          new Random()
      );
      return generator.schema();
    } catch (IOException e) {
      throw new ConnectException("Unable to read the '" + schemaFilename + "' schema file", e);
    }
  }
}