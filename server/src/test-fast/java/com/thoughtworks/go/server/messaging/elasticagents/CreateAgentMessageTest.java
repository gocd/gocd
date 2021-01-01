/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.messaging.elasticagents;

import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class CreateAgentMessageTest {
    @Test
    void shouldGetPluginId() {
        List<ConfigurationProperty> properties = singletonList(new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue("value")));
        ElasticProfile elasticProfile = new ElasticProfile("foo", "prod-cluster", properties);
        ClusterProfile clusterProfile = new ClusterProfile("foo", "plugin-id", properties);

        Map<String, String> clusterProfileConfigurations = clusterProfile.getConfigurationAsMap(true);
        Map<String, String> configurationAsMap = elasticProfile.getConfigurationAsMap(true);
        CreateAgentMessage message = new CreateAgentMessage("key", "env", elasticProfile, clusterProfile, null);

        assertThat(message.pluginId()).isEqualTo(clusterProfile.getPluginId());
        assertThat(message.getClusterProfileConfiguration()).isEqualTo(clusterProfileConfigurations);
        assertThat(message.configuration()).isEqualTo(configurationAsMap);
    }

    @Test
    void shouldReturnResolvedValues() {
        ConfigurationProperty k1 = ConfigurationPropertyMother.create("key", "value");
        ConfigurationProperty k2 = ConfigurationPropertyMother.create("key1", false, "{{SECRET:[config_id][lookup_key]}}");
        k2.getSecretParams().get(0).setValue("some-resolved-value");
        ElasticProfile elasticProfile = new ElasticProfile("foo", "prod-cluster", k1, k2);
        ClusterProfile clusterProfile = new ClusterProfile("foo", "plugin-id", k1, k2);

        Map<String, String> clusterProfileConfigurations = clusterProfile.getConfigurationAsMap(true, true);
        Map<String, String> configurationAsMap = elasticProfile.getConfigurationAsMap(true, true);
        CreateAgentMessage message = new CreateAgentMessage("key", "env", elasticProfile, clusterProfile, null);

        assertThat(message.pluginId()).isEqualTo(clusterProfile.getPluginId());
        assertThat(message.getClusterProfileConfiguration()).isEqualTo(clusterProfileConfigurations);
        assertThat(message.configuration()).isEqualTo(configurationAsMap);
    }
}
