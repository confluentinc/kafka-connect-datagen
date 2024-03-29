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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.avro.Schema;
import org.apache.kafka.common.config.Config;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.config.ConfigValue;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatagenConnector extends SourceConnector {

  private static Logger log = LoggerFactory.getLogger(DatagenConnector.class);
  private DatagenConnectorConfig config;
  private Map<String, String> props;

  @Override
  public String version() {
    return VersionUtil.getVersion();
  }

  @Override
  public void start(Map<String, String> props) {
    try {
      this.props = props;
      config = new DatagenConnectorConfig(props);
    } catch (ConfigException e) {
      throw new ConfigException(
          "Datagen connector could not start because of an error in the configuration: ",
          e
      );
    }
  }

  @Override
  public Class<? extends Task> taskClass() {
    return DatagenTask.class;
  }

  @Override
  public List<Map<String, String>> taskConfigs(int maxTasks) {
    List<Map<String, String>> taskConfigs = new ArrayList<>();
    for (int i = 0; i < maxTasks; i++) {
      Map<String, String> taskConfig = new HashMap<>(this.props);
      taskConfig.put(DatagenTask.TASK_ID, Integer.toString(i));
      taskConfigs.add(taskConfig);
    }
    return taskConfigs;
  }

  @Override
  public void stop() {
  }

  @Override
  public ConfigDef config() {
    return DatagenConnectorConfig.conf();
  }

  @Override
  public Config validate(Map<String, String> connectorConfigs) {
    Config config = super.validate(connectorConfigs);
    boolean isSingleSchemaSource = validateSchemaSource(config);

    // skip further validations if any single config validations have failed
    try {
      this.config = new DatagenConnectorConfig(connectorConfigs);
    } catch (ConfigException e) {
      return config;
    }

    if (isSingleSchemaSource) {
      validateSchemaKeyField(config, this.config.getSchema());
    }
    return config;
  }

  private boolean validateSchemaSource(Config config) {
    List<ConfigValue> schemaSources = config.configValues().stream()
        .filter(v -> DatagenConnectorConfig.isExplicitlySetSchemaSource(v.name(), v.value()))
        .collect(Collectors.toList());
    String schemaSourceError = "Must set exactly one of "
            + String.join(", ", DatagenConnectorConfig.schemaSourceKeys());
    if (schemaSources.size() > 1) {
      for (ConfigValue v : schemaSources) {
        v.addErrorMessage(schemaSourceError);
      }
      return false;
    }
    if (schemaSources.size() == 0) {
      config.configValues().stream()
          .filter(v -> DatagenConnectorConfig.schemaSourceKeys().contains(v.name()))
          .forEach(v -> v.addErrorMessage(schemaSourceError));
      return false;
    }
    return true;
  }

  private void validateSchemaKeyField(Config config, Schema schema) {
    ConfigValue schemaKeyField = getConfigValue(
        config,
        DatagenConnectorConfig.SCHEMA_KEYFIELD_CONF
    );

    if (schemaKeyField != null && !schemaKeyField.value().equals("")) {
      if (schema.getField((String) schemaKeyField.value()) == null) {
        schemaKeyField.addErrorMessage(
            "The schema does not contain the field provided in '"
              + DatagenConnectorConfig.SCHEMA_KEYFIELD_CONF + "'"
        );
      }
    }
  }

  private ConfigValue getConfigValue(Config config, String configName) {
    return config.configValues().stream()
            .filter(value -> value.name().equals(configName))
            .findFirst().orElse(null);
  }
}
