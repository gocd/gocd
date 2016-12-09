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

import com.thoughtworks.go.plugin.access.common.models.Image;
import com.thoughtworks.go.plugin.access.common.models.PluginProfileMetadataKey;
import com.thoughtworks.go.plugin.access.common.models.PluginProfileMetadataKeys;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginConstants;
import com.thoughtworks.go.plugin.access.elastic.ElasticPluginConfigMetadataStore;
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
    private ElasticPluginConfigMetadataStore elasticPluginConfigMetadataStore;

    ElasticAgentViewViewModelBuilder(ElasticPluginConfigMetadataStore elasticPluginConfigMetadataStore) {
        this.elasticPluginConfigMetadataStore = elasticPluginConfigMetadataStore;
    }

    @Override
    public List<PluginInfo> allPluginInfos() {
        List<PluginInfo> pluginInfos = new ArrayList<>();

        for (PluginDescriptor descriptor : elasticPluginConfigMetadataStore.getPlugins()) {
            Image icon = elasticPluginConfigMetadataStore.getIcon(descriptor);
            pluginInfos.add(new PluginInfo(descriptor, ElasticAgentPluginConstants.EXTENSION_NAME, null, null, icon));
        }

        return pluginInfos;
    }

    @Override
    public PluginInfo pluginInfoFor(String pluginId) {
        PluginDescriptor descriptor = elasticPluginConfigMetadataStore.find(pluginId);

        if (descriptor == null) {
            return null;
        }

        Image icon = elasticPluginConfigMetadataStore.getIcon(descriptor);
        ArrayList<PluginConfiguration> pluginConfigurations = getPluginConfigurations(elasticPluginConfigMetadataStore.getProfileMetadata(descriptor));

        PluginView pluginView = new PluginView(elasticPluginConfigMetadataStore.getProfileView(descriptor));
        PluggableInstanceSettings settings = new PluggableInstanceSettings(pluginConfigurations, pluginView);

        return new PluginInfo(descriptor, ElasticAgentPluginConstants.EXTENSION_NAME, null, settings, icon);
    }

    private ArrayList<PluginConfiguration> getPluginConfigurations(PluginProfileMetadataKeys config) {
        ArrayList<PluginConfiguration> pluginConfigurations = new ArrayList<>();
        for (PluginProfileMetadataKey property : config) {
            Map<String, Object> metaData = new HashMap<>();
            metaData.put(REQUIRED_OPTION, property.getMetadata().isRequired());
            metaData.put(SECURE_OPTION, property.getMetadata().isSecure());

            pluginConfigurations.add(new PluginConfiguration(property.getKey(), metaData));
        }
        return pluginConfigurations;
    }
}
