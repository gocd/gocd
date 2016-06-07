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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMetadataStore;
import com.thoughtworks.go.plugin.access.packagematerial.RepositoryMetadataStore;
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference;
import com.thoughtworks.go.plugin.access.scm.SCMConfiguration;
import com.thoughtworks.go.plugin.access.scm.SCMMetadataStore;
import com.thoughtworks.go.plugin.access.scm.SCMPreference;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.ui.PluginViewModel;
import com.thoughtworks.go.server.ui.PluginConfigurationViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public enum PluginBuilder {
    SCM("scm") {
        public PluginViewModel build(String pluginId, GoPluginDescriptor.About about, boolean includeConfigurations) {
            if(!includeConfigurations) {
                return new PluginViewModel(pluginId, about.name(), about.version(), PluginBuilder.SCM.pluginExtension);
            }

            ArrayList<PluginConfigurationViewModel> configurations = new ArrayList<>();
            SCMPreference scmPreference = SCMMetadataStore.getInstance().preferenceFor(pluginId);

            for(SCMConfiguration configuration : scmPreference.getScmConfigurations().list()) {
                Map<String, Object> metaData = new HashMap<>();
                metaData.put("required", configuration.getOption(Property.REQUIRED));
                metaData.put("secure", configuration.getOption(Property.SECURE));
                metaData.put("part_of_identity", configuration.getOption(Property.PART_OF_IDENTITY));

                configurations.add(new PluginConfigurationViewModel(configuration.getKey(), metaData));
            }

            return new PluginViewModel(pluginId, about.name(), about.version(), PluginBuilder.SCM.pluginExtension,
                                       scmPreference.getScmView().template(), configurations);
        }
    },

    TASK("task") {
        public PluginViewModel build(String pluginId, GoPluginDescriptor.About about, boolean includeConfigurations) {
            if(!includeConfigurations) {
                return new PluginViewModel(pluginId, about.name(), about.version(), PluginBuilder.TASK.pluginExtension);
            }

            ArrayList<PluginConfigurationViewModel> configurations = new ArrayList<>();
            TaskPreference taskPreference = PluggableTaskConfigStore.store().preferenceFor(pluginId);

            for(Property property: taskPreference.getConfig().list()) {
                Map<String, Object> metaData = new HashMap<>();
                metaData.put("required", property.getOption(Property.REQUIRED));
                metaData.put("secure", property.getOption(Property.SECURE));

                configurations.add(new PluginConfigurationViewModel(property.getKey(), metaData));
            }
            return new PluginViewModel(pluginId, about.name(), about.version(), PluginBuilder.TASK.pluginExtension,
                                       taskPreference.getView().template(), configurations);
        }
    },

    PACKAGE_REPOSITORY("package-repository") {
        public PluginViewModel build(String pluginId, GoPluginDescriptor.About about, boolean includeConfigurations) {
            if(!includeConfigurations) {
                return new PluginViewModel(pluginId, about.name(), about.version(), PluginBuilder.PACKAGE_REPOSITORY.pluginExtension);
            }

            ArrayList<PluginConfigurationViewModel> configurations = new ArrayList<>();
            PackageConfigurations packageConfigurations = PackageMetadataStore.getInstance().getMetadata(pluginId);
            PackageConfigurations repositoryConfigurations = RepositoryMetadataStore.getInstance().getMetadata(pluginId);

            for(PackageConfiguration configuration: repositoryConfigurations.list()) {
                Map<String, Object> metaData = new HashMap<>();
                metaData.put("required", configuration.getOption(Property.REQUIRED));
                metaData.put("secure", configuration.getOption(Property.SECURE));
                metaData.put("part_of_identity", configuration.getOption(Property.PART_OF_IDENTITY));

                configurations.add(new PluginConfigurationViewModel(configuration.getKey(), metaData, "repository"));
            }

            for(PackageConfiguration configuration: packageConfigurations.list()) {
                Map<String, Object> metaData = new HashMap<>();
                metaData.put("required", configuration.getOption(Property.REQUIRED));
                metaData.put("secure", configuration.getOption(Property.SECURE));
                metaData.put("part_of_identity", configuration.getOption(Property.PART_OF_IDENTITY));

                configurations.add(new PluginConfigurationViewModel(configuration.getKey(), metaData, "package"));
            }

            return new PluginViewModel(pluginId, about.name(), about.version(), PluginBuilder.PACKAGE_REPOSITORY.pluginExtension, configurations);
        }
    },

    AUTHENTICATION("authentication") {
        public PluginViewModel build(String pluginId, GoPluginDescriptor.About about, boolean includeConfigurations) {
            return new PluginViewModel(pluginId, about.name(), about.version(), PluginBuilder.AUTHENTICATION.pluginExtension);
        }
    },

    NOTIFICATION("notification") {
        public PluginViewModel build(String pluginId, GoPluginDescriptor.About about, boolean includeConfigurations) {
            return new PluginViewModel(pluginId, about.name(), about.version(), PluginBuilder.NOTIFICATION.pluginExtension);
        }
    };

    private String pluginExtension;

    PluginBuilder(String pluginExtension) {
        this.pluginExtension = pluginExtension;
    }

    public static PluginBuilder getByExtension(String extension) {
        for(PluginBuilder builder : PluginBuilder.values()) {
            if(builder.pluginExtension.equals(extension)) {
                return builder;
            }
        }
        return null;
    }

    public PluginViewModel build(String pluginId, GoPluginDescriptor.About about, boolean includeConfigurations) {
        return null;
    }
}
