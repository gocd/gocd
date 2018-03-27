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

package com.thoughtworks.go.apiv1.artifactstoreconfig.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ConfigurationPropertyRepresenter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.domain.config.ConfigurationProperty;

import java.util.ArrayList;
import java.util.List;

public class ArtifactStoreRepresenter {
    public static void toJSON(OutputWriter outputWriter, ArtifactStore store) {
        outputWriter
                .add("id", store.getId())
                .add("plugin_id", store.getPluginId())
                .addChildList("properties", listWriter ->
                        store.forEach(property ->
                                listWriter.addChild(propertyWriter ->
                                        ConfigurationPropertyRepresenter.toJSON(propertyWriter, property))));

    }

    public static ArtifactStore fromJSON(JsonReader jsonReader) {
        List<ConfigurationProperty> configurationProperties = readConfigurationProperties(jsonReader);
        return new ArtifactStore(
                jsonReader.getString("id"),
                jsonReader.getString("plugin_id"),
                configurationProperties.toArray(new ConfigurationProperty[]{}));

    }

    private static List<ConfigurationProperty> readConfigurationProperties(JsonReader jsonReader) {
        List<ConfigurationProperty> configurationProperties = new ArrayList<>();
        jsonReader.readArrayIfPresent("properties", properties -> {
            properties.forEach(property ->
                    configurationProperties.add(ConfigurationPropertyRepresenter.fromJSON(
                            new JsonReader(property.getAsJsonObject()))));
        });
        return configurationProperties;
    }
}
