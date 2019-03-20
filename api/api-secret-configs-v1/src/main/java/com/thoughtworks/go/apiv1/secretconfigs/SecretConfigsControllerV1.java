/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.secretconfigs;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.secretconfigs.representers.SecretConfigRepresenter;
import com.thoughtworks.go.apiv1.secretconfigs.representers.SecretConfigsRepresenter;
import com.thoughtworks.go.config.SecretConfig;
import com.thoughtworks.go.config.SecretConfigs;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.HttpException;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.SecretConfigService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.function.Consumer;

import static spark.Spark.*;

@Component
public class SecretConfigsControllerV1 extends ApiController implements SparkSpringController, CrudController<SecretConfig> {

    public static final String CONFIG_ID_PARAM = "config_id";
    public final ApiAuthenticationHelper apiAuthenticationHelper;
    private final SecretConfigService configService;
    private final EntityHashingService entityHashingService;

    @Autowired
    public SecretConfigsControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, SecretConfigService configService, EntityHashingService entityHashingService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.configService = configService;
        this.entityHashingService = entityHashingService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.SecretConfigsAPI.BASE; // to be implemented
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", mimeType, apiAuthenticationHelper::checkAdminUserAnd403);
            before("/*", mimeType, apiAuthenticationHelper::checkAdminUserAnd403);

            get("", mimeType, this::index);
            get(Routes.SecretConfigsAPI.ID, mimeType, this::show);

            post("", mimeType, this::create);
            put(Routes.SecretConfigsAPI.ID, mimeType, this::update);
            delete(Routes.SecretConfigsAPI.ID, mimeType, this::destroy);

            exception(HttpException.class, this::httpException);
        });
    }

    public String index(Request request, Response response) throws IOException {
        SecretConfigs allSecretConfigs = configService.getAllSecretConfigs();
        return writerForTopLevelObject(request, response, writer -> SecretConfigsRepresenter.toJSON(writer, allSecretConfigs));
    }

    public String show(Request request, Response response) throws IOException {
        SecretConfig secretConfig = fetchEntityFromConfig(request.params(CONFIG_ID_PARAM));

        if (isGetOrHeadRequestFresh(request, secretConfig)) {
            return notModified(response);
        }

        setEtagHeader(response, etagFor(secretConfig));
        return writerForTopLevelObject(request, response, writer -> SecretConfigRepresenter.toJSON(writer, secretConfig));
    }

    public String create(Request request, Response response)throws IOException {
        return writerForTopLevelObject(request, response, writer -> writer.add("action", "create"));
    }

//    public String create(Request request, Response response)throws IOException {
//        final SecretConfig secretConfigToCreate = buildEntityFromRequestBody(request);
//        haltIfEntityWithSameIdExists(secretConfigToCreate);
//
//        final HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
//        configService.create(currentUsername(), secretConfigToCreate, operationResult);
//
//        return handleCreateOrUpdateResponse(request, response, secretConfigToCreate, operationResult);
//    }

//    private void haltIfEntityWithSameIdExists(SecretConfig secretConfig) {
//        if (configService.findProfile(secretConfig.getId()) == null) {
//            return;
//        }
//        secretConfig.addError("id", format("Secret Configuration ids should be unique. Secret Configuration with id '%s' already exists.", secretConfig.getId()));
//        throw haltBecauseEntityAlreadyExists(jsonWriter(secretConfig), "secretConfig", secretConfig.getId());
//    }

    public String update(Request request, Response response)throws IOException {
        return writerForTopLevelObject(request, response, writer -> writer.add("action", "update" + request.params(CONFIG_ID_PARAM)));
    }

    public String destroy(Request request, Response response) throws IOException {
        return writerForTopLevelObject(request, response, writer -> writer.add("action", "destroy" + request.params(CONFIG_ID_PARAM)));
    }

    @Override
    public String etagFor(SecretConfig entityFromServer) {
        return entityHashingService.md5ForEntity(entityFromServer);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SecretConfig;
    }

    @Override
    public SecretConfig doFetchEntityFromConfig(String name) {
        return configService.getAllSecretConfigs().find(name);
    }

    @Override
    public SecretConfig buildEntityFromRequestBody(Request req) {
        return SecretConfigRepresenter.fromJSON(GsonTransformer.getInstance().jsonReaderFrom(req.body()));
    }

    @Override
    public Consumer<OutputWriter> jsonWriter(SecretConfig secretConfig) {
        return null;
    }
}
