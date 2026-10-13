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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Parser;
import org.apache.kafka.common.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigUtils {
  private static final Logger log = LoggerFactory.getLogger(ConfigUtils.class);

  // avro-random-generator only rejects negative lengths, so arg.properties.length (or its
  // min/max form) could otherwise request arbitrarily large allocations. This ceiling bounds
  // every length-bearing arg.properties value, regardless of schema type.
  static final long MAX_GENERATED_LENGTH = 1_048_576L; // 1 MiB per field

  private static final String ARG_PROPERTIES_PROP = "arg.properties";
  private static final String LENGTH_PROP = "length";
  private static final String LENGTH_PROP_MIN = "min";
  private static final String LENGTH_PROP_MAX = "max";
  private static final String KEYS_PROP = "keys";

  public static Schema getSchemaFromSchemaString(String schemaString) {
    Schema.Parser schemaParser = new Parser();
    Schema schema;
    try {
      schema = schemaParser.parse(schemaString);
    } catch (RuntimeException e) {
      log.error("Unable to parse the provided schema", e);
      throw new ConfigException("Unable to parse the provided schema");
    }
    validateGeneratedLengthBounds(schema);
    return schema;
  }

  public static Schema getSchemaFromSchemaFileName(String schemaFileName) {
    Schema.Parser schemaParser = new Parser();
    Schema schema;
    try (InputStream stream = new FileInputStream(schemaFileName)) {
      schema = schemaParser.parse(stream);
    } catch (FileNotFoundException e) {
      try {
        if (DatagenTask.class.getClassLoader()
            .getResource(schemaFileName) == null) {
          throw new ConfigException("Unable to find the schema file");
        }
        schema = schemaParser.parse(
          DatagenTask.class.getClassLoader().getResourceAsStream(schemaFileName)
        );
      } catch (RuntimeException | IOException i) {
        log.error("Unable to parse the provided schema", i);
        throw new ConfigException("Unable to parse the provided schema");
      }
    } catch (RuntimeException | IOException e) {
      log.error("Unable to parse the provided schema", e);
      throw new ConfigException("Unable to parse the provided schema");
    }
    validateGeneratedLengthBounds(schema);
    return schema;
  }

  /**
   * Walks every node of the schema (record fields, array elements, map values, union
   * branches) and rejects any {@code arg.properties.length} (or {@code .length.min} /
   * {@code .length.max}, including the nested {@code keys.length} used for map keys) that
   * exceeds {@link #MAX_GENERATED_LENGTH}.
   */
  static void validateGeneratedLengthBounds(Schema schema) {
    validateGeneratedLengthBounds(schema, new HashSet<>());
  }

  private static void validateGeneratedLengthBounds(Schema schema, Set<Schema> visited) {
    if (!visited.add(schema)) {
      // Already checked this exact (sub)schema; Avro gives named/recursive types a single
      // shared instance, so this also guards against infinite recursion.
      return;
    }
    checkArgProperties(schema.getObjectProp(ARG_PROPERTIES_PROP));
    switch (schema.getType()) {
      case RECORD:
        for (Schema.Field field : schema.getFields()) {
          validateGeneratedLengthBounds(field.schema(), visited);
        }
        break;
      case ARRAY:
        validateGeneratedLengthBounds(schema.getElementType(), visited);
        break;
      case MAP:
        validateGeneratedLengthBounds(schema.getValueType(), visited);
        break;
      case UNION:
        for (Schema branch : schema.getTypes()) {
          validateGeneratedLengthBounds(branch, visited);
        }
        break;
      default:
        break;
    }
  }

  private static void checkArgProperties(Object argProperties) {
    if (!(argProperties instanceof Map)) {
      return;
    }
    Map<?, ?> props = (Map<?, ?>) argProperties;
    checkLengthProp(props.get(LENGTH_PROP));
    Object keysProp = props.get(KEYS_PROP);
    if (keysProp instanceof Map) {
      checkLengthProp(((Map<?, ?>) keysProp).get(LENGTH_PROP));
    }
  }

  private static void checkLengthProp(Object lengthProp) {
    if (lengthProp instanceof Number) {
      rejectIfTooLarge((Number) lengthProp);
    } else if (lengthProp instanceof Map) {
      Map<?, ?> bounds = (Map<?, ?>) lengthProp;
      rejectIfTooLarge(bounds.get(LENGTH_PROP_MIN));
      rejectIfTooLarge(bounds.get(LENGTH_PROP_MAX));
    }
  }

  private static void rejectIfTooLarge(Object value) {
    if (!(value instanceof Number)) {
      return;
    }
    long length = ((Number) value).longValue();
    if (length > MAX_GENERATED_LENGTH) {
      throw new ConfigException(String.format(
          "schema specifies a generated length of %d, which exceeds the maximum allowed "
              + "value of %d. Lower arg.properties.length (or its min/max bounds) in the "
              + "provided schema.",
          length,
          MAX_GENERATED_LENGTH
      ));
    }
  }
}
