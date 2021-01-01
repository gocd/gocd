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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.server.scheduling.TriggerMonitor;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.HealthStateScope;

/**
 * ADD_UNDERSTANDS_BLOCK
 */
public class AboutToBeTriggeredChecker implements SchedulingChecker{
    private final CaseInsensitiveString pipelineName;
    private final TriggerMonitor triggerMonitor;
    private final PipelineScheduleQueue pipelineScheduleQueue;

    public AboutToBeTriggeredChecker(CaseInsensitiveString pipelineName, TriggerMonitor triggerMonitor, PipelineScheduleQueue pipelineScheduleQueue) {
        this.pipelineName = pipelineName;
        this.triggerMonitor = triggerMonitor;
        this.pipelineScheduleQueue = pipelineScheduleQueue;
    }

    @Override
    public void check(OperationResult result) {
        String pipelineNameString = CaseInsensitiveString.str(pipelineName);
        HealthStateType type = HealthStateType.general(HealthStateScope.forPipeline(pipelineNameString));
        if (triggerMonitor.isAlreadyTriggered(pipelineName) || pipelineScheduleQueue.hasForcedBuildCause(pipelineName)) {
            result.conflict("Failed to trigger pipeline: " + pipelineName,
                            "Pipeline already triggered",
                            HealthStateType.general(HealthStateScope.forPipeline(pipelineNameString)));
        } else {
            result.success(type);
        }

    }
}
