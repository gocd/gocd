/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.messaging.plugin;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.notificationdata.StageNotificationData;
import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.service.GoConfigService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class StageStatusPluginNotifierTest {
    @Mock
    private NotificationPluginRegistry notificationPluginRegistry;
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private PipelineSqlMapDao pipelineSqlMapDao;
    @Mock
    private PluginNotificationQueue pluginNotificationQueue;
    @Mock
    private Stage stage;

    private ArgumentCaptor<PluginNotificationMessage> captor;
    private StageStatusPluginNotifier stageStatusPluginNotifier;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        captor = ArgumentCaptor.forClass(PluginNotificationMessage.class);

        stageStatusPluginNotifier = new StageStatusPluginNotifier(notificationPluginRegistry, goConfigService, pipelineSqlMapDao, pluginNotificationQueue);
    }

    @Test
    public void shouldNotNotifyInterestedPluginsIfNoPluginIsInterested() throws Exception {
        when(notificationPluginRegistry.isAnyPluginInterestedIn(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION)).thenReturn(false);

        stageStatusPluginNotifier.stageStatusChanged(stage);

        verify(pluginNotificationQueue, never()).post(any(PluginNotificationMessage.class));
    }

    @Test
    public void shouldNotNotifyInterestedPluginsIfStageStateIsNotScheduledOrCompleted() throws Exception {
        when(notificationPluginRegistry.isAnyPluginInterestedIn(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION)).thenReturn(true);
        when(stage.isScheduled()).thenReturn(false);
        when(stage.isReRun()).thenReturn(false);
        when(stage.getState()).thenReturn(StageState.Unknown);

        stageStatusPluginNotifier.stageStatusChanged(stage);

        verify(pluginNotificationQueue, never()).post(any(PluginNotificationMessage.class));
    }

    @Test
    public void shouldNotifyInterestedPluginsIfStageStateIsScheduled() throws Exception {
        when(notificationPluginRegistry.isAnyPluginInterestedIn(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION)).thenReturn(true);
        when(stage.isScheduled()).thenReturn(true);
        when(stage.isReRun()).thenReturn(false);
        when(stage.getState()).thenReturn(StageState.Unknown);
        String pipelineName = "pipeline-name";
        when(stage.getIdentifier()).thenReturn(new StageIdentifier(pipelineName, 1, "stage", "1"));
        when(goConfigService.findGroupNameByPipeline(new CaseInsensitiveString(pipelineName))).thenReturn("group1");
        when(pipelineSqlMapDao.findBuildCauseOfPipelineByNameAndCounter(pipelineName, stage.getIdentifier().getPipelineCounter())).thenReturn(BuildCause.createManualForced());
        stageStatusPluginNotifier.stageStatusChanged(stage);

        verify(pluginNotificationQueue).post(captor.capture());
        assertThat(captor.getValue().getData() instanceof StageNotificationData, is(true));
        StageNotificationData data = (StageNotificationData) captor.getValue().getData();
        assertThat(data.getStage(), is(stage));
        assertThat(data.getBuildCause(), is(BuildCause.createManualForced()));
        assertThat(data.getPipelineGroup(), is("group1"));
    }

    @Test
    public void shouldNotifyInterestedPluginsIfStageStateIsReScheduled() throws Exception {
        when(notificationPluginRegistry.isAnyPluginInterestedIn(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION)).thenReturn(true);
        when(stage.isScheduled()).thenReturn(false);
        when(stage.isReRun()).thenReturn(true);
        when(stage.getState()).thenReturn(StageState.Unknown);
        when(stage.getIdentifier()).thenReturn(new StageIdentifier("pipeline-name", 1, "stage", "1"));

        stageStatusPluginNotifier.stageStatusChanged(stage);

        verify(pluginNotificationQueue).post(any(PluginNotificationMessage.class));
    }

    @Test
    public void shouldNotifyInterestedPluginsIfStageStateIsCompleted() throws Exception {
        when(notificationPluginRegistry.isAnyPluginInterestedIn(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION)).thenReturn(true);
        when(stage.isScheduled()).thenReturn(false);
        when(stage.isReRun()).thenReturn(false);
        when(stage.getState()).thenReturn(StageState.Passed);
        when(stage.getIdentifier()).thenReturn(new StageIdentifier("pipeline-name", 1, "stage", "1"));

        stageStatusPluginNotifier.stageStatusChanged(stage);

        verify(pluginNotificationQueue).post(any(PluginNotificationMessage.class));
    }
}
