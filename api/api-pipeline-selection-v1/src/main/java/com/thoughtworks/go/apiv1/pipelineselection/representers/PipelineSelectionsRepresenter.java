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

import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.representers.JsonWriter;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import com.thoughtworks.go.spark.RequestContext;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PipelineSelectionsRepresenter {
    public static Map<String, Object> toJSON(PipelineSelectionResponse pipelineSelectionResponse, RequestContext requestContext) {

        return new JsonWriter(requestContext)
                .add("selections", pipelineSelectionResponse.getSelectedPipelines().pipelineList())
                .add("blacklist", pipelineSelectionResponse.getSelectedPipelines().isBlacklist())
                .add("pipelines", pipelines(pipelineSelectionResponse, requestContext))
                .getAsMap();
    }

    private static Map<String, Object> pipelines(PipelineSelectionResponse pipelineSelectionResponse, RequestContext requestContext) {
        JsonWriter jsonWriter = new JsonWriter(requestContext);

        pipelineSelectionResponse.getPipelineConfigs().forEach(pipelineConfigs -> {
            List<String> pipelineNames = pipelineConfigs.getPipelines().stream().map(PipelineConfig::getName).map(CaseInsensitiveString::toString).collect(Collectors.toList());
            jsonWriter.add(pipelineConfigs.getGroup(), pipelineNames);
        });

        return jsonWriter.getAsMap();
    }

    public static PipelineSelectionResponse fromJSON(JsonReader reader) {
        List<String> selections = reader.readStringArrayIfPresent("selections").orElse(Collections.emptyList());
        Boolean blacklist = reader.optBoolean("blacklist").orElse(true);

        return new PipelineSelectionResponse(new PipelineSelections(selections, new Date(), -1L, blacklist), null);
    }
}
