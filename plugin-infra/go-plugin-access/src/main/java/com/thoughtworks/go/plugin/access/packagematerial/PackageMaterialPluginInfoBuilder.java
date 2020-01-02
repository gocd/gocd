/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.access.packagematerial;

import com.thoughtworks.go.plugin.access.common.PluginInfoBuilder;
import com.thoughtworks.go.plugin.api.config.Configuration;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PackageMaterialMetadata;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.packagematerial.PackageMaterialPluginInfo;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Component
public class PackageMaterialPluginInfoBuilder implements PluginInfoBuilder<PackageMaterialPluginInfo> {

    private PackageRepositoryExtension extension;

    @Autowired
    public PackageMaterialPluginInfoBuilder(PackageRepositoryExtension extension) {
        this.extension = extension;
    }

    @Override
    public PackageMaterialPluginInfo pluginInfoFor(GoPluginDescriptor descriptor) {
        RepositoryConfiguration repositoryConfiguration = extension.getRepositoryConfiguration(descriptor.id());
        com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration = extension.getPackageConfiguration(descriptor.id());

        if (repositoryConfiguration == null) {
            throw new RuntimeException(format("Plugin[%s] returned null repository configuration", descriptor.id()));
        }
        if (packageConfiguration == null) {
            throw new RuntimeException(format("Plugin[%s] returned null package configuration", descriptor.id()));
        }
        PluggableInstanceSettings pluginSettingsAndView = getPluginSettingsAndView(descriptor, extension);

        return new PackageMaterialPluginInfo(descriptor, new PluggableInstanceSettings(packageRepoConfigurations(repositoryConfiguration)), new PluggableInstanceSettings(packageRepoConfigurations(packageConfiguration)), pluginSettingsAndView);
    }

    private List<PluginConfiguration> packageRepoConfigurations(Configuration repositoryConfiguration) {
        List<? extends Property> list = repositoryConfiguration.list();

        return list.stream().map((Function<Property, PluginConfiguration>) property -> new PluginConfiguration(property.getKey(), toMetadata(property))).collect(Collectors.toList());
    }

    private Metadata toMetadata(Property configuration) {
        return new PackageMaterialMetadata(
                configuration.getOption(Property.REQUIRED),
                configuration.getOption(Property.SECURE),
                configuration.getOption(Property.PART_OF_IDENTITY),
                configuration.getOption(Property.DISPLAY_NAME),
                configuration.getOption(Property.DISPLAY_ORDER)
        );
    }

}

