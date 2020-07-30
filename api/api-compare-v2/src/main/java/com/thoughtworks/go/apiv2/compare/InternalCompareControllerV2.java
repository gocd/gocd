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
package com.thoughtworks.go.apiv2.compare;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.HistoryMethods;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv2.compare.representers.PipelineInstanceModelsRepresenter;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels;
import com.thoughtworks.go.server.service.PipelineHistoryService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static spark.Spark.*;

@Component
public class InternalCompareControllerV2 extends ApiController implements SparkSpringController, HistoryMethods {
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final PipelineHistoryService pipelineHistoryService;

    @Autowired
    public InternalCompareControllerV2(ApiAuthenticationHelper apiAuthenticationHelper, PipelineHistoryService pipelineHistoryService) {
        super(ApiVersion.v2);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.pipelineHistoryService = pipelineHistoryService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.CompareAPI.INTERNAL_BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("/*", mimeType, this::setContentType);
            before("/*", mimeType, this::verifyContentType);

            before(Routes.CompareAPI.INTERNAL_LIST, mimeType, this.apiAuthenticationHelper::checkPipelineViewPermissionsAnd403);

            get(Routes.CompareAPI.INTERNAL_LIST, mimeType, this::list);
        });
    }

    String list(Request request, Response response) throws IOException {
        String pipelineName = request.params("pipeline_name");
        Integer pageSize = getPageSize(request);
        String pattern = request.queryParamOrDefault("pattern", "");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        PipelineInstanceModels pipelineInstanceModels = pipelineHistoryService.findMatchingPipelineInstances(pipelineName, pattern, pageSize, currentUsername(), result);
        if (result.isSuccessful()) {
            return writerForTopLevelObject(request, response, outputWriter -> PipelineInstanceModelsRepresenter.toJSON(outputWriter, pipelineInstanceModels));
        } else {
            return renderHTTPOperationResult(result, request, response);
        }
    }
}
