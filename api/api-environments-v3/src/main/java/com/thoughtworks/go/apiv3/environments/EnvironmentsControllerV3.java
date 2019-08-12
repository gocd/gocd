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
import com.thoughtworks.go.config.exceptions.HttpException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.merge.MergeEnvironmentConfig;
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
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.thoughtworks.go.api.representers.EnvironmentVariableRepresenter.toJSONArray;
import static com.thoughtworks.go.api.util.HaltApiResponses.*;
import static com.thoughtworks.go.apiv3.environments.representers.EnvironmentsRepresenter.toJSON;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Comparator.comparing;
import static java.util.Objects.hash;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static spark.Spark.*;

@Component
public class EnvironmentsControllerV3 extends ApiController implements SparkSpringController, CrudController<EnvironmentConfig> {

    private static final String SEP_CHAR = "/";
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

            before("", mimeType, apiAuthenticationHelper::checkAdminUserAnd403);
            before("/*", mimeType, apiAuthenticationHelper::checkAdminUserAnd403);
            get("", mimeType, this::index);
            get(Routes.Environments.NAME, mimeType, this::show);
            post("", mimeType, this::create);
            put(Routes.Environments.NAME, mimeType, this::update);
            patch(Routes.Environments.NAME, mimeType, this::partialUpdate);
            delete(Routes.Environments.NAME, mimeType, this::remove);

            exception(HttpException.class, this::httpException);
        });
    }

    public String index(Request request, Response response) throws IOException {
        Set<EnvironmentConfig> envConfigSet = environmentConfigService.getEnvironments();
        List<EnvironmentConfig> envViewModelList = filterUnknownAndSortEnvConfigs(envConfigSet);
        setEtagHeader(response, calculateEtag(envViewModelList));
        return writerForTopLevelObject(request, response, outputWriter -> toJSON(outputWriter, envViewModelList));
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

        if (!environmentConfigToCreate.getAllErrors().isEmpty()) {
            operationResult.unprocessableEntity("Error parsing environment config from the request");
            return handleCreateOrUpdateResponse(request, response, environmentConfigToCreate, operationResult);
        }

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

        if (!environmentConfig.getAllErrors().isEmpty()) {
            operationResult.unprocessableEntity("Error parsing environment config from the request");
            return handleCreateOrUpdateResponse(request, response, environmentConfig, operationResult);
        }

        if (isPutRequestStale(request, oldEnvironmentConfig)) {
            throw haltBecauseEtagDoesNotMatch("environment", environmentName);
        }

        if (!StringUtils.equals(environmentName, environmentConfig.name().toString())) {
            throw haltBecauseRenameOfEntityIsNotSupported("environment");
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
            String errorMessage = MessageJson.create("Error parsing patch request",
                    writer -> toJSONArray(writer, "environment_variables", configs));
            response.status(422);
            return errorMessage;
        }

        List<String> dummyAgentList = new ArrayList<>();
        environmentConfigService.patchEnvironment(environmentConfig, req.getPipelineToAdd(), req.getPipelineToRemove(),
                dummyAgentList, dummyAgentList, req.getEnvironmentVariablesToAdd(),
                req.getEnvironmentVariablesToRemove(), currentUsername(), result);

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
        if (entityFromServer instanceof MergeEnvironmentConfig || entityFromServer instanceof UnknownEnvironmentConfig) {
            return md5Hex(valueOf(hash(entityFromServer)));
        }
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
    public EnvironmentConfig fetchEntityFromConfig(String nameOrId) {
        EnvironmentConfig entity = doFetchEntityFromConfig(nameOrId);
        if (entity == null || entity.isUnknown()) {
            throw new RecordNotFoundException(getEntityType(), nameOrId);
        }
        return entity;
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

    List<EnvironmentConfig> filterUnknownAndSortEnvConfigs(Set<EnvironmentConfig> envConfigSet) {
        return envConfigSet.stream()
                .filter(envConfig -> !envConfig.isUnknown())
                .sorted(comparing(EnvironmentConfig::name))
                .collect(toList());
    }

    private void haltIfEntityWithSameNameExists(EnvironmentConfig environmentConfig) {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ConfigElementForEdit<EnvironmentConfig> existingEnvConfig = environmentConfigService.getMergedEnvironmentforDisplay(environmentConfig.name().toString(), result);
        if (existingEnvConfig == null || existingEnvConfig.getConfigElement().isUnknown()) {
            return;
        }

        environmentConfig.addError("name", format("Environment name should be unique. Environment with name '%s' already exists.", environmentConfig.name().toString()));
        throw haltBecauseEntityAlreadyExists(jsonWriter(environmentConfig), "environment", environmentConfig.name().toString());
    }

    private String calculateEtag(Collection<EnvironmentConfig> environmentConfigs) {
        final String environmentConfigSegment = environmentConfigs
                .stream()
                .map(this::etagFor)
                .collect(Collectors.joining(SEP_CHAR));

        return DigestUtils.sha256Hex(environmentConfigSegment);
    }
}
