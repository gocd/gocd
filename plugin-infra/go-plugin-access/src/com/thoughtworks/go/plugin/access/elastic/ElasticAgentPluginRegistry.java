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

import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.infra.PluginChangeListener;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.ListUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class ElasticAgentPluginRegistry implements PluginChangeListener {

    private static Logger LOGGER = LoggerFactory.getLogger(ElasticAgentPluginRegistry.class);

    private final ElasticAgentExtension elasticAgentExtension;
    private final List<PluginDescriptor> plugins;

    @Autowired
    public ElasticAgentPluginRegistry(PluginManager pluginManager, ElasticAgentExtension elasticAgentExtension) {
        this.elasticAgentExtension = elasticAgentExtension;
        this.plugins = new ArrayList<>();
        pluginManager.addPluginChangeListener(this, GoPlugin.class);
    }

    @Override
    public void pluginLoaded(GoPluginDescriptor pluginDescriptor) {
        if (elasticAgentExtension.canHandlePlugin(pluginDescriptor.id())) {
            this.plugins.add(pluginDescriptor);
        }
    }

    @Override
    public void pluginUnLoaded(GoPluginDescriptor pluginDescriptor) {
        if (elasticAgentExtension.canHandlePlugin(pluginDescriptor.id())) {
            this.plugins.remove(pluginDescriptor);
        }
    }

    public List<PluginDescriptor> getPlugins() {
        return Collections.unmodifiableList(plugins);
    }

    public void createAgent(final String pluginId, String autoRegisterKey, String environment, Map<String, String> configuration) {
        PluginDescriptor plugin = findPlugin(pluginId);
        if (plugin != null) {
            LOGGER.debug("Processing create agent for plugin: {} with environment: {} with configuration: {}", pluginId, environment, configuration);
            elasticAgentExtension.createAgent(pluginId, autoRegisterKey, environment, configuration);
            LOGGER.debug("Done processing create agent for plugin: {} with environment: {} with configuration: {}", pluginId, environment, configuration);
        } else {
            LOGGER.warn("Could not find plugin with id: {}", pluginId);
        }
    }

    public void serverPing(String pluginId) {
        LOGGER.debug("Processing server ping for plugin {}", pluginId);
        elasticAgentExtension.serverPing(pluginId);
        LOGGER.debug("Done processing server ping for plugin {}", pluginId);
    }

    public boolean shouldAssignWork(PluginDescriptor plugin, AgentMetadata agent, String environment, Map<String, String> configuration) {
        LOGGER.debug("Processing should assign work for plugin: {} with agent: {} with environment: {} with configuration: {}", plugin.id(), agent, environment, configuration);
        boolean result = elasticAgentExtension.shouldAssignWork(plugin.id(), agent, environment, configuration);
        LOGGER.debug("Done processing should assign work (result: {}) for plugin: {} with agent: {} with environment: {} with configuration {}", result, plugin.id(), agent, environment, configuration);
        return result;
    }

    public PluginDescriptor findPlugin(final String pluginId) {
        return ListUtil.find(plugins, new ListUtil.Condition() {
            @Override
            public <T> boolean isMet(T item) {
                PluginDescriptor pluginDescriptor = (PluginDescriptor) item;
                return pluginDescriptor.id().equals(pluginId);
            }
        });
    }
}
