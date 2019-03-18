/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.google.gson.Gson;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class JobAgentMetadata extends PersistentObject {
    private Long jobId;
    private String metadata;
    private String metadataVersion;

    private JobAgentMetadata() {

    }

    public JobAgentMetadata(long jobId, ElasticProfile profile) {
        this.jobId = jobId;
        this.metadata = toJSON(profile);
        this.metadataVersion = "1.0";
    }

    public ElasticProfile elasticProfile() {
        Gson gson = new Gson();
        Map map = gson.fromJson(metadata, LinkedHashMap.class);
        String pluginId = (String) map.get("pluginId");
        String clusterProfileId = (String) map.get("clusterProfileId");
        String id = (String) map.get("id");
        Map<String, String> properties = (Map<String, String>) map.get("properties");

        Collection<ConfigurationProperty> configProperties = properties.entrySet().stream().map(entry -> new ConfigurationProperty(new ConfigurationKey(entry.getKey()), new ConfigurationValue(entry.getValue()))).collect(Collectors.toList());

        return new ElasticProfile(id, pluginId, clusterProfileId, configProperties);
    }

    private static String toJSON(ElasticProfile elasticProfile) {
        Gson gson = new Gson();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("clusterProfileId", elasticProfile.getClusterProfileId());
        map.put("pluginId", elasticProfile.getPluginId());
        map.put("id", elasticProfile.getId());
        map.put("properties", elasticProfile.getConfigurationAsMap(true));
        return gson.toJson(map);
    }

}
