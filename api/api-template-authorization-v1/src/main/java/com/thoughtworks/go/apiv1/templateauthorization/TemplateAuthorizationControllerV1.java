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

package com.thoughtworks.go.apiv1.templateauthorization;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.api.util.MessageJson;
import com.thoughtworks.go.apiv1.templateauthorization.representers.AuthorizationRepresenter;
import com.thoughtworks.go.config.Authorization;
import com.thoughtworks.go.config.PipelineTemplateConfig;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.NotImplementedException;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.TemplateConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.function.Consumer;

import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseEtagDoesNotMatch;
import static spark.Spark.*;

@Component
public class TemplateAuthorizationControllerV1 extends ApiController implements SparkSpringController, CrudController<PipelineTemplateConfig> {
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final EntityHashingService entityHashingService;
    private TemplateConfigService templateConfigService;

    @Autowired
    public TemplateAuthorizationControllerV1(ApiAuthenticationHelper apiAuthenticationHelper,
                                             EntityHashingService entityHashingService,
                                             TemplateConfigService templateConfigService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.entityHashingService = entityHashingService;
        this.templateConfigService = templateConfigService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.PipelineTemplateConfig.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", this.mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);
            before("/*", this.mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);

            get(Routes.PipelineTemplateConfig.AUTHORIZATION, mimeType, this::show);
            put(Routes.PipelineTemplateConfig.AUTHORIZATION, mimeType, this::update);
        });
    }

    public String show(Request request, Response response) throws IOException {
        final PipelineTemplateConfig templateConfig = fetchEntityFromConfig(request.params("template_name"));
        String etag = etagFor(templateConfig);

        if (fresh(request, etag)) {
            return notModified(response);
        }

        setEtagHeader(response, etag);
        return writerForTopLevelObject(request, response, jsonWriter(templateConfig));
    }

    public String update(Request request, Response response) {
        final PipelineTemplateConfig templateConfig = fetchEntityFromConfig(request.params("template_name"));
        final Authorization authorization = buildAuthorizationFromRequestBody(request);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        if (isPutRequestStale(request, templateConfig)) {
            throw haltBecauseEtagDoesNotMatch("template", templateConfig.name());
        }

        templateConfigService.updateTemplateAuthConfig(currentUsername(), templateConfig, authorization, result, etagFor(templateConfig));

        return handleCreateOrUpdateResponse(request, response, authorization, result);
    }

    @Override
    public String etagFor(PipelineTemplateConfig entityFromServer) {
        return entityHashingService.hashForEntity(entityFromServer);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.Template;
    }

    @Override
    public PipelineTemplateConfig doFetchEntityFromConfig(String name) {
        return templateConfigService.loadForView(name, new HttpLocalizedOperationResult());
    }

    @Override
    public PipelineTemplateConfig buildEntityFromRequestBody(Request req) {
        throw new NotImplementedException("Not Implemented");
    }

    public Authorization buildAuthorizationFromRequestBody(Request req) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());

        return AuthorizationRepresenter.fromJSON(jsonReader);
    }

    @Override
    public Consumer<OutputWriter> jsonWriter(PipelineTemplateConfig templateAuthorization) {
        return outputWriter -> AuthorizationRepresenter.toJSON(outputWriter, templateAuthorization.getAuthorization());
    }

    private String handleCreateOrUpdateResponse(Request req, Response res, Authorization authorization, HttpLocalizedOperationResult result) {
        if (result.isSuccessful()) {
            PipelineTemplateConfig templateConfig = fetchEntityFromConfig(req.params("template_name"));
            setEtagHeader(templateConfig, res);
            return jsonize(req, templateConfig);
        } else {
            res.status(result.httpCode());
            String errorMessage = result.message();

            return null == authorization ? MessageJson.create(errorMessage)
                    : MessageJson.create(errorMessage, jsonWriter(new PipelineTemplateConfig(null, authorization)));
        }
    }
}
