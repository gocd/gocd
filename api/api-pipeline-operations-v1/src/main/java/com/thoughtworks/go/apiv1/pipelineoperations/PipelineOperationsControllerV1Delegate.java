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
import com.thoughtworks.go.apiv1.pipelineoperations.representers.TriggerOptions;
import com.thoughtworks.go.apiv1.pipelineoperations.representers.TriggerWithOptionsViewRepresenter;
import com.thoughtworks.go.config.EnvironmentVariablesConfig;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PipelineHistoryService;
import com.thoughtworks.go.server.service.PipelinePauseService;
import com.thoughtworks.go.server.service.PipelineUnlockApiService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.spark.RequestContext;
import com.thoughtworks.go.spark.Routes;
import spark.Request;
import spark.Response;

import static spark.Spark.*;

public class PipelineOperationsControllerV1Delegate extends ApiController {
    private final PipelinePauseService pipelinePauseService;
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final Localizer localizer;
    private final PipelineUnlockApiService pipelineUnlockApiService;
    private GoConfigService goConfigService;
    private PipelineHistoryService pipelineHistoryService;

    public PipelineOperationsControllerV1Delegate(PipelinePauseService pipelinePauseService, PipelineUnlockApiService pipelineUnlockApiService, ApiAuthenticationHelper apiAuthenticationHelper, Localizer localizer, GoConfigService goConfigService, PipelineHistoryService pipelineHistoryService) {
        super(ApiVersion.v1);
        this.pipelinePauseService = pipelinePauseService;
        this.pipelineUnlockApiService = pipelineUnlockApiService;
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

            post(Routes.Pipeline.PAUSE_PATH, mimeType, this::pause, GsonTransformer.getInstance());
            post(Routes.Pipeline.UNPAUSE_PATH, mimeType, this::unpause, GsonTransformer.getInstance());
            post(Routes.Pipeline.UNLOCK_PATH, mimeType, this::unlock, GsonTransformer.getInstance());
            get(Routes.Pipeline.TRIGGER_OPTIONS_PATH, mimeType, this::triggerOptions, GsonTransformer.getInstance());

            exception(RecordNotFoundException.class, this::notFound);
        });
    }

    public Object pause(Request req, Response res) {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        JsonReader requestBody = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        String pipelineName = req.params("pipeline_name");
        String pauseCause = requestBody.optString("pause_cause").orElse(null);
        pipelinePauseService.pause(pipelineName, pauseCause, currentUsername(), result);
        return renderHTTPOperationResult(result, res, localizer);
    }

    public Object unpause(Request req, Response res) {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String pipelineName = req.params("pipeline_name");
        pipelinePauseService.unpause(pipelineName, currentUsername(), result);
        return renderHTTPOperationResult(result, res, localizer);
    }

    public Object unlock(Request req, Response res) {
        HttpOperationResult result = new HttpOperationResult();
        String pipelineName = req.params("pipeline_name");
        pipelineUnlockApiService.unlock(pipelineName, currentUsername(), result);
        return renderHTTPOperationResult(result, res);
    }

    public Object triggerOptions(Request request, Response response) {
        String pipelineName = request.params("pipeline_name");

        EnvironmentVariablesConfig variables = goConfigService.variablesFor(pipelineName);
        PipelineInstanceModel pipelineInstanceModel = pipelineHistoryService.latest(pipelineName, currentUsername());

        TriggerOptions triggerOptions = new TriggerOptions(variables, pipelineInstanceModel);
        return TriggerWithOptionsViewRepresenter.toJSON(triggerOptions, RequestContext.requestContext(request));
    }

}
