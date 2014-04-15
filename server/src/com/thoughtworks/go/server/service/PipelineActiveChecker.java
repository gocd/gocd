/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.domain.PipelineIdentifier;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;

public class PipelineActiveChecker implements SchedulingChecker {
    private CurrentActivityService activityService;
    private PipelineIdentifier pipelineIdentifier;

    public PipelineActiveChecker(CurrentActivityService activityService, PipelineIdentifier pipelineIdentifier) {
        this.activityService = activityService;
        this.pipelineIdentifier = pipelineIdentifier;
    }

    public void check(OperationResult result) {
        HealthStateType id = HealthStateType.general(HealthStateScope.forPipeline(pipelineIdentifier.getName()));
        if (activityService.isAnyStageActive(pipelineIdentifier)) {
            String description = String.format("Pipeline[name='%s', counter='%s', label='%s'] is still in progress",
                    pipelineIdentifier.getName(), pipelineIdentifier.getCounter(), pipelineIdentifier.getLabel());
            String message = String.format("Failed to trigger pipeline [%s]", pipelineIdentifier.getName());
            result.error(message, description, id);
        } else {
            result.success(id);
        }
    }
}
