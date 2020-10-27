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
package com.thoughtworks.go.apiv3.stageinstance;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.api.util.HaltApiResponses;
import com.thoughtworks.go.apiv3.stageinstance.representers.StageInstancesRepresenter;
import com.thoughtworks.go.apiv3.stageinstance.representers.StageRepresenter;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.NullStage;
import com.thoughtworks.go.domain.PipelineRunIdInfo;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels;
import com.thoughtworks.go.server.service.ScheduleService;
import com.thoughtworks.go.server.service.StageService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static spark.Spark.*;

@Component
public class StageInstanceControllerV3 extends ApiController implements SparkSpringController {
    private final static String JOB_NAMES_PROPERTY = "jobs";
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final StageService stageService;
    private final ScheduleService scheduleService;

    @Autowired
    public StageInstanceControllerV3(ApiAuthenticationHelper apiAuthenticationHelper, StageService stageService, ScheduleService scheduleService) {
        super(ApiVersion.v3);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.stageService = stageService;
        this.scheduleService = scheduleService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.Stage.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("/*", mimeType, this::setContentType);
            before("/*", mimeType, this::verifyContentType);

            before(Routes.Stage.TRIGGER_FAILED_JOBS_PATH, mimeType, apiAuthenticationHelper::checkPipelineGroupOperateOfPipelineOrGroupInURLUserAnd403);
            before(Routes.Stage.TRIGGER_SELECTED_JOBS_PATH, mimeType, apiAuthenticationHelper::checkPipelineGroupOperateOfPipelineOrGroupInURLUserAnd403);
            before(Routes.Stage.CANCEL_STAGE_PATH, mimeType, apiAuthenticationHelper::checkPipelineGroupOperateOfPipelineOrGroupInURLUserAnd403);
            before(Routes.Stage.INSTANCE_V2, mimeType, apiAuthenticationHelper::checkPipelineViewPermissionsAnd403);
            before(Routes.Stage.STAGE_HISTORY, mimeType, apiAuthenticationHelper::checkPipelineViewPermissionsAnd403);

            post(Routes.Stage.TRIGGER_FAILED_JOBS_PATH, mimeType, this::rerunFailedJobs);
            post(Routes.Stage.TRIGGER_SELECTED_JOBS_PATH, mimeType, this::rerunSelectedJobs);
            post(Routes.Stage.CANCEL_STAGE_PATH, mimeType, this::cancelStage);
            get(Routes.Stage.INSTANCE_V2, mimeType, this::instanceByCounter);
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

    public String cancelStage(Request req, Response res) throws Exception {
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
        Long after = getCursor(request, "after");
        Long before = getCursor(request, "before");
        int pageSize = getPageSize(request);

        StageInstanceModels stageInstanceModels = stageService.findStageHistoryViaCursor(currentUsername(), pipelineName, stageName, after, before, pageSize);
        PipelineRunIdInfo latestAndOldestPipelineIds = stageService.getOldestAndLatestStageInstanceId(currentUsername(), pipelineName, stageName);
        return writerForTopLevelObject(request, response, writer -> StageInstancesRepresenter.toJSON(writer, stageInstanceModels, latestAndOldestPipelineIds));
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
}
