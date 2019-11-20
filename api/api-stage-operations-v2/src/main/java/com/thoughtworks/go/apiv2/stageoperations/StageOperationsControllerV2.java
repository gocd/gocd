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

package com.thoughtworks.go.apiv2.stageoperations;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.api.util.HaltApiResponses;
import com.thoughtworks.go.apiv2.stageoperations.representers.StageInstancesRepresenter;
import com.thoughtworks.go.apiv2.stageoperations.representers.StageRepresenter;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.NullStage;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels;
import com.thoughtworks.go.server.service.PipelineService;
import com.thoughtworks.go.server.service.ScheduleService;
import com.thoughtworks.go.server.service.StageService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseOfReason;
import static spark.Spark.*;

@Component
public class StageOperationsControllerV2 extends ApiController implements SparkSpringController {
    static final String BAD_PAGE_SIZE_MSG = "The query parameter `page_size`, if specified must be a number between 10 and 100.";
    static final String BAD_OFFSET_MSG = "The query parameter `offset`, if specified must be a number greater or equal to 0.";
    private static final Logger LOGGER = LoggerFactory.getLogger(StageOperationsControllerV2.class);
    private final static String JOB_NAMES_PROPERTY = "jobs";
    private final StageService stageService;
    private final ScheduleService scheduleService;
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final PipelineService pipelineService;

    @Autowired
    public StageOperationsControllerV2(ScheduleService scheduleService, StageService stageService, ApiAuthenticationHelper apiAuthenticationHelper,
                                       PipelineService pipelineService) {
        super(ApiVersion.v2);
        this.stageService = stageService;
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
            before(Routes.Stage.TRIGGER_FAILED_JOBS_PATH, mimeType, apiAuthenticationHelper::checkPipelineGroupOperateOfPipelineOrGroupInURLUserAnd403);
            before(Routes.Stage.TRIGGER_SELECTED_JOBS_PATH, mimeType, apiAuthenticationHelper::checkPipelineGroupOperateOfPipelineOrGroupInURLUserAnd403);
            before(Routes.Stage.CANCEL_STAGE_PATH, mimeType, apiAuthenticationHelper::checkPipelineGroupOperateOfPipelineOrGroupInURLUserAnd403);
            before(Routes.Stage.INSTANCE_BY_COUNTER, mimeType, apiAuthenticationHelper::checkPipelineViewPermissionsAnd403);
            before(Routes.Stage.STAGE_HISTORY, mimeType, apiAuthenticationHelper::checkPipelineViewPermissionsAnd403);

            post(Routes.Stage.TRIGGER_STAGE_PATH, mimeType, this::triggerStage);
            post(Routes.Stage.TRIGGER_FAILED_JOBS_PATH, mimeType, this::rerunFailedJobs);
            post(Routes.Stage.TRIGGER_SELECTED_JOBS_PATH, mimeType, this::rerunSelectedJobs);
            post(Routes.Stage.CANCEL_STAGE_PATH, mimeType, this::cancelStage);
            get(Routes.Stage.INSTANCE_BY_COUNTER, mimeType, this::instanceByCounter);
            get(Routes.Stage.STAGE_HISTORY, mimeType, this::history);
        });
    }

    public String rerunSelectedJobs(Request req, Response res) throws IOException {
        HttpOperationResult result = new HttpOperationResult();
        haltIfRequestBodyDoesNotContainPropertyJobs(req);

        JsonReader requestBody = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        List<String> requestedJobs = requestBody.readStringArrayIfPresent(JOB_NAMES_PROPERTY).get();

        Optional<Stage> optionalStage = getStageFromRequestParam(req, result);
        optionalStage.ifPresent(stage -> {
            HealthStateType healthStateType = HealthStateType.general(HealthStateScope.forStage(stage.getIdentifier()
                    .getPipelineName(), stage.getName()));

            Set<String> jobsInStage = stage.getJobInstances()
                    .stream()
                    .map(JobInstance::getName)
                    .collect(Collectors.toSet());

            List<String> unknownJobs = requestedJobs.stream()
                    .filter(jobToRun -> !jobsInStage.contains(jobToRun))
                    .collect(Collectors.toList());

            if (unknownJobs.isEmpty()) {
                scheduleService.rerunJobs(stage, requestedJobs, result);
            } else {
                String msg = String.format("Job(s) %s does not exist in stage '%s'.", unknownJobs, stage.getIdentifier().getStageLocator());
                result.notFound(msg, "", healthStateType);
            }

        });

        return renderHTTPOperationResult(result, req, res);
    }

    public String rerunFailedJobs(Request req, Response res) throws IOException {
        HttpOperationResult result = new HttpOperationResult();

        Optional<Stage> optionalStage = getStageFromRequestParam(req, result);
        optionalStage.ifPresent(stage -> scheduleService.rerunFailedJobs(stage, result));
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

    public String cancelStage(Request req, Response res) throws IOException, Exception {
        HttpOperationResult result = new HttpOperationResult();
        Optional<Stage> optionalStage = getStageFromRequestParam(req, result);
        if (!result.isSuccess()) {
            return renderHTTPOperationResult(result, req, res);
        }

        HttpLocalizedOperationResult localizedOperationResult = new HttpLocalizedOperationResult();
        scheduleService.cancelAndTriggerRelevantStages(optionalStage.get().getId(), currentUsername(), localizedOperationResult);
        return renderHTTPOperationResult(localizedOperationResult, req, res);
    }

    public String instanceByCounter(Request req, Response res) throws IOException {
        String pipelineName = req.params("pipeline_name");
        String pipelineCounter = req.params("pipeline_counter");
        String stageName = req.params("stage_name");
        String stageCounter = req.params("stage_counter");
        HttpOperationResult result = new HttpOperationResult();

        Stage stageModel = stageService.findStageWithIdentifier(pipelineName,
                Integer.parseInt(pipelineCounter),
                stageName,
                stageCounter,
                currentUsername().getUsername().toString(),
                result);

        if (result.canContinue()) {
            return writerForTopLevelObject(req, res, writer -> StageRepresenter.toJSON(writer, stageModel));
        } else {
            return renderHTTPOperationResult(result, req, res);
        }
    }

    public String history(Request request, Response response) throws IOException {
        String pipelineName = request.params("pipeline_name");
        String stageName = request.params("stage_name");
        int offsetFromRequest = getOffset(request);
        int pageSize = getPageSize(request);
        int stageInstanceCount = stageService.getCount(pipelineName, stageName);
        HttpOperationResult result = new HttpOperationResult();

        Pagination pagination = Pagination.pageStartingAt(offsetFromRequest, stageInstanceCount, pageSize);
        StageInstanceModels stageInstanceModels = stageService.findDetailedStageHistoryByOffset(pipelineName, stageName, pagination, currentUsername().getUsername().toString(), result);
        if (result.canContinue()) {
            return writerForTopLevelObject(request, response, writer -> StageInstancesRepresenter.toJSON(writer, stageInstanceModels, pagination));
        } else {
            return renderHTTPOperationResult(result, request, response);
        }
    }

    private Optional<Stage> getStageFromRequestParam(Request request, HttpOperationResult operationResult) {
        String pipelineName = request.params("pipeline_name");
        String pipelineCounter = request.params("pipeline_counter");
        String stageName = request.params("stage_name");
        String stageCounter = request.params("stage_counter");

        Stage stage = stageService.findStageWithIdentifier(pipelineName,
                Integer.parseInt(pipelineCounter),
                stageName,
                stageCounter,
                currentUsername().getUsername().toString(),
                operationResult);

        if (!operationResult.isSuccess()) {
            return Optional.empty();
        }

        if (stage == null || stage instanceof NullStage) {
            String message = String.format("Stage '%s' with counter '%s' not found. Please make sure specified stage or stage run with specified counter exists.", stageName, stageCounter);
            operationResult.notFound("Not Found", message, HealthStateType.general(HealthStateScope.GLOBAL));
            return Optional.empty();
        }

        return Optional.ofNullable(stage);
    }

    private void haltIfRequestBodyDoesNotContainPropertyJobs(Request req) {
        JsonReader requestBody = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        if (!requestBody.hasJsonObject(JOB_NAMES_PROPERTY)) {
            throw HaltApiResponses.haltBecauseOfReason("Could not read property '%s' in request body", JOB_NAMES_PROPERTY);
        }
        requestBody.readStringArrayIfPresent(JOB_NAMES_PROPERTY);
    }

    private Integer getPageSize(Request request) {
        Integer offset;
        try {
            offset = Integer.valueOf(request.queryParamOrDefault("page_size", "10"));
            if (offset < 10 || offset > 100) {
                throw new BadRequestException(BAD_PAGE_SIZE_MSG);
            }
        } catch (NumberFormatException e) {
            throw new BadRequestException(BAD_PAGE_SIZE_MSG);
        }
        return offset;
    }

    private Integer getOffset(Request request) {
        Integer offset;
        try {
            offset = Integer.valueOf(request.queryParamOrDefault("offset", "0"));
            if (offset < 0) {
                throw new BadRequestException(BAD_OFFSET_MSG);
            }
        } catch (NumberFormatException e) {
            throw new BadRequestException(BAD_OFFSET_MSG);
        }
        return offset;
    }
}
