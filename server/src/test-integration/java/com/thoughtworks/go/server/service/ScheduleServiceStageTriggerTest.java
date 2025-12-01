/*
 * Copyright Thoughtworks, Inc.
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.TransactionStatus;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
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
    @Autowired private ChangesetService changesetService;
    @Autowired private TransactionSynchronizationManager transactionSynchronizationManager;
    @Autowired private GoCache goCache;

    private PipelineWithTwoStages pipelineFixture;
    private SchedulerFixture schedulerFixture;
    private final GoConfigFileHelper configHelper = new GoConfigFileHelper();

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws Exception {
        pipelineFixture = new PipelineWithTwoStages(materialRepository, transactionTemplate, tempDir);
        configHelper.usingCruiseConfigDao(goConfigDao);

        pipelineFixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
        schedulerFixture = new SchedulerFixture(dbHelper, stageDao, scheduleService);
    }

    @AfterEach
    public void teardown() throws Exception {
        pipelineFixture.onTearDown();
    }

    @Test
    public void shouldTriggerNextStageByHistoricalOrder() {
        // having a pipeline with two stages both are completed
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        // now we reorder the two stages via config from dev -> ft to ft -> dev
        reOrderTwoStages();

        // and then rerun the devstage
        scheduleService.rerunStage(pipeline, pipelineFixture.devStage(), "anyone");
        pipeline = pipelineService.mostRecentFullPipelineByName(pipelineFixture.pipelineName);
        dbHelper.passStage(pipeline.getFirstStage());
        Stage devStage = stageDao.mostRecentWithBuilds(pipelineFixture.pipelineName, pipelineFixture.devStage());
        Stage oldFtStage = stageDao.mostRecentWithBuilds(pipelineFixture.pipelineName, pipelineFixture.ftStage());

        // after devStage passes, it should automatically trigger the NEXT stage according to historical order
        // (ftStage), but NOT according to what is currently defined in the config file (none)
        scheduleService.automaticallyTriggerRelevantStagesFollowingCompletionOf(devStage);

        // verifying that ftStage is rerun
        Stage ftStage = stageDao.mostRecentWithBuilds(pipelineFixture.pipelineName, pipelineFixture.ftStage());
        assertThat(ftStage.getId() > oldFtStage.getId()).describedAs(String.format("Should schedule new ft stage: old id: %s, new id: %s",
            oldFtStage.getId(), ftStage.getId())).isTrue();
        assertThat(ftStage.getJobInstances().first().getState()).isEqualTo(JobState.Scheduled);
    }

    @Test
    public void shouldNotTriggerNextStageFromConfigIfItIsScheduled() {
        // having a pipeline with two stages both are completed
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        Stage oldDevStage = pipeline.getStages().byName(pipelineFixture.devStage);

        // now we reorder the two stages via config from dev -> ft to ft -> dev
        reOrderTwoStages();

        // and then rerun the ftstage
        schedulerFixture.rerunAndPassStage(pipeline, pipelineFixture.ftStage());

        // after ftStage passes, it should NOT trigger dev stage again otherwise this will
        // ends up in a deadlock
        Stage ftStage = stageDao.mostRecentWithBuilds(pipelineFixture.pipelineName, pipelineFixture.ftStage());
        scheduleService.automaticallyTriggerRelevantStagesFollowingCompletionOf(ftStage);

        // verifying that devStage is NOT rerun
        Stage devStage = stageDao.mostRecentWithBuilds(pipelineFixture.pipelineName, pipelineFixture.devStage());
        assertThat(devStage.getId()).isEqualTo(oldDevStage.getId());
    }

    @Test
    public void shouldNotRerunCurrentStageInNewerPipeline() {
        Pipeline olderPipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        Pipeline newerPipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        Stage oldFtStage = newerPipeline.getStages().byName(pipelineFixture.ftStage);

        schedulerFixture.rerunAndPassStage(olderPipeline, pipelineFixture.ftStage());
        Stage passedFtStage = pipelineService.fullPipelineById(olderPipeline.getId()).getStages().byName(
                pipelineFixture.ftStage);
        scheduleService.automaticallyTriggerRelevantStagesFollowingCompletionOf(passedFtStage);

        Stage ftStage = pipelineService.mostRecentFullPipelineByName(pipelineFixture.pipelineName).getStages().byName(
                pipelineFixture.ftStage);
        assertThat(ftStage.getId()).isEqualTo(oldFtStage.getId());
    }

    @Test
    public void cancelCurrentStageShouldTriggerSameStageInMostRecentPipeline() throws Exception {
        Pipeline oldest = pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageRunning();
        pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();
        pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();

        long cancelledStageId = oldest.getStages().byName(pipelineFixture.ftStage).getId();
        scheduleService.cancelAndTriggerRelevantStages(cancelledStageId, null, null);

        Pipeline mostRecent = pipelineService.mostRecentFullPipelineByName(pipelineFixture.pipelineName);
        Stage cancelledStage = stageService.stageById(cancelledStageId);
        assertThat(cancelledStage.stageState()).isEqualTo(StageState.Cancelled);
        assertThat(mostRecent.getStages().byName(pipelineFixture.ftStage).stageState()).isEqualTo(StageState.Building);
    }

    @Test
    public void errorInSchedulingSubsequentStageShouldNotRollbackCancelAction() throws Exception {
        Pipeline oldest = pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageRunning();
        pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();
        long cancelledStageId = oldest.getStages().byName(pipelineFixture.ftStage).getId();
        pipelineFixture.setRunOnAllAgentsForSecondStage();
        try {
            scheduleService.cancelAndTriggerRelevantStages(cancelledStageId, null, null);
            fail("Must have failed scheduling the next stage as it has run on all agents");
        } catch (CannotScheduleException expected) {
        }

        Pipeline mostRecent = pipelineService.mostRecentFullPipelineByName(pipelineFixture.pipelineName);
        Stage cancelledStage = stageService.stageById(cancelledStageId);
        assertThat(cancelledStage.stageState()).isEqualTo(StageState.Cancelled);
        assertThat(mostRecent.getStages().size()).isEqualTo(1);
    }


    @Test
    public void cancelCurrentStageShouldNotTriggerSameStageInMostRecentPipelineWhenItIsScheduledAlready()
                throws Exception {
        Pipeline oldest = pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageRunning();
        pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();
        pipelineFixture.createdPipelineWithAllStagesPassed();

        long cancelledStageId = oldest.getStages().byName(pipelineFixture.ftStage).getId();
        scheduleService.cancelAndTriggerRelevantStages(cancelledStageId, null, null);

        Pipeline mostRecent = pipelineService.mostRecentFullPipelineByName(pipelineFixture.pipelineName);
        Stage cancelledStage = stageService.stageById(cancelledStageId);
        assertThat(cancelledStage.stageState()).isEqualTo(StageState.Cancelled);
        assertThat(mostRecent.getStages().byName(pipelineFixture.ftStage).stageState()).isEqualTo(StageState.Passed);
    }

    @Test
    public void shouldDoCancellationInTransaction() throws Exception {
        Pipeline oldest = pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageRunning();
        pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();
        pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();

        Stage stage = oldest.getStages().byName(pipelineFixture.ftStage);

        StageStatusTopic stageStatusTopic = mock(StageStatusTopic.class);
        JobResultTopic jobResultTopic = mock(JobResultTopic.class);
        StageStatusListener stageStatusListener = mock(StageStatusListener.class);

        StageService stageService = mock(StageService.class);
        when(stageService.stageById(stage.getId())).thenReturn(stage);

        doAnswer(invocationOnMock -> {
            throw new RuntimeException();
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
        when(serverHealthService.logsSorted()).thenReturn(new ServerHealthStates());
        return new JobInstanceService(jobInstanceDao, jobResultTopic, jobStatusCache, transactionTemplate,
                transactionSynchronizationManager, null, null, goConfigService, null, serverHealthService);
    }

    @Test
    public void shouldNotNotifyListenersForWhenCancelStageTransactionRollsback() throws Exception {
        Pipeline oldest = pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageRunning();
        pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();
        pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();

        final Stage stage = oldest.getStages().byName(pipelineFixture.ftStage);

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
        Pipeline oldest = pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageRunning();
        pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();
        pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();

        Stage stage = oldest.getStages().byName(pipelineFixture.ftStage);

        StageIdentifier identifier = stage.getIdentifier();
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Stage cancelledStage = scheduleService.cancelAndTriggerRelevantStages(stage.getId(), null, result);

        Pipeline mostRecent = pipelineService.mostRecentFullPipelineByName(pipelineFixture.pipelineName);
        assertThat(cancelledStage.stageState()).isEqualTo(StageState.Cancelled);
        assertThat(mostRecent.getStages().byName(pipelineFixture.ftStage).stageState()).isEqualTo(StageState.Building);
        assertThat(result.message()).isEqualTo("Stage cancelled successfully.");
    }

    @Test
    public void shouldNotAllowManualTriggerIfPreviousStageFails() {
        Pipeline pipeline = pipelineFixture.createPipelineWithFirstStageFailedAndSecondStageHasNotStarted();
        StageConfig stageConfig = pipelineFixture.ftStage();
        configHelper.configureStageAsManualApproval(pipeline.getName(), stageConfig.name().toString(), true);

        Throwable exception = assertThrows(RuntimeException.class, () -> scheduleService.rerunStage(pipeline.getName(), 1, stageConfig.name().toString()));

        assertThat(exception.getMessage()).isEqualTo("Cannot schedule ft as the previous stage dev has Failed!");
    }

    private void reOrderTwoStages() {
        configHelper.removeStage(pipelineFixture.pipelineName, pipelineFixture.devStage);
        configHelper.addStageToPipeline(pipelineFixture.pipelineName, pipelineFixture.devStage);
    }
}
