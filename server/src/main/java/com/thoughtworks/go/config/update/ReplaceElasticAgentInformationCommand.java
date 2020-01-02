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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.UpdateConfigCommand;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.plugin.access.elastic.models.ElasticAgentInformation;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.service.ClusterProfilesService;
import com.thoughtworks.go.server.service.ElasticProfileService;

import java.util.HashMap;
import java.util.List;

public class ReplaceElasticAgentInformationCommand implements UpdateConfigCommand {
    private final ClusterProfilesService clusterProfilesService;
    private final ElasticProfileService elasticProfileService;
    private final ElasticAgentExtension elasticAgentExtension;
    private final GoPluginDescriptor pluginDescriptor;
    private final HashMap<String, String> pluginSettings;

    public ReplaceElasticAgentInformationCommand(ClusterProfilesService clusterProfilesService, ElasticProfileService elasticProfileService, ElasticAgentExtension elasticAgentExtension, GoPluginDescriptor pluginDescriptor, HashMap<String, String> pluginSettings) {
        this.clusterProfilesService = clusterProfilesService;
        this.elasticProfileService = elasticProfileService;
        this.elasticAgentExtension = elasticAgentExtension;
        this.pluginDescriptor = pluginDescriptor;
        this.pluginSettings = pluginSettings;
    }

    @Override
    public CruiseConfig update(CruiseConfig preprocessedConfig) throws Exception {
        String pluginId = pluginDescriptor.id();

        List<ClusterProfile> clusterProfiles = clusterProfilesService.getPluginProfiles().findByPluginId(pluginId);
        List<ElasticProfile> elasticAgentProfiles = elasticProfileService.findElasticAgentProfilesByPluginId(pluginId);

        ElasticAgentInformation elasticAgentInformation = new ElasticAgentInformation(pluginSettings, clusterProfiles, elasticAgentProfiles);

        ElasticAgentInformation migratedElasticAgentInformation = elasticAgentExtension.migrateConfig(pluginId, elasticAgentInformation);

        List<ClusterProfile> migratedClusterProfiles = migratedElasticAgentInformation.getClusterProfiles();
        List<ElasticProfile> migratedElasticAgentProfiles = migratedElasticAgentInformation.getElasticAgentProfiles();

        preprocessedConfig.getElasticConfig().getClusterProfiles().removeAll(clusterProfiles);
        preprocessedConfig.getElasticConfig().getClusterProfiles().addAll(migratedClusterProfiles);

        preprocessedConfig.getElasticConfig().getProfiles().removeAll(elasticAgentProfiles);
        preprocessedConfig.getElasticConfig().getProfiles().addAll(migratedElasticAgentProfiles);

        return preprocessedConfig;
    }
}
