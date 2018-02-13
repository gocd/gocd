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

package com.thoughtworks.go.apiv2.dashboard;


import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv2.dashboard.representers.PipelineGroupsRepresenter;
import com.thoughtworks.go.server.dashboard.GoDashboardPipelineGroup;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import com.thoughtworks.go.server.service.GoDashboardService;
import com.thoughtworks.go.server.service.PipelineSelectionsService;
import com.thoughtworks.go.spark.RequestContext;
import com.thoughtworks.go.spark.Routes;
import spark.Request;
import spark.Response;

import java.util.List;
import java.util.Map;

import static spark.Spark.*;

public class DashboardControllerDelegate extends ApiController {

    private final PipelineSelectionsService pipelineSelectionsService;
    private final GoDashboardService goDashboardService;

    public DashboardControllerDelegate(PipelineSelectionsService pipelineSelectionsService, GoDashboardService goDashboardService) {
        super(ApiVersion.v2);
        this.pipelineSelectionsService = pipelineSelectionsService;
        this.goDashboardService = goDashboardService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.Dashboard.SELF;
    }

    @Override
    public void setupRoutes() {
        path(controllerPath(), () -> {
            before("", mimeType, this::setContentType);
            before("", this::verifyContentType);
            get("", this::index, GsonTransformer.getInstance());
        });
    }

    private Map index(Request request, Response response) {
        String selectedPipelinesCookie = request.cookie("selected_pipelines");
        Long userId = currentUserId(request);
        Username userName = currentUsername();

        PipelineSelections selectedPipelines = pipelineSelectionsService.getPersistedSelectedPipelines(selectedPipelinesCookie, userId);
        List<GoDashboardPipelineGroup> pipelineGroups = goDashboardService.allPipelineGroupsForDashboard(selectedPipelines, userName);

        return PipelineGroupsRepresenter.toJSON(pipelineGroups, RequestContext.requestContext(request), userName);
    }
}
