/**
 * Copyright 2026 Confluent Inc.
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import org.apache.kafka.common.config.ConfigException;
import org.junit.jupiter.api.Test;

class ConfigUtilsTest {

  private static final long MAX = ConfigUtils.MAX_GENERATED_LENGTH;

  private static String record(String fieldType) {
    return "{\"type\":\"record\",\"name\":\"r\",\"fields\":[{\"name\":\"f\",\"type\":"
        + fieldType + "}]}";
  }

  private static void assertRejected(String schema) {
    assertThrows(ConfigException.class, () -> ConfigUtils.getSchemaFromSchemaString(schema));
  }

  private static void assertAccepted(String schema) {
    assertDoesNotThrow(() -> ConfigUtils.getSchemaFromSchemaString(schema));
  }

  @Test
  void shouldAcceptLengthAtCeiling() {
    assertAccepted(record("{\"type\":\"bytes\",\"arg.properties\":{\"length\":" + MAX + "}}"));
  }

  @Test
  void shouldRejectLengthJustAboveCeiling() {
    assertRejected(record(
        "{\"type\":\"bytes\",\"arg.properties\":{\"length\":" + (MAX + 1) + "}}"));
  }

  @Test
  void shouldRejectOversizedFixedLengthOnString() {
    assertRejected(record(
        "{\"type\":\"string\",\"arg.properties\":{\"length\":2000000000}}"));
  }

  @Test
  void shouldRejectOversizedMinOnly() {
    assertRejected(record(
        "{\"type\":\"bytes\",\"arg.properties\":{\"length\":{\"min\":2000000000}}}"));
  }

  @Test
  void shouldRejectOversizedMaxOnly() {
    assertRejected(record(
        "{\"type\":\"bytes\",\"arg.properties\":{\"length\":{\"max\":2000000000}}}"));
  }

  @Test
  void shouldRejectLengthBeyondIntRange() {
    assertRejected(record(
        "{\"type\":\"bytes\",\"arg.properties\":{\"length\":{\"min\":9999999999999}}}"));
  }

  @Test
  void shouldRejectOversizedLengthOnMapValues() {
    assertRejected(record(
        "{\"type\":\"map\",\"values\":\"string\",\"arg.properties\":"
            + "{\"length\":{\"min\":2000000000}}}"));
  }

  @Test
  void shouldRejectOversizedMapKeysLengthBounds() {
    assertRejected(record(
        "{\"type\":\"map\",\"values\":\"string\",\"arg.properties\":"
            + "{\"keys\":{\"length\":{\"min\":2000000000}}}}"));
  }

  @Test
  void shouldRejectOversizedLengthInArrayItems() {
    assertRejected(record(
        "{\"type\":\"array\",\"items\":{\"type\":\"bytes\",\"arg.properties\":"
            + "{\"length\":2000000000}}}"));
  }

  @Test
  void shouldRejectOversizedLengthInMapValueSchema() {
    assertRejected(record(
        "{\"type\":\"map\",\"values\":{\"type\":\"string\",\"arg.properties\":"
            + "{\"length\":2000000000}}}"));
  }

  @Test
  void shouldRejectOversizedLengthInUnionBranch() {
    assertRejected(record(
        "[\"null\",{\"type\":\"bytes\",\"arg.properties\":{\"length\":2000000000}}]"));
  }

  @Test
  void shouldRejectOversizedLengthInNestedRecord() {
    assertRejected(record(
        "{\"type\":\"record\",\"name\":\"inner\",\"fields\":[{\"name\":\"g\",\"type\":"
            + "{\"type\":\"bytes\",\"arg.properties\":{\"length\":2000000000}}}]}"));
  }

  @Test
  void shouldRejectOversizedLengthOnRecordItself() {
    assertRejected("{\"type\":\"record\",\"name\":\"r\",\"arg.properties\":"
        + "{\"length\":2000000000},\"fields\":[{\"name\":\"f\",\"type\":\"string\"}]}");
  }

  @Test
  void shouldTerminateOnRecursiveSchema() {
    assertAccepted("{\"type\":\"record\",\"name\":\"node\",\"fields\":["
        + "{\"name\":\"next\",\"type\":[\"null\",\"node\"]},"
        + "{\"name\":\"v\",\"type\":{\"type\":\"bytes\",\"arg.properties\":{\"length\":10}}}]}");
  }

  @Test
  void shouldRejectOversizedLengthInRecursiveSchema() {
    assertRejected("{\"type\":\"record\",\"name\":\"node\",\"fields\":["
        + "{\"name\":\"next\",\"type\":[\"null\",\"node\"]},"
        + "{\"name\":\"v\",\"type\":{\"type\":\"bytes\",\"arg.properties\":"
        + "{\"length\":2000000000}}}]}");
  }

  @Test
  void shouldAcceptSchemaWithoutArgProperties() {
    assertAccepted(record("\"string\""));
  }

  @Test
  void shouldAcceptNonLengthArgProperties() {
    assertAccepted(record("{\"type\":\"string\",\"arg.properties\":{\"regex\":\"[a-z]+\"}}"));
  }

  @Test
  void shouldAcceptSmallMinMaxBounds() {
    assertAccepted(record(
        "{\"type\":\"bytes\",\"arg.properties\":{\"length\":{\"min\":0,\"max\":1000}}}"));
  }

  @Test
  void shouldAcceptAllBundledSchemaFiles() throws Exception {
    File[] files = new File("src/main/resources").listFiles((d, n) -> n.endsWith(".avro"));
    assertNotNull(files);
    for (File file : files) {
      assertDoesNotThrow(() -> ConfigUtils.getSchemaFromSchemaFileName(file.getName()),
          file.getName());
    }
  }
}
