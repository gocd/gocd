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
package com.thoughtworks.go.server.dao;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.feed.FeedEntry;
import com.thoughtworks.go.domain.feed.stage.StageFeedEntry;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.presentation.pipelinehistory.JobHistory;
import com.thoughtworks.go.presentation.pipelinehistory.StageHistoryEntry;
import com.thoughtworks.go.presentation.pipelinehistory.StageHistoryPage;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.InstanceFactory;
import com.thoughtworks.go.server.service.ScheduleService;
import com.thoughtworks.go.server.service.ScheduleTestUtil;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.transaction.SqlMapClientTemplate;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.TimeProvider;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.util.ReflectionUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Supplier;

import static com.thoughtworks.go.domain.PersistentObject.NOT_PERSISTED;
import static com.thoughtworks.go.helper.PipelineMother.custom;
import static com.thoughtworks.go.helper.PipelineMother.twoBuildPlansWithResourcesAndMaterials;
import static com.thoughtworks.go.util.DataStructureUtils.a;
import static com.thoughtworks.go.util.GoConstants.DEFAULT_APPROVED_BY;
import static com.thoughtworks.go.util.IBatisUtil.arguments;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
    "classpath:/applicationContext-global.xml",
    "classpath:/applicationContext-dataLocalAccess.xml",
    "classpath:/testPropertyConfigurer.xml",
    "classpath:/spring-all-servlet.xml",
})
public class StageSqlMapDaoIntegrationTest {
    @Autowired
    private GoCache goCache;
    @Autowired
    private StageSqlMapDao stageDao;
    @Autowired
    private JobInstanceDao jobInstanceDao;
    @Autowired
    private PipelineDao pipelineDao;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private MaterialRepository materialRepository;
    @Autowired
    private InstanceFactory instanceFactory;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private static final String STAGE_DEV = "dev";
    private PipelineConfig mingleConfig;
    private static final String PIPELINE_NAME = "mingle";
    private SqlMapClientTemplate origTemplate;
    private String md5 = "md5";
    private ScheduleTestUtil scheduleUtil;

    @BeforeEach
    public void setup() throws Exception {
        goCache.clear();
        dbHelper.onSetUp();
        mingleConfig = twoBuildPlansWithResourcesAndMaterials(PIPELINE_NAME, STAGE_DEV);
        origTemplate = stageDao.getSqlMapClientTemplate();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        scheduleUtil = new ScheduleTestUtil(transactionTemplate, materialRepository, dbHelper, configHelper);
    }

    @AfterEach
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        configHelper.onTearDown();
        stageDao.setSqlMapClientTemplate(origTemplate);
    }

    @Test
    public void shouldUpdateCompletingTransitionIdWhenUpdatingResult() throws Exception {
        Pipeline pipeline = pipelineWithOnePassedAndOneCurrentlyRunning(mingleConfig)[1];
        Stage stage = pipeline.getStages().get(0);
        stage.setCompletedByTransitionId(10L);
        updateResultInTransaction(stage, StageResult.Passed);
        Stage reloaded = stageDao.stageById(stage.getId());
        assertThat(reloaded.getCompletedByTransitionId()).isGreaterThan(0l);
    }

    @Test
    public void onStageUpdateShouldSetTheLastTransitionedTimeOnTheUpdateStageSinceTheTimeIsGeneratedByADatabaseTrigger() throws Exception {
        Pipeline pipeline = pipelineWithFirstStageRunning(mingleConfig);
        Stage stage = pipeline.getStages().get(0);

        assertThat(stage.getLastTransitionedTime()).isNull();

        updateResultInTransaction(stage, StageResult.Passed);

        assertThat(stage.getLastTransitionedTime()).isNotNull();
    }

    @Test
    public void shouldUpdateStageStateWhenUpdatingResult() throws Exception {
        Pipeline pipeline = pipelineWithOnePassedAndOneCurrentlyRunning(mingleConfig)[1];
        Stage stage = pipeline.getStages().get(0);
        stage.calculateResult();
        StageState initialState = stage.stageState();
        updateResultInTransaction(stage, StageResult.Passed);
        Stage reloaded = stageDao.stageById(stage.getId());
        assertThat(reloaded.getState()).isEqualTo(initialState);
    }

    @Test
    public void shouldFindAllRunsOfAStageForAPipelineRun() throws Exception {
        Pipeline[] pipelines = pipelineWithOnePassedAndOneCurrentlyRunning(mingleConfig);
        Pipeline completed = pipelines[0];

        rerunFirstStage(completed);

        Stages stages = stageDao.getAllRunsOfStageForPipelineInstance(completed.getName(), completed.getCounter(), STAGE_DEV);
        assertThat(stages.size()).isEqualTo(2);
        assertThat(stages.first().getIdentifier()).isEqualTo(new StageIdentifier(completed.getName(), completed.getCounter(), completed.getLabel(), STAGE_DEV, "1"));
        assertThat(stages.last().getIdentifier()).isEqualTo(new StageIdentifier(completed.getName(), completed.getCounter(), completed.getLabel(), STAGE_DEV, "2"));
    }

    @Test
    public void shouldGetPassedStagesByName() throws Exception {
        List<Pipeline> completedPipelines = createFourPipelines();

        Stages stages = stageDao.getPassedStagesByName(CaseInsensitiveString.str(mingleConfig.name()), STAGE_DEV, 2, 0);
        Stage firstStage = stages.first();
        Pipeline firstPipeline = completedPipelines.get(0);
        assertThat(firstStage.getPipelineId()).isEqualTo(firstPipeline.getId());
        assertThat(firstStage.getIdentifier()).isEqualTo(new StageIdentifier(firstPipeline, firstStage));
        assertThat(firstStage.getJobInstances().get(0).getName()).isEqualTo("NixBuild");

        assertThat(stages.size()).isEqualTo(2);
        assertThat(stages.last().getPipelineId()).isEqualTo(completedPipelines.get(1).getId());
        stages = stageDao.getPassedStagesByName(CaseInsensitiveString.str(mingleConfig.name()), STAGE_DEV, 2, 2);
        assertThat(stages.size()).isEqualTo(2);
        assertThat(stages.first().getPipelineId()).isEqualTo(completedPipelines.get(2).getId());
        assertThat(stages.last().getPipelineId()).isEqualTo(completedPipelines.get(3).getId());
    }

    @Test
    public void shouldGetStageInstancesOfTheSameStageRunStartingFromTheLatest() throws Exception {
        List<Pipeline> completedPipelines = new ArrayList<>();
        mingleConfig.add(StageConfigMother.custom("new-stage", "job-1"));
        for (int i = 0; i < 10; i++) {
            Pipeline completed = dbHelper.schedulePipelineWithAllStages(mingleConfig, ModificationsMother.modifySomeFiles(mingleConfig));
            dbHelper.pass(completed);
            completedPipelines.add(completed);
        }
        List<Stage> stages = stageDao.findStageHistoryForChart(mingleConfig.name().toString(), mingleConfig.first().name().toString(), 10, 0);
        assertStagesFound(stages, 10, CaseInsensitiveString.str(mingleConfig.first().name()));
    }

    @Test
    public void shouldGetStageInstancesBasedUponAPageNumberAndLimit() {
        List<Pipeline> completedPipelines = new ArrayList<>();
        mingleConfig.add(StageConfigMother.custom("new-stage", "job-1"));
        for (int i = 0; i < 10; i++) {
            Pipeline completed = dbHelper.schedulePipelineWithAllStages(mingleConfig, ModificationsMother.modifySomeFiles(mingleConfig));
            dbHelper.pass(completed);
            completedPipelines.add(completed);
        }

        List<Stage> stages = stageDao.findStageHistoryForChart(mingleConfig.name().toString(), mingleConfig.first().name().toString(), 5, 0);
        assertThat(stages.get(0).getIdentifier().getPipelineCounter()).isEqualTo(10);
        assertStagesFound(stages, 5, CaseInsensitiveString.str(mingleConfig.first().name()));
        stages = stageDao.findStageHistoryForChart(mingleConfig.name().toString(), mingleConfig.first().name().toString(), 5, 5);
        assertThat(stages.get(0).getIdentifier().getPipelineCounter()).isEqualTo(5);
        assertStagesFound(stages, 5, CaseInsensitiveString.str(mingleConfig.first().name()));
    }

    @Test
    public void shouldNotIncludeStageWithJobRerunWhileGettingLastStageInstances() throws Exception {
        configHelper.addPipeline(mingleConfig);
        configHelper.turnOffSecurity();
        List<Pipeline> completedPipelines = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Pipeline completed = dbHelper.schedulePipelineWithAllStages(mingleConfig, ModificationsMother.modifySomeFiles(mingleConfig));
            dbHelper.pass(completed);
            completedPipelines.add(completed);
        }
        HttpOperationResult result = new HttpOperationResult();
        scheduleService.rerunJobs(completedPipelines.get(0).getFirstStage(), asList(CaseInsensitiveString.str(mingleConfig.first().getJobs().first().name())), result);
        List<Stage> stages = stageDao.findStageHistoryForChart(mingleConfig.name().toString(), mingleConfig.first().name().toString(), 10, 0);
        assertThat(stages.size()).isEqualTo(5);
    }

    @Test
    public void shouldNotIncludeCancelledStagesWhileGettingLastStageInstances() throws Exception {
        configHelper.addPipeline(mingleConfig);
        configHelper.turnOffSecurity();
        List<Pipeline> completedPipelines = new ArrayList<>();
        Pipeline pipeline;
        for (int i = 0; i < 3; i++) {
            pipeline = dbHelper.schedulePipelineWithAllStages(mingleConfig, ModificationsMother.modifySomeFiles(mingleConfig));
            dbHelper.pass(pipeline);
            completedPipelines.add(pipeline);
        }
        List<Stage> stages = stageDao.findStageHistoryForChart(mingleConfig.name().toString(), mingleConfig.first().name().toString(), 10, 0);
        assertThat(stages.size()).isEqualTo(3);

        pipeline = dbHelper.schedulePipelineWithAllStages(mingleConfig, ModificationsMother.modifySomeFiles(mingleConfig));
        dbHelper.cancelStage(pipeline.getFirstStage());

        stages = stageDao.findStageHistoryForChart(mingleConfig.name().toString(), mingleConfig.first().name().toString(), 10, 0);
        assertThat(stages.size()).isEqualTo(3);
    }

    @Test
    public void shouldGetTotalStageCountForChart() {
        configHelper.addPipeline(mingleConfig);
        configHelper.turnOffSecurity();
        List<Pipeline> completedPipelines = new ArrayList<>();
        Pipeline pipeline;
        for (int i = 0; i < 3; i++) {
            pipeline = dbHelper.schedulePipelineWithAllStages(mingleConfig, ModificationsMother.modifySomeFiles(mingleConfig));
            dbHelper.pass(pipeline);
            completedPipelines.add(pipeline);
        }
        assertThat(stageDao.getTotalStageCountForChart(mingleConfig.name().toString(), mingleConfig.first().name().toString())).isEqualTo(3);

        pipeline = dbHelper.schedulePipelineWithAllStages(mingleConfig, ModificationsMother.modifySomeFiles(mingleConfig));
        dbHelper.cancelStage(pipeline.getFirstStage());

        assertThat(stageDao.getTotalStageCountForChart(mingleConfig.name().toString(), mingleConfig.first().name().toString())).isEqualTo(3);
    }

    @Test
    public void getTotalStageCountForChart_shouldCacheTheCount() throws SQLException {
        SqlMapClientTemplate mockClient = mock(SqlMapClientTemplate.class);
        stageDao.setSqlMapClientTemplate(mockClient);

        Map<String, Object> toGet = arguments("pipelineName", "maar").and("stageName", "khoon").asMap();

        when(mockClient.queryForObject("getTotalStageCountForChart", toGet)).thenReturn(3);

        assertThat(stageDao.getTotalStageCountForChart("maar", "khoon")).isEqualTo(3);//Should prime the cache
        assertThat(stageDao.getTotalStageCountForChart("maar", "khoon")).isEqualTo(3);//should use the cache

        verify(mockClient).queryForObject("getTotalStageCountForChart", toGet);
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    public void getTotalStageCountForChart_shouldInvalidateTheCountCacheOnStageSchedule() throws SQLException {
        SqlMapClientTemplate mockClient = mock(SqlMapClientTemplate.class);
        stageDao.setSqlMapClientTemplate(mockClient);

        Map<String, Object> toGet = arguments("pipelineName", "maar").and("stageName", "khoon").asMap();

        when(mockClient.queryForObject("getTotalStageCountForChart", toGet)).thenReturn(3).thenReturn(4);

        assertThat(stageDao.getTotalStageCountForChart("maar", "khoon")).isEqualTo(3);//Should prime the cache
        Stage stage = new Stage("khoon", new JobInstances(), "foo", null, "manual", new TimeProvider());
        Pipeline pipeline = new Pipeline("maar", "${COUNT}", BuildCause.createWithEmptyModifications(), new EnvironmentVariables(), stage);
        pipeline.setId(1);
        stageDao.save(pipeline, stage);//Should Invalidate the cache

        assertThat(stageDao.getTotalStageCountForChart("maar", "khoon")).isEqualTo(4);//should refetch

        verify(mockClient, times(2)).queryForObject("getTotalStageCountForChart", toGet);
    }

    @Test
    public void getTotalStageCountForChart_shouldInvalidateTheCountCacheOnStageUpdate() throws SQLException {
        SqlMapClientTemplate mockClient = mock(SqlMapClientTemplate.class);
        stageDao.setSqlMapClientTemplate(mockClient);

        Map<String, Object> toGet = arguments("pipelineName", "maar").and("stageName", "khoon").asMap();

        when(mockClient.queryForObject("getTotalStageCountForChart", toGet)).thenReturn(3).thenReturn(4);

        assertThat(stageDao.getTotalStageCountForChart("maar", "khoon")).isEqualTo(3);//Should prime the cache
        Stage stage = new Stage("khoon", new JobInstances(), "foo", null, "manual", new TimeProvider());
        stage.setIdentifier(new StageIdentifier("maar/2/khoon/1"));
        updateResultInTransaction(stage, StageResult.Cancelled);//Should Invalidate the cache

        assertThat(stageDao.getTotalStageCountForChart("maar", "khoon")).isEqualTo(4);//should refetch

        verify(mockClient, times(2)).queryForObject("getTotalStageCountForChart", toGet);
    }

    private void assertStagesFound(List<Stage> stages, int size, String stageName) {
        assertThat(stages.size()).isEqualTo(size);
        for (Stage stage : stages) {
            assertThat(stage.getIdentifier().getStageName()).isEqualTo(stageName);
        }
    }

    @Test
    public void shouldGetPassedStagesAfterAGivenStage() throws Exception {
        List<Pipeline> completedPipelines = createFourPipelines();

        Pipeline firstPipeline = completedPipelines.get(0);
        Pipeline secondPipeline = completedPipelines.get(1);
        Pipeline thirdPipeline = completedPipelines.get(2);
        Pipeline fourthPipeline = completedPipelines.get(3);

        Stage firstStageOfPipeline = firstPipeline.getStages().get(0);
        StageIdentifier stageIdentifierOfFirstStageOfFirstPipeline = firstStageOfPipeline.getIdentifier();

        List<StageAsDMR> stages = stageDao.getPassedStagesAfter(stageIdentifierOfFirstStageOfFirstPipeline, 2, 0);

        StageAsDMR firstStage = stages.get(0);
        //ensure populates the relevant fields
        assertThat(firstStage).isEqualTo(stageAsDmr(stageDao.stageById(secondPipeline.getFirstStage().getId())));

        //ensure got the correct records
        assertThat(stages.size()).isEqualTo(2);

        assertThat(stages.get(0)).isEqualTo(stageAsDmr(stageDao.stageById(secondPipeline.getFirstStage().getId())));
        assertThat(stages.get(1)).isEqualTo(stageAsDmr(stageDao.stageById(thirdPipeline.getFirstStage().getId())));

        //ensure gets the next page
        stages = stageDao.getPassedStagesAfter(stageIdentifierOfFirstStageOfFirstPipeline, 2, 2);

        assertThat(stages.size()).isEqualTo(1);
        Stage fourthPipelineStage = fourthPipeline.getFirstStage();
        assertThat(stages.get(0)).isEqualTo(stageAsDmr(stageDao.stageById(fourthPipelineStage.getId())));
    }

    @Test
    public void shouldNotGetFailedCancelledUnknownStagesAfterAGivenStage_AndShouldGetStageTransitionTimeFromStageTable() throws Exception {
        List<Pipeline> pipelines = new ArrayList<>();
        pipelines.add(pipelineWithFirstStagePassed(mingleConfig));
        pipelines.add(pipelineWithFirstStageCancelled(mingleConfig));
        pipelines.add(pipelineWithFirstStagePassed(mingleConfig));
        pipelines.add(pipelineWithFirstStageRunning(mingleConfig));
        pipelines.add(pipelineWithFirstStagePassed(mingleConfig));
        pipelines.add(pipelineWithFirstStageFailed(mingleConfig));
        pipelines.add(pipelineWithFirstStagePassed(mingleConfig));

        Pipeline firstPipeline_passed = pipelines.get(0);
        Pipeline thirdPipeline_passed = pipelines.get(2);
        Pipeline fifthPipeline_passed = pipelines.get(4);
        Pipeline seventhPipeline_passed = pipelines.get(6);

        StageAsDMR stageOfThirdPipeline = stageAsDmr(stageDao.stageById(thirdPipeline_passed.getStages().get(0).getId()));
        StageAsDMR stageOfFifthPipeline = stageAsDmr(stageDao.stageById(fifthPipeline_passed.getStages().get(0).getId()));
        StageAsDMR stageOfSeventhPipeline = stageAsDmr(stageDao.stageById(seventhPipeline_passed.getStages().get(0).getId()));

        List<StageAsDMR> twoPassedAfterFirstPipeline = stageDao.getPassedStagesAfter(firstPipeline_passed.getStages().get(0).getIdentifier(), 2, 0);
        assertThat(twoPassedAfterFirstPipeline.size()).isEqualTo(2);
        assertThat(twoPassedAfterFirstPipeline).isEqualTo(asList(stageOfThirdPipeline, stageOfFifthPipeline));

        List<StageAsDMR> highLimitOfPipelineWhichPassedAfterFirstPipeline = stageDao.getPassedStagesAfter(firstPipeline_passed.getStages().get(0).getIdentifier(), 5, 0);
        assertThat(highLimitOfPipelineWhichPassedAfterFirstPipeline.size()).isEqualTo(3);
        assertThat(highLimitOfPipelineWhichPassedAfterFirstPipeline).isEqualTo(asList(stageOfThirdPipeline, stageOfFifthPipeline, stageOfSeventhPipeline));

        List<StageAsDMR> pipelineAfterLatestRun = stageDao.getPassedStagesAfter(seventhPipeline_passed.getStages().get(0).getIdentifier(), 1, 0);
        assertThat(pipelineAfterLatestRun.size()).isEqualTo(0);
    }

    @Test
    public void getAllRunsOfStageForPipelineInstance_shouldCacheAllTheStages() {
        SqlMapClientTemplate mockTemplate = mock(SqlMapClientTemplate.class);

        Stage first = StageMother.passedStageInstance("pipeline", "stage", 1, "job", new Date());
        Stage second = StageMother.passedStageInstance("pipeline", "stage", 2, "job", new Date());
        Stage third = StageMother.passedStageInstance("pipeline", "stage", 3, "job", new Date());
        List<Stage> expected = asList(third, second, first);

        stageDao.setSqlMapClientTemplate(mockTemplate);
        when(mockTemplate.queryForList(eq("getAllRunsOfStageForPipelineInstance"), any())).thenReturn((List) expected);

        Stages actual = stageDao.getAllRunsOfStageForPipelineInstance("pipeline", 1, "stage");
        assertThat(actual).isEqualTo(expected);
        assertThat(expected == actual).isFalse();
        assertThat(expected.get(0) == actual.get(0)).isFalse();
        stageDao.getAllRunsOfStageForPipelineInstance("pipeline", 1, "stage");
        verify(mockTemplate, times(1)).queryForList(eq("getAllRunsOfStageForPipelineInstance"), any());
    }

    @Test
    public void getAllRunsOfStageForPipelineInstance_shouldClearCacheOnJobStateChange() {
        SqlMapClientTemplate mockTemplate = mock(SqlMapClientTemplate.class);

        Stage first = StageMother.passedStageInstance("pipeline", "stage", 1, "job", new Date());
        Stage second = StageMother.passedStageInstance("pipeline", "stage", 2, "job", new Date());
        Stage third = StageMother.passedStageInstance("pipeline", "stage", 3, "job", new Date());
        List<Stage> expected = asList(third, second, first);

        stageDao.setSqlMapClientTemplate(mockTemplate);
        when(mockTemplate.queryForList(eq("getAllRunsOfStageForPipelineInstance"), any())).thenReturn((List) expected);

        assertThat(stageDao.getAllRunsOfStageForPipelineInstance("pipeline", 1, "stage")).isEqualTo(expected);
        stageDao.jobStatusChanged(first.getFirstJob());
        assertThat(stageDao.getAllRunsOfStageForPipelineInstance("pipeline", 1, "stage")).isEqualTo(expected);
        stageDao.jobStatusChanged(second.getFirstJob());
        assertThat(stageDao.getAllRunsOfStageForPipelineInstance("pipeline", 1, "stage")).isEqualTo(expected);

        verify(mockTemplate, times(3)).queryForList(eq("getAllRunsOfStageForPipelineInstance"), any());
    }

    @Test
    public void getAllRunsOfStageForPipelineInstance_shouldRemoveFromCacheOnStageSave() {
        SqlMapClientTemplate mockTemplate = mock(SqlMapClientTemplate.class);

        Stage newStage = StageMother.passedStageInstance("pipeline", "stage", 2, "job", new Date());
        Stage first = StageMother.passedStageInstance("pipeline", "stage", 1, "job", new Date());
        List<Stage> expected = asList(first);
        List<Stage> expectedSecondTime = asList(first, newStage);

        stageDao.setSqlMapClientTemplate(mockTemplate);
        when(mockTemplate.queryForList(eq("getAllRunsOfStageForPipelineInstance"), any())).thenReturn((List) expected, (List) expectedSecondTime);

        stageDao.getAllRunsOfStageForPipelineInstance("pipeline", 1, "stage");

        Pipeline pipeline = PipelineMother.pipeline("pipeline");
        pipeline.setCounter(1);
        stageDao.save(pipeline, newStage);//should remove first from cache

        Stages actual = stageDao.getAllRunsOfStageForPipelineInstance("pipeline", 1, "stage");
        assertThat(actual.size()).isEqualTo(2);
        assertThat(actual).contains(newStage);
        assertThat(actual).contains(first);

        verify(mockTemplate, times(2)).queryForList(eq("getAllRunsOfStageForPipelineInstance"), any());
    }

    @Test
    public void getAllRunsOfStageForPipelineInstance_shouldRemoveFromCacheOnStageStatusChange() {
        SqlMapClientTemplate mockTemplate = mock(SqlMapClientTemplate.class);

        Stage newStage = StageMother.passedStageInstance("pipeline", "stage", 2, "job", new Date());
        Stage first = StageMother.passedStageInstance("pipeline", "stage", 1, "job", new Date());
        List<Stage> expected = asList(first);
        List<Stage> expectedSecondTime = asList(first, newStage);

        stageDao.setSqlMapClientTemplate(mockTemplate);
        when(mockTemplate.queryForList(eq("getAllRunsOfStageForPipelineInstance"), any())).thenReturn((List) expected, (List) expectedSecondTime);

        stageDao.getAllRunsOfStageForPipelineInstance("pipeline", 1, "stage");

        Pipeline pipeline = PipelineMother.pipeline("pipeline");
        pipeline.setCounter(1);
        updateResultInTransaction(newStage, StageResult.Passed);

        Stages actual = stageDao.getAllRunsOfStageForPipelineInstance("pipeline", 1, "stage");
        assertThat(actual.size()).isEqualTo(2);
        assertThat(actual).contains(newStage);
        assertThat(actual).contains(first);

        verify(mockTemplate, times(2)).queryForList(eq("getAllRunsOfStageForPipelineInstance"), any());
    }

    @Test
    public void findStageWithIdentifier_shouldCacheTheStage() {
        SqlMapClientTemplate mockTemplate = mock(SqlMapClientTemplate.class);

        Stage stage = StageMother.passedStageInstance("pipeline", "stage", "job", new Date());

        stageDao.setSqlMapClientTemplate(mockTemplate);
        when(mockTemplate.queryForObject(eq("findStageWithJobsByIdentifier"), any())).thenReturn(stage);

        Stage actual = stageDao.findStageWithIdentifier(new StageIdentifier("pipeline", 1, "stage", "1"));
        assertThat(actual).isEqualTo(stage);
        stageDao.findStageWithIdentifier(new StageIdentifier("pipeline", 1, "stage", "1"));
        verify(mockTemplate, times(1)).queryForObject(eq("findStageWithJobsByIdentifier"), any());
    }

    @Test
    public void findStageWithIdentifier_shouldClearCacheWhenJobStateChanges() {
        SqlMapClientTemplate mockTemplate = mock(SqlMapClientTemplate.class);

        Stage stage = StageMother.passedStageInstance("pipeline", "stage", "job", new Date());

        stageDao.setSqlMapClientTemplate(mockTemplate);
        when(mockTemplate.queryForObject(eq("findStageWithJobsByIdentifier"), any())).thenReturn(stage);

        assertThat(stageDao.findStageWithIdentifier(new StageIdentifier("pipeline", 1, "stage", "1"))).isEqualTo(stage);
        stageDao.jobStatusChanged(stage.getFirstJob());
        assertThat(stageDao.findStageWithIdentifier(new StageIdentifier("pipeline", 1, "stage", "1"))).isEqualTo(stage);
        verify(mockTemplate, times(2)).queryForObject(eq("findStageWithJobsByIdentifier"), any());
    }

    @Test
    public void findStageWithIdentifier_shouldNotCacheWhenTheStageIsNull() {
        SqlMapClientTemplate mockTemplate = mock(SqlMapClientTemplate.class);

        stageDao.setSqlMapClientTemplate(mockTemplate);
        when(mockTemplate.queryForObject(eq("findStageWithJobsByIdentifier"), any())).thenReturn(null);

        Stage actual = stageDao.findStageWithIdentifier(new StageIdentifier("pipeline", 1, "stage", "1"));
        assertThat(actual).isEqualTo(new NullStage("stage"));
        stageDao.findStageWithIdentifier(new StageIdentifier("pipeline", 1, "stage", "1"));

        verify(mockTemplate, times(2)).queryForObject(eq("findStageWithJobsByIdentifier"), any());
    }

    @Test
    public void findStageWithIdentifier_shouldRemoveFromTheCacheAllStagesWithTheNameOfTheSameCounterOnStageStatusChange() {
        SqlMapClientTemplate mockTemplate = mock(SqlMapClientTemplate.class);

        Stage first = StageMother.passedStageInstance("pipeline", "stage", "job", new Date());
        first.setCounter(1);
        Stage second = StageMother.passedStageInstance("pipeline", "stage", "job", new Date());
        second.setCounter(2);

        stageDao.setSqlMapClientTemplate(mockTemplate);
        when(mockTemplate.queryForObject(eq("findStageWithJobsByIdentifier"), any())).thenReturn(first);

        Stage actual = stageDao.findStageWithIdentifier(new StageIdentifier("pipeline", 1, "stage", "1"));
        assertThat(actual).isEqualTo(first);

        updateResultInTransaction(second, StageResult.Unknown); //status of stage 2 changed. This should invalidate the stage 1 because of the latest run state.

        stageDao.findStageWithIdentifier(new StageIdentifier("pipeline", 1, "stage", "1"));

        verify(mockTemplate, times(2)).queryForObject(eq("findStageWithJobsByIdentifier"), any());
    }

    @Test
    public void findStageWithIdentifier_shouldRemoveFromTheCacheOnSave() {
        SqlMapClientTemplate mockTemplate = mock(SqlMapClientTemplate.class);

        Stage first = StageMother.passedStageInstance("pipeline", "stage", "job", new Date());
        first.setCounter(1);
        Stage second = StageMother.passedStageInstance("pipeline", "stage", "job", new Date());
        second.setCounter(2);

        stageDao.setSqlMapClientTemplate(mockTemplate);
        when(mockTemplate.queryForObject(eq("findStageWithJobsByIdentifier"), any())).thenReturn(first);

        Stage actual = stageDao.findStageWithIdentifier(new StageIdentifier("pipeline", 1, "stage", "1"));
        assertThat(actual).isEqualTo(first);

        Pipeline pipeline = new Pipeline("pipeline", "label", BuildCause.createManualForced(), new EnvironmentVariables(), first);
        pipeline.setCounter(1);
        stageDao.save(pipeline, second); //save stage 2.. This should invalidate the stage 1 because of the latest run state.

        stageDao.findStageWithIdentifier(new StageIdentifier("pipeline", 1, "stage", "1"));

        verify(mockTemplate, times(2)).queryForObject(eq("findStageWithJobsByIdentifier"), any());
    }

    @Test
    public void findStageWithIdentifier_shouldRemoveFromTheCacheOnStageStatusChange() {
        SqlMapClientTemplate mockTemplate = mock(SqlMapClientTemplate.class);

        Stage stage = StageMother.passedStageInstance("pipeline", "stage", "job", new Date());

        stageDao.setSqlMapClientTemplate(mockTemplate);
        when(mockTemplate.queryForObject(eq("findStageWithJobsByIdentifier"), any())).thenReturn(stage);

        Stage actual = stageDao.findStageWithIdentifier(new StageIdentifier("pipeline", 1, "stage", "1"));
        assertThat(actual).isEqualTo(stage);
        assertThat(stage == actual).as("Make sure the cached object is cloned").isFalse();
        updateResultInTransaction(actual, StageResult.Passed);
        stageDao.findStageWithIdentifier(new StageIdentifier("pipeline", 1, "stage", "1"));
        verify(mockTemplate, times(2)).queryForObject(eq("findStageWithJobsByIdentifier"), any());
    }

    @Test
    public void findStageHistoryPage_shouldCacheStageHistoryPage() {
        SqlMapClientTemplate mockTemplate = mock(SqlMapClientTemplate.class);

        Stage stage = StageMother.passedStageInstance("dev", "java", "pipeline-name");
        stage.setApprovedBy("admin");

        stageDao.setSqlMapClientTemplate(mockTemplate);
        when(mockTemplate.queryForObject(eq("getStageHistoryCount"), any())).thenReturn(20);
        when(mockTemplate.queryForObject(eq("findOffsetForStage"), any())).thenReturn(10);
        List<StageHistoryEntry> stageList = asList(new StageHistoryEntry(stage, 1, 10));
        when(mockTemplate.queryForList(eq("findStageHistoryPage"), any())).thenReturn((List) stageList);

        StageHistoryPage stageHistoryPage = stageDao.findStageHistoryPage(stage, 10);
        StageHistoryPage stageHistoryPageInNextQuery = stageDao.findStageHistoryPage(stage, 10);

        assertThat(stageHistoryPage.getStages()).isEqualTo(stageList);
        assertThat(stageHistoryPage.getPagination()).isEqualTo(Pagination.pageFor(10, 20, 10));
        assertThat(stageHistoryPageInNextQuery.getStages()).isEqualTo(stageList);
        assertThat(stageHistoryPageInNextQuery.getPagination()).isEqualTo(Pagination.pageFor(10, 20, 10));

        stageHistoryPage.getStages().get(0).setState(StageState.Failing);
        assertThat(stageHistoryPageInNextQuery.getStages().get(0).getState()).isEqualTo(StageState.Passed);

        verify(mockTemplate, times(1)).queryForList(eq("findStageHistoryPage"), any());
    }

    @Test
    public void shouldGetMostRecentEligibleStage() throws Exception {
        Pipeline expected = dbHelper.schedulePipeline(mingleConfig, new TimeProvider());
        pass(expected);
        setupRescheduledBuild(expected);

        Pipeline second = dbHelper.schedulePipeline(mingleConfig, new TimeProvider());
        fail(second);
        Pipeline third = pipelineWithFirstStageRunning(mingleConfig);

        Stage actual = stageDao.mostRecentPassed(CaseInsensitiveString.str(mingleConfig.name()), STAGE_DEV);
        assertThat(actual.getId()).isEqualTo(pipelineAndFirstStageOf(expected).stage.getId());
        assertThat(actual.getApprovedBy()).isEqualTo(DEFAULT_APPROVED_BY);
    }

    private void setupRescheduledBuild(Pipeline expected) {
        setupRescheduledBuild(expected, JobResult.Unknown);
    }

    private void setupDiscontinuedBuild(Pipeline pipeline) {
        JobInstance instance = pipeline.getFirstStage().getJobInstances().first();
        instance.discontinue();
        jobInstanceDao.updateStateAndResult(instance);
    }

    private void setupRescheduledBuild(Pipeline pipeline, JobResult jobResult) {
        Stage stage = pipeline.getStages().first();
        JobInstance rescheduled = stage.getJobInstances().first().clone();
        rescheduled.changeState(JobState.Rescheduled);
        rescheduled.setResult(jobResult);
        jobInstanceDao.save(stage.getId(), rescheduled);
    }

    @Test
    public void shouldGetMostRecentStageWithBuilds() throws Exception {
        pipelineWithOnePassedAndOneCurrentlyRunning(mingleConfig);
        Stage completed = stageDao.mostRecentWithBuilds(CaseInsensitiveString.str(mingleConfig.name()), mingleConfig.get(0));
        verifyBuildInstancesWithoutCaringAboutTransitions(STAGE_DEV, completed);
    }

    @Test
    public void mostRecentId_shouldCacheResults() throws Exception {
        SqlMapClientTemplate mockTemplate = mock(SqlMapClientTemplate.class);
        stageDao.setSqlMapClientTemplate(mockTemplate);
        when(mockTemplate.queryForObject(eq("getMostRecentId"), any())).thenReturn(20L);

        stageDao.mostRecentId(CaseInsensitiveString.str(mingleConfig.name()), CaseInsensitiveString.str(mingleConfig.get(0).name()));
        Long id = stageDao.mostRecentId(CaseInsensitiveString.str(mingleConfig.name()), CaseInsensitiveString.str(mingleConfig.get(0).name()));

        assertThat(id).isEqualTo(20L);
        verify(mockTemplate, times(1)).queryForObject(eq("getMostRecentId"), any());
    }

    @Test
    public void shouldRemoveCachedMostRecentIdOnStageStatusChange() throws Exception {
        SqlMapClientTemplate mockTemplate = mock(SqlMapClientTemplate.class);
        stageDao.setSqlMapClientTemplate(mockTemplate);
        when(mockTemplate.queryForObject(eq("getMostRecentId"), any())).thenReturn(20L);

        String pipelineName = CaseInsensitiveString.str(mingleConfig.name());
        String stageName = CaseInsensitiveString.str(mingleConfig.get(0).name());
        String key = stageDao.cacheKeyForMostRecentId(pipelineName, stageName);

        // should query and cache value
        stageDao.mostRecentId(pipelineName, stageName);
        Long id = stageDao.mostRecentId(pipelineName, stageName);
        assertThat(id).isEqualTo(20L);

        // should clear the cache
        Stage stage = StageMother.custom(pipelineName, stageName, new JobInstances());
        stageDao.stageStatusChanged(stage);
        assertThat(goCache.get(key)).isNull();

        // should requery and cache
        stageDao.mostRecentId(pipelineName, stageName);
        id = stageDao.mostRecentId(pipelineName, stageName);
        assertThat(id).isEqualTo(20L);
        assertThat(goCache.get(key)).isEqualTo(20L);

        verify(mockTemplate, times(2)).queryForObject(eq("getMostRecentId"), any());
    }

    @Test
    public void shouldNotAllowCachedCopyToBeMutated() throws Exception {
        Pipeline[] pipelines = pipelineWithOnePassedAndOneCurrentlyRunning(mingleConfig);
        long id = pipelines[0].getStages().get(0).getId();
        Stage loaded = stageDao.stageById(id);
        loaded.setName("quux-baz-bar-foo");
        assertThat(loaded.getName()).isEqualTo("quux-baz-bar-foo");
        assertThat(stageDao.stageById(id).getName()).isEqualTo(STAGE_DEV);
    }

    @Test
    public void shouldClear_StageById_Cache_OnStageStatusChange() throws Exception {
        Pipeline[] pipelines = pipelineWithOnePassedAndOneCurrentlyRunning(mingleConfig);
        Stage stage = pipelines[1].getStages().get(0);
        long id = stage.getId();

        Stage loadedBeforeChange = stageDao.stageById(id);
        dbHelper.failStage(stage);
        stageDao.stageStatusChanged(stage);

        assertThat(stageDao.stageById(id)).isNotEqualTo(loadedBeforeChange);
    }

    @Test
    public void shouldServeStageByIdLookupFromCache() throws Exception {
        Pipeline[] pipelines = pipelineWithOnePassedAndOneCurrentlyRunning(mingleConfig);
        Stage stage = pipelines[1].getStages().get(0);

        SqlMapClientTemplate mockTemplate = mock(SqlMapClientTemplate.class);
        stageDao.setSqlMapClientTemplate(mockTemplate);

        when(mockTemplate.queryForObject("getStageById", stage.getId())).thenReturn(stage);

        assertThat(stageDao.stageById(stage.getId())).isEqualTo(stage);
        assertThat(stageDao.stageById(stage.getId())).isNotSameAs(stageDao.stageById(stage.getId()));

        stageDao.jobStatusChanged(stage.getFirstJob());

        assertThat(stageDao.stageById(stage.getId())).isEqualTo(stage);

        verify(mockTemplate, times(2)).queryForObject("getStageById", stage.getId());
    }

    @Test
    public void shouldGetMostRecentStageWithIdentifier() throws Exception {
        Pipeline[] pipelines = pipelineWithOnePassedAndOneCurrentlyRunning(mingleConfig);
        Stage loaded = stageDao.mostRecentStage(new StageConfigIdentifier(PIPELINE_NAME, STAGE_DEV));
        Stage stage = pipelines[1].getStages().byName(STAGE_DEV);
        assertThat(loaded.getId()).isEqualTo(stage.getId());
        assertThat(loaded.getIdentifier()).isEqualTo(new StageIdentifier(pipelines[1], stage));
        assertThat(loaded.getIdentifier().getId()).isEqualTo(stage.getId());
    }

    @Test
    public void shouldReturnNullForMostRecentStageIfNoStage() throws Exception {
        Stage stage = stageDao.mostRecentStage(new StageConfigIdentifier(PIPELINE_NAME, STAGE_DEV));
        assertThat(stage).isNull();
    }

    @Test
    public void shouldGetMostRecentCompletedStage() throws Exception {
        pipelineWithOnePassedAndOneCurrentlyRunning(mingleConfig);
        Stage stage = stageDao.mostRecentCompleted(new StageConfigIdentifier(PIPELINE_NAME, STAGE_DEV));
        assertThat(stage.getResult()).isEqualTo(StageResult.Passed);
    }

    @Test
    public void shouldReturnNullForMostRecentCompletedIfNoStage() throws Exception {
        Stage stage = stageDao.mostRecentCompleted(new StageConfigIdentifier(PIPELINE_NAME, STAGE_DEV));
        assertThat(stage).isNull();
    }

    @Test
    public void shouldReturnNullIfNoMostRecentCompletedStage() throws Exception {
        pipelineWithOneJustScheduled();
        Stage stage = stageDao.mostRecentCompleted(new StageConfigIdentifier(PIPELINE_NAME, STAGE_DEV));
        assertThat(stage).isNull();
    }

    @Test
    public void shouldFindStageWithJobsAndTransitionsByIdentifier() throws Exception {
        PipelineConfig pipelineConfig = twoBuildPlansWithResourcesAndMaterials("pipeline-1", "stage-1");

        Pipeline pipeline = pipelineWithOnePassedAndOneCurrentlyRunning(pipelineConfig)[0];
        Stage expect = pipeline.getFirstStage();
        Stage stage = stageDao.findStageWithIdentifier(new StageIdentifier(pipeline, expect));
        assertThat(stage.getId()).isEqualTo(expect.getId());
        assertThat(stage.getIdentifier()).isEqualTo(new StageIdentifier(pipeline, stage));
        assertThat(stage.getJobInstances().size()).isEqualTo(2);

        for (JobInstance job : stage.getJobInstances()) {
            assertThat(job.getIdentifier()).isEqualTo(new JobIdentifier(pipeline, stage, job));
            assertThat(job.getTransitions().size()).isGreaterThan(0);
            assertThat(job.getTransitions().first().getCurrentState()).isEqualTo(JobState.Scheduled);
        }
    }

    @Test
    public void shouldFindStageEvenMultiplePipelinesWithSameLabel() throws Exception {
        mingleConfig.setLabelTemplate("fixed-label");
        Pipeline pipeline = pipelineWithFirstStagePassed(mingleConfig);
        Stage expect = pipeline.getFirstStage();
        Stage stage = stageDao.findStageWithIdentifier(new StageIdentifier(pipeline, expect));
        assertThat(stage.getId()).isEqualTo(expect.getId());
    }

    @Test
    public void shouldReturnNullStageWhenStageNotExist() {
        assertThat(stageDao.findStageWithIdentifier(new StageIdentifier("no-pipeline", null, "1", "no-stage", "1"))).isInstanceOf(NullStage.class);
    }

    @Test
    public void shouldReturnNoStageConfigWhenNoBuildsExist() {
        Stage completed = stageDao.mostRecentWithBuilds(CaseInsensitiveString.str(mingleConfig.name()), mingleConfig.get(0));
        assertThat(completed.getId()).isEqualTo(-1L);
    }

    @Test
    public void shouldGetMostRecentlyCompletedAndIncompleteWhenThereAreMulipleCompletedBuildInstances() {
        dbHelper.pass(dbHelper.schedulePipeline(mingleConfig, new TimeProvider()));
        dbHelper.pass(dbHelper.schedulePipeline(mingleConfig, new TimeProvider()));
        Pipeline running = pipelineWithFirstStageRunning(mingleConfig);

        Stage completed = stageDao.mostRecentWithBuilds(CaseInsensitiveString.str(mingleConfig.name()), mingleConfig.get(0));
        verifyBuildInstancesWithoutCaringAboutTransitions(STAGE_DEV, completed);
    }

    private void verifyBuildInstancesWithoutCaringAboutTransitions(String stageName, Stage instance) {
        assertThat(instance.getName()).isEqualTo(stageName);
        assertThat(instance.getId() > 0).isTrue();
        JobInstances instances = instance.getJobInstances();
        assertThat(instances.size()).isEqualTo(2);
        JobInstance nixJob = instances.get(0);
        assertThat(nixJob.getName()).isEqualTo("NixBuild");
        JobInstance winJob = instances.get(1);
        assertThat(winJob.getName()).isEqualTo("WinBuild");
        assertThat(nixJob.getState()).isEqualTo(JobState.Completed);
    }


    private JobInstance scheduleBuildInstances(Stage scheduledInstance) {
        JobInstances scheduledBuilds = scheduledInstance.getJobInstances();
        JobInstance bi = scheduledBuilds.first();
        bi.schedule();
        jobInstanceDao.updateStateAndResult(bi);
        bi = scheduledBuilds.get(1);
        bi.completing(JobResult.Passed);
        bi.completed(new Date());
        jobInstanceDao.updateStateAndResult(bi);
        return bi;
    }

    private JobInstances assignBuildInstances(Stage scheduledStage, Stage completedStage) {
        JobInstances completed = completedStage.getJobInstances();
        for (JobInstance instance : scheduledStage.getJobInstances()) {
            String oldAgentUuid = completed.getByName(instance.getName()).getAgentUuid();
            instance.assign(oldAgentUuid, new Date());
            jobInstanceDao.updateAssignedInfo(instance);
        }
        return completed;
    }

    @Test
    public void shouldSaveStageWithCreatedTimeAndAllItsBuilds() {
        Pipeline pipeline = dbHelper.schedulePipeline(mingleConfig, new TimeProvider());
        Stage stage = pipelineAndFirstStageOf(pipeline).stage;
        Stage actualStage = stageDao.stageById(stage.getId());
        assertThat(actualStage.getCreatedTime()).isNotNull();
        JobInstances actualJobs = actualStage.getJobInstances();
        assertThat(actualJobs.size()).isEqualTo(2);
        for (JobInstance job : actualJobs) {
            DatabaseAccessHelper.assertIsInserted(job.getId());
            assertForeignKey(actualStage.getId(), job.getStageId());
        }
    }

    @Test
    public void shouldGetTheCreatedAndCompletedTimeOfACompletedStage() {
        Clock clock = mock(Clock.class);
        Date date = new Date();
        when(clock.currentTimeMillis()).thenReturn(date.getTime());
        when(clock.currentTime()).thenReturn(date);
        Pipeline pipeline = dbHelper.schedulePipeline(custom("pipeline", "stage", new JobConfigs(new JobConfig("job")), new MaterialConfigs(MaterialConfigsMother.hgMaterialConfig())), clock);
        Stage actualStage = stageDao.stageById(pipelineAndFirstStageOf(pipeline).stage.getId());
        assertThat(actualStage.completedDate()).isNull();
        assertThat(actualStage.getLastTransitionedTime().getTime()).isEqualTo(actualStage.getJobInstances().first().getTransition(JobState.Scheduled).getStateChangeTime().getTime());

        dbHelper.pass(pipeline);

        actualStage = stageDao.stageById(pipelineAndFirstStageOf(pipeline).stage.getId());

        assertThat(actualStage.scheduledDate()).isEqualTo(date);
        assertThat(actualStage.getJobInstances().first().getTransition(JobState.Scheduled).getStateChangeTime()).isEqualTo(date);
        assertThat(actualStage.completedDate()).isEqualTo(actualStage.getJobInstances().last().getTransition(JobState.Completed).getStateChangeTime());
    }

    @Test
    public void shouldGetPreviousStageDurations() throws Exception {
        Pipeline completed = dbHelper.schedulePipeline(mingleConfig, new TimeProvider());
        dbHelper.pass(completed);
        Pipeline scheduled = dbHelper.schedulePipeline(mingleConfig, new TimeProvider());
        assignBuildInstances(pipelineAndFirstStageOf(scheduled).stage, pipelineAndFirstStageOf(completed).stage);
        Long duration = stageDao.getDurationOfLastSuccessfulOnAgent(CaseInsensitiveString.str(mingleConfig.name()), STAGE_DEV, scheduled.getFirstStage().getJobInstances().get(0));
        assertThat(duration).isGreaterThan(0L);
    }

    @Test
    public void shouldGetCount() throws Exception {
        Pipeline[] pipelines = pipelineWithOnePassedAndOneCurrentlyRunning(mingleConfig);
        Pipeline completed = pipelines[0];

        assertThat(stageDao.getCount(completed.getName(), STAGE_DEV)).isEqualTo(2);
    }

    @Test
    public void shouldGetStagesByPipelineId() throws Exception {
        Pipeline[] pipelines = pipelineWithOnePassedAndOneCurrentlyRunning(mingleConfig);
        Pipeline completed = pipelines[0];
        ignoreFirstBuildInFirstStage(completed);

        Stages stages = stageDao.getStagesByPipelineId(completed.getId());
        assertThat(stages.size()).isEqualTo(1);
        assertThat(stages.first().getPipelineId()).isEqualTo(completed.getId());
        assertThat(stages.first().getJobInstances().size()).isEqualTo(1);
    }

    @Test
    public void shouldGetLatestStageInstancesByPipelineId() throws Exception {
        Pipeline[] pipelines = pipelineWithOnePassedAndOneCurrentlyRunning(mingleConfig);
        Pipeline completed = pipelines[0];

        rerunFirstStage(completed);

        Stages stages = stageDao.getStagesByPipelineId(completed.getId());
        assertThat(stages.size()).isEqualTo(1);
        assertThat(stages.first().getPipelineId()).isEqualTo(completed.getId());
        assertThat(stages.first().getJobInstances().size()).isEqualTo(2);
    }

    @Test
    public void shouldContainIdentifierAfterSaved() throws Exception {
        Pipeline pipeline = dbHelper.schedulePipeline(mingleConfig, new TimeProvider());
        dbHelper.pass(pipeline);
        Stage stage = instanceFactory.createStageInstance(mingleConfig.getFirstStageConfig(), new DefaultSchedulingContext("anyone"), md5, new TimeProvider());
        stage.building();

        Stage savedStage = stageDao.save(pipeline, stage);
        assertThat(savedStage.getIdentifier()).isEqualTo(new StageIdentifier(pipeline, savedStage));
    }

    @Test
    public void shouldSet_StageContainsRerunJobs_FlagAsInferred() throws Exception {
        Pipeline pipeline = dbHelper.schedulePipeline(mingleConfig, new TimeProvider());
        dbHelper.pass(pipeline);
        Stage stage = instanceFactory.createStageInstance(mingleConfig.getFirstStageConfig(), new DefaultSchedulingContext("anyone"), md5, new TimeProvider());
        stage.building();

        Stage savedStage = stageDao.save(pipeline, stage);
        saveJobsFor(savedStage, pipeline);

        assertThat(savedStage.hasRerunJobs()).isFalse();
        assertThat(stageDao.stageById(savedStage.getId()).hasRerunJobs()).isFalse();

        Stage rerunStage = instanceFactory.createStageForRerunOfJobs(stage, a(stage.getJobInstances().get(0).getName()), new DefaultSchedulingContext("foo"), mingleConfig.getFirstStageConfig(), new TimeProvider(), "md5");
        savedStage = stageDao.save(pipeline, rerunStage);
        saveJobsFor(savedStage, pipeline);
        assertThat(rerunStage.hasRerunJobs()).isTrue();
        assertThat(stageDao.findStageWithIdentifier(savedStage.getIdentifier()).hasRerunJobs()).isTrue();
    }

    private void saveJobsFor(Stage stage, final Pipeline pipeline) {
        JobInstances jobInstances = stage.getJobInstances();
        for (JobInstance job : jobInstances) {
            jobInstanceDao.save(stage.getId(), job);
        }

        for (JobInstance jobInstance : jobInstances) {
            jobInstance.setIdentifier(new JobIdentifier(pipeline, stage, jobInstance));
        }
    }

    @Test
    public void shouldMarkPreviousRunAsNotLatestWhenSavingALaterOne() throws Exception {
        Pipeline pipeline = dbHelper.schedulePipeline(mingleConfig, new TimeProvider());
        dbHelper.pass(pipeline);
        Stage firstRunOfStage = pipeline.getStages().get(0);
        assertThat(firstRunOfStage.isLatestRun()).isTrue();
        assertThat(stageDao.findStageWithIdentifier(firstRunOfStage.getIdentifier()).isLatestRun()).isTrue();
        Stage stage = instanceFactory.createStageInstance(mingleConfig.getFirstStageConfig(), new DefaultSchedulingContext("anyone"), md5, new TimeProvider());
        stage.building();

        Stage rerun = stageDao.save(pipeline, stage);
        assertThat(stageDao.findStageWithIdentifier(firstRunOfStage.getIdentifier()).isLatestRun()).isFalse();
        assertThat(rerun.isLatestRun()).isTrue();
        assertThat(stageDao.findStageWithIdentifier(rerun.getIdentifier()).isLatestRun()).isTrue();
    }

    private Stage rerunFirstStage(Pipeline pipeline) {
        Stage firstStage = pipeline.getFirstStage();
        Stage newInstance = instanceFactory.createStageInstance(mingleConfig.findBy(new CaseInsensitiveString(firstStage.getName())), new DefaultSchedulingContext("anyone"), md5, new TimeProvider());
        return stageDao.saveWithJobs(pipeline, newInstance);
    }

    private void ignoreFirstBuildInFirstStage(Pipeline completed) {
        JobInstance ignored = completed.getStages().first().getJobInstances().first();
        jobInstanceDao.ignore(ignored);
    }

    @Test
    public void shouldReturnTrueIfAnyStageIsActive() throws Exception {
        dbHelper.schedulePipeline(mingleConfig, new TimeProvider());
        assertThat(stageDao.isStageActive(CaseInsensitiveString.str(mingleConfig.name()), CaseInsensitiveString.str(mingleConfig.getFirstStageConfig().name()))).isTrue();
    }

    @Test
    public void isStageActive_shouldCacheTheResultAfterFirstExecution() {
        SqlMapClientTemplate mockTemplate = mock(SqlMapClientTemplate.class);
        stageDao.setSqlMapClientTemplate(mockTemplate);

        when(mockTemplate.queryForObject(eq("isStageActive"), any())).thenReturn(1);

        boolean stageActive = stageDao.isStageActive(CaseInsensitiveString.str(mingleConfig.name()), CaseInsensitiveString.str(mingleConfig.getFirstStageConfig().name()));
        assertThat(stageActive).isTrue();
        stageActive = stageDao.isStageActive(CaseInsensitiveString.str(mingleConfig.name()), CaseInsensitiveString.str(mingleConfig.getFirstStageConfig().name()));
        assertThat(stageActive).isTrue();

        verify(mockTemplate, times(1)).queryForObject(eq("isStageActive"), any());
    }

    @Test
    public void isStageActive_shouldMakeIsActiveFalseWhenTheStageCompletes() {
        SqlMapClientTemplate mockTemplate = mock(SqlMapClientTemplate.class);
        stageDao.setSqlMapClientTemplate(mockTemplate);

        when(mockTemplate.queryForObject(eq("isStageActive"), any())).thenReturn(1).thenReturn(0);

        boolean stageActive = stageDao.isStageActive(CaseInsensitiveString.str(mingleConfig.name()), CaseInsensitiveString.str(mingleConfig.getFirstStageConfig().name()));
        assertThat(stageActive).isTrue();

        Stage stage = StageMother.completedFailedStageInstance(CaseInsensitiveString.str(mingleConfig.name()), CaseInsensitiveString.str(mingleConfig.getFirstStageConfig().name()), "job");
        stageDao.stageStatusChanged(stage);//The cached 'true' should now be removed

        assertThat(stageDao.isStageActive(CaseInsensitiveString.str(mingleConfig.name()), CaseInsensitiveString.str(mingleConfig.getFirstStageConfig().name()))).isFalse();
        verify(mockTemplate, times(2)).queryForObject(eq("isStageActive"), any());
    }


    @Test
    public void shouldReturnFalseIfNoStagesAreActive() throws Exception {
        Pipeline passed = dbHelper.schedulePipeline(mingleConfig, new TimeProvider());
        dbHelper.pass(passed);
        setupRescheduledBuild(passed);
        assertThat(stageDao.isStageActive(CaseInsensitiveString.str(mingleConfig.name()), CaseInsensitiveString.str(mingleConfig.getFirstStageConfig().name()))).isFalse();
    }

    @Test
    public void discontinuedBuildShouldNotBeConsideredAsActive() throws Exception {
        Pipeline passed = dbHelper.schedulePipeline(mingleConfig, new TimeProvider());
        dbHelper.pass(passed);
        setupDiscontinuedBuild(passed);
        assertThat(stageDao.isStageActive(CaseInsensitiveString.str(mingleConfig.name()), CaseInsensitiveString.str(mingleConfig.getFirstStageConfig().name()))).isFalse();
    }

    @Test
    public void shouldHaveResultUnknownByDefault() throws Exception {
        Pipeline pipeline = dbHelper.schedulePipeline(mingleConfig, new TimeProvider());
        Stage firstStage = pipeline.getFirstStage();
        assertThat(firstStage.getResult()).isEqualTo(StageResult.Unknown);
    }

    @Test
    public void shouldLoadStageById() throws Exception {
        Pipeline pipeline = dbHelper.schedulePipeline(mingleConfig, new TimeProvider());
        Stage firstStage = pipeline.getFirstStage();
        updateResultInTransaction(firstStage, StageResult.Passed);
        Stage stage = stageDao.stageById(firstStage.getId());
        assertThat(stage.getResult()).isEqualTo(StageResult.Passed);
        assertThat(stage.getIdentifier()).isEqualTo(new StageIdentifier(pipeline.getName(), pipeline.getCounter(), pipeline.getLabel(),
            stage.getName(), String.valueOf(stage.getCounter())));

        for (JobInstance jobInstance : stage.getJobInstances()) {
            assertThat(jobInstance.getIdentifier()).isEqualTo(new JobIdentifier(pipeline, stage, jobInstance));
        }
    }

    @Test
    public void shouldReturnZeorAsMaxCountWhenStageIsNotExist() throws Exception {
        assertThat(stageDao.getMaxStageCounter(1, "not-exist")).isEqualTo(0);
    }

    @Test
    public void shouldReturnMaxCount() throws Exception {
        Pipeline pipeline = dbHelper.schedulePipeline(mingleConfig, new TimeProvider());
        assertThat(stageDao.getMaxStageCounter(pipeline.getId(), CaseInsensitiveString.str(mingleConfig.first().name()))).isEqualTo(1);
    }

    @Test
    public void shouldReturnMaxStageCounterByPipelineCounter() throws Exception {
        Pipeline pipeline = dbHelper.schedulePipeline(mingleConfig, new TimeProvider());
        PipelineIdentifier pipelineIdentifier = new PipelineIdentifier(pipeline.getName(), pipeline.getCounter());
        assertThat(stageDao.findLatestStageCounter(pipelineIdentifier, CaseInsensitiveString.str(mingleConfig.first().name()))).isEqualTo(1);
    }

    @Test
    public void shouldReturnAllCompletedStagesForTheGivenPipeline() throws SQLException {
        PipelineAndStage[] cruiseStages = run4Pipelines("cruise");
        run4Pipelines("mingle");

        List<FeedEntry> completedStages = new ArrayList<>(stageDao.findCompletedStagesFor("cruise", FeedModifier.Latest, -1, 5));
        StageFeedEntry latestFeedEntry = (StageFeedEntry) completedStages.get(0);

        assertThat(completedStages.size()).isEqualTo(4);
        assertFeed(completedStages.get(0), cruiseStages[3].stage);
        assertFeed(completedStages.get(1), cruiseStages[2].stage);
        assertFeed(completedStages.get(2), cruiseStages[1].stage);
        assertFeed(completedStages.get(3), cruiseStages[0].stage);

        assertThat(latestFeedEntry.getResult()).isEqualTo(StageResult.Failed.name());
        assertThat(latestFeedEntry.getPipelineId()).isEqualTo(cruiseStages[3].stage.getPipelineId());
    }

    @Test
    public void findCompletedStagesFor_shouldInvalidateCacheOnCompletionOfAStageForTheGivenPipeline() {
        SqlMapClientTemplate mockTemplate = mock(SqlMapClientTemplate.class);
        stageDao.setSqlMapClientTemplate(mockTemplate);

        List<StageFeedEntry> entries = asList(new StageFeedEntry(1L, 1L, null, 1L, null, null));
        when(mockTemplate.queryForList(eq("allCompletedStagesForPipeline"), any())).thenReturn((List) entries);

        stageDao.findCompletedStagesFor("cruise", FeedModifier.Latest, 10, 100);

        updateResultInTransaction(StageMother.completedFailedStageInstance("cruise", "stage", "job"), StageResult.Failed);

        List<FeedEntry> actual = new ArrayList<>(stageDao.findCompletedStagesFor("cruise", FeedModifier.Latest, 10, 100));
        assertThat(actual).isEqualTo(entries);

        verify(mockTemplate, times(2)).queryForList(eq("allCompletedStagesForPipeline"), any());
    }

    @Test
    public void shouldLoadApproverAndUnderstandIfBuildWasForced() throws SQLException {
        mingleConfig.get(0).updateApproval(Approval.manualApproval());
        Pipeline cancelled = dbHelper.schedulePipeline(mingleConfig, ModificationsMother.modifySomeFiles(mingleConfig), "loser", new TimeProvider());
        dbHelper.cancelStage(pipelineAndFirstStageOf(cancelled).stage);

        mingleConfig.get(0).updateApproval(Approval.automaticApproval());
        Pipeline passed = dbHelper.schedulePipeline(mingleConfig, ModificationsMother.modifySomeFiles(mingleConfig), "boozer", new TimeProvider());
        dbHelper.passStage(pipelineAndFirstStageOf(passed).stage);

        Pipeline failed = dbHelper.schedulePipeline(mingleConfig, new TimeProvider());
        dbHelper.failStage(pipelineAndFirstStageOf(failed).stage);

        List<StageFeedEntry> completedStages = stageDao.findCompletedStagesFor(mingleConfig.name().toString(), FeedModifier.Before, transitionId(pipelineAndFirstStageOf(failed).stage), 2);

        assertThat(completedStages.get(0).isManuallyTriggered()).isFalse();
        assertThat(completedStages.get(0).getApprovedBy()).isEqualTo("boozer");

        assertThat(completedStages.get(1).isManuallyTriggered()).isTrue();
        assertThat(completedStages.get(1).getApprovedBy()).isEqualTo("loser");
    }

    @Test
    public void shouldReturnAllCompletedStagesBeforeTheGivenPipeline() throws SQLException {
        PipelineAndStage[] cruiseStages = run4Pipelines("cruise");
        run4Pipelines("mingle");

        //Page size 1
        List<StageFeedEntry> completedStages = stageDao.findCompletedStagesFor("cruise", FeedModifier.Before, transitionId(cruiseStages[3].stage), 1);

        assertThat(completedStages.size()).isEqualTo(1);
        assertFeed(completedStages.get(0), cruiseStages[2].stage);

        //Page size 2
        completedStages = stageDao.findCompletedStagesFor("cruise", FeedModifier.Before, transitionId(cruiseStages[3].stage), 2);

        assertThat(completedStages.size()).isEqualTo(2);
        assertFeed(completedStages.get(0), cruiseStages[2].stage);
        assertFeed(completedStages.get(1), cruiseStages[1].stage);

        //Page size 3
        completedStages = stageDao.findCompletedStagesFor("cruise", FeedModifier.Before, transitionId(cruiseStages[2].stage), 3);

        assertThat(completedStages.size()).isEqualTo(2);
        assertFeed(completedStages.get(0), cruiseStages[1].stage);
        assertFeed(completedStages.get(1), cruiseStages[0].stage);
    }

    @Test
    public void shouldFindAllCompletedStages() throws SQLException {
        PipelineAndStage[] stages = run4Pipelines();

        List<FeedEntry> completedStages = new ArrayList<>(stageDao.findAllCompletedStages(FeedModifier.Latest, -1, 5));

        assertThat(completedStages.size()).isEqualTo(4);
        assertFeed(completedStages.get(0), stages[3].stage);
        assertFeed(completedStages.get(1), stages[2].stage);
    }

    @Test
    public void shouldFindAllCompletedStagesBeforeAGivenStage() throws SQLException {
        PipelineAndStage[] stages = run4Pipelines();

        List<FeedEntry> completedStages = new ArrayList<>(stageDao.findAllCompletedStages(FeedModifier.Before, transitionId(stages[3].stage), 2));

        assertThat(completedStages.size()).isEqualTo(2);
        assertFeed(completedStages.get(0), stages[2].stage);
        assertFeed(completedStages.get(1), stages[1].stage);
    }

    @Test
    public void shouldFindStagesBetween() throws SQLException {
        PipelineConfig config = PipelineMother.createPipelineConfig("pipeline", new MaterialConfigs(MaterialConfigsMother.hgMaterialConfig()), "firstStage", "secondStage");
        Pipeline pipeline0 = dbHelper.newPipelineWithAllStagesPassed(config);
        dbHelper.updateNaturalOrder(pipeline0.getId(), 4.0);

        //First run Failed, Rerun Passed
        Pipeline pipeline1 = dbHelper.newPipelineWithFirstStagePassed(config);
        Stage stage = dbHelper.scheduleStage(pipeline1, config.get(1));
        dbHelper.failStage(stage);
        stage = dbHelper.scheduleStage(pipeline1, config.get(1));
        dbHelper.passStage(stage);
        dbHelper.updateNaturalOrder(pipeline1.getId(), 5.0);

        Pipeline pipeline2 = dbHelper.newPipelineWithFirstStagePassed(config);
        stage = dbHelper.scheduleStage(pipeline2, config.get(1));
        dbHelper.failStage(stage);
        dbHelper.updateNaturalOrder(pipeline2.getId(), 6.0);

        Pipeline pipeline3 = dbHelper.newPipelineWithFirstStagePassed(config);
        dbHelper.updateNaturalOrder(pipeline3.getId(), 7.0);

        Pipeline pipeline4 = dbHelper.newPipelineWithFirstStagePassed(config);
        stage = dbHelper.scheduleStage(pipeline4, config.get(1));
        dbHelper.cancelStage(stage);
        dbHelper.updateNaturalOrder(pipeline4.getId(), 8.0);

        Pipeline pipeline5 = dbHelper.newPipelineWithFirstStagePassed(config);
        dbHelper.scheduleStage(pipeline5, config.get(1));
        dbHelper.updateNaturalOrder(pipeline5.getId(), 9.0);

        //First run passed, rerun failed.
        Pipeline pipeline6 = dbHelper.newPipelineWithAllStagesPassed(config);
        stage = dbHelper.scheduleStage(pipeline6, config.get(1));
        dbHelper.failStage(stage);
        dbHelper.updateNaturalOrder(pipeline6.getId(), 10.0);

        Pipeline pipeline7 = dbHelper.newPipelineWithFirstStagePassed(config);
        stage = dbHelper.scheduleStage(pipeline7, config.get(1));
        dbHelper.failStage(stage);
        dbHelper.updateNaturalOrder(pipeline7.getId(), 11.0);

        pipeline7 = pipelineDao.loadPipeline(pipeline7.getId());
        pipeline6 = pipelineDao.loadPipeline(pipeline6.getId());
        pipeline4 = pipelineDao.loadPipeline(pipeline4.getId());
        pipeline2 = pipelineDao.loadPipeline(pipeline2.getId());

        List<StageIdentifier> list = stageDao.findFailedStagesBetween("pipeline", "secondStage", 5.0, 11.0);

        assertThat(list.size()).isEqualTo(3);
        StageIdentifier identifier = list.get(0);
        assertThat(identifier).isEqualTo(new StageIdentifier("pipeline", 8, "secondStage", "1"));
        assertThat(identifier).isEqualTo(pipeline7.findStage("secondStage").getIdentifier());
        assertThat(list.get(1)).isEqualTo(pipeline6.findStage("secondStage").getIdentifier());
        assertThat(list.get(2)).isEqualTo(pipeline2.findStage("secondStage").getIdentifier());

        list = stageDao.findFailedStagesBetween("pipeline", "secondStage", 5.0, 10.0);
        assertThat(list.size()).isEqualTo(2);
        assertThat(list.get(0)).isEqualTo(pipeline6.findStage("secondStage").getIdentifier());
        assertThat(list.get(1)).isEqualTo(pipeline2.findStage("secondStage").getIdentifier());

        list = stageDao.findFailedStagesBetween("pipeline", "secondStage", 5.0, 9.0);
        assertThat(list.size()).isEqualTo(1);
        assertThat(list.get(0)).isEqualTo(pipeline2.findStage("secondStage").getIdentifier());

        list = stageDao.findFailedStagesBetween("pipeline", "secondStage", 5.0, 4.0);
        assertThat(list.size()).isEqualTo(0);
    }

    @Test
    public void shouldFindRerunStagesWhenFindStagesBetween() throws SQLException {
        Pipeline pipeline = dbHelper.newPipelineWithFirstStageFailed(mingleConfig);
        dbHelper.updateNaturalOrder(pipeline.getId(), 5.0);
        Stage rerunedStage = rerunFirstStage(pipeline);
        dbHelper.failStage(rerunedStage);

        List<StageIdentifier> list = stageDao.findFailedStagesBetween(PIPELINE_NAME, STAGE_DEV, 3.0, 5.0);
        assertThat(list.size()).isEqualTo(1);
        assertThat(list.get(0)).isEqualTo(rerunedStage.getIdentifier());
    }

    @Test
    public void shouldFindStagesBetweenAtTheBeginingOfAPipeline() throws SQLException {
        PipelineConfig config = PipelineMother.createPipelineConfig("pipeline", new MaterialConfigs(MaterialConfigsMother.hgMaterialConfig()), "firstStage", "secondStage");

        Pipeline pipeline1 = dbHelper.newPipelineWithFirstStagePassed(config);
        dbHelper.updateNaturalOrder(pipeline1.getId(), 1.0);

        Pipeline pipeline2 = dbHelper.newPipelineWithFirstStagePassed(config);
        Stage stage = dbHelper.scheduleStage(pipeline2, config.get(1));
        dbHelper.failStage(stage);
        dbHelper.updateNaturalOrder(pipeline2.getId(), 2.0);

        Pipeline pipeline3 = dbHelper.newPipelineWithFirstStagePassed(config);
        dbHelper.updateNaturalOrder(pipeline3.getId(), 1.5);

        Pipeline pipeline4 = dbHelper.newPipelineWithFirstStagePassed(config);
        stage = dbHelper.scheduleStage(pipeline4, config.get(1));
        dbHelper.cancelStage(stage);
        dbHelper.updateNaturalOrder(pipeline4.getId(), 0.5);

        Pipeline pipeline7 = dbHelper.newPipelineWithFirstStagePassed(config);
        stage = dbHelper.scheduleStage(pipeline7, config.get(1));
        dbHelper.failStage(stage);
        dbHelper.updateNaturalOrder(pipeline7.getId(), 11.0);

        pipeline7 = pipelineDao.loadPipeline(pipeline7.getId());
        pipeline4 = pipelineDao.loadPipeline(pipeline4.getId());
        pipeline2 = pipelineDao.loadPipeline(pipeline2.getId());

        List<StageIdentifier> list = stageDao.findFailedStagesBetween("pipeline", "secondStage", 0.0, 11.0);
        assertThat(list.size()).isEqualTo(2);
        assertThat(list.get(0)).isEqualTo(pipeline7.findStage("secondStage").getIdentifier());
        assertThat(list.get(1)).isEqualTo(pipeline2.findStage("secondStage").getIdentifier());
    }

    @Test
    public void shouldReturnAllTheStagesOfAGivenPipeline() throws SQLException {
        StageConfig first = StageConfigMother.custom("first", "job1");
        StageConfig second = StageConfigMother.custom("second", "job1");
        StageConfig third = StageConfigMother.custom("third", "job1");
        Pipeline pipeline = dbHelper.newPipelineWithAllStagesPassed(PipelineConfigMother.pipelineConfig("pipeline", first, second, third));
        Stage stage = dbHelper.scheduleStage(pipeline, second);
        Stages stages = stageDao.findAllStagesFor("pipeline", 1);
        assertThat(stages.size()).isEqualTo(4);
        Stages pipelineStages = pipeline.getStages();
        assertThat(stages).isEqualTo(asList(stage, pipelineStages.get(0), pipelineStages.get(1), pipelineStages.get(2)));
    }

    @Test
    public void shouldCacheAllStagesForAPipelineInstance() {
        SqlMapClientTemplate mockTemplate = mock(SqlMapClientTemplate.class);
        stageDao.setSqlMapClientTemplate(mockTemplate);

        Stage stage1 = StageMother.passedStageInstance("first", "job", "pipeline");
        Stage stage2 = StageMother.passedStageInstance("second", "job", "pipeline");
        List<Stage> stages = asList(stage1, stage2);
        when(mockTemplate.queryForList("getStagesByPipelineNameAndCounter", arguments("pipelineName", "pipeline").and("pipelineCounter", 1).asMap())).thenReturn((List) stages);

        Stages actual = stageDao.findAllStagesFor("pipeline", 1);
        assertThat(actual).isEqualTo(new Stages(stages));
        actual = stageDao.findAllStagesFor("pipeline", 1); //Should return from cache
        assertThat(actual).isEqualTo(new Stages(stages));

        verify(mockTemplate, times(1)).queryForList("getStagesByPipelineNameAndCounter", arguments("pipelineName", "pipeline").and("pipelineCounter", 1).asMap());
    }

    @Test
    public void shouldInvalidateCachedAllStagesForAPipelineInstanceWhenTheStatusOfAStageOfThisPipelineInstanceIsChanged() {
        SqlMapClientTemplate mockTemplate = mock(SqlMapClientTemplate.class);
        stageDao.setSqlMapClientTemplate(mockTemplate);

        Stage stage1 = StageMother.passedStageInstance("first", "job", "pipeline");
        Stage stage2 = StageMother.passedStageInstance("second", "job", "pipeline");
        List<Stage> stages = asList(stage1, stage2);
        when(mockTemplate.queryForList("getStagesByPipelineNameAndCounter", arguments("pipelineName", "pipeline").and("pipelineCounter", 1).asMap())).thenReturn((List) stages);

        Stages actual = stageDao.findAllStagesFor("pipeline", 1);
        assertThat(actual).isEqualTo(new Stages(stages));

        stageDao.stageStatusChanged(StageMother.scheduledStage("pipeline", 1, "first", 2, "job"));//Should invalidate the cached stages

        actual = stageDao.findAllStagesFor("pipeline", 1);
        assertThat(actual).isEqualTo(new Stages(stages));

        verify(mockTemplate, times(2)).queryForList("getStagesByPipelineNameAndCounter", arguments("pipelineName", "pipeline").and("pipelineCounter", 1).asMap());
    }

    @Test
    public void shouldNotInvalidateCachedAllStagesForAPipelineInstanceWhenTheStatusOfAStageOfDifferentPipelineInstance() {
        SqlMapClientTemplate mockTemplate = mock(SqlMapClientTemplate.class);
        stageDao.setSqlMapClientTemplate(mockTemplate);

        Stage stage1 = StageMother.passedStageInstance("first", "job", "pipeline");
        Stage stage2 = StageMother.passedStageInstance("second", "job", "pipeline");
        List<Stage> stages = asList(stage1, stage2);
        when(mockTemplate.queryForList("getStagesByPipelineNameAndCounter", arguments("pipelineName", "pipeline").and("pipelineCounter", 1).asMap())).thenReturn((List) stages);

        Stages actual = stageDao.findAllStagesFor("pipeline", 1);
        assertThat(actual).isEqualTo(new Stages(stages));

        stageDao.stageStatusChanged(StageMother.scheduledStage("pipeline", 2, "first", 2, "job"));//Should not invalidate the cached stages as it is from a different Pipeline instance

        actual = stageDao.findAllStagesFor("pipeline", 1);
        assertThat(actual).isEqualTo(new Stages(stages));

        verify(mockTemplate, times(1)).queryForList("getStagesByPipelineNameAndCounter", arguments("pipelineName", "pipeline").and("pipelineCounter", 1).asMap());
    }

    @Test
    public void shouldLoadOldestStagesHavingArtifactsInBatchOf100() {
        Pipeline[] pipelines = new Pipeline[101];
        for (int i = 0; i < 101; i++) {
            Pipeline pipeline = dbHelper.schedulePipeline(PipelineConfigMother.createPipelineConfig("foo_" + i, "stage1", "job1"), new TimeProvider());
            dbHelper.pass(pipeline);
            pipelines[i] = pipeline;
        }
        List<Stage> stages = stageDao.oldestStagesHavingArtifacts();
        assertThat(stages.size()).isEqualTo(100);
        for (int i = 0; i < 100; i++) {
            Stage stage = stages.get(i);
            assertThat(stage.getIdentifier()).isEqualTo(pipelines[i].getFirstStage().getIdentifier());
            stageDao.markArtifactsDeletedFor(stage);
        }
        stages = stageDao.oldestStagesHavingArtifacts();
        assertThat(stages.size()).isEqualTo(1);
        stageDao.markArtifactsDeletedFor(stages.get(0));
        assertThat(stageDao.oldestStagesHavingArtifacts().size()).isEqualTo(0);
    }

    @Test
    public void shouldOnlyLoadCompletedStagesAsOldestStagesHavingArtifacts() {
        Pipeline pipeline = dbHelper.schedulePipeline(PipelineConfigMother.createPipelineConfig("foo", "stage1", "job1"), new TimeProvider());
        List<Stage> stages = stageDao.oldestStagesHavingArtifacts();
        assertThat(stages.size()).isEqualTo(0);
        dbHelper.pass(pipeline);
        stages = stageDao.oldestStagesHavingArtifacts();
        assertThat(stages.size()).isEqualTo(1);
    }

    @Test
    public void shouldUpdateConfigVersionWhenStageIsSaved() throws Exception {
        Stage stage = StageMother.scheduledStage("foo-pipeline", 1, "dev", 1, "java");
        stage.setConfigVersion("git-sha");
        Pipeline pipeline = PipelineMother.pipeline("foo-pipeline", stage);
        dbHelper.savePipelineWithStagesAndMaterials(pipeline);

        Stage reloaded = stageDao.stageById(stage.getId());

        assertThat(reloaded.getName()).isEqualTo("dev");
        assertThat(reloaded.getConfigVersion()).isEqualTo("git-sha");
    }

    @Test
    public void findStageHistoryPage_shouldReturnStageHistoryEntryWithConfigVersion() {
        SqlMapClientTemplate mockTemplate = mock(SqlMapClientTemplate.class);

        Stage stage = StageMother.passedStageInstance("dev", "java", "pipeline-name");
        stage.setApprovedBy("admin");
        stage.setConfigVersion("md5-test");

        stageDao.setSqlMapClientTemplate(mockTemplate);
        when(mockTemplate.queryForObject(eq("getStageHistoryCount"), any())).thenReturn(20);
        when(mockTemplate.queryForObject(eq("findOffsetForStage"), any())).thenReturn(10);
        List<StageHistoryEntry> stageList = asList(new StageHistoryEntry(stage, 1, 10));
        when(mockTemplate.queryForList(eq("findStageHistoryPage"), any())).thenReturn((List) stageList);

        StageHistoryPage stageHistoryPage = stageDao.findStageHistoryPage(stage, 10);

        assertThat(stageHistoryPage.getStages().get(0).getConfigVersion()).isEqualTo("md5-test");
    }

    @Test
    public void shouldLoadTheStageHistoryEntryNextInTimeFromAGivenStageHistoryEntry() throws Exception {
        HgMaterial hg = new HgMaterial("url", null);
        String[] hg_revs = {"h1"};
        scheduleUtil.checkinInOrder(hg, hg_revs);
        String pipelineName = "p1";
        String stageName = "stage_name";
        ScheduleTestUtil.AddedPipeline p1 = scheduleUtil.saveConfigWith(pipelineName, stageName, scheduleUtil.m(hg));
        for (int i = 0; i < 11; i++) {
            scheduleUtil.runAndPass(p1, "h1");
        }
        StageHistoryPage historyPage = stageDao.findStageHistoryPage(pipelineName, stageName, new Supplier<Pagination>() {
            @Override
            public Pagination get() {
                return Pagination.pageByNumber(2, 2, 10);
            }
        });
        StageHistoryEntry topOfSecondPage = historyPage.getStages().get(0);
        StageHistoryEntry bottomOfFirstPage = stageDao.findImmediateChronologicallyForwardStageHistoryEntry(topOfSecondPage);
        assertThat(bottomOfFirstPage.getId()).isEqualTo(topOfSecondPage.getId() + 1);
        assertThat(bottomOfFirstPage.getIdentifier().getPipelineName()).isEqualTo(pipelineName);
        assertThat(bottomOfFirstPage.getIdentifier().getStageName()).isEqualTo(stageName);
        assertThat(bottomOfFirstPage.getIdentifier().getPipelineCounter()).isEqualTo(2);
    }

    @Test
    public void shouldReturnNullStageHistoryEntryWhenGettingHistoryForPage1() throws Exception {
        HgMaterial hg = new HgMaterial("url", null);
        String[] hg_revs = {"h1"};
        scheduleUtil.checkinInOrder(hg, hg_revs);
        String pipelineName = "p1";
        String stageName = "stage_name";
        ScheduleTestUtil.AddedPipeline p1 = scheduleUtil.saveConfigWith(pipelineName, stageName, scheduleUtil.m(hg));
        for (int i = 0; i < 10; i++) {
            scheduleUtil.runAndPass(p1, "h1");
        }
        StageHistoryPage historyPage = stageDao.findStageHistoryPage(pipelineName, stageName, new Supplier<Pagination>() {
            @Override
            public Pagination get() {
                return Pagination.pageByNumber(1, 1, 10);
            }
        });
        StageHistoryEntry topOfSecondPage = historyPage.getStages().get(0);
        StageHistoryEntry bottomOfFirstPage = stageDao.findImmediateChronologicallyForwardStageHistoryEntry(topOfSecondPage);
        assertThat(bottomOfFirstPage).isNull();
    }

    @Test
    public void shouldLoadTheStageHistoryEntryNextInTimeFromAGivenStageHistoryEntryEvenThoughOtherStageInstanceAreInBetween() throws Exception {
        HgMaterial hg = new HgMaterial("url", null);
        String[] hg_revs = {"h1"};
        scheduleUtil.checkinInOrder(hg, hg_revs);
        String pipelineName = "p1";
        String anotherPipeline = "p2";
        String stageName = "stage_name";
        String anotherStage = "another_stage_name";
        ScheduleTestUtil.AddedPipeline p1 = scheduleUtil.saveConfigWith(pipelineName, stageName, scheduleUtil.m(hg));
        ScheduleTestUtil.AddedPipeline p2 = scheduleUtil.saveConfigWith(anotherPipeline, anotherStage, scheduleUtil.m(hg));
        for (int i = 0; i < 11; i++) {
            scheduleUtil.runAndPass(p1, "h1");
            scheduleUtil.runAndPass(p2, "h1");
        }
        StageHistoryPage historyPage = stageDao.findStageHistoryPage(pipelineName, stageName, new Supplier<Pagination>() {
            @Override
            public Pagination get() {
                return Pagination.pageByNumber(2, 2, 10);
            }
        });
        StageHistoryEntry topOfSecondPage = historyPage.getStages().get(0);
        StageHistoryEntry bottomOfFirstPage = stageDao.findImmediateChronologicallyForwardStageHistoryEntry(topOfSecondPage);
        assertThat(bottomOfFirstPage.getId()).isEqualTo(topOfSecondPage.getId() + 2);
        assertThat(bottomOfFirstPage.getIdentifier().getPipelineName()).isEqualTo(pipelineName);
        assertThat(bottomOfFirstPage.getIdentifier().getStageName()).isEqualTo(stageName);
        assertThat(bottomOfFirstPage.getIdentifier().getPipelineCounter()).isEqualTo(2);
    }

    @Test
    public void shouldCacheStageHistoryPageAndCountAndOffset() throws Exception {
        HgMaterial hg = new HgMaterial("url", null);
        String[] hg_revs = {"h1"};
        scheduleUtil.checkinInOrder(hg, hg_revs);
        String pipelineName = "p1";
        String stageName = "stage_name";
        ScheduleTestUtil.AddedPipeline p1 = scheduleUtil.saveConfigWith(pipelineName, stageName, scheduleUtil.m(hg));
        scheduleUtil.runAndPass(p1, "h1");

        Stage stage = stageDao.mostRecentStage(new StageConfigIdentifier(pipelineName, stageName));
        stageDao.findStageHistoryPage(stage, 10); // PRIME CACHE

        Method cacheKeyForStageHistories = getMethodViaReflection("cacheKeyForStageHistories", String.class, String.class);
        Method cacheKeyForStageCount = getMethodViaReflection("cacheKeyForStageCount", String.class, String.class);
        Method cacheKeyForStageOffset = getMethodViaReflection("cacheKeyForStageOffset", Stage.class);
        Object primedStageHistoryPage = goCache.get((String) cacheKeyForStageHistories.invoke(stageDao, pipelineName, stageName));
        Object primedStageHistoryCount = goCache.get((String) cacheKeyForStageCount.invoke(stageDao, pipelineName, stageName));
        Object primedStageHistoryOffset = goCache.get((String) cacheKeyForStageOffset.invoke(stageDao, stage), String.valueOf(stage.getId()));

        stageDao.findStageHistoryPage(stage, 10); // SHOULD RETURN FROM CACHE

        Object cachedStageHistoryPage = goCache.get((String) cacheKeyForStageHistories.invoke(stageDao, pipelineName, stageName));
        Object cachedStageHistoryCount = goCache.get((String) cacheKeyForStageCount.invoke(stageDao, pipelineName, stageName));
        Object cachedStageHistoryOffset = goCache.get((String) cacheKeyForStageOffset.invoke(stageDao, stage), String.valueOf(stage.getId()));

        assertThat(cachedStageHistoryPage).isSameAs(primedStageHistoryPage);
        assertThat(cachedStageHistoryCount).isSameAs(primedStageHistoryCount);
        assertThat(cachedStageHistoryOffset).isSameAs(primedStageHistoryOffset);
    }

    @Test
    public void shouldInvalidateStageHistoryCachesOnStageSave() throws Exception {
        HgMaterial hg = new HgMaterial("url", null);
        String[] hg_revs = {"h1"};
        scheduleUtil.checkinInOrder(hg, hg_revs);
        String pipelineName = "p1";
        String stageName = "stage_name";
        ScheduleTestUtil.AddedPipeline p1 = scheduleUtil.saveConfigWith(pipelineName, stageName, scheduleUtil.m(hg));
        scheduleUtil.runAndPass(p1, "h1");

        Stage stage = stageDao.mostRecentStage(new StageConfigIdentifier(pipelineName, stageName));
        stageDao.findStageHistoryPage(stage, 10); // PRIME CACHE

        CacheEventListener listener = mock(CacheEventListener.class);
        goCache.addListener(listener);

        scheduleUtil.runAndPass(p1, "h1"); // NEW RUN OF STAGE, CACHE SHOULD BE INVALIDATED

        stage = stageDao.mostRecentStage(new StageConfigIdentifier(pipelineName, stageName));
        stageDao.findStageHistoryPage(stage, 10); // SHOULD QUERY AGAIN

        ArgumentCaptor<Element> elementRemovedCaptor = ArgumentCaptor.forClass(Element.class);
        ArgumentCaptor<Element> elementAddedCaptor = ArgumentCaptor.forClass(Element.class);
        verify(listener, atLeastOnce()).notifyElementRemoved(any(), elementRemovedCaptor.capture());
        verify(listener, atLeastOnce()).notifyElementPut(any(), elementAddedCaptor.capture());

        List<Serializable> keysThatWereRemoved = new ArrayList<>();
        List<Serializable> keysThatWereAdded = new ArrayList<>();

        for (Element element : elementRemovedCaptor.getAllValues()) {
            keysThatWereRemoved.add(element.getKey());
        }

        for (Element element : elementAddedCaptor.getAllValues()) {
            keysThatWereAdded.add(element.getKey());
        }

        Assertions.assertThat(keysThatWereRemoved).contains(
            stageDao.cacheKeyForStageHistories(pipelineName, stageName),
            stageDao.cacheKeyForStageCount(pipelineName, stageName),
            stageDao.cacheKeyForStageOffset(stage)
        );

        Assertions.assertThat(keysThatWereAdded).contains(
            stageDao.cacheKeyForStageHistories(pipelineName, stageName),
            stageDao.cacheKeyForStageCount(pipelineName, stageName),
            stageDao.cacheKeyForStageOffset(stage)
        );
    }

    @Test
    public void shouldGetDetailedStageHistory() throws Exception {
        HgMaterial hg = new HgMaterial("url", null);
        String[] hg_revs = {"h1", "h2", "h3"};
        scheduleUtil.checkinInOrder(hg, hg_revs);

        String pipelineName = "p1";
        String stageName = "stage_name";

        ScheduleTestUtil.AddedPipeline p1 = scheduleUtil.saveConfigWith(pipelineName, stageName, scheduleUtil.m(hg), new String[]{"job1", "job2"});
        scheduleUtil.runAndPass(p1, "h1");
        scheduleUtil.runAndPass(p1, "h2");
        scheduleUtil.runAndPass(p1, "h3");

        Pagination pagination = Pagination.pageStartingAt(0, 3, 2);
        StageInstanceModels stageInstanceModels = stageDao.findDetailedStageHistoryByOffset(pipelineName, stageName, pagination);

        assertThat(stageInstanceModels.size()).isEqualTo(2);

        assertThat(stageInstanceModels.get(0).getResult()).isEqualTo(StageResult.Passed);
        assertThat(stageInstanceModels.get(0).getIdentifier().getPipelineName()).isEqualTo(pipelineName);
        assertThat(stageInstanceModels.get(0).getIdentifier().getPipelineCounter()).isEqualTo(3);
        assertThat(stageInstanceModels.get(0).getIdentifier().getStageName()).isEqualTo(stageName);
        assertThat(stageInstanceModels.get(0).getIdentifier().getStageCounter()).isEqualTo("1");
        assertJobDetails(stageInstanceModels.get(0).getBuildHistory());

        assertThat(stageInstanceModels.get(1).getResult()).isEqualTo(StageResult.Passed);
        assertThat(stageInstanceModels.get(1).getIdentifier().getPipelineName()).isEqualTo(pipelineName);
        assertThat(stageInstanceModels.get(1).getIdentifier().getPipelineCounter()).isEqualTo(2);
        assertThat(stageInstanceModels.get(1).getIdentifier().getStageName()).isEqualTo(stageName);
        assertThat(stageInstanceModels.get(1).getIdentifier().getStageCounter()).isEqualTo("1");
        assertJobDetails(stageInstanceModels.get(1).getBuildHistory());

        pagination = Pagination.pageStartingAt(2, 3, 2);
        stageInstanceModels = stageDao.findDetailedStageHistoryByOffset(pipelineName, stageName, pagination);

        assertThat(stageInstanceModels.size()).isEqualTo(1);

        assertThat(stageInstanceModels.get(0).getResult()).isEqualTo(StageResult.Passed);
        assertThat(stageInstanceModels.get(0).getIdentifier().getPipelineName()).isEqualTo(pipelineName);
        assertThat(stageInstanceModels.get(0).getIdentifier().getPipelineCounter()).isEqualTo(1);
        assertThat(stageInstanceModels.get(0).getIdentifier().getStageName()).isEqualTo(stageName);
        assertThat(stageInstanceModels.get(0).getIdentifier().getStageCounter()).isEqualTo("1");
        assertJobDetails(stageInstanceModels.get(0).getBuildHistory());
    }

    private void assertJobDetails(JobHistory buildHistory) {
        assertThat(buildHistory.size()).isEqualTo(2);
        Set<String> jobNames = new HashSet<>(Arrays.asList(buildHistory.get(0).getName(), buildHistory.get(1).getName()));
        assertThat(jobNames).contains("job2", "job1");
        assertThat(buildHistory.get(0).getResult()).isEqualTo(JobResult.Passed);
        assertThat(buildHistory.get(1).getResult()).isEqualTo(JobResult.Passed);
    }

    @Test
    public void shouldCacheDetailedStageHistoryPageAndCountAndOffset() throws Exception {
        HgMaterial hg = new HgMaterial("url", null);
        String[] hg_revs = {"h1"};
        scheduleUtil.checkinInOrder(hg, hg_revs);

        String pipelineName = "p1";
        String stageName = "stage_name";
        Pagination pagination = Pagination.pageStartingAt(0, 10, 10);

        ScheduleTestUtil.AddedPipeline p1 = scheduleUtil.saveConfigWith(pipelineName, stageName, scheduleUtil.m(hg));
        scheduleUtil.runAndPass(p1, "h1");

        Stage stage = stageDao.mostRecentStage(new StageConfigIdentifier(pipelineName, stageName));
        stageDao.findDetailedStageHistoryByOffset(pipelineName, stageName, pagination); // PRIME CACHE

        Method cacheKeyForDetailedStageHistories = getMethodViaReflection("cacheKeyForDetailedStageHistories", String.class, String.class);
        Object primedDetailedStageHistoryPage = goCache.get((String) cacheKeyForDetailedStageHistories.invoke(stageDao, pipelineName, stageName));

        stageDao.findDetailedStageHistoryByOffset(pipelineName, stageName, pagination); // SHOULD RETURN FROM CACHE

        Object cachedDetailedStageHistoryPage = goCache.get((String) cacheKeyForDetailedStageHistories.invoke(stageDao, pipelineName, stageName));

        assertThat(cachedDetailedStageHistoryPage).isSameAs(primedDetailedStageHistoryPage);
    }

    @Test
    public void shouldInvalidateDetailedStageHistoryCachesOnStageSave() throws Exception {
        HgMaterial hg = new HgMaterial("url", null);
        String[] hg_revs = {"h1"};
        scheduleUtil.checkinInOrder(hg, hg_revs);

        String pipelineName = "p1";
        String stageName = "stage_name";
        Pagination pagination = Pagination.pageStartingAt(0, 10, 10);

        ScheduleTestUtil.AddedPipeline p1 = scheduleUtil.saveConfigWith(pipelineName, stageName, scheduleUtil.m(hg));
        scheduleUtil.runAndPass(p1, "h1");

        Stage stage = stageDao.mostRecentStage(new StageConfigIdentifier(pipelineName, stageName));
        stageDao.findDetailedStageHistoryByOffset(pipelineName, stageName, pagination); // PRIME CACHE

        Method cacheKeyForDetailedStageHistories = getMethodViaReflection("cacheKeyForDetailedStageHistories", String.class, String.class);
        Object primedDetailedStageHistoryPage = goCache.get((String) cacheKeyForDetailedStageHistories.invoke(stageDao, pipelineName, stageName));

        scheduleUtil.runAndPass(p1, "h1"); // NEW RUN OF STAGE, CACHE SHOULD BE INVALIDATED

        stage = stageDao.mostRecentStage(new StageConfigIdentifier(pipelineName, stageName));
        stageDao.findDetailedStageHistoryByOffset(pipelineName, stageName, pagination); // SHOULD QUERY AGAIN

        Object reprimedDetailedStageHistoryPage = goCache.get((String) cacheKeyForDetailedStageHistories.invoke(stageDao, pipelineName, stageName));

        assertThat(reprimedDetailedStageHistoryPage).isNotSameAs(primedDetailedStageHistoryPage);
    }

    @Test
    public void shouldPaginateBasedOnOffset() throws Exception {
        HgMaterial hg = new HgMaterial("url", null);
        String[] hg_revs = {"h1", "h2", "h3"};
        scheduleUtil.checkinInOrder(hg, hg_revs);

        String pipelineName = "p1";
        String stageName = "stage_name";

        ScheduleTestUtil.AddedPipeline p1 = scheduleUtil.saveConfigWith(pipelineName, stageName, scheduleUtil.m(hg));
        String run1 = scheduleUtil.runAndPass(p1, "h1");
        String run2 = scheduleUtil.runAndPass(p1, "h2");
        String run3 = scheduleUtil.runAndPass(p1, "h3");
        String run4 = scheduleUtil.runAndPass(p1, "h1", "h2");
        String run5 = scheduleUtil.runAndPass(p1, "h2", "h3");
        String run6 = scheduleUtil.runAndPass(p1, "h3", "h1");
        String run7 = scheduleUtil.runAndPass(p1, "h1", "h2", "h3");

        Pagination pagination = Pagination.pageStartingAt(0, 7, 3);
        StageInstanceModels stageInstanceModels = stageDao.findDetailedStageHistoryByOffset(pipelineName, stageName, pagination);
        assertStageModels(stageInstanceModels, run7, run6, run5);

        pagination = Pagination.pageStartingAt(1, 7, 3);
        stageInstanceModels = stageDao.findDetailedStageHistoryByOffset(pipelineName, stageName, pagination);
        assertStageModels(stageInstanceModels, run6, run5, run4);

        pagination = Pagination.pageStartingAt(2, 7, 3);
        stageInstanceModels = stageDao.findDetailedStageHistoryByOffset(pipelineName, stageName, pagination);
        assertStageModels(stageInstanceModels, run5, run4, run3);

        pagination = Pagination.pageStartingAt(3, 7, 3);
        stageInstanceModels = stageDao.findDetailedStageHistoryByOffset(pipelineName, stageName, pagination);
        assertStageModels(stageInstanceModels, run4, run3, run2);

        pagination = Pagination.pageStartingAt(4, 7, 3);
        stageInstanceModels = stageDao.findDetailedStageHistoryByOffset(pipelineName, stageName, pagination);
        assertStageModels(stageInstanceModels, run3, run2, run1);

        pagination = Pagination.pageStartingAt(5, 7, 3);
        stageInstanceModels = stageDao.findDetailedStageHistoryByOffset(pipelineName, stageName, pagination);
        assertStageModels(stageInstanceModels, run2, run1);

        pagination = Pagination.pageStartingAt(6, 7, 3);
        stageInstanceModels = stageDao.findDetailedStageHistoryByOffset(pipelineName, stageName, pagination);
        assertStageModels(stageInstanceModels, run1);

        pagination = Pagination.pageStartingAt(7, 7, 3);
        stageInstanceModels = stageDao.findDetailedStageHistoryByOffset(pipelineName, stageName, pagination);
        assertThat(stageInstanceModels.size()).as("Expected no models. Found: " + stageInstanceModels).isEqualTo(0);

        pagination = Pagination.pageStartingAt(20, 7, 3);
        stageInstanceModels = stageDao.findDetailedStageHistoryByOffset(pipelineName, stageName, pagination);
        assertThat(stageInstanceModels.size()).as("Expected no models. Found: " + stageInstanceModels).isEqualTo(0);

        pagination = Pagination.pageStartingAt(1, 7, 4);
        stageInstanceModels = stageDao.findDetailedStageHistoryByOffset(pipelineName, stageName, pagination);
        assertStageModels(stageInstanceModels, run6, run5, run4, run3);
    }

    @Test
    public void stageHistoryViaCursor_shouldReturnEmptyListIfNoRecordsArePresent() {
        StageInstanceModels stageHistory = stageDao.findDetailedStageHistoryViaCursor(PIPELINE_NAME, STAGE_DEV, FeedModifier.Latest, 0, 10);

        assertThat(stageHistory).isEmpty();
    }

    @Test
    public void stageHistoryViaCursor_shouldReturnListWithTheLatestRecords() {
        HgMaterial hg = new HgMaterial("url", null);
        String[] hg_revs = {"h1", "h2", "h3"};
        scheduleUtil.checkinInOrder(hg, hg_revs);

        ScheduleTestUtil.AddedPipeline p1 = scheduleUtil.saveConfigWith(PIPELINE_NAME, STAGE_DEV, scheduleUtil.m(hg));
        String run1 = scheduleUtil.runAndPass(p1, "h1");
        String run2 = scheduleUtil.runAndPass(p1, "h2");
        String run3 = scheduleUtil.runAndPass(p1, "h3");
        String run4 = scheduleUtil.runAndPass(p1, "h1", "h2");
        String run5 = scheduleUtil.runAndPass(p1, "h2", "h3");
        String run6 = scheduleUtil.runAndPass(p1, "h3", "h1");
        String run7 = scheduleUtil.runAndPass(p1, "h1", "h2", "h3");

        StageInstanceModels stageHistory = stageDao.findDetailedStageHistoryViaCursor(PIPELINE_NAME, STAGE_DEV, FeedModifier.Latest, 0, 3);

        assertStageModels(stageHistory, run7, run6, run5);
    }

    @Test
    public void stageHistoryViaCursor_shouldReturnListWithTheRecordsAfterTheSpecifiedCursor() { //older records
        HgMaterial hg = new HgMaterial("url", null);
        String[] hg_revs = {"h1", "h2", "h3"};
        scheduleUtil.checkinInOrder(hg, hg_revs);

        String pipelineName = PIPELINE_NAME + "-" + UUID.randomUUID();
        ScheduleTestUtil.AddedPipeline p1 = scheduleUtil.saveConfigWith(pipelineName, STAGE_DEV, scheduleUtil.m(hg));
        Date date = new Date();
        Pipeline run1 = scheduleUtil.runAndPassAndReturnPipelineInstance(p1, date, "h1");
        Pipeline run2 = scheduleUtil.runAndPassAndReturnPipelineInstance(p1, date, "h2");
        Pipeline run3 = scheduleUtil.runAndPassAndReturnPipelineInstance(p1, date, "h3");
        Pipeline run4 = scheduleUtil.runAndPassAndReturnPipelineInstance(p1, date, "h1", "h2");
        Pipeline run5 = scheduleUtil.runAndPassAndReturnPipelineInstance(p1, date, "h2", "h3");

        StageInstanceModels stageHistory = stageDao.findDetailedStageHistoryViaCursor(pipelineName, STAGE_DEV, FeedModifier.After, run4.getFirstStage().getId(), 3);

        assertThat(stageHistory.size()).isEqualTo(3);
        assertThat(stageHistory.get(0).getId()).isEqualTo(run3.getFirstStage().getId());
        assertThat(stageHistory.get(1).getId()).isEqualTo(run2.getFirstStage().getId());
        assertThat(stageHistory.get(2).getId()).isEqualTo(run1.getFirstStage().getId());
    }

    @Test
    public void stageHistoryViaCursor_shouldReturnListWithTheRecordsBeforeTheSpecifiedCursor() { //newer records
        HgMaterial hg = new HgMaterial("url", null);
        String[] hg_revs = {"h1", "h2", "h3"};
        scheduleUtil.checkinInOrder(hg, hg_revs);

        String pipelineName = PIPELINE_NAME + "-" + UUID.randomUUID();
        ScheduleTestUtil.AddedPipeline p1 = scheduleUtil.saveConfigWith(pipelineName, STAGE_DEV, scheduleUtil.m(hg));
        Date date = new Date();
        Pipeline run1 = scheduleUtil.runAndPassAndReturnPipelineInstance(p1, date, "h1");
        Pipeline run2 = scheduleUtil.runAndPassAndReturnPipelineInstance(p1, date, "h2");
        Pipeline run3 = scheduleUtil.runAndPassAndReturnPipelineInstance(p1, date, "h3");
        Pipeline run4 = scheduleUtil.runAndPassAndReturnPipelineInstance(p1, date, "h1", "h2");
        Pipeline run5 = scheduleUtil.runAndPassAndReturnPipelineInstance(p1, date, "h2", "h3");

        StageInstanceModels stageHistory = stageDao.findDetailedStageHistoryViaCursor(pipelineName, STAGE_DEV, FeedModifier.Before, run3.getFirstStage().getId(), 3);

        assertThat(stageHistory.size()).isEqualTo(2);
        assertThat(stageHistory.get(0).getId()).isEqualTo(run5.getFirstStage().getId());
        assertThat(stageHistory.get(1).getId()).isEqualTo(run4.getFirstStage().getId());
    }

    @Test
    public void latestAndOldest_shouldReturnOldestAndLatestStageRun() {
        HgMaterial hg = new HgMaterial("foo-bar", null);
        String[] hg_revs = {"rev1", "rev2", "rev3"};
        scheduleUtil.checkinInOrder(hg, hg_revs);

        Date date = new Date();
        String pipelineName = PIPELINE_NAME + "-" + UUID.randomUUID();
        ScheduleTestUtil.AddedPipeline p1 = scheduleUtil.saveConfigWith(pipelineName, STAGE_DEV, scheduleUtil.m(hg));
        Pipeline run1 = scheduleUtil.runAndPassAndReturnPipelineInstance(p1, date, "rev1");
        Pipeline run2 = scheduleUtil.runAndPassAndReturnPipelineInstance(p1, date, "rev2");
        Pipeline run3 = scheduleUtil.runAndPassAndReturnPipelineInstance(p1, date, "rev3");

        PipelineRunIdInfo info = stageDao.getOldestAndLatestStageInstanceId(pipelineName, STAGE_DEV);

        assertThat(info.getOldestRunId()).isEqualTo(run1.getFirstStage().getId());
        assertThat(info.getLatestRunId()).isEqualTo(run3.getFirstStage().getId());
    }


    @Test
    public void findStageFeedBy_shouldGetStageFeeds() {
        PipelineAndStage[] cruiseStages = run4Pipelines("cruise");
        run4Pipelines("mingle");

        List<StageFeedEntry> completedStages = stageDao.findStageFeedBy("cruise", 1, FeedModifier.Latest, 2);
        StageFeedEntry latestFeedEntry = completedStages.get(0);

        assertThat(completedStages.size()).isEqualTo(2);
        assertFeed(completedStages.get(0), cruiseStages[3].stage);
        assertFeed(completedStages.get(1), cruiseStages[2].stage);

        assertThat(latestFeedEntry.getResult()).isEqualTo(StageResult.Failed.name());
        assertThat(latestFeedEntry.getPipelineId()).isEqualTo(cruiseStages[3].stage.getPipelineId());
    }

    @Test
    public void findStageFeedBy_shouldGetStageFeedsAfterCounter() {
        PipelineAndStage[] cruiseStages = run4Pipelines("cruise");
        run4Pipelines("mingle");

        List<StageFeedEntry> completedStages = stageDao.findStageFeedBy("cruise", 3, FeedModifier.After, 4);

        assertThat(completedStages.size()).isEqualTo(2);
        assertFeed(completedStages.get(0), cruiseStages[3].stage);
        assertFeed(completedStages.get(1), cruiseStages[2].stage);
    }

    @Test
    public void findStageFeedBy_shouldGetStageFeedsBeforeCounter() {
        PipelineAndStage[] cruiseStages = run4Pipelines("cruise");
        run4Pipelines("mingle");

        List<StageFeedEntry> completedStages = stageDao.findStageFeedBy("cruise", 3, FeedModifier.Before, 4);

        assertThat(completedStages.size()).isEqualTo(1);
        assertFeed(completedStages.get(0), cruiseStages[0].stage);
    }

    @Test
    public void findStageFeedBy_shouldAlwaysGetLatestStageFeedsWhenCounterIsNull() {
        PipelineAndStage[] cruiseStages = run4Pipelines("cruise");
        run4Pipelines("mingle");

        List<StageFeedEntry> completedStages = stageDao.findStageFeedBy("cruise", null, FeedModifier.Before, 4);

        assertThat(completedStages.size()).isEqualTo(4);
        assertFeed(completedStages.get(0), cruiseStages[3].stage);
        assertFeed(completedStages.get(1), cruiseStages[2].stage);
        assertFeed(completedStages.get(2), cruiseStages[1].stage);
        assertFeed(completedStages.get(3), cruiseStages[0].stage);
    }

    private void assertStageModels(StageInstanceModels stageInstanceModels, String... runIdentifiers) {
        String message = "Expected: " + stageInstanceModels + " to match: " + Arrays.asList(runIdentifiers);
        assertThat(stageInstanceModels.size()).as(message).isEqualTo(runIdentifiers.length);

        for (int i = 0; i < runIdentifiers.length; i++) {
            assertThat(stageInstanceModels.get(i).getIdentifier().getStageLocator()).as(message + ". Failed at index: " + i).isEqualTo(runIdentifiers[i]);
        }
    }

    private Method getMethodViaReflection(String methodName, Class<?>... classes) {
        Method method = ReflectionUtils.findMethod(stageDao.getClass(), methodName, classes);
        method.setAccessible(true);
        return method;
    }

    private long transitionId(Stage cancelledStage) {
        return transition(cancelledStage).getId();
    }

    private JobStateTransition transition(Stage cancelledStage) {
        return cancelledStage.getJobInstances().get(0).getTransition(JobState.Completed);
    }

    private void assertForeignKey(long stageId, long stageForeignKey) {
        assertThat(stageForeignKey).as("Not same foreign key").isEqualTo(stageId);
        assertThat(stageForeignKey).isNotEqualTo(NOT_PERSISTED);
    }

    private PipelineAndStage pipelineAndFirstStageOf(Pipeline pipeline) {
        Stages stages = pipeline.getStages();
        assertThat(stages.size()).isEqualTo(1);
        return new PipelineAndStage(pipeline, stages.get(0));
    }

    private void pass(Pipeline pipeline) {
        for (Stage stage : pipeline.getStages()) {
            dbHelper.passStage(stage);
        }
    }

    private void fail(Pipeline pipeline) {
        for (Stage stage : pipeline.getStages()) {
            dbHelper.failStage(stage);
        }
    }

    private List<Pipeline> createFourPipelines() throws Exception {
        List<Pipeline> completedPipelines = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Pipeline[] createdPipelines = pipelineWithOnePassedAndOneCurrentlyRunning(mingleConfig);
            completedPipelines.add(createdPipelines[0]);
        }
        return completedPipelines;
    }

    private Pipeline pipelineWithFirstStageCancelled(PipelineConfig pipelineConfig) {
        Pipeline pipeline = dbHelper.schedulePipeline(pipelineConfig, new TimeProvider());
        dbHelper.cancelStage(pipelineAndFirstStageOf(pipeline).stage);
        return pipeline;
    }

    private Pipeline pipelineWithOneJustScheduled() throws Exception {
        return dbHelper.schedulePipeline(mingleConfig, new TimeProvider());
    }

    private Pipeline pipelineWithFirstStagePassed(PipelineConfig pipelineConfig) throws Exception {
        Pipeline completed = dbHelper.schedulePipeline(pipelineConfig, new TimeProvider());
        assertThat(dbHelper.updateNaturalOrder(completed.getId(), 1.0)).isEqualTo(1);
        dbHelper.pass(completed);
        return completed;
    }

    private Pipeline[] pipelineWithOnePassedAndOneCurrentlyRunning(PipelineConfig pipelineConfig) throws Exception {
        Pipeline completed = pipelineWithFirstStagePassed(pipelineConfig);

        Pipeline running = dbHelper.schedulePipeline(pipelineConfig, new TimeProvider());
        assertThat(dbHelper.updateNaturalOrder(running.getId(), 2.0)).isEqualTo(1);
        scheduleBuildInstances(pipelineAndFirstStageOf(running).stage);
        return new Pipeline[]{completed, running};
    }

    private Pipeline pipelineWithFirstStageRunning(PipelineConfig pipeline) {
        Pipeline running = dbHelper.schedulePipeline(pipeline, new TimeProvider());
        scheduleBuildInstances(pipelineAndFirstStageOf(running).stage);
        return running;
    }

    public static StageAsDMR stageAsDmr(Stage stage) {
        return new StageAsDMR(stage.getIdentifier(), stage.completedDate());
    }

    private PipelineAndStage[] run4Pipelines() throws SQLException {
        return run4Pipelines(PIPELINE_NAME);
    }

    private static class PipelineAndStage {
        final Pipeline pipeline;
        final Stage stage;

        private PipelineAndStage(Pipeline pipeline, Stage stage) {
            this.pipeline = pipeline;
            this.stage = stage;
        }
    }

    private PipelineAndStage[] run4Pipelines(String pipelineName) {
        PipelineConfig mingleConfig = PipelineConfigMother.createPipelineConfig(pipelineName, "stage", "job");
        dbHelper.schedulePipeline(mingleConfig, new TimeProvider()); // save a scheduled one as a control pipeline
        Pipeline completed = dbHelper.schedulePipeline(mingleConfig, new TimeProvider());
        dbHelper.cancelStage(pipelineAndFirstStageOf(completed).stage);
        Pipeline passed = dbHelper.schedulePipeline(mingleConfig, new TimeProvider());
        dbHelper.pass(passed);
        Pipeline firstFailure = pipelineWithFirstStageFailed(mingleConfig);
        Pipeline secondFailure = pipelineWithFirstStageFailed(mingleConfig);
        return new PipelineAndStage[]{pipelineAndFirstStageOf(completed), pipelineAndFirstStageOf(passed), pipelineAndFirstStageOf(firstFailure), pipelineAndFirstStageOf(secondFailure)};
    }

    private Pipeline pipelineWithFirstStageFailed(PipelineConfig mingleConfig) {
        Pipeline firstFailure = dbHelper.schedulePipeline(mingleConfig, new TimeProvider());
        dbHelper.failStage(pipelineAndFirstStageOf(firstFailure).stage);
        return firstFailure;
    }

    private void assertFeed(FeedEntry feedEntry, Stage cancelledStage) {
        assertThat(feedEntry.getId()).isEqualTo(cancelledStage.getId());
        assertThat(feedEntry.getEntryId()).isEqualTo(transitionId(cancelledStage));
        StageIdentifier id = cancelledStage.getIdentifier();
        assertThat(feedEntry.getTitle()).isEqualTo(format("%s(%s) stage %s(%s) %s", id.getPipelineName(), id.getPipelineCounter(), id.getStageName(), id.getStageCounter(), cancelledStage.getResult()));//cruise(5) stage stage Failed
        assertThat(feedEntry.getUpdatedDate()).isEqualTo(cancelledStage.latestTransitionDate());
    }

    private void updateResultInTransaction(final Stage stage, final StageResult result) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {

                stageDao.updateResult(stage, result, null);
            }
        });
    }
}
