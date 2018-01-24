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
import com.thoughtworks.go.api.representers.JsonWriter;
import com.thoughtworks.go.domain.PipelinePauseInfo;
import com.thoughtworks.go.presentation.pipelinehistory.EmptyPipelineInstanceModel;
import com.thoughtworks.go.server.dashboard.GoDashboardPipeline;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.spark.RequestContext;
import org.apache.commons.lang.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class PipelineRepresenter {

    private static void addLinks(GoDashboardPipeline model, JsonWriter jsonWriter) {
        String pipelineName = model.name().toString();
        ImmutableMap<String, Object> args = ImmutableMap.of("pipeline_name", pipelineName);
        jsonWriter.addLink("self", "/api/pipelines/${pipeline_name}/history", args);
        jsonWriter.addDocLink("https://api.go.cd/current/#pipelines");
        jsonWriter.addLink("settings_path", "/admin/pipelines/${pipeline_name}/general", args);
        jsonWriter.addLink("trigger", "/api/pipelines/${pipeline_name}/schedule", args);
        jsonWriter.addLink("trigger_with_options", "/api/pipelines/${pipeline_name}/schedule", args);
        jsonWriter.addLink("pause", "/api/pipelines/${pipeline_name}/pause", args);
        jsonWriter.addLink("unpause", "/api/pipelines/${pipeline_name}/unpause", args);
    }

    public static Map toJSON(GoDashboardPipeline model, RequestContext requestContext, Username username) {
        JsonWriter jsonWriter = new JsonWriter(requestContext);
        addLinks(model, jsonWriter);
        jsonWriter.add("name", model.name().toString());
        jsonWriter.add("last_updated_timestamp", model.getLastUpdatedTimeStamp());
        jsonWriter.add("locked", model.model().getLatestPipelineInstance().isCurrentlyLocked());
        jsonWriter.add("pause_info", getPauseInfo(model));
        String usernameString = username.getUsername().toString();
        jsonWriter.add("can_operate", model.isPipelineOperator(usernameString));
        jsonWriter.add("can_administer", model.canBeAdministeredBy(usernameString));
        jsonWriter.add("can_unlock", model.canBeOperatedBy(usernameString));
        jsonWriter.add("can_pause", model.canBeOperatedBy(usernameString));
        jsonWriter.addEmbedded("instances", getInstances(model, requestContext));
        return jsonWriter.getAsMap();
    }

    private static List<Map> getInstances(GoDashboardPipeline model, RequestContext requestContext) {
        return model.model().getActivePipelineInstances().stream()
                .filter(instanceModel -> !(instanceModel instanceof EmptyPipelineInstanceModel))
                .map(instanceModel -> PipelineInstanceRepresenter.toJSON(instanceModel, requestContext))
                .collect(toList());
    }

    private static Map<String, Object> getPauseInfo(GoDashboardPipeline model) {
        PipelinePauseInfo pausedInfo = model.model().getPausedInfo();
        LinkedHashMap<String, Object> pauseInfoJSON = new LinkedHashMap<>();
        pauseInfoJSON.put("paused", pausedInfo.isPaused());
        pauseInfoJSON.put("paused_by", StringUtils.isBlank(pausedInfo.getPauseBy()) ? null : pausedInfo.getPauseBy());
        pauseInfoJSON.put("pause_reason", StringUtils.isBlank(pausedInfo.getPauseCause()) ? null : pausedInfo.getPauseCause());
        return pauseInfoJSON;
    }
}
