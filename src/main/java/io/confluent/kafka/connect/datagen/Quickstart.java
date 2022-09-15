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
  USERS_("users_array_map_schema.avro", "userid"),
  PAGEVIEWS("pageviews_schema.avro", "viewtime"),
  STOCK_TRADES("stock_trades_schema.avro", "symbol"),
  INVENTORY("inventory.avro", "id"),
  PRODUCT("product.avro", "id"),
  PURCHASES("purchase.avro", "id"),
  TRANSACTIONS("transactions.avro", "transaction_id"),
  STORES("stores.avro", "store_id"),
  CREDIT_CARDS("credit_cards.avro", "card_id"),
  CAMPAIGN_FINANCE("campaign_finance.avro", "candidate_id"),
  FLEET_MGMT_DESCRIPTION("fleet_mgmt_description.avro", "vehicle_id"),
  FLEET_MGMT_LOCATION("fleet_mgmt_location.avro", "vehicle_id"),
  FLEET_MGMT_SENSORS("fleet_mgmt_sensors.avro", "vehicle_id"),
  PIZZA_ORDERS("pizza_orders.avro", "store_id"),
  PIZZA_ORDERS_COMPLETED("pizza_orders_completed.avro", "store_id"),
  PIZZA_ORDERS_CANCELLED("pizza_orders_cancelled.avro", "store_id"),
  INSURANCE_OFFERS("insurance_offers.avro", "offer_id"),
  INSURANCE_CUSTOMERS("insurance_customers.avro", "customer_id"),
  INSURANCE_CUSTOMER_ACTIVITY("insurance_customer_activity.avro", "activity_id"),
  GAMING_GAMES("gaming_games.avro", "id"),
  GAMING_PLAYERS("gaming_players.avro", "player_id"),
  GAMING_PLAYER_ACTIVITY("gaming_player_activity.avro", "player_id"),
  PAYROLL_EMPLOYEE("payroll_employee.avro", "employee_id"),
  PAYROLL_EMPLOYEE_LOCATION("payroll_employee_location.avro", "employee_id"),
  PAYROLL_BONUS("payroll_bonus.avro", "employee_id"),
  SYSLOG_LOGS("syslog_logs.avro", "host"),
  DEVICE_INFORMATION("device_information.avro", "device_ip"),
  SIEM_LOGS("siem_logs.avro", "hostname"),
  SHOES("shoes.avro", "product_id"),
  SHOE_CUSTOMERS("shoe_customers.avro", "id"),
  SHOE_ORDERS("shoe_orders.avro", "order_id"),
  SHOE_CLICKSTREAM("shoe_clickstream.avro", "product_id");

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
