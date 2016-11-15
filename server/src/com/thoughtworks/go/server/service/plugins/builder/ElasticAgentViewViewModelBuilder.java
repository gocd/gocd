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

package com.thoughtworks.go.server.service.plugins.builder;

import com.thoughtworks.go.plugin.access.common.settings.Image;
import com.thoughtworks.go.plugin.access.elastic.Constants;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginRegistry;
import com.thoughtworks.go.plugin.api.config.Configuration;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.server.ui.plugins.PluggableInstanceSettings;
import com.thoughtworks.go.server.ui.plugins.PluginConfiguration;
import com.thoughtworks.go.server.ui.plugins.PluginInfo;
import com.thoughtworks.go.server.ui.plugins.PluginView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ElasticAgentViewViewModelBuilder implements ViewModelBuilder {
    private final ElasticAgentPluginRegistry registry;

    ElasticAgentViewViewModelBuilder(ElasticAgentPluginRegistry registry) {
        this.registry = registry;
    }

    @Override
    public List<PluginInfo> allPluginInfos() {
        List<PluginInfo> pluginInfos = new ArrayList<>();

        for (PluginDescriptor descriptor : registry.getPlugins()) {
            Image icon = registry.getIcon(descriptor.id());
            pluginInfos.add(new PluginInfo(descriptor, Constants.EXTENSION_NAME, null, null, icon));
        }

        return pluginInfos;
    }

    @Override
    public PluginInfo pluginInfoFor(String pluginId) {
        PluginDescriptor plugin = registry.findPlugin(pluginId);

        if (plugin == null) {
            return null;
        }

        Image icon = registry.getIcon(pluginId);

        ArrayList<PluginConfiguration> pluginConfigurations = getPluginConfigurations(registry.getProfileMetadata(pluginId));

        PluginView pluginView = new PluginView(registry.getProfileView(pluginId));

        PluggableInstanceSettings settings = new PluggableInstanceSettings(pluginConfigurations, pluginView);
        return new PluginInfo(plugin, Constants.EXTENSION_NAME, null, settings, icon);
    }

    private ArrayList<PluginConfiguration> getPluginConfigurations(Configuration config) {
        ArrayList<PluginConfiguration> pluginConfigurations = new ArrayList<>();
        for (Property property : config.list()) {
            Map<String, Object> metaData = new HashMap<>();
            metaData.put(REQUIRED_OPTION, property.getOption(Property.REQUIRED));
            metaData.put(SECURE_OPTION, property.getOption(Property.SECURE));

            pluginConfigurations.add(new PluginConfiguration(property.getKey(), metaData));
        }
        return pluginConfigurations;
    }
}
