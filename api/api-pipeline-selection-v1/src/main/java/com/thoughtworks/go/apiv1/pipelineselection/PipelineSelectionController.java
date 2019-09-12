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
package com.thoughtworks.go.apiv1.pipelineselection;


import com.google.gson.JsonParseException;
import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.HaltApiResponses;
import com.thoughtworks.go.apiv1.pipelineselection.representers.PipelineSelectionResponse;
import com.thoughtworks.go.apiv1.pipelineselection.representers.PipelineSelectionsRepresenter;
import com.thoughtworks.go.apiv1.pipelineselection.representers.PipelinesDataRepresenter;
import com.thoughtworks.go.apiv1.pipelineselection.representers.PipelinesDataResponse;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.domain.user.FilterValidationException;
import com.thoughtworks.go.server.domain.user.Filters;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import com.thoughtworks.go.server.service.PipelineConfigService;
import com.thoughtworks.go.server.service.PipelineSelectionsService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static spark.Spark.*;

@Component
public class PipelineSelectionController extends ApiController implements SparkSpringController {
    private static final int ONE_YEAR = 3600 * 24 * 365;
    private static final String COOKIE_NAME = "selected_pipelines";

    private static final int OK = HttpStatus.OK.value();
    private static final String DATA_IS_OUT_OF_DATE = "Update failed because the view is out-of-date. Try refreshing the page.";

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final PipelineSelectionsService pipelineSelectionsService;
    private final PipelineConfigService pipelineConfigService;
    private final SystemEnvironment systemEnvironment;

    @Autowired
    public PipelineSelectionController(ApiAuthenticationHelper apiAuthenticationHelper,
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
            before("", mimeType, this::verifyContentType);
            before("/*", mimeType, this::verifyContentType);

            get(Routes.PipelineSelection.PIPELINES_DATA, mimeType, this::pipelinesData);
            get("", mimeType, this::show);
            put("", mimeType, this::update);
        });
    }

    public String pipelinesData(Request request, Response response) {
        List<PipelineConfigs> groups = pipelineConfigService.viewableGroupsFor(currentUsername());
        String etag = calcPipelinesDataEtag(currentUsername(), groups);

        if (fresh(request, etag)) {
            return notModified(response);
        }

        setEtagHeader(response, etag);

        PipelinesDataResponse pipelineSelectionResponse = new PipelinesDataResponse(groups);
        return PipelinesDataRepresenter.toJSON(pipelineSelectionResponse);
    }

    public String show(Request request, Response response) {
        String fromCookie = request.cookie(COOKIE_NAME);
        PipelineSelections pipelineSelections = pipelineSelectionsService.load(fromCookie, currentUserId(request));

        if (fresh(request, pipelineSelections.getEtag())) {
            return notModified(response);
        }

        setEtagHeader(response, pipelineSelections.getEtag());

        PipelineSelectionResponse pipelineSelectionResponse = new PipelineSelectionResponse(pipelineSelections.getViewFilters());

        return PipelineSelectionsRepresenter.toJSON(pipelineSelectionResponse);
    }

    public String update(Request request, Response response) {
        String fromCookie = request.cookie(COOKIE_NAME);
        final Long userId = currentUserId(request);

        if (!Objects.equals(pipelineSelectionsService.load(fromCookie, userId).getEtag(), getIfMatch(request))) {
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
        return format("{ \"contentHash\": \"%s\" }", pipelineSelectionsService.load(fromCookie, userId).getEtag());
    }

    private String calcPipelinesDataEtag(Username username, List<PipelineConfigs> pipelineConfigs) {
        final HashMap<String, List<CaseInsensitiveString>> pipelinesDataSegment = new HashMap<>();
        for (PipelineConfigs group : pipelineConfigs) {
            final List<PipelineConfig> pipelines = group.getPipelines();
            if (!pipelines.isEmpty()) {
                List<CaseInsensitiveString> pipelineNames = pipelines.stream().map(PipelineConfig::name).collect(Collectors.toList());
                pipelinesDataSegment.put(group.getGroup(), pipelineNames);
            }
        }
        return DigestUtils.md5Hex(StringUtils.joinWith("/", username.getUsername(), pipelinesDataSegment));
    }
}
