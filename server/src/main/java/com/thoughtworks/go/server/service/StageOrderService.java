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
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.domain.PipelineInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StageOrderService {
    private final GoConfigService goConfigService;

    @Autowired
    public StageOrderService(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }

    public StageConfig getNextStage(PipelineInfo pipeline, String stageName) {
        StageConfig nextStageFromHistory = nextStageFromHistory(pipeline, stageName);
        return nextStageFromHistory == null ? nextStageFromConfig(pipeline, stageName) : nextStageFromHistory;
    }

    private StageConfig nextStageFromHistory(PipelineInfo pipeline, String stageName) {
        String nextStageName = pipeline.nextStageName(stageName);
        return nextStageName == null ? null : goConfigService.stageConfigNamed(pipeline.getName(), nextStageName);
    }

    private StageConfig nextStageFromConfig(PipelineInfo pipeline, String stageName) {
        if (goConfigService.hasNextStage(pipeline.getName(), stageName)) {
            StageConfig nextStageConfig = goConfigService.nextStage(pipeline.getName(), stageName);
            return stageHasBeenRun(pipeline, nextStageConfig) ? null : nextStageConfig;
        }
        return null;
    }

    private boolean stageHasBeenRun(PipelineInfo currentPipeline, StageConfig nextStageConfig) {
        return currentPipeline.hasStageBeenRun(CaseInsensitiveString.str(nextStageConfig.name()));
    }
}
