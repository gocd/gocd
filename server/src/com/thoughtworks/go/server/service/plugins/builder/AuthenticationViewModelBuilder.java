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

import com.thoughtworks.go.plugin.access.authentication.AuthenticationExtension;
import com.thoughtworks.go.plugin.access.authentication.AuthenticationPluginRegistry;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.ui.plugins.PluginInfo;

import java.util.ArrayList;
import java.util.List;

class AuthenticationViewModelBuilder implements ViewModelBuilder {
    private PluginManager pluginManager;
    private AuthenticationPluginRegistry registry;

    public AuthenticationViewModelBuilder(PluginManager manager, AuthenticationPluginRegistry registry) {
        this.pluginManager = manager;
        this.registry = registry;
    }

    public List<PluginInfo> allPluginInfos() {
        List<PluginInfo> pluginInfos = new ArrayList<>();

        for(String pluginId : registry.getAuthenticationPlugins()) {
            GoPluginDescriptor descriptor = pluginManager.getPluginDescriptorFor(pluginId);

            pluginInfos.add(new PluginInfo(pluginId, descriptor.about().name(), descriptor.about().version(),
                    AuthenticationExtension.EXTENSION_NAME, null));
        }
        return pluginInfos;
    }

    public PluginInfo pluginInfoFor(String pluginId) {
        if(!registry.getAuthenticationPlugins().contains(pluginId)) {
            return null;
        }

        GoPluginDescriptor descriptor = pluginManager.getPluginDescriptorFor(pluginId);

        return new PluginInfo(pluginId, descriptor.about().name(), descriptor.about().version(),
                AuthenticationExtension.EXTENSION_NAME, null, null);
    }
}