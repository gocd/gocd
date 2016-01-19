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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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

    public void createAgent(String autoRegisterKey, Collection<String> resources, String environment) {
        PluginDescriptor plugin = findPluginMatching(resources, environment);
        if (plugin != null) {
            LOGGER.debug("Processing create agent for plugin: {} with resources: [{}] and environment: {}", plugin.id(), resources, environment);
            elasticAgentExtension.createAgent(autoRegisterKey, plugin.id(), resources, environment);
            LOGGER.debug("Done processing create agent for plugin: {} with resources: [{}] and environment: {}", plugin.id(), resources, environment);
        } else {
            LOGGER.warn("Could not find plugin matching resources: [{}] and environment: {}", resources, environment);
        }
    }

    public void serverPing(String pluginId, Collection<AgentMetadata> agents) {
        LOGGER.debug("Processing server ping {} [{}]", pluginId, agents);
        elasticAgentExtension.serverPing(pluginId, agents);
        LOGGER.debug("Done processing server ping {} [{}]", pluginId, agents);
    }

    public boolean shouldAssignWork(PluginDescriptor plugin, AgentMetadata agent, Collection<String> resources, String environment) {
        LOGGER.debug("Processing should assign work for plugin: {} with agent: {} resources: [{}] and environment: {}", plugin.id(), agent, resources, environment);
        boolean result = elasticAgentExtension.shouldAssignWork(plugin.id(), agent, resources, environment);
        LOGGER.debug("Done processing should assign work (result: {}) for plugin: {} with agent: {} resources: [{}] and environment: {}", result, plugin.id(), agent, resources, environment);
        return result;
    }

    public void notifyAgentBusy(PluginDescriptor plugin, AgentMetadata agent) {
        LOGGER.debug("Processing notify agent busy for plugin: {} with agent: {}", plugin.id(), agent);
        elasticAgentExtension.notifyAgentBusy(plugin.id(), agent);
        LOGGER.debug("Done processing notify agent busy for plugin: {} with agent: {}", plugin.id(), agent);
    }

    public void notifyAgentIdle(PluginDescriptor plugin, AgentMetadata agent) {
        LOGGER.debug("Processing notify agent idle for plugin: {} with agent: {}", plugin.id(), agent);
        elasticAgentExtension.notifyAgentIdle(plugin.id(), agent);
        LOGGER.debug("Done processing notify agent idle for plugin: {} with agent: {}", plugin.id(), agent);
    }

    private PluginDescriptor findPluginMatching(Collection<String> resources, String environment) {
        for (PluginDescriptor pluginDescriptor : plugins) {
            if (elasticAgentExtension.canPluginHandle(pluginDescriptor.id(), resources, environment)) {
                return pluginDescriptor;
            }
        }
        return null;
    }
}
