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

import io.confluent.kafka.connect.datagen.DatagenTask.Quickstart;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DatagenConnectorTest {

  private static final String TOPIC = "my-topic";
  private static final int NUM_MESSAGES = 100;
  private static final int MAX_INTERVAL_MS = 0;

  private Map<String, String> config;
  private DatagenConnector connector;

  @Before
  public void setUp() throws Exception {
    config = new HashMap<>();
    config.put(DatagenConnectorConfig.KAFKA_TOPIC_CONF, TOPIC);
    config.put(DatagenConnectorConfig.ITERATIONS_CONF, Integer.toString(NUM_MESSAGES));
    config.put(DatagenConnectorConfig.MAXINTERVAL_CONF, Integer.toString(MAX_INTERVAL_MS));
    config.put(DatagenConnectorConfig.QUICKSTART_CONF, DatagenTask.Quickstart.USERS.name());
    connector = new DatagenConnector();
  }

  @After
  public void tearDown() throws Exception {
    connector.stop();
  }

  @Test
  public void shouldCreateTasks() {
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
  public void shouldAllowSettingQuickstart() {
    clearSchemaSources();
    config.put(DatagenConnectorConfig.QUICKSTART_CONF, Quickstart.USERS.name());

    Config validated = connector.validate(config);

    for (String k : DatagenConnectorConfig.schemaSourceKeys()) {
      assertThat(validated, hasNoValidationErrorsFor(k));
    }
  }

  @Test
  public void shouldAllowSettingSchema() {
    clearSchemaSources();
    config.put(DatagenConnectorConfig.SCHEMA_STRING_CONF, "a schema");

    Config validated = connector.validate(config);

    for (String k : DatagenConnectorConfig.schemaSourceKeys()) {
      assertThat(validated, hasNoValidationErrorsFor(k));
    }
  }

  @Test
  public void shouldAllowSettingSchemaFile() {
    clearSchemaSources();
    config.put(DatagenConnectorConfig.SCHEMA_FILENAME_CONF, "a schema file");

    Config validated = connector.validate(config);

    for (String k : DatagenConnectorConfig.schemaSourceKeys()) {
      assertThat(validated, hasNoValidationErrorsFor(k));
    }
  }

  @Test
  public void shouldFailValidationWithMultipleSchemaSources() {
    clearSchemaSources();
    config.put(DatagenConnectorConfig.SCHEMA_STRING_CONF, "a schema");
    config.put(DatagenConnectorConfig.SCHEMA_FILENAME_CONF, "a schema file");

    Config validated = connector.validate(config);

    assertThat(
        validated,
        hasValidationError(
            DatagenConnectorConfig.SCHEMA_STRING_CONF,
            DatagenConnector.SCHEMA_SOURCE_ERR
        )
    );
    assertThat(
        validated,
        hasValidationError(
            DatagenConnectorConfig.SCHEMA_FILENAME_CONF,
            DatagenConnector.SCHEMA_SOURCE_ERR
        )
    );
  }

  @Test
  public void shouldFailValidationWithNoSchemaSources() {
    clearSchemaSources();

    Config validated = connector.validate(config);

    for (String k : DatagenConnectorConfig.schemaSourceKeys()) {
      assertThat(validated, hasValidationError(k, DatagenConnector.SCHEMA_SOURCE_ERR));
    }
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

  private Matcher<Config> hasValidationError(String key, String error) {
    return new HasValidationError(key, error);
  }

  private static class HasValidationError extends TypeSafeMatcher<Config> {
    final String key;
    final String error;

    private HasValidationError(String key, String error) {
      this.key = Objects.requireNonNull(key, "key");
      this.error = Objects.requireNonNull(error, "error");
    }

    @Override
    protected boolean matchesSafely(Config config) {
      Optional<ConfigValue> value = value(config);
      return value.isPresent() && value.get().errorMessages().contains(error);
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