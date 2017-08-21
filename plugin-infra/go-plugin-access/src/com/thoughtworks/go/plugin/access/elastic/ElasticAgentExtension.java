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

package com.thoughtworks.go.plugin.access.elastic;

import com.thoughtworks.go.plugin.access.DefaultPluginInteractionCallback;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.plugin.access.elastic.v1.ElasticAgentExtensionConverterV1;
import com.thoughtworks.go.plugin.access.elastic.v2.ElasticAgentExtensionConverterV2;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.elastic.Capabilities;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginConstants.*;
import static java.lang.String.format;

@Component
public class ElasticAgentExtension extends AbstractExtension {

    private final HashMap<String, ElasticAgentMessageConverter> messageHandlerMap = new HashMap<>();

    @Autowired
    public ElasticAgentExtension(PluginManager pluginManager) {
        super(pluginManager, new PluginRequestHelper(pluginManager, SUPPORTED_VERSIONS, ElasticAgentPluginConstants.EXTENSION_NAME), ElasticAgentPluginConstants.EXTENSION_NAME);
        addHandler(ElasticAgentExtensionConverterV1.VERSION, new PluginSettingsJsonMessageHandler1_0(), new ElasticAgentExtensionConverterV1());
        addHandler(ElasticAgentExtensionConverterV2.VERSION, new PluginSettingsJsonMessageHandler1_0(), new ElasticAgentExtensionConverterV2());
    }

    private void addHandler(String version, PluginSettingsJsonMessageHandler messageHandler, ElasticAgentMessageConverter extensionHandler) {
        pluginSettingsMessageHandlerMap.put(version, messageHandler);
        messageHandlerMap.put(version, extensionHandler);
    }

    public void createAgent(String pluginId, final String autoRegisterKey, final String environment, final Map<String, String> configuration) {
        pluginRequestHelper.submitRequest(pluginId, REQUEST_CREATE_AGENT, new DefaultPluginInteractionCallback<Void>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getElasticAgentMessageConverter(resolvedExtensionVersion).createAgentRequestBody(autoRegisterKey, environment, configuration);
            }
        });
    }

    public void serverPing(final String pluginId) {
        pluginRequestHelper.submitRequest(pluginId, REQUEST_SERVER_PING, new DefaultPluginInteractionCallback<Void>());
    }

    public boolean shouldAssignWork(String pluginId, final AgentMetadata agent, final String environment, final Map<String, String> configuration) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_SHOULD_ASSIGN_WORK, new DefaultPluginInteractionCallback<Boolean>() {

            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getElasticAgentMessageConverter(resolvedExtensionVersion).shouldAssignWorkRequestBody(agent, environment, configuration);
            }

            @Override
            public Boolean onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getElasticAgentMessageConverter(resolvedExtensionVersion).shouldAssignWorkResponseFromBody(responseBody);
            }
        });
    }

    public ElasticAgentMessageConverter getElasticAgentMessageConverter(String version) {
        return messageHandlerMap.get(version);
    }

    List<PluginConfiguration> getProfileMetadata(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_PROFILE_METADATA, new DefaultPluginInteractionCallback<List<PluginConfiguration>>() {
            @Override
            public List<PluginConfiguration> onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getElasticAgentMessageConverter(resolvedExtensionVersion).getProfileMetadataResponseFromBody(responseBody);
            }
        });
    }

    String getProfileView(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_PROFILE_VIEW, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getElasticAgentMessageConverter(resolvedExtensionVersion).getProfileViewResponseFromBody(responseBody);
            }
        });
    }

    public ValidationResult validate(final String pluginId, final Map<String, String> configuration) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_VALIDATE_PROFILE, new DefaultPluginInteractionCallback<ValidationResult>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getElasticAgentMessageConverter(resolvedExtensionVersion).validateRequestBody(configuration);
            }

            @Override
            public ValidationResult onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getElasticAgentMessageConverter(resolvedExtensionVersion).getValidationResultResponseFromBody(responseBody);
            }
        });
    }

    com.thoughtworks.go.plugin.domain.common.Image getIcon(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_PLUGIN_SETTINGS_ICON, new DefaultPluginInteractionCallback<com.thoughtworks.go.plugin.domain.common.Image>() {
            @Override
            public com.thoughtworks.go.plugin.domain.common.Image onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getElasticAgentMessageConverter(resolvedExtensionVersion).getImageResponseFromBody(responseBody);
            }
        });
    }

    public String getStatusReport(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_STATUS_REPORT, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String onSuccess(String responseBody, String resolvedExtensionVersion) {
                final ElasticAgentExtensionConverterV2 converter = (ElasticAgentExtensionConverterV2) getElasticAgentMessageConverter(resolvedExtensionVersion);
                return converter.getStatusReportView(responseBody);
            }
        });
    }

    public Capabilities getCapabilities(String pluginId) {
        if (!ElasticAgentExtensionConverterV2.VERSION.equals(pluginManager.resolveExtensionVersion(pluginId, SUPPORTED_VERSIONS))) {
            throw new UnsupportedOperationException(format("Plugin `%s` implements Elastic Agent V1, `Capabilities` endpoint is not supported in this version.", pluginId));
        }

        return pluginRequestHelper.submitRequest(pluginId, REQUEST_CAPABILTIES, new DefaultPluginInteractionCallback<Capabilities>() {
            @Override
            public Capabilities onSuccess(String responseBody, String resolvedExtensionVersion) {
                final ElasticAgentExtensionConverterV2 converter = (ElasticAgentExtensionConverterV2) getElasticAgentMessageConverter(resolvedExtensionVersion);
                return converter.getCapabilitiesFromResponseBody(responseBody);
            }
        });
    }
}