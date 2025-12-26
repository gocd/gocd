/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.domain;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class JobAgentMetadata extends PersistentObject {
    private static final Gson GSON = new GsonBuilder()
        .serializeNulls()
        .setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();

    private long jobId;
    private String elasticAgentProfileMetadata;
    private String clusterProfileMetadata;
    private String metadataVersion;

    private JobAgentMetadata() {
    }

    public JobAgentMetadata(long jobId, ElasticProfile profile, ClusterProfile clusterProfile) {
        this.jobId = jobId;
        this.elasticAgentProfileMetadata = jsonizeElasticAgentProfile(profile, clusterProfile.getPluginId());
        this.clusterProfileMetadata = jsonizeClusterProfile(clusterProfile);
        this.metadataVersion = "2.0";
    }

    @SuppressWarnings("unchecked")
    public ElasticProfile elasticProfile() {
        Map<String, Object> map = GSON.<Map<String, Object>>fromJson(elasticAgentProfileMetadata, LinkedHashMap.class);

        return new ElasticProfile(
            (String) map.get("id"),
            (String) map.get("clusterProfileId"),
            propertiesCollectionFor((Map<String, String>) map.get("properties")));
    }

    @SuppressWarnings("unchecked")
    public ClusterProfile clusterProfile() {
        Map<String, Object> map = GSON.<Map<String, Object>>fromJson(clusterProfileMetadata, LinkedHashMap.class);

        if (map == null || map.isEmpty()) {
            return null;
        }

        return new ClusterProfile(
            (String) map.get("id"),
            (String) map.get("pluginId"),
            propertiesCollectionFor((Map<String, String>) map.get("properties")));
    }

    private static @NotNull Collection<ConfigurationProperty> propertiesCollectionFor(Map<String, String> properties) {
        return properties.entrySet()
            .stream()
            .map(entry -> new ConfigurationProperty(new ConfigurationKey(entry.getKey()), new ConfigurationValue(entry.getValue())))
            .collect(Collectors.toList());
    }

    private static String jsonizeElasticAgentProfile(ElasticProfile elasticProfile, String pluginId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("clusterProfileId", elasticProfile.getClusterProfileId());
        //todo: elastic agent extension v6 should remove plugin id.
        map.put("pluginId", pluginId);
        map.put("id", elasticProfile.getId());
        map.put("properties", elasticProfile.getConfigurationAsMap(true));
        return GSON.toJson(map);
    }

    private static String jsonizeClusterProfile(ClusterProfile clusterProfile) {
        Map<String, Object> map = new LinkedHashMap<>();

        if (clusterProfile != null) {
            map.put("pluginId", clusterProfile.getPluginId());
            map.put("id", clusterProfile.getId());
            map.put("properties", clusterProfile.getConfigurationAsMap(true));
        }

        return GSON.toJson(map);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        JobAgentMetadata that = (JobAgentMetadata) o;
        return jobId == that.jobId &&
                Objects.equals(elasticAgentProfileMetadata, that.elasticAgentProfileMetadata) &&
                Objects.equals(clusterProfileMetadata, that.clusterProfileMetadata) &&
                Objects.equals(metadataVersion, that.metadataVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), jobId, elasticAgentProfileMetadata, clusterProfileMetadata, metadataVersion);
    }
}
