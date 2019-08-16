/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.config.exceptions.NotAuthorizedException;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.activity.AgentAssignment;
import com.thoughtworks.go.domain.activity.JobStatusCache;
import com.thoughtworks.go.domain.activity.StageStatusCache;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.fixture.SchedulerFixture;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.StageStatusListener;
import com.thoughtworks.go.server.messaging.JobResultMessage;
import com.thoughtworks.go.server.messaging.JobResultTopic;
import com.thoughtworks.go.server.messaging.StageStatusMessage;
import com.thoughtworks.go.server.messaging.StageStatusTopic;
import com.thoughtworks.go.server.perf.SchedulingPerformanceLogger;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.transaction.TransactionCallback;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthStates;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml",
        "classpath:WEB-INF/spring-all-servlet.xml",
})
public class ScheduleServiceStageTriggerTest {
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private StageService stageService;
    @Autowired private PipelineService pipelineService;
    @Autowired private ScheduleService scheduleService;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private GoConfigService goConfigService;
    @Autowired private SchedulingCheckerService schedulingCheckerService;
    @Autowired private PipelineDao pipelineDao;
    @Autowired private StageDao stageDao;
    @Autowired private StageOrderService stageOrderService;
    @Autowired private SecurityService securityService;
    @Autowired private PipelineScheduleQueue pipelineScheduleQueue;
    @Autowired private JobInstanceService jobInstanceService;
    @Autowired private JobInstanceDao jobInstanceDao;
    @Autowired private AgentAssignment agentAssignment;
    @Autowired private EnvironmentConfigService environmentConfigService;
    @Autowired private PipelineLockService pipelineLockService;
    @Autowired private ServerHealthService serverHealthService;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private StageStatusCache stageStatusCache;
    @Autowired private JobStatusCache jobStatusCache;
    @Autowired private PropertiesService propertiesService;
    @Autowired private ChangesetService changesetService;
    @Autowired private TransactionSynchronizationManager transactionSynchronizationManager;
    @Autowired private GoCache goCache;
    @Autowired private PluginManager pluginManager;
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private PipelineWithTwoStages preCondition;
    private SchedulerFixture schedulerFixture;
    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();
    @Before
    public void setUp() throws Exception {
        preCondition = new PipelineWithTwoStages(materialRepository, transactionTemplate, temporaryFolder);
        configHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao);

        dbHelper.onSetUp();
        preCondition.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
        schedulerFixture = new SchedulerFixture(dbHelper, stageDao, scheduleService);
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        preCondition.onTearDown();
    }

    @Test
    public void shouldTriggerNextStageByHistoricalOrder() throws Exception {
        // having a pipeline with two stages both are completed
        Pipeline pipeline = preCondition.createdPipelineWithAllStagesPassed();
        // now we reorder the two stages via config from dev -> ft to ft -> dev
        reOrderTwoStages();

        // and then rerun the devstage
        scheduleService.rerunStage(pipeline, preCondition.devStage(), "anyone");
        pipeline = pipelineService.mostRecentFullPipelineByName(preCondition.pipelineName);
        dbHelper.passStage(pipeline.getFirstStage());
        Stage devStage = stageDao.mostRecentWithBuilds(preCondition.pipelineName, preCondition.devStage());
        Stage oldFtStage = stageDao.mostRecentWithBuilds(preCondition.pipelineName, preCondition.ftStage());

        // after devStage passes, it should automatically trigger the NEXT stage according to historical order
        // (ftStage), but NOT according to what is currently defined in the config file (none)
        scheduleService.automaticallyTriggerRelevantStagesFollowingCompletionOf(devStage);

        // verifying that ftStage is rerun
        Stage ftStage = stageDao.mostRecentWithBuilds(preCondition.pipelineName, preCondition.ftStage());
        assertThat(String.format("Should schedule new ft stage: old id: %s, new id: %s",
                oldFtStage.getId(), ftStage.getId()), ftStage.getId() > oldFtStage.getId(), is(true));
        assertThat(ftStage.getJobInstances().first().getState(), is(JobState.Scheduled));
    }

    @Test
    public void shouldNotTriggerNextStageFromConfigIfItIsScheduled() throws Exception {
        // having a pipeline with two stages both are completed
        Pipeline pipeline = preCondition.createdPipelineWithAllStagesPassed();
        Stage oldDevStage = pipeline.getStages().byName(preCondition.devStage);

        // now we reorder the two stages via config from dev -> ft to ft -> dev
        reOrderTwoStages();

        // and then rerun the ftstage
        schedulerFixture.rerunAndPassStage(pipeline, preCondition.ftStage());

        // after ftStage passes, it should NOT trigger dev stage again otherwise this will
        // ends up in a deadlock
        Stage ftStage = stageDao.mostRecentWithBuilds(preCondition.pipelineName, preCondition.ftStage());
        scheduleService.automaticallyTriggerRelevantStagesFollowingCompletionOf(ftStage);

        // verifying that devStage is NOT rerun
        Stage devStage = stageDao.mostRecentWithBuilds(preCondition.pipelineName, preCondition.devStage());
        assertThat("Should not schedule dev stage again", devStage.getId(), is(oldDevStage.getId()));
    }

    @Test
    public void shouldNotRerunCurrentStageInNewerPipeline() throws Exception {
        Pipeline olderPipeline = preCondition.createdPipelineWithAllStagesPassed();
        Pipeline newerPipeline = preCondition.createdPipelineWithAllStagesPassed();
        Stage oldFtStage = newerPipeline.getStages().byName(preCondition.ftStage);

        schedulerFixture.rerunAndPassStage(olderPipeline, preCondition.ftStage());
        Stage passedFtStage = pipelineService.fullPipelineById(olderPipeline.getId()).getStages().byName(
                preCondition.ftStage);
        scheduleService.automaticallyTriggerRelevantStagesFollowingCompletionOf(passedFtStage);

        Stage ftStage = pipelineService.mostRecentFullPipelineByName(preCondition.pipelineName).getStages().byName(
                preCondition.ftStage);
        assertThat("Should not rerun ft in newer pipeline", ftStage.getId(), is(oldFtStage.getId()));
    }

    @Test
    public void cancelCurrentStageShouldTriggerSameStageInMostRecentPipeline() throws Exception {
        Pipeline oldest = preCondition.createPipelineWithFirstStagePassedAndSecondStageRunning();
        preCondition.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();
        preCondition.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();

        long cancelledStageId = oldest.getStages().byName(preCondition.ftStage).getId();
        scheduleService.cancelAndTriggerRelevantStages(cancelledStageId, null, null);

        Pipeline mostRecent = pipelineService.mostRecentFullPipelineByName(preCondition.pipelineName);
        Stage cancelledStage = stageService.stageById(cancelledStageId);
        assertThat(cancelledStage.stageState(), is(StageState.Cancelled));
        assertThat(mostRecent.getStages().byName(preCondition.ftStage).stageState(), is(StageState.Building));
    }

    @Test
    public void errorInSchedulingSubsequentStageShouldNotRollbackCancelAction() throws Exception {
        Pipeline oldest = preCondition.createPipelineWithFirstStagePassedAndSecondStageRunning();
        preCondition.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();
        long cancelledStageId = oldest.getStages().byName(preCondition.ftStage).getId();
        preCondition.setRunOnAllAgentsForSecondStage();
        try {
            scheduleService.cancelAndTriggerRelevantStages(cancelledStageId, null, null);
            fail("Must have failed scheduling the next stage as it has run on all agents");
        } catch (CannotScheduleException expected) {
        }

        Pipeline mostRecent = pipelineService.mostRecentFullPipelineByName(preCondition.pipelineName);
        Stage cancelledStage = stageService.stageById(cancelledStageId);
        assertThat(cancelledStage.stageState(), is(StageState.Cancelled));
        assertThat(mostRecent.getStages().size(), is(1));
    }


    @Test
    public void cancelCurrentStageShouldNotTriggerSameStageInMostRecentPipelineWhenItIsScheduledAlready()
                throws Exception {
        Pipeline oldest = preCondition.createPipelineWithFirstStagePassedAndSecondStageRunning();
        preCondition.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();
        preCondition.createdPipelineWithAllStagesPassed();

        long cancelledStageId = oldest.getStages().byName(preCondition.ftStage).getId();
        scheduleService.cancelAndTriggerRelevantStages(cancelledStageId, null, null);

        Pipeline mostRecent = pipelineService.mostRecentFullPipelineByName(preCondition.pipelineName);
        Stage cancelledStage = stageService.stageById(cancelledStageId);
        assertThat(cancelledStage.stageState(), is(StageState.Cancelled));
        assertThat(mostRecent.getStages().byName(preCondition.ftStage).stageState(), is(StageState.Passed));
    }

    @Test
    public void shouldDoCancellationInTransaction() throws Exception {
        Pipeline oldest = preCondition.createPipelineWithFirstStagePassedAndSecondStageRunning();
        preCondition.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();
        preCondition.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();

        Stage stage = oldest.getStages().byName(preCondition.ftStage);

        StageStatusTopic stageStatusTopic = mock(StageStatusTopic.class);
        JobResultTopic jobResultTopic = mock(JobResultTopic.class);
        StageStatusListener stageStatusListener = mock(StageStatusListener.class);

        StageService stageService = mock(StageService.class);
        when(stageService.stageById(stage.getId())).thenReturn(stage);

        Mockito.doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                throw new RuntimeException();
            }
        }).when(stageService).cancelStage(stage, null);

        StageOrderService stageOrderService = mock(StageOrderService.class);
        SchedulingPerformanceLogger schedulingPerformanceLogger = mock(SchedulingPerformanceLogger.class);
        scheduleService = new ScheduleService(goConfigService, pipelineService, stageService, schedulingCheckerService, pipelineDao, stageDao,
                stageOrderService, securityService, pipelineScheduleQueue, this.jobInstanceService, jobInstanceDao, agentAssignment, environmentConfigService, pipelineLockService, serverHealthService,
                transactionTemplate, null, transactionSynchronizationManager, null, null, null, null, schedulingPerformanceLogger, null, null);

        try {
            scheduleService.cancelAndTriggerRelevantStages(stage.getId(), null, null);
        } catch (RuntimeException e) {
            //ignore
        }

        verify(stageStatusTopic, never()).post(any(StageStatusMessage.class));
        verify(jobResultTopic, never()).post(any(JobResultMessage.class));
        verify(stageStatusListener, never()).stageStatusChanged(any(Stage.class));
    }

    private JobInstanceService jobInstanceService(JobResultTopic jobResultTopic) {
        ServerHealthService serverHealthService = mock(ServerHealthService.class);
        when(serverHealthService.logs()).thenReturn(new ServerHealthStates());
        return new JobInstanceService(jobInstanceDao, propertiesService, jobResultTopic, jobStatusCache, transactionTemplate,
                transactionSynchronizationManager, null, null, goConfigService, null, serverHealthService);
    }

    @Test
    public void shouldNotNotifyListenersForWhenCancelStageTransactionRollsback() throws Exception {
        Pipeline oldest = preCondition.createPipelineWithFirstStagePassedAndSecondStageRunning();
        preCondition.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();
        preCondition.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();

        final Stage stage = oldest.getStages().byName(preCondition.ftStage);

        final StageIdentifier identifier = stage.getIdentifier();

        StageStatusTopic stageStatusTopic = mock(StageStatusTopic.class);
        JobResultTopic jobResultTopic = mock(JobResultTopic.class);
        StageStatusListener stageStatusListener = mock(StageStatusListener.class);

        JobInstanceService jobInstanceService = jobInstanceService(jobResultTopic);
        StageService stageService = new StageService(stageDao, jobInstanceService, stageStatusTopic, stageStatusCache, securityService, pipelineDao, changesetService, goConfigService,
                transactionTemplate,
                transactionSynchronizationManager, goCache, stageStatusListener);

        SchedulingPerformanceLogger schedulingPerformanceLogger = mock(SchedulingPerformanceLogger.class);
        scheduleService = new ScheduleService(goConfigService, pipelineService, stageService, schedulingCheckerService, pipelineDao, stageDao,
                stageOrderService, securityService, pipelineScheduleQueue, this.jobInstanceService, jobInstanceDao, agentAssignment, environmentConfigService, pipelineLockService, serverHealthService,
                transactionTemplate, null, transactionSynchronizationManager, null, null, null, null, schedulingPerformanceLogger, null, null);

        try {
            transactionTemplate.executeWithExceptionHandling(new TransactionCallback() {
                @Override public Object doInTransaction(TransactionStatus status) throws Exception {
                    scheduleService.cancelAndTriggerRelevantStages(stage.getId(), null, null);
                    throw new NotAuthorizedException("blah");
                }
            });
        } catch (Exception e) {
            //ignore
        }

        verify(stageStatusTopic, never()).post(any(StageStatusMessage.class));
        verify(jobResultTopic, never()).post(any(JobResultMessage.class));
        verify(stageStatusListener, never()).stageStatusChanged(any(Stage.class));
    }

    @Test
    public void shouldBeAbletoCancelStageByName() throws Exception {
        Pipeline oldest = preCondition.createPipelineWithFirstStagePassedAndSecondStageRunning();
        preCondition.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();
        preCondition.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();

        Stage stage = oldest.getStages().byName(preCondition.ftStage);

        StageIdentifier identifier = stage.getIdentifier();
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Stage cancelledStage = scheduleService.cancelAndTriggerRelevantStages(stage.getId(), null, result);

        Pipeline mostRecent = pipelineService.mostRecentFullPipelineByName(preCondition.pipelineName);
        assertThat(cancelledStage.stageState(), is(StageState.Cancelled));
        assertThat(mostRecent.getStages().byName(preCondition.ftStage).stageState(), is(StageState.Building));
        assertThat(result.message(), is("Stage cancelled successfully."));
    }

    @Test
    public void shouldNotAllowManualTriggerIfPreviousStageFails() {
        Pipeline pipeline = preCondition.createPipelineWithFirstStageFailedAndSecondStageHasNotStarted();
        StageConfig stageConfig = preCondition.ftStage();
        configHelper.configureStageAsManualApproval(pipeline.getName(), stageConfig.name().toString(), true);

        Throwable exception = assertThrows(RuntimeException.class, () -> {
            scheduleService.rerunStage(pipeline.getName(), 1, stageConfig.name().toString());
        });

        assertThat(exception.getClass(), is(RuntimeException.class));
        assertThat(exception.getMessage(), is("Cannot schedule ft as the previous stage dev has Failed!"));
    }

    private void reOrderTwoStages() throws Exception {
        configHelper.removeStage(preCondition.pipelineName, preCondition.devStage);
        configHelper.addStageToPipeline(preCondition.pipelineName, preCondition.devStage);
    }
}
