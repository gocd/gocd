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

package com.thoughtworks.go.apiv1.pipelineselection.representers;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.server.domain.user.DashboardFilter;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

import static com.thoughtworks.go.server.domain.user.Marshaling.CaseInsensitiveStringSerializer;
import static com.thoughtworks.go.server.domain.user.Marshaling.DashboardFilterSerializer;

public class PipelineSelectionsRepresenter {
    private static Type GROUPS_TYPE = new TypeToken<List<PipelineConfigs>>() {}.getType();
    private static Gson GSON = new GsonBuilder().
            registerTypeAdapter(PipelineSelectionResponse.class, new PersonalizationResponseSerializer()).
            registerTypeAdapter(GROUPS_TYPE, new PipelineGroupsSerializer()).
            registerTypeAdapter(DashboardFilter.class, new DashboardFilterSerializer()).
            registerTypeAdapter(CaseInsensitiveString.class, new CaseInsensitiveStringSerializer()).
            create();

    public static String toJSON(PipelineSelectionResponse pipelineSelectionResponse) {
        return GSON.toJson(pipelineSelectionResponse, PipelineSelectionResponse.class);
    }

    private static class PersonalizationResponseSerializer implements JsonSerializer<PipelineSelectionResponse> {
        @Override
        public JsonElement serialize(PipelineSelectionResponse src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject serialized = new JsonObject();
            JsonArray filters = new JsonArray();
            src.filters().filters().forEach((f) -> {
                filters.add(context.serialize(f, DashboardFilter.class));
            });
            serialized.add("filters", filters);
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
