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

import com.thoughtworks.go.plugin.access.DefaultPluginInteractionCallback;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.authentication.model.AuthenticationPluginConfiguration;
import com.thoughtworks.go.plugin.access.authentication.model.User;
import com.thoughtworks.go.plugin.access.common.settings.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

@Component
public class AuthenticationExtension extends AbstractExtension {
    public static final String EXTENSION_NAME = "authentication";
    private static final List<String> goSupportedVersions = asList("1.0");

    public static final String REQUEST_PLUGIN_CONFIGURATION = "go.authentication.plugin-configuration";
    public static final String REQUEST_AUTHENTICATE_USER = "go.authentication.authenticate-user";
    public static final String REQUEST_SEARCH_USER = "go.authentication.search-user";

    private Map<String, JsonMessageHandler> messageHandlerMap = new HashMap<>();

    @Autowired
    public AuthenticationExtension(PluginManager defaultPluginManager) {
        super(defaultPluginManager, new PluginRequestHelper(defaultPluginManager, goSupportedVersions, EXTENSION_NAME), EXTENSION_NAME);
        this.pluginSettingsMessageHandlerMap.put("1.0", new PluginSettingsJsonMessageHandler1_0());
        this.messageHandlerMap.put("1.0", new JsonMessageHandler1_0());
    }

    public AuthenticationPluginConfiguration getPluginConfiguration(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_PLUGIN_CONFIGURATION, new DefaultPluginInteractionCallback<AuthenticationPluginConfiguration>() {
            @Override
            public AuthenticationPluginConfiguration onSuccess(String responseBody, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForPluginConfiguration(responseBody);
            }
        });
    }

    public User authenticateUser(String pluginId, final String username, final String password) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_AUTHENTICATE_USER, new DefaultPluginInteractionCallback<User>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).requestMessageForAuthenticateUser(username, password);
            }

            @Override
            public User onSuccess(String responseBody, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForAuthenticateUser(responseBody);
            }
        });
    }

    public List<User> searchUser(String pluginId, final String searchTerm) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_SEARCH_USER, new DefaultPluginInteractionCallback<List<User>>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).requestMessageForSearchUser(searchTerm);
            }

            @Override
            public List<User> onSuccess(String responseBody, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForSearchUser(responseBody);
            }
        });
    }

    Map<String, PluginSettingsJsonMessageHandler> getPluginSettingsMessageHandlerMap() {
        return pluginSettingsMessageHandlerMap;
    }

    Map<String, JsonMessageHandler> getMessageHandlerMap() {
        return messageHandlerMap;
    }
}
