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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Objects;
import java.util.Optional;
import org.apache.kafka.common.config.Config;
import org.apache.kafka.common.config.ConfigValue;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DatagenConnectorTest {

  private static final String TOPIC = "my-topic";
  private static final int NUM_MESSAGES = 100;
  private static final int MAX_INTERVAL_MS = 0;

  private Map<String, String> config;
  private DatagenConnector connector;

  @BeforeEach
  void setUp() throws Exception {
    config = new HashMap<>();
    config.put(DatagenConnectorConfig.KAFKA_TOPIC_CONF, TOPIC);
    config.put(DatagenConnectorConfig.ITERATIONS_CONF, Integer.toString(NUM_MESSAGES));
    config.put(DatagenConnectorConfig.MAXINTERVAL_CONF, Integer.toString(MAX_INTERVAL_MS));
    config.put(DatagenConnectorConfig.QUICKSTART_CONF, Quickstart.USERS.name());
    connector = new DatagenConnector();
  }

  @AfterEach
  void tearDown() throws Exception {
    connector.stop();
  }

  @Test
  void shouldCreateTasks() {
    connector.start(config);

    assertTaskConfigs(1);
    assertTaskConfigs(2);
    assertTaskConfigs(4);
    assertTaskConfigs(10);
    for (int i=0; i!=100; ++i) {
      assertTaskConfigs(0);
    }
  }

  @Test
  void shouldAllowSettingQuickstart() {
    clearSchemaSources();
    config.put(DatagenConnectorConfig.QUICKSTART_CONF, Quickstart.USERS.name());
    Config validated = connector.validate(config);

    for (String k : DatagenConnectorConfig.schemaSourceKeys()) {
      assertThat(validated, hasNoValidationErrorsFor(k));
    }
  }

  @Test
  void shouldAllowSettingSchema() {
    clearSchemaSources();
    config.put(DatagenConnectorConfig.SCHEMA_STRING_CONF,
            "{\"namespace\":\"ksql\",\"name\":\"test_schema\",\"type\":\"record\",\"fields\":" +
                    "[{\"name\":\"id\",\"type\":{\"type\":\"long\",\"arg.properties\"" +
                    ":{\"iteration\":{\"start\":0}}}}]}");
    Config validated = connector.validate(config);

    for (String k : DatagenConnectorConfig.schemaSourceKeys()) {
      assertThat(validated, hasNoValidationErrorsFor(k));
    }
  }

  @Test
  void shouldAllowSettingSchemaFile() {
    clearSchemaSources();
    config.put(DatagenConnectorConfig.SCHEMA_FILENAME_CONF, "product.avro");
    Config validated = connector.validate(config);

    for (String k : DatagenConnectorConfig.schemaSourceKeys()) {
      assertThat(validated, hasNoValidationErrorsFor(k));
    }
  }

  @Test
  void shouldFailValidationWithMultipleSchemaSources() {
    clearSchemaSources();
    config.put(DatagenConnectorConfig.SCHEMA_STRING_CONF,
            "{\"namespace\":\"ksql\",\"name\":\"test_schema\",\"type\":\"record\",\"fields\":"
                + "[{\"name\":\"id\",\"type\":{\"type\":\"long\",\"arg.properties\""
                + ":{\"iteration\":{\"start\":0}}}}]}");
    config.put(DatagenConnectorConfig.SCHEMA_FILENAME_CONF, "product.avro");
    Config validated = connector.validate(config);

    assertThat(
        validated,
        hasValidationError(DatagenConnectorConfig.SCHEMA_STRING_CONF, 1)
    );
    assertThat(
        validated,
        hasValidationError(DatagenConnectorConfig.SCHEMA_FILENAME_CONF, 1)
    );
  }

  @Test
  void shouldFailValidationWithNoSchemaSources() {
    clearSchemaSources();
    Config validated = connector.validate(config);

    for (String k : DatagenConnectorConfig.schemaSourceKeys()) {
      assertThat(validated, hasValidationError(k, 1));
    }
  }

  @Test
  void shouldFailValidationWithInvalidSchemaString() {
    clearSchemaSources();
    config.put(DatagenConnectorConfig.SCHEMA_STRING_CONF, "a schema");
    Config validated = connector.validate(config);
    assertThat(
      validated,
      hasValidationError(DatagenConnectorConfig.SCHEMA_STRING_CONF, 1)
    );
  }

  @Test
  void shouldFailValidationWithInvalidFileName() {
    clearSchemaSources();
    config.put(DatagenConnectorConfig.SCHEMA_FILENAME_CONF, "a file name");
    Config validated = connector.validate(config);
    assertThat(
      validated,
      hasValidationError(DatagenConnectorConfig.SCHEMA_FILENAME_CONF, 1)
    );
  }

  @Test
  void shouldFailValidationWithInvalidSchemaFile() {
    clearSchemaSources();
    config.put(DatagenConnectorConfig.SCHEMA_FILENAME_CONF, "invalid_users_schema.avro");
    Config validated = connector.validate(config);
    assertThat(
      validated,
      hasValidationError(DatagenConnectorConfig.SCHEMA_FILENAME_CONF, 1)
    );
  }

  @Test
  void shouldFailValidationWithInvalidSchemaKeyField() {
    clearSchemaSources();
    config.put(DatagenConnectorConfig.SCHEMA_FILENAME_CONF, "product.avro");
    config.put(DatagenConnectorConfig.SCHEMA_KEYFIELD_CONF, "key_does_not_exist");
    Config validated = connector.validate(config);
    assertThat(
      validated,
      hasValidationError(DatagenConnectorConfig.SCHEMA_KEYFIELD_CONF, 1)
    );
  }

  @Test
  void shouldNotValidateSchemaKeyFieldWhenSchemaSourceFailsValidation() {
    clearSchemaSources();
    config.put(DatagenConnectorConfig.SCHEMA_FILENAME_CONF, "invalid_users_schema.avro");
    config.put(DatagenConnectorConfig.SCHEMA_KEYFIELD_CONF, "key_does_not_exist");
    Config validated = connector.validate(config);
    assertThat(
      validated,
      hasValidationError(DatagenConnectorConfig.SCHEMA_FILENAME_CONF, 1)
    );
    assertThat(
      validated,
      hasNoValidationErrorsFor(DatagenConnectorConfig.SCHEMA_KEYFIELD_CONF)
    );
  }

  @Test
  void shouldNotValidateSchemaKeyFieldWhenMultipleSchemaSourcesAreSet() {
    clearSchemaSources();
    config.put(DatagenConnectorConfig.SCHEMA_FILENAME_CONF, "product.avro");
    config.put(DatagenConnectorConfig.QUICKSTART_CONF, "clickstream");
    config.put(DatagenConnectorConfig.SCHEMA_KEYFIELD_CONF, "key_does_not_exist");
    Config validated = connector.validate(config);
    assertThat(
      validated,
      hasValidationError(DatagenConnectorConfig.SCHEMA_FILENAME_CONF, 1)
    );
    assertThat(
      validated,
      hasValidationError(DatagenConnectorConfig.QUICKSTART_CONF, 1)
    );
    assertThat(
      validated,
      hasNoValidationErrorsFor(DatagenConnectorConfig.SCHEMA_KEYFIELD_CONF)
    );
  }

  protected void assertTaskConfigs(int maxTasks) {
    List<Map<String, String>> taskConfigs = connector.taskConfigs(maxTasks);
    assertEquals(maxTasks, taskConfigs.size());
    // All task configs should match the connector config
    for (int i = 0; i < taskConfigs.size(); i++) {
      Map<String, String> taskConfig = taskConfigs.get(i);
      Map<String, String> expectedTaskConfig = new HashMap<>(config);
      expectedTaskConfig.put(DatagenTask.TASK_ID, Integer.toString(i));
      assertEquals(expectedTaskConfig, taskConfig);
    }
  }

  private void clearSchemaSources() {
    for (String k : DatagenConnectorConfig.schemaSourceKeys()) {
      config.remove(k);
    }
  }

  private Matcher<Config> hasNoValidationErrorsFor(String key) {
    return new HasNoValidationErrors(key);
  }

  private static class HasNoValidationErrors extends TypeSafeMatcher<Config> {
    final String key;

    private HasNoValidationErrors(String key) {
      this.key = Objects.requireNonNull(key, "key");
    }

    @Override
    protected boolean matchesSafely(Config config) {
      Optional<ConfigValue> value = value(config);
      return value.isPresent() && value.get().errorMessages().isEmpty();
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("Should have no validation errors for key: " + key);
    }

    @Override
    protected void describeMismatchSafely(Config config, Description mismatchDescription) {
      Optional<ConfigValue> value = value(config);
      if (value.isPresent()) {
        mismatchDescription.appendText(
            "Found errors for " + key + ":" + String.join(", ", value.get().errorMessages()));
      } else {
        mismatchDescription.appendText("Found no value for " + key);
      }
      super.describeMismatchSafely(config, mismatchDescription);
    }

    private Optional<ConfigValue> value(Config config) {
      return config.configValues().stream().filter(v -> v.name().equals(key)).findFirst();
    }
  }

  private Matcher<Config> hasValidationError(String key, Integer errorCount) {
    return new HasValidationError(key, errorCount);
  }

  private static class HasValidationError extends TypeSafeMatcher<Config> {
    final String key;
    final Integer errorCount;

    private HasValidationError(String key, Integer errorCount) {
      this.key = Objects.requireNonNull(key, "key");
      this.errorCount = Objects.requireNonNull(errorCount, "errorCount");
    }

    @Override
    protected boolean matchesSafely(Config config) {
      Optional<ConfigValue> value = value(config);
      return value.isPresent() && value.get().errorMessages().size() == errorCount;
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("Should have no validation errors for key: " + key);
    }

    @Override
    protected void describeMismatchSafely(Config config, Description mismatchDescription) {
      Optional<ConfigValue> value = value(config);
      if (value.isPresent()) {
        mismatchDescription.appendText(
            "errors for " + key + ":" + String.join(", ", value.get().errorMessages()));
      } else {
        mismatchDescription.appendText("Found no value for " + key);
      }
      super.describeMismatchSafely(config, mismatchDescription);
    }

    private Optional<ConfigValue> value(Config config) {
      return config.configValues().stream().filter(v -> v.name().equals(key)).findFirst();
    }
  }
}