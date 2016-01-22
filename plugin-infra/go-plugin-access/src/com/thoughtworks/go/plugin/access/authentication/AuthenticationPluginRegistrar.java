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

package com.thoughtworks.go.plugin.access.authentication;

import com.thoughtworks.go.plugin.access.authentication.model.AuthenticationPluginConfiguration;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.infra.PluginChangeListener;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.apache.log4j.Logger.getLogger;

@Component
public class AuthenticationPluginRegistrar implements PluginChangeListener {
    private static Logger LOGGER = getLogger(AuthenticationPluginRegistrar.class);

    private AuthenticationExtension authenticationExtension;
    private AuthenticationPluginRegistry authenticationPluginRegistry;

    @Autowired
    public AuthenticationPluginRegistrar(PluginManager pluginManager, AuthenticationExtension authenticationExtension, AuthenticationPluginRegistry authenticationPluginRegistry) {
        this.authenticationExtension = authenticationExtension;
        this.authenticationPluginRegistry = authenticationPluginRegistry;
        pluginManager.addPluginChangeListener(this, GoPlugin.class);
    }

    @Override
    public void pluginLoaded(GoPluginDescriptor pluginDescriptor) {
        if (authenticationExtension.canHandlePlugin(pluginDescriptor.id())) {
            try {
                AuthenticationPluginConfiguration configuration = authenticationExtension.getPluginConfiguration(pluginDescriptor.id());
                authenticationPluginRegistry.registerPlugin(pluginDescriptor.id(), configuration);
            } catch (Exception e) {
                LOGGER.warn("Error occurred during plugin registration.", e);
            }
        }
    }

    @Override
    public void pluginUnLoaded(GoPluginDescriptor pluginDescriptor) {
        authenticationPluginRegistry.deregisterPlugin(pluginDescriptor.id());
    }
}
