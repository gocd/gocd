/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.ui.plugins.PluggableInstanceSettings;
import com.thoughtworks.go.server.ui.plugins.PluggableTaskPluginInfo;
import com.thoughtworks.go.server.ui.plugins.PluginConfiguration;
import com.thoughtworks.go.server.ui.plugins.PluginView;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
@Deprecated
public class PluggableTaskPluginInfoBuilder implements NewPluginInfoBuilder<PluggableTaskPluginInfo> {
    private final PluginManager pluginManager;
    private final PluggableTaskConfigStore store;

    public PluggableTaskPluginInfoBuilder(PluginManager pluginManager, PluggableTaskConfigStore store) {
        this.pluginManager = pluginManager;
        this.store = store;
    }

    @Override
    public PluggableTaskPluginInfo pluginInfoFor(String pluginId) {
        if (!store.pluginIds().contains(pluginId)) {
            return null;
        }

        GoPluginDescriptor plugin = pluginManager.getPluginDescriptorFor(pluginId);
        TaskPreference taskPreference = store.preferenceFor(pluginId);

        List<PluginConfiguration> pluginConfigurations = configurations(taskPreference.getConfig());
        PluginView pluginView = new PluginView(taskPreference.getView().template());

        return new PluggableTaskPluginInfo(plugin, taskPreference.getView().displayValue(), new PluggableInstanceSettings(pluginConfigurations, pluginView));
    }

    @Override
    public Collection<PluggableTaskPluginInfo> allPluginInfos() {
        return store.pluginIds().stream().map(new Function<String, PluggableTaskPluginInfo>() {
            @Override
            public PluggableTaskPluginInfo apply(String pluginId) {
                return PluggableTaskPluginInfoBuilder.this.pluginInfoFor(pluginId);
            }
        }).collect(Collectors.toList());
    }

    static List<PluginConfiguration> configurations(TaskConfig config) {
        ArrayList<PluginConfiguration> pluginConfigurations = new ArrayList<>();

        for (Property property : config.list()) {
            Map<String, Object> metaData = new HashMap<>();
            metaData.put("required", property.getOption(Property.REQUIRED));
            metaData.put("secure", property.getOption(Property.SECURE));
            pluginConfigurations.add(new PluginConfiguration(property.getKey(), metaData));
        }
        return pluginConfigurations;
    }

}
