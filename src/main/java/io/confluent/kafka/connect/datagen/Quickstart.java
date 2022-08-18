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

import com.google.common.collect.ImmutableSet;

import java.util.Arrays;
import java.util.Set;

enum Quickstart {
  CLICKSTREAM_CODES("clickstream_codes_schema.avro", "code"),
  CLICKSTREAM("clickstream_schema.avro", "ip"),
  CLICKSTREAM_USERS("clickstream_users_schema.avro", "user_id"),
  ORDERS("orders_schema.avro", "orderid"),
  RATINGS("ratings_schema.avro", "rating_id"),
  USERS("users_schema.avro", "userid"),
  USERS_ARRAY_MAP("users_array_map_schema.avro", "userid"),
  PAGEVIEWS("pageviews_schema.avro", "viewtime"),
  STOCK_TRADES("stock_trades_schema.avro", "symbol"),
  INVENTORY("inventory.avro", "id"),
  PRODUCT("product.avro", "id"),
  PURCHASES("purchase.avro", "id"),
  TRANSACTIONS("transactions.avro", "transaction_id"),
  STORES("stores.avro", "store_id"),
  CREDIT_CARDS("credit_cards.avro", "card_id"),
  FLEET_MGMT_DESCRIPTION("fleet_mgmt_description.avro", "vehicle_id");

  private static final Set<String> configValues;

  static {
    ImmutableSet.Builder<String> immutableSetBuilder = ImmutableSet.builder();
    Arrays.stream(Quickstart.values())
      .map(Quickstart::name)
      .map(String::toLowerCase)
      .forEach(immutableSetBuilder::add);
    configValues = immutableSetBuilder.build();
  }

  private final String schemaFilename;
  private final String keyName;

  Quickstart(String schemaFilename, String keyName) {
    this.schemaFilename = schemaFilename;
    this.keyName = keyName;
  }

  public static Set<String> configValues() {
    return configValues;
  }

  public String getSchemaFilename() {
    return schemaFilename;
  }

  public String getSchemaKeyField() {
    return keyName;
  }
}
