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

import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMetadataStore;
import com.thoughtworks.go.plugin.access.packagematerial.RepositoryMetadataStore;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.ui.plugins.PluggableInstanceSettings;
import com.thoughtworks.go.server.ui.plugins.PluginConfiguration;
import com.thoughtworks.go.server.ui.plugins.PluginInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.PACKAGE_MATERIAL_EXTENSION;

@Deprecated
class PackageViewModelBuilder implements ViewModelBuilder {
    private PluginManager pluginManager;

    public PackageViewModelBuilder(PluginManager manager) {
        this.pluginManager = manager;
    }

    public List<PluginInfo> allPluginInfos() {
        List<PluginInfo> pluginInfos = new ArrayList<>();

        for (String pluginId : PackageMetadataStore.getInstance().pluginIds()) {
            GoPluginDescriptor descriptor = pluginManager.getPluginDescriptorFor(pluginId);

            pluginInfos.add(new PluginInfo(descriptor, PACKAGE_MATERIAL_EXTENSION, null, null, null));
        }
        return pluginInfos;
    }

    public PluginInfo pluginInfoFor(String pluginId) {
        String PACKAGE_CONFIGRATION_TYPE = "package";
        String REPOSITORY_CONFIGRATION_TYPE = "repository";

        if (!PackageMetadataStore.getInstance().hasPreferenceFor(pluginId)) {
            return null;
        }

        GoPluginDescriptor descriptor = pluginManager.getPluginDescriptorFor(pluginId);
        ArrayList<PluginConfiguration> pluginConfigurations = new ArrayList<>();

        pluginConfigurations.addAll(configurations(PackageMetadataStore.getInstance().getMetadata(pluginId), PACKAGE_CONFIGRATION_TYPE));
        pluginConfigurations.addAll(configurations(RepositoryMetadataStore.getInstance().getMetadata(pluginId), REPOSITORY_CONFIGRATION_TYPE));

        return new PluginInfo(descriptor, PACKAGE_MATERIAL_EXTENSION, null, new PluggableInstanceSettings(pluginConfigurations));
    }

    private List<PluginConfiguration> configurations(PackageConfigurations packageConfigurations, String type) {
        ArrayList<PluginConfiguration> pluginConfigurations = new ArrayList<>();

        for (PackageConfiguration configuration : packageConfigurations.list()) {
            Map<String, Object> metaData = new HashMap<>();

            metaData.put(REQUIRED_OPTION, configuration.getOption(Property.REQUIRED));
            metaData.put(SECURE_OPTION, configuration.getOption(Property.SECURE));
            metaData.put(PART_OF_IDENTITY_OPTION, configuration.getOption(Property.PART_OF_IDENTITY));
            metaData.put(DISPLAY_NAME_OPTION, configuration.getOption(Property.DISPLAY_NAME));
            metaData.put(DISPLAY_ORDER_OPTION, configuration.getOption(Property.DISPLAY_ORDER));

            pluginConfigurations.add(new PluginConfiguration(configuration.getKey(), metaData, type));
        }
        return pluginConfigurations;
    }
}
