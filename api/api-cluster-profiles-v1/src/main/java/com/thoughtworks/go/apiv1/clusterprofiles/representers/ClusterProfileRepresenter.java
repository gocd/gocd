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
package com.thoughtworks.go.apiv1.clusterprofiles.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ConfigurationPropertyRepresenter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.spark.Routes;

import java.util.Collections;
import java.util.Map;

public class ClusterProfileRepresenter {
    public static void toJSON(OutputWriter outputWriter, ClusterProfile clusterProfile) {
        outputWriter
                .addLinks(linksWriter -> linksWriter
                        .addLink("self", Routes.ClusterProfilesAPI.id(clusterProfile.getId()))
                        .addAbsoluteLink("doc", Routes.ClusterProfilesAPI.DOC)
                        .addLink("find", Routes.ClusterProfilesAPI.find()))
                .add("id", clusterProfile.getId())
                .add("plugin_id", clusterProfile.getPluginId())
                .addChildList("properties", listWriter ->
                        clusterProfile.forEach(property -> listWriter.addChild(propertyWriter -> ConfigurationPropertyRepresenter.toJSON(propertyWriter, property))));

        if (clusterProfile.hasErrors()) {
            Map<String, String> fieldMapping = Collections.singletonMap("pluginId", "plugin_id");
            outputWriter.addChild("errors", errorWriter -> new ErrorGetter(fieldMapping).toJSON(errorWriter, clusterProfile));
        }
    }

    public static ClusterProfile fromJSON(JsonReader jsonReader) {
        ClusterProfile clusterProfile = new ClusterProfile(
                jsonReader.getString("id"),
                jsonReader.getString("plugin_id"));
        clusterProfile.addConfigurations(ConfigurationPropertyRepresenter.fromJSONArray(jsonReader, "properties"));

        return clusterProfile;
    }
}
