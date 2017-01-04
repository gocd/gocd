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

import com.thoughtworks.go.plugin.access.authorization.AuthorizationPluginConfigMetadataStore;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationPluginConstants;
import com.thoughtworks.go.plugin.access.common.models.Image;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.server.ui.plugins.PluggableInstanceSettings;
import com.thoughtworks.go.server.ui.plugins.PluginConfiguration;
import com.thoughtworks.go.server.ui.plugins.PluginInfo;
import com.thoughtworks.go.server.ui.plugins.PluginView;

import java.util.ArrayList;
import java.util.List;

class AuthorizationViewModelBuilder implements ViewModelBuilder {
    private AuthorizationPluginConfigMetadataStore metadataStore;

    AuthorizationViewModelBuilder(AuthorizationPluginConfigMetadataStore metadataStore) {
        this.metadataStore = metadataStore;
    }

    @Override
    public List<PluginInfo> allPluginInfos() {
        List<PluginInfo> pluginInfos = new ArrayList<>();

        for (PluginDescriptor descriptor : metadataStore.getPlugins()) {
            Image icon = metadataStore.getIcon(descriptor);
            pluginInfos.add(new PluginInfo(descriptor, AuthorizationPluginConstants.EXTENSION_NAME, null, null, icon));
        }

        return pluginInfos;
    }


    @Override
    public PluginInfo pluginInfoFor(String pluginId) {
        PluginDescriptor descriptor = metadataStore.find(pluginId);

        if (descriptor == null) {
            return null;
        }

        Image icon = metadataStore.getIcon(descriptor);
        ArrayList<PluginConfiguration> pluginConfigurations = PluginConfiguration.getPluginConfigurations(metadataStore.getProfileMetadata(descriptor));
        PluginView profileView = new PluginView(metadataStore.getProfileView(descriptor));
        PluggableInstanceSettings profileSettings = new PluggableInstanceSettings(pluginConfigurations, profileView);

        ArrayList<PluginConfiguration> roleConfigurations = PluginConfiguration.getPluginConfigurations(metadataStore.getRoleMetadata(descriptor));
        PluginView roleView = new PluginView(metadataStore.getRoleView(descriptor));
        PluggableInstanceSettings roleSettings = new PluggableInstanceSettings(roleConfigurations, roleView);

        return new PluginInfo(descriptor, AuthorizationPluginConstants.EXTENSION_NAME, null, profileSettings, roleSettings, icon);
    }

}
