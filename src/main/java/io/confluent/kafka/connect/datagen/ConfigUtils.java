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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Parser;
import org.apache.avro.SchemaParseException;
import org.apache.kafka.common.config.ConfigException;

public class ConfigUtils {
  public static Schema getSchemaFromQuickstart(String quickstart) {
    String schemaFilename = Quickstart.valueOf(quickstart.toUpperCase()).getSchemaFilename();
    return getSchemaFromSchemaFileName(schemaFilename);
  }

  public static Schema getSchemaFromSchemaString(String schemaString) {
    Schema.Parser schemaParser = new Parser();
    Schema schema;
    try {
      schema = schemaParser.parse(schemaString);
    } catch (SchemaParseException e) {
      throw new ConfigException("Unable to parse the provided schema");
    }
    return schema;
  }

  public static Schema getSchemaFromSchemaFileName(String schemaFileName) {
    Schema.Parser schemaParser = new Parser();
    Schema schema;
    try {
      InputStream stream = new FileInputStream(schemaFileName);
      schema = schemaParser.parse(stream);
    } catch (FileNotFoundException e) {
      try {
        if (DatagenTask.class.getClassLoader()
            .getResource(schemaFileName) == null) {
          throw new ConfigException("Unable to parse the provided schema");
        }
        schema = schemaParser.parse(DatagenTask.class.getClassLoader()
          .getResourceAsStream(schemaFileName));
      } catch (SchemaParseException | IOException i) {
        throw new ConfigException("Unable to parse the provided schema");
      }
    } catch (SchemaParseException | IOException e) {
      throw new ConfigException("Unable to parse the provided schema");
    }
    return schema;
  }
}
