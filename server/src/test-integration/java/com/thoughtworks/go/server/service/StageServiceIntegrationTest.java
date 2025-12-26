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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.feed.Author;
import com.thoughtworks.go.domain.feed.FeedEntries;
import com.thoughtworks.go.domain.feed.stage.StageFeedEntry;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.dto.DurationBean;
import com.thoughtworks.go.fixture.PipelineWithMultipleStages;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.presentation.pipelinehistory.StageHistoryEntry;
import com.thoughtworks.go.presentation.pipelinehistory.StageHistoryPage;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.StageIdentity;
import com.thoughtworks.go.server.domain.StageStatusListener;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.materials.DependencyMaterialUpdateNotifier;
import com.thoughtworks.go.server.messaging.JobResultTopic;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.scheduling.ScheduleHelper;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.ui.StageSummaryModels;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.thoughtworks.go.domain.JobResult.Passed;
import static com.thoughtworks.go.helper.BuildPlanMother.withBuildPlans;
import static com.thoughtworks.go.helper.JobInstanceMother.building;
import static com.thoughtworks.go.helper.JobInstanceMother.completed;
import static com.thoughtworks.go.helper.ModificationsMother.checkinWithComment;
import static com.thoughtworks.go.helper.ModificationsMother.modifyOneFile;
import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
    "classpath:/applicationContext-global.xml",
    "classpath:/applicationContext-dataLocalAccess.xml",
    "classpath:/testPropertyConfigurer.xml",
    "classpath:/spring-all-servlet.xml",
})
public class StageServiceIntegrationTest {
    @Autowired private JobInstanceDao jobInstanceDao;
    @Autowired private StageService stageService;
    @Autowired private AgentService agentService;
    @Autowired private StageDao stageDao;
    @Autowired private PipelineSqlMapDao pipelineDao;
	@Autowired private DatabaseAccessHelper dbHelper;
	@Autowired private ScheduleHelper scheduleHelper;
    @Autowired private ScheduleService scheduleService;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private JobResultTopic jobResultTopic;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private TransactionSynchronizationManager transactionSynchronizationManager;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private ConfigDbStateRepository configDbStateRepository;
    @Autowired private GoConfigService goConfigService;
    @Autowired private ChangesetService changesetService;
    @Autowired private GoCache goCache;
    @Autowired private InstanceFactory instanceFactory;
    @Autowired private DependencyMaterialUpdateNotifier notifier;

    private static final String PIPELINE_NAME = "mingle";
    private static final String STAGE_NAME = "dev";
    private JobInstance job;
    private static final String UUID = "uuid";
    private Pipeline savedPipeline;
    private PipelineConfig pipelineConfig;
    private Stage stage;
    private final GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private PipelineWithMultipleStages pipelineFixture;
    private String md5 = "md5-test";
    private JobState receivedState;
    private JobResult receivedResult;
    private StageResult receivedStageResult;

    @BeforeEach
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        pipelineConfig = PipelineMother.withSingleStageWithMaterials(PIPELINE_NAME, STAGE_NAME, withBuildPlans("unit", "dev", "blah"));
        pipelineConfig.getFirstStageConfig().setFetchMaterials(false);
        pipelineConfig.getFirstStageConfig().setCleanWorkingDir(true);
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        configHelper.addPipeline(PIPELINE_NAME, STAGE_NAME);
        savedPipeline = scheduleHelper.schedule(pipelineConfig, BuildCause.createWithModifications(modifyOneFile(pipelineConfig), ""), GoConstants.DEFAULT_APPROVED_BY);
        stage = savedPipeline.getStages().first();
        job = stage.getJobInstances().first();
        job.setAgentUuid(UUID);
        jobInstanceDao.updateAssignedInfo(job);
        AgentIdentifier agentIdentifier = new AgentIdentifier("localhost", "127.0.0.1", UUID);
        agentService.updateRuntimeInfo(new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"));
        receivedState = null;
        receivedResult = null;
        receivedStageResult = null;
        notifier.disableUpdates();
    }

    @AfterEach
    public void teardown() throws Exception {
        if (pipelineFixture != null) {
            pipelineFixture.onTearDown();
        }
        dbHelper.onTearDown();
        configHelper.onTearDown();
        notifier.enableUpdates();
    }

    @Test
    public void shouldPostAllMessagesAfterTheDatabaseIsUpdatedWhenCancellingAStage() {
        jobResultTopic.addListener(message -> {
            JobIdentifier jobIdentifier = message.getJobIdentifier();
            JobInstance instance = jobInstanceDao.mostRecentJobWithTransitions(jobIdentifier);
            receivedState = instance.getState();
            receivedResult = instance.getResult();
        });
        stageService.addStageStatusListener(stage -> {
            Stage retrievedStage = stageDao.stageById(stage.getId());
            receivedStageResult = retrievedStage.getResult();
        });
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                stageService.cancelStage(stage, null);
            }
        });

        await().atMost(10, TimeUnit.SECONDS).until(() -> receivedResult != null && receivedState != null && receivedStageResult != null);
        assertThat(receivedState).isEqualTo(JobState.Completed);
        assertThat(receivedResult).isEqualTo(JobResult.Cancelled);
        assertThat(receivedStageResult).isEqualTo(StageResult.Cancelled);
    }

    @Test
    public void shouldReturnFalseWhenAllStagesAreCompletedInAGivenPipeline(@TempDir Path tempDir) throws Exception {
        pipelineFixture = new PipelineWithMultipleStages(4, materialRepository, transactionTemplate, tempDir);
        pipelineFixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        assertThat(stageService.isAnyStageActiveForPipeline(pipeline.getIdentifier())).isFalse();
    }

    @Test
    public void shouldReturnTrueIfAnyStageIsBuildingInAGivenPipeline(@TempDir Path tempDir) throws Exception {
        pipelineFixture = new PipelineWithMultipleStages(4, materialRepository, transactionTemplate, tempDir);
        pipelineFixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();

        Pipeline pipeline = pipelineFixture.createPipelineWithFirstStageAssigned();
        assertThat(stageService.isAnyStageActiveForPipeline(pipeline.getIdentifier())).isTrue();
    }

    @Test
    public void testShouldReturnTrueIfAStageOfAPipelineHasBeenScheduled(@TempDir Path tempDir) throws Exception {
        pipelineFixture = new PipelineWithMultipleStages(3, materialRepository, transactionTemplate, tempDir);
        pipelineFixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
        Pipeline pipeline = pipelineFixture.createPipelineWithFirstStageScheduled();
        assertThat(stageService.isAnyStageActiveForPipeline(pipeline.getIdentifier())).isTrue();
    }

    @Test
    public void shouldReturnStageWithSpecificCounter() {
        Stage firstStage = savedPipeline.getStages().first();
        Stage newInstance = instanceFactory.createStageInstance(pipelineConfig.first(), new DefaultSchedulingContext(
            "anonymous"), md5, new TimeProvider());
        Stage newSavedStage = stageService.save(savedPipeline, newInstance);

        Stage latestStage = stageService.findStageWithIdentifier(
            new StageIdentifier(CaseInsensitiveString.str(pipelineConfig.name()), savedPipeline.getCounter(), savedPipeline.getLabel(), firstStage.getName(), String.valueOf(newSavedStage.getCounter())));
        assertThat(latestStage).isEqualTo(newSavedStage);

    }

    @Test
    public void shouldReturnStageWithSpecificCounter_findStageWithIdentifier() {
        Stage newInstance = instanceFactory.createStageInstance(pipelineConfig.first(), new DefaultSchedulingContext("anonymous"), md5, new TimeProvider());
        Stage newSavedStage = stageService.save(savedPipeline, newInstance);

        StageIdentifier identifier = newSavedStage.getIdentifier();
        Stage latestStage = stageService.findStageWithIdentifier(identifier.getPipelineName(), identifier.getPipelineCounter(), identifier.getStageName(), identifier.getStageCounter(), "admin", new HttpOperationResult());

        assertThat(latestStage).isEqualTo(newSavedStage);
    }

    @Test
    public void shouldReturnTrueIfStageIsActive() {
        savedPipeline.getStages().first();
        Stage newInstance = instanceFactory.createStageInstance(pipelineConfig.first(), new DefaultSchedulingContext("anonymous"), md5, new TimeProvider());
        stageService.save(savedPipeline, newInstance);

        boolean stageActive = stageService.isStageActive(CaseInsensitiveString.str(pipelineConfig.name()), CaseInsensitiveString.str(pipelineConfig.first().name()));
        assertThat(stageActive).isTrue();
    }

    @Test
    public void shouldSaveStageWithFetchMaterialsFlag() {
        Stage firstStage = savedPipeline.getStages().first();
        Stage savedStage = stageService.stageById(firstStage.getId());
        assertThat(savedStage.shouldFetchMaterials()).isFalse();
    }

    @Test
    public void shouldSaveStageWithCleanWorkingDirFlag() {
        Stage firstStage = savedPipeline.getStages().first();
        Stage savedStage = stageService.stageById(firstStage.getId());
        assertThat(savedStage.shouldCleanWorkingDir()).isTrue();
    }

    @Test
    public void shouldGetStageHistoryForCurrentPage() {

        StageHistoryEntry[] stages = createFiveStages();

        StageHistoryPage history = stageService.findStageHistoryPage(stageService.stageById(stages[2].getId()), 3);
        assertThat(history.getPagination()).isEqualTo(Pagination.pageByOffset(0, 5, 3));
        assertThat(history.getStages().size()).isEqualTo(3);
        assertThat(history.getStages().get(0)).isEqualTo(stages[4]);
        assertThat(history.getStages().get(1)).isEqualTo(stages[3]);
        assertThat(history.getStages().get(2)).isEqualTo(stages[2]);

    }

    @Test
    public void shouldGetStageHistoryForLastPage() {

        StageHistoryEntry[] stages = createFiveStages();

        StageHistoryPage history = stageService.findStageHistoryPage(stageService.stageById(stages[0].getId()), 3);
        assertThat(history.getStages().get(0)).isEqualTo(stages[1]);
        assertThat(history.getStages().get(1)).isEqualTo(stages[0]);
        assertThat(history.getPagination()).isEqualTo(Pagination.pageByOffset(3, 5, 3));
        assertThat(history.getPagination().getCurrentPage()).isEqualTo(2);
    }

    private StageHistoryEntry[] createFiveStages() {
        Stage[] stages = new Stage[5];
        stages[0] = savedPipeline.getFirstStage();
        for (int i = 1; i < stages.length; i++) {
            DefaultSchedulingContext ctx = new DefaultSchedulingContext("anonumous");
            StageConfig stageCfg = pipelineConfig.first();
            stages[i] = i % 2 == 0 ? instanceFactory.createStageInstance(stageCfg, ctx, md5, new TimeProvider()) : instanceFactory.createStageForRerunOfJobs(stages[i - 1], List.of("unit", "blah"), ctx,
                stageCfg, new TimeProvider(), "md5");
            stageService.save(savedPipeline, stages[i]);
        }
        StageHistoryEntry[] entries = new StageHistoryEntry[5];
        for (int i = 0; i < stages.length; i++) {
            entries[i] = new StageHistoryEntry(stages[i], pipelineDao.loadHistory(savedPipeline.getId()).getNaturalOrder(), stages[i].getRerunOfCounter());
        }
        return entries;
    }

    @Test
    public void shouldGetStageHistoryByPageNumber() {
        StageHistoryEntry[] stages = createFiveStages();

        StageHistoryPage history = stageService.findStageHistoryPageByNumber(PIPELINE_NAME, STAGE_NAME, 2, 3);
        assertThat(history.getPagination()).isEqualTo(Pagination.pageByOffset(3, 5, 3));
        assertThat(history.getStages().size()).isEqualTo(2);
        assertThat(history.getStages().get(0)).isEqualTo(stages[1]);
        assertThat(history.getStages().get(1)).isEqualTo(stages[0]);
    }

    @Test
    public void shouldSaveStageWithStateBuilding() {
        Stage stage = instanceFactory.createStageInstance(pipelineConfig.first(), new DefaultSchedulingContext("anonumous"), md5, new TimeProvider());
        stageService.save(savedPipeline, stage);
        Stage latestStage = stageService.findLatestStage(CaseInsensitiveString.str(pipelineConfig.name()), CaseInsensitiveString.str(pipelineConfig.first().name()));
        assertThat(latestStage.getState()).isEqualTo(StageState.Building);
    }

    @Test
    public void shouldIgnoreErrorsWhenNotifyingListenersDuringSave() {
        List<StageStatusListener> original = new ArrayList<>(stageService.getStageStatusListeners());

        try {
            stageService.getStageStatusListeners().clear();
            StageStatusListener failingListener = mock(StageStatusListener.class);
            doThrow(new RuntimeException("Should not be rethrown by save")).when(failingListener).stageStatusChanged(any());
            StageStatusListener passingListener = mock(StageStatusListener.class);
            stageService.getStageStatusListeners().add(failingListener);
            stageService.getStageStatusListeners().add(passingListener);
            Stage newInstance = instanceFactory.createStageInstance(pipelineConfig.first(), new DefaultSchedulingContext("anonumous"), md5, new TimeProvider());
            Stage savedStage = stageService.save(savedPipeline, newInstance);
            assertThat(savedStage.getId()).isGreaterThan(0L);
            verify(passingListener).stageStatusChanged(any());
        } finally {
            stageService.getStageStatusListeners().clear();
            stageService.getStageStatusListeners().addAll(original);
        }
    }

    @Test
    public void shouldNotifyListenersWhenStageScheduled() {
        StageStatusListener listener = mock(StageStatusListener.class);
        stageService.addStageStatusListener(listener);

        Stage newInstance = instanceFactory.createStageInstance(pipelineConfig.first(),
            new DefaultSchedulingContext("anonymous"), md5, new TimeProvider());
        Stage savedStage = stageService.save(savedPipeline, newInstance);

        verify(listener).stageStatusChanged(savedStage);
    }

    @Test
    public void shouldNotifyListenersAfterStageIsCancelled() {
        StageStatusListener listener = mock(StageStatusListener.class);
        stageService.addStageStatusListener(listener);

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                stageService.cancelStage(stage, null);
            }
        });
        stage = stageService.stageById(stage.getId());

        verify(listener).stageStatusChanged(stage);
    }

    @Test
    public void shouldLookupModifiedStageById_afterCancel() {
        Stage stageLoadedBeforeCancellation = stageService.stageById(stage.getId());
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                stageService.cancelStage(stage, null);
            }
        });

        assertThat(stageService.stageById(stage.getId())).isNotEqualTo(stageLoadedBeforeCancellation);
    }

    @Test
    public void shouldSetCancelledByWhileCancellingAStage() {
        Stage stageLoadedBeforeCancellation = stageService.stageById(stage.getId());
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                stageService.cancelStage(stage, "foo");
            }
        });

        Stage afterCancel = stageService.stageById(stage.getId());
        assertThat(afterCancel.getCancelledBy()).isEqualTo("foo");
        assertNull(stageLoadedBeforeCancellation.getCancelledBy());
    }

    @Test
    public void shouldLookupModifiedStageById_afterJobUpdate() {
        Stage stageLoadedBeforeUpdate = stageService.stageById(stage.getId());
        dbHelper.completeAllJobs(stage, JobResult.Passed);
        assertThat(stageService.stageById(stage.getId())).isEqualTo(stageLoadedBeforeUpdate);
        stageService.updateResult(stage);

        assertThat(stageService.stageById(stage.getId())).isNotEqualTo(stageLoadedBeforeUpdate);
    }

    @Test
    public void shouldNotCancelAlreadyCompletedBuild() {
        jobResultTopic.addListener(message -> {
            JobIdentifier jobIdentifier = message.getJobIdentifier();
            JobInstance instance = jobInstanceDao.mostRecentJobWithTransitions(jobIdentifier);
            receivedState = instance.getState();
            receivedResult = instance.getResult();
        });
        JobInstanceMother.completed(job, Passed, new Date());
        jobInstanceDao.updateStateAndResult(job);
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                stageService.cancelStage(stage, null);
            }
        });

        JobInstance instanceFromDB = jobInstanceDao.buildByIdWithTransitions(job.getId());
        assertThat(instanceFromDB.getState()).isEqualTo(JobState.Completed);
        assertThat(instanceFromDB.getResult()).isEqualTo(Passed);
        assertThat(agentService.findAgentAndRefreshStatus(UUID).isCancelled()).isFalse();
        assertThat(receivedState).isNull();
        assertThat(receivedResult).isNull();
    }

    @Test
    // #2328
    public void shouldGetDurationBasedOnPipelineNameStageNameJobNameAndAgentUUID() {
        String pipelineName = "Cruise";
        configHelper.addPipeline(pipelineName, STAGE_NAME);
        Stage saveStage = dbHelper.saveTestPipeline(pipelineName, STAGE_NAME).getStages().first();
        JobInstance job1 = completed("unit", Passed, new Date(), Dates.from(ZonedDateTime.now().minusMinutes(1)));
        job1.setAgentUuid(UUID);

        jobInstanceDao.save(saveStage.getId(), job1);

        String pipeline2Name = "Cruise-1.1";
        configHelper.addPipeline(pipeline2Name, STAGE_NAME);
        Stage stage11 = dbHelper.saveTestPipeline(pipeline2Name, STAGE_NAME).getStages().first();

        final JobInstance job2 = building("unit", new Date());
        job2.setAgentUuid(UUID);
        JobInstance buildingJob = jobInstanceDao.save(stage11.getId(), job2);

        final DurationBean duration = stageService.getBuildDuration("Cruise-1.1", STAGE_NAME, buildingJob);
        assertThat(duration.getDuration())
            .describedAs("we should not load duration according to stage name + job name + agent uuid only, "
                + "we should also use pipeline name as a parameter")
            .isEqualTo(0L);
    }

    @Test
    public void shouldNotifyListenerOnStageStatusChange() {
        StageStatusListener listener = mock(StageStatusListener.class);

        stageService.addStageStatusListener(listener);
        stageService.updateResult(stage);

        verify(listener).stageStatusChanged(stage);
    }


    @Test
    public void ensurePipelineSqlMapDaoIsAStageStatusListener() {
        assertThat(stageService.getStageStatusListeners()).contains(pipelineDao);
    }

    @Test
    public void shouldNotNotifyListenersWhenJobCancellationTransactionRollsback() {
        StageStatusListener listener = mock(StageStatusListener.class);
        JobInstanceService jobInstanceService = mock(JobInstanceService.class);
        JobInstance job = JobInstanceMother.building("foo");
        doThrow(new RuntimeException("test exception")).when(jobInstanceService).cancelJob(job);
        JobIdentifier jobId = new JobIdentifier("pipeline", 10, "label-10", "stage", "1", "foo");
        job.setIdentifier(jobId);
        StageDao stageDao = mock(StageDao.class);
        Stage stage = StageMother.custom("stage");
        when(stageDao.findStageWithIdentifier(jobId.getStageIdentifier())).thenReturn(stage);
        StageService service = new StageService(stageDao, jobInstanceService, null, null, null, null, changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager,
            goCache, listener);
        try {
            service.cancelJob(job);
            fail("should have thrown up when underlying service bombed");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("test exception");
        }
        verify(listener, never()).stageStatusChanged(any());
    }

    @Test
    public void shouldNotifyListenersWhenJobCancelledSuccessfully() {
        StageStatusListener listener = mock(StageStatusListener.class);
        JobInstanceService jobInstanceService = mock(JobInstanceService.class);
        JobInstance job = JobInstanceMother.building("foo");
        JobIdentifier jobId = new JobIdentifier("pipeline", 10, "label-10", "stage", "1", "foo");
        job.setIdentifier(jobId);
        StageDao stageDao = mock(StageDao.class);
        Stage stage = StageMother.custom("stage");
        when(stageDao.findStageWithIdentifier(jobId.getStageIdentifier())).thenReturn(stage);
        StageService service = new StageService(stageDao, jobInstanceService, null, null, null, null, changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager,
            goCache, listener);
        service.cancelJob(job);
        verify(listener).stageStatusChanged(stage);
    }

    @Test
    public void shouldLoadStagesHavingArtifactsWhenStageIsNotCleanupProtected() {
        PipelineConfig pipelineConfig = configHelper.addPipeline("pipeline-1", "stage-1", "job-1");

        Pipeline completed = dbHelper.schedulePipelineWithAllStages(pipelineConfig, ModificationsMother.modifySomeFiles(pipelineConfig));
        dbHelper.pass(completed);
        List<Stage> stages = stageService.oldestStagesWithDeletableArtifacts();
        assertThat(stages.size()).isEqualTo(1);

        CruiseConfig cruiseConfig = configHelper.currentConfig();
        pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline-1"));
        ReflectionUtil.setField(pipelineConfig.get(0), "artifactCleanupProhibited", true);
        configHelper.writeConfigFile(cruiseConfig);

        configDbStateRepository.flushConfigState();

        stages = stageService.oldestStagesWithDeletableArtifacts();
        assertThat(stages.size()).isEqualTo(0);
    }

    @Test
    public void shouldLoadPageOfOldestStagesHavingArtifacts() {
        CruiseConfig cruiseConfig = configHelper.currentConfig();
        PipelineConfig mingleConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(PIPELINE_NAME));
        ReflectionUtil.setField(mingleConfig.get(0), "artifactCleanupProhibited", true);
        configHelper.writeConfigFile(cruiseConfig);

        configDbStateRepository.flushConfigState();

        Pipeline[] pipelines = new Pipeline[101];
        for (int i = 0; i < 101; i++) {
            PipelineConfig pipelineCfg = configHelper.addPipeline("pipeline-" + i, "stage", "job");
            Pipeline pipeline = dbHelper.schedulePipeline(pipelineCfg, new TimeProvider());
            dbHelper.pass(pipeline);
            pipelines[i] = pipeline;
        }

        List<Stage> stages = stageService.oldestStagesWithDeletableArtifacts();
        for (int i = 0; i < 100; i++) {
            Stage stage = stages.get(i);
            assertThat(stage.getIdentifier()).isEqualTo(pipelines[i].getFirstStage().getIdentifier());
            stageService.markArtifactsDeletedFor(stage);
        }
        assertThat(stages.size()).isEqualTo(100);

        stages = stageService.oldestStagesWithDeletableArtifacts();
        assertThat(stages.size()).isEqualTo(1);
        Stage stage = stages.get(0);
        assertThat(stage.getIdentifier()).isEqualTo(pipelines[100].getFirstStage().getIdentifier());
        stageService.markArtifactsDeletedFor(stage);

        assertThat(stageService.oldestStagesWithDeletableArtifacts().size()).isEqualTo(0);
    }

    @Test
    public void findStageHistoryForChart_shouldFindLatestStageInstancesForChart() {
        PipelineConfig pipelineConfig = configHelper.addPipeline("pipeline-1", "stage-1");
        configHelper.turnOffSecurity();
        Pipeline pipeline;
        for (int i = 0; i < 16; i++) {
            pipeline = dbHelper.schedulePipelineWithAllStages(pipelineConfig, ModificationsMother.modifySomeFiles(pipelineConfig));
            dbHelper.pass(pipeline);
        }
        StageSummaryModels stages = stageService.findStageHistoryForChart(pipelineConfig.name().toString(), pipelineConfig.first().name().toString(), 1, 4);
        assertThat(stages.size()).isEqualTo(4);
        assertThat(stages.get(0).getIdentifier().getPipelineCounter()).isEqualTo(16);
        stages = stageService.findStageHistoryForChart(pipelineConfig.name().toString(), pipelineConfig.first().name().toString(), 3, 4);
        assertThat(stages.size()).isEqualTo(4);
        assertThat(stages.get(0).getIdentifier().getPipelineCounter()).isEqualTo(8);
        assertThat(stages.getPagination().getTotalPages()).isEqualTo(4);
    }

    @Test
    public void findStageHistoryForChart_shouldNotRetrieveCancelledStagesAndStagesWithRerunJobs() {
        PipelineConfig pipelineConfig = configHelper.addPipeline("pipeline-1", "stage-1");
        configHelper.turnOffSecurity();
        Pipeline pipeline = dbHelper.schedulePipelineWithAllStages(pipelineConfig, ModificationsMother.modifySomeFiles(pipelineConfig));
        dbHelper.pass(pipeline);
        StageSummaryModels stages = stageService.findStageHistoryForChart(pipelineConfig.name().toString(), pipelineConfig.first().name().toString(), 1, 10);
        assertThat(stages.size()).isEqualTo(1);

        scheduleService.rerunJobs(pipeline.getFirstStage(), List.of(CaseInsensitiveString.str(pipelineConfig.first().getJobs().first().name())), new HttpOperationResult());
        stages = stageService.findStageHistoryForChart(pipelineConfig.name().toString(), pipelineConfig.first().name().toString(), 1, 10);

        assertThat(stages.size()).isEqualTo(1); //should not retrieve stages with rerun jobs

        pipeline = dbHelper.schedulePipelineWithAllStages(pipelineConfig, ModificationsMother.modifySomeFiles(pipelineConfig));
        dbHelper.cancelStage(pipeline.getFirstStage());
        stages = stageService.findStageHistoryForChart(pipelineConfig.name().toString(), pipelineConfig.first().name().toString(), 1, 10);

        assertThat(stages.size()).isEqualTo(1); //should not retrieve cancelled stages
    }

    @Test
    public void shouldSaveTheStageStatusProperlyUponJobCancelAfterInvalidatingTheCache(@TempDir Path tempDir) throws Exception {
        pipelineFixture = (PipelineWithMultipleStages) new PipelineWithMultipleStages(2, materialRepository, transactionTemplate, tempDir).usingTwoJobs();
        pipelineFixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();

        Pipeline pipeline = pipelineFixture.createPipelineWithFirstStageAssigned();
        Stage currentStage = pipeline.getFirstStage();
        JobInstances jobs = currentStage.getJobInstances();

        JobInstance firstJob = jobs.first();
        firstJob.completing(JobResult.Passed);
        firstJob.completed(new Date());
        jobInstanceDao.updateStateAndResult(firstJob);

        // prime the cache
        stageService.findStageWithIdentifier(new StageIdentifier(pipeline, currentStage));

        stageService.cancelJob(jobs.last());

        Stage savedStage = stageService.stageById(currentStage.getId());
        assertThat(savedStage.getState()).isEqualTo(StageState.Cancelled);
        assertThat(savedStage.getResult()).isEqualTo(StageResult.Cancelled);
    }

    @Test
    public void shouldNotLoadStageAuthors_fromUpstreamInvisibleToUser() {
        PipelineConfig downstream = setup2DependentInstances();

        configHelper.enableSecurity();
        configHelper.addAdmins("super-hero");

        configHelper.addAuthorizedUserForPipelineGroup("loser", "upstream-without-mingle");
        configHelper.addAuthorizedUserForPipelineGroup("loser", "downstream");
        configHelper.addAuthorizedUserForPipelineGroup("boozer", "upstream-with-mingle");

        FeedEntries feed = stageService.feed(downstream.name().toString(), new Username(new CaseInsensitiveString("loser")));

        assertAuthorsOnEntry((StageFeedEntry) feed.get(0),
            List.of(new Author("svn 3 guy", "svn.3@gmail.com"),
                new Author("p4 2 guy", "p4.2@gmail.com")));

        assertAuthorsOnEntry((StageFeedEntry) feed.get(1),
            List.of(new Author("svn 1 guy", "svn.1@gmail.com"),
                new Author("svn 2 guy", "svn.2@gmail.com"),
                new Author("p4 1 guy", "p4.1@gmail.com")));
    }

    @Test
    public void shouldLoadStageAuthors_forFirstPageOfFeed() {
        PipelineConfig downstream = setup2DependentInstances();

        FeedEntries feed = stageService.feed(downstream.name().toString(), new Username(new CaseInsensitiveString("loser")));

        assertStageEntryAuthor(feed);
    }

    @Test
    public void shouldLoadStageAuthors_forSubsequentPages() {
        PipelineConfig downstream = setup2DependentInstances();

        FeedEntries feed = stageService.feedBefore(Integer.MAX_VALUE, downstream.name().toString(), new Username(new CaseInsensitiveString("loser")));

        assertStageEntryAuthor(feed);
    }

    @Test
    public void shouldFetchLatestStageInstanceForEachStage() {
        setup2DependentInstances();
        List<StageIdentity> latestStageInstances = stageService.findLatestStageInstances();
        assertThat(latestStageInstances.size()).isEqualTo(4);
        assertThat(latestStageInstances.contains(new StageIdentity("mingle", "dev", 8L))).isTrue();
        assertThat(latestStageInstances.contains(new StageIdentity("upstream-without-mingle", "stage", 13L))).isTrue();
        assertThat(latestStageInstances.contains(new StageIdentity("downstream", "down-stage", 14L))).isTrue();
        assertThat(latestStageInstances.contains(new StageIdentity("upstream-with-mingle", "stage", 10L))).isTrue();
    }

    @Test
    public void testShouldReturnTrueIfAStageIsActive_CaseInsensitive(@TempDir Path tempDir) throws Exception {
        pipelineFixture = new PipelineWithMultipleStages(4, materialRepository, transactionTemplate, tempDir);
        pipelineFixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
        Pipeline pipeline = pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageRunning();
        assertThat(stageService.isStageActive(pipeline.getName().toUpperCase(), "FT")).isTrue();
    }

    @Test
    public void shouldReturnTheLatestAndOldestStageInstanceId() {
        StageHistoryEntry[] stages = createFiveStages();

        PipelineRunIdInfo oldestAndLatestPipelineId = stageService.getOldestAndLatestStageInstanceId(new Username(new CaseInsensitiveString("admin1")), savedPipeline.getName(), savedPipeline.getFirstStage().getName());

        assertThat(oldestAndLatestPipelineId.getLatestRunId()).isEqualTo(stages[4].getId());
        assertThat(oldestAndLatestPipelineId.getOldestRunId()).isEqualTo(stages[0].getId());
    }

    @Test
    public void shouldReturnLatestPipelineHistory() {
        StageHistoryEntry[] stages = createFiveStages();

        StageInstanceModels history = stageService.findStageHistoryViaCursor(new Username(new CaseInsensitiveString("admin1")), savedPipeline.getName(), savedPipeline.getFirstStage().getName(), 0, 0, 10);

        assertThat(history.size()).isEqualTo(5);
        assertThat(history.get(0).getId()).isEqualTo(stages[4].getId());
        assertThat(history.get(4).getId()).isEqualTo(stages[0].getId());
    }

    @Test
    public void shouldReturnThePipelineHistoryAfterTheSpecifiedCursor() {
        StageHistoryEntry[] stages = createFiveStages();

        StageInstanceModels history = stageService.findStageHistoryViaCursor(new Username(new CaseInsensitiveString("admin1")), savedPipeline.getName(), savedPipeline.getFirstStage().getName(), stages[2].getId(), 0, 10);

        assertThat(history.size()).isEqualTo(2);
        assertThat(history.get(0).getId()).isEqualTo(stages[1].getId());
        assertThat(history.get(1).getId()).isEqualTo(stages[0].getId());
    }

    @Test
    public void shouldReturnThePipelineHistoryBeforeTheSpecifiedCursor() {
        StageHistoryEntry[] stages = createFiveStages();

        StageInstanceModels history = stageService.findStageHistoryViaCursor(new Username(new CaseInsensitiveString("admin1")), savedPipeline.getName(), savedPipeline.getFirstStage().getName(), 0, stages[2].getId(), 10);

        assertThat(history.size()).isEqualTo(2);
        assertThat(history.get(0).getId()).isEqualTo(stages[4].getId());
        assertThat(history.get(1).getId()).isEqualTo(stages[3].getId());
    }

    private void assertStageEntryAuthor(FeedEntries feed) {

        assertAuthorsOnEntry((StageFeedEntry) feed.get(0),
            List.of(new Author("hg 3 guy", "hg.3@gmail.com"),
                new Author("git 2&3 guy", "git.2.and.3@gmail.com"),
                new Author("svn 3 guy", "svn.3@gmail.com"),
                new Author("p4 2 guy", "p4.2@gmail.com")));

        assertAuthorsOnEntry((StageFeedEntry) feed.get(1),
            List.of(new Author("hg 1 guy", "hg.1@gmail.com"),
                new Author("hg 2 guy", null),
                new Author("git 1 guy", "git.1@gmail.com"),
                new Author("svn 1 guy", "svn.1@gmail.com"),
                new Author("svn 2 guy", "svn.2@gmail.com"),
                new Author("p4 1 guy", "p4.1@gmail.com")));
    }

    private void assertAuthorsOnEntry(StageFeedEntry stage2, List<Author> authors) {
        List<Author> stage2Authors = stage2.getAuthors();

        assertThat(stage2Authors).contains(authors.toArray(new Author[0]));

        assertThat(stage2Authors.size()).isEqualTo(authors.size());

    }

    private PipelineConfig setup2DependentInstances() {
        Username loser = new Username(new CaseInsensitiveString("loser"));
        ManualBuild build = new ManualBuild(loser);
        Date checkinTime = new Date();

        GitMaterial git = MaterialsMother.gitMaterial("http://google.com", null, "master");
        git.setFolder("git");
        HgMaterial hg = MaterialsMother.hgMaterial();
        hg.setFolder("hg");
        PipelineConfig upstreamWithMingle = PipelineConfigMother.createPipelineConfig("upstream-with-mingle", "stage", "build");
        upstreamWithMingle.setMaterialConfigs(new MaterialConfigs(git.config(), hg.config()));
        configHelper.addPipelineToGroup(upstreamWithMingle, "upstream-with-mingle");

        P4Material p4 = MaterialsMother.p4Material("loser:007", "loser", "boozer", "through-the-window", true);
        PipelineConfig upstreamWithoutMingle = PipelineConfigMother.createPipelineConfig("upstream-without-mingle", "stage", "build");
        upstreamWithoutMingle.setMaterialConfigs(new MaterialConfigs(p4.config()));
        configHelper.addPipelineToGroup(upstreamWithoutMingle, "upstream-without-mingle");

        DependencyMaterial dependencyMaterial = MaterialsMother.dependencyMaterial(upstreamWithMingle.name().toString(), upstreamWithMingle.get(0).name().toString());
        SvnMaterial svn = MaterialsMother.svnMaterial("http://svn.com");
        DependencyMaterial dependencyMaterialViaP4 = MaterialsMother.dependencyMaterial(upstreamWithoutMingle.name().toString(), upstreamWithoutMingle.get(0).name().toString());
        PipelineConfig downstream = PipelineConfigMother.createPipelineConfig("downstream", "down-stage", "job");
        downstream.setMaterialConfigs(new MaterialConfigs(dependencyMaterial.config(), svn.config(), dependencyMaterialViaP4.config()));

        configHelper.addPipelineToGroup(downstream, "downstream");

        //mingle card nos.
        //svn: 1xx
        //p4: 2xx
        //hg: 3xx
        //git: 4xx

        Modification svnCommit1 = checkin(svn, "888", "svn commit #101 1", "svn 1 guy", "svn.1@gmail.com", checkinTime);

        Modification hgCommit1 = checkin(hg, "abcd", "hg commit 1 #301", "hg 1 guy", "hg.1@gmail.com", checkinTime);

        Modification gitCommit1 = checkin(git, "1234", "#401 - git commit 1", "git 1 guy", "git.1@gmail.com", checkinTime);

        Modification gitCommit2 = checkin(git, "2355", "git #402 commit 2", "git 2&3 guy", "git.2.and.3@gmail.com", checkinTime);//used in a later instance

        Modification hgCommit2 = checkin(hg, "abc", "hg commit #302 2", "hg 2 guy", null, checkinTime);

        Pipeline pipelineOne = dbHelper.checkinRevisionsToBuild(build, upstreamWithMingle, new MaterialRevision(git, gitCommit1), new MaterialRevision(hg, hgCommit2, hgCommit1));

        Modification hgCommit3 = checkin(hg, "bcde", "hg commit 3 #303", "hg 3 guy", "hg.3@gmail.com", checkinTime);

        Modification gitCommit3 = checkin(git, "567", "git #403 commit 3", "git 2&3 guy", "git.2.and.3@gmail.com", checkinTime);

        Pipeline pipelineTwo = dbHelper.checkinRevisionsToBuild(build, upstreamWithMingle, new MaterialRevision(git, gitCommit3, gitCommit2), new MaterialRevision(hg, hgCommit3));

        Modification p4Commit1 = checkin(p4, "777", "#201 - p4 commit 1", "p4 1 guy", "p4.1@gmail.com", checkinTime);

        Pipeline pipelineP4 = dbHelper.checkinRevisionsToBuild(build, upstreamWithoutMingle, new MaterialRevision(p4, p4Commit1));

        List<MaterialRevision> materialRevisionsFor1 = new ArrayList<>();
        dbHelper.addDependencyRevisionModification(materialRevisionsFor1, dependencyMaterial, pipelineOne);
        dbHelper.addDependencyRevisionModification(materialRevisionsFor1, dependencyMaterialViaP4, pipelineP4);

        Modification svnCommit2 = checkin(svn, "999", "svn #102 commit 2", "svn 2 guy", "svn.2@gmail.com", checkinTime);

        materialRevisionsFor1.add(new MaterialRevision(svn, svnCommit2, svnCommit1));
        //save downstream pipeline 1
        dbHelper.passPipelineFirstStageOnly(dbHelper.checkinRevisionsToBuild(build, downstream, materialRevisionsFor1));

        Modification p4Commit2 = checkin(p4, "007", "#202 - p4 commit 2", "p4 2 guy", "p4.2@gmail.com", checkinTime);

        Pipeline pipeline2P4 = dbHelper.checkinRevisionsToBuild(build, upstreamWithoutMingle, new MaterialRevision(p4, p4Commit2));

        List<MaterialRevision> materialRevisionsFor2 = new ArrayList<>();
        dbHelper.addDependencyRevisionModification(materialRevisionsFor2, dependencyMaterial, pipelineTwo);
        dbHelper.addDependencyRevisionModification(materialRevisionsFor2, dependencyMaterialViaP4, pipeline2P4);

        Modification svnCommit3 = checkin(svn, "1000", "svn commit #103 3", "svn 3 guy", "svn.3@gmail.com", checkinTime);
        materialRevisionsFor2.add(new MaterialRevision(svn, svnCommit3));

        //save downstream pipeline 2
        dbHelper.passPipelineFirstStageOnly(dbHelper.checkinRevisionsToBuild(build, downstream, materialRevisionsFor2));
        return downstream;
    }

    private Modification checkin(Material material, String rev, String comment, String user, String email, Date checkinTime) {
        Modification commit = checkinWithComment(rev, comment, user, email, checkinTime);
        dbHelper.addRevisionsWithModifications(material, commit);//saved now, used later
        return commit;
    }
}
