/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.elastic;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.elastic.Capabilities;

import java.util.List;
import java.util.Map;

public interface VersionedElasticAgentExtension {
    com.thoughtworks.go.plugin.domain.common.Image getIcon(String pluginId);

    Capabilities getCapabilities(String pluginId);

    List<PluginConfiguration> getElasticProfileMetadata(String pluginId);

    String getElasticProfileView(String pluginId);

    ValidationResult validateElasticProfile(String pluginId, Map<String, String> configuration);

    List<PluginConfiguration> getClusterProfileMetadata(String pluginId);

    String getClusterProfileView(String pluginId);

    ValidationResult validateClusterProfile(String pluginId, Map<String, String> configuration);

    void createAgent(String pluginId, String autoRegisterKey, String environment, Map<String, String> configuration, JobIdentifier jobIdentifier);

    void serverPing(String pluginId);

    boolean shouldAssignWork(String pluginId, AgentMetadata agent, String environment, Map<String, String> configuration, JobIdentifier identifier);

    String getPluginStatusReport(String pluginId);

    String getAgentStatusReport(String pluginId, JobIdentifier identifier, String elasticAgentId);

    void jobCompletion(String pluginId, String elasticAgentId, JobIdentifier jobIdentifier);
}
