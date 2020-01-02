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

import com.thoughtworks.go.config.Approval;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.PipelineIdentifier;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.scheduling.TriggerMonitor;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Arrays.asList;

@Service
public class SchedulingCheckerService {
    private final GoConfigService goConfigService;
    private final StageService stageService;
    private final SecurityService securityService;
    private final PipelineLockService pipelineLockService;
    private final TriggerMonitor triggerMonitor;
    private final PipelineScheduleQueue pipelineScheduleQueue;
    private final PipelineService pipelineService;
    private final OutOfDiskSpaceChecker outOfDiskSpaceChecker;
    private PipelinePauseService pipelinePauseService;

    @Autowired
    public SchedulingCheckerService(GoConfigService goConfigService,
                                    StageService stageService,
                                    SecurityService securityService,
                                    PipelineLockService pipelineLockService,
                                    TriggerMonitor triggerMonitor, PipelineScheduleQueue pipelineScheduleQueue,
                                    PipelinePauseService pipelinePauseService, PipelineService pipelineService, OutOfDiskSpaceChecker outOfDiskSpaceChecker) {
        this.goConfigService = goConfigService;
        this.stageService = stageService;
        this.securityService = securityService;
        this.pipelineLockService = pipelineLockService;
        this.triggerMonitor = triggerMonitor;
        this.pipelineScheduleQueue = pipelineScheduleQueue;
        this.pipelinePauseService = pipelinePauseService;
        this.pipelineService = pipelineService;
        this.outOfDiskSpaceChecker = outOfDiskSpaceChecker;
    }

    public boolean canTriggerManualPipeline(String pipelineName, String username, OperationResult result) {
        PipelineConfig pipelineConfig = goConfigService.currentCruiseConfig().pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        return canTriggerManualPipeline(pipelineConfig, username, result);
    }

    public boolean canTriggerManualPipeline(PipelineConfig pipelineConfig, String username, OperationResult result) {
        CompositeChecker checker = buildScheduleCheckers(asList(manualTriggerCheckers(pipelineConfig, username), diskCheckers()));
        checker.check(result);
        return result.canContinue();
    }

    public void canTriggerPipelineWithTimer(String name, ServerHealthStateOperationResult result) {
        PipelineConfig pipelineConfig = goConfigService.getCurrentConfig().pipelineConfigByName(new CaseInsensitiveString(name));
        canTriggerPipelineWithTimer(pipelineConfig, result);
    }

    public void canTriggerPipelineWithTimer(PipelineConfig pipelineConfig, OperationResult operationResult) {
        CompositeChecker compositeChecker = buildScheduleCheckers(asList(timerTriggerCheckers(pipelineConfig)));
        compositeChecker.check(operationResult);
    }

    public boolean canSchedule(OperationResult result) {
        CompositeChecker checker = buildScheduleCheckers(asList(diskCheckers()));
        checker.check(result);
        return result.canContinue();
    }


    public boolean canManuallyTrigger(String pipelineName, Username username) {
        return canManuallyTrigger(goConfigService.pipelineConfigNamed(new CaseInsensitiveString(pipelineName)), CaseInsensitiveString.str(username.getUsername()),
                new ServerHealthStateOperationResult());
    }

    public boolean canManuallyTrigger(PipelineConfig pipelineConfig, String username, OperationResult result) {
        SchedulingChecker checker = buildScheduleCheckers(asList(manualTriggerCheckers(pipelineConfig, username)));
        checker.check(result);
        return result.getServerHealthState().isSuccess();
    }

    public boolean pipelineCanBeTriggeredManually(PipelineConfig pipelineConfig) {
        SchedulingChecker checker = buildScheduleCheckers(asList(manualTriggerCheckersWithoutPermissionsCheck(pipelineConfig)));
        OperationResult result = new HttpOperationResult();
        checker.check(result);
        return result.canContinue();
    }

    public boolean canAutoTriggerConsumer(PipelineConfig pipelineConfig) {
        OperationResult result = new ServerHealthStateOperationResult();
        String pipelineName = CaseInsensitiveString.str(pipelineConfig.name());
        String stageName = CaseInsensitiveString.str(pipelineConfig.getFirstStageConfig().name());
        SchedulingChecker checker = buildScheduleCheckers(asList(new PipelinePauseChecker(pipelineName, pipelinePauseService), new PipelineLockChecker(pipelineName, pipelineLockService),
                new StageActiveChecker(pipelineName, stageName, stageService)));
        checker.check(result);
        return result.getServerHealthState().isSuccess();
    }

    public void canAutoTriggerProducer(PipelineConfig pipelineConfig, OperationResult operationResult) {
        String pipelineName = CaseInsensitiveString.str(pipelineConfig.name());

        SchedulingChecker checker = buildScheduleCheckers(asList(
                new PipelineLockChecker(pipelineName, pipelineLockService),
                new ManualPipelineChecker(pipelineConfig),
                new PipelinePauseChecker(pipelineName, pipelinePauseService),
                new StageActiveChecker(pipelineName, CaseInsensitiveString.str(pipelineConfig.getFirstStageConfig().name()), stageService)));
        checker.check(operationResult);
    }

    public boolean canRerunStage(PipelineIdentifier pipelineIdentifier, String stageName, String username,
                                 OperationResult result) {
        String pipelineName = pipelineIdentifier.getName();

        SchedulingChecker canRerunChecker = buildScheduleCheckers(asList(
                new StageAuthorizationChecker(pipelineName, stageName, username, securityService),
                new PipelinePauseChecker(pipelineName, pipelinePauseService),
                new PipelineActiveChecker(stageService, pipelineIdentifier),
                new StageActiveChecker(pipelineName, stageName, stageService),
                diskCheckers()));
        canRerunChecker.check(result);
        return result.getServerHealthState().isSuccess();
    }

    //TODO: what's the difference between this with the canRerunStage method?
    public boolean canScheduleStage(PipelineIdentifier pipelineIdentifier, String stageName, String username,
                                    final OperationResult result) {
        String pipelineName = pipelineIdentifier.getName();
        CompositeChecker checker = buildScheduleCheckers(asList(
                new StageAuthorizationChecker(pipelineName, stageName, username, securityService),
                new StageLockChecker(pipelineIdentifier, pipelineLockService),
                new PipelinePauseChecker(pipelineName, pipelinePauseService),
                new PipelineActiveChecker(stageService, pipelineIdentifier),
                new StageActiveChecker(pipelineName, stageName, stageService),
                new StageManualTriggerChecker(pipelineName, pipelineIdentifier.getCounter(), stageName, this, pipelineService),
                diskCheckers()));
        checker.check(result);
        return result.canContinue();
    }

    ScheduleStageResult shouldAllowSchedulingStage(Pipeline pipeline, String stageName) {
        if (pipeline == null) {
            return ScheduleStageResult.PipelineNotFound;
        }
        if (pipeline.hasStageBeenRun(stageName)) {
            return ScheduleStageResult.CanSchedule;
        }
        String pipelineName = pipeline.getName();
        if (!goConfigService.hasPreviousStage(pipelineName, stageName)) {
            return ScheduleStageResult.CanSchedule;
        }
        CaseInsensitiveString previousStageName = goConfigService.previousStage(pipelineName, stageName).name();
        if (!pipeline.hasStageBeenRun(CaseInsensitiveString.str(previousStageName))) {
            return ScheduleStageResult.PreviousStageNotRan;
        }
        StageConfig currentStageConfig = goConfigService.nextStage(pipelineName, previousStageName.toString());
        Approval approval = currentStageConfig.getApproval();
        if (approval.isAllowOnlyOnSuccess()) {
            Stage previousStage = pipeline.getStages().byName(previousStageName.toString());
            StageResult previousStageResult = previousStage.getResult();
            if (previousStageResult != StageResult.Passed) {
                return ScheduleStageResult.PreviousStageNotPassed.setPreviousStageValues(previousStageName.toString(), previousStageResult.name());
            }
        }
        return ScheduleStageResult.CanSchedule;
    }

    CompositeChecker buildScheduleCheckers(List<SchedulingChecker> schedulingCheckers) {
        return new CompositeChecker(schedulingCheckers.toArray(new SchedulingChecker[schedulingCheckers.size()]));
    }

    private SchedulingChecker manualTriggerCheckers(PipelineConfig pipelineConfig, String username) {
        String pipelineName = CaseInsensitiveString.str(pipelineConfig.name());
        String stageName = CaseInsensitiveString.str(pipelineConfig.getFirstStageConfig().name());
        StageAuthorizationChecker stageAuthorizationChecker = new StageAuthorizationChecker(pipelineName, stageName, username, securityService);
        return new CompositeChecker(manualTriggerCheckersWithoutPermissionsCheck(pipelineConfig), stageAuthorizationChecker);
    }

    private SchedulingChecker manualTriggerCheckersWithoutPermissionsCheck(PipelineConfig pipelineConfig) {
        return timerTriggerCheckers(pipelineConfig);
    }

    private SchedulingChecker timerTriggerCheckers(PipelineConfig pipelineConfig) {
        String pipelineName = CaseInsensitiveString.str(pipelineConfig.name());
        String stageName = CaseInsensitiveString.str(pipelineConfig.getFirstStageConfig().name());

        return new CompositeChecker(
                new AboutToBeTriggeredChecker(pipelineConfig.name(), triggerMonitor, pipelineScheduleQueue),
                new PipelinePauseChecker(pipelineName, pipelinePauseService),
                new StageActiveChecker(pipelineName, stageName, stageService),
                new PipelineLockChecker(pipelineName, pipelineLockService),
                diskCheckers());
    }

    private SchedulingChecker diskCheckers() {
        return outOfDiskSpaceChecker;
    }

}
