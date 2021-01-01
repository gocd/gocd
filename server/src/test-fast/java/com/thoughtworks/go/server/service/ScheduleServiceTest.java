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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.activity.AgentAssignment;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.perf.SchedulingPerformanceLogger;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.transaction.TestTransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TestTransactionTemplate;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static com.thoughtworks.go.domain.JobResult.*;
import static com.thoughtworks.go.domain.JobState.Building;
import static com.thoughtworks.go.domain.JobState.Completed;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class ScheduleServiceTest {
    private ScheduleService service;
    private JobInstanceService jobInstanceService;
    private GoConfigService goConfigService;
    private EnvironmentConfigService environmentConfigService;
    private ServerHealthService serverHealthService;
    private SchedulingCheckerService schedulingChecker;
    private PipelineScheduleQueue pipelineScheduleQueue;
    private ConsoleActivityMonitor consoleActivityMonitor;
    public PipelinePauseService pipelinePauseService;
    private StageService stageService;
    private SecurityService securityService;
    private PipelineService pipelineService;
    private TimeProvider timeProvider;
    private InstanceFactory instanceFactory = null;
    private SchedulingPerformanceLogger schedulingPerformanceLogger;
    private ElasticProfileService elasticProfileService;
    private ClusterProfilesService clusterProfileService;
    private StageOrderService stageOrderService;
    private PipelineLockService pipelineLockService;

    @Before
    public void setup() {
        createMocks();
    }

    @Test
    public void shouldCancelStage() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Pipeline pipeline = PipelineMother.pipeline("pipeline-name", StageMother.passedStageInstance("mingle", "job-bar", "pipeline-name"));
        Stage spiedStage = spy(pipeline.getFirstStage());
        long stageId = spiedStage.getId();
        Username admin = new Username(new CaseInsensitiveString("admin"));
        doReturn(true).when(spiedStage).isActive();
        when(stageService.stageById(stageId)).thenReturn(spiedStage);
        when(securityService.hasOperatePermissionForStage(pipeline.getName(), spiedStage.getName(), admin.getUsername().toString())).thenReturn(true);
        ScheduleService spyedService = spy(service);
        doNothing().when(spyedService).automaticallyTriggerRelevantStagesFollowingCompletionOf(spiedStage);
        Stage resultStage = spyedService.cancelAndTriggerRelevantStages(stageId, admin, result);

        assertThat(resultStage, is(spiedStage));
        assertThat(result.httpCode(), is(SC_OK));
        assertThat(result.isSuccessful(), is(true));
        assertThat(result.message(), is("Stage cancelled successfully."));

        verify(securityService).hasOperatePermissionForStage(pipeline.getName(), spiedStage.getName(), admin.getUsername().toString());
        verify(stageService).cancelStage(spiedStage, admin.getUsername().toString());
        verify(spiedStage).isActive();
    }

    @Test
    public void shouldNotCancelStageIfItsNotActive() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Pipeline pipeline = PipelineMother.pipeline("pipeline-name", StageMother.passedStageInstance("mingle", "job-bar", "pipeline-name"));
        Stage firstStage = pipeline.getFirstStage();
        long stageId = firstStage.getId();
        Username admin = new Username(new CaseInsensitiveString("admin"));
        when(stageService.stageById(stageId)).thenReturn(firstStage);
        Stage resultStage = service.cancelAndTriggerRelevantStages(stageId, admin, result);
        assertThat(resultStage, is(firstStage));
        assertThat(result.httpCode(), is(SC_OK));
        assertThat(result.isSuccessful(), is(true));
        assertThat(result.hasMessage(), is(true));
        String respMsg = "Stage is not active. Cancellation Ignored.";
        assertThat(result.message(), is(respMsg));
        verify(stageService).stageById(stageId);
    }

    @Test
    public void shouldNotCancelStageWhenTheUserDoesNotHaveOperatePermission() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Pipeline pipeline = PipelineMother.pipeline("pipeline-name", StageMother.passedStageInstance("mingle", "job-bar", "pipeline-name"));
        Stage spiedStage = spy(pipeline.getFirstStage());

        long stageId = spiedStage.getId();
        Username admin = new Username(new CaseInsensitiveString("admin"));

        doReturn(true).when(spiedStage).isActive();
        when(stageService.stageById(stageId)).thenReturn(spiedStage);
        when(securityService.hasOperatePermissionForStage(pipeline.getName(), spiedStage.getName(), admin.getUsername().toString())).thenReturn(false);

        Stage resultStage = service.cancelAndTriggerRelevantStages(stageId, admin, result);

        assertThat(resultStage, is(nullValue()));
        assertThat(result.httpCode(), is(SC_FORBIDDEN));
        assertThat(result.isSuccessful(), is(false));
        verify(securityService).hasOperatePermissionForStage(pipeline.getName(), spiedStage.getName(), admin.getUsername().toString());
        verify(stageService, never()).cancelStage(spiedStage, admin.getUsername().toString());
        verify(spiedStage).isActive();
    }

    @Test
    public void shouldNotCompleteBuildThatIsCancelled() {
        String agentUuid = "uuid";
        JobIdentifier jobIdendifier = new JobIdentifier("pipeline", 1, "label", "stage", "LATEST", "build", 1L);

        final JobInstance job = new JobInstance("test");

        //MF this is!

        job.setResult(JobResult.Cancelled);
        when(jobInstanceService.buildByIdWithTransitions(jobIdendifier.getBuildId())).thenReturn(job);
        service.jobCompleting(jobIdendifier, Passed, agentUuid);
        verify(jobInstanceService).buildByIdWithTransitions(jobIdendifier.getBuildId());
        verifyNoMoreInteractions(jobInstanceService);
    }

    @Test
    public void shouldSetServerHealthMessageWhenStageScheduleFailsWithCannotScheduleException() {
        final PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline-quux");
        when(goConfigService.pipelineConfigNamed(new CaseInsensitiveString("pipeline-quux"))).thenReturn(pipelineConfig);
        when(environmentConfigService.agentsForPipeline(new CaseInsensitiveString("pipeline-quux"))).thenReturn(new Agents());
        when(goConfigService.scheduleStage("pipeline-quux", "mingle", new DefaultSchedulingContext("loser", new Agents()))).thenThrow(new CannotScheduleException("foo", "stage-baz"));
        try {
            Pipeline pipeline = PipelineMother.pipeline("pipeline-quux", StageMother.passedStageInstance("mingle", "job-bar", "pipeline-name"));
            service.scheduleStage(pipeline, "mingle", "loser",
                    new ScheduleService.NewStageInstanceCreator(goConfigService), new ScheduleService.ExceptioningErrorHandler());
            fail("should have failed as stage could not be scheduled");
        } catch (CannotScheduleException e) {
            assertThat(e.getMessage(), is("foo"));
        }
        verify(serverHealthService).update(ServerHealthState.failedToScheduleStage(HealthStateType.general(HealthStateScope.forStage("pipeline-quux", "stage-baz")),
                "pipeline-quux", "stage-baz", "foo"));
    }

    @Test
    public void shouldClearServerHealthMessageWhenStageScheduleCompletesSuccessfully() {
        final PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline-quux");
        when(goConfigService.pipelineConfigNamed(new CaseInsensitiveString("pipeline-quux"))).thenReturn(pipelineConfig);
        when(environmentConfigService.agentsForPipeline(new CaseInsensitiveString("pipeline-quux"))).thenReturn(new Agents());
        when(goConfigService.scheduleStage("pipeline-quux", "mingle", new DefaultSchedulingContext("loser", new Agents()))).thenReturn(new Stage());
        Stage stageConfig = StageMother.passedStageInstance("mingle", "job-bar", "pipeline-name");
        service.scheduleStage(PipelineMother.pipeline("pipeline-quux", stageConfig), "mingle", "loser",
                new ScheduleService.NewStageInstanceCreator(goConfigService), new ScheduleService.ExceptioningErrorHandler());
        verify(serverHealthService).update(ServerHealthState.success(HealthStateType.general(HealthStateScope.forStage("pipeline-quux", "mingle"))));
    }

    @Test
    public void shouldSetServerHealthMessageWhenPipelineCreationFailsWithCannotScheduleException() {
        final PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline-quux");
        when(goConfigService.pipelineConfigNamed(new CaseInsensitiveString("pipeline-quux"))).thenReturn(pipelineConfig);
        CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);
        when(cruiseConfig.getMd5()).thenReturn("md5-test");
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        when(schedulingChecker.canAutoTriggerConsumer(pipelineConfig)).thenReturn(true);
        when(pipelineScheduleQueue.createPipeline(any(BuildCause.class), eq(pipelineConfig), any(SchedulingContext.class), eq("md5-test"), eq(timeProvider))).thenThrow(
                new CannotScheduleException("foo", "stage-baz"));
        final HashMap<CaseInsensitiveString, BuildCause> map = new HashMap<>();
        map.put(new CaseInsensitiveString("pipeline-quux"), BuildCause.createManualForced());
        when(pipelineScheduleQueue.toBeScheduled()).thenReturn(map);

        service.autoSchedulePipelinesFromRequestBuffer();

        verify(serverHealthService).update(ServerHealthState.failedToScheduleStage(HealthStateType.general(HealthStateScope.forStage("pipeline-quux", "stage-baz")),
                "pipeline-quux", "stage-baz", "foo"));
    }

    @Test
    public void shouldClearServerHealthMessageWhenPipelineGetsCreatedSuccessfully() {
        final PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline-quux");
        final MaterialConfig materialConfig = pipelineConfig.materialConfigs().get(0);
        when(goConfigService.pipelineConfigNamed(new CaseInsensitiveString("pipeline-quux"))).thenReturn(pipelineConfig);
        CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);
        when(cruiseConfig.getMd5()).thenReturn("md5-test");
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        when(schedulingChecker.canAutoTriggerConsumer(pipelineConfig)).thenReturn(true);
        when(pipelineScheduleQueue.createPipeline(any(BuildCause.class), eq(pipelineConfig), any(SchedulingContext.class), eq("md5-test"), eq(timeProvider))).thenReturn(PipelineMother.schedule(pipelineConfig,
                BuildCause.createManualForced(new MaterialRevisions(new MaterialRevision(new MaterialConfigConverter().toMaterial(materialConfig), ModificationsMother.aCheckIn("123", "foo.c"))), new Username(new CaseInsensitiveString("loser")))));
        final HashMap<CaseInsensitiveString, BuildCause> map = new HashMap<>();
        map.put(new CaseInsensitiveString("pipeline-quux"), BuildCause.createManualForced());
        when(pipelineScheduleQueue.toBeScheduled()).thenReturn(map);

        service.autoSchedulePipelinesFromRequestBuffer();

        verify(serverHealthService).update(ServerHealthState.success(HealthStateType.general(HealthStateScope.forStage("pipeline-quux", "mingle"))));
    }

    @Test
    public void shouldCancelUnresponsiveJobs() {
        service.cancelHungJobs();
        verify(consoleActivityMonitor).cancelUnresponsiveJobs(service);
    }

    @Test
    public void shouldUnlockPipelineBasedOnLockSetting() throws Exception {
        assertUnlockPipeline("unlock when next stage is manual, this stage is passed and pipeline is unlockable", Completed, Passed, false, true, true, true);
        assertUnlockPipeline("don't unlock when next stage manual, this stage is passed and pipeline is not unlockable", Completed, Passed, false, true, false, false);
        assertUnlockPipeline("unlock when next stage is manual, this stage is failed and pipeline is unlockable", Completed, Failed, false, true, true, true);
        assertUnlockPipeline("don't unlock when next stage manual, this stage is failed and pipeline is not unlockable", Completed, Failed, false, true, false, false);

        assertUnlockPipeline("unlock when last stage is passed and pipeline is unlockable", Completed, Passed, true, false, true, true);
        assertUnlockPipeline("unlock when last stage is failed and pipeline is unlockable", Completed, Failed, true, false, true, true);

        assertUnlockPipeline("unlock when last stage is passed and pipeline is not unlockable", Completed, Passed, true, false, false, true);
        assertUnlockPipeline("unlock when last stage is failed and pipeline is not unlockable", Completed, Failed, true, false, false, true);

        assertUnlockPipeline("unlock when non-last stage is cancelled and pipeline is unlockable", Completed, Cancelled, false, false, true, true);
        assertUnlockPipeline("don't unlock when non-last stage is cancelled and pipeline is not unlockable", Completed, Cancelled, false, false, false, false);
        assertUnlockPipeline("unlock when last stage is cancelled and pipeline is unlockable", Completed, Cancelled, true, false, true, true);
        assertUnlockPipeline("unlock when last stage is cancelled and pipeline is not unlockable", Completed, Cancelled, true, false, false, true);

        assertUnlockPipeline("unlock when non-last stage is failed and pipeline is unlockable", Completed, Failed, false, false, true, true);
        assertUnlockPipeline("don't unlock when non-last stage is failed and pipeline is not unlockable", Completed, Failed, false, false, false, false);
        assertUnlockPipeline("unlock when non-last stage is failed, next stage is manual and pipeline is unlockable", Completed, Failed, false, true, true, true);
        assertUnlockPipeline("don't unlock when non-last stage is failed, next stage is manual and pipeline is not unlockable", Completed, Failed, false, true, false, false);

        assertUnlockPipeline("don't unlock when stage has not completed and pipeline is not unlockable", Building, Cancelled, false, true, false, false);
        assertUnlockPipeline("don't unlock when stage has not completed and pipeline is unlockable", Building, Unknown, false, true, true, false);
    }

    private void assertUnlockPipeline(String message, JobState state, JobResult result, boolean isLastStage, boolean isNextStageManual, boolean isUnlockablePipeline, boolean shouldUnlock) {
        createMocks();

        Stage stage = StageMother.stageWithNBuildsHavingEndState(state, result, "stage1", "job1");
        StageConfig stage2Config = isNextStageManual ? StageConfigMother.manualStage("stage2") : StageConfigMother.custom("stage2");
        Pipeline pipeline = PipelineMother.pipeline("pipeline1");

        when(stageOrderService.getNextStage(pipeline, "stage1")).thenReturn(isLastStage ? null : stage2Config);
        when(goConfigService.isUnlockableWhenFinished(pipeline.getName())).thenReturn(isUnlockablePipeline);

        service.unlockIfNecessary(pipeline, stage);

        verify(pipelineLockService, times(shouldUnlock ? 1 : 0)).unlock(pipeline.getName());
    }

    private void createMocks() {
        jobInstanceService = mock(JobInstanceService.class);
        goConfigService = mock(GoConfigService.class);
        environmentConfigService = mock(EnvironmentConfigService.class);
        serverHealthService = mock(ServerHealthService.class);
        final TestTransactionSynchronizationManager synchronizationManager = new TestTransactionSynchronizationManager();
        schedulingChecker = mock(SchedulingCheckerService.class);
        pipelineScheduleQueue = mock(PipelineScheduleQueue.class);
        consoleActivityMonitor = mock(ConsoleActivityMonitor.class);
        pipelinePauseService = mock(PipelinePauseService.class);
        stageService = mock(StageService.class);
        securityService = mock(SecurityService.class);
        pipelineService = mock(PipelineService.class);
        timeProvider = new TimeProvider();
        schedulingPerformanceLogger = mock(SchedulingPerformanceLogger.class);
        elasticProfileService = mock(ElasticProfileService.class);
        stageOrderService = mock(StageOrderService.class);
        pipelineLockService = mock(PipelineLockService.class);
        clusterProfileService = mock(ClusterProfilesService.class);
        service = new ScheduleService(goConfigService, pipelineService, stageService, schedulingChecker, mock(PipelineDao.class), mock(StageDao.class), stageOrderService, securityService, pipelineScheduleQueue,
                jobInstanceService, mock(JobInstanceDao.class), mock(AgentAssignment.class), environmentConfigService, pipelineLockService, serverHealthService,
                new TestTransactionTemplate(synchronizationManager),
                mock(AgentService.class), synchronizationManager, timeProvider, consoleActivityMonitor, pipelinePauseService, instanceFactory, schedulingPerformanceLogger, elasticProfileService, clusterProfileService);
    }
}
