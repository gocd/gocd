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
package com.thoughtworks.go.apiv1.buildcause;


import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.buildcause.representers.BuildCauseRepresenter;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.server.service.PipelineHistoryService;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static spark.Spark.*;

@Component
public class BuildCauseController extends ApiController implements SparkSpringController {
    private final PipelineHistoryService pipelineHistoryService;
    private final ApiAuthenticationHelper apiAuthenticationHelper;

    @Autowired
    public BuildCauseController(PipelineHistoryService pipelineHistoryService, ApiAuthenticationHelper apiAuthenticationHelper) {
        super(ApiVersion.v1);
        this.pipelineHistoryService = pipelineHistoryService;
        this.apiAuthenticationHelper = apiAuthenticationHelper;
    }

    @Override
    public String controllerBasePath() {
        return Routes.BuildCause.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerPath(Routes.BuildCause.PATH), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", mimeType, apiAuthenticationHelper::checkPipelineViewPermissionsAnd403);
            before("/*", mimeType, apiAuthenticationHelper::checkPipelineViewPermissionsAnd403);

            before("", mimeType, this::verifyContentType);
            before("/*", mimeType, this::verifyContentType);

            get("", mimeType, this::index);
        });
    }

    public String index(Request req, Response res) throws IOException {
        HttpOperationResult httpOperationResult = new HttpOperationResult();

        int result;
        try {
            result = Integer.parseInt(req.params(":pipeline_counter"));
        } catch (NumberFormatException nfe) {
            throw new BadRequestException("Parameter `pipeline_counter` must be an integer.");
        }

        String pipelineName = req.params("pipeline_name");

        PipelineInstanceModel pipelineInstance = pipelineHistoryService.findPipelineInstance(pipelineName, result, currentUsername(), httpOperationResult);
        if (httpOperationResult.isSuccess()) {
            return writerForTopLevelObject(req, res, outputWriter -> BuildCauseRepresenter.toJSON(outputWriter, pipelineInstance.getBuildCause()));
        } else {
            return renderHTTPOperationResult(httpOperationResult, req, res);
        }
    }

}
