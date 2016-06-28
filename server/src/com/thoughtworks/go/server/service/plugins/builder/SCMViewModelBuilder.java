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

import com.thoughtworks.go.plugin.access.scm.SCMConfiguration;
import com.thoughtworks.go.plugin.access.scm.SCMConfigurations;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.access.scm.SCMMetadataStore;
import com.thoughtworks.go.plugin.access.scm.SCMPreference;
import com.thoughtworks.go.plugin.api.config.Property;
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

class SCMViewModelBuilder implements ViewModelBuilder {
    private PluginManager pluginManager;

    public SCMViewModelBuilder(PluginManager manager) {
        this.pluginManager = manager;
    }

    public List<PluginInfo> allPluginInfos() {
        List<PluginInfo> pluginInfos = new ArrayList<>();

        for(String pluginId : SCMMetadataStore.getInstance().pluginIds()) {
            GoPluginDescriptor descriptor = pluginManager.getPluginDescriptorFor(pluginId);

            SCMPreference scmPreference = SCMMetadataStore.getInstance().preferenceFor(pluginId);

            pluginInfos.add(new PluginInfo(pluginId, descriptor.about().name(), descriptor.about().version(),
                    SCMExtension.EXTENSION_NAME, scmPreference.getScmView().displayValue()));
        }
        return pluginInfos;
    }

    public PluginInfo pluginInfoFor(String pluginId) {
        if(!SCMMetadataStore.getInstance().hasPreferenceFor(pluginId)) {
            return null;
        }

        GoPluginDescriptor descriptor = pluginManager.getPluginDescriptorFor(pluginId);
        SCMPreference scmPreference = SCMMetadataStore.getInstance().preferenceFor(pluginId);

        List<PluginConfiguration> pluginConfigurations = configurations(scmPreference.getScmConfigurations());
        PluginView pluginView = new PluginView(scmPreference.getScmView().template());

        return new PluginInfo(pluginId, descriptor.about().name(), descriptor.about().version(), SCMExtension.EXTENSION_NAME,
                scmPreference.getScmView().displayValue(), new PluggableInstanceSettings(pluginConfigurations, pluginView));
    }

    private List<PluginConfiguration> configurations(SCMConfigurations scmConfigurations) {
        List<PluginConfiguration> pluginConfigurations = new ArrayList<>();

        for(SCMConfiguration configuration : scmConfigurations.list()) {
            Map<String, Object> metaData = new HashMap<>();
            metaData.put(REQUIRED_OPTION, configuration.getOption(Property.REQUIRED));
            metaData.put(SECURE_OPTION, configuration.getOption(Property.SECURE));
            metaData.put(PART_OF_IDENTITY_OPTION, configuration.getOption(Property.PART_OF_IDENTITY));

            pluginConfigurations.add(new PluginConfiguration(configuration.getKey(), metaData));
        }
        return pluginConfigurations;
    }
}