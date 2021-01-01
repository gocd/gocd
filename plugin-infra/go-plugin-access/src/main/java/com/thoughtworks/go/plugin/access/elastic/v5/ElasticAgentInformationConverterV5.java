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
package com.thoughtworks.go.plugin.access.elastic.v5;

import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.plugin.access.elastic.DataConverter;
import com.thoughtworks.go.plugin.access.elastic.models.ElasticAgentInformation;

import java.util.List;
import java.util.stream.Collectors;

class ElasticAgentInformationConverterV5 implements DataConverter<ElasticAgentInformation, ElasticAgentInformationDTO> {
    @Override
    public ElasticAgentInformation fromDTO(ElasticAgentInformationDTO elasticAgentInformationDTO) {
        return new ElasticAgentInformation(elasticAgentInformationDTO.getPluginSettings(), elasticAgentInformationDTO.getClusterProfiles(), elasticAgentInformationDTO.getElasticAgentProfiles());
    }

    @Override
    public ElasticAgentInformationDTO toDTO(ElasticAgentInformation elasticAgentInformation) {
        return new ElasticAgentInformationDTO(elasticAgentInformation.getPluginSettings(),
                elasticAgentInformation.getClusterProfiles().stream().map(clusterProfile -> new ClusterProfileDTO(clusterProfile.getId(), clusterProfile.getPluginId(), clusterProfile.getConfigurationAsMap(true))).collect(Collectors.toList()),
                elasticAgentInformation.getElasticAgentProfiles().stream().map(elasticProfile -> new ElasticProfileDTO(elasticProfile.getId(), findPluginIdFromReferencedCluster(elasticAgentInformation.getClusterProfiles(), elasticProfile), elasticProfile.getClusterProfileId(), elasticProfile.getConfigurationAsMap(true))).collect(Collectors.toList()));
    }

    private String findPluginIdFromReferencedCluster(List<ClusterProfile> clusterProfiles, ElasticProfile elasticProfile) {
        for (ClusterProfile clusterProfile : clusterProfiles) {
            if (clusterProfile.getId().equals(elasticProfile.getClusterProfileId())) {
                return clusterProfile.getPluginId();
            }
        }

        throw new RuntimeException(String.format("No Cluster Profile exists with the specified cluster_profile_id '%s' for Elastic Agent Profile '%s'.", elasticProfile.getClusterProfileId(), elasticProfile.getId()));
    }
}
