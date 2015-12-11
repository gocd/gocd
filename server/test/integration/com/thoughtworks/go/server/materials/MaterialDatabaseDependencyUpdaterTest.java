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

package com.thoughtworks.go.server.materials;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.domain.Stages;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.DependencyMaterialSourceDao;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.MaterialExpansionService;
import com.thoughtworks.go.server.service.MaterialService;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class MaterialDatabaseDependencyUpdaterTest {
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired protected MaterialRepository materialRepository;
    @Autowired private GoCache goCache;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private TransactionSynchronizationManager transactionSynchronizationManager;
    @Autowired private MaterialService materialService;
    @Autowired private LegacyMaterialChecker legacyMaterialChecker;
    @Autowired private SubprocessExecutionContext subprocessExecutionContext;
    @Autowired private MaterialExpansionService materialExpansionService;

    protected MaterialDatabaseUpdater updater;
    protected Material material;
    private DependencyMaterialSourceDao dependencyMaterialSourceDao;
    private ServerHealthService healthService;
    private DependencyMaterialUpdater dependencyMaterialUpdater;
    private ScmMaterialUpdater scmMaterialUpdater;

    @Before
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        goCache.clear();
        dependencyMaterialSourceDao = Mockito.mock(DependencyMaterialSourceDao.class);
        healthService = Mockito.mock(ServerHealthService.class);
        dependencyMaterialUpdater = new DependencyMaterialUpdater(goCache, transactionSynchronizationManager, dependencyMaterialSourceDao, materialRepository, materialService);
        scmMaterialUpdater = new ScmMaterialUpdater(materialRepository, legacyMaterialChecker, subprocessExecutionContext, materialService);
        updater = new MaterialDatabaseUpdater(materialRepository, healthService, transactionTemplate, goCache, dependencyMaterialUpdater, scmMaterialUpdater, null, null, materialExpansionService);
    }

    @After
    public void tearDown() throws Exception {
        dbHelper.onTearDown();
    }

    @Test
    public void shouldCreateEntriesForCompletedPipelines() throws Exception {
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));

        stubStageServiceGetHistory(null, stages(9));

        updater.updateMaterial(dependencyMaterial);

        List<Modification> modification = materialRepository.findLatestModification(dependencyMaterial).getMaterialRevision(0).getModifications();

        assertThat(modification.size(), is(1));
        assertThat(modification.get(0).getRevision(), is("pipeline-name/9/stage-name/0"));
        assertThat(modification.get(0).getPipelineLabel(), is("LABEL-9"));
    }

    @Test
    public void shouldUpdateServerHealthIfCheckFails() throws Exception {
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));

        RuntimeException runtimeException = new RuntimeException("Description of error");
        Mockito.when(dependencyMaterialSourceDao.getPassedStagesByName(new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name")),
                Pagination.pageStartingAt(0, null, MaterialDatabaseUpdater.STAGES_PER_PAGE)))
                .thenThrow(runtimeException);

        try {
            updater.updateMaterial(dependencyMaterial);
            fail("should have thrown exception " + runtimeException.getMessage());
        } catch (Exception e) {
            assertSame(e, runtimeException);
        }

        HealthStateType scope = HealthStateType.general(HealthStateScope.forMaterial(dependencyMaterial));
        ServerHealthState state = ServerHealthState.error("Modification check failed for material: pipeline-name", "Description of error", scope);
        Mockito.verify(healthService).update(state);
    }

    @Test
    public void shouldClearServerHealthIfCheckSucceeds() throws Exception {
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));

        Mockito.when(dependencyMaterialSourceDao.getPassedStagesByName(new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name")),
                Pagination.pageStartingAt(0, null, MaterialDatabaseUpdater.STAGES_PER_PAGE)))
                .thenReturn(new ArrayList<Modification>());

        updater.updateMaterial(dependencyMaterial);

        Mockito.verify(healthService).removeByScope(HealthStateScope.forMaterial(dependencyMaterial));
    }

    @Test
    public void shouldReturnNoNewModificationsIfNoNewPipelineHasBennCompleted() throws Exception {
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));

        stubStageServiceGetHistory(null, stages(9));
        updater.updateMaterial(dependencyMaterial);

        List<Modification> modification = materialRepository.findLatestModification(dependencyMaterial).getMaterialRevision(0).getModifications();

        stubStageServiceGetHistoryAfter(dependencyMaterial, 9, stages());

        updater.updateMaterial(dependencyMaterial);

        List<Modification> newModifications = materialRepository.findModificationsSince(dependencyMaterial, new MaterialRevision(dependencyMaterial, modification));

        assertThat(newModifications.size(), is(0));
    }

    private void stubStageServiceGetHistoryAfter(DependencyMaterial material, int pipelineCounter, Stages... stageses) {
        if(material == null){
            material = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));
        }
        StageIdentifier identifier = new StageIdentifier(String.format("%s/%s/%s/0", material.getPipelineName().toString(), pipelineCounter, material.getStageName().toString()));
        for (int i = 0; i < stageses.length; i++) {
            Stages stages = stageses[i];
            List<Modification> mods = new ArrayList<Modification>();
            for (Stage stage : stages) {
                StageIdentifier id = stage.getIdentifier();
                mods.add(new Modification(stage.completedDate(), id.stageLocator(), id.getPipelineLabel(), stage.getPipelineId()));
            }
            Mockito.when(dependencyMaterialSourceDao.getPassedStagesAfter(identifier.stageLocator(),
                    material,
                    Pagination.pageStartingAt(i * MaterialDatabaseUpdater.STAGES_PER_PAGE, null, MaterialDatabaseUpdater.STAGES_PER_PAGE)
            )).thenReturn(mods);
        }
        when(dependencyMaterialSourceDao.getPassedStagesAfter(identifier.stageLocator(),
                material,
                Pagination.pageStartingAt(MaterialDatabaseUpdater.STAGES_PER_PAGE * stageses.length, null, MaterialDatabaseUpdater.STAGES_PER_PAGE)
        )).thenReturn(new ArrayList<Modification>());
    }

    @Test
    public void shouldReturnNoNewModificationsIfPipelineHasNeverBeenScheduled() throws Exception {
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));

        stubStageServiceGetHistory(null);
        updater.updateMaterial(dependencyMaterial);

        MaterialRevisions materialRevisions = materialRepository.findLatestModification(dependencyMaterial);

        assertThat("materialRevisions.isEmpty()", materialRevisions.isEmpty(), is(true));
    }

    @Test
    public void shouldReturnLatestPipelineIfThereHasBeenANewOne() throws Exception {
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));

        stubStageServiceGetHistory(null, stages(9));
        updater.updateMaterial(dependencyMaterial);

        List<Modification> modification = materialRepository.findLatestModification(dependencyMaterial).getMaterialRevision(0).getModifications();

        stubStageServiceGetHistoryAfter(null, 9, stages(10));
        updater.updateMaterial(dependencyMaterial);

        List<Modification> newModifications = materialRepository.findModificationsSince(dependencyMaterial, new MaterialRevision(dependencyMaterial, modification));

        assertThat(newModifications.size(), is(1));
        assertThat(newModifications.get(0).getRevision(), is("pipeline-name/10/stage-name/0"));
        assertThat(newModifications.get(0).getPipelineLabel(), is("LABEL-10"));
    }

    @Test
    public void shouldInsertAllHistoricRunsOfUpstreamStageTheFirstTime() throws Exception {
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));

        stubStageServiceGetHistory(null, stages(9, 10, 11), stages(12, 13));

        updater.updateMaterial(dependencyMaterial);

        for (Integer revision : new int[]{9, 10, 11, 12, 13}) {
            String stageLocator = String.format("pipeline-name/%s/stage-name/0", revision);
            Modification modification = materialRepository.findModificationWithRevision(dependencyMaterial, stageLocator);
            assertThat(modification.getRevision(), is(stageLocator));
            assertThat(modification.getPipelineLabel(), is(String.format("LABEL-%s", revision)));
        }
    }

    @Test
    public void shouldCacheDependencyMaterialUpdatedStatusForAPipelineThatHasNeverRun() throws Exception {
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));
        stubStageServiceGetHistory(null, stages(), stages());

        // create the material instance
        updater.updateMaterial(dependencyMaterial);

        // update first time & should mark cache as updated
        updater.updateMaterial(dependencyMaterial);
        assertThat(goCache.get(DependencyMaterialUpdater.cacheKeyForDependencyMaterial(dependencyMaterial)), not(nullValue()));

        // update subsequently should not hit the database
        updater.updateMaterial(dependencyMaterial);
        Mockito.verify(dependencyMaterialSourceDao, times(2)).getPassedStagesByName(any(DependencyMaterial.class), any(Pagination.class));
    }

    @Test
    public void shouldUpdateMaterialCorrectlyIfCaseOfPipelineNameIsDifferentInConfigurationOfDependencyMaterial() throws Exception {

        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("PIPEline-name"), new CaseInsensitiveString("STAge-name"));
        stubStageServiceGetHistory(dependencyMaterial, stages(1));

        // create the material instance
        updater.updateMaterial(dependencyMaterial);

        stubStageServiceGetHistoryAfter(dependencyMaterial, 1, stages(2));

        // update first time & should mark cache as updated
        updater.updateMaterial(dependencyMaterial);

        Stage stage = stage(3);
        ReflectionUtil.setField(stage, "result", StageResult.Passed);

        // stage status update should invalidate cache
        dependencyMaterialUpdater.stageStatusChanged(stage);

        // update subsequently should hit database
        updater.updateMaterial(dependencyMaterial);

        Mockito.verify(dependencyMaterialSourceDao, times(2)).getPassedStagesAfter(any(String.class), any(DependencyMaterial.class), any(Pagination.class));
        Mockito.verify(dependencyMaterialSourceDao, times(2)).getPassedStagesByName(any(DependencyMaterial.class), any(Pagination.class));

    }

    @Test
    public void shouldCacheDependencyMaterialUpdatedStatusWhenUpdatingNewerRevisions() throws Exception {
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));

        stubStageServiceGetHistory(null, stages(1), stages(2));

        // create the material instance
        updater.updateMaterial(dependencyMaterial);

        stubStageServiceGetHistory(null);

        // insert newer modifications
        updater.updateMaterial(dependencyMaterial);
        assertThat(materialRepository.findModificationWithRevision(dependencyMaterial, "pipeline-name/2/stage-name/0"), not(nullValue()));

        // update subsequently should not hit the database
        updater.updateMaterial(dependencyMaterial);
        Mockito.verify(dependencyMaterialSourceDao, times(3)).getPassedStagesByName(any(DependencyMaterial.class), (Pagination) any());
        Mockito.verify(dependencyMaterialSourceDao, times(1)).getPassedStagesAfter(any(String.class), any(DependencyMaterial.class), (Pagination) any());
    }

    @Test
    public void stageStatusChanged_shouldSynchronizeOnDependencyMaterialCacheKey() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] flag = new boolean[]{false};

        updater = new MaterialDatabaseUpdater(materialRepository, healthService, transactionTemplate, goCache,
                dependencyMaterialUpdater, scmMaterialUpdater, null, null, materialExpansionService) {
            @Override
            void updateMaterialWithNewRevisions(Material material) {
                try {
                    flag[0] = true;
                    latch.countDown();
                    Thread.sleep(5000);
                    flag[0] = false;
                } catch (InterruptedException e) {
                    //ignore
                }
            }
        };

        dependencyMaterialUpdater = new DependencyMaterialUpdater(goCache, transactionSynchronizationManager, dependencyMaterialSourceDao, materialRepository, materialService) {
            @Override
            void removeCacheKey(String key) {
                assertThat(flag[0], is(false));
            }
        };

        final DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));
        Stages stages = stages(9, 10, 11);
        stubStageServiceGetHistory(null, stages);

        Thread updaterThread = new Thread(new Runnable() {
            public void run() {
                try {
                    updater.updateMaterial(dependencyMaterial); // let it get created
                    updater.updateMaterial(dependencyMaterial); // now update it
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }, "updaterthread");
        updaterThread.start();

        latch.await();

        Thread thread = new Thread(new Runnable() {
            public void run() {
                dependencyMaterialUpdater.stageStatusChanged(StageMother.passedStageInstance("stage-name", "job-name", "pipeline-name"));
            }
        }, "otherthread");
        thread.start();

        updaterThread.join();
        thread.join();
    }

    @Test
    public void stageStatusChanged_shouldNotRemoveCacheKeyOnlyWhenStageHasNotPassed() throws Exception {
        final DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));

        String key = DependencyMaterialUpdater.cacheKeyForDependencyMaterial(dependencyMaterial);
        goCache.put(key, "foo");

        dependencyMaterialUpdater.stageStatusChanged(StageMother.completedFailedStageInstance("pipeline-name", "stage-name", "job-name"));

        assertThat((String) goCache.get(key), is("foo"));
    }

    @Test
    public void stageStatusChanged_shouldRemoveCacheKeyOnlyWhenStageHasPassed() throws Exception {
        final DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));

        String key = DependencyMaterialUpdater.cacheKeyForDependencyMaterial(dependencyMaterial);
        goCache.put(key, "foo");

        dependencyMaterialUpdater.stageStatusChanged(StageMother.createPassedStage("pipeline-name", 1, "stage-name", 1, "job-name", new Date()));

        assertThat(goCache.get(key), is(nullValue()));
    }

    private Stages stages(int... pipelineCounters) {
        Stages stages = new Stages();
        for (int counter : pipelineCounters) {
            stages.add(stage(counter));
        }
        return stages;
    }

    private Stage stage(int pipelineCounter) {
        Stage stage = new Stage();
        stage.setIdentifier(new StageIdentifier("pipeline-name", pipelineCounter, "LABEL-" + pipelineCounter, "stage-name", "0"));
        return stage;
    }

    private void stubStageServiceGetHistory(DependencyMaterial dependencyMaterial, Stages... stageses) {
        if(material == null) {
            dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));
        }
        for (int i = 0; i < stageses.length; i++) {
            ArrayList<Modification> mods = new ArrayList<Modification>();
            for (Stage stage : stageses[i]) {
                StageIdentifier id = stage.getIdentifier();
                mods.add(new Modification(stage.completedDate(), id.stageLocator(), id.getPipelineLabel(), stage.getPipelineId()));
            }
            Mockito.when(dependencyMaterialSourceDao.getPassedStagesByName(dependencyMaterial,
                    Pagination.pageStartingAt(i * MaterialDatabaseUpdater.STAGES_PER_PAGE, null, MaterialDatabaseUpdater.STAGES_PER_PAGE)))
                    .thenReturn(mods);
        }
        Mockito.when(dependencyMaterialSourceDao.getPassedStagesByName(dependencyMaterial,
                Pagination.pageStartingAt(MaterialDatabaseUpdater.STAGES_PER_PAGE * stageses.length, null, MaterialDatabaseUpdater.STAGES_PER_PAGE)
        )).thenReturn(new ArrayList<Modification>());
    }
}
