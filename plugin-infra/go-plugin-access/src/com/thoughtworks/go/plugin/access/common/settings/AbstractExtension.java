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

package com.thoughtworks.go.plugin.access.common.settings;

import com.thoughtworks.go.plugin.access.PluginInteractionCallback;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.PluginManager;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractExtension {
    protected PluginManager pluginManager;
    protected final PluginRequestHelper pluginRequestHelper;
    protected Map<String, PluginSettingsJsonMessageHandler> pluginSettingsMessageHandlerMap = new HashMap<String, PluginSettingsJsonMessageHandler>();

    protected AbstractExtension(PluginManager pluginManager, PluginRequestHelper pluginRequestHelper) {
        this.pluginManager = pluginManager;
        this.pluginRequestHelper = pluginRequestHelper;
    }

    public boolean isPluginOfType(String pluginId, String extensionName) {
        return pluginManager.isPluginOfType(extensionName, pluginId);
    }

    public PluginSettingsConfiguration getPluginSettingsConfiguration(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, PluginSettingsConstants.REQUEST_PLUGIN_SETTINGS_CONFIGURATION, new PluginInteractionCallback<PluginSettingsConfiguration>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return null;
            }

            @Override
            public Map<String, String> requestParams(String resolvedExtensionVersion) {
                return null;
            }

            @Override
            public PluginSettingsConfiguration onSuccess(String responseBody, String resolvedExtensionVersion) {
                return pluginSettingsMessageHandlerMap.get(resolvedExtensionVersion).responseMessageForPluginSettingsConfiguration(responseBody);
            }
        });
    }

    public String getPluginSettingsView(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, PluginSettingsConstants.REQUEST_PLUGIN_SETTINGS_VIEW, new PluginInteractionCallback<String>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return null;
            }

            @Override
            public Map<String, String> requestParams(String resolvedExtensionVersion) {
                return null;
            }

            @Override
            public String onSuccess(String responseBody, String resolvedExtensionVersion) {
                return pluginSettingsMessageHandlerMap.get(resolvedExtensionVersion).responseMessageForPluginSettingsView(responseBody);
            }
        });
    }

    public ValidationResult validatePluginSettings(String pluginId, final PluginSettingsConfiguration configuration) {
        return pluginRequestHelper.submitRequest(pluginId, PluginSettingsConstants.REQUEST_VALIDATE_PLUGIN_SETTINGS, new PluginInteractionCallback<ValidationResult>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return pluginSettingsMessageHandlerMap.get(resolvedExtensionVersion).requestMessageForPluginSettingsValidation(configuration);
            }

            @Override
            public Map<String, String> requestParams(String resolvedExtensionVersion) {
                return null;
            }

            @Override
            public ValidationResult onSuccess(String responseBody, String resolvedExtensionVersion) {
                return pluginSettingsMessageHandlerMap.get(resolvedExtensionVersion).responseMessageForPluginSettingsValidation(responseBody);
            }
        });
    }
}
