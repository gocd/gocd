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

import com.thoughtworks.go.api.representers.JsonWriter;
import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.spark.RequestContext;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ArtifactStoresRepresenter {
    private static JsonWriter addLinks(JsonWriter jsonWriter) {
        return jsonWriter.addLink("self", "/go/api/admin/artifact_stores")
                .addDocLink("https://api.gocd.org/#artifact-stores")
                .addLink("find", "/go/api/admin/artifact_stores/:storeId");
    }

    public static Map toJSON(List<ArtifactStore> artifactStores, RequestContext requestContext) {
        List<Map> rolesArray = artifactStores.stream()
                .map(role -> ArtifactStoreRepresenter.toJSON(role, requestContext))
                .collect(Collectors.toList());

        return addLinks(new JsonWriter(requestContext))
                .addEmbedded("artifact_stores", rolesArray)
                .getAsMap();
    }
}
