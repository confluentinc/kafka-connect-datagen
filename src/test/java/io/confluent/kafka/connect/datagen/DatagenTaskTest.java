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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.confluent.connect.avro.AvroData;

import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.kafka.connect.data.ConnectSchema;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTaskContext;
import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DatagenTaskTest {

  private static final String TOPIC = "my-topic";
  private static final int NUM_MESSAGES = 100;
  private static final int MAX_INTERVAL_MS = 0;
  private static final int TASK_ID = 0;

  private static final AvroData AVRO_DATA = new AvroData(20);

  private Map<String, String> config;
  private DatagenTask task;
  private List<SourceRecord> records;
  private Schema expectedValueConnectSchema;
  private Schema expectedKeyConnectSchema;
  private Map<String, Object> sourceOffsets;

  @Before
  public void setUp() throws Exception {
    config = new HashMap<>();
    records = new ArrayList<>();
    sourceOffsets = null;
  }

  @After
  public void tearDown() throws Exception {
    task.stop();
    task = null;
  }

  @Test
  public void shouldGenerateFilesForClickstreamCodesQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.CLICKSTREAM_CODES);
  }

  @Test
  public void shouldGenerateFilesForClickstreamUsersQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.CLICKSTREAM_USERS);
  }

  @Test
  public void shouldGenerateFilesForClickstreamQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.CLICKSTREAM);
  }

  @Test
  public void shouldGenerateFilesForPersonQuickstart() throws Exception {
    String personSchema = "{\n" +
            "  \"name\": \"SimplePersonAvro\",\n" +
            "  \"type\": \"record\",\n" +
            "  \"namespace\": \"simple.avro\",\n" +
            "  \"fields\": [\n" +
            "    {\n" +
            "      \"name\": \"person\",\n" +
            "      \"type\": {\n" +
            "        \"type\": \"array\",\n" +
            "        \"items\": {\n" +
            "          \"name\": \"Person\",\n" +
            "          \"type\": \"record\",\n" +
            "          \"fields\": [\n" +
            "            {\n" +
            "              \"name\": \"name\",\n" +
            "              \"type\": [\n" +
            "                \"null\",\n" +
            "                \"string\"\n" +
            "              ],\n" +
            "              \"default\": null\n" +
            "            },\n" +
            "            {\n" +
            "              \"name\": \"age\",\n" +
            "              \"type\": [\n" +
            "                \"null\",\n" +
            "                \"string\"\n" +
            "              ],\n" +
            "              \"default\": null\n" +
            "            }\n" +
            "          ]\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"father\",\n" +
            "      \"default\": null,\n" +
            "      \"type\": [\n" +
            "        \"null\",\n" +
            "        {\n" +
            "          \"name\": \"Parent\",\n" +
            "          \"type\": \"record\",\n" +
            "          \"fields\": [\n" +
            "            {\n" +
            "              \"name\": \"greatGrandParents\",\n" +
            "              \"default\": null,\n" +
            "              \"type\": [\n" +
            "                \"null\",\n" +
            "                {\n" +
            "                  \"type\": \"array\",\n" +
            "                  \"items\": \"simple.avro.Person\"\n" +
            "                }\n" +
            "              ]\n" +
            "            }\n" +
            "          ]\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    generateAndValidateRecordsForSchemaString(personSchema, "person");
  }

  @Test
  public void shouldGenerateFilesForOrdersQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.ORDERS);
  }

  @Test
  public void shouldGenerateFilesForRatingsQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.RATINGS);
  }

  @Test
  public void shouldGenerateFilesForUsersQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.USERS);
  }

  @Test
  public void shouldGenerateFilesForUsers2Quickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.USERS_);
  }

  @Test
  public void shouldGenerateFilesForPageviewsQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.PAGEVIEWS);
  }

  @Test
  public void shouldGenerateFilesForStockTradesQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.STOCK_TRADES);
  }

  @Test
  public void shouldGenerateFilesForProductQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.PRODUCT);
  }

  @Test
  public void shouldGenerateFilesForPurchaseQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.PURCHASES);
  }

  @Test
  public void shouldGenerateFilesForInventoryQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.INVENTORY);
  }

  @Test
  public void shouldGenerateFilesForCreditCardsQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.CREDIT_CARDS);
  }

  @Test
  public void shouldGenerateFilesForTransactionsQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.TRANSACTIONS);
  }

  @Test
  public void shouldGenerateFilesForStoresQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.STORES);
  }
  
  @Test
  public void shouldGenerateFilesForCampaignFinanceQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.CAMPAIGN_FINANCE);
  }

  @Test
  public void shouldGenerateFilesForFleetMgmtDescriptionQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.FLEET_MGMT_DESCRIPTION);
  }

  @Test
  public void shouldGenerateFilesForFleetMgmtLocationQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.FLEET_MGMT_LOCATION);
  }

  @Test
  public void shouldGenerateFilesForFleetMgmtSensorsQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.FLEET_MGMT_SENSORS);
  }

  @Test
  public void shouldGenerateFilesForPizzaOrdersQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.PIZZA_ORDERS);
  }

  @Test
  public void shouldGenerateFilesForPizzaOrdersCompletedQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.PIZZA_ORDERS_COMPLETED);
  }

  @Test
  public void shouldGenerateFilesForPizzaOrdersCancelledQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.PIZZA_ORDERS_CANCELLED);
  }

  @Test
  public void shouldGenerateFilesForInsuranceOffersQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.INSURANCE_OFFERS);
  }

  @Test
  public void shouldGenerateFilesForInsuranceCustomersQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.INSURANCE_CUSTOMERS);
  }

  @Test
  public void shouldGenerateFilesForInsuranceCustomerActivityQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.INSURANCE_CUSTOMER_ACTIVITY);
  }

  @Test
  public void shouldGenerateFilesForGamingGamesQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.GAMING_GAMES);
  }

  @Test
  public void shouldGenerateFilesForGamingPlayersQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.GAMING_PLAYERS);
  }

  @Test
  public void shouldGenerateFilesForGamingPlayerActivityQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.GAMING_PLAYER_ACTIVITY);
  }

  @Test
  public void shouldGenerateFilesForPayrollEmployeeQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.PAYROLL_EMPLOYEE);
  }

  @Test
  public void shouldGenerateFilesForPayrollEmployeeLocationQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.PAYROLL_EMPLOYEE_LOCATION);
  }

  @Test
  public void shouldGenerateFilesForPayrollBonusQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.PAYROLL_BONUS);
  }

  @Test
  public void shouldGenerateFilesForSysLogsLogsQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.SYSLOG_LOGS);
  }

  @Test
  public void shouldGenerateFilesForDeviceInformationQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.DEVICE_INFORMATION);
  }

  @Test
  public void shouldGenerateFilesForSiemLogsQuickstart() throws Exception {
    generateAndValidateRecordsFor(Quickstart.SIEM_LOGS);
  }

  @Test
  public void shouldUseConfiguredKeyFieldForQuickstartIfProvided() throws Exception {
    // Do the same thing with schema text
    Quickstart quickstart = Quickstart.PAGEVIEWS;
    assertNotEquals(quickstart.getSchemaKeyField(), "pageid");
    createTaskWithSchemaText(slurp(quickstart.getSchemaFilename()), "pageid");
    generateRecords();
    assertRecordsMatchSchemas();
  }

  @Test
  public void shouldRestoreFromSourceOffsets() throws Exception {
    // Give the task an arbitrary source offset
    sourceOffsets = new HashMap<>();
    sourceOffsets.put(DatagenTask.RANDOM_SEED, 100L);
    sourceOffsets.put(DatagenTask.CURRENT_ITERATION, 50L);
    sourceOffsets.put(DatagenTask.TASK_GENERATION, 0L);
    createTaskWith(Quickstart.ORDERS);

    // poll once to advance the generator
    SourceRecord firstPoll = task.poll().get(0);
    // poll a second time to predict the future
    SourceRecord pollA = task.poll().get(0);
    // extract the offsets after the first poll to restore to the next task instance
    //noinspection unchecked
    sourceOffsets = (Map<String, Object>) firstPoll.sourceOffset();
    createTaskWith(Quickstart.ORDERS);
    // poll once after the restore
    SourceRecord pollB = task.poll().get(0);

    // the generation should have incremented, but the remaining details of the record should be identical
    assertEquals(1L, pollA.sourceOffset().get(DatagenTask.TASK_GENERATION));
    assertEquals(2L, pollB.sourceOffset().get(DatagenTask.TASK_GENERATION));
    assertEquals(pollA.sourceOffset().get(DatagenTask.TASK_ID), pollB.sourceOffset().get(DatagenTask.TASK_ID));
    assertEquals(pollA.sourceOffset().get(DatagenTask.CURRENT_ITERATION), pollB.sourceOffset().get(DatagenTask.CURRENT_ITERATION));
    assertEquals(pollA.sourcePartition(), pollB.sourcePartition());
    assertEquals(pollA.valueSchema(), pollB.valueSchema());
    assertEquals(pollA.value(), pollB.value());
  }

  @Test
  public void shouldInjectHeaders()  throws Exception {
    createTaskWith(Quickstart.USERS);
    generateRecords();
    for (SourceRecord record : records) {
      assertEquals((long) TASK_ID, record.headers().lastWithName(DatagenTask.TASK_ID).value());
      assertEquals(0L, record.headers().lastWithName(DatagenTask.TASK_GENERATION).value());
      assertNotNull(record.headers().lastWithName(DatagenTask.CURRENT_ITERATION));
    }
  }

  @Test
  public void shouldFailToGenerateMoreRecordsThanSpecified() throws Exception {
    // Generate the expected number of records
    createTaskWith(Quickstart.USERS);
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

  private void generateAndValidateRecordsFor(Quickstart quickstart) throws Exception {
    createTaskWith(quickstart);
    generateRecords();
    assertRecordsMatchSchemas();

    // Do the same thing with schema file
    createTaskWithSchema(quickstart.getSchemaFilename(), quickstart.getSchemaKeyField());
    generateRecords();
    assertRecordsMatchSchemas();

    // Do the same thing with schema text
    createTaskWithSchemaText(slurp(quickstart.getSchemaFilename()), quickstart.getSchemaKeyField());
    generateRecords();
    assertRecordsMatchSchemas();
  }

  private void generateAndValidateRecordsForSchemaString(String schemaString, String keyField) throws Exception {
    createTaskWithSchemaText(schemaString, keyField);
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
      assertEquals(expectedKeyConnectSchema.name(), record.keySchema().name());
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
    try {
      ConnectSchema.validateValue(expected, value);
    } catch (DataException e) {
      return false;
    }
    return true;
  }

  private void dropSchemaSourceConfigs() {
    config.remove(DatagenConnectorConfig.QUICKSTART_CONF);
    config.remove(DatagenConnectorConfig.SCHEMA_FILENAME_CONF);
    config.remove(DatagenConnectorConfig.SCHEMA_STRING_CONF);
  }

  private void createTaskWith(Quickstart quickstart) {
    dropSchemaSourceConfigs();
    config.put(
        DatagenConnectorConfig.QUICKSTART_CONF,
        quickstart.name().toLowerCase(Locale.getDefault())
    );
    createTask();
    loadKeyAndValueSchemas(quickstart.getSchemaFilename(), quickstart.getSchemaKeyField());
  }

  private void createTaskWithSchema(String schemaResourceFilename, String idFieldName) {
    dropSchemaSourceConfigs();
    config.put(DatagenConnectorConfig.SCHEMA_FILENAME_CONF, schemaResourceFilename);
    config.put(DatagenConnectorConfig.SCHEMA_KEYFIELD_CONF, idFieldName);
    createTask();
    loadKeyAndValueSchemas(schemaResourceFilename, idFieldName);
  }

  private void createTaskWithSchemaText(String schemaText, String keyField) {
    dropSchemaSourceConfigs();
    config.put(DatagenConnectorConfig.SCHEMA_STRING_CONF, schemaText);
    config.put(DatagenConnectorConfig.SCHEMA_KEYFIELD_CONF, keyField);
    createTask();
    loadKeyAndValueSchemasFromString(schemaText, keyField);
  }

  private void createTask() {
    config.putIfAbsent(DatagenConnectorConfig.KAFKA_TOPIC_CONF, TOPIC);
    config.putIfAbsent(DatagenConnectorConfig.ITERATIONS_CONF, Integer.toString(NUM_MESSAGES));
    config.putIfAbsent(DatagenConnectorConfig.MAXINTERVAL_CONF, Integer.toString(MAX_INTERVAL_MS));
    config.putIfAbsent(DatagenTask.TASK_ID, Integer.toString(TASK_ID));

    task = new DatagenTask();
    // Initialize an offsetStorageReader that returns mocked sourceOffsets.
    task.initialize(new SourceTaskContext() {
      @Override
      public Map<String, String> configs() {
        return config;
      }

      @Override
      public OffsetStorageReader offsetStorageReader() {
        return new OffsetStorageReader() {
          @Override
          public <T> Map<String, Object> offset(final Map<String, T> partition) {
            return offsets(Collections.singletonList(partition)).get(partition);
          }

          @Override
          public <T> Map<Map<String, T>, Map<String, Object>> offsets(
              final Collection<Map<String, T>> partitions) {
            if (sourceOffsets == null) {
              return Collections.emptyMap();
            }
            return partitions
                .stream()
                .collect(Collectors.toMap(
                    Function.identity(),
                    ignored -> sourceOffsets
                ));
          }
        };
      }
    });
    task.start(config);
  }

  private void loadKeyAndValueSchemasFromString(String schemaString, String keyFieldName) {
    org.apache.avro.Schema avroSchema = new org.apache.avro.Schema.Parser().parse(schemaString);
    loadKeyAndValueSchemas(avroSchema, keyFieldName);
  }

  private void loadKeyAndValueSchemas(String schemaResourceFilename, String idFieldName) {
    org.apache.avro.Schema expectedValueAvroSchema = loadAvroSchema(schemaResourceFilename);
    loadKeyAndValueSchemas(expectedValueAvroSchema, idFieldName);
  }

  private org.apache.avro.Schema loadAvroSchema(String schemaFilename) {
    try {
      InputStream schemaStream = getClass().getClassLoader().getResourceAsStream(schemaFilename);
      return (new org.apache.avro.Schema.Parser()).parse(schemaStream);
    } catch (IOException e) {
      throw new ConnectException("Unable to read the '" + schemaFilename + "' schema file", e);
    }
  }

  private void loadKeyAndValueSchemas(org.apache.avro.Schema expectedSchema, String idFieldName) {
    expectedValueConnectSchema = AVRO_DATA.toConnectSchema(expectedSchema);

    // Datagen defaults to an optional string key schema if a key field is not specified
    expectedKeyConnectSchema = Schema.OPTIONAL_STRING_SCHEMA;

    if (idFieldName != null) {
      // Check that the Avro schema has the named field
      org.apache.avro.Schema expectedKeyAvroSchema = expectedSchema.getField(idFieldName).schema();
      assertNotNull(expectedKeyAvroSchema);
      expectedKeyConnectSchema = AVRO_DATA.toConnectSchema(expectedKeyAvroSchema);
    }
  }

  private String slurp(final String filename) {
    final InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filename);
    if (inputStream == null) {
      throw new RuntimeException("Could not find file " + filename);
    }
    return new BufferedReader(new InputStreamReader(inputStream)).lines()
        .collect(Collectors.joining("\n"));
  }
}
