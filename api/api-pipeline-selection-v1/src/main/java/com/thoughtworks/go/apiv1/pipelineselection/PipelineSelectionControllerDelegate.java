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

package com.thoughtworks.go.apiv1.pipelineselection;


import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.pipelineselection.representers.PipelineSelectionResponse;
import com.thoughtworks.go.apiv1.pipelineselection.representers.PipelineSelectionsRepresenter;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import com.thoughtworks.go.server.service.PipelineConfigService;
import com.thoughtworks.go.server.service.PipelineSelectionsService;
import com.thoughtworks.go.spark.RequestContext;
import com.thoughtworks.go.spark.Routes;
import spark.Request;
import spark.Response;

import java.util.List;

import static spark.Spark.*;

public class PipelineSelectionControllerDelegate extends ApiController {
    private static final int ONE_YEAR = 3600 * 24 * 365;
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final PipelineSelectionsService pipelineSelectionsService;
    private final PipelineConfigService pipelineConfigService;

    public PipelineSelectionControllerDelegate(ApiAuthenticationHelper apiAuthenticationHelper, PipelineSelectionsService pipelineSelectionsService, PipelineConfigService pipelineConfigService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.pipelineSelectionsService = pipelineSelectionsService;
        this.pipelineConfigService = pipelineConfigService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.PipelineSelection.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", this::verifyContentType);
            before("/*", this::verifyContentType);

            get("", mimeType, this::show, GsonTransformer.getInstance());
            put("", mimeType, this::update, GsonTransformer.getInstance());
        });
    }

    public Object show(Request request, Response response) {
        String fromCookie = request.cookie("selected_pipelines");

        PipelineSelections selectedPipelines = pipelineSelectionsService.getSelectedPipelines(fromCookie, currentUserId(request));
        List<PipelineConfigs> pipelineConfigs = pipelineConfigService.viewableGroupsFor(currentUsername());

        PipelineSelectionResponse pipelineSelectionResponse = new PipelineSelectionResponse(selectedPipelines, pipelineConfigs);
        return PipelineSelectionsRepresenter.toJSON(pipelineSelectionResponse, RequestContext.requestContext(request));
    }

    public Object update(Request request, Response response) {
        String fromCookie = request.cookie("selected_pipelines");

        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(request.body());

        PipelineSelectionResponse selectionResponse = PipelineSelectionsRepresenter.fromJSON(jsonReader);

        Long recordId = pipelineSelectionsService.persistSelectedPipelines(fromCookie, currentUserId(request), selectionResponse.getSelectedPipelines().pipelineList(), selectionResponse.getSelectedPipelines().isBlacklist());

        if (!apiAuthenticationHelper.securityEnabled()) {
            response.cookie("/go", "selected_pipelines", String.valueOf(recordId), ONE_YEAR, true, true);
        }
        return null;
    }
}
