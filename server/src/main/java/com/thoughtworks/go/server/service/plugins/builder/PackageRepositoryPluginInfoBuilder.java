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
import com.thoughtworks.go.server.ui.plugins.PackageRepositoryPluginInfo;
import com.thoughtworks.go.server.ui.plugins.PluggableInstanceSettings;
import com.thoughtworks.go.server.ui.plugins.PluginConfiguration;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Deprecated
public class PackageRepositoryPluginInfoBuilder implements NewPluginInfoBuilder<PackageRepositoryPluginInfo> {
    private final RepositoryMetadataStore repositoryMetadataStore;

    private final PluginManager pluginManager;
    private final PackageMetadataStore packageMetadataStore;

    public PackageRepositoryPluginInfoBuilder(PluginManager pluginManager, PackageMetadataStore packageMetadataStore, RepositoryMetadataStore repositoryMetadataStore) {
        this.pluginManager = pluginManager;
        this.packageMetadataStore = packageMetadataStore;
        this.repositoryMetadataStore = repositoryMetadataStore;
    }

    @Override
    public PackageRepositoryPluginInfo pluginInfoFor(String pluginId) {
        if (!packageMetadataStore.getPlugins().contains(pluginId)) {
            return null;
        }

        GoPluginDescriptor plugin = pluginManager.getPluginDescriptorFor(pluginId);

        return new PackageRepositoryPluginInfo(plugin,
                new PluggableInstanceSettings(configurations(packageMetadataStore.getMetadata(pluginId))),
                new PluggableInstanceSettings(configurations(repositoryMetadataStore.getMetadata(pluginId)))
        );
    }

    @Override
    public Collection<PackageRepositoryPluginInfo> allPluginInfos() {
        return packageMetadataStore.getPlugins().stream().map(new Function<String, PackageRepositoryPluginInfo>() {
            @Override
            public PackageRepositoryPluginInfo apply(String pluginId) {
                return PackageRepositoryPluginInfoBuilder.this.pluginInfoFor(pluginId);
            }
        }).collect(Collectors.toList());
    }

    static List<PluginConfiguration> configurations(PackageConfigurations packageConfigurations) {
        ArrayList<PluginConfiguration> pluginConfigurations = new ArrayList<>();

        for (PackageConfiguration configuration : packageConfigurations.list()) {
            Map<String, Object> metaData = new HashMap<>();

            metaData.put("required", configuration.getOption(Property.REQUIRED));
            metaData.put("secure", configuration.getOption(Property.SECURE));
            metaData.put("part_of_identity", configuration.getOption(Property.PART_OF_IDENTITY));
            metaData.put("display_name", configuration.getOption(Property.DISPLAY_NAME));
            metaData.put("display_order", configuration.getOption(Property.DISPLAY_ORDER));

            pluginConfigurations.add(new PluginConfiguration(configuration.getKey(), metaData));
        }
        return pluginConfigurations;
    }


}
