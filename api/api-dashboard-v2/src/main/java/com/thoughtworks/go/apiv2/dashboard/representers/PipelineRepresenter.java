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
import com.thoughtworks.go.domain.PipelinePauseInfo;
import com.thoughtworks.go.presentation.pipelinehistory.EmptyPipelineInstanceModel;
import com.thoughtworks.go.server.dashboard.GoDashboardPipeline;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.spark.Link;
import com.thoughtworks.go.spark.RequestContext;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.api.representers.RepresenterUtils.addLinks;
import static java.util.stream.Collectors.toList;

public class PipelineRepresenter {

    private static List<Link> getLinks(GoDashboardPipeline model, RequestContext requestContext) {
        String pipelineName = model.name().toString();
        return Arrays.asList(
                requestContext.build("self", "/api/pipelines/%s/history", pipelineName),
                new Link("doc", "https://api.go.cd/current/#pipelines"),
                requestContext.build("settings_path", "/admin/pipelines/%s/general", pipelineName),
                requestContext.build("trigger", "/api/pipelines/%s/schedule", pipelineName),
                requestContext.build("trigger_with_options", "/api/pipelines/%s/schedule", pipelineName),
                requestContext.build("pause", "/api/pipelines/%s/pause", pipelineName),
                requestContext.build("unpause", "/api/pipelines/%s/unpause", pipelineName)
        );
    }

    public static Map toJSON(GoDashboardPipeline model, RequestContext requestContext, Username username) {
        Map<String, Object> json = new LinkedHashMap<>();
        addLinks(getLinks(model, requestContext), json);
        json.put("name", model.name().toString());
        json.put("last_updated_timestamp", model.getLastUpdatedTimeStamp());
        json.put("locked", model.model().getLatestPipelineInstance().isCurrentlyLocked());
        json.put("pause_info", getPauseInfo(model));
        String usernameString = username.getUsername().toString();
        json.put("can_operate", model.isPipelineOperator(usernameString));
        json.put("can_administer", model.canBeAdministeredBy(usernameString));
        json.put("can_unlock", model.canBeOperatedBy(usernameString));
        json.put("can_pause", model.canBeOperatedBy(usernameString));
        json.put("_embedded", getEmbedded(model, requestContext));
        return json;
    }

    private static Map getEmbedded(GoDashboardPipeline model, RequestContext requestContext) {
        List<Map> instances = model.model().getActivePipelineInstances().stream()
                .filter(instanceModel -> !(instanceModel instanceof EmptyPipelineInstanceModel))
                .map(instanceModel -> PipelineInstanceRepresenter.toJSON(instanceModel, requestContext))
                .collect(toList());
        return ImmutableMap.of("instances", instances);
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
