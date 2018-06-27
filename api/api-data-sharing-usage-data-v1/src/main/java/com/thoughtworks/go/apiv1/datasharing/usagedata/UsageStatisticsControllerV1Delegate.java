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

package com.thoughtworks.go.apiv1.datasharing.usagedata;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.datasharing.usagedata.representers.UsageStatisticsRepresenter;
import com.thoughtworks.go.server.domain.UsageStatistics;
import com.thoughtworks.go.server.service.DataSharingUsageDataService;
import com.thoughtworks.go.server.util.RSAEncryptionHelper;
import com.thoughtworks.go.spark.Routes.DataSharing;
import com.thoughtworks.go.util.SystemEnvironment;
import spark.Request;
import spark.Response;

import static spark.Spark.*;

public class UsageStatisticsControllerV1Delegate extends ApiController {
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private DataSharingUsageDataService dataSharingUsageDataService;
    private SystemEnvironment systemEnvironment;

    public UsageStatisticsControllerV1Delegate(ApiAuthenticationHelper apiAuthenticationHelper, DataSharingUsageDataService dataSharingUsageDataService, SystemEnvironment systemEnvironment) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.dataSharingUsageDataService = dataSharingUsageDataService;
        this.systemEnvironment = systemEnvironment;
    }

    @Override
    public String controllerBasePath() {
        return DataSharing.USAGE_DATA_PATH;
    }

    @Override
    public void setupRoutes() {
        path(controllerPath(), () -> {
            before("", mimeType, this::setContentType);
            before("/encrypted", this::setEncryptedContentType);
            before("", this::verifyContentType);
            before("/*", this::verifyContentType);

            before("", mimeType, apiAuthenticationHelper::checkAdminUserAnd403);
            before("/encrypted", mimeType, apiAuthenticationHelper::checkUserAnd403);

            get("", this::getUsageStatistics);
            get("/encrypted", this::getEncryptedUsageStatistics);
        });
    }

    public String getUsageStatistics(Request request, Response response) {
        UsageStatistics usageStatistics = dataSharingUsageDataService.get();
        return jsonizeAsTopLevelObject(request, writer -> UsageStatisticsRepresenter.toJSON(writer, usageStatistics));
    }

    public String getEncryptedUsageStatistics(Request request, Response response) throws Exception {
        return RSAEncryptionHelper.encrypt(getUsageStatistics(request, response), systemEnvironment.getUpdateServerPublicKeyPath());
    }
}
