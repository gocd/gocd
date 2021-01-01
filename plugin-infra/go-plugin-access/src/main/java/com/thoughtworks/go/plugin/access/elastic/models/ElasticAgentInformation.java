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
package com.thoughtworks.go.plugin.access.elastic.models;

import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ElasticAgentInformation {
    private final Map<String, String> pluginSettings;
    private final List<ClusterProfile> clusterProfiles;
    private final List<ElasticProfile> elasticAgentProfiles;

    public ElasticAgentInformation(Map<String, String> pluginSettings, List<ClusterProfile> clusterProfiles, List<ElasticProfile> elasticAgentProfiles) {
        this.pluginSettings = pluginSettings;
        this.clusterProfiles = clusterProfiles;
        this.elasticAgentProfiles = elasticAgentProfiles;
    }

    public Map<String, String> getPluginSettings() {
        return pluginSettings;
    }

    public List<ClusterProfile> getClusterProfiles() {
        return clusterProfiles;
    }

    public List<ElasticProfile> getElasticAgentProfiles() {
        return elasticAgentProfiles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ElasticAgentInformation that = (ElasticAgentInformation) o;
        return Objects.equals(pluginSettings, that.pluginSettings) &&
                Objects.equals(clusterProfiles, that.clusterProfiles) &&
                Objects.equals(elasticAgentProfiles, that.elasticAgentProfiles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pluginSettings, clusterProfiles, elasticAgentProfiles);
    }

    @Override
    public String toString() {
        return "ElasticAgentInformation{" +
                "pluginSettings=" + pluginSettings +
                ", clusterProfiles=" + clusterProfiles +
                ", elasticAgentProfiles=" + elasticAgentProfiles +
                '}';
    }
}
