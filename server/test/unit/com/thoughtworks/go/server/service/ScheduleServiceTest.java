/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.perf.SchedulingPerformanceLogger;
import com.thoughtworks.go.server.scheduling.PipelineScheduledMessage;
import com.thoughtworks.go.server.scheduling.PipelineScheduledTopic;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.transaction.TestTransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TestTransactionTemplate;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.commons.httpclient.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static com.thoughtworks.go.helper.ModificationsMother.modifySomeFiles;
import static com.thoughtworks.go.util.DataStructureUtils.m;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
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
    private Localizer localizer;
    private TimeProvider timeProvider;
    private InstanceFactory instanceFactory = null;
    private SchedulingPerformanceLogger schedulingPerformanceLogger;
    private ElasticProfileService elasticProfileService;

    @Before
    public void setup() {
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
        localizer = mock(Localizer.class);
        timeProvider = new TimeProvider();
        schedulingPerformanceLogger = mock(SchedulingPerformanceLogger.class);
        elasticProfileService = mock(ElasticProfileService.class);
        service = new ScheduleService(goConfigService, pipelineService, stageService, schedulingChecker, mock(
                PipelineScheduledTopic.class), mock(PipelineDao.class), mock(StageDao.class), mock(StageOrderService.class), securityService, pipelineScheduleQueue,
                jobInstanceService, mock(JobInstanceDao.class), mock(AgentAssignment.class), environmentConfigService, mock(PipelineLockService.class), serverHealthService,
                new TestTransactionTemplate(synchronizationManager),
                mock(AgentService.class), synchronizationManager, timeProvider, consoleActivityMonitor, pipelinePauseService, instanceFactory, schedulingPerformanceLogger, elasticProfileService);
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
        assertThat(result.httpCode(), is(HttpStatus.SC_OK));
        assertThat(result.isSuccessful(), is(true));
        when(localizer.localize("STAGE_CANCELLED_SUCCESSFULLY", new Object[] {})).thenReturn("Stage cancelled successfully.");
        assertThat(result.message(localizer), is("Stage cancelled successfully."));
        verify(localizer).localize("STAGE_CANCELLED_SUCCESSFULLY", new Object[] {});

        verify(securityService).hasOperatePermissionForStage(pipeline.getName(), spiedStage.getName(), admin.getUsername().toString());
        verify(stageService).cancelStage(spiedStage);
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
        assertThat(result.httpCode(), is(HttpStatus.SC_OK));
        assertThat(result.isSuccessful(), is(true));
        assertThat(result.hasMessage(), is(true));
        Localizer localizer = mock(Localizer.class);
        String respMsg = "Stage is not active. Cancellation Ignored";
        String stageNotActiveKey = "STAGE_IS_NOT_ACTIVE_FOR_CANCELLATION";
        when(localizer.localize(eq(stageNotActiveKey),anyVararg())).thenReturn(respMsg);
        assertThat(result.message(localizer),is(respMsg));
        verify(stageService).stageById(stageId);
        verify(localizer).localize(eq(stageNotActiveKey),anyVararg());
    }

    @Test
    public void shouldCancelStageUsingPipelineNameAndStageName() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String pipelineName = "pipeline-name";
        Username admin = new Username(new CaseInsensitiveString("admin"));
        String stageName = "mingle";
        Pipeline pipeline = PipelineMother.pipeline(pipelineName, StageMother.passedStageInstance(stageName, "job-bar", pipelineName));
        Stage firstStage = pipeline.getFirstStage();
        long stageId = firstStage.getId();
        when(stageService.findLatestStage(pipelineName, stageName)).thenReturn(firstStage);
        ScheduleService spyedService = spy(service);
        doReturn(firstStage).when(spyedService).cancelAndTriggerRelevantStages(stageId, admin, result);
        Stage resultStage = spyedService.cancelAndTriggerRelevantStages(pipelineName, stageName, admin, result);
        assertThat(resultStage, is(firstStage));
        assertThat(result.httpCode(), is(HttpStatus.SC_OK));
        assertThat(result.isSuccessful(), is(true));
        verify(stageService).findLatestStage(pipelineName, stageName);
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
        assertThat(result.httpCode(), is(HttpStatus.SC_UNAUTHORIZED));
        assertThat(result.isSuccessful(), is(false));
        verify(securityService).hasOperatePermissionForStage(pipeline.getName(), spiedStage.getName(), admin.getUsername().toString());
        verify(stageService, never()).cancelStage(spiedStage);
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
        service.jobCompleting(jobIdendifier, JobResult.Passed, agentUuid);
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
                "pipeline-quux","stage-baz" , "foo"));
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
        final HashMap<String, BuildCause> map = new HashMap<String, BuildCause>();
        map.put("pipeline-quux", BuildCause.createManualForced());
        when(pipelineScheduleQueue.toBeScheduled()).thenReturn(map);

        service.autoSchedulePipelinesFromRequestBuffer();

        verify(serverHealthService).update(ServerHealthState.failedToScheduleStage(HealthStateType.general(HealthStateScope.forStage("pipeline-quux", "stage-baz")),
                "pipeline-quux","stage-baz" , "foo"));
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
                BuildCause.createManualForced(new MaterialRevisions(new MaterialRevision(MaterialsMother.createMaterialFromMaterialConfig(materialConfig), ModificationsMother.aCheckIn("123", "foo.c"))), new Username(new CaseInsensitiveString("loser")))));
        final HashMap<String, BuildCause> map = new HashMap<String, BuildCause>();
        map.put("pipeline-quux", BuildCause.createManualForced());
        when(pipelineScheduleQueue.toBeScheduled()).thenReturn(map);

        service.autoSchedulePipelinesFromRequestBuffer();

        verify(serverHealthService).update(ServerHealthState.success(HealthStateType.general(HealthStateScope.forStage("pipeline-quux", "mingle"))));
    }

    @Test
    public void shouldNotifyForEveryPipelineCreated() throws Exception {
        CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);
        when(cruiseConfig.getMd5()).thenReturn("md5-test");
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        StubPipelineScheduleTopic stubTopic = new StubPipelineScheduleTopic();
        service = new ScheduleService(goConfigService, null, null, schedulingChecker, stubTopic, null, null, null, null, pipelineScheduleQueue,
                jobInstanceService, null, null, environmentConfigService, null, serverHealthService, null, null, null, timeProvider, null, null, null, schedulingPerformanceLogger, elasticProfileService);

        PipelineConfig mingleConfig = PipelineConfigMother.createPipelineConfig("mingle", "build", "unit", "functional");
        PipelineConfig evolveConfig = PipelineConfigMother.createPipelineConfig("evolve", "build", "unit");
        BuildCause mingleBuildCause = modifySomeFiles(mingleConfig);
        BuildCause evolveBuildCause = modifySomeFiles(evolveConfig);
        when(pipelineScheduleQueue.toBeScheduled()).thenReturn(m("mingle", mingleBuildCause, "evolve", evolveBuildCause));

        when(goConfigService.pipelineConfigNamed(new CaseInsensitiveString("mingle"))).thenReturn(mingleConfig);
        when(goConfigService.pipelineConfigNamed(new CaseInsensitiveString("evolve"))).thenReturn(evolveConfig);

        when(schedulingChecker.canAutoTriggerConsumer(mingleConfig)).thenReturn(true);
        when(schedulingChecker.canAutoTriggerConsumer(evolveConfig)).thenReturn(true);

        when(pipelineScheduleQueue.createPipeline(mingleBuildCause, mingleConfig, new DefaultSchedulingContext(GoConstants.DEFAULT_APPROVED_BY, new Agents()), "md5-test", timeProvider)).thenReturn(
                PipelineMother.schedule(mingleConfig, mingleBuildCause));
        when(pipelineScheduleQueue.createPipeline(evolveBuildCause, evolveConfig, new DefaultSchedulingContext(GoConstants.DEFAULT_APPROVED_BY, new Agents()), "md5-test", timeProvider)).thenReturn(
                PipelineMother.schedule(evolveConfig, evolveBuildCause));

        service.autoSchedulePipelinesFromRequestBuffer();

        assertThat(stubTopic.callCount, Matchers.is(2));
    }

    @Test
    public void shouldCancelUnresponsiveJobs() {
        service.cancelHungJobs();
        verify(consoleActivityMonitor).cancelUnresponsiveJobs(service);
    }

    private static class StubPipelineScheduleTopic extends PipelineScheduledTopic {
        private int callCount;

        public StubPipelineScheduleTopic() {
            super(null);
        }

        public void post(PipelineScheduledMessage message) {
            callCount++;
        }
    }

}
