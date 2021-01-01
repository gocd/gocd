/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.service;

import static java.lang.String.format;

import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.HealthStateScope;

/**
 * @understands whether a pipeline is locked
 */
public class PipelineLockChecker implements SchedulingChecker {
    private final String pipelineName;
    private final PipelineLockService pipelineLockService;

    public PipelineLockChecker(String pipelineName, PipelineLockService pipelineLockService) {
        this.pipelineName = pipelineName;
        this.pipelineLockService = pipelineLockService;
    }

    @Override
    public void check(OperationResult result) {
        HealthStateType healthStateType = HealthStateType.general(HealthStateScope.forPipeline(pipelineName));
        if (pipelineLockService.isLocked(pipelineName)) {
            String message = format("Pipeline %s cannot be scheduled", pipelineName);
            String description = format("Pipeline %s is locked as another instance of this pipeline is running.", pipelineName);
            result.conflict(message, description, healthStateType);
        }
        else{
            result.success(healthStateType);
        }

    }
}
