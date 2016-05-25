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

import com.thoughtworks.go.plugin.access.pluggabletask.JsonBasedTaskExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.ui.plugins.PluginConfiguration;
import com.thoughtworks.go.server.ui.plugins.PluggableInstanceSettings;
import com.thoughtworks.go.server.ui.plugins.PluginInfo;
import com.thoughtworks.go.server.ui.plugins.PluginView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class PluggableTaskViewModelBuilder implements ViewModelBuilder {
    private PluginManager pluginManager;

    public PluggableTaskViewModelBuilder(PluginManager manager) {
        this.pluginManager = manager;
    }

    public List<PluginInfo> allPluginInfos() {
        List<PluginInfo> pluginInfos = new ArrayList<>();

        for(String pluginId : PluggableTaskConfigStore.store().pluginIds()) {
            GoPluginDescriptor descriptor = pluginManager.getPluginDescriptorFor(pluginId);

            TaskPreference taskPreference = PluggableTaskConfigStore.store().preferenceFor(pluginId);

            pluginInfos.add(new PluginInfo(pluginId, descriptor.about().name(), descriptor.about().version(),
                    JsonBasedTaskExtension.TASK_EXTENSION, taskPreference.getView().displayValue()));
        }
        return pluginInfos;
    }

    public PluginInfo pluginInfoFor(String pluginId) {
        if(!PluggableTaskConfigStore.store().hasPreferenceFor(pluginId)) {
            return null;
        }

        GoPluginDescriptor descriptor = pluginManager.getPluginDescriptorFor(pluginId);
        TaskPreference taskPreference = PluggableTaskConfigStore.store().preferenceFor(pluginId);

        List<PluginConfiguration> pluginConfigurations = configurations(taskPreference.getConfig());
        PluginView pluginView = new PluginView(taskPreference.getView().template());

        return new PluginInfo(pluginId, descriptor.about().name(), descriptor.about().version(), JsonBasedTaskExtension.TASK_EXTENSION,
                taskPreference.getView().displayValue(), new PluggableInstanceSettings(pluginConfigurations, pluginView));
    }

    private List<PluginConfiguration> configurations(TaskConfig config) {
        ArrayList<PluginConfiguration> pluginConfigurations = new ArrayList<>();

        for(Property property: config.list()) {
            Map<String, Object> metaData = new HashMap<>();
            metaData.put(REQUIRED_OPTION, property.getOption(Property.REQUIRED));
            metaData.put(SECURE_OPTION, property.getOption(Property.SECURE));

            pluginConfigurations.add(new PluginConfiguration(property.getKey(), metaData));
        }
        return pluginConfigurations;
    }
}
