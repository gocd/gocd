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

import com.google.common.collect.ImmutableMap;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.spark.Link;
import com.thoughtworks.go.spark.RequestContext;

import java.util.*;

import static com.thoughtworks.go.api.representers.RepresenterUtils.addLinks;
import static java.util.stream.Collectors.toList;

public class PipelineInstanceRepresenter {

    private static final String SELF_HREF = "/api/pipelines/${pipeline_name}/instance/${pipeline_counter}";
    private static final String COMPARE_HREF = "/compare/${pipeline_name}/${from_counter}/with/${to_counter}";
    private static final String HISTORY_HREF = "/api/pipelines/${pipeline_name}/history";
    private static final String VSM_HREF = "/pipelines/value_stream_map/${pipeline_name}/${pipeline_counter}";

    private static List<Link> getLinks(PipelineInstanceModel model, RequestContext requestContext) {
        return Arrays.asList(
                requestContext.buildWithNamedArgs("self", SELF_HREF,
                        ImmutableMap.of("pipeline_name", model.getName(), "pipeline_counter", model.getCounter())),
                requestContext.buildWithNamedArgs("compare_url", COMPARE_HREF,
                        ImmutableMap.of("pipeline_name", model.getName(), "from_counter", model.getCounter() - 1, "to_counter", model.getCounter())),
                requestContext.buildWithNamedArgs("history_url", HISTORY_HREF,
                        ImmutableMap.of("pipeline_name", model.getName())),
                requestContext.buildWithNamedArgs("vsm_url", VSM_HREF,
                        ImmutableMap.of("pipeline_name", model.getName(), "pipeline_counter", model.getCounter()))
        );
    }


    public static Map toJSON(PipelineInstanceModel model, RequestContext requestContext) {
        Map<String, Object> json = new HashMap<>();

        addLinks(getLinks(model, requestContext), json);

        json.put("label", model.getLabel());
        json.put("triggered_by", model.getApprovedByForDisplay());
        json.put("scheduled_at", model.getScheduledDate());
        json.put("build_cause", BuildCauseRepresenter.toJSON(model.getBuildCause(), requestContext));
        json.put("_embedded", getEmbedded(model, requestContext));
        return json;
    }

    private static Map getEmbedded(PipelineInstanceModel model, RequestContext requestContext) {
        Map<String, Object> embedded = new LinkedHashMap<>();
        embedded.put("stages", model.getStageHistory().stream()
                .map(stage -> StageRepresenter.toJSON(stage, requestContext, model.getName(), String.valueOf(model.getCounter())))
                .collect(toList()));
        return embedded;
    }

}
