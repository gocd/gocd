/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.config.JobConfigs;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.activity.JobStatusCache;
import com.thoughtworks.go.domain.activity.StageStatusCache;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.JobStatusListener;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.domain.StageStatusListener;
import com.thoughtworks.go.server.messaging.JobResultTopic;
import com.thoughtworks.go.server.messaging.StageStatusTopic;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TestTransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TestTransactionTemplate;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthStates;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.thoughtworks.go.helper.ModificationsMother.*;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class PipelineServiceTest {
    private PipelineTimeline pipelineTimeline;
    private PipelineService service;
    private PipelineSqlMapDao pipelineDao;
    private MaterialRepository materialRepository;
    private Modification first;
    private Modification third;
    private Modification second;
    @Autowired
    private TransactionTemplate actualTransactionTemplate;
    @Autowired
    private TransactionSynchronizationManager transactionSynchronizationManager;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private GoCache goCache;
    @Autowired
    private SystemEnvironment systemEnvironment;
    @Autowired
    private MaterialConfigConverter materialConfigConverter;

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
    public void shouldTellPipelineMaterialModificationsToUpdateItselfOnSave() throws Exception {
        Pipeline pipeline = PipelineMother.pipeline("cruise");
        when(pipelineDao.save(pipeline)).thenReturn(pipeline);
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
        ServerHealthService serverHealthService = mock(ServerHealthService.class);
        when(serverHealthService.logs()).thenReturn(new ServerHealthStates());
        JobInstanceService jobInstanceService = new JobInstanceService(mock(JobInstanceDao.class), mock(JobResultTopic.class), mock(JobStatusCache.class),
                actualTransactionTemplate, transactionSynchronizationManager, null, null, goConfigService, null, serverHealthService, jobStatusListener);

        StageService stageService = new StageService(stageDao, jobInstanceService, mock(StageStatusTopic.class), mock(StageStatusCache.class), mock(SecurityService.class), mock(PipelineDao.class),
                mock(ChangesetService.class), mock(GoConfigService.class), actualTransactionTemplate, transactionSynchronizationManager,
                goCache);
        Stage savedStage = StageMother.passedStageInstance("stage", "job", "pipeline-name");
        when(stageDao.save(any(Pipeline.class), any(Stage.class))).thenReturn(savedStage);

        stageService.addStageStatusListener(stageStatusListener);

        service = new PipelineService(pipelineDao, stageService, mock(PipelineLockService.class), pipelineTimeline, materialRepository, actualTransactionTemplate, systemEnvironment, null, materialConfigConverter);
        Pipeline pipeline = PipelineMother.pipeline("cruise", savedStage);
        when(pipelineDao.save(pipeline)).thenReturn(pipeline);
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

        when(materialRepository.findMaterialRevisionsForPipeline(11L)).thenReturn(next);
        when(pipelineDao.save(pipeline)).thenReturn(savedPipeline);

        service.save(pipeline);
    }

    @Test
    public void shouldReturnTheOrderedListOfStageIdentifiers() throws Exception {
        //TODO: does it? while we trust it, may be its a good idea to validate --shilpa & jj
    }

    @Test
    public void shouldReturnTrueIfEitherInstanceIsaBisect() {
        String pipelineName = "pipeline";
        Integer fromCounter = 2;
        Integer toCounter = 3;

        Pipeline fromPipeline = mock(Pipeline.class);
        Pipeline toPipeline = mock(Pipeline.class);

        when(pipelineDao.findPipelineByNameAndCounter(pipelineName, fromCounter)).thenReturn(fromPipeline);
        when(pipelineDao.findPipelineByNameAndCounter(pipelineName, toCounter)).thenReturn(toPipeline);
        when(fromPipeline.isBisect()).thenReturn(true);
        when(toPipeline.isBisect()).thenReturn(false);

        boolean isBisect = service.isPipelineBisect(pipelineName, fromCounter, toCounter);

        assertTrue(isBisect);
    }

    @Test
    public void shouldReturnFalseIfBothInstancesAreNotBisect() {
        String pipelineName = "pipeline";
        Integer fromCounter = 2;
        Integer toCounter = 3;

        Pipeline fromPipeline = mock(Pipeline.class);
        Pipeline toPipeline = mock(Pipeline.class);

        when(pipelineDao.findPipelineByNameAndCounter(pipelineName, fromCounter)).thenReturn(fromPipeline);
        when(pipelineDao.findPipelineByNameAndCounter(pipelineName, toCounter)).thenReturn(toPipeline);
        when(fromPipeline.isBisect()).thenReturn(false);
        when(toPipeline.isBisect()).thenReturn(false);

        boolean isBisect = service.isPipelineBisect(pipelineName, fromCounter, toCounter);

        assertFalse(isBisect);
    }

    @Test
    public void shouldThrowExceptionIfPipelineWithFromCounterNotFound() {
        String pipelineName = "pipeline";
        Integer fromCounter = 2;
        Integer toCounter = 3;

        Pipeline toPipeline = mock(Pipeline.class);

        when(pipelineDao.findPipelineByNameAndCounter(pipelineName, fromCounter)).thenReturn(null);
        when(pipelineDao.findPipelineByNameAndCounter(pipelineName, toCounter)).thenReturn(toPipeline);

        assertThatCode(() -> service.isPipelineBisect(pipelineName, fromCounter, toCounter))
                .isInstanceOf(RecordNotFoundException.class)
                .hasMessage("Pipeline 'pipeline' with counter '2' not found!");
    }

    @Test
    public void shouldThrowExceptionIfPipelineWithToCounterNotFound() {
        String pipelineName = "pipeline";
        Integer fromCounter = 2;
        Integer toCounter = 3;

        Pipeline fromPipeline = mock(Pipeline.class);

        when(pipelineDao.findPipelineByNameAndCounter(pipelineName, toCounter)).thenReturn(null);
        when(pipelineDao.findPipelineByNameAndCounter(pipelineName, fromCounter)).thenReturn(fromPipeline);

        assertThatCode(() -> service.isPipelineBisect(pipelineName, fromCounter, toCounter))
                .isInstanceOf(RecordNotFoundException.class)
                .hasMessage("Pipeline 'pipeline' with counter '3' not found!");
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
