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

package com.thoughtworks.go.apiv1.adminsconfig;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.adminsconfig.representers.AdminsConfigRepresenter;
import com.thoughtworks.go.config.AdminsConfig;
import com.thoughtworks.go.server.service.AdminsConfigService;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseEtagDoesNotMatch;
import static spark.Spark.*;

public class AdminControllerV1Delegate extends ApiController implements CrudController<AdminsConfig> {
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final EntityHashingService entityHashingService;
    private final AdminsConfigService adminsConfigService;


    public AdminControllerV1Delegate(ApiAuthenticationHelper apiAuthenticationHelper,
                                     EntityHashingService entityHashingService, AdminsConfigService service) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.entityHashingService = entityHashingService;
        this.adminsConfigService = service;
    }

    @Override
    public void setupRoutes() {
        path(controllerPath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", mimeType, this::verifyContentType);
            before("/*", mimeType, this::verifyContentType);
            before("", mimeType, apiAuthenticationHelper::checkAdminUserAnd403);
            before("/*", mimeType, apiAuthenticationHelper::checkAdminUserAnd403);

            get("", mimeType, this::show);
            put("", mimeType, this::update);
        });
    }

    public String show(Request req, Response res) throws IOException {
        AdminsConfig adminsConf = adminsConfigService.systemAdmins();
        if (isGetOrHeadRequestFresh(req, adminsConf)) {
            return notModified(res);
        } else {
            setEtagHeader(adminsConf, res);
            return writerForTopLevelObject(req, res, writer -> AdminsConfigRepresenter.toJSON(writer, adminsConf));
        }
    }

    public String update(Request req, Response res) throws IOException {
        AdminsConfig adminsConfigFromRequest = getEntityFromRequestBody(req);
        AdminsConfig adminsConfigFromServer = adminsConfigService.systemAdmins();

        if (!isPutRequestFresh(req, adminsConfigFromServer)) {
            throw haltBecauseEtagDoesNotMatch();
        }

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        adminsConfigService.update(currentUsername(), adminsConfigFromRequest, etagFor(adminsConfigFromServer), result);

        return handleCreateOrUpdateResponse(req, res, adminsConfigFromRequest, result);
    }

    @Override
    public String etagFor(AdminsConfig entityFromServer) {
        return entityHashingService.md5ForEntity(entityFromServer);
    }

    @Override
    public AdminsConfig doGetEntityFromConfig(String name) {
        throw new RuntimeException("Not implemented. Unlike other entities, AdminsConfig has a single representation in the config.");
    }

    @Override
    public AdminsConfig getEntityFromRequestBody(Request req) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        return AdminsConfigRepresenter.fromJSON(jsonReader);
    }

    @Override
    public String jsonize(Request req, AdminsConfig admins) {
        return jsonizeAsTopLevelObject(req, writer -> AdminsConfigRepresenter.toJSON(writer, admins));
    }

    @Override
    public JsonNode jsonNode(Request req, AdminsConfig admins) throws IOException {
        String jsonize = jsonize(req, admins);
        return new ObjectMapper().readTree(jsonize);
    }

    @Override
    public String controllerBasePath() {
        return Routes.SystemAdmins.BASE;
    }

}
