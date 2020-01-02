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
package com.thoughtworks.go.plugin.access.artifact.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class PublishArtifactResponse {
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    public PublishArtifactResponse() {
    }

    public PublishArtifactResponse(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Expose
    @SerializedName("metadata")
    private Map<String, Object> metadata;

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public static PublishArtifactResponse fromJSON(String json) {
        return GSON.fromJson(json, PublishArtifactResponse.class);
    }
}
