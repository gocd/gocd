/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.plugin.access.authentication;

import com.thoughtworks.go.plugin.access.PluginInteractionCallback;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.authentication.model.AuthenticationPluginConfiguration;
import com.thoughtworks.go.plugin.access.authentication.model.User;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

@Component
public class AuthenticationExtension {
    public static final String EXTENSION_NAME = "authentication";
    private static final List<String> goSupportedVersions = asList("1.0");

    public static final String PLUGIN_CONFIGURATION = "plugin-configuration";
    public static final String AUTHENTICATE_USER = "authenticate-user";
    public static final String GET_USER_DETAILS = "get-user-details";
    public static final String SEARCH_USER = "search-user";

    private PluginManager pluginManager;
    private final PluginRequestHelper pluginRequestHelper;
    private Map<String, JsonMessageHandler> messageHandlerMap = new HashMap<String, JsonMessageHandler>();

    @Autowired
    public AuthenticationExtension(PluginManager defaultPluginManager) {
        this.pluginManager = defaultPluginManager;
        this.pluginRequestHelper = new PluginRequestHelper(pluginManager, goSupportedVersions, EXTENSION_NAME);
        this.messageHandlerMap.put("1.0", new JsonMessageHandler1_0());
    }

    public AuthenticationPluginConfiguration getPluginConfiguration(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, PLUGIN_CONFIGURATION, new PluginInteractionCallback<AuthenticationPluginConfiguration>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return null;
            }

            @Override
            public Map<String, String> requestParams(String resolvedExtensionVersion) {
                return null;
            }

            @Override
            public AuthenticationPluginConfiguration onSuccess(String responseBody, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForPluginConfiguration(responseBody);
            }
        });
    }

    public Result authenticateUser(String pluginId, final String username, final String password) {
        return pluginRequestHelper.submitRequest(pluginId, AUTHENTICATE_USER, new PluginInteractionCallback<Result>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).requestMessageForAuthenticateUser(username, password);
            }

            @Override
            public Map<String, String> requestParams(String resolvedExtensionVersion) {
                return null;
            }

            @Override
            public Result onSuccess(String responseBody, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForAuthenticateUser(responseBody);
            }
        });
    }

    public User getUserDetails(String pluginId, final String username) {
        return pluginRequestHelper.submitRequest(pluginId, GET_USER_DETAILS, new PluginInteractionCallback<User>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).requestMessageForGetUserDetails(username);
            }

            @Override
            public Map<String, String> requestParams(String resolvedExtensionVersion) {
                return null;
            }

            @Override
            public User onSuccess(String responseBody, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForGetUserDetails(responseBody);
            }
        });
    }

    public List<User> searchUser(String pluginId, final String searchTerm) {
        return pluginRequestHelper.submitRequest(pluginId, SEARCH_USER, new PluginInteractionCallback<List<User>>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).requestMessageForSearchUser(searchTerm);
            }

            @Override
            public Map<String, String> requestParams(String resolvedExtensionVersion) {
                return null;
            }

            @Override
            public List<User> onSuccess(String responseBody, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForSearchUser(responseBody);
            }
        });
    }

    boolean isAuthenticationPlugin(String pluginId) {
        return pluginManager.isPluginOfType(EXTENSION_NAME, pluginId);
    }

    Map<String, JsonMessageHandler> getMessageHandlerMap() {
        return messageHandlerMap;
    }
}
