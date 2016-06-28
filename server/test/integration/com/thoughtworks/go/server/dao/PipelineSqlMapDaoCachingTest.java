/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.dao;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.git.GitMaterialInstance;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.presentation.pipelinehistory.*;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.database.DatabaseStrategy;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestUtils;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ibatis.SqlMapClientTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

import static com.thoughtworks.go.util.DataStructureUtils.m;
import static com.thoughtworks.go.util.IBatisUtil.arguments;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class PipelineSqlMapDaoCachingTest {
    @Autowired private GoCache goCache;
    @Autowired private SqlMapClient sqlMapClient;
    private PipelineSqlMapDao pipelineDao;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private TransactionSynchronizationManager transactionSynchronizationManager;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private SystemEnvironment systemEnvironment;
    @Autowired private GoConfigDao configFileDao;
    @Autowired private DatabaseStrategy databaseStrategy;
    private SqlMapClientTemplate mockTemplate;
    private MaterialRepository repository;
    private EnvironmentVariableDao environmentVariableDao;

    @Before
    public void setup() throws Exception {
        goCache.clear();
        dbHelper.onSetUp();
        mockTemplate = mock(SqlMapClientTemplate.class);
        repository = mock(MaterialRepository.class);
        environmentVariableDao = mock(EnvironmentVariableDao.class);
        pipelineDao = new PipelineSqlMapDao(null, repository, goCache, environmentVariableDao, transactionTemplate, null,
                transactionSynchronizationManager, systemEnvironment, configFileDao, databaseStrategy);
        pipelineDao.setSqlMapClientTemplate(mockTemplate);
    }

    @After
    public void tearDown() throws Exception {
        dbHelper.onTearDown();
    }

    @Test
    public void lockedPipelineCacheKey_shouldReturnTheSameInstanceForAGivenPipeline() {
        assertSame(
                pipelineDao.lockedPipelineCacheKey("dev"),
                pipelineDao.lockedPipelineCacheKey("dev"));
    }

    @Test
    public void buildCauseForPipelineIdentifiedByNameAndCounter_shouldUseCacheAndBeCaseInsensitive() throws Exception {
        Pipeline expected = PipelineMother.pipeline("pipeline");
        expected.setId(3);

        MaterialRevisions originalMaterialRevisions = ModificationsMother.createHgMaterialRevisions();

        when(repository.findMaterialRevisionsForPipeline(3)).thenReturn(originalMaterialRevisions);
        when(mockTemplate.queryForObject("findPipelineByNameAndCounter", m("name", "pipeline", "counter", 15))).thenReturn(expected);

        BuildCause buildCause = (BuildCause) transactionTemplate.transactionSurrounding(new TransactionTemplate.TransactionSurrounding<RuntimeException>() {
            public Object surrounding() {
                return pipelineDao.findBuildCauseOfPipelineByNameAndCounter("pipeline", 15);
            }
        });

        verify(mockTemplate).queryForObject("findPipelineByNameAndCounter", m("name", "pipeline", "counter", 15));
        verify(repository).findMaterialRevisionsForPipeline(3);

        assertThat(buildCause.getMaterialRevisions(), is(originalMaterialRevisions));


        buildCause = pipelineDao.findBuildCauseOfPipelineByNameAndCounter("pipeline".toUpperCase(), 15);

        assertThat(buildCause.getMaterialRevisions(), is(originalMaterialRevisions));

        verifyNoMoreInteractions(mockTemplate, repository);
    }

    @Test
    public void pipelineWithModsByStageId_shouldCachePipelineWithMods() throws Exception {
        Pipeline expected = PipelineMother.pipeline("pipeline");
        expected.setId(1);

        when(mockTemplate.queryForObject("getPipelineByStageId", 1L)).thenReturn(expected);

        Pipeline actual = pipelineDao.pipelineWithModsByStageId("pipeline", 1L);
        assertSame(expected, actual);

        pipelineDao.pipelineWithModsByStageId("pipeline".toUpperCase(), 1L);
        verify(mockTemplate, times(1)).queryForObject("getPipelineByStageId", 1L);
    }

    @Test
    public void lockedPipeline_shouldCacheLockedPipelineStatus() throws Exception {
        StageIdentifier expected = new StageIdentifier("mingle", 1, "foo", "stage", "1");
        when(mockTemplate.queryForObject("lockedPipeline", "mingle")).thenReturn(expected);

        pipelineDao.lockedPipeline("mingle");
        StageIdentifier actual = pipelineDao.lockedPipeline("mingle");

        assertSame(expected, actual);
        verify(mockTemplate, times(1)).queryForObject("lockedPipeline", "mingle");
    }

    @Test
    public void lockedPipeline_shouldReturnNullIfPipelineIsNotLocked() throws Exception {
        pipelineDao.lockedPipeline("mingle");
        StageIdentifier actual = pipelineDao.lockedPipeline("mingle");

        assertNull("got " + actual, actual);
        verify(mockTemplate, times(1)).queryForObject("lockedPipeline", "mingle");
    }

    @Test
    public void lockPipeline_shouldInvalidateCacheOnTransactionCommit() throws Exception {
        final Pipeline pipeline = PipelineMother.pipeline("mingle");

        when(mockTemplate.queryForObject("lockedPipeline", "mingle")).thenReturn(pipeline.getFirstStage().getIdentifier());
        pipelineDao.lockedPipeline("mingle");

        pipelineDao.lockPipeline(pipeline);

        pipelineDao.lockedPipeline("mingle");
        verify(mockTemplate, times(2)).queryForObject("lockedPipeline", "mingle");
        verify(mockTemplate, times(1)).update("lockPipeline", pipeline.getId());
    }

    @Test
    public void lockPipeline_shouldHandleSerializationProperly() throws Exception {
        when(mockTemplate.queryForObject("lockedPipeline", "mingle")).thenReturn(null);
        assertNull(pipelineDao.lockedPipeline("mingle"));
        goCache.put(pipelineDao.lockedPipelineCacheKey("mingle"), new StageIdentifier("NOT_LOCKED", 0, "NOT_LOCKED", null));
        assertNull(pipelineDao.lockedPipeline("mingle"));
    }

    @Test
    public void unlockPipeline_shouldInvalidateCacheOnTransactionCommit() throws Exception {
        final Pipeline pipeline = PipelineMother.pipeline("mingle");

        when(mockTemplate.queryForObject("lockedPipeline", "mingle")).thenReturn(pipeline.getFirstStage().getIdentifier());
        pipelineDao.lockedPipeline("mingle");

        pipelineDao.unlockPipeline("mingle");

        pipelineDao.lockedPipeline("mingle");
        verify(mockTemplate, times(1)).update("unlockLockedPipeline", "mingle");
        verify(mockTemplate, times(2)).queryForObject("lockedPipeline", "mingle");
    }

    @Test(timeout = 10 * 1000)
    public void lockPipeline_shouldEnsureOnlyOneThreadCanLockAPipelineSuccessfully() throws Exception {

        pipelineDao = new PipelineSqlMapDao(null, null, goCache, null, transactionTemplate, sqlMapClient, transactionSynchronizationManager, systemEnvironment,
                configFileDao, databaseStrategy) {
            @Override
            public StageIdentifier lockedPipeline(String pipelineName) {
                StageIdentifier result = super.lockedPipeline(pipelineName);
                TestUtils.sleepQuietly(20); // simulate multiple threads trying to lock a pipeline
                return result;
            }
        };

        List<Thread> threads = new ArrayList<Thread>();
        final int[] errors = new int[1];
        for (int i = 0; i < 10; i++) {
            JobInstances jobInstances = new JobInstances(JobInstanceMother.completed("job"));
            Stage stage = new Stage("stage-1", jobInstances, "shilpa", "auto", new TimeProvider());
            final Pipeline pipeline = PipelineMother.pipeline("mingle", stage);
            pipeline.setCounter(i + 1);
            dbHelper.savePipelineWithStagesAndMaterials(pipeline);

            Thread thread = new Thread(new Runnable() {
                public void run() {
                    try {
                        pipelineDao.lockPipeline(pipeline);
                    } catch (Exception e) {
                        errors[0]++;
                    }
                }
            }, "thread-" + i);
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertThat(errors[0], is(9));
    }

    @Test
    public void findLastSuccessfulStageIdentifier_shouldCacheResult() {
        StageIdentifier expected = new StageIdentifier("pipeline", 0, "${COUNT}", "stage", "1");
        when(mockTemplate.queryForObject(eq("getLastSuccessfulStageInPipeline"), any())).thenReturn(PipelineMother.passedPipelineInstance("pipeline", "stage", "job"));
        pipelineDao.findLastSuccessfulStageIdentifier("pipeline", "stage");

        StageIdentifier actual = pipelineDao.findLastSuccessfulStageIdentifier("pipeline", "stage");

        assertEquals(actual, expected);
        verify(mockTemplate, times(1)).queryForObject(eq("getLastSuccessfulStageInPipeline"), any());
    }

    @Test
    public void findPipelineIds_shouldCacheResultWhenOnlyLatestPipelineIdIsRequested() {
        List<Long> expectedIds = new ArrayList<Long>();
        expectedIds.add(1L);
        when(mockTemplate.queryForList(eq("getPipelineRange"), any())).thenReturn(expectedIds);
        pipelineDao.findPipelineIds("pipelineName", 1, 0);
        List<Long> actual = pipelineDao.findPipelineIds("pipelineName", 1, 0);
        assertThat(actual.size(), is(1));
        assertEquals(expectedIds.get(0), actual.get(0));
        verify(mockTemplate, times(1)).queryForList(eq("getPipelineRange"), any());
    }

    @Test
    public void findPipelineIds_shouldNotCacheResultWhenMultiplePipelineIdsOrPipelineIdsFromASubsequentPageAreRequested() {
        List<Long> expectedIds = new ArrayList<Long>();
        expectedIds.add(1L);
        expectedIds.add(2L);
        when(mockTemplate.queryForList(eq("getPipelineRange"), any())).thenReturn(expectedIds);
        pipelineDao.findPipelineIds("pipelineName", 2, 0);
        pipelineDao.findPipelineIds("pipelineName", 2, 0);
        pipelineDao.findPipelineIds("pipelineName", 1, 2);
        pipelineDao.findPipelineIds("pipelineName", 1, 2);
        verify(mockTemplate, times(4)).queryForList(eq("getPipelineRange"), any());
    }

    @Test
    public void savePipeline_shouldClearLatestPipelineIdCacheCaseInsensitively() {
        when(mockTemplate.queryForList(eq("getPipelineRange"), any())).thenReturn(Arrays.asList(99L));
        pipelineDao.save(PipelineMother.pipeline("pipelineName"));
        pipelineDao.findPipelineIds("pipelineName", 1, 0);
        pipelineDao.save(PipelineMother.pipeline("pipelineName".toUpperCase()));
        pipelineDao.findPipelineIds("pipelineName", 1, 0);
        verify(mockTemplate, times(2)).queryForList(eq("getPipelineRange"), any());
    }

    @Test
    public void findLastSuccessfulStageIdentifier_shouldEnsureOnlyOneThreadCanUpdateCacheAtATime() throws Exception {
        Pipeline pipelineWithOlderStage = PipelineMother.passedPipelineInstance("pipeline-name", "stage", "job");
        Stage stage = pipelineWithOlderStage.findStage("stage");
        stage.setIdentifier(new StageIdentifier("pipeline-name", 1, "stage", "1"));
        stage.setPipelineId(1L);
        when(mockTemplate.queryForObject(eq("getLastSuccessfulStageInPipeline"), any())).thenReturn(pipelineWithOlderStage);

        goCache = new GoCache(goCache) {
            @Override
            public void put(String key, Object value) {
                TestUtils.sleepQuietly(1000); // sleep so we can have multiple threads try to update the cache with a value from the db
                super.put(key, value);
            }
        };
        pipelineDao = new PipelineSqlMapDao(null, null, goCache, null, null, null, transactionSynchronizationManager, systemEnvironment, configFileDao, databaseStrategy);
        pipelineDao.setSqlMapClientTemplate(mockTemplate);

        final Stage newerStage = StageMother.passedStageInstance("stage", "job", "pipeline-name");
        newerStage.setPipelineId(1L);
        final StageIdentifier newerIdentifer = new StageIdentifier("pipeline-name", 1, "stage", "999999");
        newerStage.setIdentifier(newerIdentifer);

        new Thread(new Runnable() {
            public void run() {
                pipelineDao.stageStatusChanged(newerStage);
            }
        }).start();
        TestUtils.sleepQuietly(200);

        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < 10; i++) {
            final Pipeline pipeline = PipelineMother.pipeline("mingle");
            pipeline.setCounter(i + 1);

            Thread thread = new Thread(new Runnable() {
                public void run() {
                    assertEquals(newerIdentifer, pipelineDao.findLastSuccessfulStageIdentifier("pipeline-name", "stage"));
                }
            }, "thread-" + i);
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        verify(mockTemplate, never()).queryForObject(eq("getLastSuccessfulStageInPipeline"), any());
    }

    @Test
    public void stageStateChanged_shouldUpdateLatestSuccessfulStageIdentifierCache() {
        Stage passed = StageMother.passedStageInstance("dev", "job", "pipeline-name");
        passed.setPipelineId(1L);
        pipelineDao.stageStatusChanged(passed);
        StageIdentifier actual = pipelineDao.findLastSuccessfulStageIdentifier("pipeline-name", "dev");
        assertEquals(passed.getIdentifier(), actual);
        verify(mockTemplate, never()).queryForObject(eq("getLastSuccessfulStageInPipeline"), any());
    }

    @Test
    public void stageStateChanged_shouldNotUpdateLatestSuccessfulStageIdentifierCacheIfStageIsNotPassed() {
        Stage passed = StageMother.createPassedStage("pipeline", 1, "stage", 8, "job", new Date());
        passed.setPipelineId(1L);
        pipelineDao.stageStatusChanged(passed);
        pipelineDao.findLastSuccessfulStageIdentifier("pipeline", "stage");

        Stage failed = StageMother.completedFailedStageInstance("pipeline", "dev", "job");
        failed.setIdentifier(new StageIdentifier("pipeline", 1, "LABEL-1", "stage", "10000"));
        failed.setPipelineId(1L);
        pipelineDao.stageStatusChanged(failed);

        StageIdentifier actual = pipelineDao.findLastSuccessfulStageIdentifier("pipeline", "stage");
        assertEquals(passed.getIdentifier(), actual);

        verify(mockTemplate, never()).queryForObject(eq("getLastSuccessfulStageInPipeline"), any());
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
        CruiseConfig mockCruiseConfig=mock(BasicCruiseConfig.class);
        GoConfigDao mockconfigFileDao = mock(GoConfigDao.class);
        when(mockconfigFileDao.load()).thenReturn(mockCruiseConfig);
        when(mockCruiseConfig.getAllPipelineNames()).thenReturn(Arrays.asList(new CaseInsensitiveString(pipelineName)));

        //need to mock configfileDao for this test
        pipelineDao = new PipelineSqlMapDao(null, repository, goCache, environmentVariableDao, transactionTemplate, null,
                transactionSynchronizationManager, systemEnvironment, mockconfigFileDao, databaseStrategy);
        pipelineDao.setSqlMapClientTemplate(mockTemplate);

        PipelineInstanceModel pipeline = new PipelineInstanceModel(pipelineName, -2, "label", BuildCause.createManualForced(), new StageInstanceModels());
        PipelineInstanceModels pims = PipelineInstanceModels.createPipelineInstanceModels(pipeline);
        when(mockTemplate.queryForList("allActivePipelines")).thenReturn(pims);
        when(mockTemplate.queryForObject(eq("getPipelineHistoryById"), any())).thenReturn(pipeline);
        PipelineInstanceModels loaded;
        loaded = pipelineDao.loadActivePipelines();
        loaded = pipelineDao.loadActivePipelines();
        assertNotSame(pipeline, loaded);
        assertTrue(ToStringBuilder.reflectionToString(loaded) + " not equal to\n" + ToStringBuilder.reflectionToString(pipeline),
                EqualsBuilder.reflectionEquals(loaded, pims));
        verify(mockTemplate, times(1)).queryForList("allActivePipelines");
        verify(mockTemplate,times(1)).queryForObject(eq("getPipelineHistoryById"), any());
        verify(mockconfigFileDao,times(2)).load();
        verify(mockCruiseConfig,times(2)).getAllPipelineNames();
    }



    @Test
    public void shouldLockedPipelineCacheWhenStageStatusChanges() {
        PipelineInstanceModel pipeline = new PipelineInstanceModel("pipeline", -2, "label", BuildCause.createManualForced(), new StageInstanceModels());
        when(mockTemplate.queryForObject(eq("getPipelineHistoryById"), any())).thenReturn(pipeline);

        PipelineInstanceModel loaded;
        pipelineDao.lockedPipeline("pipeline");
        pipelineDao.lockedPipeline("pipeline");
        verify(mockTemplate, times(1)).queryForObject(eq("lockedPipeline"), any());

        changeStageStatus();

        pipelineDao.lockedPipeline("pipeline");
        verify(mockTemplate, times(2)).queryForObject(eq("lockedPipeline"), any());
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
        PipelineInstanceModels pims = PipelineInstanceModels.createPipelineInstanceModels(first);
        when(mockTemplate.queryForList("allActivePipelines")).thenReturn(pims);
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
        PipelineInstanceModels pims = PipelineInstanceModels.createPipelineInstanceModels();
        when(mockTemplate.queryForList("allActivePipelines")).thenReturn(pims);
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
        CruiseConfig mockCruiseConfig=mock(BasicCruiseConfig.class);
        GoConfigDao mockconfigFileDao = mock(GoConfigDao.class);
        when(mockconfigFileDao.load()).thenReturn(mockCruiseConfig);
        when(mockCruiseConfig.getAllPipelineNames()).thenReturn(Arrays.asList(new CaseInsensitiveString(pipelineName)));

        //need to mock configfileDao for this test
        pipelineDao = new PipelineSqlMapDao(null, repository, goCache, environmentVariableDao, transactionTemplate, null,
                transactionSynchronizationManager, systemEnvironment, mockconfigFileDao, databaseStrategy);
        pipelineDao.setSqlMapClientTemplate(mockTemplate);

        PipelineInstanceModel first = model(1, JobState.Building, JobResult.Unknown);
        PipelineInstanceModel second = model(2, JobState.Building, JobResult.Unknown);
        PipelineInstanceModels pims = PipelineInstanceModels.createPipelineInstanceModels(first, second);
        when(mockTemplate.queryForList("allActivePipelines")).thenReturn(pims);
        when(mockTemplate.queryForObject("getPipelineHistoryById", arguments("id", 1L).asMap())).thenReturn(first);
        when(mockTemplate.queryForObject("getPipelineHistoryById", arguments("id", 2L).asMap())).thenReturn(second);

        // ensure cache is initialized
        pipelineDao.loadActivePipelines();

        Stage stage = new Stage("first", new JobInstances(JobInstanceMother.assigned("job")), "me", "whatever", new TimeProvider());
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
        CruiseConfig mockCruiseConfig=mock(BasicCruiseConfig.class);
        GoConfigDao mockconfigFileDao = mock(GoConfigDao.class);
        when(mockconfigFileDao.load()).thenReturn(mockCruiseConfig);
        when(mockCruiseConfig.getAllPipelineNames()).thenReturn(Arrays.asList(new CaseInsensitiveString(pipelineName)));

        //need to mock configfileDao for this test
        pipelineDao = new PipelineSqlMapDao(null, repository, goCache, environmentVariableDao, transactionTemplate, null,
                transactionSynchronizationManager, systemEnvironment, mockconfigFileDao, databaseStrategy);
        pipelineDao.setSqlMapClientTemplate(mockTemplate);

        PipelineInstanceModel first = model(1, JobState.Completed, JobResult.Passed);
        PipelineInstanceModel second = model(2, JobState.Building, JobResult.Unknown);
        PipelineInstanceModels pims = PipelineInstanceModels.createPipelineInstanceModels(first);
        when(mockTemplate.queryForList("allActivePipelines")).thenReturn(pims);
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

        assertThat((StageIdentifier) goCache.get(pipelineDao.cacheKeyForlatestPassedStage(pipelineId, stage)), is(StageIdentifier.NULL));

        actual = pipelineDao.latestPassedStageIdentifier(pipelineId, stage);
        assertThat(actual, is(StageIdentifier.NULL));

        verify(mockTemplate, times(1)).queryForObject("latestPassedStageForPipelineId", args);
    }

    @Test
    public void shouldRemoveLatestPassedStageForPipelineFromCacheUponStageStatusChangeCaseInsensitively() {
        String stage = "stage";
        Stage passedStage = StageMother.passedStageInstance(stage.toUpperCase(), "job", "pipeline-name");
        passedStage.setPipelineId(10L);

        goCache.put(pipelineDao.cacheKeyForlatestPassedStage(passedStage.getPipelineId(), stage), new StageIdentifier());
        pipelineDao.stageStatusChanged(passedStage);
        assertThat(goCache.get(pipelineDao.cacheKeyForlatestPassedStage(passedStage.getPipelineId(), stage)), is(nullValue()));
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
    public void shouldCachePipelineInstancesTriggeredOutOfDependencyMaterialCaseInsensitively() throws Exception {
        List<PipelineIdentifier> results = Arrays.asList(new PipelineIdentifier("p1", 1));
        when(mockTemplate.queryForList(eq("pipelineInstancesTriggeredOutOfDependencyMaterial"), anyString())).thenReturn(results);

        pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial("p1", new PipelineIdentifier("p", 1));

        //Query second time should return from cache
        pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial("p1".toUpperCase(), new PipelineIdentifier("P", 1));

        verify(mockTemplate, times(1)).queryForList(eq("pipelineInstancesTriggeredOutOfDependencyMaterial"), anyString());
    }

    @Test
    public void shouldCacheEmptyPipelineInstancesTriggeredOutOfDependencyMaterial() throws Exception {
        String cacheKey = (PipelineSqlMapDao.class + "_cacheKeyForPipelineInstancesWithDependencyMaterial_" + "p1_p_1").intern();
        when(mockTemplate.queryForList(eq("pipelineInstancesTriggeredOutOfDependencyMaterial"), anyString())).thenReturn(new ArrayList());

        List<PipelineIdentifier> actual = pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial("p1", new PipelineIdentifier("p", 1));
        assertThat(actual, hasSize(0));
        assertThat((List<PipelineIdentifier>) goCache.get(cacheKey), is(actual));
    }

    @Test
    public void shouldInvalidateCacheOfPipelineInstancesTriggeredWithDependencyMaterial() throws Exception {
        String cacheKey = (PipelineSqlMapDao.class + "_cacheKeyForPipelineInstancesWithDependencyMaterial_" + "p1_p_1").intern();
        List<PipelineIdentifier> result = Arrays.asList(new PipelineIdentifier("p1", 1, "1"));
        when(mockTemplate.queryForList(eq("pipelineInstancesTriggeredOutOfDependencyMaterial"), anyString())).thenReturn(new ArrayList());

        List<PipelineIdentifier> actual = pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial("p1", new PipelineIdentifier("p", 1));
        assertThat(actual, hasSize(0));
        assertThat((List<PipelineIdentifier>) goCache.get(cacheKey), hasSize(0));


        MaterialRevisions materialRevisions = new MaterialRevisions(
                new MaterialRevision(new DependencyMaterial(new CaseInsensitiveString("p"), new CaseInsensitiveString("s")), new Modification("u", "comment", "email", new Date(), "p/1/s/1")));
        Pipeline pipeline = new Pipeline("p1", BuildCause.createWithModifications(materialRevisions, ""));

        pipelineDao.save(pipeline);
        assertThat(goCache.get(cacheKey), is(Matchers.nullValue()));
    }

    @Test
    public void shouldNotInvalidateCacheOfPipelineInstancesTriggeredWithDependencyMaterial_WhenADifferentPipelineInstanceIsCreatedWithDifferentRevision() throws Exception {
        String cacheKey = (PipelineSqlMapDao.class + "_cacheKeyForPipelineInstancesWithDependencyMaterial_" + "p1_p_1").intern();
        List<PipelineIdentifier> result = Arrays.asList(new PipelineIdentifier("p1", 1, "1"));
        when(mockTemplate.queryForList(eq("pipelineInstancesTriggeredOutOfDependencyMaterial"), anyString())).thenReturn(result);

        List<PipelineIdentifier> actual = pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial("p1", new PipelineIdentifier("p", 1));
        assertThat(actual, Matchers.is(result));
        assertThat((List<PipelineIdentifier>) goCache.get(cacheKey), is(result));


        MaterialRevisions materialRevisions = new MaterialRevisions(
                new MaterialRevision(new DependencyMaterial(new CaseInsensitiveString("p"), new CaseInsensitiveString("s")), new Modification("u", "comment", "email", new Date(), "p/2/s/1")));
        Pipeline pipeline = new Pipeline("p1", BuildCause.createWithModifications(materialRevisions, ""));

        pipelineDao.save(pipeline);
        assertThat((List<PipelineIdentifier>) goCache.get(cacheKey), is(result));
    }

	@Test
	public void shouldCachePipelineInstancesTriggeredOutOfMaterialRevision() throws Exception {
		GitMaterialInstance materialInstance = new GitMaterialInstance("url", "branch", "submodule", "flyweight");
		List<PipelineIdentifier> results = Arrays.asList(new PipelineIdentifier("p1", 1));
		String cacheKey = pipelineDao.cacheKeyForPipelineInstancesTriggeredWithDependencyMaterial("p1", materialInstance.getFingerprint(), "r1");
		when(mockTemplate.queryForList(eq("pipelineInstancesTriggeredOffOfMaterialRevision"), anyString())).thenReturn(results);

		assertThat(goCache.get(cacheKey), is(Matchers.nullValue()));
		List<PipelineIdentifier> actual = pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial("p1", materialInstance, "r1");

		//Query second time should return from cache
		pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial("p1".toUpperCase(), materialInstance, "r1");
		assertThat((List<PipelineIdentifier>) goCache.get(cacheKey), is(actual));

		verify(mockTemplate, times(1)).queryForList(eq("pipelineInstancesTriggeredOffOfMaterialRevision"), anyString());
	}

	@Test
	public void shouldCacheEmptyPipelineInstancesTriggeredOutOfMaterialRevision() throws Exception {
		GitMaterialInstance materialInstance = new GitMaterialInstance("url", "branch", "submodule", "flyweight");
		String cacheKey = pipelineDao.cacheKeyForPipelineInstancesTriggeredWithDependencyMaterial("p1", materialInstance.getFingerprint(), "r1");
		when(mockTemplate.queryForList(eq("pipelineInstancesTriggeredOffOfMaterialRevision"), anyString())).thenReturn(new ArrayList());

		List<PipelineIdentifier> actual = pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial("p1", materialInstance, "r1");

		assertThat(actual, hasSize(0));
		assertThat((List<PipelineIdentifier>) goCache.get(cacheKey), is(actual));
	}

	@Test
	public void shouldInvalidateCacheOfPipelineInstancesTriggeredWithMaterialRevision() throws Exception {
		GitMaterialInstance materialInstance = new GitMaterialInstance("url", "branch", "submodule", "flyweight");
		String cacheKey = pipelineDao.cacheKeyForPipelineInstancesTriggeredWithDependencyMaterial("p1", materialInstance.getFingerprint(), "r1");
		List<PipelineIdentifier> result = Arrays.asList(new PipelineIdentifier("p1", 1, "1"));
		when(mockTemplate.queryForList(eq("pipelineInstancesTriggeredOffOfMaterialRevision"), anyString())).thenReturn(result);

		List<PipelineIdentifier> actual = pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial("p1", materialInstance, "r1");
		assertThat(actual, hasSize(1));
		assertThat((List<PipelineIdentifier>) goCache.get(cacheKey), hasSize(1));

		MaterialRevisions materialRevisions = new MaterialRevisions(new MaterialRevision(new GitMaterial("url", "branch"), new Modification("user", "comment", "email", new Date(), "r1")));
		Pipeline pipeline = new Pipeline("p1", BuildCause.createWithModifications(materialRevisions, ""));

		pipelineDao.save(pipeline);
		assertThat(goCache.get(cacheKey), is(Matchers.nullValue()));
	}

	@Test
	public void shouldNotInvalidateCacheOfPipelineInstancesTriggeredWithMaterialRevision_WhenAPipelineInstanceIsCreatedWithDifferentMaterial() throws Exception {
		GitMaterialInstance materialInstance = new GitMaterialInstance("url", "branch", "submodule", "flyweight");
		String cacheKey = pipelineDao.cacheKeyForPipelineInstancesTriggeredWithDependencyMaterial("p1", materialInstance.getFingerprint(), "r1");
		List<PipelineIdentifier> result = Arrays.asList(new PipelineIdentifier("p1", 1, "1"));
		when(mockTemplate.queryForList(eq("pipelineInstancesTriggeredOffOfMaterialRevision"), anyString())).thenReturn(result);

		List<PipelineIdentifier> actual = pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial("p1", materialInstance, "r1");
		assertThat(actual, Matchers.is(result));
		assertThat((List<PipelineIdentifier>) goCache.get(cacheKey), is(result));

		MaterialRevisions materialRevisions = new MaterialRevisions(new MaterialRevision(new GitMaterial("url", "branch"), new Modification("user", "comment", "email", new Date(), "r2")));
		Pipeline pipeline = new Pipeline("p1", BuildCause.createWithModifications(materialRevisions, ""));

		pipelineDao.save(pipeline);
		assertThat((List<PipelineIdentifier>) goCache.get(cacheKey), is(result));
	}

    private PipelineInstanceModel model(long id, JobState jobState, JobResult jobResult) {
        StageInstanceModels models = new StageInstanceModels();
        models.add(new StageInstanceModel("first", "1", JobHistory.withJob("job", jobState, jobResult, new Date())));
        PipelineInstanceModel model = new PipelineInstanceModel("pipeline", -2, "label", BuildCause.createManualForced(), models);
        model.setId(id);
        return model;
    }

}
