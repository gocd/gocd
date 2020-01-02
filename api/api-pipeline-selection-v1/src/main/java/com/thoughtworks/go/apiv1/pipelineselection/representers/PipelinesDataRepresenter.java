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
package com.thoughtworks.go.apiv1.pipelineselection.representers;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.server.domain.user.Marshaling;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

public class PipelinesDataRepresenter {
    private static Type GROUPS_TYPE = new TypeToken<List<PipelineConfigs>>() {}.getType();
    private static Gson GSON = new GsonBuilder().
            registerTypeAdapter(PipelinesDataResponse.class, new PersonalizationResponseSerializer()).
            registerTypeAdapter(GROUPS_TYPE, new PipelineGroupsSerializer()).
            registerTypeAdapter(CaseInsensitiveString.class, new Marshaling.CaseInsensitiveStringSerializer()).
            create();

    public static String toJSON(PipelinesDataResponse pipelineSelectionResponse) {
        return GSON.toJson(pipelineSelectionResponse, PipelinesDataResponse.class);
    }

    private static class PersonalizationResponseSerializer implements JsonSerializer<PipelinesDataResponse> {
        @Override
        public JsonElement serialize(PipelinesDataResponse src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject serialized = new JsonObject();
            serialized.add("pipelines", context.serialize(src.getPipelineConfigs(), GROUPS_TYPE));
            return serialized;
        }
    }

    private static class PipelineGroupsSerializer implements JsonSerializer<List<PipelineConfigs>> {
        @Override
        public JsonElement serialize(List<PipelineConfigs> src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject serialized = new JsonObject();
            for (PipelineConfigs group : src) {
                final List<PipelineConfig> pipelines = group.getPipelines();
                if (!pipelines.isEmpty()) {
                    List<CaseInsensitiveString> pipelineNames = pipelines.stream().map(PipelineConfig::name).collect(Collectors.toList());
                    serialized.add(group.getGroup(), context.serialize(pipelineNames));
                }
            }
            return serialized;
        }
    }
}
