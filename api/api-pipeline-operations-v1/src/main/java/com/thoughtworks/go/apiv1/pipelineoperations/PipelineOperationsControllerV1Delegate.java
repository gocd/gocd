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

package com.thoughtworks.go.apiv1.pipelineoperations;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.api.util.HaltApiMessages;
import com.thoughtworks.go.api.util.MessageJson;
import com.thoughtworks.go.apiv1.pipelineoperations.exceptions.InvalidGoCipherTextException;
import com.thoughtworks.go.apiv1.pipelineoperations.representers.PipelineScheduleOptionsRepresenter;
import com.thoughtworks.go.apiv1.pipelineoperations.representers.TriggerOptions;
import com.thoughtworks.go.apiv1.pipelineoperations.representers.TriggerWithOptionsViewRepresenter;
import com.thoughtworks.go.config.EnvironmentVariablesConfig;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.server.domain.PipelineScheduleOptions;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.spark.Routes;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static spark.Spark.*;

public class PipelineOperationsControllerV1Delegate extends ApiController {
    private final PipelinePauseService pipelinePauseService;
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final Localizer localizer;
    private final PipelineUnlockApiService pipelineUnlockApiService;
    private PipelineTriggerService pipelineTriggerService;
    private GoConfigService goConfigService;
    private PipelineHistoryService pipelineHistoryService;

    public PipelineOperationsControllerV1Delegate(PipelinePauseService pipelinePauseService, PipelineUnlockApiService pipelineUnlockApiService, PipelineTriggerService pipelineTriggerService, ApiAuthenticationHelper apiAuthenticationHelper, Localizer localizer, GoConfigService goConfigService, PipelineHistoryService pipelineHistoryService) {
        super(ApiVersion.v1);
        this.pipelinePauseService = pipelinePauseService;
        this.pipelineUnlockApiService = pipelineUnlockApiService;
        this.pipelineTriggerService = pipelineTriggerService;
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.localizer = localizer;
        this.goConfigService = goConfigService;
        this.pipelineHistoryService = pipelineHistoryService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.Pipeline.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerPath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", this::verifyContentType);
            before("/*", this::verifyContentType);

            before(Routes.Pipeline.PAUSE_PATH, mimeType, apiAuthenticationHelper::checkPipelineGroupOperateUserAnd401);
            before(Routes.Pipeline.UNPAUSE_PATH, mimeType, apiAuthenticationHelper::checkPipelineGroupOperateUserAnd401);
            before(Routes.Pipeline.UNLOCK_PATH, mimeType, apiAuthenticationHelper::checkPipelineGroupOperateUserAnd401);
            before(Routes.Pipeline.TRIGGER_OPTIONS_PATH, mimeType, apiAuthenticationHelper::checkPipelineGroupOperateUserAnd401);
            before(Routes.Pipeline.SCHEDULE_PATH, mimeType, apiAuthenticationHelper::checkPipelineGroupOperateUserAnd401);

            post(Routes.Pipeline.PAUSE_PATH, mimeType, this::pause);
            post(Routes.Pipeline.UNPAUSE_PATH, mimeType, this::unpause);
            post(Routes.Pipeline.UNLOCK_PATH, mimeType, this::unlock);
            get(Routes.Pipeline.TRIGGER_OPTIONS_PATH, mimeType, this::triggerOptions);
            post(Routes.Pipeline.SCHEDULE_PATH, mimeType, this::schedule);

            exception(RecordNotFoundException.class, this::notFound);
            exception(InvalidGoCipherTextException.class, (InvalidGoCipherTextException exception, Request request, Response response) -> {
                response.status(HttpStatus.INTERNAL_SERVER_ERROR.value());
                response.body(MessageJson.create(HaltApiMessages.errorWhileEncryptingMessage()));
            });
        });
    }

    public String pause(Request req, Response res) throws IOException {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        JsonReader requestBody = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        String pipelineName = req.params("pipeline_name");
        String pauseCause = requestBody.optString("pause_cause").orElse(null);
        pipelinePauseService.pause(pipelineName, pauseCause, currentUsername(), result);
        return renderHTTPOperationResult(result, req, res, localizer);
    }

    public String unpause(Request req, Response res) throws IOException {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String pipelineName = req.params("pipeline_name");
        pipelinePauseService.unpause(pipelineName, currentUsername(), result);
        return renderHTTPOperationResult(result, req, res, localizer);
    }

    public String unlock(Request req, Response res) throws IOException {
        HttpOperationResult result = new HttpOperationResult();
        String pipelineName = req.params("pipeline_name");
        pipelineUnlockApiService.unlock(pipelineName, currentUsername(), result);
        return renderHTTPOperationResult(result, req, res);
    }

    public String triggerOptions(Request request, Response response) throws IOException {
        String pipelineName = request.params("pipeline_name");

        EnvironmentVariablesConfig variables = goConfigService.variablesFor(pipelineName);
        PipelineInstanceModel pipelineInstanceModel = pipelineHistoryService.latest(pipelineName, currentUsername());

        TriggerOptions triggerOptions = new TriggerOptions(variables, pipelineInstanceModel);

        return writerForTopLevelObject(request, response, writer -> TriggerWithOptionsViewRepresenter.toJSON(writer, triggerOptions));
    }

    public String schedule(Request req, Response res) throws IOException {
        HttpOperationResult result = new HttpOperationResult();
        String pipelineName = req.params("pipeline_name");
        pipelineTriggerService.schedule(pipelineName, getScheduleOptions(req), currentUsername(), result);
        return renderHTTPOperationResult(result, req, res);
    }

    private PipelineScheduleOptions getScheduleOptions(Request req) {
        if (StringUtils.isBlank(req.body())) {
            return new PipelineScheduleOptions();
        }
        GsonTransformer gsonTransformer = GsonTransformer.getInstance();
        JsonReader jsonReader = gsonTransformer.jsonReaderFrom(req.body());
        return PipelineScheduleOptionsRepresenter.fromJSON(jsonReader);
    }
}
