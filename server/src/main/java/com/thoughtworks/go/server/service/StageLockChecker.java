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

import com.thoughtworks.go.domain.PipelineIdentifier;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;

/**
 * @understands if a Stage can be scheduled in a locked pipeline
 */
public class StageLockChecker implements SchedulingChecker {
    private final PipelineIdentifier pipeline;
    private final PipelineLockService lockService;

    public StageLockChecker(PipelineIdentifier pipeline, PipelineLockService pipelineLockService) {
        this.pipeline = pipeline;
        lockService = pipelineLockService;
    }

    @Override
    public void check(OperationResult result) {
        if (!lockService.canScheduleStageInPipeline(pipeline)) {
            result.notAcceptable(String.format("Pipeline %s is locked.", pipeline.getName()), HealthStateType.general(HealthStateScope.GLOBAL));
        }
    }
}
