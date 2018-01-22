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

import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel;
import com.thoughtworks.go.spark.Link;
import com.thoughtworks.go.spark.RequestContext;

import java.util.*;

import static com.thoughtworks.go.api.representers.RepresenterUtils.addLinks;
import static java.util.Collections.singletonList;


public class StageRepresenter {

    private static final String SELF_HREF = "/api/stages/${pipeline_name}/${pipeline_counter}/${stage_name}/${stage_counter}";

    private static List<Link> getLinks(StageInstanceModel model, RequestContext requestContext,
                                       String pipelineName, String pipelineCounter) {
        return singletonList(
                requestContext.buildWithNamedArgs("self", SELF_HREF,
                        new HashMap<String, Object>() {{
                            put("pipeline_name", pipelineName);
                            put("stage_name", model.getName());
                            put("pipeline_counter", pipelineCounter);
                            put("stage_counter", model.getCounter());

                        }}
                ));
    }

    public static Map toJSON(StageInstanceModel model, RequestContext requestContext,
                             String pipelineName, String pipelineCounter) {
        Map<String, Object> json = new LinkedHashMap<>();

        addLinks(getLinks(model, requestContext, pipelineName, pipelineCounter), json);

        json.put("name", model.getName());
        json.put("status", model.getState());
        json.put("approved_by", model.getApprovedBy());
        json.put("scheduled_at", model.getScheduledDate());
        if (model.getPreviousStage() != null) {
            json.put("previous_stage", StageRepresenter.toJSON(model.getPreviousStage(), requestContext, pipelineName, pipelineCounter));
        }

        return json;
    }
}
