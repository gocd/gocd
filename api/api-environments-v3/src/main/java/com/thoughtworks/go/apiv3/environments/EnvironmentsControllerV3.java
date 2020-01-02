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
package com.thoughtworks.go.apiv3.environments;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.api.util.MessageJson;
import com.thoughtworks.go.apiv3.environments.model.PatchEnvironmentRequest;
import com.thoughtworks.go.apiv3.environments.representers.EnvironmentRepresenter;
import com.thoughtworks.go.apiv3.environments.representers.PatchEnvironmentRequestRepresenter;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.policy.SupportedEntity;
import com.thoughtworks.go.domain.ConfigElementForEdit;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.EnvironmentConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static com.thoughtworks.go.api.representers.EnvironmentVariableRepresenter.toJSONArray;
import static com.thoughtworks.go.api.util.HaltApiResponses.*;
import static com.thoughtworks.go.apiv3.environments.representers.EnvironmentsRepresenter.toJSON;
import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toCollection;
import static spark.Spark.*;

@Component
public class EnvironmentsControllerV3 extends ApiController implements SparkSpringController, CrudController<EnvironmentConfig> {
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final EnvironmentConfigService environmentConfigService;
    private final EntityHashingService entityHashingService;

    @Autowired
    public EnvironmentsControllerV3(ApiAuthenticationHelper apiAuthenticationHelper, EnvironmentConfigService environmentConfigService, EntityHashingService entityHashingService) {
        super(ApiVersion.v3);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.environmentConfigService = environmentConfigService;
        this.entityHashingService = entityHashingService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.Environments.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", mimeType, (request, response) -> {
                String resourceToOperateOn = "*";
                if (request.requestMethod().equalsIgnoreCase("GET")) {
                    apiAuthenticationHelper.checkUserAnd403(request, response);
                    return;
                }

                if (request.requestMethod().equalsIgnoreCase("POST")) {
                    resourceToOperateOn = GsonTransformer.getInstance().jsonReaderFrom(request.body()).getString("name");
                }

                apiAuthenticationHelper.checkUserHasPermissions(currentUsername(), getAction(request), SupportedEntity.ENVIRONMENT, resourceToOperateOn);
            });

            before(Routes.Environments.NAME, mimeType, (request, response) -> {
                apiAuthenticationHelper.checkUserHasPermissions(currentUsername(), getAction(request), SupportedEntity.ENVIRONMENT, request.params("name"));
            });

            get("", mimeType, this::index);
            get(Routes.Environments.NAME, mimeType, this::show);
            post("", mimeType, this::create);
            put(Routes.Environments.NAME, mimeType, this::update);
            patch(Routes.Environments.NAME, mimeType, this::partialUpdate);
            delete(Routes.Environments.NAME, mimeType, this::remove);
        });
    }

    public String index(Request request, Response response) throws IOException {
        Set<EnvironmentConfig> userSpecificEnvironments = new HashSet<>();
        for (EnvironmentConfig environmentConfig : environmentConfigService.getEnvironments()) {
            if (apiAuthenticationHelper.doesUserHasPermissions(currentUsername(), getAction(request), SupportedEntity.ENVIRONMENT, environmentConfig.name().toString())) {
                userSpecificEnvironments.add(environmentConfig);
            }
        }

        EnvironmentsConfig envViewModelList = sortEnvConfigs(userSpecificEnvironments);
        setEtagHeader(response, calculateEtag(envViewModelList));
        return writerForTopLevelObject(request, response, outputWriter -> toJSON(outputWriter, envViewModelList));
    }

    public String show(Request request, Response response) throws IOException {
        String environmentName = request.params("name");

        EnvironmentConfig environmentConfig = fetchEntityFromConfig(environmentName);

        String etag = etagFor(environmentConfig);
        if (fresh(request, etag)) {
            return notModified(response);
        }
        setEtagHeader(environmentConfig, response);

        return writerForTopLevelObject(request, response,
                outputWriter -> EnvironmentRepresenter.toJSON(outputWriter, environmentConfig));
    }

    public String create(Request request, Response response) {
        final BasicEnvironmentConfig environmentConfigToCreate = (BasicEnvironmentConfig) buildEntityFromRequestBody(request);
        final HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();

        haltIfEntityWithSameNameExists(environmentConfigToCreate);

        if (!environmentConfigToCreate.getAllErrors().isEmpty()) {
            operationResult.unprocessableEntity("Error parsing environment config from the request");
            return handleCreateOrUpdateResponse(request, response, environmentConfigToCreate, operationResult);
        }

        environmentConfigService.createEnvironment(environmentConfigToCreate, currentUsername(), operationResult);

        setEtagHeader(environmentConfigToCreate, response);
        return handleCreateOrUpdateResponse(request, response, environmentConfigToCreate, operationResult);
    }

    public String update(Request request, Response response) {
        String environmentName = request.params("name");
        BasicEnvironmentConfig environmentConfig = (BasicEnvironmentConfig) buildEntityFromRequestBody(request);
        EnvironmentConfig oldEnvironmentConfig = fetchEntityFromConfig(environmentName);
        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();

        if (!StringUtils.equalsIgnoreCase(environmentName, environmentConfig.name().toString())) {
            throw haltBecauseRenameOfEntityIsNotSupported("environment");
        }

        if (!environmentConfig.getAllErrors().isEmpty()) {
            operationResult.unprocessableEntity("Error parsing environment config from the request");
            return handleCreateOrUpdateResponse(request, response, environmentConfig, operationResult);
        }

        if (isPutRequestStale(request, oldEnvironmentConfig)) {
            throw haltBecauseEtagDoesNotMatch("environment", environmentName);
        }

        environmentConfigService.updateEnvironment(environmentConfig.name().toString(), environmentConfig, currentUsername(), etagFor(oldEnvironmentConfig), operationResult);

        setEtagHeader(environmentConfig, response);
        return handleCreateOrUpdateResponse(request, response, environmentConfig, operationResult);
    }

    public String partialUpdate(Request request, Response response) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(request.body());
        PatchEnvironmentRequest req = PatchEnvironmentRequestRepresenter.fromJSON(jsonReader);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        String environmentName = request.params("name");
        EnvironmentConfig environmentConfig = fetchEntityFromConfig(environmentName);

        Optional<EnvironmentVariableConfig> parsingErrors = req.getEnvironmentVariablesToAdd().stream()
                .filter(envVar -> !envVar.errors().isEmpty())
                .findFirst();

        if (parsingErrors.isPresent()) {
            EnvironmentVariablesConfig configs = new EnvironmentVariablesConfig(req.getEnvironmentVariablesToAdd());
            response.status(422);
            return MessageJson.create("Error parsing patch request",
                    writer -> toJSONArray(writer, "environment_variables", configs));
        }

        environmentConfigService.patchEnvironment(environmentConfig, req.getPipelineToAdd(), req.getPipelineToRemove(),
                req.getEnvironmentVariablesToAdd(), req.getEnvironmentVariablesToRemove(), currentUsername(), result);

        EnvironmentConfig updateConfigElement = fetchEntityFromConfig(environmentName);

        setEtagHeader(updateConfigElement, response);
        return handleCreateOrUpdateResponse(request, response, updateConfigElement, result);
    }

    public String remove(Request request, Response response) {
        String environmentName = request.params("name");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        EnvironmentConfig environmentConfig = fetchEntityFromConfig(environmentName);

        environmentConfigService.deleteEnvironment(environmentConfig, currentUsername(), result);
        return handleSimpleMessageResponse(response, result);
    }

    @Override
    public String etagFor(EnvironmentConfig entityFromServer) {
        return entityHashingService.md5ForEntity(entityFromServer);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.Environment;
    }

    @Override
    public EnvironmentConfig doFetchEntityFromConfig(String name) {
        return environmentConfigService.getEnvironmentConfig(name);
    }

    @Override
    public EnvironmentConfig buildEntityFromRequestBody(Request req) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        return EnvironmentRepresenter.fromJSON(jsonReader);
    }

    @Override
    public Consumer<OutputWriter> jsonWriter(EnvironmentConfig environmentConfig) {
        return writer -> EnvironmentRepresenter.toJSON(writer, environmentConfig);
    }

    EnvironmentsConfig sortEnvConfigs(Set<EnvironmentConfig> envConfigSet) {
        return envConfigSet.stream()
                .sorted(comparing(EnvironmentConfig::name))
                .collect(toCollection(EnvironmentsConfig::new));
    }

    private void haltIfEntityWithSameNameExists(EnvironmentConfig environmentConfig) {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ConfigElementForEdit<EnvironmentConfig> existingEnvConfig = environmentConfigService.getMergedEnvironmentforDisplay(environmentConfig.name().toString(), result);
        if (existingEnvConfig == null) {
            return;
        }

        environmentConfig.addError("name", format("Environment name should be unique. Environment with name '%s' already exists.", environmentConfig.name().toString()));
        throw haltBecauseEntityAlreadyExists(jsonWriter(environmentConfig), "environment", environmentConfig.name().toString());
    }

    private String calculateEtag(EnvironmentsConfig envConfigs) {
        return entityHashingService.md5ForEntity(envConfigs);
    }
}
