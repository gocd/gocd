/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentAgentsConfigTest {
    private EnvironmentAgentsConfig envAgentsConfig = new EnvironmentAgentsConfig();

    @Test
    void shouldGetAllAgentUUIDs() {
        EnvironmentAgentConfig envAgentConfig1 = new EnvironmentAgentConfig("uuid1");
        EnvironmentAgentConfig envAgentConfig2 = new EnvironmentAgentConfig("uuid2");
        EnvironmentAgentConfig envAgentConfig3 = new EnvironmentAgentConfig("uuid3");
        envAgentsConfig.addAll(List.of(envAgentConfig1, envAgentConfig2, envAgentConfig3));

        List<String> uuids = envAgentsConfig.getUuids();

        assertThat(uuids.size()).isEqualTo(3);
        assertThat(uuids.containsAll(List.of("uuid1", "uuid2", "uuid3"))).isTrue();
    }

    @Test
    void shouldGetEmptyListOfUUIDsWhenThereAreNoAgentsAssociatedWithEnvironment() {
        List<String> uuids = envAgentsConfig.getUuids();
        assertThat(uuids.size()).isEqualTo(0);
    }

    @Test
    void shouldSetAgentConfigAttributes() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("uuid", "agent-1");

        Map<String, String> map2 = new HashMap<>();
        map2.put("uuid", "agent-2");

        List<Map<String, String>> mapList = new ArrayList<>(List.of(map1, map2));
        envAgentsConfig.setConfigAttributes(mapList);

        assertThat(envAgentsConfig.size()).isEqualTo(2);
        assertThat(envAgentsConfig.getUuids()).isEqualTo(List.of("agent-1", "agent-2"));
    }

    @Test
    void shouldNotSetAgentConfigAttributesWhenItIsNull() {
        envAgentsConfig.setConfigAttributes(null);
        assertThat(envAgentsConfig.size()).isEqualTo(0);
    }
}