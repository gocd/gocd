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
package com.thoughtworks.go.apiv2.stageoperations;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.server.service.PipelineService;
import com.thoughtworks.go.server.service.ScheduleService;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.Optional;

import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseOfReason;
import static spark.Spark.*;

@Component
public class StageOperationsControllerV2 extends ApiController implements SparkSpringController {
    private static final Logger LOGGER = LoggerFactory.getLogger(StageOperationsControllerV2.class);

    private final ScheduleService scheduleService;
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final PipelineService pipelineService;

    @Autowired
    public StageOperationsControllerV2(ScheduleService scheduleService, ApiAuthenticationHelper apiAuthenticationHelper, PipelineService pipelineService) {
        super(ApiVersion.v2);
        this.scheduleService = scheduleService;
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.pipelineService = pipelineService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.Stage.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerPath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", mimeType, this::verifyContentType);
            before("/*", mimeType, this::verifyContentType);

            before(Routes.Stage.TRIGGER_STAGE_PATH, mimeType, apiAuthenticationHelper::checkPipelineGroupOperateOfPipelineOrGroupInURLUserAnd403);

            post(Routes.Stage.TRIGGER_STAGE_PATH, mimeType, this::triggerStage);
        });
    }

    public String triggerStage(Request req, Response res) throws IOException {
        String pipelineName = req.params("pipeline_name");
        String pipelineCounter = req.params("pipeline_counter");
        String stageName = req.params("stage_name");
        HttpOperationResult result = new HttpOperationResult();

        Optional<Integer> pipelineCounterValue = pipelineService.resolvePipelineCounter(pipelineName, pipelineCounter);
        if (!pipelineCounterValue.isPresent()) {
            String errorMessage = String.format("Error while running [%s/%s/%s]. Received non-numeric pipeline counter '%s'.", pipelineName, pipelineCounter, stageName, pipelineCounter);
            LOGGER.error(errorMessage);
            throw haltBecauseOfReason(errorMessage);
        }

        scheduleService.rerunStage(pipelineName, pipelineCounterValue.get(), stageName, result);
        return renderHTTPOperationResult(result, req, res);
    }
}
