/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.apiv2.backups;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv2.backups.representers.BackupRepresenter;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.server.domain.ServerBackup;
import com.thoughtworks.go.server.service.BackupService;
import com.thoughtworks.go.spark.GlobalExceptionMapper;
import com.thoughtworks.go.spark.RequestContext;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.Optional;

import static com.thoughtworks.go.spark.Routes.Backups.ID_PATH;
import static spark.Spark.*;

@Component
public class BackupsControllerV2 extends ApiController implements SparkSpringController {

    private static final String RETRY_INTERVAL_IN_SECONDS = "5";
    private static final String RUNNING = "running";

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final BackupService backupService;

    @Autowired
    public BackupsControllerV2(ApiAuthenticationHelper apiAuthenticationHelper, BackupService backupService) {
        super(ApiVersion.v2);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.backupService = backupService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.Backups.BASE;
    }

    @Override
    public void setupRoutes(GlobalExceptionMapper exceptionMapper) {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", mimeType, this::verifyContentType);

            before("", this.mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);
            before("/*", this.mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);

            post("", mimeType, this::create);
            get(ID_PATH, mimeType, this::show);
        });
    }

    public String create(Request request, Response response) {
        ServerBackup backup = backupService.scheduleBackup(currentUsername());
        RequestContext requestContext = RequestContext.requestContext(request);
        String backupPath = requestContext.pathFor(Routes.Backups.serverBackup(String.valueOf(backup.getId())));
        response.status(202);
        response.header("Location", backupPath);
        response.header("Retry-After", RETRY_INTERVAL_IN_SECONDS);
        return NOTHING;
    }

    public String show(Request request, Response response) throws IOException {
        String backupId = request.params("id");
        Optional<ServerBackup> backup;
        if (RUNNING.equals(backupId)) {
            backup = backupService.runningBackup();
        } else {
            try {
                backup = backupService.getServerBackup(Long.parseLong(backupId));
            } catch (NumberFormatException e) {
                throw new BadRequestException("The `id` parameter should be a number, or the keyword `running`");
            }
        }

        if (backup.isEmpty()) {
            throw new RecordNotFoundException(EntityType.Backup, backupId);
        }
        return writerForTopLevelObject(request, response, outputWriter -> BackupRepresenter.toJSON(outputWriter, backup.get()));
    }
}
