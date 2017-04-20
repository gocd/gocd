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
import com.thoughtworks.go.plugin.access.authorization.models.Capabilities;
import com.thoughtworks.go.plugin.access.authorization.models.SupportedAuthType;
import com.thoughtworks.go.server.ui.plugins.AuthorizationPluginInfo;

public class AuthorizationPluginInfoBuilder extends PluginConfigMetadataStoreBasedPluginInfoBuilder<AuthorizationPluginInfo, AuthorizationMetadataStore> {

    public AuthorizationPluginInfoBuilder(AuthorizationMetadataStore store) {
        super(store);
    }

    @Override
    public AuthorizationPluginInfo pluginInfoFor(String pluginId) {
        com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo pluginInfo = store.getPluginInfo(pluginId);

        if (pluginInfo == null) {
            return null;
        }

        return new AuthorizationPluginInfo(pluginInfo.getDescriptor(), settings(pluginInfo.getAuthConfigSettings()),
                settings(pluginInfo.getRoleSettings()), image(pluginInfo.getImage()), capabilities(pluginInfo.getCapabilities()));
    }

    private Capabilities capabilities(com.thoughtworks.go.plugin.domain.authorization.Capabilities capabilities) {
        return new Capabilities(supportedAuthType(capabilities.getSupportedAuthType()), capabilities.canSearch(), capabilities.canAuthorize());
    }

    private SupportedAuthType supportedAuthType(com.thoughtworks.go.plugin.domain.authorization.SupportedAuthType supportedAuthType) {
        return SupportedAuthType.valueOf(supportedAuthType.name());
    }
}
