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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class ElasticAgentPluginRegistry implements PluginChangeListener {

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

    public PluginDescriptor findPluginsMatching(List<String> resources, String environment) {
        for (PluginDescriptor pluginDescriptor : plugins) {
            if (elasticAgentExtension.canPluginHandle(pluginDescriptor.id(), resources, environment)) {
                return pluginDescriptor;
            }
        }
        return null;
    }

    public void createAgent(PluginDescriptor descriptor, List<String> resources, String environment) {
        elasticAgentExtension.createAgent(descriptor.id(), resources, environment);
    }

    public void serverPing(String pluginId, ArrayList<AgentMetadata> metadata) {
        for (PluginDescriptor descriptor : plugins) {
            elasticAgentExtension.serverPing(descriptor.id(), metadata);
        }
    }

    public boolean shouldAssignWork(PluginDescriptor descriptor, String elasticAgentId, List<String> resources, String environment) {
        return elasticAgentExtension.shouldAssignWork(descriptor.id(), elasticAgentId, resources, environment);
    }

    public void notifyJobAssigned(PluginDescriptor descriptor, String elasticAgentId) {
        elasticAgentExtension.notifyAgentBusy(descriptor.id(), elasticAgentId);
    }

    public void notifyAgentIdle(PluginDescriptor descriptor, String elasticAgentId) {
        elasticAgentExtension.notifyAgentIdle(descriptor.id(), elasticAgentId);
    }

}
