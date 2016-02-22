/*
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
 */

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class PipelineLockServiceTest {
    private PipelineLockService pipelineLockService;
    private PipelineSqlMapDao pipelineDao;
    private GoConfigService goConfigService;

    @Before
    public void setup() throws Exception {
        pipelineDao = mock(PipelineSqlMapDao.class);
        goConfigService = mock(GoConfigService.class);
        pipelineLockService = new PipelineLockService(goConfigService, pipelineDao);
        pipelineLockService.initialize();
    }

    @Test
    public void shouldLockPipeline() throws Exception {
        when(goConfigService.isLockable("mingle")).thenReturn(true);

        Pipeline pipeline = PipelineMother.firstStageBuildingAndSecondStageScheduled("mingle", asList("dev", "ft"), asList("test"));
        pipelineLockService.lockIfNeeded(pipeline);
        verify(pipelineDao).lockPipeline(pipeline);
    }

    @Test
    public void shouldNotLockPipelineWhenNotLockable() throws Exception {
        when(goConfigService.isLockable("mingle")).thenReturn(false);

        Pipeline pipeline = PipelineMother.firstStageBuildingAndSecondStageScheduled("mingle", asList("dev", "ft"), asList("test"));
        pipelineLockService.lockIfNeeded(pipeline);
        verify(pipelineDao, never()).lockPipeline(pipeline);
    }

    @Test
    public void shouldKnowIfPipelineIsLocked() throws Exception {
        when(pipelineDao.lockedPipeline("mingle")).thenReturn(new StageIdentifier("mingle", 1, "1", "stage", "1"));

        assertThat(pipelineLockService.isLocked("mingle"), is(true));
        assertThat(pipelineLockService.isLocked("twist"), is(false));
    }

    @Test
    public void shouldUnlockPipelineIrrespectiveOfItBeingLockable() throws Exception {
        pipelineLockService.unlock("mingle");
        verify(pipelineDao).unlockPipeline("mingle");
    }

    @Test
    public void shouldAllowStageFromCurrentPipelineToBeScheduled() throws Exception {
        Pipeline pipeline = PipelineMother.firstStageBuildingAndSecondStageScheduled("mingle", asList("dev", "ft"), asList("test"));

        when(pipelineDao.lockedPipeline("mingle")).thenReturn(pipeline.getStages().get(0).getIdentifier());
        when(goConfigService.isLockable(pipeline.getName())).thenReturn(true);

        pipelineLockService.lockIfNeeded(pipeline);
        assertThat(pipelineLockService.canScheduleStageInPipeline(pipeline.getIdentifier()), is(true));
    }

    @Test
    public void shouldNotAllowStageFromLockedPipelineToBeScheduled() throws Exception {
        Pipeline pipeline = PipelineMother.firstStageBuildingAndSecondStageScheduled("mingle", asList("dev", "ft"), asList("test"));

        when(pipelineDao.lockedPipeline("mingle")).thenReturn(new StageIdentifier(pipeline.getName(), 9999, "1.2.9999", "stage", "1"));
        when(goConfigService.isLockable(pipeline.getName())).thenReturn(true);

        pipelineLockService.lockIfNeeded(pipeline);
        assertThat(pipelineLockService.canScheduleStageInPipeline(pipeline.getIdentifier()), is(false));
    }

    @Test
    public void shouldAllowStageFromAnotherPipelineIfThePipelineIsNotLockabler() throws Exception {
        Pipeline pipeline = PipelineMother.firstStageBuildingAndSecondStageScheduled("mingle", asList("dev", "ft"), asList("test"));


        when(pipelineDao.lockedPipeline("mingle")).thenReturn(new StageIdentifier(pipeline.getName(), 9999, "1.2.9999", "stage", "1"));
        when(goConfigService.isLockable(pipeline.getName())).thenReturn(false);

        pipelineLockService.lockIfNeeded(pipeline);
        assertThat(pipelineLockService.canScheduleStageInPipeline(pipeline.getIdentifier()), is(true));
    }

    @Test
    public void shouldAllowStageFromAnotherPipelineIfThePipelineIsLockable() throws Exception {
        Pipeline pipeline = PipelineMother.firstStageBuildingAndSecondStageScheduled("mingle", asList("dev", "ft"), asList("test"));

        when(pipelineDao.lockedPipeline("another-pipeline")).thenReturn(null);
        when(goConfigService.isLockable(pipeline.getName())).thenReturn(true);

        pipelineLockService.lockIfNeeded(pipeline);
        assertThat(pipelineLockService.canScheduleStageInPipeline(pipeline.getIdentifier()), is(true));
    }

    @Test
    public void shouldUnlockAnyCurrentlyLockedPipelinesThatAreNoLongerLockable() throws Exception {
        CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);

        when(pipelineDao.lockedPipelines()).thenReturn(asList("mingle", "twist"));
        when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("mingle"))).thenReturn(true);
        when(cruiseConfig.isPipelineLocked("mingle")).thenReturn(true);
        when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("twist"))).thenReturn(true);
        when(cruiseConfig.isPipelineLocked("twist")).thenReturn(false);

        pipelineLockService.onConfigChange(cruiseConfig);

        verify(pipelineDao, never()).unlockPipeline("mingle");
        verify(pipelineDao).unlockPipeline("twist");
    }

    @Test
    public void shouldUnlockAnyCurrentlyLockedPipelinesThatNoLongerExist() throws Exception {
        CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);

        when(pipelineDao.lockedPipelines()).thenReturn(asList("mingle", "twist"));
        when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("mingle"))).thenReturn(false);
        when(cruiseConfig.isPipelineLocked("mingle")).thenThrow(new PipelineNotFoundException("mingle not there"));
        when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("twist"))).thenReturn(true);
        when(cruiseConfig.isPipelineLocked("twist")).thenReturn(false);

        pipelineLockService.onConfigChange(cruiseConfig);

        verify(pipelineDao).unlockPipeline("mingle");
        verify(pipelineDao).unlockPipeline("twist");
    }

    private EntityConfigChangedListener<PipelineConfig> getPipelineConfigEntityConfigChangedListener() {
        ArgumentCaptor<ConfigChangedListener> captor = ArgumentCaptor.forClass(ConfigChangedListener.class);
        doNothing().when(goConfigService).register(captor.capture());
        pipelineLockService.initialize();
        List<ConfigChangedListener> listeners = captor.getAllValues();
        assertThat(listeners.get(1) instanceof EntityConfigChangedListener, is(true));
        EntityConfigChangedListener<PipelineConfig> pipelineConfigChangeListener = (EntityConfigChangedListener<PipelineConfig>) listeners.get(1);
        return pipelineConfigChangeListener;
    }

    @Test
    public void shouldUnlockCurrentlyLockedPipelineThatIsNoLongerLockableWhenPipelineConfigChanges() throws Exception {
        EntityConfigChangedListener<PipelineConfig> changedListener = getPipelineConfigEntityConfigChangedListener();
        PipelineConfig pipelineConfig = mock(PipelineConfig.class);

        when(pipelineDao.lockedPipelines()).thenReturn(asList("locked_pipeline", "other_pipeline"));
        when(pipelineConfig.isLock()).thenReturn(false);
        when(pipelineConfig.name()).thenReturn(new CaseInsensitiveString("locked_pipeline"));

        changedListener.onEntityConfigChange(pipelineConfig);

        verify(pipelineDao, never()).unlockPipeline("other_pipeline");
        verify(pipelineDao).unlockPipeline("locked_pipeline");
    }

    @Test
    public void shouldNotUnlockCurrentlyLockedPipelineThatContinuesToBeLockableWhenPipelineConfigChanges() throws Exception {
        EntityConfigChangedListener<PipelineConfig> changedListener = getPipelineConfigEntityConfigChangedListener();
        PipelineConfig pipelineConfig = mock(PipelineConfig.class);

        when(pipelineDao.lockedPipelines()).thenReturn(asList("locked_pipeline"));
        when(pipelineConfig.isLock()).thenReturn(true);
        when(pipelineConfig.name()).thenReturn(new CaseInsensitiveString("locked_pipeline"));

        changedListener.onEntityConfigChange(pipelineConfig);

        verify(pipelineDao, never()).unlockPipeline("locked_pipeline");
    }

    @Test
    public void shouldRegisterItselfAsAConfigChangeListener() throws Exception {
        GoConfigService mockGoConfigService = mock(GoConfigService.class);
        PipelineLockService service = new PipelineLockService(mockGoConfigService, pipelineDao);
        service.initialize();
        verify(mockGoConfigService).register(service);
    }
}
