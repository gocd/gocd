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

import java.util.List;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.PipelineIdentifier;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.scheduling.TriggerMonitor;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.util.Arrays.asList;

@Service
public class SchedulingCheckerService {
    private final GoConfigService goConfigService;
    private final CurrentActivityService activityService;
    private final SystemEnvironment systemEnvironment;
    private final SecurityService securityService;
    private final PipelineLockService pipelineLockService;
    private final UserService userService;
    private final TriggerMonitor triggerMonitor;
    private final PipelineScheduleQueue pipelineScheduleQueue;
    private ServerHealthService serverHealthService;
    private PipelinePauseService pipelinePauseService;

    @Autowired
    public SchedulingCheckerService(GoConfigService goConfigService,
                                    CurrentActivityService activityService,
                                    SystemEnvironment systemEnvironment,
                                    SecurityService securityService,
                                    PipelineLockService pipelineLockService,
                                    TriggerMonitor triggerMonitor, PipelineScheduleQueue pipelineScheduleQueue,
                                    UserService userService, ServerHealthService serverHealthService,
                                    PipelinePauseService pipelinePauseService) {
        this.goConfigService = goConfigService;
        this.activityService = activityService;
        this.systemEnvironment = systemEnvironment;
        this.securityService = securityService;
        this.pipelineLockService = pipelineLockService;
        this.triggerMonitor = triggerMonitor;
        this.pipelineScheduleQueue = pipelineScheduleQueue;
        this.userService = userService;
        this.serverHealthService = serverHealthService;
        this.pipelinePauseService = pipelinePauseService;
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

    public boolean canAutoTriggerConsumer(PipelineConfig pipelineConfig) {
        OperationResult result = new ServerHealthStateOperationResult();
        String pipelineName = CaseInsensitiveString.str(pipelineConfig.name());
        String stageName = CaseInsensitiveString.str(pipelineConfig.getFirstStageConfig().name());
        SchedulingChecker checker = buildScheduleCheckers(asList(new PipelinePauseChecker(pipelineName, pipelinePauseService), new PipelineLockChecker(pipelineName, pipelineLockService),
                new StageActiveChecker(pipelineName, stageName, activityService)));
        checker.check(result);
        return result.getServerHealthState().isSuccess();
    }

    public void canAutoTriggerProducer(PipelineConfig pipelineConfig, OperationResult operationResult) {
        String pipelineName = CaseInsensitiveString.str(pipelineConfig.name());

        SchedulingChecker checker = buildScheduleCheckers(asList(
                new PipelineLockChecker(pipelineName, pipelineLockService),
                new ManualPipelineChecker(pipelineConfig),
                new PipelinePauseChecker(pipelineName, pipelinePauseService),
                new PipelineLockChecker(pipelineName, pipelineLockService),
                new StageActiveChecker(pipelineName, CaseInsensitiveString.str(pipelineConfig.getFirstStageConfig().name()), activityService)));
        checker.check(operationResult);
    }

    public boolean canRerunStage(PipelineIdentifier pipelineIdentifier, String stageName, String username,
                                 OperationResult result) {
        String pipelineName = pipelineIdentifier.getName();

        SchedulingChecker canRerunChecker = buildScheduleCheckers(asList(
                new StageAuthorizationChecker(pipelineName, stageName, username, securityService),
                new PipelinePauseChecker(pipelineName, pipelinePauseService),
                new PipelineActiveChecker(activityService, pipelineIdentifier),
                new StageActiveChecker(pipelineName, stageName, activityService),
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
                new PipelineActiveChecker(activityService, pipelineIdentifier),
                new StageActiveChecker(pipelineName, stageName, activityService),
                diskCheckers()));
        checker.check(result);
        return result.canContinue();
    }

    CompositeChecker buildScheduleCheckers(List<SchedulingChecker> schedulingCheckers) {
        return new CompositeChecker(schedulingCheckers.toArray(new SchedulingChecker[schedulingCheckers.size()]));
    }


    SchedulingChecker manualTriggerCheckers(PipelineConfig pipelineConfig, String username) {
        String pipelineName = CaseInsensitiveString.str(pipelineConfig.name());
        String stageName = CaseInsensitiveString.str(pipelineConfig.getFirstStageConfig().name());
        return new CompositeChecker(timerTriggerCheckers(pipelineConfig), new StageAuthorizationChecker(pipelineName, stageName, username, securityService));
    }

    SchedulingChecker timerTriggerCheckers(PipelineConfig pipelineConfig) {
        String pipelineName = CaseInsensitiveString.str(pipelineConfig.name());
        String stageName = CaseInsensitiveString.str(pipelineConfig.getFirstStageConfig().name());

        return new CompositeChecker(
                new AboutToBeTriggeredChecker(pipelineName, triggerMonitor, pipelineScheduleQueue),
                new PipelinePauseChecker(pipelineName, pipelinePauseService),
                new StageActiveChecker(pipelineName, stageName, activityService),
                new PipelineLockChecker(pipelineName, pipelineLockService),
                diskCheckers());
    }

    SchedulingChecker diskCheckers() {
        ArtifactsDiskSpaceFullChecker artifactsDiskSpaceFullChecker =
                new ArtifactsDiskSpaceFullChecker(systemEnvironment, goConfigService);

        DatabaseDiskSpaceFullChecker databaseDiskSpaceFullChecker = new DatabaseDiskSpaceFullChecker(
                systemEnvironment, goConfigService);

        return new CompositeChecker(artifactsDiskSpaceFullChecker, databaseDiskSpaceFullChecker);
    }

}
