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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.config.JobConfigs;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
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
import com.thoughtworks.go.server.domain.JobStatusListener;
import com.thoughtworks.go.server.domain.PipelineConfigDependencyGraph;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.domain.StageStatusListener;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.functional.helpers.MaterialRevisionBuilder;
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.thoughtworks.go.helper.GoConfigMother.createPipelineConfigWithMaterialConfig;
import static com.thoughtworks.go.helper.ModificationsMother.changedDependencyMaterialRevision;
import static com.thoughtworks.go.helper.ModificationsMother.createHgMaterialWithMultipleRevisions;
import static com.thoughtworks.go.helper.ModificationsMother.createSvnMaterialWithMultipleRevisions;
import static com.thoughtworks.go.helper.ModificationsMother.dependencyMaterialRevision;
import static com.thoughtworks.go.helper.ModificationsMother.oneModifiedFile;
import static com.thoughtworks.go.domain.config.CaseInsensitiveStringMother.str;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
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
public class PipelineServiceTriangleDependencyTest {
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

    @Before public void setUp() throws Exception {
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

        MaterialRevisions finalRevisions = service.getRevisionsBasedOnDependencies(new PipelineConfigDependencyGraph(pipelineConfig), revs);
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

        service = new PipelineService(pipelineDao, stageService, mock(PipelineLockService.class), pipelineTimeline, materialRepository, actualTransactionTemplate,systemEnvironment, null, materialConfigConverter);
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
    public void shouldGetTheRevisionsFromTheUpStreamPipelineThatUsesTheSameMaterial() throws Exception {
        MaterialRevisions expected = new MaterialRevisions();
        MaterialRevision up1Revision = dependencyMaterialRevision("up1", 1, "label", "stage", 1, new Date());
        up1Revision.markAsChanged();
        expected.addRevision(up1Revision);
        expected.addAll(createHgMaterialWithMultipleRevisions(1L, first));
        expected.addAll(createSvnMaterialWithMultipleRevisions(2L, third));

        MaterialRevisions actual = new MaterialRevisions();
        actual.addRevision(up1Revision);
        actual.addAll(createHgMaterialWithMultipleRevisions(1L, third));
        actual.addAll(createSvnMaterialWithMultipleRevisions(2L, third));

        PipelineConfig current = createPipelineConfigWithMaterialConfig("current", new DependencyMaterialConfig(new CaseInsensitiveString("up1"), new CaseInsensitiveString("first")),
                actual.getMaterials().get(0).config());
        PipelineConfig up1 = createPipelineConfigWithMaterialConfig("up1", expected.getMaterials().get(1).config(), expected.getMaterials().get(2).config());

        Pipeline pipeline = PipelineMother.passedPipelineInstance("up1", "stage", "job");
        pipeline.setId(10);
        when(pipelineDao.findPipelineByNameAndCounter("up1", 1)).thenReturn(pipeline);
        when(materialRepository.findMaterialRevisionsForPipeline(10)).thenReturn(expected);

        PipelineConfigDependencyGraph dependencyGraph = new PipelineConfigDependencyGraph(current, new PipelineConfigDependencyGraph(up1));
        assertThat(service.getRevisionsBasedOnDependencies(dependencyGraph, actual), is(expected));
    }

    @Test
    public void shouldGetTheRevisionsFromTheUpStreamPipelineFor2SameMaterial() throws Exception {
        MaterialRevision up1Revision = dependencyMaterialRevision("up1", 1, "label", "stage", 1, new Date());
        up1Revision.markAsChanged();

        MaterialRevisions expected = new MaterialRevisions();
        expected.addRevision(up1Revision);
        expected.addAll(createHgMaterialWithMultipleRevisions(1L, first));
        expected.addAll(createSvnMaterialWithMultipleRevisions(2L, first));

        MaterialRevisions actual = new MaterialRevisions();
        actual.addRevision(up1Revision);
        actual.addAll(createHgMaterialWithMultipleRevisions(1L, third));
        actual.addAll(createSvnMaterialWithMultipleRevisions(2L, third));

        PipelineConfig current = createPipelineConfigWithMaterialConfig("current", new DependencyMaterialConfig(new CaseInsensitiveString("up1"), new CaseInsensitiveString("first")),
                MaterialConfigsMother.hgMaterialConfig(), MaterialConfigsMother.svnMaterialConfig());
        PipelineConfig up1 = createPipelineConfigWithMaterialConfig("up1", MaterialConfigsMother.hgMaterialConfig(), MaterialConfigsMother.svnMaterialConfig());

        Pipeline pipeline = PipelineMother.passedPipelineInstance("up1", "stage", "job");
        pipeline.setId(10);
        when(pipelineDao.findPipelineByNameAndCounter("up1", 1)).thenReturn(pipeline);
        when(materialRepository.findMaterialRevisionsForPipeline(10)).thenReturn(expected);

        PipelineConfigDependencyGraph dependencyGraph = new PipelineConfigDependencyGraph(current, new PipelineConfigDependencyGraph(up1));
        assertThat(service.getRevisionsBasedOnDependencies(dependencyGraph, actual), is(expected));
    }

    @Test
    public void shouldChooseTheRevisionFromThirdWhenSecondIsNotModified() throws Exception {
        //      Third* <- Second
        //         |     /
        //         |   /
        //         Last
        //
        // * indicates changed

        Date date = new Date();
        MaterialRevisionBuilder builder = new MaterialRevisionBuilder(pipelineDao, materialRepository);
        PipelineConfigDependencyGraph graph = builder.depInstance("last", 1, date, builder.depInstance("third", 3, date, builder.depInstance("second", 2, date, builder.svnInstance("1", date))),
                builder.depInstance("second", 4, date, builder.svnInstance("2", date))).getGraph();

        MaterialRevisions actual = new MaterialRevisions();
        actual.addRevision(builder.lookingAtDep("third", 3, date).markAsChanged().revision());
        actual.addRevision(builder.lookingAtDep("second", 4, date).revision());

        MaterialRevisions expected = new MaterialRevisions();
        expected.addRevision(builder.depInstance("third", 3, date).getRevision());
        expected.addRevision(builder.depInstance("second", 2, date).getRevision());

        MaterialRevisions finalRevisions = service.getRevisionsBasedOnDependencies(graph, actual);
        assertThat(finalRevisions, is(expected));
        for (int i = 0; i < expected.numberOfRevisions(); i++) {
            assertTrue(finalRevisions.getMaterialRevision(i) == actual.getMaterialRevision(i));
        }
    }

    @Test
    public void shouldChooseTheRevisionFromSecondWhenThirdIsNotModified() throws Exception {
        //      Third <- Second*
        //         |     /
        //         |   /
        //         Last
        //
        // * indicates changed

        Date date = new Date();
        MaterialRevisionBuilder builder = new MaterialRevisionBuilder(pipelineDao, materialRepository);
        PipelineConfigDependencyGraph graph = builder.depInstance("last", 1, date, builder.depInstance("third", 3, date,
                builder.depInstance("second", 2, date, builder.svnInstance("1", date))),
                builder.depInstance("second", 4, date, builder.svnInstance("2", date))).getGraph();

        MaterialRevisions actual = new MaterialRevisions();
        actual.addRevision(builder.lookingAtDep("third", 3, date).revision());
        actual.addRevision(builder.lookingAtDep("second", 4, date).markAsChanged().revision());

        MaterialRevisions expected = new MaterialRevisions();
        expected.addRevision(builder.depInstance("third", 3, date).getRevision());
        expected.addRevision(builder.depInstance("second", 4, date).getRevision());

        MaterialRevisions finalRevisions = service.getRevisionsBasedOnDependencies(graph, actual);
        assertThat(finalRevisions, is(expected));
        for (int i = 0; i < expected.numberOfRevisions(); i++) {
            assertTrue(finalRevisions.getMaterialRevision(i) == actual.getMaterialRevision(i));
        }
    }

    @Test
    public void shouldChooseTheRevisionFromSecondWhenThirdIsNotModifiedInspiteOfSecondBeingFirstMaterialInConfig() throws Exception {
        //      Third <- Second*
        //         |     /
        //         |   /
        //         Last
        //
        // * indicates changed

        Date date = new Date();
        MaterialRevisionBuilder builder = new MaterialRevisionBuilder(pipelineDao, materialRepository);
        PipelineConfigDependencyGraph graph = builder.depInstance("last", 1, date, builder.depInstance("second", 4, date,
                builder.svnInstance("2", date)), builder.depInstance("third", 3, date,
                builder.depInstance("second", 2, date, builder.svnInstance("1", date)))).getGraph();

        MaterialRevisions actual = new MaterialRevisions();
        actual.addRevision(builder.lookingAtDep("second", 4, date).markAsChanged().revision());
        actual.addRevision(builder.lookingAtDep("third", 3, date).revision());

        MaterialRevisions expected = new MaterialRevisions();
        expected.addRevision(builder.depInstance("second", 4, date).getRevision());
        expected.addRevision(builder.depInstance("third", 3, date).getRevision());

        MaterialRevisions finalRevisions = service.getRevisionsBasedOnDependencies(graph, actual);
        assertThat(finalRevisions, is(expected));
        for (int i = 0; i < expected.numberOfRevisions(); i++) {
            assertTrue(finalRevisions.getMaterialRevision(i) == actual.getMaterialRevision(i));
        }
    }

    @Test
    public void shouldChooseTheRevisionFromThirdWhenBothThirdAndSecondAreModified() throws Exception {
        //      Third* <- Second*
        //         |     /
        //         |   /
        //         Last
        //
        // * indicates changed

        Date date = new Date();
        MaterialRevisionBuilder builder = new MaterialRevisionBuilder(pipelineDao, materialRepository);
        PipelineConfigDependencyGraph graph = builder.depInstance("last", 1, date, builder.depInstance("third", 3, date, builder.depInstance("second", 2, date, builder.svnInstance("1", date))),
                builder.depInstance("second", 4, date, builder.svnInstance("2", date))).getGraph();

        MaterialRevisions actual = new MaterialRevisions();
        actual.addRevision(builder.lookingAtDep("third", 3, date).markAsChanged().revision());
        actual.addRevision(builder.lookingAtDep("second", 4, date).markAsChanged().revision());

        MaterialRevisions expected = new MaterialRevisions();
        expected.addRevision(builder.depInstance("third", 3, date).getRevision());
        expected.addRevision(builder.depInstance("second", 2, date).getRevision());

        MaterialRevisions finalRevisions = service.getRevisionsBasedOnDependencies(graph, actual);
        assertThat(finalRevisions, is(expected));
        for (int i = 0; i < expected.numberOfRevisions(); i++) {
            assertTrue(finalRevisions.getMaterialRevision(i) == actual.getMaterialRevision(i));
        }
    }

    @Test
    public void shouldChooseTheRevisionFromThirdWhenSecondComesBeforeThirdInConfiguration() throws Exception {
        //      Third* <- Second*
        //         |     /
        //         |   /
        //         Last
        //
        // * indicates changed

        Date date = new Date();
        MaterialRevisionBuilder builder = new MaterialRevisionBuilder(pipelineDao, materialRepository);
        PipelineConfigDependencyGraph graph = builder.depInstance("last", 1, date, builder.depInstance("second", 4, date,
                builder.svnInstance("2", date)), builder.depInstance("third", 3, date,
                builder.depInstance("second", 2, date, builder.svnInstance("1", date)))).getGraph();

        MaterialRevisions actual = new MaterialRevisions();
        actual.addRevision(builder.lookingAtDep("second", 4, date).markAsChanged().revision());
        actual.addRevision(builder.lookingAtDep("third", 3, date).markAsChanged().revision());

        MaterialRevisions expected = new MaterialRevisions();
        expected.addRevision(builder.depInstance("second", 2, date).getRevision());
        expected.addRevision(builder.depInstance("third", 3, date).getRevision());

        MaterialRevisions finalRevisions = service.getRevisionsBasedOnDependencies(graph, actual);
        assertThat(finalRevisions, is(expected));
        for (int i = 0; i < expected.numberOfRevisions(); i++) {
            assertTrue(finalRevisions.getMaterialRevision(i) == actual.getMaterialRevision(i));
        }
    }

    @Test
    public void shouldChooseTheRevisionFromSecondInAComplexSituation() throws Exception {
        // hg -> First          git
        //  |      \             |
        //  |      Third  <- Second*
        //  |        |      /
        //  |        |    /
        //  +------> Last
        //
        // * indicates changed

        Date date = new Date();
        MaterialRevisionBuilder builder = new MaterialRevisionBuilder(pipelineDao, materialRepository);
        PipelineConfigDependencyGraph graph = builder.depInstance("last", 1, date, builder.hgInstance("rev2", date),
                builder.depInstance("second", 4, date, builder.svnInstance("2", date)),
                builder.depInstance("third", 3, date, builder.depInstance("first", 1, date, builder.hgInstance("rev1", date)),
                        builder.depInstance("second", 2, date, builder.svnInstance("1", date)))).getGraph();

        MaterialRevisions actual = new MaterialRevisions();
        actual.addRevision(builder.lookingAtHg("rev2", date).markAsChanged().revision());
        actual.addRevision(builder.lookingAtDep("second", 4, date).markAsChanged().revision());
        actual.addRevision(builder.lookingAtDep("third", 3, date).markAsChanged().revision());

        MaterialRevisions expected = new MaterialRevisions();
        expected.addRevision(builder.hgInstance("rev1", date).getRevision());
        expected.addRevision(builder.depInstance("second", 2, date).getRevision());
        expected.addRevision(builder.depInstance("third", 3, date).getRevision());

        MaterialRevisions finalRevisions = service.getRevisionsBasedOnDependencies(graph, actual);
        assertThat(finalRevisions, is(expected));
        for (int i = 0; i < expected.numberOfRevisions(); i++) {
            assertTrue(finalRevisions.getMaterialRevision(i) == actual.getMaterialRevision(i));
        }
    }

    @Test
    public void shouldIgnoreUpstreamPipelineWhenThereIsNothingInCommon() throws Exception {
        MaterialRevision up1Revision = dependencyMaterialRevision("up1", 1, "label", "stage", 1, new Date());
        up1Revision.markAsChanged();

        MaterialRevision earlierUp0Revision = dependencyMaterialRevision("up0", 1, "label", "stage", 1, new Date());

        MaterialRevision laterUp0Revision = dependencyMaterialRevision("up0", 2, "label", "stage", 1, new Date());
        laterUp0Revision.markAsChanged();

        MaterialRevisions expected = new MaterialRevisions();
        expected.addRevision(earlierUp0Revision);
        expected.addRevision(up1Revision);
        expected.addAll(createSvnMaterialWithMultipleRevisions(2L, third));

        MaterialRevisions allUp1Revisions = new MaterialRevisions();
        allUp1Revisions.addRevision(earlierUp0Revision);

        MaterialRevisions allUp0Revisions = new MaterialRevisions();
        allUp0Revisions.addAll(createHgMaterialWithMultipleRevisions(3L, first));

        MaterialRevisions actual = new MaterialRevisions();
        actual.addRevision(laterUp0Revision);
        actual.addRevision(up1Revision);
        actual.addAll(createSvnMaterialWithMultipleRevisions(2L, third));

        DependencyMaterialConfig sameUpstream = new DependencyMaterialConfig(new CaseInsensitiveString("up0"), new CaseInsensitiveString("stage"));

        PipelineConfig current = createPipelineConfigWithMaterialConfig("current", sameUpstream,
                new DependencyMaterialConfig(new CaseInsensitiveString("up1"), new CaseInsensitiveString("stage")),
                MaterialConfigsMother.svnMaterialConfig());
        PipelineConfig up1 = createPipelineConfigWithMaterialConfig("up1", sameUpstream);
        PipelineConfig up0 = createPipelineConfigWithMaterialConfig("up0", MaterialConfigsMother.hgMaterialConfig());

        Pipeline up1Pipeline = PipelineMother.passedPipelineInstance("up1", "stage", "job");
        up1Pipeline.setId(10);

        Pipeline up0Pipeline = PipelineMother.passedPipelineInstance("up0", "stage", "job");
        up0Pipeline.setId(5);

        when(pipelineDao.findPipelineByNameAndCounter("up1", 1)).thenReturn(up1Pipeline);
        when(materialRepository.findMaterialRevisionsForPipeline(10)).thenReturn(allUp1Revisions);

        when(pipelineDao.findPipelineByNameAndCounter("up0", 1)).thenReturn(up0Pipeline);
        when(materialRepository.findMaterialRevisionsForPipeline(5)).thenReturn(allUp0Revisions);

        when(pipelineDao.findPipelineByNameAndCounter("up0", 2)).thenReturn(up0Pipeline);
        when(materialRepository.findMaterialRevisionsForPipeline(5)).thenReturn(allUp0Revisions);

        PipelineConfigDependencyGraph dependencyGraph = new PipelineConfigDependencyGraph(current, new PipelineConfigDependencyGraph(up1, new PipelineConfigDependencyGraph(up0)),
                new PipelineConfigDependencyGraph(up0));
        assertThat(service.getRevisionsBasedOnDependencies(dependencyGraph, actual), is(expected));
    }

    @Test
    public void shouldGetTheRevisionsForDependencyMaterialFromUpStreamPipeline() throws Exception {
        Date modifiedTime = new Date();

        MaterialRevisions expected = new MaterialRevisions();
        expected.addRevision(dependencyMaterialRevision("up1", 1, "label", "stage", 1, modifiedTime));
        expected.addRevision(dependencyMaterialRevision("common", 3, "label", "first", 1, modifiedTime));

        MaterialRevisions actual = new MaterialRevisions();
        actual.addRevision(changedDependencyMaterialRevision("up1", 1, "label", "stage", 1, modifiedTime));
        actual.addRevision(dependencyMaterialRevision("common", 4, "label", "first", 1, modifiedTime));

        PipelineConfig current = createPipelineConfigWithMaterialConfig("current", new DependencyMaterialConfig(new CaseInsensitiveString("common"), new CaseInsensitiveString("first")),
                new DependencyMaterialConfig(new CaseInsensitiveString("up1"), new CaseInsensitiveString("first")));
        PipelineConfig up1 = createPipelineConfigWithMaterialConfig("up1", new DependencyMaterialConfig(new CaseInsensitiveString("common"), new CaseInsensitiveString("first")));
        PipelineConfig common = createPipelineConfigWithMaterialConfig("common", MaterialConfigsMother.hgMaterialConfig());

        Pipeline pipeline = PipelineMother.passedPipelineInstance("up1", "stage", "job");
        pipeline.setId(10);
        when(pipelineDao.findPipelineByNameAndCounter("up1", 1)).thenReturn(pipeline);

        MaterialRevisions upStreamPipelinesRevisions = new MaterialRevisions();
        upStreamPipelinesRevisions.addRevision(dependencyMaterialRevision("common", 3, "label", "first", 1, modifiedTime));
        when(materialRepository.findMaterialRevisionsForPipeline(10)).thenReturn(upStreamPipelinesRevisions);

        PipelineConfigDependencyGraph dependencyGraph = new PipelineConfigDependencyGraph(current, new PipelineConfigDependencyGraph(up1, new PipelineConfigDependencyGraph(common)));
        MaterialRevisions finalRevisions = service.getRevisionsBasedOnDependencies(dependencyGraph, actual);

        assertThat(finalRevisions, is(expected));
        for (int i = 0; i < expected.numberOfRevisions(); i++) {
            assertTrue(finalRevisions.getMaterialRevision(i) == actual.getMaterialRevision(i));
        }
    }

    @Test
    public void shouldGetTheRevisionsForDependencyMaterial_WithSharedParentInMiddleOfTheTree() throws Exception {
        Date modifiedTime = new Date();

        MaterialRevisions expected = new MaterialRevisions();
        expected.addRevision(dependencyMaterialRevision("up1", 1, "label", "stage", 1, modifiedTime));
        expected.addRevision(dependencyMaterialRevision("common", 3, "label", "first", 1, modifiedTime));

        MaterialRevisions actual = new MaterialRevisions();
        actual.addRevision(changedDependencyMaterialRevision("up1", 1, "label", "stage", 1, modifiedTime));
        actual.addRevision(changedDependencyMaterialRevision("common", 4, "label", "first", 1, modifiedTime));

        PipelineConfig current = createPipelineConfigWithMaterialConfig("current", new DependencyMaterialConfig(new CaseInsensitiveString("common"), new CaseInsensitiveString("first")),
                new DependencyMaterialConfig(new CaseInsensitiveString("up1"), new CaseInsensitiveString("first")));
        PipelineConfig up1 = createPipelineConfigWithMaterialConfig("up1", new DependencyMaterialConfig(new CaseInsensitiveString("common"), new CaseInsensitiveString("first")));
        PipelineConfig common = createPipelineConfigWithMaterialConfig("common", new DependencyMaterialConfig(new CaseInsensitiveString("commonsParent"), new CaseInsensitiveString("first")));
        PipelineConfig commonsParent = createPipelineConfigWithMaterialConfig("commonsParent", MaterialConfigsMother.hgMaterialConfig());

        Pipeline pipeline = PipelineMother.passedPipelineInstance("up1", "stage", "job");
        pipeline.setId(10);
        when(pipelineDao.findPipelineByNameAndCounter("up1", 1)).thenReturn(pipeline);

        MaterialRevisions upStreamPipelinesRevisions = new MaterialRevisions();
        upStreamPipelinesRevisions.addRevision(dependencyMaterialRevision("common", 3, "label", "first", 1, modifiedTime));
        when(materialRepository.findMaterialRevisionsForPipeline(10)).thenReturn(upStreamPipelinesRevisions);

        Pipeline commonPipeline = PipelineMother.passedPipelineInstance("common", "first", "job");
        commonPipeline.setId(5);
        when(pipelineDao.findPipelineByNameAndCounter("common", 3)).thenReturn(commonPipeline);

        MaterialRevisions commonPipelinesRevisions = new MaterialRevisions();
        upStreamPipelinesRevisions.addRevision(dependencyMaterialRevision("commonsParent", 2, "label-2", "first", 1, modifiedTime));
        when(materialRepository.findMaterialRevisionsForPipeline(5)).thenReturn(commonPipelinesRevisions);

        PipelineConfigDependencyGraph dependencyGraph = new PipelineConfigDependencyGraph(current,
                new PipelineConfigDependencyGraph(up1, new PipelineConfigDependencyGraph(common, new PipelineConfigDependencyGraph(commonsParent))),
                new PipelineConfigDependencyGraph(common, new PipelineConfigDependencyGraph(commonsParent)));
        MaterialRevisions finalRevisions = service.getRevisionsBasedOnDependencies(dependencyGraph, actual);

        assertThat(finalRevisions, is(expected));
        for (int i = 0; i < expected.numberOfRevisions(); i++) {
            assertTrue(finalRevisions.getMaterialRevision(i) == actual.getMaterialRevision(i));
        }
    }

    @Test
    public void shouldGetTheRevisionsFromTheUpStreamPipelineBasedOnCurrentConfiguration() throws Exception {
        MaterialRevisions expectedIfPegged = createHgMaterialWithMultipleRevisions(1L, first);
        MaterialRevision up1Revision = dependencyMaterialRevision("up1", 1, "label", "stage", 1, new Date());
        up1Revision.markAsChanged();
        expectedIfPegged.addRevision(up1Revision);

        MaterialRevisions actual = createHgMaterialWithMultipleRevisions(1L, third);
        actual.addRevision(up1Revision);

        PipelineConfig current = createPipelineConfigWithMaterialConfig("current", actual.getMaterials().get(0).config(),
                new DependencyMaterialConfig(new CaseInsensitiveString("up1"), new CaseInsensitiveString("first")));
        PipelineConfig up1 = createPipelineConfigWithMaterialConfig("up1");//The pipeline does not have the material anymore

        when(pipelineDao.findBuildCauseOfPipelineByNameAndCounter("up1", 1)).thenReturn(BuildCause.createManualForced(expectedIfPegged, new Username(str("loser"))));

        PipelineConfigDependencyGraph dependencyGraph = new PipelineConfigDependencyGraph(current, new PipelineConfigDependencyGraph(up1));
        assertThat(service.getRevisionsBasedOnDependencies(dependencyGraph, actual), is(actual));
    }

    @Test
    public void shouldGetTheRevisionsFromTheNearestUpStreamPipeline() throws Exception {
        MaterialRevisions uppestRevision = createHgMaterialWithMultipleRevisions(1L, first);
        MaterialRevisions secondHgRevision = createHgMaterialWithMultipleRevisions(1L, second);
        ((HgMaterial) secondHgRevision.getMaterialRevision(0).getMaterial()).setFolder("mother");
        MaterialRevisions upRevision = new MaterialRevisions(secondHgRevision.getMaterialRevision(0), dependencyMaterialRevision("up0", 2, "label", "stage", 1, new Date()));

        MaterialRevisions actual = createHgMaterialWithMultipleRevisions(1L, third);
        ((HgMaterial) actual.getMaterials().get(0)).setFolder("mother");
        MaterialRevision up1Modification = dependencyMaterialRevision("up1", 1, "label", "stage", 1, new Date());
        up1Modification.markAsChanged();
        actual.addRevision(up1Modification);

        PipelineConfig current = createPipelineConfigWithMaterialConfig("current", actual.getMaterials().get(0).config(),
                new DependencyMaterialConfig(new CaseInsensitiveString("up1"), new CaseInsensitiveString("stage")));
        PipelineConfig up1 = createPipelineConfigWithMaterialConfig("up1", upRevision.getMaterials().get(0).config(), upRevision.getMaterials().get(1).config());
        PipelineConfig up0 = createPipelineConfigWithMaterialConfig("up0", MaterialConfigsMother.hgMaterialConfig());

        Pipeline pipeline = PipelineMother.passedPipelineInstance("up1", "stage", "job");
        pipeline.setId(10);

        Pipeline uppestPipeline = PipelineMother.passedPipelineInstance("up0", "stage", "job");
        uppestPipeline.setId(5);

        when(pipelineDao.findPipelineByNameAndCounter("up1", 1)).thenReturn(pipeline);
        when(materialRepository.findMaterialRevisionsForPipeline(10)).thenReturn(upRevision);
        when(pipelineDao.findPipelineByNameAndCounter("up0", 2)).thenReturn(uppestPipeline);
        when(materialRepository.findMaterialRevisionsForPipeline(5)).thenReturn(uppestRevision);

        PipelineConfigDependencyGraph dependencyGraph = new PipelineConfigDependencyGraph(current, new PipelineConfigDependencyGraph(up1, new PipelineConfigDependencyGraph(up0)));

        secondHgRevision.addRevision(up1Modification);

        assertThat(service.getRevisionsBasedOnDependencies(dependencyGraph, actual), is(secondHgRevision));
    }

    @Test
    public void shouldNotGetTheRevisionsFromUpStreamPipelineIfTheDependencyMaterialHasNotChanged() throws Exception {
        MaterialRevisions expected = createHgMaterialWithMultipleRevisions(1L, first);
        MaterialRevision up1Revision = dependencyMaterialRevision("up1", 1, "label", "stage", 1, new Date());
        expected.addRevision(up1Revision);

        MaterialRevisions actual = createHgMaterialWithMultipleRevisions(1L, third);
        actual.addRevision(up1Revision);
        up1Revision.markAsNotChanged();

        PipelineConfig current = createPipelineConfigWithMaterialConfig("current", actual.getMaterials().get(0).config(),
                new DependencyMaterialConfig(new CaseInsensitiveString("up1"), new CaseInsensitiveString("first")));
        PipelineConfig up1 = createPipelineConfigWithMaterialConfig("up1", expected.getMaterials().get(0).config());

        when(pipelineDao.findBuildCauseOfPipelineByNameAndCounter("up1", 1)).thenReturn(BuildCause.createManualForced(expected, new Username(str("loser"))));

        PipelineConfigDependencyGraph dependencyGraph = new PipelineConfigDependencyGraph(current, new PipelineConfigDependencyGraph(up1));
        assertThat(service.getRevisionsBasedOnDependencies(dependencyGraph, actual), is(actual));
    }

    @Test
    public void shouldGetTheRevisionsFromTheUpStreamPipelineThatUsesTheSameMaterialEvenIfItIsNotADirectMaterial() throws Exception {
        MaterialRevisions uppestRevision = createHgMaterialWithMultipleRevisions(1L, first);
        MaterialRevisions upRevision = new MaterialRevisions(dependencyMaterialRevision("up0", 2, "label", "stage", 1, new Date()));

        MaterialRevisions expected = new MaterialRevisions(uppestRevision.getMaterialRevision(0));
        MaterialRevision up1Revision = dependencyMaterialRevision("up1", 1, "label", "stage", 1, new Date());
        up1Revision.markAsChanged();
        expected.addRevision(up1Revision);

        MaterialRevisions actual = createHgMaterialWithMultipleRevisions(1L, third);
        actual.addRevision(up1Revision);

        PipelineConfig current = createPipelineConfigWithMaterialConfig("current", actual.getMaterials().get(0).config(),
                new DependencyMaterialConfig(new CaseInsensitiveString("up1"), new CaseInsensitiveString("stage")));
        PipelineConfig up1 = createPipelineConfigWithMaterialConfig("up1", upRevision.getMaterials().get(0).config());
        PipelineConfig up0 = createPipelineConfigWithMaterialConfig("up0", uppestRevision.getMaterials().get(0).config());

        Pipeline pipeline = PipelineMother.passedPipelineInstance("up1", "stage", "job");
        pipeline.setId(10);

        Pipeline uppestPipeline = PipelineMother.passedPipelineInstance("up0", "stage", "job");
        uppestPipeline.setId(5);

        when(pipelineDao.findPipelineByNameAndCounter("up1", 1)).thenReturn(pipeline);
        when(materialRepository.findMaterialRevisionsForPipeline(10)).thenReturn(upRevision);
        when(pipelineDao.findPipelineByNameAndCounter("up0", 2)).thenReturn(uppestPipeline);
        when(materialRepository.findMaterialRevisionsForPipeline(5)).thenReturn(uppestRevision);

        PipelineConfigDependencyGraph dependencyGraph = new PipelineConfigDependencyGraph(current, new PipelineConfigDependencyGraph(up1, new PipelineConfigDependencyGraph(up0)));
        assertThat(service.getRevisionsBasedOnDependencies(dependencyGraph, actual), is(expected));
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
}
