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
package com.thoughtworks.go.apiv1.agentjobhistory;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.agentjobhistory.representers.AgentJobHistoryRepresenter;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.JobInstanceService;
import com.thoughtworks.go.server.ui.JobInstancesModel;
import com.thoughtworks.go.server.ui.SortOrder;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import static spark.Spark.*;

@Component
public class AgentJobHistoryControllerV1 extends ApiController implements SparkSpringController {

    static final String BAD_PAGE_SIZE_MSG = "The query parameter `page_size`, if specified must be a number between 10 and 100.";
    static final String BAD_OFFSET_MSG = "The query parameter `offset`, if specified must be a number greater or equal to 0.";
    static final String BAD_SORT_COLUMN_MSG = "The query parameter `column` must be one of " + Arrays.stream(JobInstanceService.JobHistoryColumns.values()).map(Enum::name).collect(Collectors.joining(", "));
    static final String BAD_SORT_ORDER_MSG = "The query parameter `order` must be one of " + Arrays.stream(SortOrder.values()).map(Enum::name).collect(Collectors.joining(", "));

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final JobInstanceService jobInstanceService;
    private final AgentService agentService;

    @Autowired
    public AgentJobHistoryControllerV1(ApiAuthenticationHelper apiAuthenticationHelper,
                                       JobInstanceService jobInstanceService,
                                       AgentService agentService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.jobInstanceService = jobInstanceService;
        this.agentService = agentService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.AgentJobHistory.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);
            before("/*", mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);

            get("", mimeType, this::index);
        });
    }

    public String index(Request request, Response response) throws IOException {

        String uuid = request.params(":uuid");

        Integer offset = getOffset(request);
        Integer pageSize = getPageSize(request);
        JobInstanceService.JobHistoryColumns column = getSortColumn(request);
        SortOrder sortOrder = getSortOrder(request);

        Integer total = jobInstanceService.totalCompletedJobsCountOn(uuid);
        Pagination pagination = Pagination.pageStartingAt(offset, total, pageSize);

        AgentInstance agent = agentService.findAgent(uuid);
        if (agent.isNullAgent()) {
            throw new RecordNotFoundException(EntityType.Agent, uuid);
        }
        JobInstancesModel jobInstances = jobInstanceService.completedJobsOnAgent(uuid, column, sortOrder, pagination);
        return writerForTopLevelObject(request, response, outputWriter -> AgentJobHistoryRepresenter.toJSON(outputWriter, uuid, jobInstances));
    }

    private SortOrder getSortOrder(Request request) {
        try {
            return SortOrder.valueOf(request.queryParamOrDefault("sort_order", "DESC"));
        } catch (Exception e) {
            throw new BadRequestException(BAD_SORT_ORDER_MSG);
        }
    }

    private JobInstanceService.JobHistoryColumns getSortColumn(Request request) {
        try {
            return JobInstanceService.JobHistoryColumns.valueOf(request.queryParamOrDefault("sort_column", "completed"));
        } catch (Exception e) {
            throw new BadRequestException(BAD_SORT_COLUMN_MSG);
        }
    }

    protected Integer getPageSize(Request request) {
        Integer offset;
        try {
            offset = Integer.valueOf(request.queryParamOrDefault("page_size", "50"));
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
