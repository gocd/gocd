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

package com.thoughtworks.go.apiv1.datasharing.reporting;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.datasharing.reporting.representers.UsageStatisticsReportingRepresenter;
import com.thoughtworks.go.domain.UsageStatisticsReporting;
import com.thoughtworks.go.server.dao.UsageStatisticsReportingSqlMapDao.DuplicateMetricReporting;
import com.thoughtworks.go.server.service.DataSharingService;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.Routes.DataSharing;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.Map;

import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseEtagDoesNotMatch;
import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseRenameOfEntityIsNotSupported;
import static spark.Spark.*;

public class UsageStatisticsReportingControllerV1Delegate extends ApiController implements CrudController<UsageStatisticsReporting> {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final DataSharingService dataSharingService;
    private final EntityHashingService entityHashingService;
    private final String SERVER_ID_KEY = "server_id";

    public UsageStatisticsReportingControllerV1Delegate(ApiAuthenticationHelper apiAuthenticationHelper, DataSharingService dataSharingService, EntityHashingService entityHashingService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.dataSharingService = dataSharingService;
        this.entityHashingService = entityHashingService;
    }

    @Override
    public String controllerBasePath() {
        return DataSharing.REPORTING_PATH;
    }

    @Override
    public void setupRoutes() {
        path(controllerPath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", this::verifyContentType);
            before("/*", this::verifyContentType);

            before("", mimeType, apiAuthenticationHelper::checkUserAnd403);

            get("", this::getUsageStatisticsReporting);
            patch("", mimeType, this::updateUsageStatisticsReporting);
        });
    }

    public String getUsageStatisticsReporting(Request request, Response response) {
        UsageStatisticsReporting usageStatisticsReporting = dataSharingService.getUsageStatisticsReporting();
        setEtagHeader(response, etagFor(usageStatisticsReporting));
        return jsonize(request, usageStatisticsReporting);
    }

    public String updateUsageStatisticsReporting(Request request, Response response) throws IOException, DuplicateMetricReporting {
        Map<String, Object> bodyAsJSON = readRequestBodyAsJSON(request);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        if (bodyAsJSON.get(SERVER_ID_KEY) != null) {
            throw haltBecauseRenameOfEntityIsNotSupported(SERVER_ID_KEY);
        }
        if (!isPutRequestFresh(request, dataSharingService.getUsageStatisticsReporting())) {
            throw haltBecauseEtagDoesNotMatch();
        }
        UsageStatisticsReporting statisticsReporting = getEntityFromRequestBody(request);
        dataSharingService.updateUsageStatisticsReporting(statisticsReporting, result);
        if (result.isSuccessful()) {
            statisticsReporting = dataSharingService.getUsageStatisticsReporting();
        }
        return handleCreateOrUpdateResponse(request, response, statisticsReporting, result);
    }

    @Override
    public String etagFor(UsageStatisticsReporting usageStatisticsReporting) {
        return entityHashingService.md5ForEntity(usageStatisticsReporting);
    }

    @Override
    public UsageStatisticsReporting doGetEntityFromConfig(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UsageStatisticsReporting getEntityFromRequestBody(Request request) {
        Map<String, Object> bodyAsJSON = readRequestBodyAsJSON(request);
        return UsageStatisticsReportingRepresenter.fromJSON(GsonTransformer.getInstance().jsonReaderFrom(bodyAsJSON));
    }

    @Override
    public String jsonize(Request request, UsageStatisticsReporting usageStatisticsReporting) {
        return jsonizeAsTopLevelObject(request, writer -> UsageStatisticsReportingRepresenter.toJSON(writer, usageStatisticsReporting));
    }

    @Override
    public JsonNode jsonNode(Request request, UsageStatisticsReporting reporting) throws IOException {
        return new ObjectMapper().readTree(jsonize(request, reporting));
    }
}
