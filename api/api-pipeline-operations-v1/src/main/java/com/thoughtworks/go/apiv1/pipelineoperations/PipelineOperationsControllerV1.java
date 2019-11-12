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

package com.thoughtworks.go.apiv1.pipelineoperations;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.pipelineoperations.representers.PipelineScheduleOptionsRepresenter;
import com.thoughtworks.go.apiv1.pipelineoperations.representers.TriggerOptions;
import com.thoughtworks.go.apiv1.pipelineoperations.representers.TriggerWithOptionsViewRepresenter;
import com.thoughtworks.go.apiv1.pipelineoperations.representers.*;
import com.thoughtworks.go.config.EnvironmentVariablesConfig;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;
import com.thoughtworks.go.presentation.PipelineStatusModel;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.server.domain.PipelineScheduleOptions;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static spark.Spark.*;

@Component
public class PipelineOperationsControllerV1 extends ApiController implements SparkSpringController {
    private final PipelinePauseService pipelinePauseService;
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final PipelineUnlockApiService pipelineUnlockApiService;
    private final PipelineTriggerService pipelineTriggerService;
    private final GoConfigService goConfigService;
    private final PipelineHistoryService pipelineHistoryService;

    @Autowired
    public PipelineOperationsControllerV1(PipelinePauseService pipelinePauseService, PipelineUnlockApiService pipelineUnlockApiService, PipelineTriggerService pipelineTriggerService, ApiAuthenticationHelper apiAuthenticationHelper, GoConfigService goConfigService, PipelineHistoryService pipelineHistoryService) {
        super(ApiVersion.v1);
        this.pipelinePauseService = pipelinePauseService;
        this.pipelineUnlockApiService = pipelineUnlockApiService;
        this.pipelineTriggerService = pipelineTriggerService;
        this.apiAuthenticationHelper = apiAuthenticationHelper;
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
            before("", mimeType, this::verifyContentType);
            before("/*", mimeType, this::verifyContentType);

            before(Routes.Pipeline.PAUSE_PATH, mimeType, apiAuthenticationHelper::checkPipelineGroupOperateOfPipelineOrGroupInURLUserAnd403);
            before(Routes.Pipeline.UNPAUSE_PATH, mimeType, apiAuthenticationHelper::checkPipelineGroupOperateOfPipelineOrGroupInURLUserAnd403);
            before(Routes.Pipeline.UNLOCK_PATH, mimeType, apiAuthenticationHelper::checkPipelineGroupOperateOfPipelineOrGroupInURLUserAnd403);
            before(Routes.Pipeline.TRIGGER_OPTIONS_PATH, mimeType, apiAuthenticationHelper::checkPipelineGroupOperateOfPipelineOrGroupInURLUserAnd403);
            before(Routes.Pipeline.SCHEDULE_PATH, mimeType, apiAuthenticationHelper::checkPipelineGroupOperateOfPipelineOrGroupInURLUserAnd403);
            before(Routes.Pipeline.STATUS_PATH, mimeType, apiAuthenticationHelper::checkPipelineGroupOperateOfPipelineOrGroupInURLUserAnd403);

            post(Routes.Pipeline.PAUSE_PATH, mimeType, this::pause);
            post(Routes.Pipeline.UNPAUSE_PATH, mimeType, this::unpause);
            post(Routes.Pipeline.UNLOCK_PATH, mimeType, this::unlock);
            get(Routes.Pipeline.TRIGGER_OPTIONS_PATH, mimeType, this::triggerOptions);
            post(Routes.Pipeline.SCHEDULE_PATH, mimeType, this::schedule);
            get(Routes.Pipeline.STATUS_PATH, mimeType, this::getStatusInfo);
        });
    }

    public String pause(Request req, Response res) throws IOException {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        JsonReader requestBody = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        String pipelineName = req.params("pipeline_name");
        String pauseCause = requestBody.optString("pause_cause").orElse(null);
        pipelinePauseService.pause(pipelineName, pauseCause, currentUsername(), result);
        return renderHTTPOperationResult(result, req, res);
    }

    public String unpause(Request req, Response res) throws IOException {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String pipelineName = req.params("pipeline_name");
        pipelinePauseService.unpause(pipelineName, currentUsername(), result);
        return renderHTTPOperationResult(result, req, res);
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

    String getStatusInfo(Request request, Response response) throws IOException {
        String pipelineName = request.params("pipeline_name");
        HttpOperationResult result = new HttpOperationResult();
        PipelineStatusModel pipelineStatus = pipelineHistoryService.getPipelineStatus(pipelineName, currentUsernameString(), result);
        if (result.canContinue()) {
            return writerForTopLevelObject(request, response, (outputWriter) -> PipelineStatusModelRepresenter.toJSON(outputWriter, pipelineStatus));
        }
        return renderHTTPOperationResult(result, request, response);
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
