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
package com.thoughtworks.go.apiv1.pipelineinstance;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.pipelineinstance.representers.PipelineInstanceModelRepresenter;
import com.thoughtworks.go.apiv1.pipelineinstance.representers.PipelineInstanceModelsRepresenter;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;
import com.thoughtworks.go.domain.PipelineRunIdInfo;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels;
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
        return Routes.PipelineInstance.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("/*", mimeType, this::setContentType);
            before("/*", mimeType, this::verifyContentType);

            before(Routes.PipelineInstance.INSTANCE_PATH, mimeType, apiAuthenticationHelper::checkPipelineViewPermissionsAnd403);
            before(Routes.PipelineInstance.HISTORY_PATH, mimeType, apiAuthenticationHelper::checkPipelineViewPermissionsAnd403);
            before(Routes.PipelineInstance.COMMENT_PATH, mimeType, apiAuthenticationHelper::checkPipelineGroupOperateOfPipelineOrGroupInURLUserAnd403);

            get(Routes.PipelineInstance.HISTORY_PATH, mimeType, this::getHistoryInfo);
            get(Routes.PipelineInstance.INSTANCE_PATH, mimeType, this::getInstanceInfo);
            post(Routes.PipelineInstance.COMMENT_PATH, mimeType, this::comment);
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

    String getHistoryInfo(Request request, Response response) throws IOException {
        String pipelineName = request.params("pipeline_name");
        Integer pageSize = getPageSize(request);
        Long after = getCursor(request, "after");
        Long before = getCursor(request, "before");
        PipelineInstanceModels pipelineInstanceModels = pipelineHistoryService.loadPipelineHistoryData(currentUsername(), pipelineName, after, before, pageSize);
        PipelineRunIdInfo latestAndOldestPipelineIds = pipelineHistoryService.getOldestAndLatestPipelineId(pipelineName, currentUsername());
        return writerForTopLevelObject(request, response, (outputWriter) -> PipelineInstanceModelsRepresenter.toJSON(outputWriter, pipelineInstanceModels, latestAndOldestPipelineIds));
    }

    String comment(Request request, Response response) {
        String pipelineName = request.params("pipeline_name");
        Integer pipelineCounter = getCounterValue(request);

        JsonReader reader = GsonTransformer.getInstance().jsonReaderFrom(request.body());

        pipelineHistoryService.updateComment(pipelineName, pipelineCounter, reader.getString("comment"), currentUsername());

        return renderMessage(response, 200, "Comment successfully updated.");
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
