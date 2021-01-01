/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.access.authorization;

import com.thoughtworks.go.plugin.access.common.PluginInfoBuilder;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.plugin.domain.authorization.Capabilities;
import com.thoughtworks.go.plugin.domain.common.Image;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginView;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationPluginInfoBuilder implements PluginInfoBuilder<AuthorizationPluginInfo> {

    private AuthorizationExtension extension;

    @Autowired
    public AuthorizationPluginInfoBuilder(AuthorizationExtension extension) {
        this.extension = extension;
    }

    @Override
    public AuthorizationPluginInfo pluginInfoFor(GoPluginDescriptor descriptor) {
        Capabilities capabilities = capabilities(descriptor.id());

        PluggableInstanceSettings authConfigSettings = authConfigSettings(descriptor.id());
        PluggableInstanceSettings roleSettings = roleSettings(descriptor.id(), capabilities);
        Image image = image(descriptor.id());

        return new AuthorizationPluginInfo(descriptor, authConfigSettings, roleSettings, image, capabilities);
    }

    private Capabilities capabilities(String pluginId) {
        return extension.getCapabilities(pluginId);
    }

    private PluggableInstanceSettings authConfigSettings(String pluginId) {
        return new PluggableInstanceSettings(extension.getAuthConfigMetadata(pluginId),
                new PluginView(extension.getAuthConfigView(pluginId)));
    }

    private PluggableInstanceSettings roleSettings(String pluginId, Capabilities capabilities) {
        if (capabilities.canAuthorize()) {
            return new PluggableInstanceSettings(extension.getRoleConfigurationMetadata(pluginId),
                    new PluginView(extension.getRoleConfigurationView(pluginId)));
        }

        return null;
    }

    private Image image(String pluginId) {
        return extension.getIcon(pluginId);
    }
}

