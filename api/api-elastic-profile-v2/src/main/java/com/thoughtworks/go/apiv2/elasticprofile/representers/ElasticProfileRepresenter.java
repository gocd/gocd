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
package com.thoughtworks.go.apiv2.elasticprofile.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ConfigurationPropertyRepresenter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.spark.Routes;

import java.util.Collections;
import java.util.Map;

public class ElasticProfileRepresenter {
    public static void toJSON(OutputWriter outputWriter, ElasticProfile elasticProfile) {
        outputWriter
                .addLinks(linksWriter -> linksWriter
                        .addLink("self", Routes.ElasticProfileAPI.id(elasticProfile.getId()))
                        .addAbsoluteLink("doc", Routes.ElasticProfileAPI.DOC)
                        .addLink("find", Routes.ElasticProfileAPI.find()))
                .add("id", elasticProfile.getId())
                .add("cluster_profile_id", elasticProfile.getClusterProfileId())
                .addChildList("properties", listWriter ->
                        elasticProfile.forEach(property ->
                                listWriter.addChild(propertyWriter ->
                                        ConfigurationPropertyRepresenter.toJSON(propertyWriter, property))));
        if (elasticProfile.hasErrors()) {
            Map<String, String> fieldMapping = Collections.singletonMap("pluginId", "plugin_id");
            outputWriter.addChild("errors", errorWriter -> new ErrorGetter(fieldMapping).toJSON(errorWriter, elasticProfile));
        }
    }

    public static ElasticProfile fromJSON(JsonReader jsonReader) {
        ElasticProfile elasticProfile = new ElasticProfile(
                jsonReader.getString("id"),
                jsonReader.getString("cluster_profile_id"));
        elasticProfile.addConfigurations(ConfigurationPropertyRepresenter.fromJSONArray(jsonReader, "properties"));
        return elasticProfile;
    }
}
