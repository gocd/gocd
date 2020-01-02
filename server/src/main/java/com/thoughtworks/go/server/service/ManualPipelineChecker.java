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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;

public class ManualPipelineChecker implements SchedulingChecker {
    private PipelineConfig pipelineConfig;

    public ManualPipelineChecker(PipelineConfig pipelineConfig) {
        this.pipelineConfig = pipelineConfig;
    }

    @Override
    public void check(OperationResult result) {
        HealthStateType type = HealthStateType.general(HealthStateScope.forPipeline(CaseInsensitiveString.str(pipelineConfig.name())));
        if (pipelineConfig.isFirstStageManualApproval()) {
            String message = String.format("Failed to trigger pipeline [%s]", pipelineConfig.name());
            result.error(message, String.format("The first stage of pipeline \"%s\" has manual approval",
                                pipelineConfig.name()), type);
        } else {
            result.success(type);
        }
    }
}
