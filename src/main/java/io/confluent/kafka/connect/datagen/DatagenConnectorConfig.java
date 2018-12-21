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

import java.util.Map;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigDef.Importance;

public class DatagenConnectorConfig extends AbstractConfig {

  public static final String KAFKA_TOPIC_CONF = "kafka.topic";
  private static final String KAFKA_TOPIC_DOC = "Topic to write to";
  public static final String MAXINTERVAL_CONF = "max.interval";
  private static final String MAXINTERVAL_DOC = "Max interval between messages (ms)";
  public static final String ITERATIONS_CONF = "iterations";
  private static final String ITERATIONS_DOC = "Number of messages to send, or less than 1 for "
                                               + "unlimited";
  public static final String SCHEMA_FILENAME_CONF = "schema.filename";
  private static final String SCHEMA_FILENAME_DOC = "Filename of schema to use";
  public static final String SCHEMA_KEYFIELD_CONF = "schema.keyfield";
  private static final String SCHEMA_KEYFIELD_DOC = "Name of field to use as the message key";
  public static final String QUICKSTART_CONF = "quickstart";
  private static final String QUICKSTART_DOC = "Name of quickstart to use";

  public DatagenConnectorConfig(ConfigDef config, Map<String, String> parsedConfig) {
    super(config, parsedConfig);
  }

  public DatagenConnectorConfig(Map<String, String> parsedConfig) {
    this(conf(), parsedConfig);
  }

  public static ConfigDef conf() {
    return new ConfigDef()
        .define(KAFKA_TOPIC_CONF, Type.STRING, Importance.HIGH, KAFKA_TOPIC_DOC)
        .define(MAXINTERVAL_CONF, Type.LONG, 500L, Importance.HIGH, MAXINTERVAL_DOC)
        .define(ITERATIONS_CONF, Type.INT, 1000000, Importance.HIGH, ITERATIONS_DOC)
        .define(SCHEMA_FILENAME_CONF, Type.STRING, "", Importance.HIGH, SCHEMA_FILENAME_DOC)
        .define(SCHEMA_KEYFIELD_CONF, Type.STRING, "", Importance.HIGH, SCHEMA_KEYFIELD_DOC)
        .define(QUICKSTART_CONF, Type.STRING, "", Importance.HIGH, QUICKSTART_DOC);
  }

  public String getKafkaTopic() {
    return this.getString(KAFKA_TOPIC_CONF);
  }

  public Long getMaxInterval() {
    return this.getLong(MAXINTERVAL_CONF);
  }

  public Integer getIterations() {
    return this.getInt(ITERATIONS_CONF);
  }

  public String getSchemaFilename() {
    return this.getString(SCHEMA_FILENAME_CONF);
  }

  public String getSchemaKeyfield() {
    return this.getString(SCHEMA_KEYFIELD_CONF);
  }

  public String getQuickstart() {
    return this.getString(QUICKSTART_CONF);
  }

}

