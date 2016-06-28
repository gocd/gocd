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

import java.util.Date;
import java.util.HashMap;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.activity.JobStatusCache;
import com.thoughtworks.go.domain.activity.StageStatusCache;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.*;
import com.thoughtworks.go.server.messaging.JobResultTopic;
import com.thoughtworks.go.server.messaging.StageStatusTopic;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TestTransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TestTransactionTemplate;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.thoughtworks.go.domain.config.CaseInsensitiveStringMother.str;
import static com.thoughtworks.go.helper.GoConfigMother.createPipelineConfigWithMaterialConfig;
import static com.thoughtworks.go.helper.ModificationsMother.createHgMaterialWithMultipleRevisions;
import static com.thoughtworks.go.helper.ModificationsMother.createSvnMaterialWithMultipleRevisions;
import static com.thoughtworks.go.helper.ModificationsMother.dependencyMaterialRevision;
import static com.thoughtworks.go.helper.ModificationsMother.oneModifiedFile;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class PipelineServiceTest {
    private PipelineTimeline pipelineTimeline;
    private PipelineService service;
    private PipelineSqlMapDao pipelineDao;
    private MaterialRepository materialRepository;
    private Modification first;
    private Modification third;
    private Modification second;
    @Autowired private TransactionTemplate actualTransactionTemplate;
    @Autowired private TransactionSynchronizationManager transactionSynchronizationManager;
    @Autowired private GoConfigService goConfigService;
    @Autowired private GoCache goCache;
    @Autowired private SystemEnvironment systemEnvironment;
    @Autowired private PluginManager pluginManager;
    @Autowired private MaterialConfigConverter materialConfigConverter;

    @Before
    public void setUp() throws Exception {
        pipelineTimeline = mock(PipelineTimeline.class);
        pipelineDao = mock(PipelineSqlMapDao.class);
        materialRepository = mock(MaterialRepository.class);
        TestTransactionSynchronizationManager mockTransactionSynchronizationManager = new TestTransactionSynchronizationManager();
        TransactionTemplate mockTransactionTemplate = new TestTransactionTemplate(mockTransactionSynchronizationManager);
        service = new PipelineService(pipelineDao, mock(StageService.class), mock(PipelineLockService.class), pipelineTimeline, materialRepository, mockTransactionTemplate, systemEnvironment, null,
                materialConfigConverter);
        first = oneModifiedFile("1");
        third = oneModifiedFile("3");
        second = oneModifiedFile("2");
        first.setId(1);
        third.setId(3);
        second.setId(2);
    }

    @Test
    @Ignore("Current implementation of DD does not support Duplicate Materials - Sriki/Sachin")
    public void shouldCopyMissingRevisionsForSameMaterialThatsUsedMoreThanOnce() throws Exception {
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("last");
        pipelineConfig.materialConfigs().clear();
        HgMaterialConfig onDirOne = MaterialConfigsMother.hgMaterialConfig("google.com", "dirOne");
        HgMaterialConfig onDirTwo = MaterialConfigsMother.hgMaterialConfig("google.com", "dirTwo");
        pipelineConfig.addMaterialConfig(onDirOne);
        pipelineConfig.addMaterialConfig(onDirTwo);

        HashMap<Material, String> materialToCommit = new HashMap<Material, String>();
        materialToCommit.put(MaterialsMother.createMaterialFromMaterialConfig(onDirOne), "abc");
        materialToCommit.put(MaterialsMother.createMaterialFromMaterialConfig(onDirTwo), "abc");
        MaterialRevisions revs = ModificationsMother.getMaterialRevisions(materialToCommit);

        PipelineConfigDependencyGraph dependencyGraph = new PipelineConfigDependencyGraph(pipelineConfig);
        MaterialRevisions finalRevisions = service.getRevisionsBasedOnDependencies(revs,
                createCruiseConfigFromGraph(new BasicCruiseConfig(), dependencyGraph), dependencyGraph.getCurrent().name());
        assertThat(finalRevisions.getRevisions(), is(revs.getRevisions()));
    }

    @Test
    public void shouldTellPipelineMaterialModificationsToUpdateItselfOnSave() throws Exception {
        Pipeline pipeline = PipelineMother.pipeline("cruise");
        when(pipelineDao.save(pipeline)).thenReturn(pipeline);
        when(pipelineTimeline.pipelineBefore(anyLong())).thenReturn(9L);
        when(pipelineTimeline.pipelineAfter(pipeline.getId())).thenReturn(-1L);
        when(materialRepository.findMaterialRevisionsForPipeline(9L)).thenReturn(MaterialRevisions.EMPTY);
        service.save(pipeline);
        Mockito.verify(pipelineTimeline).update();
    }

    @Test
    public void shouldNotNotifyStatusListenersWhenTransactionRollsback() throws Exception {
        StageStatusListener stageStatusListener = mock(StageStatusListener.class);
        JobStatusListener jobStatusListener = mock(JobStatusListener.class);
        Pipeline pipeline = stubPipelineSaveForStatusListener(stageStatusListener, jobStatusListener);
        Mockito.doThrow(new RuntimeException()).when(pipelineTimeline).update();

        try {
            service.save(pipeline);
        } catch (RuntimeException e) {
            //ignore
        }
        verify(stageStatusListener, never()).stageStatusChanged(any(Stage.class));
        verify(jobStatusListener, never()).jobStatusChanged(any(JobInstance.class));
    }

    @Test
    public void shouldNotifyStageStatusListenersOnlyWhenTransactionCommits() throws Exception {
        StageStatusListener stageStatusListener = mock(StageStatusListener.class);
        JobStatusListener jobStatusListener = mock(JobStatusListener.class);
        Pipeline pipeline = stubPipelineSaveForStatusListener(stageStatusListener, jobStatusListener);

        service.save(pipeline);

        verify(stageStatusListener).stageStatusChanged(any(Stage.class));
        verify(jobStatusListener).jobStatusChanged(any(JobInstance.class));
    }

    private Pipeline stubPipelineSaveForStatusListener(StageStatusListener stageStatusListener, JobStatusListener jobStatusListener) {
        StageDao stageDao = mock(StageDao.class);
        JobInstanceService jobInstanceService = new JobInstanceService(mock(JobInstanceDao.class), mock(PropertiesService.class), mock(JobResultTopic.class), mock(JobStatusCache.class),
                actualTransactionTemplate, transactionSynchronizationManager, null, null, goConfigService, null, pluginManager, jobStatusListener);

        StageService stageService = new StageService(stageDao, jobInstanceService, mock(StageStatusTopic.class), mock(StageStatusCache.class), mock(SecurityService.class), mock(PipelineDao.class),
                mock(ChangesetService.class), mock(GoConfigService.class), actualTransactionTemplate, transactionSynchronizationManager,
                goCache);
        Stage savedStage = StageMother.passedStageInstance("stage", "job", "pipeline-name");
        when(stageDao.save(any(Pipeline.class), any(Stage.class))).thenReturn(savedStage);

        stageService.addStageStatusListener(stageStatusListener);

        service = new PipelineService(pipelineDao, stageService, mock(PipelineLockService.class), pipelineTimeline, materialRepository, actualTransactionTemplate, systemEnvironment, null, materialConfigConverter);
        Pipeline pipeline = PipelineMother.pipeline("cruise", savedStage);
        when(pipelineDao.save(pipeline)).thenReturn(pipeline);
        when(pipelineTimeline.pipelineBefore(anyLong())).thenReturn(9L);
        when(pipelineTimeline.pipelineAfter(pipeline.getId())).thenReturn(-1L);
        when(materialRepository.findMaterialRevisionsForPipeline(9L)).thenReturn(MaterialRevisions.EMPTY);
        return pipeline;
    }

    @Test
    public void shouldUpdateTheToAndFromRevisionOfThePipelineAfterThePipelineBeingSaved() throws Exception {
        MaterialRevisions scheduleTime = createHgMaterialWithMultipleRevisions(1L, first);
        scheduleTime.addAll(createSvnMaterialWithMultipleRevisions(2, first));

        MaterialRevisions next = createHgMaterialWithMultipleRevisions(1L, third, second, first);
        next.addAll(createSvnMaterialWithMultipleRevisions(2, third, second, first));

        MaterialRevisions expected = createHgMaterialWithMultipleRevisions(1L, third, second);
        expected.addAll(createSvnMaterialWithMultipleRevisions(2L, third, second));

        Pipeline pipeline = pipeline(scheduleTime, jobs());
        Pipeline savedPipeline = pipeline(scheduleTime, jobs());

        when(pipelineTimeline.pipelineAfter(pipeline.getId())).thenReturn(11L);
        when(pipelineTimeline.pipelineBefore(pipeline.getId())).thenReturn(-1L);
        when(materialRepository.findMaterialRevisionsForPipeline(11L)).thenReturn(next);
        when(pipelineDao.save(pipeline)).thenReturn(savedPipeline);

        service.save(pipeline);
    }

    @Test
    @Ignore("We do not support this now")
    public void shouldGetTheRevisionsFromTheUpStreamPipelineBasedOnCurrentConfiguration() throws Exception {
        MaterialRevisions expectedIfPegged = createHgMaterialWithMultipleRevisions(1L, first);
        MaterialRevision up1Revision = dependencyMaterialRevision("up1", 1, "label", "stage", 1, new Date());
        up1Revision.markAsChanged();
        expectedIfPegged.addRevision(up1Revision);

        MaterialRevisions actual = createHgMaterialWithMultipleRevisions(1L, third);
        actual.addRevision(up1Revision);

        PipelineConfig current = createPipelineConfigWithMaterialConfig("current", actual.getMaterials().get(0).config(),
                new DependencyMaterialConfig(new CaseInsensitiveString("up1"), new CaseInsensitiveString("first")));
        GitMaterialConfig gitMaterialConfig = new GitMaterialConfig("git", "master");
        gitMaterialConfig.setFolder("folder");
        PipelineConfig up1 = createPipelineConfigWithMaterialConfig("up1", gitMaterialConfig); // The pipeline does not have the material anymore

        when(pipelineDao.findBuildCauseOfPipelineByNameAndCounter("up1", 1)).thenReturn(BuildCause.createManualForced(expectedIfPegged, new Username(str("loser"))));

        PipelineConfigDependencyGraph dependencyGraph = new PipelineConfigDependencyGraph(current, new PipelineConfigDependencyGraph(up1));
        assertThat(service.getRevisionsBasedOnDependencies(actual, createCruiseConfigFromGraph(new BasicCruiseConfig(), dependencyGraph), dependencyGraph.getCurrent().name()), is(actual));
    }

    @Test
    public void shouldReturnTheOrderedListOfStageIdentifiers() throws Exception {
        //TODO: does it? while we trust it, may be its a good idea to validate --shilpa & jj
    }

    private JobConfigs jobs() {
        JobConfigs configs = new JobConfigs();
        configs.add(new JobConfig("job"));
        return configs;
    }

    private Pipeline pipeline(MaterialRevisions scheduleTime, JobConfigs configs) {
        Pipeline pipeline = PipelineMother.schedule(PipelineConfigMother.pipelineConfig("mummy", scheduleTime.getMaterialRevision(0).getMaterial().config(), configs),
                BuildCause.createWithModifications(scheduleTime, "me"));
        pipeline.setId(10);
        return pipeline;
    }

    private CruiseConfig createCruiseConfigFromGraph(CruiseConfig cruiseConfig, final PipelineConfigDependencyGraph pdg) {
        String groupName = "defaultGroup";
        cruiseConfig.addPipeline(groupName, pdg.getCurrent());
        for (PipelineConfigDependencyGraph upstreamDependency : pdg.getUpstreamDependencies()) {
            createCruiseConfigFromGraph(cruiseConfig, upstreamDependency);
        }
        return cruiseConfig;
    }
}
