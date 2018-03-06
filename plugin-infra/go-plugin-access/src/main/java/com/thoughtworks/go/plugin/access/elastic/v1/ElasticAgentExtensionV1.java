/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.elastic.v1;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.plugin.access.DefaultPluginInteractionCallback;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.elastic.VersionedElasticAgentExtension;
import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.elastic.Capabilities;

import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.plugin.access.elastic.v1.ElasticAgentPluginConstantsV1.*;

public class ElasticAgentExtensionV1 implements VersionedElasticAgentExtension {
    public static final String VERSION = "1.0";
    private final PluginRequestHelper pluginRequestHelper;
    private final ElasticAgentExtensionConverterV1 elasticAgentExtensionConverterV1;

    public ElasticAgentExtensionV1(PluginRequestHelper pluginRequestHelper) {
        this.pluginRequestHelper = pluginRequestHelper;
        this.elasticAgentExtensionConverterV1 = new ElasticAgentExtensionConverterV1();
    }

    @Override
    public com.thoughtworks.go.plugin.domain.common.Image getIcon(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_PLUGIN_SETTINGS_ICON, new DefaultPluginInteractionCallback<com.thoughtworks.go.plugin.domain.common.Image>() {
            @Override
            public com.thoughtworks.go.plugin.domain.common.Image onSuccess(String responseBody, String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV1.getImageResponseFromBody(responseBody);
            }
        });
    }

    @Override
    public Capabilities getCapabilities(String pluginId) {
        return new Capabilities(false, false);
    }

    @Override
    public List<PluginConfiguration> getElasticProfileMetadata(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_PROFILE_METADATA, new DefaultPluginInteractionCallback<List<PluginConfiguration>>() {
            @Override
            public List<PluginConfiguration> onSuccess(String responseBody, String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV1.getElasticProfileMetadataResponseFromBody(responseBody);
            }
        });
    }

    @Override
    public String getElasticProfileView(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_PROFILE_VIEW, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String onSuccess(String responseBody, String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV1.getProfileViewResponseFromBody(responseBody);
            }
        });
    }

    @Override
    public ValidationResult validateElasticProfile(final String pluginId, final Map<String, String> configuration) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_VALIDATE_PROFILE, new DefaultPluginInteractionCallback<ValidationResult>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV1.validateElasticProfileRequestBody(configuration);
            }

            @Override
            public ValidationResult onSuccess(String responseBody, String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV1.getElasticProfileValidationResultResponseFromBody(responseBody);
            }
        });
    }

    @Override
    public void createAgent(String pluginId, final String autoRegisterKey, final String environment, final Map<String, String> configuration, JobIdentifier jobIdentifier) {
        pluginRequestHelper.submitRequest(pluginId, REQUEST_CREATE_AGENT, new DefaultPluginInteractionCallback<Void>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV1.createAgentRequestBody(autoRegisterKey, environment, configuration, jobIdentifier);
            }
        });
    }

    @Override
    public void serverPing(final String pluginId) {
        pluginRequestHelper.submitRequest(pluginId, REQUEST_SERVER_PING, new DefaultPluginInteractionCallback<Void>());
    }

    @Override
    public boolean shouldAssignWork(String pluginId, final AgentMetadata agent, final String environment, final Map<String, String> configuration, JobIdentifier identifier) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_SHOULD_ASSIGN_WORK, new DefaultPluginInteractionCallback<Boolean>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV1.shouldAssignWorkRequestBody(agent, environment, configuration, identifier);
            }

            @Override
            public Boolean onSuccess(String responseBody, String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV1.shouldAssignWorkResponseFromBody(responseBody);
            }
        });
    }

    @Override
    public String getPluginStatusReport(String pluginId) {
        throw new UnsupportedOperationException(String.format("Plugin status report is not supported in elastic agent extension %s.", VERSION));
    }

    @Override
    public String getAgentStatusReport(String pluginId, JobIdentifier identifier, String elasticAgentId) {
        throw new UnsupportedOperationException(String.format("Agent status report is not supported in elastic agent extension %s.", VERSION));
    }
}
