/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class CreateAgentMessageTest {
    @Test
    public void shouldGetPluginId() {
        List<ConfigurationProperty> properties = Arrays.asList(new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue("value")));
        ElasticProfile elasticProfile = new ElasticProfile("foo", "prod-cluster", properties);
        ClusterProfile clusterProfile = new ClusterProfile("foo", "plugin-id", properties);
        CreateAgentMessage message = new CreateAgentMessage("key", "env", elasticProfile, clusterProfile, null);
        assertThat(message.pluginId(), is(clusterProfile.getPluginId()));
        Map<String, String> configurationAsMap = elasticProfile.getConfigurationAsMap(true);
        assertThat(message.configuration(), is(configurationAsMap));
        Map<String, String> clusterProfileConfigurations = clusterProfile.getConfigurationAsMap(true);
        assertThat(message.getClusterProfileConfiguration(), is(clusterProfileConfigurations));
    }

    @Test
    public void shouldCreateCreateAgentMessageWhenClusterProfileIsNotSpecified() {
        List<ConfigurationProperty> properties = Arrays.asList(new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue("value")));
        ElasticProfile elasticProfile = new ElasticProfile("foo", "prod-cluster", properties);

        CreateAgentMessage createAgentMessage = new CreateAgentMessage(null, null, elasticProfile, null, null);
        assertThat(createAgentMessage.getClusterProfileConfiguration(), is(Collections.emptyMap()));
    }
}
