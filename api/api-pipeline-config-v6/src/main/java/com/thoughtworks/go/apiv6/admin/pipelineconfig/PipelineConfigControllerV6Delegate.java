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

package com.thoughtworks.go.apiv6.admin.pipelineconfig;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv6.shared.representers.stages.ConfigHelperOptions;
import com.thoughtworks.go.apiv6.admin.pipelineconfig.representers.PipelineConfigRepresenter;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.materials.PasswordDeserializer;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PipelineConfigService;
import com.thoughtworks.go.server.service.PipelinePauseService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import org.apache.commons.lang3.StringUtils;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static com.thoughtworks.go.api.util.HaltApiResponses.*;
import static spark.Spark.*;

public class PipelineConfigControllerV6Delegate extends ApiController implements CrudController<PipelineConfig> {

    private final PipelineConfigService pipelineConfigService;
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final EntityHashingService entityHashingService;
    private final PasswordDeserializer passwordDeserializer;
    private GoConfigService goConfigService;
    private PipelinePauseService pipelinePauseService;

    public PipelineConfigControllerV6Delegate(PipelineConfigService pipelineConfigService, ApiAuthenticationHelper apiAuthenticationHelper, EntityHashingService entityHashingService, PasswordDeserializer passwordDeserializer, GoConfigService goConfigService, PipelinePauseService pipelinePauseService) {
        super(ApiVersion.v6);
        this.pipelineConfigService = pipelineConfigService;
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.entityHashingService = entityHashingService;
        this.passwordDeserializer = passwordDeserializer;
        this.goConfigService = goConfigService;
        this.pipelinePauseService = pipelinePauseService;
    }

    @Override
    public String etagFor(PipelineConfig pipelineConfig) {
        return entityHashingService.md5ForEntity(pipelineConfig);
    }

    @Override
    public PipelineConfig doGetEntityFromConfig(String name) {
        return pipelineConfigService.getPipelineConfig(name);
    }

    @Override
    public PipelineConfig getEntityFromRequestBody(Request req) {
        ConfigHelperOptions options = new ConfigHelperOptions(goConfigService.getCurrentConfig(), passwordDeserializer);
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        if ("PUT".equalsIgnoreCase(req.requestMethod())) {
            return PipelineConfigRepresenter.fromJSON(jsonReader, options);
        }
        return PipelineConfigRepresenter.fromJSON(jsonReader.readJsonObject("pipeline"), options);
    }

    @Override
    public String jsonize(Request req, PipelineConfig pipelineConfig) {
        return jsonizeAsTopLevelObject(req, writer -> PipelineConfigRepresenter.toJSON(writer, pipelineConfig));
    }

    @Override
    public JsonNode jsonNode(Request req, PipelineConfig pipelineConfig) throws IOException {
        String jsonize = jsonize(req, pipelineConfig);
        return new ObjectMapper().readTree(jsonize);
    }

    @Override
    public String controllerBasePath() {
        return Routes.PipelineConfig.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerPath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", this::verifyContentType);
            before("/*", this::verifyContentType);
            before("", mimeType, apiAuthenticationHelper::checkPipelineCreationAuthorizationAnd403);
            before(Routes.PipelineConfig.NAME, mimeType, apiAuthenticationHelper::checkPipelineGroupAdminUserAnd403);

            post("", mimeType, this::create);

            get(Routes.PipelineConfig.NAME, mimeType, this::show);
            put(Routes.PipelineConfig.NAME, mimeType, this::update);
            delete(Routes.PipelineConfig.NAME, mimeType, this::destroy);

            exception(RecordNotFoundException.class, this::notFound);
        });
    }

    public String destroy(Request req, Response res) throws IOException {
        PipelineConfig existingPipelineConfig = getEntityFromConfig(req.params("pipeline_name"));
        haltIfPipelineIsDefinedRemotely(existingPipelineConfig);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        pipelineConfigService.deletePipelineConfig(SessionUtils.currentUsername(), existingPipelineConfig, result);


        return renderHTTPOperationResult(result, req, res);
    }

    public String update(Request req, Response res) throws IOException {
        PipelineConfig existingPipelineConfig =  getEntityFromConfig(req.params("pipeline_name"));
        PipelineConfig pipelineConfigFromRequest = getEntityFromRequestBody(req);

        if (isRenameAttempt(existingPipelineConfig, pipelineConfigFromRequest)) {
            throw haltBecauseRenameOfEntityIsNotSupported("pipelines");
        }

        haltIfPipelineIsDefinedRemotely(existingPipelineConfig);
        if (!isPutRequestFresh(req, existingPipelineConfig)) {
            throw haltBecauseEtagDoesNotMatch("pipeline", existingPipelineConfig.getName());
        }

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        pipelineConfigService.updatePipelineConfig(SessionUtils.currentUsername(), pipelineConfigFromRequest,  etagFor(existingPipelineConfig), result);
        return handleCreateOrUpdateResponse(req, res, pipelineConfigFromRequest, result);
    }

    public String create(Request req, Response res) throws IOException {
        PipelineConfig pipelineConfigFromRequest = getEntityFromRequestBody(req);

        haltIfEntityBySameNameInRequestExists(req, pipelineConfigFromRequest);
        String group = getOrHaltForGroupName(req);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Username userName = SessionUtils.currentUsername();
        pipelineConfigService.createPipelineConfig(userName, pipelineConfigFromRequest, result, group);

        if (result.isSuccessful()) {
            pipelinePauseService.pause(pipelineConfigFromRequest.name().toString(), "Under construction", userName);
        }
        return handleCreateOrUpdateResponse(req, res, pipelineConfigFromRequest, result);
    }

    public String show(Request req, Response res) throws IOException {
        PipelineConfig pipelineConfig = getEntityFromConfig(req.params("pipeline_name"));
        if (isGetOrHeadRequestFresh(req, pipelineConfig)) {
            return notModified(res);
        } else {
            setEtagHeader(pipelineConfig, res);
            return writerForTopLevelObject(req, res, writer -> PipelineConfigRepresenter.toJSON(writer, pipelineConfig));
        }
    }

    private void haltIfPipelineIsDefinedRemotely(PipelineConfig existingPipelineConfig) {
        if (!existingPipelineConfig.isLocal()) {
            throw haltBecauseOfReason(String.format("Can not operate on pipeline '%s' as it is defined remotely in '%s'.", existingPipelineConfig.name(), existingPipelineConfig.getOrigin().displayName()));
        }
    }

    private boolean isRenameAttempt(PipelineConfig existingPipelineConfig, PipelineConfig pipelineConfigFromRequest) {
        return !existingPipelineConfig.getName().equals(pipelineConfigFromRequest.getName());
    }

    private String getOrHaltForGroupName(Request req) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        if (!jsonReader.hasJsonObject("group") || StringUtils.isBlank(jsonReader.getString("group"))) {
            throw haltBecauseOfReason("Pipeline group must be specified for creating a pipeline.");
        }
        return  jsonReader.getString("group");
    }


    private void haltIfEntityBySameNameInRequestExists(Request req, PipelineConfig pipelineConfig) throws IOException {
        if (pipelineConfigService.getPipelineConfig(pipelineConfig.name().toString()) == null) {
            return;
        }
        pipelineConfig.addError("name", LocalizedMessage.resourceAlreadyExists("pipeline", pipelineConfig.name().toString()));
        throw haltBecauseEntityAlreadyExists(jsonNode(req, pipelineConfig), "pipeline", pipelineConfig.getName());
    }
}
