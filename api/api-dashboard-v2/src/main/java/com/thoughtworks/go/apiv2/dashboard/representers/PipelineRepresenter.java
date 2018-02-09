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
import com.thoughtworks.go.spark.Routes;
import org.apache.commons.lang.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class PipelineRepresenter {

    private static JsonWriter addLinks(GoDashboardPipeline model, JsonWriter jsonWriter) {
        String pipelineName = model.name().toString();
        ImmutableMap<String, Object> args = ImmutableMap.of("pipeline_name", pipelineName);
        return jsonWriter.addLink("self", Routes.Pipeline.history(pipelineName))
                .addDocLink(Routes.Pipeline.DOC)
                .addLink("settings_path", Routes.Pipeline.settings(pipelineName))
                .addLink("trigger", Routes.Pipeline.schedule(pipelineName))
                .addLink("trigger_with_options", Routes.Pipeline.schedule(pipelineName))
                .addLink("pause", Routes.Pipeline.pause(pipelineName))
                .addLink("unpause", Routes.Pipeline.unpause(pipelineName));
    }

    public static Map<String, Object> toJSON(GoDashboardPipeline model, RequestContext requestContext, Username username) {
        String usernameString = username.getUsername().toString();

        return addLinks(model, new JsonWriter(requestContext))
                .add("name", model.name().toString())
                .add("last_updated_timestamp", model.getLastUpdatedTimeStamp())
                .add("locked", model.model().getLatestPipelineInstance().isCurrentlyLocked())
                .add("pause_info", getPauseInfo(model))
                .add("can_operate", model.isPipelineOperator(usernameString))
                .add("can_administer", model.canBeAdministeredBy(usernameString))
                .add("can_unlock", model.canBeOperatedBy(usernameString))
                .add("can_pause", model.canBeOperatedBy(usernameString))
                .addOptional("tracking_tool", getTrackingToolInfo(model))
                .addEmbedded("instances", getInstances(model, requestContext))
                .getAsMap();
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

    private static Optional<Map> getTrackingToolInfo(GoDashboardPipeline model) {
        return model.getTrackingTool()
                .map(trackingTool -> new JsonWriter(null)
                        .add("regex", trackingTool.getRegex())
                        .add("link", trackingTool.getLink())
                        .getAsMap());
    }
}
