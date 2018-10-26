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


import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.types.Password;

import java.util.List;
import java.util.Map;


public class DatagenConnectorConfig extends AbstractConfig {

  public static final String IRC_SERVER_CONF = "irc.server";
  private static final String IRC_SERVER_DOC = "The IRC Server to connect to.";

  public DatagenConnectorConfig(ConfigDef config, Map<String, String> parsedConfig) {
    super(config, parsedConfig);
  }

  public DatagenConnectorConfig(Map<String, String> parsedConfig) {
    this(conf(), parsedConfig);
  }

  public static ConfigDef conf() {
    return new ConfigDef()
        .define(IRC_SERVER_CONF, Type.STRING, Importance.HIGH, IRC_SERVER_DOC);
  }

  public String getIrcServer() { return this.getString(IRC_SERVER_CONF); }

}

