/*
 * Copyright 2022 Thoughtworks, Inc.
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

package com.thoughtworks.go.spark.spa;

import com.google.common.collect.ImmutableMap;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.service.JobInstanceService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.SparkController;
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.TemplateEngine;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.String.format;
import static spark.Spark.*;

public class BuildDetailController implements SparkController {
    private final SPAAuthenticationHelper authenticationHelper;
    private final TemplateEngine engine;
    private JobInstanceService jobInstanceService;
    private SecurityService securityService;
    private PipelineDao pipelineDao;

    public BuildDetailController(SPAAuthenticationHelper authenticationHelper, JobInstanceService jobInstanceService, PipelineDao pipelineDao, SecurityService securityService, TemplateEngine engine) {
        this.authenticationHelper = authenticationHelper;
        this.jobInstanceService = jobInstanceService;
        this.pipelineDao = pipelineDao;
        this.securityService = securityService;
        this.engine = engine;
    }

    @Override
    public String controllerBasePath() {
        return Routes.BuildDetailSPA.SPA_BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before(Routes.BuildDetailSPA.JOB_INSTANCE, authenticationHelper::checkPipelineViewPermissionsAnd403);
            get(Routes.BuildDetailSPA.JOB_INSTANCE, this::index, engine);
        });
    }

    public ModelAndView index(Request request, Response response) {
        String pipelineName = request.params("pipeline_name");
        String stageName = request.params("stage_name");
        String jobName = request.params("job_name");
        Integer pipelineCounter = getValue(request, "pipeline_counter");
        Integer stageCounter = getValue(request, "stage_counter");

        JobInstance instance = jobInstanceService.findJobInstanceWithTransitions(pipelineName, stageName, jobName, pipelineCounter, stageCounter, currentUsername());
        Pipeline pipeline = pipelineDao.pipelineWithMaterialsAndModsByBuildId(instance.getId());

        ImmutableMap<String, Object> meta = new ImmutableMap.Builder<String, Object>()
                .put("jobIdentifier", new ImmutableMap.Builder<String, Object>()
                        .put("pipelineName", instance.getPipelineName())
                        .put("pipelineCounter", instance.getPipelineCounter())
                        .put("stageName", instance.getStageName())
                        .put("stageCounter", instance.getStageCounter())
                        .put("jobName", instance.getName())
                        .build()
                )
                .put("buildCause", pipeline.getBuildCauseMessage())
                .put("canOperatePipeline", securityService.hasOperatePermissionForPipeline(currentUserLoginName(), pipelineName))
                .put("canAdministerPipeline", securityService.hasAdminPermissionsForPipeline(currentUsername(), new CaseInsensitiveString(pipelineName)))
                .build();

        Map<String, Object> object = new LinkedHashMap<>() {{
            put("viewTitle", request.params(":job_name") + " Job Details");
            put("meta", meta);
        }};

        return new ModelAndView(object, null);
    }

    private Integer getValue(Request request, String paramKey) {
        String errorMsg = format("The parameter '%s' must be a number greater than 0.", paramKey);
        try {
            Integer value = Integer.valueOf(request.params(paramKey));
            if (value < 0) {
                throw new BadRequestException(errorMsg);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new BadRequestException(errorMsg);
        }
    }


}

