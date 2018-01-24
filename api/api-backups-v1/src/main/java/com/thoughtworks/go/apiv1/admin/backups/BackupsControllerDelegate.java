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

package com.thoughtworks.go.apiv1.admin.backups;


import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.admin.backups.representers.BackupRepresenter;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.domain.ServerBackup;
import com.thoughtworks.go.server.service.BackupService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.RequestContext;
import spark.Request;
import spark.Response;

import static spark.Spark.*;

public class BackupsControllerDelegate extends ApiController {
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final BackupService backupService;
    private final Localizer localizer;

    public BackupsControllerDelegate(ApiAuthenticationHelper apiAuthenticationHelper, BackupService backupService, Localizer localizer) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.backupService = backupService;
        this.localizer = localizer;
    }

    @Override
    public String controllerBasePath() {
        return "/api/backups";
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", this::verifyContentType);
            before("/*", this::verifyContentType);

            before("", this::verifyConfirmHeader);
            before("/*", this::verifyConfirmHeader);

            before("", mimeType, apiAuthenticationHelper::checkAdminUserAnd401);
            before("/*", mimeType, apiAuthenticationHelper::checkAdminUserAnd401);

            post("", mimeType, this::create, GsonTransformer.getInstance());
        });
    }

    public Object create(Request request, Response response) {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ServerBackup backup = backupService.startBackup(currentUsername(), result);
        if (result.isSuccessful()) {
            return BackupRepresenter.toJSON(backup, RequestContext.requestContext(request));
        }
        return renderHTTPOperationResult(result, response, localizer);
    }
}
