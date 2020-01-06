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
package com.thoughtworks.go.apiv1.jobinstance;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.jobinstance.representers.JobInstanceRepresenter;
import com.thoughtworks.go.apiv1.jobinstance.representers.JobInstancesRepresenter;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobInstances;
import com.thoughtworks.go.domain.PipelineRunIdInfo;
import com.thoughtworks.go.server.service.JobInstanceService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static com.thoughtworks.go.server.service.ServiceConstants.History.BAD_CURSOR_MSG;
import static com.thoughtworks.go.server.service.ServiceConstants.History.BAD_PAGE_SIZE_MSG;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static spark.Spark.*;

@Component
public class JobInstanceControllerV1 extends ApiController implements SparkSpringController {
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final JobInstanceService jobInstanceService;

    @Autowired
    public JobInstanceControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, JobInstanceService jobInstanceService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.jobInstanceService = jobInstanceService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.Job.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("/*", mimeType, this::setContentType);
            before("/*", mimeType, this::verifyContentType);

            before(Routes.Job.JOB_HISTORY, mimeType, this.apiAuthenticationHelper::checkPipelineViewPermissionsAnd403);
            before(Routes.Job.JOB_INSTANCE, mimeType, this.apiAuthenticationHelper::checkPipelineViewPermissionsAnd403);

            get(Routes.Job.JOB_HISTORY, mimeType, this::getHistoryInfo);
            get(Routes.Job.JOB_INSTANCE, mimeType, this::getInstanceInfo);
        });
    }

    String getHistoryInfo(Request request, Response response) throws IOException {
        String pipelineName = request.params("pipeline_name");
        String stageName = request.params("stage_name");
        String jobName = request.params("job_name");
        Long after = getCursor(request, "after");
        Long before = getCursor(request, "before");
        Integer pageSize = getPageSize(request);

        JobInstances jobInstances = jobInstanceService.getJobHistoryViaCursor(currentUsername(), pipelineName, stageName, jobName, after, before, pageSize);
        PipelineRunIdInfo runIdInfo = jobInstanceService.getOldestAndLatestJobInstanceId(currentUsername(), pipelineName, stageName, jobName);

        return writerForTopLevelObject(request, response, writer -> JobInstancesRepresenter.toJSON(writer, jobInstances, runIdInfo));
    }

    String getInstanceInfo(Request request, Response response) throws IOException {
        String pipelineName = request.params("pipeline_name");
        String stageName = request.params("stage_name");
        String jobName = request.params("job_name");
        Integer pipelineCounter = getValue(request, "pipeline_counter");
        Integer stageCounter = getValue(request, "stage_counter");
        JobInstance jobInstance = jobInstanceService.findJobInstance(pipelineName, stageName, jobName, pipelineCounter, stageCounter, currentUsername());
        if (jobInstance.isNull()) {
            throw new RecordNotFoundException(format("No job instance was found for '%s/%s/%s/%s/%s'.", pipelineName, pipelineCounter, stageName, stageCounter, jobName));
        }
        return writerForTopLevelObject(request, response, writer -> JobInstanceRepresenter.toJSON(writer, jobInstance));
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

    private Integer getValue(Request request, String paramKey) {
        Integer value;
        String errorMsg = format("The params '%s' must be a number greater than 0.", paramKey);
        try {
            value = Integer.valueOf(request.params(paramKey));
            if (value < 0) {
                throw new BadRequestException(errorMsg);
            }
        } catch (NumberFormatException e) {
            throw new BadRequestException(errorMsg);
        }
        return value;
    }

    private long getCursor(Request request, String key) {
        long cursor = 0;
        try {
            String value = request.queryParams(key);
            if (isBlank(value)) {
                return cursor;
            }
            cursor = Long.parseLong(value);
        } catch (NumberFormatException nfe) {
            throw new BadRequestException(String.format(BAD_CURSOR_MSG, key));
        }
        return cursor;
    }
}
