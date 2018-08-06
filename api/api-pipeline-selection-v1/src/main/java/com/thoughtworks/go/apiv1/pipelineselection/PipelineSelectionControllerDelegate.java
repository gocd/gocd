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


import com.google.gson.JsonParseException;
import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.HaltApiResponses;
import com.thoughtworks.go.apiv1.pipelineselection.representers.PipelineSelectionResponse;
import com.thoughtworks.go.apiv1.pipelineselection.representers.PipelineSelectionsRepresenter;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.server.domain.user.FilterValidationException;
import com.thoughtworks.go.server.domain.user.Filters;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import com.thoughtworks.go.server.service.PipelineConfigService;
import com.thoughtworks.go.server.service.PipelineSelectionsService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.http.HttpStatus;
import spark.Request;
import spark.Response;

import java.util.List;
import java.util.Objects;

import static java.lang.String.format;
import static spark.Spark.*;

public class PipelineSelectionControllerDelegate extends ApiController {
    private static final int ONE_YEAR = 3600 * 24 * 365;
    private static final String COOKIE_NAME = "selected_pipelines";

    private static final int OK = HttpStatus.OK.value();
    private static final String DATA_IS_OUT_OF_DATE = "Update failed because client filter data is out-of-date";

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final PipelineSelectionsService pipelineSelectionsService;
    private final PipelineConfigService pipelineConfigService;
    private final SystemEnvironment systemEnvironment;

    public PipelineSelectionControllerDelegate(ApiAuthenticationHelper apiAuthenticationHelper,
                                               PipelineSelectionsService pipelineSelectionsService,
                                               PipelineConfigService pipelineConfigService,
                                               SystemEnvironment systemEnvironment) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.pipelineSelectionsService = pipelineSelectionsService;
        this.pipelineConfigService = pipelineConfigService;
        this.systemEnvironment = systemEnvironment;
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

            get("", mimeType, this::show);
            put("", mimeType, this::update);
        });
    }

    public String show(Request request, Response response) {
        String fromCookie = request.cookie(COOKIE_NAME);
        List<PipelineConfigs> groups = pipelineConfigService.viewableGroupsFor(currentUsername());
        PipelineSelections pipelineSelections = pipelineSelectionsService.load(fromCookie, currentUserId(request));

        if (fresh(request, pipelineSelections.etag())) {
            return notModified(response);
        }

        setEtagHeader(response, pipelineSelections.etag());

        PipelineSelectionResponse pipelineSelectionResponse = new PipelineSelectionResponse(pipelineSelections.viewFilters(), groups);

        return PipelineSelectionsRepresenter.toJSON(pipelineSelectionResponse);
    }

    public String update(Request request, Response response) {
        String fromCookie = request.cookie(COOKIE_NAME);
        final Long userId = currentUserId(request);

        if (!Objects.equals(pipelineSelectionsService.load(fromCookie, userId).etag(), getIfMatch(request))) {
            throw HaltApiResponses.haltBecauseEtagDoesNotMatch(DATA_IS_OUT_OF_DATE);
        }

        Filters filters;

        try {
            filters = Filters.fromJson(request.body());
        } catch (FilterValidationException | JsonParseException e) {
            throw HaltApiResponses.haltBecauseOfReason(e.getMessage());
        }

        Long recordId = pipelineSelectionsService.save(fromCookie, userId, filters);

        if (!apiAuthenticationHelper.securityEnabled()) {
            response.cookie("/go", COOKIE_NAME, String.valueOf(recordId), ONE_YEAR, systemEnvironment.isSessionCookieSecure(), true);
        }

        response.status(OK);
        return format("{ \"contentHash\": \"%s\" }", pipelineSelectionsService.load(fromCookie, userId).etag());
    }
}
