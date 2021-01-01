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
package com.thoughtworks.go.server.service.builders;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.EnvironmentVariableConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.server.domain.MaterialForScheduling;
import com.thoughtworks.go.server.domain.PipelineScheduleOptions;
import com.thoughtworks.go.server.scheduling.ScheduleOptions;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;


public class ScheduleOptionsBuilder {
    private final GoConfigService goConfigService;
    private final List<Builder> builders;

    public ScheduleOptionsBuilder(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
        builders = Arrays.asList(new MDUOptionBuilder(), new MaterialsBuilder(goConfigService), new EnvironmentVariableBuilder(goConfigService), new SecureEnvironmentVariableBuilder(goConfigService));
    }

    public ScheduleOptions build(HttpOperationResult result, String pipelineName, PipelineScheduleOptions pipelineScheduleOptions) {
        ScheduleOptions scheduleOptions = new ScheduleOptions();
        HealthStateType healthStateType = HealthStateType.general(HealthStateScope.forPipeline(pipelineName));
        if (!goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))) {
            result.notFound(String.format("Pipeline '%s' not found.", pipelineName), "", healthStateType);
            return null;
        }

        for (Builder builder : builders) {
            if (result.canContinue())
                builder.build(scheduleOptions, result, pipelineName, pipelineScheduleOptions, healthStateType);
        }
        return scheduleOptions;
    }

    private interface Builder {
        void build(ScheduleOptions scheduleOptions, HttpOperationResult result, String pipelineName, PipelineScheduleOptions pipelineScheduleOptions, HealthStateType healthStateType);
    }

    private class MaterialsBuilder implements Builder {
        private GoConfigService goConfigService;

        public MaterialsBuilder(GoConfigService goConfigService) {
            this.goConfigService = goConfigService;
        }

        @Override
        public void build(ScheduleOptions scheduleOptions, HttpOperationResult result, String pipelineName, PipelineScheduleOptions pipelineScheduleOptions, HealthStateType healthStateType) {
            for (MaterialForScheduling materialForScheduling : pipelineScheduleOptions.getMaterials()) {
                try {
                    MaterialConfig material = goConfigService.materialForPipelineWithFingerprint(pipelineName, materialForScheduling.getFingerprint());
                    if (StringUtils.isBlank(materialForScheduling.getRevision())) {
                        result.unprocessibleEntity("Request to schedule pipeline rejected", String.format("Material [%s] has empty revision", materialForScheduling.getFingerprint()), HealthStateType.general(HealthStateScope.GLOBAL));
                        return;
                    }
                    scheduleOptions.getSpecifiedRevisions().put(material.getPipelineUniqueFingerprint(), materialForScheduling.getRevision());
                } catch (Exception e) {
                    result.unprocessibleEntity("Request to schedule pipeline rejected", String.format("Pipeline '%s' does not contain the following material(s): [%s].", pipelineName, materialForScheduling.getFingerprint()), healthStateType);
                    return;
                }
            }
        }
    }

    private class MDUOptionBuilder implements Builder {

        public MDUOptionBuilder() {
        }

        @Override
        public void build(ScheduleOptions scheduleOptions, HttpOperationResult result, String pipelineName, PipelineScheduleOptions pipelineScheduleOptions, HealthStateType healthStateType) {
            scheduleOptions.shouldPerformMDUBeforeScheduling(pipelineScheduleOptions.shouldPerformMDUBeforeScheduling());
        }
    }

    private class EnvironmentVariableBuilder implements Builder {
        private GoConfigService goConfigService;

        public EnvironmentVariableBuilder(GoConfigService goConfigService) {
            this.goConfigService = goConfigService;
        }

        @Override
        public void build(ScheduleOptions scheduleOptions, HttpOperationResult result, String pipelineName, PipelineScheduleOptions pipelineScheduleOptions, HealthStateType healthStateType) {
            for (EnvironmentVariableConfig environmentVariable : pipelineScheduleOptions.getPlainTextEnvironmentVariables()) {
                if (!goConfigService.hasVariableInScope(pipelineName, environmentVariable.getName())) {
                    String variableUnconfiguredMessage = String.format("Variable '%s' has not been configured for pipeline '%s'", environmentVariable.getName(), pipelineName);
                    result.unprocessibleEntity("Request to schedule pipeline rejected", variableUnconfiguredMessage, healthStateType);
                    return;
                }
                scheduleOptions.getVariables().add(environmentVariable);
            }
        }
    }

    private class SecureEnvironmentVariableBuilder implements Builder {
        private GoConfigService goConfigService;

        public SecureEnvironmentVariableBuilder(GoConfigService goConfigService) {
            this.goConfigService = goConfigService;
        }

        @Override
        public void build(ScheduleOptions scheduleOptions, HttpOperationResult result, String pipelineName, PipelineScheduleOptions pipelineScheduleOptions, HealthStateType healthStateType) {
            for (EnvironmentVariableConfig environmentVariable : pipelineScheduleOptions.getSecureEnvironmentVariables()) {
                if (!goConfigService.hasVariableInScope(pipelineName, environmentVariable.getName())) {
                    String variableUnconfiguredMessage = String.format("Variable '%s' has not been configured for pipeline '%s'", environmentVariable.getName(), pipelineName);
                    result.unprocessibleEntity("Request to schedule pipeline rejected", variableUnconfiguredMessage, healthStateType);
                    return;
                }
                environmentVariable.validate(null);
                if (!environmentVariable.errors().isEmpty()) {
                    result.unprocessibleEntity("Request to schedule pipeline rejected", environmentVariable.errors().asString(), healthStateType);
                    return;
                }
                scheduleOptions.getVariables().add(environmentVariable);
            }
        }
    }

}

