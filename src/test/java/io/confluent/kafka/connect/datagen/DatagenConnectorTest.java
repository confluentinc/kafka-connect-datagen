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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DatagenConnectorTest {

  private static final String TOPIC = "my-topic";
  private static final int NUM_MESSAGES = 100;
  private static final int MAX_INTERVAL_MS = 0;

  private Map<String, String> config;
  private DatagenConnector connector;

  @Before
  public void setUp() throws Exception {
    config = new HashMap<>();
    config.put(DatagenConnectorConfig.KAFKA_TOPIC_CONF, TOPIC);
    config.put(DatagenConnectorConfig.ITERATIONS_CONF, Integer.toString(NUM_MESSAGES));
    config.put(DatagenConnectorConfig.MAXINTERVAL_CONF, Integer.toString(MAX_INTERVAL_MS));
    config.put(DatagenConnectorConfig.QUICKSTART_CONF, DatagenTask.Quickstart.USERS.name());
    connector = new DatagenConnector();
  }

  @After
  public void tearDown() throws Exception {
    connector.stop();
  }

  @Test
  public void shouldCreateTasks() {
    connector.start(config);

    assertTaskConfigs(1);
    assertTaskConfigs(2);
    assertTaskConfigs(4);
    assertTaskConfigs(10);
    for (int i=0; i!=100; ++i) {
      assertTaskConfigs(0);
    }
  }

  protected void assertTaskConfigs(int maxTasks) {
    List<Map<String, String>> taskConfigs = connector.taskConfigs(maxTasks);
    assertEquals(maxTasks, taskConfigs.size());
    // All task configs should match the connector config
    for (Map<String, String> taskConfig : taskConfigs) {
      assertEquals(config, taskConfig);
    }
  }

}