/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv4.dashboard;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.MessageJson;
import com.thoughtworks.go.apiv4.dashboard.representers.DashboardFor;
import com.thoughtworks.go.apiv4.dashboard.representers.DashboardRepresenter;
import com.thoughtworks.go.server.dashboard.GoDashboardEnvironment;
import com.thoughtworks.go.server.dashboard.GoDashboardPipelineGroup;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.domain.user.DashboardFilter;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import com.thoughtworks.go.server.service.GoDashboardService;
import com.thoughtworks.go.server.service.PipelineSelectionsService;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.thoughtworks.go.server.domain.user.DashboardFilter.DEFAULT_NAME;
import static spark.Spark.*;

@Component
public class DashboardControllerV4 extends ApiController implements SparkSpringController {

    private static final String BEING_PROCESSED = MessageJson.create("Dashboard is being processed, this may take a few seconds. Please check back later.");
    private static final int ACCEPTED = 202;

    private static final String COOKIE_NAME = "selected_pipelines";
    private static final String SEP_CHAR = "/";
    private static final String VIEW_NAME = "viewName";

    private final PipelineSelectionsService pipelineSelectionsService;
    private final GoDashboardService goDashboardService;
    private final ApiAuthenticationHelper apiAuthenticationHelper;

    @Autowired
    public DashboardControllerV4(ApiAuthenticationHelper apiAuthenticationHelper, PipelineSelectionsService pipelineSelectionsService, GoDashboardService goDashboardService) {
        super(ApiVersion.v4);
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
            before("", mimeType, this::verifyContentType);
            before("", mimeType, apiAuthenticationHelper::checkUserAnd403);

            get("", mimeType, this::index);
            exception(Exception.class, this::handleException);
        });
    }

    public Object index(Request request, Response response) throws IOException {
        if (!goDashboardService.hasEverLoadedCurrentState()) {
            response.status(ACCEPTED);
            return BEING_PROCESSED;
        }

        final String personalizationCookie = request.cookie(COOKIE_NAME);
        final Long userId = currentUserId(request);
        final Username userName = currentUsername();
        final PipelineSelections personalization = pipelineSelectionsService.load(personalizationCookie, userId);
        final DashboardFilter filter = personalization.namedFilter(getViewName(request));

        final boolean allowEmpty = Toggles.isToggleOn(Toggles.ALLOW_EMPTY_PIPELINE_GROUPS_DASHBOARD) &&
                "true".equalsIgnoreCase(request.queryParams("allowEmpty"));

        List<GoDashboardPipelineGroup> pipelineGroups = goDashboardService.allPipelineGroupsForDashboard(filter, userName, allowEmpty);
        List<GoDashboardEnvironment> environments = goDashboardService.allEnvironmentsForDashboard(filter, userName);

        String etag = calcEtag(userName, pipelineGroups, environments);

        if (fresh(request, etag)) {
            return notModified(response);
        }

        setEtagHeader(response, etag);

        return writerForTopLevelObject(request, response, outputWriter ->
                DashboardRepresenter.toJSON(
                        outputWriter,
                        new DashboardFor(pipelineGroups, environments, userName, personalization.etag())
                )
        );
    }

    private String calcEtag(Username username, List<GoDashboardPipelineGroup> pipelineGroups, List<GoDashboardEnvironment> environments) {
        final String pipelineSegment = pipelineGroups.stream().
                map(GoDashboardPipelineGroup::etag).collect(Collectors.joining(SEP_CHAR));
        final String environmentSegment = environments.stream().
                map(GoDashboardEnvironment::etag).collect(Collectors.joining(SEP_CHAR));
        return DigestUtils.md5Hex(StringUtils.joinWith(SEP_CHAR, username.getUsername(), pipelineSegment, environmentSegment));
    }

    private String getViewName(Request request) {
        final String viewName = request.queryParams(VIEW_NAME);
        return StringUtils.isBlank(viewName) ? DEFAULT_NAME : viewName;
    }
}