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

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

class ElasticAgentInformationDTO implements Serializable {
    private static final Gson GSON = new GsonBuilder().
            excludeFieldsWithoutExposeAnnotation().
            serializeNulls().
            setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).
            create();

    @Expose
    @SerializedName("plugin_settings")
    private final Map<String, String> pluginSettings;

    @Expose
    @SerializedName("cluster_profiles")
    private final List<ClusterProfileDTO> clusterProfilesDTO;

    @Expose
    @SerializedName("elastic_agent_profiles")
    private final List<ElasticProfileDTO> elasticAgentProfilesDTO;

    public ElasticAgentInformationDTO(Map<String, String> pluginSettings, List<ClusterProfileDTO> clusterProfilesDTO, List<ElasticProfileDTO> elasticAgentProfilesDTO) {
        this.pluginSettings = pluginSettings;
        this.clusterProfilesDTO = clusterProfilesDTO;
        this.elasticAgentProfilesDTO = elasticAgentProfilesDTO;
    }

    public JsonElement toJSON() {
        return GSON.toJsonTree(this);
    }

    public Map<String, String> getPluginSettings() {
        return pluginSettings;
    }

    public List<ClusterProfile> getClusterProfiles() {
        return clusterProfilesDTO.stream().map(ClusterProfileDTO::toDomainModel).collect(Collectors.toList());
    }

    public List<ElasticProfile> getElasticAgentProfiles() {
        return elasticAgentProfilesDTO.stream().map(ElasticProfileDTO::toDomainModel).collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ElasticAgentInformationDTO that = (ElasticAgentInformationDTO) o;
        return Objects.equals(pluginSettings, that.pluginSettings) &&
                Objects.equals(clusterProfilesDTO, that.clusterProfilesDTO) &&
                Objects.equals(elasticAgentProfilesDTO, that.elasticAgentProfilesDTO);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pluginSettings, clusterProfilesDTO, elasticAgentProfilesDTO);
    }

    @Override
    public String toString() {
        return "ElasticAgentInformationDTO{" +
                "pluginSettings=" + pluginSettings +
                ", clusterProfilesDTO=" + clusterProfilesDTO +
                ", elasticAgentProfilesDTO=" + elasticAgentProfilesDTO +
                '}';
    }
}
