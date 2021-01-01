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
package com.thoughtworks.go.plugin.access.analytics;

import com.thoughtworks.go.plugin.access.DefaultPluginInteractionCallback;
import com.thoughtworks.go.plugin.access.ExtensionsRegistry;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.analytics.V1.AnalyticsMessageConverterV1;
import com.thoughtworks.go.plugin.access.analytics.V2.AnalyticsMessageConverterV2;
import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler2_0;
import com.thoughtworks.go.plugin.domain.analytics.AnalyticsData;
import com.thoughtworks.go.plugin.domain.analytics.Capabilities;
import com.thoughtworks.go.plugin.domain.common.Image;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.plugin.access.analytics.AnalyticsPluginConstants.*;
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.ANALYTICS_EXTENSION;

@Component
public class AnalyticsExtension extends AbstractExtension {
    private final HashMap<String, AnalyticsMessageConverter> messageHandlerMap = new HashMap<>();

    @Autowired
    public AnalyticsExtension(PluginManager pluginManager, ExtensionsRegistry extensionsRegistry) {
        super(pluginManager, extensionsRegistry, new PluginRequestHelper(pluginManager, SUPPORTED_VERSIONS, ANALYTICS_EXTENSION), ANALYTICS_EXTENSION);
        addHandler(AnalyticsMessageConverterV1.VERSION, new PluginSettingsJsonMessageHandler2_0(), new AnalyticsMessageConverterV1()
        );
        addHandler(AnalyticsMessageConverterV2.VERSION, new PluginSettingsJsonMessageHandler2_0(), new AnalyticsMessageConverterV2()
        );
    }

    private void addHandler(String version, PluginSettingsJsonMessageHandler messageHandler, AnalyticsMessageConverter extensionHandler) {
        pluginSettingsMessageHandlerMap.put(version, messageHandler);
        messageHandlerMap.put(version, extensionHandler);
    }

    public Capabilities getCapabilities(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_CAPABILITIES, new DefaultPluginInteractionCallback<Capabilities>() {
            @Override
            public Capabilities onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getCapabilitiesFromResponseBody(responseBody);
            }
        });
    }

    public AnalyticsData getAnalytics(String pluginId, String type, String metricId, Map params) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_ANALYTICS, new DefaultPluginInteractionCallback<AnalyticsData>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getAnalyticsRequestBody(type, metricId, params);
            }

            @Override
            public AnalyticsData onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                AnalyticsData analyticsData = getMessageConverter(resolvedExtensionVersion).getAnalyticsFromResponseBody(responseBody);
                analyticsData.setAssetRoot(getCurrentStaticAssetsPath(pluginId));
                return analyticsData;
            }
        });
    }

    public Image getIcon(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, AnalyticsPluginConstants.REQUEST_GET_PLUGIN_ICON, new DefaultPluginInteractionCallback<com.thoughtworks.go.plugin.domain.common.Image>() {
            @Override
            public com.thoughtworks.go.plugin.domain.common.Image onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getImageFromResponseBody(responseBody);
            }
        });
    }

    public String getStaticAssets(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_STATIC_ASSETS, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageConverter(resolvedExtensionVersion).getStaticAssetsFromResponseBody(responseBody);
            }
        });
    }

    private AnalyticsMessageConverter getMessageConverter(String version) {
        return messageHandlerMap.get(version);
    }

    @Override
    public List<String> goSupportedVersions() {
        return SUPPORTED_VERSIONS;
    }


    private String getCurrentStaticAssetsPath(String pluginId) {
        return AnalyticsMetadataStore.instance().getPluginInfo(pluginId).getStaticAssetsPath();
    }
}
