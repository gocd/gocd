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
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel;
import com.thoughtworks.go.spark.RequestContext;
import com.thoughtworks.go.spark.Routes;

import java.util.Map;

public class StageRepresenter {

    private static final String SELF_HREF = "/api/stages/${pipeline_name}/${pipeline_counter}/${stage_name}/${stage_counter}";

    public static Map toJSON(StageInstanceModel model, RequestContext requestContext,
                             String pipelineName, String pipelineCounter) {
        JsonWriter jsonWriter = new JsonWriter(requestContext)
                .addLink("self", Routes.Stage.self(pipelineName, pipelineCounter, model.getName(), model.getCounter()))

                .add("name", model.getName())
                .add("counter", model.getCounter())
                .add("status", model.getState())
                .add("approved_by", model.getApprovedBy())
                .add("scheduled_at", model.getScheduledDate());

        if (model.getPreviousStage() != null) {
            jsonWriter.add("previous_stage", StageRepresenter.toJSON(model.getPreviousStage(), requestContext, pipelineName, pipelineCounter));
        }

        return jsonWriter.getAsMap();
    }
}
