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

import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationPluginConstants;
import com.thoughtworks.go.plugin.access.common.models.Image;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.server.ui.plugins.PluginInfo;

import java.util.ArrayList;
import java.util.List;

@Deprecated
class AuthorizationViewModelBuilder extends AbstractViewModelBuilder {
    private AuthorizationMetadataStore metadataStore;

    AuthorizationViewModelBuilder(AuthorizationMetadataStore metadataStore) {

        this.metadataStore = metadataStore;
    }

    @Override
    public List<PluginInfo> allPluginInfos() {
        List<PluginInfo> pluginInfos = new ArrayList<>();

        for (AuthorizationPluginInfo pluginInfo : metadataStore.allPluginInfos()) {
            Image icon = image(pluginInfo.getImage());
            pluginInfos.add(new PluginInfo(pluginInfo.getDescriptor(), AuthorizationPluginConstants.EXTENSION_NAME, null, null, icon));
        }

        return pluginInfos;
    }

    @Override
    public PluginInfo pluginInfoFor(String pluginId) {
        AuthorizationPluginInfo pluginInfo = metadataStore.getPluginInfo(pluginId);

        if (pluginInfo == null) {
            return null;
        }

        return new PluginInfo(pluginInfo.getDescriptor(), AuthorizationPluginConstants.EXTENSION_NAME, null,
                settings(pluginInfo.getAuthConfigSettings()), settings(pluginInfo.getRoleSettings()), image(pluginInfo.getImage()));
    }
}
