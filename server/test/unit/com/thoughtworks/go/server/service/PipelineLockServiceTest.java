/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.PipelineState;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.dao.PipelineStateDao;
import com.thoughtworks.go.server.domain.PipelineLockStatusChangeListener;
import com.thoughtworks.go.server.domain.PipelineLockStatusChangeListener.Event;
import com.thoughtworks.go.util.LogFixture;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static com.thoughtworks.go.util.LogFixture.logFixtureFor;
import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class PipelineLockServiceTest {
    private PipelineLockService pipelineLockService;
    private PipelineStateDao pipelineStateDao;
    private GoConfigService goConfigService;

    @Before
    public void setup() throws Exception {
        pipelineStateDao = mock(PipelineStateDao.class);
        goConfigService = mock(GoConfigService.class);
        pipelineLockService = new PipelineLockService(goConfigService, pipelineStateDao);
        pipelineLockService.initialize();
    }

    @Test
    public void shouldLockPipeline() throws Exception {
        when(goConfigService.isLockable("mingle")).thenReturn(true);

        Pipeline pipeline = PipelineMother.firstStageBuildingAndSecondStageScheduled("mingle", asList("dev", "ft"), asList("test"));
        pipelineLockService.lockIfNeeded(pipeline);
        verify(pipelineStateDao).lockPipeline(pipeline);
    }

    @Test
    public void shouldNotLockPipelineWhenNotLockable() throws Exception {
        when(goConfigService.isLockable("mingle")).thenReturn(false);

        Pipeline pipeline = PipelineMother.firstStageBuildingAndSecondStageScheduled("mingle", asList("dev", "ft"), asList("test"));
        pipelineLockService.lockIfNeeded(pipeline);
        verify(pipelineStateDao, never()).lockPipeline(pipeline);
    }

    @Test
    public void shouldKnowIfPipelineIsLocked() throws Exception {
        String pipelineName = "mingle";
        PipelineState pipelineState = new PipelineState(pipelineName, new StageIdentifier(pipelineName, 1, "1", "stage", "1"));
        pipelineState.lock(1);
        when(pipelineStateDao.pipelineStateFor(pipelineName)).thenReturn(pipelineState);

        assertThat(pipelineLockService.isLocked(pipelineName), is(true));
        assertThat(pipelineLockService.isLocked("twist"), is(false));
    }

    @Test
    public void shouldUnlockPipelineIrrespectiveOfItBeingLockable() throws Exception {
        pipelineLockService.unlock("mingle");
        verify(pipelineStateDao).unlockPipeline("mingle");
    }

    @Test
    public void shouldAllowStageFromCurrentPipelineToBeScheduled() throws Exception {
        Pipeline pipeline = PipelineMother.firstStageBuildingAndSecondStageScheduled("mingle", asList("dev", "ft"), asList("test"));

        when(pipelineStateDao.pipelineStateFor("mingle")).thenReturn(new PipelineState(pipeline.getName(), pipeline.getStages().get(0).getIdentifier()));
        when(goConfigService.isLockable(pipeline.getName())).thenReturn(true);

        pipelineLockService.lockIfNeeded(pipeline);
        assertThat(pipelineLockService.canScheduleStageInPipeline(pipeline.getIdentifier()), is(true));
    }

    @Test
    public void shouldNotAllowStageFromLockedPipelineToBeScheduled() throws Exception {
        Pipeline pipeline = PipelineMother.firstStageBuildingAndSecondStageScheduled("mingle", asList("dev", "ft"), asList("test"));

        PipelineState pipelineState = new PipelineState(pipeline.getName(), new StageIdentifier(pipeline.getName(), 9999, "1.2.9999", "stage", "1"));
        pipelineState.lock(1);
        when(pipelineStateDao.pipelineStateFor("mingle")).thenReturn(pipelineState);
        when(goConfigService.isLockable(pipeline.getName())).thenReturn(true);

        pipelineLockService.lockIfNeeded(pipeline);
        assertThat(pipelineLockService.canScheduleStageInPipeline(pipeline.getIdentifier()), is(false));
    }

    @Test
    public void shouldAllowStageFromAnotherPipelineIfThePipelineIsNotLockabler() throws Exception {
        Pipeline pipeline = PipelineMother.firstStageBuildingAndSecondStageScheduled("mingle", asList("dev", "ft"), asList("test"));


        when(pipelineStateDao.pipelineStateFor("mingle")).thenReturn(new PipelineState(pipeline.getName(), new StageIdentifier(pipeline.getName(), 9999, "1.2.9999", "stage", "1")));
        when(goConfigService.isLockable(pipeline.getName())).thenReturn(false);

        pipelineLockService.lockIfNeeded(pipeline);
        assertThat(pipelineLockService.canScheduleStageInPipeline(pipeline.getIdentifier()), is(true));
    }

    @Test
    public void shouldAllowStageFromAnotherPipelineIfThePipelineIsLockable() throws Exception {
        Pipeline pipeline = PipelineMother.firstStageBuildingAndSecondStageScheduled("mingle", asList("dev", "ft"), asList("test"));

        when(pipelineStateDao.pipelineStateFor(pipeline.getName())).thenReturn(null);
        when(goConfigService.isLockable(pipeline.getName())).thenReturn(true);

        pipelineLockService.lockIfNeeded(pipeline);
        assertThat(pipelineLockService.canScheduleStageInPipeline(pipeline.getIdentifier()), is(true));
    }

    @Test
    public void shouldUnlockAnyCurrentlyLockedPipelinesThatAreNoLongerLockable() throws Exception {
        CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);

        when(pipelineStateDao.lockedPipelines()).thenReturn(asList("mingle", "twist"));
        when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("mingle"))).thenReturn(true);
        when(cruiseConfig.isPipelineLockable("mingle")).thenReturn(true);
        when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("twist"))).thenReturn(true);
        when(cruiseConfig.isPipelineLockable("twist")).thenReturn(false);

        pipelineLockService.onConfigChange(cruiseConfig);

        verify(pipelineStateDao, never()).unlockPipeline("mingle");
        verify(pipelineStateDao).unlockPipeline("twist");
    }

    @Test
    public void shouldUnlockAnyCurrentlyLockedPipelinesThatNoLongerExist() throws Exception {
        CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);

        when(pipelineStateDao.lockedPipelines()).thenReturn(asList("mingle", "twist"));
        when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("mingle"))).thenReturn(false);
        when(cruiseConfig.isPipelineLockable("mingle")).thenThrow(new PipelineNotFoundException("mingle not there"));
        when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("twist"))).thenReturn(true);
        when(cruiseConfig.isPipelineLockable("twist")).thenReturn(false);

        pipelineLockService.onConfigChange(cruiseConfig);

        verify(pipelineStateDao).unlockPipeline("mingle");
        verify(pipelineStateDao).unlockPipeline("twist");
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

        when(pipelineStateDao.lockedPipelines()).thenReturn(asList("locked_pipeline", "other_pipeline"));
        when(pipelineConfig.isLockable()).thenReturn(false);
        when(pipelineConfig.name()).thenReturn(new CaseInsensitiveString("locked_pipeline"));

        changedListener.onEntityConfigChange(pipelineConfig);

        verify(pipelineStateDao, never()).unlockPipeline("other_pipeline");
        verify(pipelineStateDao).unlockPipeline("locked_pipeline");
    }

    @Test
    public void shouldNotUnlockCurrentlyLockedPipelineThatContinuesToBeLockableWhenPipelineConfigChanges() throws Exception {
        EntityConfigChangedListener<PipelineConfig> changedListener = getPipelineConfigEntityConfigChangedListener();
        PipelineConfig pipelineConfig = mock(PipelineConfig.class);

        when(pipelineStateDao.lockedPipelines()).thenReturn(asList("locked_pipeline"));
        when(pipelineConfig.isLockable()).thenReturn(true);
        when(pipelineConfig.name()).thenReturn(new CaseInsensitiveString("locked_pipeline"));

        changedListener.onEntityConfigChange(pipelineConfig);

        verify(pipelineStateDao, never()).unlockPipeline("locked_pipeline");
    }

    @Test
    public void shouldRegisterItselfAsAConfigChangeListener() throws Exception {
        verify(goConfigService).register(pipelineLockService);
    }

    @Test
    public void shouldNotifyListenersAfterPipelineIsLocked() throws Exception {
        when(goConfigService.isLockable("pipeline1")).thenReturn(true);

        PipelineLockStatusChangeListener lockStatusChangeListener = mock(PipelineLockStatusChangeListener.class);

        Pipeline pipeline = PipelineMother.firstStageBuildingAndSecondStageScheduled("pipeline1", asList("stage1", "stage2"), asList("job1"));
        pipelineLockService.registerListener(lockStatusChangeListener);
        pipelineLockService.lockIfNeeded(pipeline);

        verify(lockStatusChangeListener).lockStatusChanged(Event.lock("pipeline1"));
    }

    @Test
    public void shouldNotifyListenersAfterPipelineIsUnlocked() throws Exception {
        PipelineLockStatusChangeListener lockStatusChangeListener = mock(PipelineLockStatusChangeListener.class);

        pipelineLockService.registerListener(lockStatusChangeListener);
        pipelineLockService.unlock("pipeline1");

        verify(lockStatusChangeListener).lockStatusChanged(Event.unLock("pipeline1"));
    }

    @Test
    public void shouldNotifyListenersAfterPipelineIsUnlockedUponConfigChange() throws Exception {
        PipelineLockStatusChangeListener lockStatusChangeListener = mock(PipelineLockStatusChangeListener.class);
        CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);

        when(pipelineStateDao.lockedPipelines()).thenReturn(asList("pipeline1"));
        when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString("pipeline1"))).thenReturn(false);
        when(cruiseConfig.isPipelineLocked("pipeline1")).thenThrow(new PipelineNotFoundException("pipeline1 not found"));

        pipelineLockService.registerListener(lockStatusChangeListener);
        pipelineLockService.onConfigChange(cruiseConfig);

        verify(lockStatusChangeListener).lockStatusChanged(Event.unLock("pipeline1"));
    }

    @Test
    public void shouldLogAndIgnoreAnyExceptionsWhileNotifyingListeners() throws Exception {
        PipelineLockStatusChangeListener listener1 = mock(PipelineLockStatusChangeListener.class);
        PipelineLockStatusChangeListener listener2 = mock(PipelineLockStatusChangeListener.class, "ListenerWhichFails");
        doThrow(new RuntimeException("Ouch.")).when(listener2).lockStatusChanged(org.mockito.Matchers.<Event>anyObject());
        PipelineLockStatusChangeListener listener3 = mock(PipelineLockStatusChangeListener.class);

        try (LogFixture logFixture = logFixtureFor(PipelineLockService.class, Level.WARN)) {

            pipelineLockService.registerListener(listener1);
            pipelineLockService.registerListener(listener2);
            pipelineLockService.registerListener(listener3);
            pipelineLockService.unlock("pipeline1");

            assertTrue(logFixture.allLogs(), logFixture.contains(Level.WARN, "Failed to notify listener (ListenerWhichFails)"));
        }

        verify(listener1).lockStatusChanged(Event.unLock("pipeline1"));
        verify(listener2).lockStatusChanged(Event.unLock("pipeline1"));
        verify(listener3).lockStatusChanged(Event.unLock("pipeline1"));
    }
}
