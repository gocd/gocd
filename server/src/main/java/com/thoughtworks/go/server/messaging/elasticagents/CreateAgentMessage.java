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
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.server.messaging.PluginAwareMessage;

import java.util.Collections;
import java.util.Map;

public class CreateAgentMessage implements PluginAwareMessage {
    private final String autoregisterKey;
    private final String environment;
    private final Map<String, String> configuration;
    private Map<String, String> clusterProfile;
    private final JobIdentifier jobIdentifier;
    private final String pluginId;

    public CreateAgentMessage(String autoregisterKey, String environment, ElasticProfile elasticProfile, ClusterProfile clusterProfile, JobIdentifier jobIdentifier) {
        this.autoregisterKey = autoregisterKey;
        this.environment = environment;
        this.pluginId = clusterProfile.getPluginId();
        this.configuration = elasticProfile.getConfigurationAsMap(true);
        this.clusterProfile = clusterProfile.getConfigurationAsMap(true);
        this.jobIdentifier = jobIdentifier;
    }

    public String autoregisterKey() {
        return autoregisterKey;
    }

    public String environment() {
        return environment;
    }

    @Override
    public String toString() {
        return "CreateAgentMessage{" +
                "autoregisterKey='" + autoregisterKey + '\'' +
                ", environment='" + environment + '\'' +
                ", configuration=" + configuration +
                ", clusterProfile=" + clusterProfile +
                ", jobIdentifier=" + jobIdentifier +
                ", pluginId='" + pluginId + '\'' +
                '}';
    }

    public String pluginId() {
        return pluginId;
    }

    public Map<String, String> configuration() {
        return configuration;
    }

    public JobIdentifier jobIdentifier() {
        return jobIdentifier;
    }

    public Map<String, String> getClusterProfileConfiguration() {
        return clusterProfile;
    }
}
