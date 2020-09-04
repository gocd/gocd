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
package com.thoughtworks.go.apiv11.admin.pipelineconfig;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv11.admin.shared.representers.PipelineConfigRepresenter;
import com.thoughtworks.go.apiv11.admin.shared.representers.stages.ConfigHelperOptions;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.materials.PasswordDeserializer;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PipelineConfigService;
import com.thoughtworks.go.server.service.PipelinePauseService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.function.Consumer;

import static com.thoughtworks.go.api.util.HaltApiResponses.*;
import static java.lang.String.format;
import static spark.Spark.*;

@Component
public class PipelineConfigControllerV11 extends ApiController implements SparkSpringController, CrudController<PipelineConfig> {
    private final PipelineConfigService pipelineConfigService;
    private final PipelinePauseService pipelinePauseService;
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final EntityHashingService entityHashingService;
    private final PasswordDeserializer passwordDeserializer;
    private GoConfigService goConfigService;
    private GoCache goCache;

    @Autowired
    public PipelineConfigControllerV11(PipelineConfigService pipelineConfigService,
                                       PipelinePauseService pipelinePauseService,
                                       ApiAuthenticationHelper apiAuthenticationHelper,
                                       EntityHashingService entityHashingService,
                                       PasswordDeserializer passwordDeserializer,
                                       GoConfigService goConfigService,
                                       GoCache goCache) {
        super(ApiVersion.v11);
        this.pipelineConfigService = pipelineConfigService;
        this.pipelinePauseService = pipelinePauseService;
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.entityHashingService = entityHashingService;
        this.passwordDeserializer = passwordDeserializer;
        this.goConfigService = goConfigService;
        this.goCache = goCache;
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
            before("", mimeType, this::verifyContentType);
            before("/*", mimeType, this::verifyContentType);
            before("", mimeType, apiAuthenticationHelper::checkPipelineCreationAuthorizationAnd403);
            before(Routes.PipelineConfig.NAME, mimeType, apiAuthenticationHelper::checkPipelineGroupAdminOfPipelineOrGroupInURLUserAnd403);
            before(Routes.PipelineConfig.EXTRACT_TO_TEMPLATE, mimeType, apiAuthenticationHelper::checkPipelineGroupAdminOfPipelineOrGroupInURLUserAnd403);

            post("", mimeType, this::create);

            get(Routes.PipelineConfig.NAME, mimeType, this::show);
            put(Routes.PipelineConfig.NAME, mimeType, this::update);
            delete(Routes.PipelineConfig.NAME, mimeType, this::destroy);
            put(Routes.PipelineConfig.EXTRACT_TO_TEMPLATE, mimeType, this::extractToTemplate);
        });
    }

    public String destroy(Request req, Response res) throws IOException {
        PipelineConfig existingPipelineConfig = fetchEntityFromConfig(req.params("pipeline_name"));
        haltIfPipelineIsDefinedRemotely(existingPipelineConfig);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        pipelineConfigService.deletePipelineConfig(SessionUtils.currentUsername(), existingPipelineConfig, result);

        return renderHTTPOperationResult(result, req, res);
    }

    public String update(Request req, Response res) {
        PipelineConfig existingPipelineConfig = fetchEntityFromConfig(req.params("pipeline_name"));
        PipelineConfig pipelineConfigFromRequest = buildEntityFromRequestBody(req);

        if (isRenameAttempt(existingPipelineConfig, pipelineConfigFromRequest)) {
            throw haltBecauseRenameOfEntityIsNotSupported("pipelines");
        }

        haltIfPipelineIsDefinedRemotely(existingPipelineConfig);
        if (isPutRequestStale(req, existingPipelineConfig)) {
            throw haltBecauseEtagDoesNotMatch("pipeline", existingPipelineConfig.getName());
        }

        String groupName = getOrHaltForGroupName(req);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        pipelineConfigService.updatePipelineConfig(SessionUtils.currentUsername(), pipelineConfigFromRequest, groupName, etagFor(existingPipelineConfig), result);
        return handleCreateOrUpdateResponse(req, res, pipelineConfigFromRequest, result);
    }

    public String extractToTemplate(Request req, Response res) throws IOException {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());

        String pipelineName = req.params("pipeline_name");
        String templateName = jsonReader.getString("template_name");

        pipelineConfigService.extractTemplateFromPipeline(pipelineName, templateName, currentUsername());

        return show(req, res);
    }

    private boolean shouldPausePipeline(Request req) {
        String pausePipelineHeader = req.headers("X-pause-pipeline");
        return Boolean.valueOf(pausePipelineHeader);
    }

    private String getUserSpecifiedOrDefaultPauseCause(Request req) {
        String pauseCauseHeaderVal = req.headers("X-pause-cause");
        return (StringUtils.isBlank(pauseCauseHeaderVal)
                ? "No pause cause was specified when pipeline was created via API"
                : pauseCauseHeaderVal);
    }

    public String create(Request req, Response res) {
        PipelineConfig pipelineConfigFromRequest = buildEntityFromRequestBody(req);

        haltIfEntityBySameNameInRequestExists(pipelineConfigFromRequest);
        String group = getOrHaltForGroupName(req);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Username userName = SessionUtils.currentUsername();
        pipelineConfigService.createPipelineConfig(userName, pipelineConfigFromRequest, result, group);

        if (shouldPausePipeline(req)) {
            String pauseCause = getUserSpecifiedOrDefaultPauseCause(req);
            pipelinePauseService.pause(pipelineConfigFromRequest.name().toString(), pauseCause, userName);
        }

        return handleCreateOrUpdateResponse(req, res, pipelineConfigFromRequest, result);
    }

    public String show(Request req, Response res) throws IOException {
        String pipelineName = req.params("pipeline_name");
        PipelineConfig pipelineConfig = fetchEntityFromConfig(pipelineName);
        String groupName = goConfigService.findGroupNameByPipeline(new CaseInsensitiveString(pipelineName));

        if (isGetOrHeadRequestFresh(req, pipelineConfig)) {
            return notModified(res);
        } else {
            setEtagHeader(pipelineConfig, res);
            return writerForTopLevelObject(req, res, writer -> PipelineConfigRepresenter.toJSON(writer, pipelineConfig, groupName));
        }
    }

    @Override
    public PipelineConfig doFetchEntityFromConfig(String name) {
        return pipelineConfigService.getPipelineConfig(name);
    }

    @Override
    public PipelineConfig buildEntityFromRequestBody(Request req) {
        ConfigHelperOptions options = new ConfigHelperOptions(goConfigService.getCurrentConfig(), passwordDeserializer);
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        if ("PUT".equalsIgnoreCase(req.requestMethod())) {
            return PipelineConfigRepresenter.fromJSON(jsonReader, options);
        }
        return PipelineConfigRepresenter.fromJSON(jsonReader.readJsonObject("pipeline"), options);
    }

    @Override
    public Consumer<OutputWriter> jsonWriter(PipelineConfig pipelineConfig) {
        String groupName = goConfigService.findGroupNameByPipeline(pipelineConfig.name());
        return writer -> PipelineConfigRepresenter.toJSON(writer, pipelineConfig, groupName);
    }

    @Override
    public String etagFor(PipelineConfig pipelineConfig) {
        String groupName = goConfigService.findGroupNameByPipeline(pipelineConfig.name());
        return entityHashingService.hashForEntity(pipelineConfig, groupName);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.Pipeline;
    }

    private void haltIfPipelineIsDefinedRemotely(PipelineConfig existingPipelineConfig) {
        if (!existingPipelineConfig.isLocal()) {
            throw haltBecauseOfReason(format("Can not operate on pipeline '%s' as it is defined remotely in '%s'.", existingPipelineConfig.name(), existingPipelineConfig.getOrigin().displayName()));
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
        return jsonReader.getString("group");
    }


    private void haltIfEntityBySameNameInRequestExists(PipelineConfig pipelineConfig) {
        if (pipelineConfigService.getPipelineConfig(pipelineConfig.name().toString()) == null) {
            return;
        }
        pipelineConfig.addError("name", EntityType.Pipeline.alreadyExists(pipelineConfig.name()));
        throw haltBecauseEntityAlreadyExists(jsonWriter(pipelineConfig), "pipeline", pipelineConfig.getName());
    }
}
