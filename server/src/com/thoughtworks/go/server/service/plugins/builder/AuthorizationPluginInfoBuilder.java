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

import com.thoughtworks.go.plugin.access.authorization.AuthorizationPluginConfigMetadataStore;
import com.thoughtworks.go.plugin.access.common.models.Image;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.server.ui.plugins.AuthorizationPluginInfo;
import com.thoughtworks.go.server.ui.plugins.PluggableInstanceSettings;
import com.thoughtworks.go.server.ui.plugins.PluginConfiguration;
import com.thoughtworks.go.server.ui.plugins.PluginView;

import java.util.ArrayList;
import java.util.List;

public class AuthorizationPluginInfoBuilder extends PluginConfigMetadataStoreBasedPluginInfoBuilder<AuthorizationPluginInfo, AuthorizationPluginConfigMetadataStore> {

    public AuthorizationPluginInfoBuilder(AuthorizationPluginConfigMetadataStore store) {
        super(store);
    }

    @Override
    public AuthorizationPluginInfo pluginInfoFor(String pluginId) {
        PluginDescriptor descriptor = store.find(pluginId);
        if (descriptor == null) {
            return null;
        }

        Image icon = store.getIcon(descriptor);
        ArrayList<PluginConfiguration> pluginConfigurations = PluginConfiguration.getPluginConfigurations(store.getProfileMetadata(descriptor));
        PluginView profileView = new PluginView(store.getProfileView(descriptor));
        PluggableInstanceSettings profileSettings = new PluggableInstanceSettings(pluginConfigurations, profileView);

        PluggableInstanceSettings roleSettings = null;
        if (store.canAuthorize(pluginId)) {
            List<PluginConfiguration> roleConfigurations = PluginConfiguration.getPluginConfigurations(store.getRoleMetadata(descriptor));
            PluginView roleView = new PluginView(store.getRoleView(descriptor));
            roleSettings = new PluggableInstanceSettings(roleConfigurations, roleView);
        }


        return new AuthorizationPluginInfo(descriptor, profileSettings, roleSettings, icon);
    }

}
