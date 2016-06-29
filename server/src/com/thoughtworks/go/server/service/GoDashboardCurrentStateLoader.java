/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.PipelineGroupVisitor;
import com.thoughtworks.go.domain.PipelinePauseInfo;
import com.thoughtworks.go.domain.PiplineConfigVisitor;
import com.thoughtworks.go.presentation.pipelinehistory.*;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.scheduling.TriggerMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.config.CaseInsensitiveString.str;
import static com.thoughtworks.go.domain.buildcause.BuildCause.createWithEmptyModifications;
import static com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel.createEmptyPipelineInstanceModel;
import static com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel.createPreparingToSchedule;
import static com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels.createPipelineInstanceModels;

/* Understands the current state of a pipeline, which is to be shown on the dashboard. */
@Service
public class GoDashboardCurrentStateLoader {
    private PipelineDao pipelineDao;
    private TriggerMonitor triggerMonitor;
    private PipelinePauseService pipelinePauseService;

    @Autowired
    public GoDashboardCurrentStateLoader(PipelineDao pipelineDao, TriggerMonitor triggerMonitor, PipelinePauseService pipelinePauseService) {
        this.pipelineDao = pipelineDao;
        this.triggerMonitor = triggerMonitor;
        this.pipelinePauseService = pipelinePauseService;
    }

    public List<PipelineGroupModel> allPipelines(CruiseConfig config) {
        final PipelineInstanceModels activeInstances = pipelineDao.loadActivePipelines();
        final List<PipelineGroupModel> groupModels = new ArrayList<>();

        config.accept(new PipelineGroupVisitor() {
            @Override
            public void visit(final PipelineConfigs group) {
                final PipelineGroupModel groupModel = new PipelineGroupModel(group.getGroup());
                groupModels.add(groupModel);

                group.accept(new PiplineConfigVisitor() {
                    @Override
                    public void visit(PipelineConfig pipelineConfig) {
                        groupModel.add(pipelineModelFor(pipelineConfig, activeInstances));
                    }
                });
            }
        });

        return groupModels;
    }

    private PipelineModel pipelineModelFor(PipelineConfig pipelineConfig, PipelineInstanceModels activeInstances) {
        String pipelineName = str(pipelineConfig.name());

        PipelinePauseInfo pauseInfo = pipelinePauseService.pipelinePauseInfo(pipelineName);
        PipelineModel pipelineModel = new PipelineModel(pipelineName, true, true, pauseInfo);
        pipelineModel.addPipelineInstances(instancesFor(pipelineConfig, activeInstances));

        return pipelineModel;
    }

    private PipelineInstanceModels instancesFor(PipelineConfig pipelineConfig, PipelineInstanceModels activeInstances) {
        PipelineInstanceModels pims = findPIMsWithFallbacks(pipelineConfig, activeInstances);

        for (PipelineInstanceModel instanceModel : pims) {
            populateStagesWhichHaventRunFromConfig(instanceModel, pipelineConfig);
        }

        return pims;
    }

    private PipelineInstanceModels findPIMsWithFallbacks(PipelineConfig pipelineConfig, PipelineInstanceModels activeInstances) {
        String pipelineName = str(pipelineConfig.name());

        PipelineInstanceModels activeInstancesForPipeline = activeInstances.findAll(pipelineName);
        if (!activeInstancesForPipeline.isEmpty()) {
            return activeInstancesForPipeline;
        }

        if (triggerMonitor.isAlreadyTriggered(pipelineName)) {
            return createPipelineInstanceModels(createPreparingToSchedule(pipelineName, new StageInstanceModels()));
        }

        PipelineInstanceModels modelsFromHistory = pipelineDao.loadHistory(pipelineName, 1, 0);
        if (!modelsFromHistory.isEmpty()) {
            return modelsFromHistory;
        }

        return createPipelineInstanceModels(createEmptyPipelineInstanceModel(pipelineName, createWithEmptyModifications(), new StageInstanceModels()));
    }

    /* TODO Same as: com.thoughtworks.go.server.service.PipelineHistoryService.appendFollowingStagesFromConfig */
    private PipelineInstanceModel populateStagesWhichHaventRunFromConfig(PipelineInstanceModel instanceModel, PipelineConfig pipelineConfig) {
        StageInstanceModels stageHistory = instanceModel.getStageHistory();
        StageInstanceModel lastStage = stageHistory.last();

        StageConfig nextStage = lastStage == null ? pipelineConfig.getFirstStageConfig() : pipelineConfig.nextStage(new CaseInsensitiveString(lastStage.getName()));
        while (nextStage != null && !stageHistory.hasStage(str(nextStage.name()))) {
            stageHistory.addFutureStage(str(nextStage.name()), !nextStage.requiresApproval());
            nextStage = pipelineConfig.nextStage(nextStage.name());
        }

        return instanceModel;
    }
}
