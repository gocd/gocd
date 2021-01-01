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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.config.builder.ConfigurationPropertyBuilder;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.domain.config.ConfigurationProperty;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class ConfigurationPropertyDTO {
    @Expose
    @SerializedName("id")
    private String id;

    @Expose
    @SerializedName("plugin_id")
    private String pluginId;

    @Expose
    @SerializedName("properties")
    private Map<String, Map<String, Object>> properties;

    public ConfigurationPropertyDTO() {
    }

    public ConfigurationPropertyDTO(String id, String pluginId, Map<String, Map<String, Object>> properties) {
        this.id = id;
        this.pluginId = pluginId;
        this.properties = properties;
    }

    public String getId() {
        return id;
    }

    public String getPluginId() {
        return pluginId;
    }

    public Map<String, Map<String, Object>> getProperties() {
        return properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigurationPropertyDTO that = (ConfigurationPropertyDTO) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(pluginId, that.pluginId) &&
                Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, pluginId, properties);
    }

    @Override
    public String toString() {
        return "ClusterProfileDTO{" +
                "id='" + id + '\'' +
                ", pluginId='" + pluginId + '\'' +
                ", properties=" + properties +
                '}';
    }

    public ClusterProfile toDomainModel() {
        ArrayList<ConfigurationProperty> configurationProperties = new ArrayList<>();

        ConfigurationPropertyBuilder builder = new ConfigurationPropertyBuilder();
        this.properties.forEach((key, valueObject) -> {
            boolean isSecure = (boolean) valueObject.get("isSecure");
            String value = isSecure ? null : (String) valueObject.get("value");
            String encryptedValue = isSecure ? (String) valueObject.get("value") : null;

            configurationProperties.add(builder.create(key, value, encryptedValue, isSecure));
        });

        return new ClusterProfile(this.id, this.pluginId, configurationProperties);
    }
}
