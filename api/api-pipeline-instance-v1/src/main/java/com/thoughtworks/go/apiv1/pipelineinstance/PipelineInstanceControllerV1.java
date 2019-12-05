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

package com.thoughtworks.go.apiv1.pipelineinstance;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.pipelineinstance.representers.PipelineInstanceModelRepresenter;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;
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
public class PipelineInstanceControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final PipelineHistoryService pipelineHistoryService;

    @Autowired
    public PipelineInstanceControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, PipelineHistoryService pipelineHistoryService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.pipelineHistoryService = pipelineHistoryService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.Pipeline.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before(Routes.Pipeline.INSTANCE_PATH, mimeType, apiAuthenticationHelper::checkPipelineViewPermissionsAnd403);

            get(Routes.Pipeline.INSTANCE_PATH, mimeType, this::getInstanceInfo);
        });
    }

    String getInstanceInfo(Request request, Response response) throws IOException {
        String pipelineName = request.params("pipeline_name");
        Integer pipelineCounter = getCounterValue(request);
        HttpOperationResult result = new HttpOperationResult();
        PipelineInstanceModel pipelineInstance = pipelineHistoryService.findPipelineInstance(pipelineName, pipelineCounter, currentUsername(), result);
        if (result.canContinue()) {
            return writerForTopLevelObject(request, response, (outputWriter) -> PipelineInstanceModelRepresenter.toJSON(outputWriter, pipelineInstance));
        }
        return renderHTTPOperationResult(result, request, response);
    }

    private Integer getCounterValue(Request request) {
        try {
            int counter = Integer.parseInt(request.params("pipeline_counter"));
            if (counter < 1) {
                throw new UnprocessableEntityException("The pipeline counter cannot be less than 1.");
            }
            return counter;
        } catch (NumberFormatException ex) {
            throw new UnprocessableEntityException("The pipeline counter should be an integer.");
        }
    }
}
