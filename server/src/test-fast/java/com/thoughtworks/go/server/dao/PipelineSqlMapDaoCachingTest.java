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

package com.thoughtworks.go.server.dao;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.database.Database;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.git.GitMaterialInstance;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.presentation.pipelinehistory.*;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.StubGoCache;
import com.thoughtworks.go.server.transaction.SqlMapClientTemplate;
import com.thoughtworks.go.server.transaction.TestTransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hamcrest.Matchers;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.util.*;

import static com.thoughtworks.go.util.DataStructureUtils.m;
import static com.thoughtworks.go.util.IBatisUtil.arguments;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PipelineSqlMapDaoCachingTest {
    private GoCache goCache;
    private PipelineSqlMapDao pipelineDao;
    private TransactionTemplate transactionTemplate;
    private TransactionSynchronizationManager transactionSynchronizationManager;
    private SqlMapClientTemplate mockTemplate;
    private EnvironmentVariableDao environmentVariableDao;
    private MaterialRepository repository;
    private TimeProvider timeProvider;

    @Before
    public void setup() throws Exception {
        transactionSynchronizationManager = mock(TransactionSynchronizationManager.class);
        goCache = new StubGoCache(new TestTransactionSynchronizationManager());
        goCache.clear();
        mockTemplate = mock(SqlMapClientTemplate.class);
        repository = mock(MaterialRepository.class);
        environmentVariableDao = mock(EnvironmentVariableDao.class);
        SessionFactory mockSessionFactory = mock(SessionFactory.class);
        repository = mock(MaterialRepository.class);
        transactionTemplate = mock(TransactionTemplate.class);
        GoConfigDao configFileDao = mock(GoConfigDao.class);
        timeProvider = mock(TimeProvider.class);
        pipelineDao = new PipelineSqlMapDao(null, repository, goCache, environmentVariableDao, transactionTemplate, null,
                transactionSynchronizationManager, null, configFileDao, mock(Database.class), mockSessionFactory, timeProvider);
        pipelineDao.setSqlMapClientTemplate(mockTemplate);
        Session session = mock(Session.class);
        when(mockSessionFactory.getCurrentSession()).thenReturn(session);
        when(configFileDao.load()).thenReturn(GoConfigMother.defaultCruiseConfig());
    }

    @Test
    public void buildCauseForPipelineIdentifiedByNameAndCounter_shouldUseCacheAndBeCaseInsensitive() {
        Pipeline expected = PipelineMother.pipeline("pipeline");
        expected.setId(3);

        MaterialRevisions originalMaterialRevisions = ModificationsMother.createHgMaterialRevisions();

        when(repository.findMaterialRevisionsForPipeline(3)).thenReturn(originalMaterialRevisions);
        when(mockTemplate.queryForObject("findPipelineByNameAndCounter", m("name", "pipeline", "counter", 15))).thenReturn(expected);

        BuildCause buildCause = pipelineDao.findBuildCauseOfPipelineByNameAndCounter("pipeline", 15);

        verify(mockTemplate).queryForObject("findPipelineByNameAndCounter", m("name", "pipeline", "counter", 15));
        verify(repository).findMaterialRevisionsForPipeline(3);

        assertThat(buildCause.getMaterialRevisions(), is(originalMaterialRevisions));

        buildCause = pipelineDao.findBuildCauseOfPipelineByNameAndCounter("pipeline".toUpperCase(), 15);

        assertThat(buildCause.getMaterialRevisions(), is(originalMaterialRevisions));

        verifyNoMoreInteractions(mockTemplate, repository);
    }

    @Test
    public void findPipelineIds_shouldCacheResultWhenOnlyLatestPipelineIdIsRequested() {
        List<Long> expectedIds = new ArrayList<>();
        expectedIds.add(1L);
        when(mockTemplate.queryForList(eq("getPipelineRange"), any())).thenReturn((List) expectedIds);
        pipelineDao.findPipelineIds("pipelineName", 1, 0);
        List<Long> actual = pipelineDao.findPipelineIds("pipelineName", 1, 0);
        assertThat(actual.size(), is(1));
        assertEquals(expectedIds.get(0), actual.get(0));
        verify(mockTemplate, times(1)).queryForList(eq("getPipelineRange"), any());
    }

    @Test
    public void findPipelineIds_shouldNotCacheResultWhenMultiplePipelineIdsOrPipelineIdsFromASubsequentPageAreRequested() {
        List<Long> expectedIds = new ArrayList<>();
        expectedIds.add(1L);
        expectedIds.add(2L);
        when(mockTemplate.queryForList(eq("getPipelineRange"), any())).thenReturn((List) expectedIds);
        pipelineDao.findPipelineIds("pipelineName", 2, 0);
        pipelineDao.findPipelineIds("pipelineName", 2, 0);
        pipelineDao.findPipelineIds("pipelineName", 1, 2);
        pipelineDao.findPipelineIds("pipelineName", 1, 2);
        verify(mockTemplate, times(4)).queryForList(eq("getPipelineRange"), any());
    }

    @Test
    public void savePipeline_shouldClearLatestPipelineIdCacheCaseInsensitively() {
        when(mockTemplate.queryForList(eq("getPipelineRange"), any())).thenReturn(Arrays.asList(99L));
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                ((TransactionSynchronizationAdapter) invocation.getArguments()[0]).afterCommit();
                return null;
            }
        }).when(transactionSynchronizationManager).registerSynchronization(any(TransactionSynchronization.class));

        when(transactionTemplate.execute(any(TransactionCallback.class))).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                ((TransactionCallback) invocation.getArguments()[0]).doInTransaction(new SimpleTransactionStatus());
                return null;
            }
        });

        pipelineDao.save(PipelineMother.pipeline("pipelineName"));
        pipelineDao.findPipelineIds("pipelineName", 1, 0);
        pipelineDao.save(PipelineMother.pipeline("pipelineName".toUpperCase()));
        pipelineDao.findPipelineIds("pipelineName", 1, 0);
        verify(mockTemplate, times(2)).queryForList(eq("getPipelineRange"), any());
    }

    @Test
    public void loadHistory_shouldCacheResult() {
        PipelineInstanceModel pipeline = new PipelineInstanceModel("pipeline", -2, "label", BuildCause.createManualForced(), new StageInstanceModels());
        when(mockTemplate.queryForObject(eq("getPipelineHistoryById"), any())).thenReturn(pipeline);
        PipelineInstanceModel loaded;
        loaded = pipelineDao.loadHistory(99);
        loaded = pipelineDao.loadHistory(99);
        assertTrue(EqualsBuilder.reflectionEquals(loaded, pipeline));
        verify(mockTemplate, times(1)).queryForObject(eq("getPipelineHistoryById"), any());
    }

    @Test
    public void loadHistory_shouldCloneResultSoThatModificationsDoNotAffectTheCachedObjects() {
        PipelineInstanceModel pipeline = new PipelineInstanceModel("pipeline", -2, "label", BuildCause.createManualForced(), new StageInstanceModels());
        when(mockTemplate.queryForObject(eq("getPipelineHistoryById"), any())).thenReturn(pipeline);
        PipelineInstanceModel loaded;
        loaded = pipelineDao.loadHistory(99);
        loaded = pipelineDao.loadHistory(99);
        assertNotSame(pipeline, loaded);
        assertTrue(ToStringBuilder.reflectionToString(loaded) + " not equal to\n" + ToStringBuilder.reflectionToString(pipeline),
                EqualsBuilder.reflectionEquals(loaded, pipeline));
        verify(mockTemplate, times(1)).queryForObject(eq("getPipelineHistoryById"), any());
    }

    @Test
    public void loadActivePipelines_shouldCacheResult() {
        final String pipelineName = "pipeline";
        CruiseConfig mockCruiseConfig = mock(BasicCruiseConfig.class);
        GoConfigDao mockconfigFileDao = mock(GoConfigDao.class);
        when(mockconfigFileDao.load()).thenReturn(mockCruiseConfig);
        when(mockCruiseConfig.getAllPipelineNames()).thenReturn(Arrays.asList(new CaseInsensitiveString(pipelineName)));

        //need to mock configfileDao for this test
        pipelineDao = new PipelineSqlMapDao(null, repository, goCache, environmentVariableDao, transactionTemplate, null,
                transactionSynchronizationManager, null, mockconfigFileDao, null, mock(SessionFactory.class), timeProvider);
        pipelineDao.setSqlMapClientTemplate(mockTemplate);

        PipelineInstanceModel pipeline = new PipelineInstanceModel(pipelineName, -2, "label", BuildCause.createManualForced(), new StageInstanceModels());
        PipelineInstanceModels pims = PipelineInstanceModels.createPipelineInstanceModels(pipeline);
        when(mockTemplate.queryForList("allActivePipelines")).thenReturn((List) pims);
        when(mockTemplate.queryForObject(eq("getPipelineHistoryById"), any())).thenReturn(pipeline);
        PipelineInstanceModels loaded;
        loaded = pipelineDao.loadActivePipelines();
        loaded = pipelineDao.loadActivePipelines();
        assertNotSame(pipeline, loaded);
        assertTrue(ToStringBuilder.reflectionToString(loaded) + " not equal to\n" + ToStringBuilder.reflectionToString(pipeline),
                EqualsBuilder.reflectionEquals(loaded, pims));
        verify(mockTemplate, times(1)).queryForList("allActivePipelines");
        verify(mockTemplate, times(1)).queryForObject(eq("getPipelineHistoryById"), any());
        verify(mockconfigFileDao, times(2)).load();
        verify(mockCruiseConfig, times(2)).getAllPipelineNames();
    }

    private void changeStageStatus() {
        changeStageStatus(99);
    }

    private void changeStageStatus(int pipelineId) {
        Stage stage = new Stage();
        stage.setName("stage");
        stage.building();
        changeStageStatus(stage, pipelineId);
    }

    private void changeStageStatus(Stage stage, int pipelineId) {
        stage.setPipelineId((long) pipelineId);
        stage.setIdentifier(new StageIdentifier("pipeline", 1, "blah-label", "stage", "1"));
        pipelineDao.stageStatusChanged(stage);
    }

    @Test
    public void shouldClearCachedPipelineHistoryWhenStageStatusChanges() {
        PipelineInstanceModel pipeline = new PipelineInstanceModel("pipeline", -2, "label", BuildCause.createManualForced(), new StageInstanceModels());
        when(mockTemplate.queryForObject(eq("getPipelineHistoryById"), any())).thenReturn(pipeline);

        PipelineInstanceModel loaded;
        loaded = pipelineDao.loadHistory(99);

        changeStageStatus();

        loaded = pipelineDao.loadHistory(99);
        assertTrue(EqualsBuilder.reflectionEquals(loaded, pipeline));
        verify(mockTemplate, times(2)).queryForObject(eq("getPipelineHistoryById"), any());
    }

    @Test
    public void shouldIgnoreStageStatusChangeWhenActivePipelineCacheIsNotYetInitialized() {
        changeStageStatus(1);
        assertThat(goCache.get(pipelineDao.activePipelinesCacheKey()), is(nullValue()));
    }

    @Test
    public void shouldCacheActivePipelineIdWhenStageStatusChanges() {
        PipelineInstanceModel first = model(1, JobState.Building, JobResult.Unknown);
        PipelineInstanceModel second = model(2, JobState.Building, JobResult.Unknown);
        List<PipelineInstanceModel> pims = PipelineInstanceModels.createPipelineInstanceModels(first);
        when(mockTemplate.queryForList("allActivePipelines")).thenReturn((List) pims);
        when(mockTemplate.queryForObject("getPipelineHistoryById", arguments("id", 1L).asMap())).thenReturn(first);
        when(mockTemplate.queryForObject("getPipelineHistoryById", arguments("id", 2L).asMap())).thenReturn(second);

        // ensure cache is initialized
        pipelineDao.loadActivePipelines();

        changeStageStatus(2);

        pipelineDao.loadActivePipelines();
        verify(mockTemplate, times(1)).queryForObject("getPipelineHistoryById", arguments("id", 1L).asMap());
        verify(mockTemplate, times(1)).queryForObject("getPipelineHistoryById", arguments("id", 2L).asMap());
    }

    @Test
    public void stageStatusChanged_shouldNotRaiseErrorWhenNoPipelinesAreActive() {
        PipelineInstanceModel first = model(1, JobState.Building, JobResult.Unknown);
        List<PipelineInstanceModel> pims = PipelineInstanceModels.createPipelineInstanceModels();
        when(mockTemplate.queryForList("allActivePipelines")).thenReturn((List) pims);
        when(mockTemplate.queryForObject("getPipelineHistoryById", arguments("id", 1L).asMap())).thenReturn(first);

        // ensure cache is initialized
        pipelineDao.loadActivePipelines();

        changeStageStatus(1);

        pipelineDao.loadActivePipelines();
        verify(mockTemplate, times(1)).queryForObject("getPipelineHistoryById", arguments("id", 1L).asMap());
    }

    @Test
    public void shouldRemovePipelineIdFromCacheWhenStageFinishesForNonLatestPipeline() {
        final String pipelineName = "pipeline";
        CruiseConfig mockCruiseConfig = mock(BasicCruiseConfig.class);
        GoConfigDao mockconfigFileDao = mock(GoConfigDao.class);
        when(mockconfigFileDao.load()).thenReturn(mockCruiseConfig);
        when(mockCruiseConfig.getAllPipelineNames()).thenReturn(Arrays.asList(new CaseInsensitiveString(pipelineName)));

        //need to mock configfileDao for this test
        pipelineDao = new PipelineSqlMapDao(null, repository, goCache, environmentVariableDao, transactionTemplate, null,
                transactionSynchronizationManager, null, mockconfigFileDao, null, mock(SessionFactory.class), timeProvider);
        pipelineDao.setSqlMapClientTemplate(mockTemplate);

        PipelineInstanceModel first = model(1, JobState.Building, JobResult.Unknown);
        PipelineInstanceModel second = model(2, JobState.Building, JobResult.Unknown);
        List<PipelineInstanceModel> pims = PipelineInstanceModels.createPipelineInstanceModels(first, second);
        when(mockTemplate.queryForList("allActivePipelines")).thenReturn((List) pims);
        when(mockTemplate.queryForObject("getPipelineHistoryById", arguments("id", 1L).asMap())).thenReturn(first);
        when(mockTemplate.queryForObject("getPipelineHistoryById", arguments("id", 2L).asMap())).thenReturn(second);

        // ensure cache is initialized
        pipelineDao.loadActivePipelines();

        Stage stage = new Stage("first", new JobInstances(JobInstanceMother.assigned("job")), "me", null, "whatever", new TimeProvider());
        stage.fail();
        stage.calculateResult();
        changeStageStatus(stage, 1);

        //notifying latest id should not remove it from the cache
        stage.setPipelineId(2l);
        pipelineDao.stageStatusChanged(stage);

        PipelineInstanceModels models = pipelineDao.loadActivePipelines();
        assertThat(models.size(), is(1));
        assertThat(models.get(0).getId(), is(2L));
    }

    @Test
    public void shouldRemovePipelineIdFromCacheWhenPipelineCeasesToBeTheLatestAndIsNotActive() {
        final String pipelineName = "pipeline";
        CruiseConfig mockCruiseConfig = mock(BasicCruiseConfig.class);
        GoConfigDao mockconfigFileDao = mock(GoConfigDao.class);
        when(mockconfigFileDao.load()).thenReturn(mockCruiseConfig);
        when(mockCruiseConfig.getAllPipelineNames()).thenReturn(Arrays.asList(new CaseInsensitiveString(pipelineName)));

        //need to mock configfileDao for this test
        pipelineDao = new PipelineSqlMapDao(null, repository, goCache, environmentVariableDao, transactionTemplate, null,
                transactionSynchronizationManager, null, mockconfigFileDao, null, mock(SessionFactory.class), timeProvider);
        pipelineDao.setSqlMapClientTemplate(mockTemplate);

        PipelineInstanceModel first = model(1, JobState.Completed, JobResult.Passed);
        PipelineInstanceModel second = model(2, JobState.Building, JobResult.Unknown);
        List<PipelineInstanceModel> pims = PipelineInstanceModels.createPipelineInstanceModels(first);
        when(mockTemplate.queryForList("allActivePipelines")).thenReturn((List) pims);
        when(mockTemplate.queryForObject("getPipelineHistoryById", arguments("id", 1L).asMap())).thenReturn(first);
        when(mockTemplate.queryForObject("getPipelineHistoryById", arguments("id", 2L).asMap())).thenReturn(second);

        // ensure cache is initialized
        pipelineDao.loadActivePipelines();

        changeStageStatus(2);

        PipelineInstanceModels models = pipelineDao.loadActivePipelines();
        assertThat(models.size(), is(1));
        assertThat(models.get(0).getId(), is(2L));
    }

    @Test
    public void shouldCachePipelinePauseState() {
        String pipelineName = "pipelineName";
        PipelinePauseInfo pauseInfo = new PipelinePauseInfo(true, "pause cause", "admin");
        when(mockTemplate.queryForObject("getPipelinePauseState", pipelineName)).thenReturn(pauseInfo);
        // ensure cache is initialized
        pipelineDao.pauseState(pipelineName);

        PipelinePauseInfo actualPauseInfo = pipelineDao.pauseState(pipelineName);

        assertThat(actualPauseInfo, is(pauseInfo));
        verify(mockTemplate, times(1)).queryForObject("getPipelinePauseState", pipelineName);
    }

    @Test
    public void shouldCacheLatestPassedStageForPipeline() {
        StageIdentifier identifier = new StageIdentifier();
        long pipelineId = 10;
        String stage = "stage";
        Map<String, Object> args = arguments("id", pipelineId).and("stage", stage).asMap();

        when(mockTemplate.queryForObject("latestPassedStageForPipelineId", args)).thenReturn(identifier);

        pipelineDao.latestPassedStageIdentifier(pipelineId, stage);
        StageIdentifier actual = pipelineDao.latestPassedStageIdentifier(pipelineId, stage);

        assertThat(actual, is(identifier));
        verify(mockTemplate, times(1)).queryForObject("latestPassedStageForPipelineId", args);
    }

    @Test
    public void shouldCacheNullStageIdentifierIfNoneOfTheRunsForStageHasEverPassed() {
        StageIdentifier identifier = new StageIdentifier();
        long pipelineId = 10;
        String stage = "stage";
        Map<String, Object> args = arguments("id", pipelineId).and("stage", stage).asMap();

        when(mockTemplate.queryForObject("latestPassedStageForPipelineId", args)).thenReturn(null);

        StageIdentifier actual = pipelineDao.latestPassedStageIdentifier(pipelineId, stage);
        assertThat(actual, is(StageIdentifier.NULL));

        assertThat(goCache.get(pipelineDao.cacheKeyForLatestPassedStage(pipelineId, stage)), is(StageIdentifier.NULL));

        actual = pipelineDao.latestPassedStageIdentifier(pipelineId, stage);
        assertThat(actual, is(StageIdentifier.NULL));

        verify(mockTemplate, times(1)).queryForObject("latestPassedStageForPipelineId", args);
    }

    @Test
    public void shouldRemoveLatestPassedStageForPipelineFromCacheUponStageStatusChangeCaseInsensitively() {
        String stage = "stage";
        Stage passedStage = StageMother.passedStageInstance(stage.toUpperCase(), "job", "pipeline-name");
        passedStage.setPipelineId(10L);

        goCache.put(pipelineDao.cacheKeyForLatestPassedStage(passedStage.getPipelineId(), stage), new StageIdentifier());
        pipelineDao.stageStatusChanged(passedStage);
        assertThat(goCache.get(pipelineDao.cacheKeyForLatestPassedStage(passedStage.getPipelineId(), stage)), is(nullValue()));
    }

    @Test
    public void shouldInvalidateCacheWhenPipelineIsUnPaused() {
        String pipelineName = "pipelineName";
        Map<String, Object> args = arguments("pipelineName", pipelineName).and("pauseCause", null).and("pauseBy", null).and("paused", false).asMap();
        when(mockTemplate.update("updatePipelinePauseState", args)).thenReturn(0);
        when(mockTemplate.queryForObject("getPipelinePauseState", pipelineName)).thenReturn(PipelinePauseInfo.notPaused());
        // ensure cache is initialized
        pipelineDao.pauseState(pipelineName);

        pipelineDao.unpause(pipelineName);

        PipelinePauseInfo actualPauseInfo = pipelineDao.pauseState(pipelineName);
        assertThat(actualPauseInfo, is(PipelinePauseInfo.notPaused()));
        verify(mockTemplate, times(2)).queryForObject("getPipelinePauseState", pipelineName);
    }

    @Test
    public void shouldInvalidateCacheWhenPipelineIsPausedCaseInsensitively() {
        String pipelineName = "pipelineName";
        String pauseBy = "foo";
        String pauseCause = "waiting";
        Map<String, Object> args = arguments("pipelineName", pipelineName).and("pauseCause", pauseCause).and("pauseBy", pauseBy).and("paused", true).asMap();
        when(mockTemplate.update("updatePipelinePauseState", args)).thenReturn(0);

        PipelinePauseInfo notPausedPipelineInfo = new PipelinePauseInfo(true, pauseCause, pauseBy);
        when(mockTemplate.queryForObject("getPipelinePauseState", pipelineName)).thenReturn(notPausedPipelineInfo);

        PipelinePauseInfo pausedPipelineInfo = new PipelinePauseInfo(true, pauseCause, pauseBy);
        when(mockTemplate.queryForObject("getPipelinePauseState", pipelineName.toUpperCase())).thenReturn(pausedPipelineInfo);

        assertThat(pipelineDao.pauseState(pipelineName), is(pausedPipelineInfo));

        pipelineDao.pause(pipelineName.toUpperCase(), pauseCause, pauseBy);

        assertThat(pipelineDao.pauseState(pipelineName), is(pausedPipelineInfo));

        verify(mockTemplate, times(2)).queryForObject("getPipelinePauseState", pipelineName);
        verify(mockTemplate).queryForObject("getPipelinePauseState", pipelineName.toUpperCase());
        verify(mockTemplate).update(eq("updatePipelinePauseState"), anyObject());
    }

    @Test
    public void shouldCachePipelineInstancesTriggeredOutOfDependencyMaterialCaseInsensitively() {
        List<PipelineIdentifier> results = Arrays.asList(new PipelineIdentifier("p1", 1));
        when(mockTemplate.queryForList(eq("pipelineInstancesTriggeredOutOfDependencyMaterial"), any())).thenReturn((List) results);

        pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial("p1", new PipelineIdentifier("p", 1));

        //Query second time should return from cache
        pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial("p1".toUpperCase(), new PipelineIdentifier("P", 1));

        verify(mockTemplate, times(1)).queryForList(eq("pipelineInstancesTriggeredOutOfDependencyMaterial"), any());
    }

    @Test
    public void shouldCacheEmptyPipelineInstancesTriggeredOutOfDependencyMaterial() {
        String cacheKey = pipelineDao.cacheKeyForPipelineInstancesTriggeredWithDependencyMaterial("p1", "p", 1);
        when(mockTemplate.queryForList(eq("pipelineInstancesTriggeredOutOfDependencyMaterial"), anyString())).thenReturn(new ArrayList());

        List<PipelineIdentifier> actual = pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial("p1", new PipelineIdentifier("p", 1));
        assertThat(actual, hasSize(0));
        assertThat(goCache.get(cacheKey), is(actual));
    }

    @Test
    public void shouldInvalidateCacheOfPipelineInstancesTriggeredWithDependencyMaterial() {
        String cacheKey = pipelineDao.cacheKeyForPipelineInstancesTriggeredWithDependencyMaterial("p1", "p", 1);
        List<PipelineIdentifier> result = Arrays.asList(new PipelineIdentifier("p1", 1, "1"));
        when(mockTemplate.queryForList(eq("pipelineInstancesTriggeredOutOfDependencyMaterial"), anyString())).thenReturn(new ArrayList());

        List<PipelineIdentifier> actual = pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial("p1", new PipelineIdentifier("p", 1));
        assertThat(actual, hasSize(0));
        assertThat((List<PipelineIdentifier>) goCache.get(cacheKey), hasSize(0));


        MaterialRevisions materialRevisions = new MaterialRevisions(
                new MaterialRevision(new DependencyMaterial(new CaseInsensitiveString("p"), new CaseInsensitiveString("s")), new Modification("u", "comment", "email", new Date(), "p/1/s/1")));
        Pipeline pipeline = new Pipeline("p1", BuildCause.createWithModifications(materialRevisions, ""));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                ((TransactionSynchronizationAdapter) invocation.getArguments()[0]).afterCommit();
                return null;
            }
        }).when(transactionSynchronizationManager).registerSynchronization(any(TransactionSynchronization.class));

        when(transactionTemplate.execute(any(TransactionCallback.class))).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                ((TransactionCallback) invocation.getArguments()[0]).doInTransaction(new SimpleTransactionStatus());
                return null;
            }
        });

        pipelineDao.save(pipeline);
        assertThat(goCache.get(cacheKey), is(Matchers.nullValue()));
    }

    @Test
    public void shouldNotInvalidateCacheOfPipelineInstancesTriggeredWithDependencyMaterial_WhenADifferentPipelineInstanceIsCreatedWithDifferentRevision() {
        String cacheKey = pipelineDao.cacheKeyForPipelineInstancesTriggeredWithDependencyMaterial("p1", "p", 1);
        List<PipelineIdentifier> result = Arrays.asList(new PipelineIdentifier("p1", 1, "1"));
        when(mockTemplate.queryForList(eq("pipelineInstancesTriggeredOutOfDependencyMaterial"), any())).thenReturn((List) result);

        List<PipelineIdentifier> actual = pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial("p1", new PipelineIdentifier("p", 1));
        assertThat(actual, Matchers.is(result));
        assertThat(goCache.get(cacheKey), is(result));


        MaterialRevisions materialRevisions = new MaterialRevisions(
                new MaterialRevision(new DependencyMaterial(new CaseInsensitiveString("p"), new CaseInsensitiveString("s")), new Modification("u", "comment", "email", new Date(), "p/2/s/1")));
        Pipeline pipeline = new Pipeline("p1", BuildCause.createWithModifications(materialRevisions, ""));

        pipelineDao.save(pipeline);
        assertThat(goCache.get(cacheKey), is(result));
    }

    @Test
    public void shouldCachePipelineInstancesTriggeredOutOfMaterialRevision() {
        GitMaterialInstance materialInstance = new GitMaterialInstance("url", null, "branch", "submodule", "flyweight");
        List<PipelineIdentifier> results = Arrays.asList(new PipelineIdentifier("p1", 1));
        String cacheKey = pipelineDao.cacheKeyForPipelineInstancesTriggeredWithDependencyMaterial("p1", materialInstance.getFingerprint(), "r1");
        when(mockTemplate.queryForList(eq("pipelineInstancesTriggeredOffOfMaterialRevision"), any())).thenReturn((List) results);

        assertThat(goCache.get(cacheKey), is(Matchers.nullValue()));
        List<PipelineIdentifier> actual = pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial("p1", materialInstance, "r1");

        //Query second time should return from cache
        pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial("p1".toUpperCase(), materialInstance, "r1");
        assertThat(goCache.get(cacheKey), is(actual));

        verify(mockTemplate, times(1)).queryForList(eq("pipelineInstancesTriggeredOffOfMaterialRevision"), any());
    }

    @Test
    public void shouldCacheEmptyPipelineInstancesTriggeredOutOfMaterialRevision() {
        GitMaterialInstance materialInstance = new GitMaterialInstance("url", null, "branch", "submodule", "flyweight");
        String cacheKey = pipelineDao.cacheKeyForPipelineInstancesTriggeredWithDependencyMaterial("p1", materialInstance.getFingerprint(), "r1");
        when(mockTemplate.queryForList(eq("pipelineInstancesTriggeredOffOfMaterialRevision"), any())).thenReturn(new ArrayList());

        List<PipelineIdentifier> actual = pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial("p1", materialInstance, "r1");

        assertThat(actual, hasSize(0));
        assertThat(goCache.get(cacheKey), is(actual));
    }

    @Test
    public void shouldInvalidateCacheOfPipelineInstancesTriggeredWithMaterialRevision() {
        GitMaterialInstance materialInstance = new GitMaterialInstance("url", null, "branch", "submodule", "flyweight");
        String cacheKey = pipelineDao.cacheKeyForPipelineInstancesTriggeredWithDependencyMaterial("p1", materialInstance.getFingerprint(), "r1");
        List<PipelineIdentifier> result = Arrays.asList(new PipelineIdentifier("p1", 1, "1"));
        when(mockTemplate.queryForList(eq("pipelineInstancesTriggeredOffOfMaterialRevision"), any())).thenReturn((List) result);

        List<PipelineIdentifier> actual = pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial("p1", materialInstance, "r1");
        assertThat(actual, hasSize(1));
        assertThat((List<PipelineIdentifier>) goCache.get(cacheKey), hasSize(1));

        MaterialRevisions materialRevisions = new MaterialRevisions(new MaterialRevision(new GitMaterial("url", "branch"), new Modification("user", "comment", "email", new Date(), "r1")));
        Pipeline pipeline = new Pipeline("p1", BuildCause.createWithModifications(materialRevisions, ""));
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                ((TransactionSynchronizationAdapter) invocation.getArguments()[0]).afterCommit();
                return null;
            }
        }).when(transactionSynchronizationManager).registerSynchronization(any(TransactionSynchronization.class));

        when(transactionTemplate.execute(any(TransactionCallback.class))).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                ((TransactionCallback) invocation.getArguments()[0]).doInTransaction(new SimpleTransactionStatus());
                return null;
            }
        });

        pipelineDao.save(pipeline);
        assertThat(goCache.get(cacheKey), is(Matchers.nullValue()));
    }

    @Test
    public void shouldNotInvalidateCacheOfPipelineInstancesTriggeredWithMaterialRevision_WhenAPipelineInstanceIsCreatedWithDifferentMaterial() {
        GitMaterialInstance materialInstance = new GitMaterialInstance("url", null, "branch", "submodule", "flyweight");
        String cacheKey = pipelineDao.cacheKeyForPipelineInstancesTriggeredWithDependencyMaterial("p1", materialInstance.getFingerprint(), "r1");
        List<PipelineIdentifier> result = Arrays.asList(new PipelineIdentifier("p1", 1, "1"));
        when(mockTemplate.queryForList(eq("pipelineInstancesTriggeredOffOfMaterialRevision"), any())).thenReturn((List) result);

        List<PipelineIdentifier> actual = pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial("p1", materialInstance, "r1");
        assertThat(actual, Matchers.is(result));
        assertThat(goCache.get(cacheKey), is(result));

        MaterialRevisions materialRevisions = new MaterialRevisions(new MaterialRevision(new GitMaterial("url", "branch"), new Modification("user", "comment", "email", new Date(), "r2")));
        Pipeline pipeline = new Pipeline("p1", BuildCause.createWithModifications(materialRevisions, ""));

        pipelineDao.save(pipeline);
        assertThat(goCache.get(cacheKey), is(result));
    }

    private PipelineInstanceModel model(long id, JobState jobState, JobResult jobResult) {
        StageInstanceModels models = new StageInstanceModels();
        models.add(new StageInstanceModel("first", "1", JobHistory.withJob("job", jobState, jobResult, new Date())));
        PipelineInstanceModel model = new PipelineInstanceModel("pipeline", -2, "label", BuildCause.createManualForced(), models);
        model.setId(id);
        return model;
    }

}
