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

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.plugin.access.DefaultPluginInteractionCallback;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.serverinfo.MessageHandlerForServerInfoRequestProcessor;
import com.thoughtworks.go.plugin.access.common.serverinfo.MessageHandlerForServerInfoRequestProcessor1_0;
import com.thoughtworks.go.plugin.access.common.settings.MessageHandlerForPluginSettingsRequestProcessor;
import com.thoughtworks.go.plugin.access.common.settings.MessageHandlerForPluginSettingsRequestProcessor1_0;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.plugin.access.elastic.v1.ElasticAgentExtensionConverterV1;
import com.thoughtworks.go.plugin.access.elastic.v2.ElasticAgentExtensionConverterV2;
import com.thoughtworks.go.plugin.access.elastic.v3.ElasticAgentExtensionConverterV3;
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
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.ELASTIC_AGENT_EXTENSION;

@Component
public class ElasticAgentExtension extends AbstractExtension {
    private final Map<String, ElasticAgentMessageConverter> messageHandlerMap = new HashMap<>();

    @Autowired
    public ElasticAgentExtension(PluginManager pluginManager) {
        super(pluginManager, new PluginRequestHelper(pluginManager, SUPPORTED_VERSIONS, ELASTIC_AGENT_EXTENSION), ELASTIC_AGENT_EXTENSION);

        addHandler(ElasticAgentExtensionConverterV1.VERSION, new PluginSettingsJsonMessageHandler1_0(), new ElasticAgentExtensionConverterV1());
        addHandler(ElasticAgentExtensionConverterV2.VERSION, new PluginSettingsJsonMessageHandler1_0(), new ElasticAgentExtensionConverterV2());
        addHandler(ElasticAgentExtensionConverterV3.VERSION, new PluginSettingsJsonMessageHandler1_0(), new ElasticAgentExtensionConverterV3());

        registerProcessor(ElasticAgentExtensionConverterV1.VERSION, new MessageHandlerForPluginSettingsRequestProcessor1_0(), new MessageHandlerForServerInfoRequestProcessor1_0());
        registerProcessor(ElasticAgentExtensionConverterV2.VERSION, new MessageHandlerForPluginSettingsRequestProcessor1_0(), new MessageHandlerForServerInfoRequestProcessor1_0());
        registerProcessor(ElasticAgentExtensionConverterV3.VERSION, new MessageHandlerForPluginSettingsRequestProcessor1_0(), new MessageHandlerForServerInfoRequestProcessor1_0());
    }

    private void addHandler(String version, PluginSettingsJsonMessageHandler messageHandler, ElasticAgentMessageConverter extensionHandler) {
        registerHandler(version, messageHandler);
        messageHandlerMap.put(version, extensionHandler);
    }

    private void registerProcessor(String version, MessageHandlerForPluginSettingsRequestProcessor pluginSettingsRequestProcessor,
                                   MessageHandlerForServerInfoRequestProcessor serverInfoRequestProcessor) {
        registerMessageHandlerForPluginSettingsRequestProcessor(version, pluginSettingsRequestProcessor);
        registerMessageHandlerForServerInfoRequestProcessor(version, serverInfoRequestProcessor);
    }

    public void createAgent(String pluginId, final String autoRegisterKey, final String environment, final Map<String, String> configuration, JobIdentifier jobIdentifier) {
        pluginRequestHelper.submitRequest(pluginId, REQUEST_CREATE_AGENT, new DefaultPluginInteractionCallback<Void>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getElasticAgentMessageConverter(resolvedExtensionVersion).createAgentRequestBody(autoRegisterKey, environment, configuration, jobIdentifier);
            }
        });
    }

    public void serverPing(final String pluginId) {
        pluginRequestHelper.submitRequest(pluginId, REQUEST_SERVER_PING, new DefaultPluginInteractionCallback<Void>());
    }

    public boolean shouldAssignWork(String pluginId, final AgentMetadata agent, final String environment, final Map<String, String> configuration, JobIdentifier identifier) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_SHOULD_ASSIGN_WORK, new DefaultPluginInteractionCallback<Boolean>() {

            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getElasticAgentMessageConverter(resolvedExtensionVersion).shouldAssignWorkRequestBody(agent, environment, configuration, identifier);
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
                return getElasticAgentMessageConverter(resolvedExtensionVersion).getStatusReportView(responseBody);
            }
        });
    }

    public String getAgentStatusReport(String pluginId, JobIdentifier identifier, String elasticAgentId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_AGENT_STATUS_REPORT, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getElasticAgentMessageConverter(resolvedExtensionVersion).getAgentStatusReportRequestBody(identifier, elasticAgentId);
            }

            @Override
            public String onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getElasticAgentMessageConverter(resolvedExtensionVersion).getStatusReportView(responseBody);
            }
        });
    }

    public Capabilities getCapabilities(String pluginId) {
        final String resolvedExtensionVersion = pluginManager.resolveExtensionVersion(pluginId, SUPPORTED_VERSIONS);
        if (ElasticAgentExtensionConverterV1.VERSION.equals(resolvedExtensionVersion)) {
            return new Capabilities(false, false);
        }

        return pluginRequestHelper.submitRequest(pluginId, REQUEST_CAPABILTIES, new DefaultPluginInteractionCallback<Capabilities>() {
            @Override
            public Capabilities onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getElasticAgentMessageConverter(resolvedExtensionVersion).getCapabilitiesFromResponseBody(responseBody);
            }
        });
    }

    @Override
    protected List<String> goSupportedVersions() {
        return SUPPORTED_VERSIONS;
    }
}
