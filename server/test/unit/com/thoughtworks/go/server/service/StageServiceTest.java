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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.activity.StageStatusCache;
import com.thoughtworks.go.domain.feed.Author;
import com.thoughtworks.go.domain.feed.FeedEntries;
import com.thoughtworks.go.domain.feed.FeedEntry;
import com.thoughtworks.go.domain.feed.stage.StageFeedEntry;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.FeedModifier;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.dao.sparql.StageRunFinder;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.messaging.StageStatusMessage;
import com.thoughtworks.go.server.messaging.StageStatusTopic;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.server.transaction.TestTransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TestTransactionTemplate;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.ui.*;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.TestingClock;
import com.thoughtworks.go.util.TimeProvider;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import java.sql.SQLException;
import java.util.*;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class StageServiceTest {

    private static final String PIPELINE_NAME = "cruise";
    private static final String STAGE_NAME = "dev";

    private StageDao stageDao;

    private GoConfigFileHelper configFileHelper = new GoConfigFileHelper();
    private JobInstanceService jobInstanceService;
    private SecurityService securityService;
    private ChangesetService changesetService;
	private CruiseConfig cruiseConfig;
    private GoConfigService goConfigService;
    private List<CaseInsensitiveString> pipelineNames;
    private Username user;
    private TransactionTemplate transactionTemplate;
    private TestTransactionSynchronizationManager transactionSynchronizationManager;
    private PipelineDao pipelineDao;


    private static final Username ALWAYS_ALLOW_USER = new Username(new CaseInsensitiveString("always allowed"));
    private LocalizedOperationResult operationResult;
    private GoCache goCache;

    @Before
    public void setUp() throws Exception {
        stageDao = mock(StageDao.class);
		pipelineDao = mock(PipelineDao.class);
        jobInstanceService = mock(JobInstanceService.class);
        securityService = mock(SecurityService.class);
        pipelineNames = asList(new CaseInsensitiveString("blah-pipeline"));
        user = new Username(new CaseInsensitiveString("poovan"));
        operationResult = new HttpLocalizedOperationResult();
		cruiseConfig = mock(BasicCruiseConfig.class);
        goConfigService = mock(GoConfigService.class);
        changesetService = mock(ChangesetService.class);
        goCache = mock(GoCache.class);

        transactionSynchronizationManager = new TestTransactionSynchronizationManager();
        transactionTemplate = new TestTransactionTemplate(transactionSynchronizationManager);
    }

    @After
    public void teardown() throws Exception {
        configFileHelper.initializeConfigFile();
    }

    @Test
    public void canGetFailureRunForThreeStages() {
        TransactionSynchronizationManager transactionSynchronizationManager = mock(TransactionSynchronizationManager.class);
        StageRunFinder runFinder = new StageService(stageDao, jobInstanceService, mock(StageStatusTopic.class), mock(StageStatusCache.class), securityService, pipelineDao,
                changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager, goCache);

        List<StageIdentifier> expectedStages = new ArrayList<StageIdentifier>();
        expectedStages.add(new StageIdentifier(PIPELINE_NAME, 3, STAGE_NAME, "1"));
        expectedStages.add(new StageIdentifier(PIPELINE_NAME, 2, STAGE_NAME, "2"));
        expectedStages.add(new StageIdentifier(PIPELINE_NAME, 1, STAGE_NAME, "1"));

        Pipeline pipeline = pipeline(10.0);
        Pipeline pipelineThatLastPassed = pipeline(5.0);

        when(pipelineDao.findPipelineByNameAndCounter(PIPELINE_NAME, 3)).thenReturn(pipeline);
        when(pipelineDao.findEarlierPipelineThatPassedForStage(PIPELINE_NAME, STAGE_NAME, 10.0)).thenReturn(pipelineThatLastPassed);
        when(stageDao.findFailedStagesBetween(PIPELINE_NAME, STAGE_NAME, 5.0, 10.0)).thenReturn(asList(identifier(3, "1"), identifier(2, "2"), identifier(1, "1")));

        assertEquals(expectedStages, runFinder.findRunForStage(new StageIdentifier(PIPELINE_NAME, 3, STAGE_NAME, "1")));
    }

    @Test
    public void canGetFailureRunForThreeStagesAtStartOfPipeline() {
        TransactionSynchronizationManager transactionSynchronizationManager = mock(TransactionSynchronizationManager.class);

        StageRunFinder runFinder = new StageService(stageDao, jobInstanceService, mock(StageStatusTopic.class), mock(StageStatusCache.class), securityService,
                pipelineDao, changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager, goCache);

        List<StageIdentifier> expectedStages = new ArrayList<StageIdentifier>();
        expectedStages.add(new StageIdentifier(PIPELINE_NAME, 3, STAGE_NAME, "1"));
        expectedStages.add(new StageIdentifier(PIPELINE_NAME, 2, STAGE_NAME, "2"));
        expectedStages.add(new StageIdentifier(PIPELINE_NAME, 1, STAGE_NAME, "1"));

        Pipeline pipeline = pipeline(10.0);

        when(pipelineDao.findPipelineByNameAndCounter(PIPELINE_NAME, 3)).thenReturn(pipeline);
        when(pipelineDao.findEarlierPipelineThatPassedForStage(PIPELINE_NAME, STAGE_NAME, 10.0)).thenReturn(null);
        when(stageDao.findFailedStagesBetween(PIPELINE_NAME, STAGE_NAME, 0.0, 10.0)).thenReturn(asList(identifier(3, "1"), identifier(2, "2"), identifier(1, "1")));

        assertEquals(expectedStages, runFinder.findRunForStage(new StageIdentifier(PIPELINE_NAME, 3, STAGE_NAME, "1")));
    }

    @Test
    public void shouldNotReturnAnythingWhenNothingIsFailing() {
        TransactionSynchronizationManager transactionSynchronizationManager = mock(TransactionSynchronizationManager.class);

        StageRunFinder runFinder = new StageService(stageDao, jobInstanceService, mock(StageStatusTopic.class), mock(StageStatusCache.class), securityService,
                pipelineDao, changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager, goCache);


        Pipeline pipeline = pipeline(10.0);
        Pipeline pipelineThatLastPassed = pipeline(5.0);

        when(pipelineDao.findPipelineByNameAndCounter(PIPELINE_NAME, 3)).thenReturn(pipeline);
        when(pipelineDao.findEarlierPipelineThatPassedForStage(PIPELINE_NAME, STAGE_NAME, 10.0)).thenReturn(pipelineThatLastPassed);
        when(stageDao.findFailedStagesBetween(PIPELINE_NAME, STAGE_NAME, 5.0, 10.0)).thenReturn(new ArrayList<StageIdentifier>());

        assertThat(runFinder.findRunForStage(new StageIdentifier(PIPELINE_NAME, 3, STAGE_NAME, "1")).isEmpty(), is(true));
    }

    @Test
    public void shouldNotReturnAnythingWhenCurrentStageHasNotFailed() {
        TransactionSynchronizationManager transactionSynchronizationManager = mock(TransactionSynchronizationManager.class);
        StageRunFinder runFinder = new StageService(stageDao, jobInstanceService, mock(StageStatusTopic.class), mock(StageStatusCache.class), securityService,
                pipelineDao, changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager, goCache);

        Pipeline pipeline = pipeline(10.0);
        Pipeline pipelineThatLastPassed = pipeline(5.0);

        when(pipelineDao.findPipelineByNameAndCounter(PIPELINE_NAME, 3)).thenReturn(pipeline);
        when(pipelineDao.findEarlierPipelineThatPassedForStage(PIPELINE_NAME, STAGE_NAME, 10.0)).thenReturn(pipelineThatLastPassed);
        when(stageDao.findFailedStagesBetween(PIPELINE_NAME, STAGE_NAME, 5.0, 10.0)).thenReturn(asList(identifier(2, "2"), identifier(1, "1")));

        assertThat(runFinder.findRunForStage(new StageIdentifier(PIPELINE_NAME, 3, STAGE_NAME, "1")).isEmpty(), is(true));
    }

    private Pipeline pipeline(double naturalOrder) {
        Pipeline pipeline = PipelineMother.completedFailedStageInstance(PIPELINE_NAME, STAGE_NAME, "foo", new Date());
        pipeline.setNaturalOrder(naturalOrder);
        return pipeline;
    }

    private StageIdentifier identifier(int pipelineCounter, String stageCounter) {
        return new StageIdentifier(PIPELINE_NAME, pipelineCounter, STAGE_NAME, stageCounter);
    }

    @Test
    public void shouldFindStageSummaryModelForGivenStageIdentifier() throws Exception {
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

        assertThat(stageForView.getName(), is(stageRun2.getName()));
        assertThat(stageForView.getState(), is(stageRun2.stageState()));
        assertThat(stageForView.getStageCounter(), is(String.valueOf(stageRun2.getCounter())));
        assertThat(stageForView.getTotalRuns(), is(2));
    }

    private SecurityService alwaysAllow() {
        SecurityService securityService = mock(SecurityService.class);
        when(securityService.hasViewPermissionForPipeline(eq(ALWAYS_ALLOW_USER), any(String.class))).thenReturn(true);
        return securityService;
    }

    @Test
    public void shouldBeAbleToGetAJobsDuration() throws Exception {

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
        assertThat(job.getElapsedTime(), is(theJob.getElapsedTime()));
        assertThat(job.getPercentComplete(), is(90));
        verify(stageDao).getExpectedDurationMillis(theJob.getPipelineName(), theJob.getStageName(), theJob);
    }

    @Test
    public void findStageSummaryByIdentifierShouldRespondWith401WhenUserDoesNotHavePermissionToViewThePipeline() throws Exception {
        SecurityService securityService = mock(SecurityService.class);
        when(securityService.hasViewPermissionForPipeline(ALWAYS_ALLOW_USER, "pipeline_name")).thenReturn(false);
        TransactionSynchronizationManager transactionSynchronizationManager = mock(TransactionSynchronizationManager.class);
        StageService service = new StageService(stageDao, null, null, null, securityService, null, changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager,
                goCache);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        StageSummaryModel model = service.findStageSummaryByIdentifier(new StageIdentifier("pipeline_name/10/stage_name/1"), ALWAYS_ALLOW_USER, result);

        assertThat(result.httpCode(), is(401));
        assertThat(model, is(nullValue()));
    }

    @Test
    public void findStageSummaryByIdentifierShouldRespondWith404WhenNoStagesFound() throws Exception {
        SecurityService securityService = mock(SecurityService.class);
        when(securityService.hasViewPermissionForPipeline(ALWAYS_ALLOW_USER, "pipeline_does_not_exist")).thenReturn(true);
        TransactionSynchronizationManager transactionSynchronizationManager = mock(TransactionSynchronizationManager.class);
        StageService service = new StageService(stageDao, null, null, null, securityService, null, changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager,
                goCache);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        StageIdentifier stageId = new StageIdentifier("pipeline_does_not_exist/10/stage_name/1");
        when(stageDao.getAllRunsOfStageForPipelineInstance(stageId.getPipelineName(), stageId.getPipelineCounter(), stageId.getStageName())).thenReturn(new Stages());

        StageSummaryModel model = service.findStageSummaryByIdentifier(stageId, ALWAYS_ALLOW_USER, result);

        assertThat(result.httpCode(), is(404));
        assertThat(model, is(nullValue()));
    }

    @Test
    public void findStageSummaryByIdentifierShouldRespondWith404WhenStagesHavingGivenCounterIsNotFound() throws Exception {
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

        assertThat(model, is(nullValue()));
        assertThat(result.httpCode(), is(404));
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

        Stage foundStage = new Stage(STAGE_NAME, foundJobInstances, "jez", "manual", new TimeProvider());
        foundStage.calculateResult();

        assertThat(foundStage.getState(), is(not(StageState.Cancelled)));
        assertThat(foundStage.getResult(), is(not(StageResult.Cancelled)));

        foundJob.setState(JobState.Completed);
        foundJob.setResult(JobResult.Cancelled);

        when(stageDao.findStageWithIdentifier(job.getIdentifier().getStageIdentifier())).thenReturn(foundStage);

        service.cancelJob(job);

        assertThat(foundStage.getState(), is(StageState.Cancelled));
        assertThat(foundStage.getResult(), is(StageResult.Cancelled));

        verify(jobInstanceService).cancelJob(job);
        verify(stageDao).updateResult(foundStage, StageResult.Cancelled);
    }

    @Test
    public void findCompletedStagesFor_shouldCacheTheResultPerPipeline() {
        Date updateDate = new Date();
        when(stageDao.findCompletedStagesFor("cruise", FeedModifier.Latest, -1, 25)).thenReturn(asList(stageFeedEntry("cruise", updateDate))).thenReturn(asList(stageFeedEntry("cruise",
                updateDate)));

        MingleConfig mingleConfig = new MingleConfig("http://foo.bar:7019/baz/", "go-project");
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfigWithMingle("cruise", mingleConfig));

        Map<Long, List<ModificationForPipeline>> expectedMap = new HashMap<Long, List<ModificationForPipeline>>();
        expectedMap.put(1L,
                asList(new ModificationForPipeline(new PipelineId("cruise", 1L), ModificationsMother.checkinWithComment("revision", "#123 hello wolrd", updateDate), "Svn", "fooBarBaaz")));
        when(changesetService.modificationsOfPipelines(asList(1L), "cruise", Username.ANONYMOUS)).thenReturn(expectedMap);

        StageService service = new StageService(stageDao, null, null, null, null, null, changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager,
                new StubGoCache(transactionSynchronizationManager));

        FeedEntry expected = stageFeedEntry("cruise", updateDate);

        FeedEntries feedEntries = service.feed("cruise", Username.ANONYMOUS);//Should prime the cache
        assertThat(feedEntries, is(new FeedEntries(asList(expected))));
        assertThat(feedEntries.get(0).getAuthors(), is(asList(new Author(ModificationsMother.MOD_USER_COMMITTER, ModificationsMother.EMAIL_ADDRESS))));
        assertThat(feedEntries.get(0).getMingleCards(), is(asList(new MingleCard(mingleConfig, "123"))));

        feedEntries = service.feed("cruise", Username.ANONYMOUS);//Should use the cache
        assertThat(feedEntries, is(new FeedEntries(asList(expected))));
        assertThat(feedEntries.get(0).getAuthors(), is(asList(new Author(ModificationsMother.MOD_USER_COMMITTER, ModificationsMother.EMAIL_ADDRESS))));
        assertThat(feedEntries.get(0).getMingleCards(), is(asList(new MingleCard(mingleConfig, "123"))));

        verify(stageDao).findCompletedStagesFor("cruise", FeedModifier.Latest, -1, 25);
        verify(changesetService).modificationsOfPipelines(asList(1L), "cruise", Username.ANONYMOUS);
        verifyNoMoreInteractions(stageDao);
        verifyNoMoreInteractions(changesetService);
    }

    @Test
    public void findCompletedStagesFor_shouldInvalidateCacheOnCompletionOfAStage() {
        Date updateDate = new Date();
        when(stageDao.findCompletedStagesFor("cruise", FeedModifier.Latest, -1, 25)).thenReturn(asList(stageFeedEntry("cruise", updateDate))).thenReturn(asList(stageFeedEntry("cruise",
                updateDate)));

        MingleConfig mingleConfig = new MingleConfig("http://foo.bar:7019/baz/", "go-project");
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfigWithMingle("cruise", mingleConfig));

        Map<Long, List<ModificationForPipeline>> expectedMap = new HashMap<Long, List<ModificationForPipeline>>();
        expectedMap.put(1L,
                asList(new ModificationForPipeline(new PipelineId("cruise", 1L), ModificationsMother.checkinWithComment("revision", "#123 hello wolrd", updateDate), "Svn", "fooBarBaaz")));
        when(changesetService.modificationsOfPipelines(asList(1L), "cruise", Username.ANONYMOUS)).thenReturn(expectedMap);

        StageService service = new StageService(stageDao, null, null, null, null, null, changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager,
                new StubGoCache(transactionSynchronizationManager));

        FeedEntry expected = stageFeedEntry("cruise", updateDate);

        FeedEntries feedEntries = service.feed("cruise", Username.ANONYMOUS);//Should cache
        assertThat(feedEntries, is(new FeedEntries(asList(expected))));
        assertThat(feedEntries.get(0).getAuthors(), is(asList(new Author(ModificationsMother.MOD_USER_COMMITTER, ModificationsMother.EMAIL_ADDRESS))));
        assertThat(feedEntries.get(0).getMingleCards(), is(asList(new MingleCard(mingleConfig, "123"))));

        Stage stage = StageMother.createPassedStage("cruise", 1, "stage", 1, "job", updateDate);
        stage.setIdentifier(new StageIdentifier("cruise", 1, "stage", String.valueOf(1)));
        service.updateResult(stage);//Should remove from the cache

        feedEntries = service.feed("cruise", Username.ANONYMOUS);// Should retrieve from db again.
        assertThat(feedEntries, is(new FeedEntries(asList(expected))));
        assertThat(feedEntries.get(0).getAuthors(), is(asList(new Author(ModificationsMother.MOD_USER_COMMITTER, ModificationsMother.EMAIL_ADDRESS))));
        assertThat(feedEntries.get(0).getMingleCards(), is(asList(new MingleCard(mingleConfig, "123"))));

        verify(stageDao, times(2)).findCompletedStagesFor("cruise", FeedModifier.Latest, -1, 25);
        verify(changesetService, times(2)).modificationsOfPipelines(asList(1L), "cruise", Username.ANONYMOUS);
        verifyNoMoreInteractions(changesetService);
    }

    private CruiseConfig cruiseConfigWithMingle(final String pipelineName, MingleConfig mingleConfig) {
        CruiseConfig config = new BasicCruiseConfig();
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig(pipelineName);
        pipelineConfig.setMingleConfig(mingleConfig);
        config.addPipeline("group", pipelineConfig);
        return config;
    }

    @Test
    public void findCompletedStagesFor_shouldNotCacheTheResultPerPipelineForFeedsBeforeAGivenID() {
        Date updateDate = new Date();
        when(stageDao.findCompletedStagesFor("cruise", FeedModifier.Before, 1L, 25)).thenReturn(asList(stageFeedEntry("cruise", updateDate))).thenReturn(asList(stageFeedEntry("cruise",
                updateDate)));

        //Setup Mingle Config
        MingleConfig mingleConfig = new MingleConfig("http://foo.bar:7019/baz/", "go-project");
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfigWithMingle("cruise", mingleConfig));

        //Setup card numbers
        Map<Long, List<ModificationForPipeline>> expectedMap = new HashMap<Long, List<ModificationForPipeline>>();
        expectedMap.put(1L,
                asList(new ModificationForPipeline(new PipelineId("cruise", 1L), ModificationsMother.checkinWithComment("revision", "#123 hello wolrd", updateDate), "Svn", "fooBarBaaz")));
        when(changesetService.modificationsOfPipelines(asList(1L), "cruise", Username.ANONYMOUS)).thenReturn(expectedMap);

        StageService service = new StageService(stageDao, null, null, null, null, null, changesetService, goConfigService, transactionTemplate, transactionSynchronizationManager,
                new StubGoCache(transactionSynchronizationManager));

        FeedEntry expected = stageFeedEntry("cruise", updateDate);

        FeedEntries feedEntries = service.feedBefore(1L, "cruise", Username.ANONYMOUS);
        assertThat(feedEntries, is(new FeedEntries(asList(expected))));
        assertThat(feedEntries.get(0).getAuthors(), is(asList(new Author(ModificationsMother.MOD_USER_COMMITTER, ModificationsMother.EMAIL_ADDRESS))));
        assertThat(feedEntries.get(0).getMingleCards(), is(asList(new MingleCard(mingleConfig, "123"))));

        feedEntries = service.feedBefore(1L, "cruise", Username.ANONYMOUS);
        assertThat(feedEntries, is(new FeedEntries(asList(expected))));
        assertThat(feedEntries.get(0).getAuthors(), is(asList(new Author(ModificationsMother.MOD_USER_COMMITTER, ModificationsMother.EMAIL_ADDRESS))));
        assertThat(feedEntries.get(0).getMingleCards(), is(asList(new MingleCard(mingleConfig, "123"))));

        verify(stageDao, times(2)).findCompletedStagesFor("cruise", FeedModifier.Before, 1L, 25);
        verifyNoMoreInteractions(stageDao);
    }

    @Test
    public void shouldReturnFeedsEvenIfUpstreamPipelineIsDeleted() {
        Date updateDate = new Date();
        CruiseConfig config = mock(BasicCruiseConfig.class);
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("down");
        MingleConfig mingleConfig = new MingleConfig("http://foo.bar:7019/baz/", "go-project");
        pipelineConfig.setMingleConfig(mingleConfig);
        Map<Long, List<ModificationForPipeline>> expectedModMapDown = new HashMap<Long, List<ModificationForPipeline>>();
        Modification mod1 = ModificationsMother.checkinWithComment("revision", "#123 hello wolrd", updateDate);
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
        assertThat(feedEntries, is(new FeedEntries(asList(expected, expected))));
        assertThat(feedEntries.get(0).getAuthors(), is(asList(new Author(ModificationsMother.MOD_USER_COMMITTER, ModificationsMother.EMAIL_ADDRESS))));
        assertEquals(feedEntries.get(0).getMingleCards().size(), 0);
        assertThat(feedEntries.get(1).getAuthors(), is(asList(new Author(ModificationsMother.MOD_USER_COMMITTER, ModificationsMother.EMAIL_ADDRESS))));
        assertThat(feedEntries.get(1).getMingleCards(), is(asList(new MingleCard(mingleConfig, "123"))));
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
            public void doInTransactionWithoutResult(TransactionStatus status) {
                service.cancelStage(cancelledStage);
            }
        });

        verify(topic).post(new StageStatusMessage(cancelledStage.getIdentifier(), StageState.Cancelled, StageResult.Cancelled, Username.ANONYMOUS));
        verifyNoMoreInteractions(topic);
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

        assertThat(actualStage, is(expectedStage));
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
        assertThat(stages.size(), is(4));
        assertThat(stages, hasItem(stageFoo));
        assertThat(stages, hasItem(stageBar));
        assertThat(stages, hasItem(stageBaz));
        assertThat(stages, hasItem(stageQuux));
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

		assertThat(stageInstanceModels, is(Matchers.nullValue()));
		assertThat(result.httpCode(), is(404));
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

		assertThat(stageInstanceModels, is(Matchers.nullValue()));
        assertThat(result.httpCode(), is(401));
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

        assertThat(stage, is(Matchers.nullValue()));
        assertThat(result.httpCode(), is(404));
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

        assertThat(stage, is(Matchers.nullValue()));
        assertThat(result.httpCode(), is(401));
    }
}
