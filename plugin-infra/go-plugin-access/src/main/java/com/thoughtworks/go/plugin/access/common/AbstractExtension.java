/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.access.common;

import com.thoughtworks.go.plugin.access.DefaultPluginInteractionCallback;
import com.thoughtworks.go.plugin.access.ExtensionsRegistry;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.common.settings.GoPluginExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConstants;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.PluginManager;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractExtension implements GoPluginExtension {
    protected PluginManager pluginManager;
    protected final PluginRequestHelper pluginRequestHelper;
    private final String extensionName;
    protected Map<String, PluginSettingsJsonMessageHandler> pluginSettingsMessageHandlerMap = new HashMap<>();

    protected AbstractExtension(PluginManager pluginManager, ExtensionsRegistry extensionsRegistry, PluginRequestHelper pluginRequestHelper, String extensionName) {
        this.pluginManager = pluginManager;
        this.pluginRequestHelper = pluginRequestHelper;
        this.extensionName = extensionName;
        extensionsRegistry.registerExtension(this);
    }

    @Override
    public boolean canHandlePlugin(String pluginId) {
        return pluginManager.isPluginOfType(this.extensionName, pluginId);
    }

    @Override
    public String extensionName() {
        return extensionName;
    }

    @Override
    public PluginSettingsConfiguration getPluginSettingsConfiguration(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, PluginSettingsConstants.REQUEST_PLUGIN_SETTINGS_CONFIGURATION, new DefaultPluginInteractionCallback<PluginSettingsConfiguration>() {
            @Override
            public PluginSettingsConfiguration onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return pluginSettingsMessageHandlerMap.get(resolvedExtensionVersion).responseMessageForPluginSettingsConfiguration(responseBody);
            }
        });
    }

    @Override
    public String getPluginSettingsView(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, PluginSettingsConstants.REQUEST_PLUGIN_SETTINGS_VIEW, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return pluginSettingsMessageHandlerMap.get(resolvedExtensionVersion).responseMessageForPluginSettingsView(responseBody);
            }
        });
    }

    @Override
    public void notifyPluginSettingsChange(String pluginId, Map<String, String> pluginSettings) {
        String resolvedExtensionVersion = pluginManager.resolveExtensionVersion(pluginId, extensionName, goSupportedVersions());

        if (!pluginSettingsMessageHandlerMap.get(resolvedExtensionVersion).supportsPluginSettingsNotification()) {
            return;
        }

        pluginRequestHelper.submitRequest(pluginId, PluginSettingsConstants.REQUEST_NOTIFY_PLUGIN_SETTINGS_CHANGE, new DefaultPluginInteractionCallback<Void>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return pluginSettingsMessageHandlerMap.get(resolvedExtensionVersion).requestMessageForNotifyPluginSettingsChange(pluginSettings);
            }
        });
    }

    @Override
    public ValidationResult validatePluginSettings(String pluginId, final PluginSettingsConfiguration configuration) {
        return pluginRequestHelper.submitRequest(pluginId, PluginSettingsConstants.REQUEST_VALIDATE_PLUGIN_SETTINGS, new DefaultPluginInteractionCallback<ValidationResult>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return pluginSettingsMessageHandlerMap.get(resolvedExtensionVersion).requestMessageForPluginSettingsValidation(configuration);
            }

            @Override
            public ValidationResult onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return pluginSettingsMessageHandlerMap.get(resolvedExtensionVersion).responseMessageForPluginSettingsValidation(responseBody);
            }
        });
    }

    public void registerHandler(String version, PluginSettingsJsonMessageHandler handler) {
        pluginSettingsMessageHandlerMap.put(version, handler);
    }
}
