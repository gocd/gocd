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

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.config.exceptions.NotAuthorizedException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.activity.StageStatusCache;
import com.thoughtworks.go.domain.feed.Author;
import com.thoughtworks.go.domain.feed.FeedEntries;
import com.thoughtworks.go.domain.feed.FeedEntry;
import com.thoughtworks.go.domain.feed.stage.StageFeedEntry;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.FeedModifier;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.messaging.StageStatusMessage;
import com.thoughtworks.go.server.messaging.StageStatusTopic;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.transaction.TestTransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TestTransactionTemplate;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.ui.JobInstanceModel;
import com.thoughtworks.go.server.ui.ModificationForPipeline;
import com.thoughtworks.go.server.ui.PipelineId;
import com.thoughtworks.go.server.ui.StageSummaryModel;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.TestingClock;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.helper.ModificationsMother.*;
import static com.thoughtworks.go.helper.PipelineMother.completedFailedStageInstance;
import static com.thoughtworks.go.server.security.GoAuthority.ROLE_ANONYMOUS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

public class StageServiceTest {

    private static final String STAGE_NAME = "dev";

    private StageDao stageDao;

    private GoConfigFileHelper configFileHelper = new GoConfigFileHelper();
    private JobInstanceService jobInstanceService;
    private SecurityService securityService;
    private ChangesetService changesetService;
    private CruiseConfig cruiseConfig;
    private GoConfigService goConfigService;
    private TransactionTemplate transactionTemplate;
    private TestTransactionSynchronizationManager transactionSynchronizationManager;
    private PipelineDao pipelineDao;

    private static final Username ALWAYS_ALLOW_USER = new Username(new CaseInsensitiveString("always allowed"));
    private GoCache goCache;

    @BeforeEach
    public void setUp() throws Exception {
        stageDao = mock(StageDao.class);
        pipelineDao = mock(PipelineDao.class);
        jobInstanceService = mock(JobInstanceService.class);
        securityService = mock(SecurityService.class);
        cruiseConfig = mock(BasicCruiseConfig.class);
        goConfigService = mock(GoConfigService.class);
        changesetService = mock(ChangesetService.class);
        goCache = mock(GoCache.class);

        transactionSynchronizationManager = new TestTransactionSynchronizationManager();
        transactionTemplate = new TestTransactionTemplate(transactionSynchronizationManager);
        SessionUtils.setCurrentUser(new GoUserPrinciple("anonymous", "anonymous", ROLE_ANONYMOUS.asAuthority()));
    }

    @AfterEach
    public void teardown() throws Exception {
        configFileHelper.initializeConfigFile();
        SessionUtils.unsetCurrentUser();
    }

    @Test
    public void shouldFindStageSummaryModelForGivenStageIdentifier() {
        SecurityService securityService = alwaysAllow();

        TransactionSynchronizationManager transactionSynchronizationManager = mock(TransactionSynchronizationManager.class);

        StageService service = new StageService(stageDao, null, null, null, securityService, null, changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager,
            goCache);

        Stage stageRun1 = StageMother.completedStageInstanceWithTwoPlans("stage_name");
        stageRun1.setIdentifier(new StageIdentifier("pipeline_name/10/stage_name/1"));
        stageRun1.setCounter(1);

        Stage stageRun2 = StageMother.completedStageInstanceWithTwoPlans("stage_name");
        stageRun2.setIdentifier(new StageIdentifier("pipeline_name/10/stage_name/2"));
        stageRun2.setCounter(2);

        Stages stages = new Stages(stageRun1, stageRun2);
        StageIdentifier stageId = new StageIdentifier("pipeline_name/10/stage_name/2");
        when(stageDao.getAllRunsOfStageForPipelineInstance(stageId.getPipelineName(), stageId.getPipelineCounter(), stageId.getStageName())).thenReturn(stages);

        StageSummaryModel stageForView = service.findStageSummaryByIdentifier(stageId, ALWAYS_ALLOW_USER, new HttpLocalizedOperationResult());

        assertThat(stageForView.getName()).isEqualTo(stageRun2.getName());
        assertThat(stageForView.getState()).isEqualTo(stageRun2.stageState());
        assertThat(stageForView.getStageCounter()).isEqualTo(String.valueOf(stageRun2.getCounter()));
        assertThat(stageForView.getTotalRuns()).isEqualTo(2);
    }

    private SecurityService alwaysAllow() {
        SecurityService securityService = mock(SecurityService.class);
        when(securityService.hasViewPermissionForPipeline(eq(ALWAYS_ALLOW_USER), any(String.class))).thenReturn(true);
        return securityService;
    }

    @Test
    public void shouldBeAbleToGetAJobsDuration() {

        TestingClock clock = new TestingClock();
        JobInstance theJob = JobInstanceMother.building("job", clock.currentTime());
        theJob.setClock(clock);
        clock.addSeconds(9);
        theJob.completing(JobResult.Passed, clock.currentTime());
        theJob.completed(clock.currentTime());

        StageIdentifier stageId = new StageIdentifier(theJob.getPipelineName(), 1, "1.0.1", "1");
        Stages stages = new Stages(StageMother.custom(theJob.getStageName(), theJob));

        TransactionSynchronizationManager transactionSynchronizationManager = mock(TransactionSynchronizationManager.class);

        StageService service = new StageService(stageDao, null, null, null, alwaysAllow(), null, changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager,
            goCache);
        when(stageDao.getAllRunsOfStageForPipelineInstance(stageId.getPipelineName(), stageId.getPipelineCounter(), stageId.getStageName())).thenReturn(stages);
        when(stageDao.getExpectedDurationMillis(theJob.getPipelineName(), theJob.getStageName(), theJob)).thenReturn(10 * 1000L);

        StageSummaryModel stageForView = service.findStageSummaryByIdentifier(stageId, ALWAYS_ALLOW_USER, new HttpLocalizedOperationResult());

        JobInstanceModel job = stageForView.passedJobs().get(0);
        assertThat(job.getElapsedTime()).isEqualTo(theJob.getElapsedTime());
        assertThat(job.getPercentComplete()).isEqualTo(90);
        verify(stageDao).getExpectedDurationMillis(theJob.getPipelineName(), theJob.getStageName(), theJob);
    }

    @Test
    public void findStageSummaryByIdentifierShouldRespondWith403WhenUserDoesNotHavePermissionToViewThePipeline() {
        SecurityService securityService = mock(SecurityService.class);
        when(securityService.hasViewPermissionForPipeline(ALWAYS_ALLOW_USER, "pipeline_name")).thenReturn(false);
        TransactionSynchronizationManager transactionSynchronizationManager = mock(TransactionSynchronizationManager.class);
        StageService service = new StageService(stageDao, null, null, null, securityService, null, changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager,
            goCache);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        StageSummaryModel model = service.findStageSummaryByIdentifier(new StageIdentifier("pipeline_name/10/stage_name/1"), ALWAYS_ALLOW_USER, result);

        assertThat(result.httpCode()).isEqualTo(403);
        assertThat(model).isNull();
    }

    @Test
    public void findStageSummaryByIdentifierShouldRespondWith404WhenNoStagesFound() {
        SecurityService securityService = mock(SecurityService.class);
        when(securityService.hasViewPermissionForPipeline(ALWAYS_ALLOW_USER, "pipeline_does_not_exist")).thenReturn(true);
        TransactionSynchronizationManager transactionSynchronizationManager = mock(TransactionSynchronizationManager.class);
        StageService service = new StageService(stageDao, null, null, null, securityService, null, changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager,
            goCache);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        StageIdentifier stageId = new StageIdentifier("pipeline_does_not_exist/10/stage_name/1");
        when(stageDao.getAllRunsOfStageForPipelineInstance(stageId.getPipelineName(), stageId.getPipelineCounter(), stageId.getStageName())).thenReturn(new Stages());

        StageSummaryModel model = service.findStageSummaryByIdentifier(stageId, ALWAYS_ALLOW_USER, result);

        assertThat(result.httpCode()).isEqualTo(404);
        assertThat(model).isNull();
    }

    @Test
    public void findStageSummaryByIdentifierShouldRespondWith404WhenStagesHavingGivenCounterIsNotFound() {
        SecurityService securityService = mock(SecurityService.class);
        when(securityService.hasViewPermissionForPipeline(ALWAYS_ALLOW_USER, "dev")).thenReturn(true);
        TransactionSynchronizationManager transactionSynchronizationManager = mock(TransactionSynchronizationManager.class);
        StageService service = new StageService(stageDao, null, null, null, securityService, null, changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager,
            goCache);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        Stages stages = new Stages();
        Stage stage1 = new Stage();
        stage1.setIdentifier(new StageIdentifier("dev/10/stage_name/1"));
        stages.add(stage1);

        StageIdentifier stageId = new StageIdentifier("dev/10/stage_name/9999999999");
        when(stageDao.getAllRunsOfStageForPipelineInstance(stageId.getPipelineName(), stageId.getPipelineCounter(), stageId.getStageName())).thenReturn(stages);

        StageSummaryModel model = service.findStageSummaryByIdentifier(stageId, ALWAYS_ALLOW_USER, result);

        assertThat(model).isNull();
        assertThat(result.httpCode()).isEqualTo(404);
    }

    @Test
    public void shouldUpdateJobInstanceAndStageOnCancellingJob() throws SQLException {

        JobInstance job = new JobInstance("job");
        job.setIdentifier(new JobIdentifier("pipeline", 10, "label", STAGE_NAME, "5", "job"));

        TransactionSynchronizationManager transactionSynchronizationManager = mock(TransactionSynchronizationManager.class);
        StageService service = new StageService(stageDao, jobInstanceService, null, null, null, null, changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager,
            goCache);

        JobInstance foundJob = new JobInstance("job");
        foundJob.setState(JobState.Scheduled);
        foundJob.setResult(JobResult.Unknown);
        JobInstances foundJobInstances = new JobInstances(foundJob);

        Stage foundStage = new Stage(STAGE_NAME, foundJobInstances, "jez", null, "manual", new TimeProvider());
        foundStage.calculateResult();

        assertThat(foundStage.getState()).isNotEqualTo(StageState.Cancelled);
        assertThat(foundStage.getResult()).isNotEqualTo(StageResult.Cancelled);

        foundJob.setState(JobState.Completed);
        foundJob.setResult(JobResult.Cancelled);

        when(stageDao.findStageWithIdentifier(job.getIdentifier().getStageIdentifier())).thenReturn(foundStage);

        service.cancelJob(job);

        assertThat(foundStage.getState()).isEqualTo(StageState.Cancelled);
        assertThat(foundStage.getResult()).isEqualTo(StageResult.Cancelled);

        verify(jobInstanceService).cancelJob(job);
        verify(stageDao).updateResult(foundStage, StageResult.Cancelled, null);
    }

    @Nested
    class FindCompletedStagesFor {
        private Date updateDate;
        private StageService service;

        @BeforeEach
        void setUp() {
            updateDate = new Date();
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig("cruise"));

            Map<Long, List<ModificationForPipeline>> expectedMap = new HashMap<>();
            ModificationForPipeline modification = new ModificationForPipeline(new PipelineId("cruise", 1L), checkinWithComment("revision", "#123 hello wolrd", updateDate), "Svn", "fooBarBaaz");
            expectedMap.put(1L, of(modification));
            when(changesetService.modificationsOfPipelines(of(1L), "cruise", Username.ANONYMOUS)).thenReturn(expectedMap);

            service = new StageService(stageDao, null, null, null, null, null, changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager,
                new StubGoCache(transactionSynchronizationManager));
        }

        @Test
        public void shouldCacheTheResultPerPipeline() {
            when(stageDao.findCompletedStagesFor("cruise", FeedModifier.Latest, -1, 25))
                .thenReturn(of(stageFeedEntry("cruise", updateDate)))
                .thenReturn(of(stageFeedEntry("cruise", updateDate)));

            FeedEntry expected = stageFeedEntry("cruise", updateDate);

            FeedEntries feedEntries = service.feed("cruise", Username.ANONYMOUS);//Should prime the cache
            assertThat(feedEntries).hasSize(1).contains(expected);
            assertThat(feedEntries.get(0).getAuthors()).hasSize(1).contains(new Author(MOD_USER_COMMITTER, EMAIL_ADDRESS));

            feedEntries = service.feed("cruise", Username.ANONYMOUS);//Should use the cache
            assertThat(feedEntries).hasSize(1).contains(expected);
            assertThat(feedEntries.get(0).getAuthors()).hasSize(1).contains(new Author(MOD_USER_COMMITTER, EMAIL_ADDRESS));

            verify(stageDao).findCompletedStagesFor("cruise", FeedModifier.Latest, -1, 25);
            verify(changesetService).modificationsOfPipelines(of(1L), "cruise", Username.ANONYMOUS);
            verifyNoMoreInteractions(stageDao);
            verifyNoMoreInteractions(changesetService);
        }

        @Test
        public void shouldInvalidateCacheOnCompletionOfAStage() {
            when(stageDao.findCompletedStagesFor("cruise", FeedModifier.Latest, -1, 25))
                .thenReturn(of(stageFeedEntry("cruise", updateDate)))
                .thenReturn(of(stageFeedEntry("cruise", updateDate)));

            FeedEntry expected = stageFeedEntry("cruise", updateDate);

            FeedEntries feedEntries = service.feed("cruise", Username.ANONYMOUS);//Should cache
            assertThat(feedEntries).hasSize(1).contains(expected);
            assertThat(feedEntries.get(0).getAuthors()).hasSize(1).contains(new Author(MOD_USER_COMMITTER, EMAIL_ADDRESS));

            Stage stage = StageMother.createPassedStage("cruise", 1, "stage", 1, "job", updateDate);
            stage.setIdentifier(new StageIdentifier("cruise", 1, "stage", String.valueOf(1)));
            service.updateResult(stage);//Should remove from the cache

            feedEntries = service.feed("cruise", Username.ANONYMOUS);// Should retrieve from db again.
            assertThat(feedEntries).hasSize(1).contains(expected);
            assertThat(feedEntries.get(0).getAuthors()).hasSize(1).contains(new Author(MOD_USER_COMMITTER, EMAIL_ADDRESS));

            verify(stageDao, times(2)).findCompletedStagesFor("cruise", FeedModifier.Latest, -1, 25);
            verify(changesetService, times(2)).modificationsOfPipelines(of(1L), "cruise", Username.ANONYMOUS);
            verifyNoMoreInteractions(changesetService);
        }

        @Test
        public void shouldNotCacheTheResultPerPipelineForFeedsBeforeAGivenID() {
            when(stageDao.findCompletedStagesFor("cruise", FeedModifier.Before, 1L, 25))
                .thenReturn(of(stageFeedEntry("cruise", updateDate)))
                .thenReturn(of(stageFeedEntry("cruise", updateDate)));

            FeedEntry expected = stageFeedEntry("cruise", updateDate);

            FeedEntries feedEntries = service.feedBefore(1L, "cruise", Username.ANONYMOUS);
            assertThat(feedEntries).hasSize(1).contains(expected);
            assertThat(feedEntries.get(0).getAuthors()).hasSize(1).contains(new Author(MOD_USER_COMMITTER, EMAIL_ADDRESS));

            feedEntries = service.feedBefore(1L, "cruise", Username.ANONYMOUS);
            assertThat(feedEntries).hasSize(1).contains(expected);
            assertThat(feedEntries.get(0).getAuthors()).hasSize(1).contains(new Author(MOD_USER_COMMITTER, EMAIL_ADDRESS));

            verify(stageDao, times(2)).findCompletedStagesFor("cruise", FeedModifier.Before, 1L, 25);
            verifyNoMoreInteractions(stageDao);
        }
    }

    @Nested
    class FindStageFeedBy {
        private StageService service;
        private Username username = new Username("bob");

        @BeforeEach
        void setUp() {
            when(changesetService.modificationsOfPipelines(of(1L), "cruise", username)).thenReturn(singletonMap(1L, emptyList()));
            service = new StageService(stageDao, null, null, null, null, null, changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager,
                new StubGoCache(transactionSynchronizationManager));
        }

        @Test
        void shouldGetTheFeedEntriesFromDB() {
            StageFeedEntry entry = stageFeedEntry("cruise", new Date());
            when(stageDao.findStageFeedBy("cruise", 1, FeedModifier.Latest, 25))
                .thenReturn(of(entry));

            FeedEntries entries = service.findStageFeedBy("cruise", 1, FeedModifier.Latest, username);

            assertThat(entries).hasSize(1).contains(entry);
            verify(stageDao).findStageFeedBy("cruise", 1, FeedModifier.Latest, 25);
        }

        @Test
        void shouldReturnTheLatestFeedEntriesFromCacheOnceItIsCachedWhenPipelineCounterIsNull() {
            StageFeedEntry entry = stageFeedEntry("cruise", new Date());
            when(stageDao.findStageFeedBy("cruise", null, null, 25))
                .thenReturn(of(entry));

            FeedEntries entries = service.findStageFeedBy("cruise", null, FeedModifier.Latest, username);
            assertThat(entries).hasSize(1).contains(entry);

            entries = service.findStageFeedBy("cruise", null, null, username);
            assertThat(entries).hasSize(1).contains(entry);

            verify(stageDao, times(1)).findStageFeedBy("cruise", null, null, 25);
            verifyNoMoreInteractions(stageDao);
        }

        @Test
        void shouldAlwaysGetFeedEntriesFromDBWhenPipelineCounterIsGiven() {
            StageFeedEntry entry = stageFeedEntry("cruise", new Date());
            when(stageDao.findStageFeedBy("cruise", 1, FeedModifier.Latest, 25))
                .thenReturn(of(entry));

            FeedEntries entries = service.findStageFeedBy("cruise", 1, FeedModifier.Latest, username);
            assertThat(entries).hasSize(1).contains(entry);

            entries = service.findStageFeedBy("cruise", 1, FeedModifier.Latest, username);
            assertThat(entries).hasSize(1).contains(entry);

            verify(stageDao, times(2)).findStageFeedBy("cruise", 1, FeedModifier.Latest, 25);
            verifyNoMoreInteractions(stageDao);
        }
    }

    private CruiseConfig cruiseConfig(final String pipelineName) {
        CruiseConfig config = new BasicCruiseConfig();
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig(pipelineName);
        config.addPipeline("group", pipelineConfig);
        return config;
    }

    @Test
    public void shouldReturnFeedsEvenIfUpstreamPipelineIsDeleted() {
        Date updateDate = new Date();
        CruiseConfig config = mock(BasicCruiseConfig.class);
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("down");
        Map<Long, List<ModificationForPipeline>> expectedModMapDown = new HashMap<>();
        Modification mod1 = checkinWithComment("revision", "#123 hello wolrd", updateDate);
        expectedModMapDown.put(1L, asList(new ModificationForPipeline(new PipelineId("down", 1L), mod1, "Svn", "fooBarBaaz")));

        FeedEntry expected = stageFeedEntry("down", updateDate);

        when(stageDao.findCompletedStagesFor("down", FeedModifier.Latest, -1, 25)).thenReturn(asList(stageFeedEntry("down", updateDate), stageFeedEntry("down", updateDate)));
        when(goConfigService.currentCruiseConfig()).thenReturn(config);
        when(changesetService.modificationsOfPipelines(asList(1L, 1L), "down", Username.ANONYMOUS)).thenReturn(expectedModMapDown);
        when(config.hasPipelineNamed(any(CaseInsensitiveString.class))).thenReturn(false).thenReturn(true);
        when(config.pipelineConfigByName(any(CaseInsensitiveString.class))).thenReturn(pipelineConfig);

        StageService service = new StageService(stageDao, null, null, null, null, null, changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager,
            new StubGoCache(transactionSynchronizationManager));

        FeedEntries feedEntries = service.feed("down", Username.ANONYMOUS);
        assertThat(feedEntries).isEqualTo(new FeedEntries(asList(expected, expected)));
        assertThat(feedEntries.get(0).getAuthors()).isEqualTo(asList(new Author(MOD_USER_COMMITTER, EMAIL_ADDRESS)));
        assertThat(feedEntries.get(1).getAuthors()).isEqualTo(asList(new Author(MOD_USER_COMMITTER, EMAIL_ADDRESS)));
    }

    private StageFeedEntry stageFeedEntry(String pipelineName, final Date updateDate) {
        return new StageFeedEntry(1L, 1L, new StageIdentifier(pipelineName + "/1/dist/1"), 1L, updateDate, StageResult.Passed);
    }

    @Test
    public void shouldSendStageStatusMessageAfterStageIsCancelled() throws SQLException {
        StageStatusTopic topic = mock(StageStatusTopic.class);
        final Stage cancelledStage = StageMother.cancelledStage("stage", "job");
        cancelledStage.setIdentifier(new StageIdentifier("pipeline/1/stage/1"));
        final StageService service = new StageService(stageDao, jobInstanceService, topic, new StageStatusCache(stageDao), null, null, changesetService, goConfigService, transactionTemplate,
            transactionSynchronizationManager,
            mock(GoCache.class));

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                service.cancelStage(cancelledStage, null);
            }
        });

        verify(topic).post(new StageStatusMessage(cancelledStage.getIdentifier(), StageState.Cancelled, StageResult.Cancelled, Username.ANONYMOUS));
        verifyNoMoreInteractions(topic);
    }

    @Test
    public void shouldNotSendStageStatusMessageAfterStageIsCancelledAndAnyOfTheJobIsAssigned() throws SQLException {
        StageStatusTopic topic = mock(StageStatusTopic.class);
        final Stage cancelledStage = StageMother.cancelledStage("stage", "job");
        cancelledStage.setIdentifier(new StageIdentifier("pipeline/1/stage/1"));
        cancelledStage.getJobInstances().first().setAgentUuid("soem-agent");

        final StageService service = new StageService(stageDao, jobInstanceService, topic, new StageStatusCache(stageDao), null, null, changesetService, goConfigService, transactionTemplate,
            transactionSynchronizationManager,
            mock(GoCache.class));

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                service.cancelStage(cancelledStage, null);
            }
        });

        verifyZeroInteractions(topic);
    }

    @Test
    public void shouldFindLatestStageFromCache() throws SQLException {
        Stage expectedStage = StageMother.custom("pipeline", "stage", null);
        StageStatusCache cache = new StageStatusCache(stageDao);
        cache.stageStatusChanged(expectedStage);
        TransactionSynchronizationManager transactionSynchronizationManager = mock(TransactionSynchronizationManager.class);
        StageService service = new StageService(stageDao, jobInstanceService, null, cache, null, null, changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager,
            goCache);
        Stage actualStage = service.findLatestStage("pipeline", "stage");

        assertThat(actualStage).isEqualTo(expectedStage);
    }

    @Test
    public void shouldOnlyLoadStagesArtifactOfWhichCanBeDeleted() {
        StageService service = new StageService(stageDao, null, null, null, securityService, null, changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager,
            goCache);
        Stage stageFoo = StageMother.passedStageInstance("stage-foo", "job", "pipeline-baz");
        Stage stageBar = StageMother.passedStageInstance("stage-bar", "job", "pipeline-quux");
        Stage stageBaz = StageMother.passedStageInstance("stage-baz", "job", "pipeline-foo");
        Stage stageQuux = StageMother.passedStageInstance("stage-quux", "job", "pipeline-bar");
        when(stageDao.oldestStagesHavingArtifacts()).thenReturn(asList(stageFoo, stageBar, stageBaz, stageQuux));

        List<Stage> stages = service.oldestStagesWithDeletableArtifacts();
        assertThat(stages.size()).isEqualTo(4);
        assertThat(stages).contains(stageFoo);
        assertThat(stages).contains(stageBar);
        assertThat(stages).contains(stageBaz);
        assertThat(stages).contains(stageQuux);
    }

    @Test
    public void shouldDelegateToDAO_findDetailedStageHistoryByOffset() {
        when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipeline"))).thenReturn(true);
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(securityService.hasViewPermissionForPipeline(Username.valueOf("looser"), "pipeline")).thenReturn(true);

        final StageService stageService = new StageService(stageDao, jobInstanceService, mock(StageStatusTopic.class), mock(StageStatusCache.class), securityService, pipelineDao,
            changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager, goCache);

        Pagination pagination = Pagination.pageStartingAt(1, 1, 1);
        stageService.findDetailedStageHistoryByOffset("pipeline", "stage", pagination, "looser", new HttpOperationResult());

        verify(stageDao).findDetailedStageHistoryByOffset("pipeline", "stage", pagination);
    }

    @Test
    public void shouldPopulateErrorWhenPipelineNotFound_findDetailedStageHistoryByOffset() {
        when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipeline"))).thenReturn(false);
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(securityService.hasViewPermissionForPipeline(Username.valueOf("looser"), "pipeline")).thenReturn(true);

        final StageService stageService = new StageService(stageDao, jobInstanceService, mock(StageStatusTopic.class), mock(StageStatusCache.class), securityService, pipelineDao,
            changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager, goCache);

        Pagination pagination = Pagination.pageStartingAt(1, 1, 1);
        HttpOperationResult result = new HttpOperationResult();
        StageInstanceModels stageInstanceModels = stageService.findDetailedStageHistoryByOffset("pipeline", "stage", pagination, "looser", result);

        assertThat(stageInstanceModels).isNull();
        assertThat(result.httpCode()).isEqualTo(404);
    }

    @Test
    public void shouldPopulateErrorWhenUnauthorized_findDetailedStageHistoryByOffset() {
        when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipeline"))).thenReturn(true);
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(securityService.hasViewPermissionForPipeline(Username.valueOf("looser"), "pipeline")).thenReturn(false);

        final StageService stageService = new StageService(stageDao, jobInstanceService, mock(StageStatusTopic.class), mock(StageStatusCache.class), securityService, pipelineDao,
            changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager, goCache);

        Pagination pagination = Pagination.pageStartingAt(1, 1, 1);
        HttpOperationResult result = new HttpOperationResult();
        StageInstanceModels stageInstanceModels = stageService.findDetailedStageHistoryByOffset("pipeline", "stage", pagination, "looser", result);

        assertThat(stageInstanceModels).isNull();
        assertThat(result.httpCode()).isEqualTo(403);
    }

    @Test
    public void shouldPopulateErrorWhenPipelineNotFound_findStageWithIdentifier() {
        when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipeline"))).thenReturn(false);
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(securityService.hasViewPermissionForPipeline(Username.valueOf("looser"), "pipeline")).thenReturn(true);

        final StageService stageService = new StageService(stageDao, jobInstanceService, mock(StageStatusTopic.class), mock(StageStatusCache.class), securityService, pipelineDao,
            changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager, goCache);

        HttpOperationResult result = new HttpOperationResult();
        Stage stage = stageService.findStageWithIdentifier("pipeline", 1, "stage", "1", "looser", result);

        assertThat(stage).isNull();
        assertThat(result.httpCode()).isEqualTo(404);
    }

    @Test
    public void shouldPopulateErrorWhenUnauthorized_findStageWithIdentifier() {
        when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipeline"))).thenReturn(true);
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(securityService.hasViewPermissionForPipeline(Username.valueOf("looser"), "pipeline")).thenReturn(false);

        final StageService stageService = new StageService(stageDao, jobInstanceService, mock(StageStatusTopic.class), mock(StageStatusCache.class), securityService, pipelineDao,
            changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager, goCache);

        HttpOperationResult result = new HttpOperationResult();
        Stage stage = stageService.findStageWithIdentifier("pipeline", 1, "stage", "1", "looser", result);

        assertThat(stage).isNull();
        assertThat(result.httpCode()).isEqualTo(403);
    }

    @Test
    public void shouldPopulateErrorWhenPipelineWithCounterNotFound_findStageWithIdentifier() {
        when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipeline"))).thenReturn(true);
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(securityService.hasViewPermissionForPipeline(Username.valueOf("looser"), "pipeline")).thenReturn(true);
        when(pipelineDao.findPipelineByNameAndCounter("pipeline", 1)).thenReturn(null);

        final StageService stageService = new StageService(stageDao, jobInstanceService, mock(StageStatusTopic.class), mock(StageStatusCache.class), securityService, pipelineDao,
            changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager, goCache);

        HttpOperationResult result = new HttpOperationResult();
        Stage stage = stageService.findStageWithIdentifier("pipeline", 1, "stage", "1", "looser", result);

        assertThat(stage).isNull();
        assertThat(result.httpCode()).isEqualTo(404);
        assertThat(result.detailedMessage()).startsWith("Not Found { Pipeline 'pipeline' with counter '1' not found }");
    }

    @Nested
    class StageHistoryViaCursor {
        private StageService stageService;
        private Username username = Username.valueOf("user");
        String pipelineName = "pipeline";

        @BeforeEach
        void setUp() {
            stageService = new StageService(stageDao, jobInstanceService, null, null, securityService, pipelineDao, changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager, goCache);
        }

        @Test
        void shouldFetchLatestRecords() {
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
            when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
            when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(true);

            stageService.findStageHistoryViaCursor(username, pipelineName, STAGE_NAME, 0, 0, 10);

            verify(stageDao).findDetailedStageHistoryViaCursor(pipelineName, STAGE_NAME, FeedModifier.Latest, 0, 10);
        }

        @Test
        void shouldFetchRecordsAfterTheSpecifiedCursor() {
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
            when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
            when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(true);

            stageService.findStageHistoryViaCursor(username, pipelineName, STAGE_NAME, 2, 0, 10);

            verify(stageDao).findDetailedStageHistoryViaCursor(pipelineName, STAGE_NAME, FeedModifier.After, 2, 10);
        }

        @Test
        void shouldFetchRecordsBeforeTheSpecifiedCursor() {
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
            when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
            when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(true);

            stageService.findStageHistoryViaCursor(username, pipelineName, STAGE_NAME, 0, 3, 10);

            verify(stageDao).findDetailedStageHistoryViaCursor(pipelineName, STAGE_NAME, FeedModifier.Before, 3, 10);
        }

        @Test
        void shouldThrowErrorIfPipelineDoesNotExist() {
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);

            assertThatCode(() -> stageService.findStageHistoryViaCursor(username, pipelineName, STAGE_NAME, 0, 0, 10))
                .isInstanceOf(RecordNotFoundException.class)
                .hasMessage("Pipeline with name 'pipeline' was not found!");
        }

        @Test
        void shouldThrowErrorIfUserDoesNotHaveAccessRights() {
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
            when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);

            assertThatCode(() -> stageService.findStageHistoryViaCursor(username, pipelineName, STAGE_NAME, 0, 0, 10))
                .isInstanceOf(NotAuthorizedException.class)
                .hasMessage("User 'user' does not have permission to view pipeline with name 'pipeline'");
        }

        @Test
        void shouldThrowErrorIfCursorIsANegativeInteger() {
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
            when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
            when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(true);

            assertThatCode(() -> stageService.findStageHistoryViaCursor(username, pipelineName, STAGE_NAME, -10, 0, 10))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The query parameter 'after', if specified, must be a positive integer.");

            assertThatCode(() -> stageService.findStageHistoryViaCursor(username, pipelineName, STAGE_NAME, 0, -10, 10))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The query parameter 'before', if specified, must be a positive integer.");
        }
    }

    @Nested
    class LatestAndOldestStageInstanceId {
        private StageService stageService;
        private Username username = Username.valueOf("user");
        String pipelineName = "pipeline";

        @BeforeEach
        void setUp() {
            stageService = new StageService(stageDao, jobInstanceService, null, null, securityService, pipelineDao, changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager, goCache);
        }

        @Test
        void shouldReturnTheLatestAndOldestRunID() {
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
            when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
            when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(true);

            stageService.getOldestAndLatestStageInstanceId(username, pipelineName, STAGE_NAME);

            verify(stageDao).getOldestAndLatestStageInstanceId(pipelineName, STAGE_NAME);
        }


        @Test
        void shouldThrowErrorIfPipelineDoesNotExist() {
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);

            assertThatCode(() -> stageService.getOldestAndLatestStageInstanceId(username, pipelineName, STAGE_NAME))
                .isInstanceOf(RecordNotFoundException.class)
                .hasMessage("Pipeline with name 'pipeline' was not found!");
        }

        @Test
        void shouldThrowErrorIfUserDoesNotHaveAccessRights() {
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
            when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);

            assertThatCode(() -> stageService.getOldestAndLatestStageInstanceId(username, pipelineName, STAGE_NAME))
                .isInstanceOf(NotAuthorizedException.class)
                .hasMessage("User 'user' does not have permission to view pipeline with name 'pipeline'");
        }
    }

    @Nested
    class FindStageWithIdentifierWithoutHttpOperationResult {
        private StageService stageService;
        private Username username = Username.valueOf("bob");

        @BeforeEach
        void setUp() {
            stageService = new StageService(stageDao, jobInstanceService, null, null,
                securityService, pipelineDao, changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager, goCache);
        }

        @Test
        void shouldThrowRecordNotFoundWhenPipelineWithNameDoesNotExist() {
            when(goConfigService.hasPipelineNamed(new CaseInsensitiveString("up42"))).thenReturn(false);

            assertThatCode(() -> stageService.findStageWithIdentifier("up42", 1, "unit-tests", "1", username))
                .isInstanceOf(RecordNotFoundException.class)
                .hasMessage("Pipeline with name 'up42' was not found!");
        }

        @Test
        void shouldThrowNotAuthorizedWhenUserDoesNotViewPermissionHave() {
            when(goConfigService.hasPipelineNamed(new CaseInsensitiveString("up42"))).thenReturn(true);
            when(securityService.hasViewPermissionForPipeline(username, "up42")).thenReturn(false);

            assertThatCode(() -> stageService.findStageWithIdentifier("up42", 1, "unit-tests", "1", username))
                .isInstanceOf(NotAuthorizedException.class)
                .hasMessage("Not authorized to view pipeline");
        }

        @Test
        void shouldThrowRecordNotFoundWhenPipelineInstanceWithCounterDoesNotExist() {
            String pipelineName = "up42";
            int pipelineCounter = 1;
            String stageName = "unit-tests";
            when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
            when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(true);
            when(pipelineDao.findPipelineByNameAndCounter(pipelineName, pipelineCounter)).thenReturn(null);

            assertThatCode(() -> {
                stageService.findStageWithIdentifier(pipelineName, pipelineCounter, stageName, "1", username);
            }).isInstanceOf(RecordNotFoundException.class)
                .hasMessage("Pipeline 'up42' with counter '1' not found!");
        }

        @Test
        void shouldGetStageWithIdentifier() {
            String pipelineName = "up42";
            int pipelineCounter = 1;
            String stageName = "unit-tests";
            String stageCounter = "1";
            Pipeline pipeline = completedFailedStageInstance(pipelineName, stageName, "junit", new Date());
            when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
            when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(true);
            when(pipelineDao.findPipelineByNameAndCounter(pipelineName, pipelineCounter)).thenReturn(pipeline);

            StageService spy = spy(stageService);
            spy.findStageWithIdentifier(pipelineName, pipelineCounter, stageName, stageCounter, username);

            verify(spy).findStageWithIdentifier(new StageIdentifier(pipelineName, pipelineCounter, stageName, stageCounter));
        }
    }
}
