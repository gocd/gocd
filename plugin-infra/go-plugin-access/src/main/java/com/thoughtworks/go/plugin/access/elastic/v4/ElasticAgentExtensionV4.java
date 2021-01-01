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
package com.thoughtworks.go.plugin.access.elastic.v4;

import com.thoughtworks.go.domain.ClusterProfilesChangedStatus;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.plugin.access.DefaultPluginInteractionCallback;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.elastic.VersionedElasticAgentExtension;
import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.plugin.access.elastic.models.ElasticAgentInformation;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.elastic.Capabilities;

import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.plugin.access.elastic.v4.ElasticAgentPluginConstantsV4.*;

public class ElasticAgentExtensionV4 implements VersionedElasticAgentExtension {
    public static final String VERSION = "4.0";
    private final PluginRequestHelper pluginRequestHelper;
    private final ElasticAgentExtensionConverterV4 elasticAgentExtensionConverterV4;

    public ElasticAgentExtensionV4(PluginRequestHelper pluginRequestHelper) {
        this.pluginRequestHelper = pluginRequestHelper;
        this.elasticAgentExtensionConverterV4 = new ElasticAgentExtensionConverterV4();
    }

    @Override
    public com.thoughtworks.go.plugin.domain.common.Image getIcon(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_PLUGIN_SETTINGS_ICON, new DefaultPluginInteractionCallback<com.thoughtworks.go.plugin.domain.common.Image>() {
            @Override
            public com.thoughtworks.go.plugin.domain.common.Image onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV4.getImageResponseFromBody(responseBody);
            }
        });
    }

    @Override
    public Capabilities getCapabilities(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_CAPABILITIES, new DefaultPluginInteractionCallback<Capabilities>() {
            @Override
            public Capabilities onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV4.getCapabilitiesFromResponseBody(responseBody);
            }
        });
    }

    @Override
    public List<PluginConfiguration> getElasticProfileMetadata(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_PROFILE_METADATA, new DefaultPluginInteractionCallback<List<PluginConfiguration>>() {
            @Override
            public List<PluginConfiguration> onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV4.getElasticProfileMetadataResponseFromBody(responseBody);
            }
        });
    }

    @Override
    public String getElasticProfileView(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_PROFILE_VIEW, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV4.getProfileViewResponseFromBody(responseBody);
            }
        });
    }

    @Override
    public boolean supportsClusterProfile() {
        return false;
    }

    @Override
    public ValidationResult validateElasticProfile(final String pluginId, final Map<String, String> configuration) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_VALIDATE_PROFILE, new DefaultPluginInteractionCallback<ValidationResult>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV4.validateElasticProfileRequestBody(configuration);
            }

            @Override
            public ValidationResult onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV4.getElasticProfileValidationResultResponseFromBody(responseBody);
            }
        });
    }

    @Override
    public List<PluginConfiguration> getClusterProfileMetadata(String pluginId) {
        throw new UnsupportedOperationException(String.format("Plugin: '%s' uses elastic agent extension v4 and cluster profile extension calls are not supported by elastic agent V4", pluginId));
    }

    @Override
    public String getClusterProfileView(String pluginId) {
        throw new UnsupportedOperationException(String.format("Plugin: '%s' uses elastic agent extension v4 and cluster profile extension calls are not supported by elastic agent V4", pluginId));
    }

    @Override
    public ValidationResult validateClusterProfile(String pluginId, Map<String, String> configuration) {
        throw new UnsupportedOperationException(String.format("Plugin: '%s' uses elastic agent extension v4 and cluster profile extension calls are not supported by elastic agent V4", pluginId));
    }

    @Override
    public void createAgent(String pluginId, final String autoRegisterKey, final String environment, final Map<String, String> configuration, Map<String, String> clusterProfileConfiguration, JobIdentifier jobIdentifier) {
        pluginRequestHelper.submitRequest(pluginId, REQUEST_CREATE_AGENT, new DefaultPluginInteractionCallback<Void>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV4.createAgentRequestBody(autoRegisterKey, environment, configuration, jobIdentifier);
            }
        });
    }

    @Override
    public void serverPing(final String pluginId, List<Map<String, String>> clusterProfileConfigurations) {
        pluginRequestHelper.submitRequest(pluginId, REQUEST_SERVER_PING, new DefaultPluginInteractionCallback<Void>());
    }

    @Override
    public boolean shouldAssignWork(String pluginId, final AgentMetadata agent, final String environment, final Map<String, String> configuration, Map<String, String> clusterProfileProperties, JobIdentifier identifier) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_SHOULD_ASSIGN_WORK, new DefaultPluginInteractionCallback<Boolean>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV4.shouldAssignWorkRequestBody(agent, environment, configuration, identifier);
            }

            @Override
            public Boolean onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV4.shouldAssignWorkResponseFromBody(responseBody);
            }
        });
    }

    @Override
    public String getPluginStatusReport(String pluginId, List<Map<String, String>> clusterProfiles) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_STATUS_REPORT, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV4.getStatusReportView(responseBody);
            }
        });
    }

    @Override
    public String getAgentStatusReport(String pluginId, JobIdentifier identifier, String elasticAgentId, Map<String, String> clusterProfile) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_AGENT_STATUS_REPORT, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV4.getAgentStatusReportRequestBody(identifier, elasticAgentId);
            }

            @Override
            public String onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV4.getStatusReportView(responseBody);
            }
        });
    }

    @Override
    public String getClusterStatusReport(String pluginId, Map<String, String> clusterProfile) {
        throw new UnsupportedOperationException(String.format("Plugin: '%s' uses elastic agent extension v4 and cluster profile extension calls are not supported by elastic agent V4", pluginId));
    }

    @Override
    public void jobCompletion(String pluginId, String elasticAgentId, JobIdentifier jobIdentifier, Map<String, String> elasticProfileConfiguration, Map<String, String> clusterProfileConfiguration) {
        pluginRequestHelper.submitRequest(pluginId, REQUEST_JOB_COMPLETION, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV4.getJobCompletionRequestBody(elasticAgentId, jobIdentifier);
            }
        });
    }

    @Override
    public ElasticAgentInformation migrateConfig(String pluginId, ElasticAgentInformation elasticAgentInformation) {
        return elasticAgentInformation;
    }

    @Override
    public void clusterProfilesChanged(String pluginId, ClusterProfilesChangedStatus status, Map<String, String> oldClusterProfile, Map<String, String> newClusterProfile) {
        throw new UnsupportedOperationException(String.format("Plugin: '%s' uses elastic agent extension v4 and cluster profile extension calls are not supported by elastic agent V4", pluginId));
    }
}
