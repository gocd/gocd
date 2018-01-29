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
import com.thoughtworks.go.server.dashboard.GoDashboardPipelineGroup;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.spark.RequestContext;
import com.thoughtworks.go.spark.Routes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PipelineGroupsRepresenter {

    public static Map toJSON(List<GoDashboardPipelineGroup> pipelineGroups, RequestContext requestContext, Username username) {
        return new JsonWriter(requestContext)
                .addLink("self", Routes.Dashboard.SELF)
                .addDocLink(Routes.Dashboard.DOC)

                .addEmbedded("pipeline_groups", getPipelineGroups(pipelineGroups, requestContext, username))
                .addEmbedded("pipelines", getPipelines(pipelineGroups, requestContext, username))
                .getAsMap();
    }

    private static List<Map> getPipelines(List<GoDashboardPipelineGroup> pipelineGroups, RequestContext requestContext, Username username) {
        return pipelineGroups.stream()
                .flatMap(pipelineGroup -> pipelineGroup.allPipelines().stream())
                .map(pipeline -> PipelineRepresenter.toJSON(pipeline, requestContext, username))
                .collect(Collectors.toList());
    }

    private static List<Map> getPipelineGroups(List<GoDashboardPipelineGroup> pipelineGroups, RequestContext requestContext, Username username) {
        return pipelineGroups.stream()
                .map(pipelineGroup -> PipelineGroupRepresenter.toJSON(pipelineGroup, requestContext, username))
                .collect(Collectors.toList());
    }

}
