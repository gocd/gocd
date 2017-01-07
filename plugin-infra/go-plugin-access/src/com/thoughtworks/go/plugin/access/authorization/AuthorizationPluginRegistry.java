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

package com.thoughtworks.go.plugin.access.authorization;

import com.thoughtworks.go.plugin.access.authorization.models.Capabilities;
import com.thoughtworks.go.plugin.access.common.AbstractPluginRegistry;
import com.thoughtworks.go.plugin.access.common.models.Image;
import com.thoughtworks.go.plugin.access.common.models.PluginProfileMetadataKeys;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationPluginRegistry extends AbstractPluginRegistry<AuthorizationExtension, AuthorizationPluginConfigMetadataStore> {

    @Autowired
    public AuthorizationPluginRegistry(PluginManager pluginManager, AuthorizationExtension extension, AuthorizationPluginConfigMetadataStore store) {
        super(pluginManager, extension, store);
    }

    public Image getIcon(String pluginId) {
        return extension.getIcon(pluginId);
    }

    public PluginProfileMetadataKeys getPluginConfigurationMetadata(String pluginId) {
        return extension.getPluginConfigurationMetadata(pluginId);
    }

    public String getPluginConfigurationView(String pluginId) {
        return extension.getPluginConfigurationView(pluginId);
    }

    public PluginProfileMetadataKeys getRoleConfigurationMetadata(String pluginId) {
        return extension.getRoleConfigurationMetadata(pluginId);
    }

    public String getRoleConfigurationView(String pluginId) {
        return extension.getRoleConfigurationView(pluginId);
    }

    public Capabilities getCapabilities(String pluginId) {
        return extension.getCapabilities(pluginId);
    }
}
