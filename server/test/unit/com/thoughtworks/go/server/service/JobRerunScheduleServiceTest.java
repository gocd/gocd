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
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.perf.SchedulingPerformanceLogger;
import com.thoughtworks.go.server.scheduling.PipelineScheduledTopic;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.server.transaction.TestTransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TestTransactionTemplate;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.LogFixture;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.springframework.transaction.support.TransactionCallback;

import java.util.Date;
import java.util.concurrent.Semaphore;

import static com.thoughtworks.go.util.DataStructureUtils.a;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class JobRerunScheduleServiceTest {
    private ScheduleService service;
    private JobInstanceService jobInstanceService;
    private GoConfigService goConfigService;
    private EnvironmentConfigService environmentConfigService;
    private ServerHealthService serverHealthService;
    private SchedulingCheckerService schedulingChecker;
    private PipelineScheduleQueue pipelineScheduleQueue;
    private PipelineService pipelineService;
    private StageService stageService;
    private SecurityService securityService;
    private PipelineLockService lockService;
    private TestTransactionTemplate txnTemplate;
    private TimeProvider timeProvider;
    private InstanceFactory instanceFactory;
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
        pipelineService = mock(PipelineService.class);
        stageService = mock(StageService.class);
        securityService = mock(SecurityService.class);
        lockService = mock(PipelineLockService.class);
        txnTemplate = new TestTransactionTemplate(synchronizationManager);
        timeProvider = new TimeProvider();
        instanceFactory = mock(InstanceFactory.class);
        schedulingPerformanceLogger = mock(SchedulingPerformanceLogger.class);
        elasticProfileService = mock(ElasticProfileService.class);

        service = new ScheduleService(goConfigService, pipelineService, stageService, schedulingChecker, mock(PipelineScheduledTopic.class), mock(PipelineDao.class),
                mock(StageDao.class), mock(StageOrderService.class), securityService, pipelineScheduleQueue, jobInstanceService, mock(JobInstanceDao.class), mock(AgentAssignment.class),
                environmentConfigService, lockService, serverHealthService, txnTemplate, mock(AgentService.class), synchronizationManager, timeProvider, null, null, instanceFactory,
                schedulingPerformanceLogger, elasticProfileService);
    }

    @Test
    public void shouldScheduleRerunJobStage() {
        PipelineConfig mingleConfig = PipelineConfigMother.createPipelineConfig("mingle", "build", "unit", "functional");

        Pipeline pipeline = PipelineMother.passedPipelineInstance("mingle", "build", "unit");

        Stage firstStage = pipeline.getFirstStage();
        Stage expectedStageToBeCreated = firstStage.createClone();

        stub(mingleConfig, pipeline, firstStage);
        stubConfigMd5Cal("latest-md5");

        when(instanceFactory.createStageForRerunOfJobs(eq(firstStage), eq(a("unit")), Matchers.<SchedulingContext>any(), eq(mingleConfig.first()), eq(timeProvider), eq("latest-md5")))
                .thenReturn(expectedStageToBeCreated);

        Stage stage = service.rerunJobs(firstStage, a("unit"), new HttpOperationResult());

        assertThat(stage, is(not(nullValue())));
        verify(stageService).save(pipeline, stage);
        verify(lockService).lockIfNeeded(pipeline);
    }

    private void stubConfigMd5Cal(String latestMd5) {
        CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        when(cruiseConfig.getMd5()).thenReturn(latestMd5);
    }

    @Test
    public void shouldMarkResultIfScheduleFailsForUnexpectedReason() {
        PipelineConfig mingleConfig = PipelineConfigMother.createPipelineConfig("mingle", "build", "unit", "functional");

        Pipeline pipeline = PipelineMother.passedPipelineInstance("mingle", "build", "unit");

        Stage firstStage = pipeline.getFirstStage();

        stub(mingleConfig, pipeline, firstStage);

        schedulingChecker = mock(SchedulingCheckerService.class);//leads to null pointer exception
        doThrow(new NullPointerException("The whole world is a big null.")).when(schedulingChecker).canSchedule(any(OperationResult.class));

        service = new ScheduleService(goConfigService, pipelineService, stageService, schedulingChecker, mock(PipelineScheduledTopic.class), mock(PipelineDao.class),
                mock(StageDao.class), mock(StageOrderService.class), securityService, pipelineScheduleQueue, jobInstanceService, mock(JobInstanceDao.class), mock(AgentAssignment.class),
                environmentConfigService, lockService, serverHealthService, txnTemplate, mock(AgentService.class), null, null, null, null, null, schedulingPerformanceLogger,
                null
        );

        HttpOperationResult result = new HttpOperationResult();
        LogFixture logFixture = LogFixture.startListening();
        Stage stage = service.rerunJobs(firstStage, a("unit"), result);
        assertThat(logFixture.contains(Level.ERROR, "Job rerun request for job(s) [unit] could not be completed because of unexpected failure. Cause: The whole world is a big null."), is(true));
        logFixture.stopListening();

        assertThat(stage, is(nullValue()));
        assertThat(result.httpCode(), is(400));
        assertThat(result.message(), is("Job rerun request for job(s) [unit] could not be completed because of unexpected failure. Cause: The whole world is a big null."));
    }

    @Test
    public void shouldNotScheduleWhenPreviousStageHasNotBeenRun() {
        PipelineConfig mingleConfig = PipelineConfigMother.createPipelineConfigWithStages("mingle", "compile", "link", "test");

        Pipeline pipeline = PipelineMother.passedPipelineInstance("mingle", "compile", "dev");

        Stage lastStage = StageMother.createPassedStage("mingle", 1, "test", 1, "dev", new Date());

        stub(mingleConfig, pipeline, lastStage);

        when(goConfigService.hasPreviousStage("mingle", lastStage.getIdentifier().getStageName())).thenReturn(true);
        when(goConfigService.previousStage("mingle", lastStage.getIdentifier().getStageName())).thenReturn(mingleConfig.get(1));

        assertScheduleFailure("dev", lastStage, "Can not run stage [test] in pipeline [mingle] because its previous stage has not been run.", 400);
    }

    @Test
    public void shouldNotScheduleWhenApproverIsNotOperator() {
        PipelineConfig mingleConfig = PipelineConfigMother.createPipelineConfig("mingle", "build", "unit", "functional");

        Pipeline pipeline = PipelineMother.passedPipelineInstance("mingle", "build", "unit");

        Stage firstStage = pipeline.getFirstStage();

        stub(mingleConfig, pipeline, firstStage);

        when(securityService.hasOperatePermissionForStage(eq("mingle"), eq(firstStage.getName()), any(String.class))).thenReturn(false);

        assertScheduleFailure("unit", firstStage, "User does not have operate permissions for stage [build] of pipeline [mingle]", 401);
    }

    @Test
    public void shouldUpdateServerHealthStateWhenCantSchedule() {//no matching agents found
        String latestMd5 = "latest-md5";

        PipelineConfig mingleConfig = PipelineConfigMother.createPipelineConfig("mingle", "build", "unit", "functional");
        StageConfig stageConfig = mingleConfig.get(0);
        stageConfig.getJobs().getJob(new CaseInsensitiveString("unit")).setRunOnAllAgents(true);

        Pipeline pipeline = PipelineMother.passedPipelineInstance("mingle", "build", "unit");

        Stage firstStage = pipeline.getFirstStage();

        stub(mingleConfig, pipeline, firstStage);
        stubConfigMd5Cal(latestMd5);

        when(instanceFactory.createStageForRerunOfJobs(eq(firstStage), eq(a("unit")), Matchers.<SchedulingContext>any(), eq(stageConfig), eq(timeProvider), eq(latestMd5)))
                .thenThrow(new CannotScheduleException("Could not find matching agents to run job [unit] of stage [build].", "build"));

        assertScheduleFailure("unit", firstStage, "Could not find matching agents to run job [unit] of stage [build].", 409);
    }

    @Test
    public void shouldSynchronizeAroundRerunJobsFlow() throws InterruptedException {
        PipelineConfig mingleConfig = PipelineConfigMother.createPipelineConfig("mingle", "build", "unit", "functional");

        Pipeline pipeline = PipelineMother.passedPipelineInstance("mingle", "build", "unit");

        final Stage firstStage = pipeline.getFirstStage();

        stub(mingleConfig, pipeline, firstStage);
        stubConfigMd5Cal("latest-md5");

        final Semaphore sem = new Semaphore(1);
        sem.acquire();

        final ThreadLocal<Integer> requestNumber = new ThreadLocal<Integer>();

        final boolean[] firstRequestFinished = new boolean[]{false};

        final boolean[] secondReqGotInAfterFirstFinished = new boolean[]{false};

        schedulingChecker = new SchedulingCheckerService(null, null, null, null, null, null, null, null, null, null) {
            @Override
            public boolean canSchedule(OperationResult result) {
                if (requestNumber.get() == 0) {//is first request, and has lock
                    sem.release(); //now we are in the locked section, so let the other request try
                }
                if (requestNumber.get() == 1) {//this is the second req
                    secondReqGotInAfterFirstFinished[0] = firstRequestFinished[0];//was the first thread done with last bit of useful work before second came in?
                }
                return true;
            }

            @Override
            public boolean canRerunStage(PipelineIdentifier pipelineIdentifier, String stageName, String username, OperationResult result) {
                return true;
            }
        };

        TestTransactionTemplate template = new TestTransactionTemplate(new TestTransactionSynchronizationManager()) {
            @Override
            public Object execute(TransactionCallback action) {
                if (requestNumber.get() == 0) {
                    try {
                        Thread.sleep(5000);//let the other thread try for 5 seconds
                    } catch (InterruptedException e) {
                        throw new RuntimeException();
                    }
                }
                return super.execute(action);
            }
        };
        service = new ScheduleService(goConfigService, pipelineService, stageService, schedulingChecker, mock(PipelineScheduledTopic.class), mock(PipelineDao.class),
                mock(StageDao.class), mock(StageOrderService.class), securityService, pipelineScheduleQueue, jobInstanceService, mock(JobInstanceDao.class), mock(AgentAssignment.class),
                environmentConfigService, lockService, serverHealthService, template, mock(AgentService.class), null, timeProvider, null, null, mock(InstanceFactory.class),
                schedulingPerformanceLogger, elasticProfileService) {
            @Override
            public Stage scheduleStage(Pipeline pipeline, String stageName, String username, StageInstanceCreator creator,
                                       ErrorConditionHandler errorHandler) {
                Stage stage = super.scheduleStage(pipeline, stageName, username, creator, errorHandler);
                if (requestNumber.get() == 0) {
                    firstRequestFinished[0] = true;
                }
                return stage;
            }
        };

        Thread firstReq = new Thread(new Runnable() {
            public void run() {
                requestNumber.set(0);
                service.rerunJobs(firstStage, a("unit"), new HttpOperationResult());
            }
        });

        Thread secondReq = new Thread(new Runnable() {
            public void run() {
                try {
                    requestNumber.set(1);
                    sem.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                service.rerunJobs(firstStage, a("unit"), new HttpOperationResult());
            }
        });

        firstReq.start();
        secondReq.start();

        firstReq.join();
        secondReq.join();

        assertThat("second request should have gone-in only after first is out", secondReqGotInAfterFirstFinished[0], is(true));
    }

    @Test
    public void shouldErrorOutWhenNoJobIsSelectedForReRun() {
        Pipeline pipeline = PipelineMother.passedPipelineInstance("mingle", "build", "unit");
        Stage firstStage = pipeline.getFirstStage();

        HttpOperationResult result = new HttpOperationResult();
        Stage stage = service.rerunJobs(firstStage, null, result);

        assertThat(stage, is(nullValue()));
        assertThat(result.httpCode(), is(400));
        assertThat(result.message(), is("No job was selected to re-run."));
    }

    private void assertScheduleFailure(String jobName, Stage oldStage, String failureMessage, int statusCode) {
        HttpOperationResult result = new HttpOperationResult();
        Stage stage = service.rerunJobs(oldStage, a(jobName), result);

        assertThat(stage, is(nullValue()));
        assertThat(result.httpCode(), is(statusCode));
        assertThat(result.message(), is(failureMessage));
    }

    private void stub(PipelineConfig mingleConfig, Pipeline pipeline, Stage lastStage) {
        StageIdentifier identifier = lastStage.getIdentifier();
        when(goConfigService.pipelineConfigNamed(new CaseInsensitiveString("mingle"))).thenReturn(mingleConfig);
        when(goConfigService.stageConfigNamed("mingle", identifier.getStageName())).thenReturn(mingleConfig.get(0));

        when(schedulingChecker.canSchedule(any(OperationResult.class))).thenReturn(true);
        when(schedulingChecker.canRerunStage(eq(pipeline.getIdentifier()), eq(lastStage.getName()), eq("anonymous"), any(OperationResult.class))).thenReturn(true);

        when(securityService.hasOperatePermissionForStage(eq("mingle"), eq(lastStage.getName()), any(String.class))).thenReturn(true);

        when(pipelineService.fullPipelineByCounterOrLabel("mingle", String.valueOf(identifier.getPipelineCounter()))).thenReturn(pipeline);
    }

}
