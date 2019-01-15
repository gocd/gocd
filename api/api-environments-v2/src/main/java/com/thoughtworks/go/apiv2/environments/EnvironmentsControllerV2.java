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

package com.thoughtworks.go.apiv2.environments;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv2.environments.representers.EnvironmentRepresenter;
import com.thoughtworks.go.apiv2.environments.representers.EnvironmentVariableRepresenter;
import com.thoughtworks.go.apiv2.environments.representers.EnvironmentsRepresenter;
import com.thoughtworks.go.config.BasicEnvironmentConfig;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.EnvironmentVariableConfig;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static com.thoughtworks.go.api.util.HaltApiResponses.*;
import static java.lang.String.format;
import static spark.Spark.*;

@Component
public class EnvironmentsControllerV2 extends ApiController implements SparkSpringController, CrudController<EnvironmentConfig> {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final EnvironmentConfigService environmentConfigService;
    private final EntityHashingService entityHashingService;

    @Autowired
    public EnvironmentsControllerV2(ApiAuthenticationHelper apiAuthenticationHelper, EnvironmentConfigService environmentConfigService, EntityHashingService entityHashingService) {
        super(ApiVersion.v2);
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
            before("", this::setContentType);
            before("/*", this::setContentType);

            before("", mimeType, apiAuthenticationHelper::checkAdminUserAnd403);
            before("/*", mimeType, apiAuthenticationHelper::checkAdminUserAnd403);
            get("", mimeType, this::index);
            get(Routes.Environments.NAME, mimeType, this::show);
            delete(Routes.Environments.NAME, this::remove);
            post("", mimeType, this::create);
            put(Routes.Environments.NAME, mimeType, this::update);
            patch(Routes.Environments.NAME, this::partialUpdate);
        });
    }

    public String index(Request request, Response response) throws IOException {
        List<EnvironmentConfig> environmentViewModelList = environmentConfigService.getAllMergedEnvironments();
        return writerForTopLevelObject(request, response,
                outputWriter -> EnvironmentsRepresenter.toJSON(outputWriter, environmentViewModelList));
    }

    public String show(Request request, Response response) throws IOException {
        String environmentName = request.params("name");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        ConfigElementForEdit<EnvironmentConfig> configElementForEdit = environmentConfigService.getMergedEnvironmentforDisplay(environmentName, result);

        if (result.isSuccessful()) {
            return writerForTopLevelObject(request, response,
                    outputWriter -> EnvironmentRepresenter.toJSON(outputWriter, configElementForEdit.getConfigElement()));
        }
        return handleSimpleMessageResponse(response, result);
    }

    public String create(Request request, Response response) {
        final BasicEnvironmentConfig environmentConfigToCreate = (BasicEnvironmentConfig) buildEntityFromRequestBody(request);

        haltIfEntityWithSameNameExists(environmentConfigToCreate);

        final HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();

        environmentConfigService.createEnvironment(environmentConfigToCreate, currentUsername(), operationResult);

        return handleCreateOrUpdateResponse(request, response, environmentConfigToCreate, operationResult);
    }

    public String update(Request request, Response response) {
        String environmentName = request.params("name");
        BasicEnvironmentConfig environmentConfig = (BasicEnvironmentConfig) buildEntityFromRequestBody(request);

        final HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();

        ConfigElementForEdit<EnvironmentConfig> configElementForEdit = environmentConfigService.getMergedEnvironmentforDisplay(environmentName, operationResult);

        if (operationResult.isSuccessful()) {
            if (!StringUtils.equals(environmentName, environmentConfig.name().toString())) {
                throw haltBecauseRenameOfEntityIsNotSupported("environment");
            }

            if (isPutRequestStale(request, configElementForEdit.getConfigElement())) {
                throw haltBecauseEtagDoesNotMatch("environment", environmentName);
            }

            environmentConfigService.updateEnvironment(environmentConfig.name().toString(),
                    environmentConfig, currentUsername(), configElementForEdit.getMd5(), operationResult);

            return handleCreateOrUpdateResponse(request, response, environmentConfig, operationResult);
        }

        return handleSimpleMessageResponse(response, operationResult);
    }

    public String partialUpdate(Request request, Response response) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(request.body());
        List<String> pipelineToAdd = extractListFromJson(jsonReader, "pipelines", "add");
        List<String> pipelineToRemove = extractListFromJson(jsonReader, "pipelines", "remove");
        List<String> agentsToAdd = extractListFromJson(jsonReader, "agents", "add");
        List<String> agentsToRemove = extractListFromJson(jsonReader, "agents", "remove");
        List<String> envVariablesToRemove = extractListFromJson(jsonReader, "environment_variables", "remove");

        List<EnvironmentVariableConfig> environmentVariablesToAdd = new ArrayList<>();

        if (jsonReader.hasJsonObject("environment_variables")) {
            jsonReader.readJsonObject("environment_variables").readArrayIfPresent("add",
                    array ->
                            array.forEach(envVariable -> environmentVariablesToAdd
                                    .add(EnvironmentVariableRepresenter.fromJSON(envVariable.getAsJsonObject()))
                            ));
        }


        String environmentName = request.params("name");

        final HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();

        ConfigElementForEdit<EnvironmentConfig> configElementForEdit = environmentConfigService.getMergedEnvironmentforDisplay(environmentName, operationResult);

        if (operationResult.isSuccessful()) {
            environmentConfigService.patchEnvironment(configElementForEdit.getConfigElement(), pipelineToAdd, pipelineToRemove,
                    agentsToAdd, agentsToRemove, environmentVariablesToAdd, envVariablesToRemove, currentUsername(), operationResult);

            ConfigElementForEdit<EnvironmentConfig> updateConfigElement = environmentConfigService.getMergedEnvironmentforDisplay(environmentName, operationResult);

            return handleCreateOrUpdateResponse(request, response, updateConfigElement.getConfigElement(), operationResult);
        }

        return handleSimpleMessageResponse(response, operationResult);
    }

    private List<String> extractListFromJson(JsonReader jsonReader, String parentKey, String childKey) {
        return (jsonReader.hasJsonObject(parentKey))
                ? jsonReader.readJsonObject(parentKey).readStringArrayIfPresent(childKey).orElseGet(Collections::emptyList)
                : Collections.emptyList();
    }

    public String remove(Request request, Response response) {
        String environmentName = request.params("name");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        ConfigElementForEdit<EnvironmentConfig> configElementForEdit = environmentConfigService.getMergedEnvironmentforDisplay(environmentName, result);

        if (result.isSuccessful()) {
            environmentConfigService.deleteEnvironment(configElementForEdit.getConfigElement(), currentUsername(),
                    result);
        }
        return handleSimpleMessageResponse(response, result);
    }

    @Override
    public String etagFor(EnvironmentConfig entityFromServer) {
        return entityHashingService.md5ForEntity(entityFromServer);
    }

    @Override
    public EnvironmentConfig doFetchEntityFromConfig(String name) {
        ConfigElementForEdit<EnvironmentConfig> mergedEnvironmentforDisplay = environmentConfigService.getMergedEnvironmentforDisplay(name, new HttpLocalizedOperationResult());

        return mergedEnvironmentforDisplay == null ? null : mergedEnvironmentforDisplay.getConfigElement();
    }

    @Override
    public EnvironmentConfig buildEntityFromRequestBody(Request req) {
        return EnvironmentRepresenter.fromJSON(GsonTransformer.getInstance().jsonReaderFrom(req.body()));
    }

    @Override
    public Consumer<OutputWriter> jsonWriter(EnvironmentConfig environmentConfig) {
        return writer -> EnvironmentRepresenter.toJSON(writer, environmentConfig);
    }

    private void haltIfEntityWithSameNameExists(EnvironmentConfig environmentConfig) {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ConfigElementForEdit<EnvironmentConfig> existingEnvironmentConfig = environmentConfigService.getMergedEnvironmentforDisplay(environmentConfig.name().toString(), result);

        if (existingEnvironmentConfig == null) {
            return;
        }

        environmentConfig.addError("name", format("Environment name should be unique. Environment with name '%s' already exists.", environmentConfig.name().toString()));
        throw haltBecauseEntityAlreadyExists(jsonWriter(environmentConfig), "environment", environmentConfig.name().toString());
    }
}