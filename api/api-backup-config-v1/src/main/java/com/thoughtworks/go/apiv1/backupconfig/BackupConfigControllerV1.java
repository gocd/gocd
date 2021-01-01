/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv1.backupconfig;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.api.util.MessageJson;
import com.thoughtworks.go.apiv1.backupconfig.representers.BackupConfigRepresenter;
import com.thoughtworks.go.config.BackupConfig;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.update.CreateOrUpdateBackupConfigCommand;
import com.thoughtworks.go.config.update.DeleteBackupConfigCommand;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.function.Consumer;

import static spark.Spark.*;

@Component
public class BackupConfigControllerV1 extends ApiController implements SparkSpringController, CrudController<BackupConfig> {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final GoConfigService goConfigService;

    @Autowired
    public BackupConfigControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, GoConfigService goConfigService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.goConfigService = goConfigService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.BackupConfig.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);


            // change the line below to enable appropriate security
            before("", mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);

            get("", mimeType, this::show);

            post("", mimeType, this::createOrUpdate);
            put("", mimeType, this::createOrUpdate);

            delete("", mimeType, this::deleteBackupConfig);
        });
    }

    public String show(Request req, Response res) throws IOException {
        BackupConfig backupConfig = doFetchEntityFromConfig();

        if (backupConfig == null) {
            backupConfig = new BackupConfig();
        }

        return writerForTopLevelObject(req, res, jsonWriter(backupConfig));
    }

    public String createOrUpdate(Request req, Response res) throws IOException {
        BackupConfig backupConfig = buildEntityFromRequestBody(req);
        try {
            goConfigService.updateConfig(new CreateOrUpdateBackupConfigCommand(backupConfig), currentUsername());
        } catch (GoConfigInvalidException e) {
            res.status(HttpStatus.SC_UNPROCESSABLE_ENTITY);
            return MessageJson.create(e.getMessage(), jsonWriter(backupConfig));
        }
        return show(req, res);
    }

    public String deleteBackupConfig(Request req, Response res) throws IOException {
        // to throw a NFE, if none is available
        fetchEntityFromConfig();

        goConfigService.updateConfig(new DeleteBackupConfigCommand(), currentUsername());

        return renderMessage(res, 200, EntityType.BackupConfig.deleteSuccessful());
    }

    @Override
    public String etagFor(BackupConfig entityFromServer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.BackupConfig;
    }

    @Override
    public BackupConfig doFetchEntityFromConfig() {
        return goConfigService.serverConfig().getBackupConfig();
    }

    @Override
    public BackupConfig buildEntityFromRequestBody(Request req) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        return BackupConfigRepresenter.fromJSON(jsonReader);
    }

    @Override
    public Consumer<OutputWriter> jsonWriter(BackupConfig backupConfig) {
        return outputWriter -> BackupConfigRepresenter.toJSON(outputWriter, backupConfig);
    }
}
