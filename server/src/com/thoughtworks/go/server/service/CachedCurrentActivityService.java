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

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.config.TrackingTool;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.PipelineIdentifier;
import com.thoughtworks.go.domain.PipelinePauseInfo;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.dto.DurationBeans;
import com.thoughtworks.go.server.presentation.models.PipelineJsonPresentationModel;
import com.thoughtworks.go.server.presentation.models.StageJsonPresentationModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CachedCurrentActivityService implements CurrentActivityService {

    private GoConfigService goConfigService;
    private StageService stageService;
    private PipelineScheduleQueue pipelineScheduleQueue;
    private PipelineService pipelineService;
    private PipelinePauseService pipelinePauseService;

    @Autowired
    public CachedCurrentActivityService(GoConfigService goConfigService,
                                        StageService stageService,
                                        PipelineScheduleQueue pipelineScheduleQueue,
                                        PipelineService pipelineService, PipelinePauseService pipelinePauseService) {
        this.goConfigService = goConfigService;
        this.stageService = stageService;
        this.pipelineScheduleQueue = pipelineScheduleQueue;
        this.pipelineService = pipelineService;
        this.pipelinePauseService = pipelinePauseService;
    }

    public boolean isStageActive(String pipelineName, String stageName) {
        return stageService.isStageActive(pipelineName, stageName);
    }

    public boolean isAnyStageActive(PipelineIdentifier pipelineIdentifier) {
        return stageService.isAnyStageActiveForPipeline(pipelineIdentifier.getName(), pipelineIdentifier.getCounter());
    }

    public PipelineJsonPresentationModel getPipelineStatus(String name) {
        return pipelineModel(goConfigService.getCurrentConfig().pipelineConfigByName(new CaseInsensitiveString(name)));
    }

    private PipelineJsonPresentationModel pipelineModel(PipelineConfig pipelineConfig) {
        String name = CaseInsensitiveString.str(pipelineConfig.name());
        PipelinePauseInfo pauseInfo = pipelinePauseService.pipelinePauseInfo(name);
        boolean forcedBuild = pipelineScheduleQueue.hasForcedBuildCause(name);
        List<StageJsonPresentationModel> stageModels = stagesModel(pipelineConfig);
        return new PipelineJsonPresentationModel(goConfigService.findGroupNameByPipeline(new CaseInsensitiveString(name)), name, pauseInfo, forcedBuild, stageModels);
    }

    private List<StageJsonPresentationModel> stagesModel(PipelineConfig pipelineConfig) {
        List<StageJsonPresentationModel> presenters = new ArrayList<>();
        for (StageConfig stageConfig : pipelineConfig) {
            Stage mostRecentStage = stageService.mostRecentStageWithBuilds(CaseInsensitiveString.str(pipelineConfig.name()), stageConfig);
            Pipeline pipeline = pipelineService.pipelineWithModsByStageId(CaseInsensitiveString.str(pipelineConfig.name()), mostRecentStage.getId());
            presenters.add(stageModel(pipeline, mostRecentStage));
        }
        return presenters;
    }

    private StageJsonPresentationModel stageModel(Pipeline currentPipeline, Stage stage) {
        StageIdentifier lastSuccessfulPipelineForStage = pipelineService.findLastSuccessfulStageIdentifier(currentPipeline.getName(), stage.getName());
        final DurationBeans durations = stageService.getBuildDurations(currentPipeline.getName(), stage);
        TrackingTool trackingTool = goConfigService.pipelineConfigNamed(new CaseInsensitiveString(currentPipeline.getName())).trackingTool();
        return new StageJsonPresentationModel(currentPipeline, stage, lastSuccessfulPipelineForStage, goConfigService.agents(), durations, trackingTool);
    }
}
