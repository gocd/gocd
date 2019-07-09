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

package com.thoughtworks.go.apiv8.admin.pipelineconfig;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv8.admin.shared.representers.PipelineConfigRepresenter;
import com.thoughtworks.go.apiv8.admin.shared.representers.stages.ConfigHelperOptions;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.HttpException;
import com.thoughtworks.go.config.materials.PasswordDeserializer;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PipelineConfigService;
import com.thoughtworks.go.server.service.PipelinePauseService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
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
import static com.thoughtworks.go.server.service.datasharing.DataSharingUsageDataService.SAVE_AND_RUN_CTA;
import static java.lang.String.format;
import static spark.Spark.*;

@Component
public class PipelineConfigControllerV8 extends ApiController implements SparkSpringController, CrudController<PipelineConfig> {
    private final PipelineConfigService pipelineConfigService;
    private final PipelinePauseService pipelinePauseService;
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final EntityHashingService entityHashingService;
    private final PasswordDeserializer passwordDeserializer;
    private GoConfigService goConfigService;
    private GoCache goCache;

    @Autowired
    public PipelineConfigControllerV8(PipelineConfigService pipelineConfigService, PipelinePauseService pipelinePauseService, ApiAuthenticationHelper apiAuthenticationHelper, EntityHashingService entityHashingService, PasswordDeserializer passwordDeserializer, GoConfigService goConfigService, GoCache goCache) {
        super(ApiVersion.v8);
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
            before(Routes.PipelineConfig.NAME, mimeType, apiAuthenticationHelper::checkPipelineGroupAdminUserAnd403);

            post("", mimeType, this::create);

            get(Routes.PipelineConfig.NAME, mimeType, this::show);
            put(Routes.PipelineConfig.NAME, mimeType, this::update);
            delete(Routes.PipelineConfig.NAME, mimeType, this::destroy);

            exception(HttpException.class, this::httpException);
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

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        pipelineConfigService.updatePipelineConfig(SessionUtils.currentUsername(), pipelineConfigFromRequest, etagFor(existingPipelineConfig), result);
        return handleCreateOrUpdateResponse(req, res, pipelineConfigFromRequest, result);
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
        } else if (Toggles.isToggleOn(Toggles.TEST_DRIVE)){
            goCache.put(SAVE_AND_RUN_CTA, true);
        }

        return handleCreateOrUpdateResponse(req, res, pipelineConfigFromRequest, result);
    }

    public String show(Request req, Response res) throws IOException {
        PipelineConfig pipelineConfig = fetchEntityFromConfig(req.params("pipeline_name"));
        if (isGetOrHeadRequestFresh(req, pipelineConfig)) {
            return notModified(res);
        } else {
            setEtagHeader(pipelineConfig, res);
            return writerForTopLevelObject(req, res, writer -> PipelineConfigRepresenter.toJSON(writer, pipelineConfig));
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
        return writer -> PipelineConfigRepresenter.toJSON(writer, pipelineConfig);
    }

    @Override
    public String etagFor(PipelineConfig pipelineConfig) {
        return entityHashingService.md5ForEntity(pipelineConfig);
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
