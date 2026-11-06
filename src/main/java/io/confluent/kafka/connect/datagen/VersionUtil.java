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

import java.io.InputStream;
import java.util.Properties;

class VersionUtil {
  // Filtered by Maven at build time to ${connector.plugin.version}.
  private static final String VERSION_RESOURCE =
      "/io/confluent/kafka/connect/datagen/version.properties";
  private static final String VERSION = loadVersion();

  public static String getVersion() {
    return VERSION;
  }

  private static String loadVersion() {
    try (InputStream in = VersionUtil.class.getResourceAsStream(VERSION_RESOURCE)) {
      if (in != null) {
        Properties props = new Properties();
        props.load(in);
        String version = props.getProperty("version");
        if (version != null && !version.isEmpty() && !version.startsWith("${")) {
          return version;
        }
      }
      return VersionUtil.class.getPackage().getImplementationVersion();
    } catch (Exception ex) {
      return "0.0.0.0";
    }
  }
}
