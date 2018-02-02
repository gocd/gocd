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

package com.thoughtworks.go.apiv2.dashboard.representers;

import com.thoughtworks.go.api.representers.JsonWriter;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.spark.RequestContext;
import com.thoughtworks.go.spark.Routes;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class PipelineInstanceRepresenter {

    private static JsonWriter addLinks(JsonWriter jsonWriter, PipelineInstanceModel model) {
        return jsonWriter
                .addLink("self", Routes.Pipeline.instance(model.getName(), model.getCounter()))
                .addLink("compare_url", Routes.PipelineInstance.compare(model.getName(), model.getCounter() - 1, model.getCounter()))
                .addLink("history_url", Routes.Pipeline.history(model.getName()))
                .addLink("vsm_url", Routes.PipelineInstance.vsm(model.getName(), model.getCounter()));
    }


    public static Map toJSON(PipelineInstanceModel model, RequestContext requestContext) {
        return addLinks(new JsonWriter(requestContext), model)
                .add("label", model.getLabel())
                .add("counter", model.getCounter())
                .add("triggered_by", model.getApprovedByForDisplay())
                .add("scheduled_at", model.getScheduledDate())
                .add("build_cause", BuildCauseRepresenter.toJSON(model.getBuildCause(), requestContext))
                .addEmbedded("stages", getStages(model, requestContext))
                .add("_embedded", getStages(model, requestContext))
                .getAsMap();
    }

    private static List<Map> getStages(PipelineInstanceModel model, RequestContext requestContext) {
        return model.getStageHistory().stream()
                .map(stage -> StageRepresenter.toJSON(stage, requestContext, model.getName(), String.valueOf(model.getCounter())))
                .collect(toList());
    }

}
