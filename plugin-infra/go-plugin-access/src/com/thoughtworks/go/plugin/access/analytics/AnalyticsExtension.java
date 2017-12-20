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

package com.thoughtworks.go.plugin.access.analytics;

import com.thoughtworks.go.plugin.access.DefaultPluginInteractionCallback;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.domain.analytics.Capabilities;
import com.thoughtworks.go.plugin.domain.common.Image;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

import static com.thoughtworks.go.plugin.access.analytics.AnalyticsPluginConstants.*;

@Component
public class AnalyticsExtension extends AbstractExtension {
    public static String EXTENSION_NAME = "analytics";
    private final HashMap<String, AnalyticsMessageConverter> messageHandlerMap = new HashMap<>();

    @Autowired
    public AnalyticsExtension(PluginManager pluginManager) {
        super(pluginManager, new PluginRequestHelper(pluginManager, SUPPORTED_VERSIONS, EXTENSION_NAME), EXTENSION_NAME);
        addHandler(AnalyticsMessageConverterV1.VERSION, new PluginSettingsJsonMessageHandler1_0(), new AnalyticsMessageConverterV1());
    }

    private void addHandler(String version, PluginSettingsJsonMessageHandler messageHandler, AnalyticsMessageConverterV1 extensionHandler) {
        pluginSettingsMessageHandlerMap.put(version, messageHandler);
        messageHandlerMap.put(AnalyticsMessageConverterV1.VERSION, extensionHandler);
    }

    public Capabilities getCapabilities(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_CAPABILITIES, new DefaultPluginInteractionCallback<Capabilities>() {
            @Override
            public Capabilities onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getCapabilitiesFromResponseBody(responseBody);
            }
        });
    }

    public String getPipelineAnalytics(String pluginId, String pipelineName) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_PIPELINE_ANALYTICS, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getPipelineAnalyticsRequestBody(pipelineName);
            }

            @Override
            public String onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getPipelineAnalyticsFromResponseBody(responseBody);
            }
        });
    }

    public Image getIcon(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, AnalyticsPluginConstants.REQUEST_GET_PLUGIN_ICON, new DefaultPluginInteractionCallback<com.thoughtworks.go.plugin.domain.common.Image>() {
            @Override
            public com.thoughtworks.go.plugin.domain.common.Image onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getImageResponseFromBody(responseBody);
            }
        });
    }

    public String getStaticAssets(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_STATIC_ASSETS, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getStaticAssets(responseBody);
            }
        });
    }

    public AnalyticsMessageConverter getMessageConverter(String version) {
        return messageHandlerMap.get(version);
    }

    @Override
    protected List<String> goSupportedVersions() {
        return SUPPORTED_VERSIONS;
    }
}
