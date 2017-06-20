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

package com.thoughtworks.go.plugin.access.authentication;

import com.thoughtworks.go.plugin.access.authentication.models.AuthenticationPluginConfiguration;
import com.thoughtworks.go.plugin.access.common.PluginInfoBuilder;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
import com.thoughtworks.go.plugin.domain.authentication.AuthenticationPluginInfo;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginView;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Deprecated
@Component
public class AuthenticationPluginInfoBuilder implements PluginInfoBuilder<AuthenticationPluginInfo> {

    private AuthenticationExtension extension;

    @Autowired
    public AuthenticationPluginInfoBuilder(AuthenticationExtension extension) {
        this.extension = extension;
    }

    public AuthenticationPluginInfo pluginInfoFor(GoPluginDescriptor descriptor) {
        PluginSettingsConfiguration pluginSettingsConfiguration = extension.getPluginSettingsConfiguration(descriptor.id());
        String pluginSettingsView = extension.getPluginSettingsView(descriptor.id());
        AuthenticationPluginConfiguration pluginConfiguration = extension.getPluginConfiguration(descriptor.id());

        PluggableInstanceSettings pluggableInstanceSettings = new PluggableInstanceSettings(configurations(pluginSettingsConfiguration), new PluginView(pluginSettingsView));
        return new AuthenticationPluginInfo(descriptor,
                pluginConfiguration.getDisplayName(),
                pluginConfiguration.getDisplayImageURL(),
                pluginConfiguration.supportsPasswordBasedAuthentication(),
                pluginConfiguration.supportsWebBasedAuthentication(),
                pluggableInstanceSettings);
    }

}

