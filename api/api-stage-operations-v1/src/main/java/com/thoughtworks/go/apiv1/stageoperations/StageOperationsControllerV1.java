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

package com.thoughtworks.go.apiv1.stageoperations;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.server.service.PipelineService;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.api.util.HaltApiResponses;
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
import java.util.List;

import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseOfReason;
import static spark.Spark.*;

@Component
public class StageOperationsControllerV1 extends ApiController implements SparkSpringController {
    private static final Logger LOGGER = LoggerFactory.getLogger(StageOperationsControllerV1.class);
    private final static String JOB_NAMES_PROPERTY = "jobs";

    private final ScheduleService scheduleService;
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final PipelineService pipelineService;

    @Autowired
    public StageOperationsControllerV1(ScheduleService scheduleService, ApiAuthenticationHelper apiAuthenticationHelper,
                                       PipelineService pipelineService) {
        super(ApiVersion.v1);
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
            before("", this::verifyContentType);
            before("/*", this::verifyContentType);

            before(Routes.Stage.TRIGGER_STAGE_PATH, mimeType, apiAuthenticationHelper::checkPipelineGroupOperateUserAnd403);
            before(Routes.Stage.TRIGGER_FAILED_JOBS_PATH, mimeType, apiAuthenticationHelper::checkPipelineGroupOperateUserAnd403);
            before(Routes.Stage.TRIGGER_SELECTED_JOBS_PATH, mimeType, apiAuthenticationHelper::checkPipelineGroupOperateUserAnd403);
            post(Routes.Stage.TRIGGER_STAGE_PATH, mimeType, this::triggerStage);
            post(Routes.Stage.TRIGGER_FAILED_JOBS_PATH, mimeType, this::rerunFailedJobs);
            post(Routes.Stage.TRIGGER_SELECTED_JOBS_PATH, mimeType, this::rerunSelectedJobs);
        });
    }

    public String rerunSelectedJobs(Request req, Response res) throws IOException {
        String pipelineName = req.params("pipeline_name");
        String pipelineCounter = req.params("pipeline_counter");
        String stageName = req.params("stage_name");
        HttpOperationResult result = new HttpOperationResult();

        validateRequestBody(req, res);

        JsonReader requestBody = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        List<String> requestedJobs = requestBody.readStringArrayIfPresent(JOB_NAMES_PROPERTY).get();

        scheduleService.rerunSelectedJobs(pipelineName, pipelineCounter, stageName, requestedJobs, result);
        return renderHTTPOperationResult(result, req, res);
    }

    public String rerunFailedJobs(Request req, Response res) throws IOException {
        String pipelineName = req.params("pipeline_name");
        String pipelineCounter = req.params("pipeline_counter");
        String stageName = req.params("stage_name");
        HttpOperationResult result = new HttpOperationResult();

        scheduleService.rerunFailedJobs(pipelineName, pipelineCounter, stageName, result);
        return renderHTTPOperationResult(result, req, res);
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

    private void validateRequestBody(Request req, Response res) {
        JsonReader requestBody = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        if (!requestBody.hasJsonObject(JOB_NAMES_PROPERTY)) {
            throw HaltApiResponses.haltBecauseOfReason(String.format("Could not read property '%s' in request body", JOB_NAMES_PROPERTY));
        }
        requestBody.readStringArrayIfPresent(JOB_NAMES_PROPERTY);
    }
}
