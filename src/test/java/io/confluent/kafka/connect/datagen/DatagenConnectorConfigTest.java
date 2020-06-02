/**
 * Copyright 2020 Confluent Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.apache.kafka.common.config.ConfigException;
import org.junit.Test;

public class DatagenConnectorConfigTest {
  private static final Map<String, String> COMMON_PROPERTIES = ImmutableMap.of(
    DatagenConnectorConfig.KAFKA_TOPIC_CONF, "topic"
  );
  private static final Map<String, String> SOURCES = ImmutableMap.of(
      DatagenConnectorConfig.SCHEMA_STRING_CONF, "schema-string",
      DatagenConnectorConfig.SCHEMA_FILENAME_CONF, "schema-file",
      DatagenConnectorConfig.QUICKSTART_CONF, "quickstart"
  );

  @Test
  public void shouldAllowSettingQuickstart() {
    DatagenConnectorConfig config = new DatagenConnectorConfig(
        ImmutableMap.<String, String>builder()
            .putAll(COMMON_PROPERTIES)
            .put(DatagenConnectorConfig.QUICKSTART_CONF, "quickstart")
            .build()
    );
    assertEquals(config.getQuickstart(), "quickstart");
  }

  @Test
  public void shouldAllowSettingSchemaFile() {
    DatagenConnectorConfig config = new DatagenConnectorConfig(
        ImmutableMap.<String, String>builder()
            .putAll(COMMON_PROPERTIES)
            .put(DatagenConnectorConfig.SCHEMA_FILENAME_CONF, "schema-file")
            .build()
    );
    assertEquals(config.getSchemaFilename(), "schema-file");
  }

  @Test
  public void shouldAllowSettingSchemaString() {
    DatagenConnectorConfig config = new DatagenConnectorConfig(
        ImmutableMap.<String, String>builder()
            .putAll(COMMON_PROPERTIES)
            .put(DatagenConnectorConfig.SCHEMA_STRING_CONF, "schema-string")
            .build()
    );
    assertEquals(config.getSchemaString(), "schema-string");
  }

  @Test
  public void shouldNotAllowSettingSchemaStringWithQuickstart() {
    shouldOnlyAllowOneSchemaSource(
        DatagenConnectorConfig.SCHEMA_STRING_CONF, DatagenConnectorConfig.QUICKSTART_CONF);
  }

  @Test
  public void shouldNotAllowSettingSchemaStringWithSchemaFile() {
    shouldOnlyAllowOneSchemaSource(
        DatagenConnectorConfig.SCHEMA_STRING_CONF, DatagenConnectorConfig.SCHEMA_FILENAME_CONF);
  }

  private void shouldOnlyAllowOneSchemaSource(String k1, String k2) {
    try {
      new DatagenConnectorConfig(ImmutableMap.<String, String>builder()
          .putAll(COMMON_PROPERTIES)
          .put(k1, SOURCES.get(k1))
          .put(k2, SOURCES.get(k2))
          .build()
      );
      fail("Expected config exception due to redundant schema sources");
    } catch (ConfigException e) {
      assertEquals(
          "Cannot set schema.string with quickstart or schema.filename.",
          e.getMessage()
      );
    }
  }
}