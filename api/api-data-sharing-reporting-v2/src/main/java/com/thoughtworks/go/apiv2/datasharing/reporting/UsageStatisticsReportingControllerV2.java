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
package com.thoughtworks.go.apiv2.datasharing.reporting;


import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv2.datasharing.reporting.representers.UsageStatisticsReportingRepresenter;
import com.thoughtworks.go.domain.UsageStatisticsReporting;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.service.datasharing.DataSharingUsageStatisticsReportingService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes.DataSharing;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static com.thoughtworks.go.server.service.datasharing.DataSharingUsageStatisticsReportingService.USAGE_DATA_IGNORE_LAST_UPDATED_AT;
import static spark.Spark.*;

@Component
public class UsageStatisticsReportingControllerV2 extends ApiController implements SparkSpringController {
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final DataSharingUsageStatisticsReportingService usageStatisticsReportingService;
    private GoCache goCache;

    @Autowired
    public UsageStatisticsReportingControllerV2(ApiAuthenticationHelper apiAuthenticationHelper, DataSharingUsageStatisticsReportingService UsageStatisticsReportingService, GoCache goCache) {
        super(ApiVersion.v2);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.usageStatisticsReportingService = UsageStatisticsReportingService;
        this.goCache = goCache;
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
            before("", mimeType, this::verifyContentType);
            before("/*", mimeType, this::verifyContentType);

            before("/info", mimeType, apiAuthenticationHelper::checkUserAnd403);
            before("/start", mimeType, apiAuthenticationHelper::checkUserAnd403);
            before("/complete", mimeType, apiAuthenticationHelper::checkUserAnd403);

            get("/info", mimeType, this::getUsageStatisticsReporting);
            post("/start", mimeType, this::startReporting);
            post("/complete", mimeType, this::completeReporting);
        });
    }

    public String startReporting(Request request, Response response) throws IOException {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        usageStatisticsReportingService.startReporting(result);

        if (!result.isSuccessful()) {
            return renderHTTPOperationResult(result, request, response);
        }

        response.status(204);
        return NOTHING;
    }

    public String completeReporting(Request request, Response response) throws IOException {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        usageStatisticsReportingService.completeReporting(result);

        if (!result.isSuccessful()) {
            return renderHTTPOperationResult(result, request, response);
        }
        goCache.put(USAGE_DATA_IGNORE_LAST_UPDATED_AT, false);

        response.status(204);
        return NOTHING;
    }

    public String getUsageStatisticsReporting(Request request, Response response) {
        UsageStatisticsReporting usageStatisticsReporting = usageStatisticsReportingService.get();
        return jsonizeAsTopLevelObject(request, writer -> UsageStatisticsReportingRepresenter.toJSON(writer, usageStatisticsReporting));
    }
}
