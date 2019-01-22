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
import com.thoughtworks.go.apiv2.environments.model.PatchEnvironmentRequest;
import com.thoughtworks.go.apiv2.environments.representers.EnvironmentRepresenter;
import com.thoughtworks.go.apiv2.environments.representers.EnvironmentsRepresenter;
import com.thoughtworks.go.apiv2.environments.representers.PatchEnvironmentRequestRepresenter;
import com.thoughtworks.go.config.BasicEnvironmentConfig;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.ConfigElementForEdit;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.EnvironmentConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.thoughtworks.go.api.util.HaltApiResponses.*;
import static java.lang.String.format;
import static spark.Spark.*;

@Component
public class EnvironmentsControllerV2 extends ApiController implements SparkSpringController, CrudController<EnvironmentConfig> {

    private static final String SEP_CHAR = "/";
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
            post("", mimeType, this::create);
            put(Routes.Environments.NAME, mimeType, this::update);
            patch(Routes.Environments.NAME, this::partialUpdate);
            delete(Routes.Environments.NAME, this::remove);
            exception(RecordNotFoundException.class, this::notFound);
        });
    }

    public String index(Request request, Response response) throws IOException {
        List<EnvironmentConfig> environmentViewModelList = environmentConfigService.getAllMergedEnvironments();

        setEtagHeader(response, calculateEtag(environmentViewModelList));

        return writerForTopLevelObject(request, response,
                outputWriter -> EnvironmentsRepresenter.toJSON(outputWriter, environmentViewModelList));
    }

    public String show(Request request, Response response) throws IOException {
        String environmentName = request.params("name");

        EnvironmentConfig environmentConfig = fetchEntityFromConfig(environmentName);

        setEtagHeader(environmentConfig, response);

        return writerForTopLevelObject(request, response,
                outputWriter -> EnvironmentRepresenter.toJSON(outputWriter, environmentConfig));
    }

    public String create(Request request, Response response) {
        final BasicEnvironmentConfig environmentConfigToCreate = (BasicEnvironmentConfig) buildEntityFromRequestBody(request);
        final HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();

        haltIfEntityWithSameNameExists(environmentConfigToCreate);

        environmentConfigService.createEnvironment(environmentConfigToCreate, currentUsername(), operationResult);

        setEtagHeader(environmentConfigToCreate, response);
        return handleCreateOrUpdateResponse(request, response, environmentConfigToCreate, operationResult);
    }

    public String update(Request request, Response response) {
        String environmentName = request.params("name");
        BasicEnvironmentConfig environmentConfig = (BasicEnvironmentConfig) buildEntityFromRequestBody(request);
        EnvironmentConfig oldEnvironmentConfig = fetchEntityFromConfig(environmentName);
        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();

        if (isPutRequestStale(request, oldEnvironmentConfig)) {
            throw haltBecauseEtagDoesNotMatch("environment", environmentName);
        }

        if (!StringUtils.equals(environmentName, environmentConfig.name().toString())) {
            throw haltBecauseRenameOfEntityIsNotSupported("environment");
        }

        environmentConfigService.updateEnvironment(environmentConfig.name().toString(),
                environmentConfig, currentUsername(), etagFor(oldEnvironmentConfig), operationResult);

        setEtagHeader(environmentConfig, response);
        return handleCreateOrUpdateResponse(request, response, environmentConfig, operationResult);
    }

    public String partialUpdate(Request request, Response response) {
        String environmentName = request.params("name");
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(request.body());
        PatchEnvironmentRequest patchRequest = PatchEnvironmentRequestRepresenter.fromJSON(jsonReader);
        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        EnvironmentConfig environmentConfig = fetchEntityFromConfig(environmentName);

        environmentConfigService.patchEnvironment(environmentConfig,
                patchRequest.getPipelineToAdd(),
                patchRequest.getPipelineToRemove(),
                patchRequest.getAgentsToAdd(),
                patchRequest.getAgentsToRemove(),
                patchRequest.getEnvironmentVariablesToAdd(),
                patchRequest.getEnvironmentVariablesToRemove(),
                currentUsername(),
                operationResult
        );

        EnvironmentConfig updateConfigElement = fetchEntityFromConfig(environmentName);

        setEtagHeader(updateConfigElement, response);
        return handleCreateOrUpdateResponse(request, response, updateConfigElement, operationResult);
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
    public EnvironmentConfig doFetchEntityFromConfig(String name) {
        ConfigElementForEdit<EnvironmentConfig> mergedEnvironmentforDisplay = environmentConfigService.getMergedEnvironmentforDisplay(name, new HttpLocalizedOperationResult());

        return mergedEnvironmentforDisplay == null ? null : mergedEnvironmentforDisplay.getConfigElement();
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

    private void haltIfEntityWithSameNameExists(EnvironmentConfig environmentConfig) {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ConfigElementForEdit<EnvironmentConfig> existingEnvironmentConfig = environmentConfigService.getMergedEnvironmentforDisplay(environmentConfig.name().toString(), result);

        if (existingEnvironmentConfig == null) {
            return;
        }

        environmentConfig.addError("name", format("Environment name should be unique. Environment with name '%s' already exists.", environmentConfig.name().toString()));
        throw haltBecauseEntityAlreadyExists(jsonWriter(environmentConfig), "environment", environmentConfig.name().toString());
    }

    private String calculateEtag(List<EnvironmentConfig> environmentConfigs) {
        final String environmentConfigSegment = environmentConfigs
                .stream()
                .map(this::etagFor)
                .collect(Collectors.joining(SEP_CHAR));

        return DigestUtils.sha256Hex(environmentConfigSegment);
    }
}