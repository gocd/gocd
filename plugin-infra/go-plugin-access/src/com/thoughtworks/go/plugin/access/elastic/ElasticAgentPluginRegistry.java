/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import com.thoughtworks.go.plugin.access.common.AbstractPluginRegistry;
import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ElasticAgentPluginRegistry extends AbstractPluginRegistry<ElasticAgentExtension> {

    @Autowired
    public ElasticAgentPluginRegistry(PluginManager pluginManager, ElasticAgentExtension elasticAgentExtension) {
        super(pluginManager, elasticAgentExtension);
    }

    public void createAgent(final String pluginId, String autoRegisterKey, String environment, Map<String, String> configuration, JobIdentifier jobIdentifier) {
        PluginDescriptor plugin = findPlugin(pluginId);
        if (plugin != null) {
            LOGGER.debug("Processing create agent for plugin: {} with environment: {} with configuration: {}", pluginId, environment, configuration);
            extension.createAgent(pluginId, autoRegisterKey, environment, configuration, jobIdentifier);
            LOGGER.debug("Done processing create agent for plugin: {} with environment: {} with configuration: {}", pluginId, environment, configuration);
        } else {
            LOGGER.warn("Could not find plugin with id: {}", pluginId);
        }
    }

    public void serverPing(String pluginId) {
        LOGGER.debug("Processing server ping for plugin {}", pluginId);
        extension.serverPing(pluginId);
        LOGGER.debug("Done processing server ping for plugin {}", pluginId);
    }

    public boolean shouldAssignWork(PluginDescriptor plugin, AgentMetadata agent, String environment, Map<String, String> configuration, JobIdentifier identifier) {
        LOGGER.debug("Processing should assign work for plugin: {} with agent: {} with environment: {} with configuration: {}", plugin.id(), agent, environment, configuration);
        boolean result = extension.shouldAssignWork(plugin.id(), agent, environment, configuration, identifier);
        LOGGER.debug("Done processing should assign work (result: {}) for plugin: {} with agent: {} with environment: {} with configuration {}", result, plugin.id(), agent, environment, configuration);
        return result;
    }

    public boolean has(String pluginId) {
        return findPlugin(pluginId) != null;
    }
}
