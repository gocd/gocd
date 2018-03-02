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
import com.thoughtworks.go.api.util.MessageJson;
import com.thoughtworks.go.apiv2.dashboard.representers.DashboardFor;
import com.thoughtworks.go.apiv2.dashboard.representers.PipelineGroupsRepresenter;
import com.thoughtworks.go.server.dashboard.GoDashboardPipelineGroup;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import com.thoughtworks.go.server.service.GoDashboardService;
import com.thoughtworks.go.server.service.PipelineSelectionsService;
import com.thoughtworks.go.spark.Routes;
import org.apache.commons.codec.digest.DigestUtils;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static spark.Spark.*;

public class DashboardControllerDelegate extends ApiController {

    private static final String BEING_PROCESSED = MessageJson.create("Dashboard is being processed, this may take a few seconds. Please check back later.");
    private static final String FEATURE_NOT_ENABLED = MessageJson.create("The quicker dashboard feature has not been enabled!");

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

            get("", this::index);
        });
    }

    private Object index(Request request, Response response) throws IOException {
        if (goDashboardService.isFeatureToggleDisabled()) {
            response.status(424);
            return FEATURE_NOT_ENABLED;
        }
        if (!goDashboardService.hasEverLoadedCurrentState()) {
            response.status(202);
            return BEING_PROCESSED;
        }

        String selectedPipelinesCookie = request.cookie("selected_pipelines");
        Long userId = currentUserId(request);
        Username userName = currentUsername();

        PipelineSelections selectedPipelines = pipelineSelectionsService.getPersistedSelectedPipelines(selectedPipelinesCookie, userId);
        List<GoDashboardPipelineGroup> pipelineGroups = goDashboardService.allPipelineGroupsForDashboard(selectedPipelines, userName);
        String etag = DigestUtils.md5Hex(pipelineGroups.stream().map(GoDashboardPipelineGroup::etag).collect(Collectors.joining("/")));

        if (fresh(request, etag)) {
            return notModified(response);
        }

        setEtagHeader(response, etag);

        return writerForTopLevelObject(request, response, outputWriter -> PipelineGroupsRepresenter.toJSON(outputWriter, new DashboardFor(pipelineGroups, userName)));
    }
}
