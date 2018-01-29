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

package com.thoughtworks.go.apiv1.admin.artifactstore.representers;

import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.representers.JsonWriter;
import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.spark.RequestContext;
import com.thoughtworks.go.spark.Routes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArtifactStoreRepresenter {
    private static void addLinks(ArtifactStore model, JsonWriter jsonWriter) {
        jsonWriter.addDocLink(Routes.ArtifactStores.DOC);
        jsonWriter.addLink("find", Routes.ArtifactStores.FIND);
        jsonWriter.addLink("self", Routes.ArtifactStores.name(model.getId()));
    }

    public static Map toJSON(ArtifactStore artifactStore, RequestContext requestContext) {
        if (artifactStore == null) {
            return null;
        }

        final JsonWriter jsonWriter = new JsonWriter(requestContext);
        addLinks(artifactStore, jsonWriter);
        return jsonWriter
                .add("id", artifactStore.getId())
                .add("plugin_id", artifactStore.getPluginId())
                .add("properties", ConfigurationPropertyRepresenter.toJSON(artifactStore, requestContext))
                .getAsMap();
    }

    public static ArtifactStore fromJSON(JsonReader jsonReader) {
        final ArtifactStore model = new ArtifactStore();
        if (jsonReader == null) {
            return model;
        }
        jsonReader.readStringIfPresent("id", model::setId);
        jsonReader.readStringIfPresent("plugin_id", model::setPluginId);
        jsonReader.readArrayIfPresent("properties", properties -> {
            List<ConfigurationProperty> configurationProperties = new ArrayList<>();
            properties.forEach(property -> {
                JsonReader configPropertyReader = new JsonReader(property.getAsJsonObject());
                ConfigurationProperty configurationProperty = ConfigurationPropertyRepresenter.fromJSON(configPropertyReader);
                configurationProperties.add(configurationProperty);
            });
            model.addConfigurations(configurationProperties);
        });
        return model;
    }
}
