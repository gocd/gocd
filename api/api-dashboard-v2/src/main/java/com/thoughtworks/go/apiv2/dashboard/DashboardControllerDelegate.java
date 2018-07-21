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
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.MessageJson;
import com.thoughtworks.go.apiv2.dashboard.representers.DashboardFor;
import com.thoughtworks.go.apiv2.dashboard.representers.PipelineGroupsRepresenter;
import com.thoughtworks.go.server.dashboard.GoDashboardPipelineGroup;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.domain.user.DashboardFilter;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import com.thoughtworks.go.server.service.GoDashboardService;
import com.thoughtworks.go.server.service.PipelineSelectionsService;
import com.thoughtworks.go.spark.Routes;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.thoughtworks.go.server.domain.user.DashboardFilter.DEFAULT_NAME;
import static spark.Spark.*;

public class DashboardControllerDelegate extends ApiController {

    private static final String BEING_PROCESSED = MessageJson.create("Dashboard is being processed, this may take a few seconds. Please check back later.");
    private static final String COOKIE_NAME = "selected_pipelines";

    private final PipelineSelectionsService pipelineSelectionsService;
    private final GoDashboardService goDashboardService;
    private final ApiAuthenticationHelper apiAuthenticationHelper;

    public DashboardControllerDelegate(ApiAuthenticationHelper apiAuthenticationHelper, PipelineSelectionsService pipelineSelectionsService, GoDashboardService goDashboardService) {
        super(ApiVersion.v2);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
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
            before("", apiAuthenticationHelper::checkUserAnd403);

            get("", this::index);
        });
    }

    public Object index(Request request, Response response) throws IOException {
        if (!goDashboardService.hasEverLoadedCurrentState()) {
            response.status(202);
            return BEING_PROCESSED;
        }

        String selectedPipelinesCookie = request.cookie(COOKIE_NAME);
        Long userId = currentUserId(request);
        Username userName = currentUsername();

        PipelineSelections selectedPipelines = pipelineSelectionsService.loadPipelineSelections(selectedPipelinesCookie, userId);

        final String filterName = getViewName(request);
        final DashboardFilter filter = selectedPipelines.namedFilter(filterName);

        List<GoDashboardPipelineGroup> pipelineGroups = goDashboardService.allPipelineGroupsForDashboard(filter, userName);
        String pipelineGroupsEtag = pipelineGroups.stream().map(GoDashboardPipelineGroup::etag).collect(Collectors.joining("/"));
        String etag = DigestUtils.md5Hex(currentUserLoginName().toString() + "/" + pipelineGroupsEtag);

        if (fresh(request, etag)) {
            return notModified(response);
        }

        setEtagHeader(response, etag);

        return writerForTopLevelObject(request, response, outputWriter -> PipelineGroupsRepresenter.toJSON(outputWriter, new DashboardFor(pipelineGroups, userName)));
    }

    private String getViewName(Request request) {
        final String viewName = request.queryParams("viewName");
        return StringUtils.isBlank(viewName) ? DEFAULT_NAME : viewName;
    }
}
