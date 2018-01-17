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
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.service.PipelinePauseService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import spark.Request;
import spark.Response;

import java.util.Map;

import static spark.Spark.*;

public class PipelineOperationsControllerV1Delegate extends ApiController {
    private final PipelinePauseService pipelinePauseService;
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final Localizer localizer;

    public PipelineOperationsControllerV1Delegate(PipelinePauseService pipelinePauseService, ApiAuthenticationHelper apiAuthenticationHelper, Localizer localizer) {
        super(ApiVersion.v1);
        this.pipelinePauseService = pipelinePauseService;
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.localizer = localizer;
    }

    @Override
    public String controllerBasePath() {
        return "/api/pipelines";
    }

    @Override
    public void setupRoutes() {
        path(controllerPath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", this::verifyContentType);
            before("/*", this::verifyContentType);

            before("/:pipeline_name/pause", mimeType, apiAuthenticationHelper::checkPipelineGroupOperateUserAnd401);
            before("/:pipeline_name/unpause", mimeType, apiAuthenticationHelper::checkPipelineGroupOperateUserAnd401);

            post("/:pipeline_name/pause", mimeType, this::pause, GsonTransformer.getInstance());
            post("/:pipeline_name/unpause", mimeType, this::unpause, GsonTransformer.getInstance());

            exception(RecordNotFoundException.class, this::notFound);
        });
    }

    public Map pause(Request req, Response res) {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Map<String, String> requestBody = readRequestBodyAsJSON(req);
        String pipelineName = req.params("pipeline_name");
        String pauseCause = requestBody.get("pause_cause");
        pipelinePauseService.pause(pipelineName, pauseCause, currentUsername(), result);
        return renderHTTPOperationResult(result, res, localizer);
    }

    public Map unpause(Request req, Response res) {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String pipelineName = req.params("pipeline_name");
        pipelinePauseService.unpause(pipelineName, currentUsername(), result);
        return renderHTTPOperationResult(result, res, localizer);
    }
}
