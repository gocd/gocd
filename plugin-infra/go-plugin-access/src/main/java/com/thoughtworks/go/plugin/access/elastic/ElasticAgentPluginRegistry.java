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
package com.thoughtworks.go.plugin.access.elastic;

import com.thoughtworks.go.domain.ClusterProfilesChangedStatus;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.plugin.access.common.AbstractPluginRegistry;
import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ElasticAgentPluginRegistry extends AbstractPluginRegistry<ElasticAgentExtension> {

    @Autowired
    public ElasticAgentPluginRegistry(PluginManager pluginManager, ElasticAgentExtension elasticAgentExtension) {
        super(pluginManager, elasticAgentExtension);
    }

    public void createAgent(final String pluginId, String autoRegisterKey, String environment, Map<String, String> configuration, Map<String, String> clusterProfileConfiguration, JobIdentifier jobIdentifier) {
        PluginDescriptor plugin = findPlugin(pluginId);
        if (plugin != null) {
            LOGGER.debug("Processing create agent for plugin: {} with environment: {} with elastic agent configuration: {} in cluster: {}", pluginId, environment, configuration, clusterProfileConfiguration);
            extension.createAgent(pluginId, autoRegisterKey, environment, configuration, clusterProfileConfiguration, jobIdentifier);
            LOGGER.debug("Done processing create agent for plugin: {} with environment: {} with elastic agent configuration: {} in cluster: {}", pluginId, environment, configuration, clusterProfileConfiguration);
        } else {
            LOGGER.warn("Could not find plugin with id: {}", pluginId);
        }
    }

    public void serverPing(String pluginId, List<Map<String, String>> clusterProfiles) {
        LOGGER.debug("Processing server ping for plugin {} with clusters {}", pluginId, clusterProfiles);
        extension.serverPing(pluginId, clusterProfiles);
        LOGGER.debug("Done processing server ping for plugin {} with clusters {}", pluginId, clusterProfiles);
    }

    public boolean shouldAssignWork(PluginDescriptor plugin, AgentMetadata agent, String environment, Map<String, String> configuration, Map<String, String> clusterProfileProperties, JobIdentifier identifier) {
        LOGGER.debug("Processing should assign work for plugin: {} with agent: {} with environment: {} with configuration: {} in cluster: {}", plugin.id(), agent, environment, configuration, clusterProfileProperties);
        boolean result = extension.shouldAssignWork(plugin.id(), agent, environment, configuration, clusterProfileProperties, identifier);
        LOGGER.debug("Done processing should assign work (result: {}) for plugin: {} with agent: {} with environment: {} with configuration {} in cluster: {}", result, plugin.id(), agent, environment, configuration, clusterProfileProperties);
        return result;
    }

    public String getPluginStatusReport(String pluginId, List<Map<String, String>> clusterProfiles) {
        LOGGER.debug("Processing get plugin status report for plugin: {} with clusters: {} ", pluginId, clusterProfiles);
        final String statusReportView = extension.getPluginStatusReport(pluginId, clusterProfiles);
        LOGGER.debug("Done processing get plugin status report for plugin: {} with clusters: {}", pluginId, clusterProfiles);
        return statusReportView;
    }

    public String getAgentStatusReport(String pluginId, JobIdentifier jobIdentifier, String elasticAgentId, Map<String, String> cluster) {
        LOGGER.debug("Processing get plugin status report for plugin: {} with job-identifier: {} with elastic-agent-id: {} and cluster: {}", pluginId, jobIdentifier, elasticAgentId, cluster);
        final String agentStatusReportView = extension.getAgentStatusReport(pluginId, jobIdentifier, elasticAgentId, cluster);
        LOGGER.debug("Done processing get plugin status report for plugin: {} with job-identifier: {} with elastic-agent-id: {} and cluster: {}", pluginId, jobIdentifier, elasticAgentId, cluster);
        return agentStatusReportView;
    }

    public String getClusterStatusReport(String pluginId, Map<String, String> clusterProfileConfigurations) {
        LOGGER.debug("Processing get cluster status report for plugin: {} for cluster: {}", pluginId, clusterProfileConfigurations);
        final String clusterStatusReportView = extension.getClusterStatusReport(pluginId, clusterProfileConfigurations);
        LOGGER.debug("Done processing get cluster status report for plugin: {} for cluster: {}", pluginId, clusterProfileConfigurations);
        return clusterStatusReportView;
    }

    public boolean has(String pluginId) {
        return findPlugin(pluginId) != null;
    }

    public void reportJobCompletion(String pluginId, String elasticAgentId, JobIdentifier jobIdentifier, Map<String, String> elasticProfileConfiguration, Map<String, String> clusterProfileConfiguration) {
        LOGGER.debug("Processing report job completion for plugin: {} for elasticAgentId: {} for job: {} with configuration: {} in cluster: {}", pluginId, elasticAgentId, jobIdentifier, elasticProfileConfiguration, clusterProfileConfiguration);
        extension.reportJobCompletion(pluginId, elasticAgentId, jobIdentifier, elasticProfileConfiguration, clusterProfileConfiguration);
        LOGGER.debug("Done processing report job completion for plugin: {} for elasticAgentId: {} for job: {} with configuration: {} in cluster: {}", pluginId, elasticAgentId, jobIdentifier, elasticProfileConfiguration, clusterProfileConfiguration);
    }

    public void notifyPluginAboutClusterProfileChanged(String pluginId, ClusterProfilesChangedStatus status, Map<String, String> oldClusterProfile, Map<String, String> newClusterProfile) {
        try {
            LOGGER.debug("Processing report cluster profile changed for plugin: {} with status: {} with old cluster: {} and new cluster: {} ", pluginId, status, oldClusterProfile, newClusterProfile);
            extension.clusterProfileChanged(pluginId, status, oldClusterProfile, newClusterProfile);
            LOGGER.debug("Done processing report cluster profile changed for plugin: {} with status: {} with old cluster: {} and new cluster: {} ", pluginId, status, oldClusterProfile, newClusterProfile);
        } catch (Exception e) {
            LOGGER.error("An error occurred while processing report cluster profile changed for plugin: {} with status: {} with old cluster: {} and new cluster: {}. Error: {}", pluginId, status, oldClusterProfile, newClusterProfile, e);
        }
    }

}
