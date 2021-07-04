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
package com.thoughtworks.go.server.messaging.notifications;

import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageState;
import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.service.GoConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StageStatusPluginNotifierTest {
    @Mock
    private NotificationPluginRegistry notificationPluginRegistry;
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private PipelineSqlMapDao pipelineSqlMapDao;
    @Mock
    private Stage stage;
    @Mock
    private PluginNotificationService pluginNotificationService;

    private StageStatusPluginNotifier stageStatusPluginNotifier;

    @BeforeEach
    public void setUp() throws Exception {
        stageStatusPluginNotifier = new StageStatusPluginNotifier(notificationPluginRegistry, pluginNotificationService);
    }

    @Test
    public void shouldNotNotifyInterestedPluginsIfNoPluginIsInterested() throws Exception {
        when(notificationPluginRegistry.isAnyPluginInterestedIn(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION)).thenReturn(false);

        stageStatusPluginNotifier.stageStatusChanged(stage);

        verify(pluginNotificationService, never()).notifyStageStatus(stage);
    }

    @Test
    public void shouldNotNotifyInterestedPluginsIfStageStateIsNotScheduledOrCompleted() throws Exception {
        when(notificationPluginRegistry.isAnyPluginInterestedIn(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION)).thenReturn(true);
        when(stage.isScheduled()).thenReturn(false);
        when(stage.isReRun()).thenReturn(false);
        when(stage.getState()).thenReturn(StageState.Unknown);

        stageStatusPluginNotifier.stageStatusChanged(stage);

        verify(pluginNotificationService, never()).notifyStageStatus(stage);
    }

    @Test
    public void shouldNotifyInterestedPluginsIfStageStateIsScheduled() throws Exception {
        when(notificationPluginRegistry.isAnyPluginInterestedIn(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION)).thenReturn(true);
        when(stage.isScheduled()).thenReturn(true);
        stageStatusPluginNotifier.stageStatusChanged(stage);

        verify(pluginNotificationService).notifyStageStatus(stage);
    }

    @Test
    public void shouldNotifyInterestedPluginsIfStageStateIsReScheduled() throws Exception {
        when(notificationPluginRegistry.isAnyPluginInterestedIn(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION)).thenReturn(true);
        when(stage.isScheduled()).thenReturn(false);
        when(stage.isReRun()).thenReturn(true);

        stageStatusPluginNotifier.stageStatusChanged(stage);

        verify(pluginNotificationService).notifyStageStatus(stage);
    }

    @Test
    public void shouldNotifyInterestedPluginsIfStageStateIsCompleted() throws Exception {
        when(notificationPluginRegistry.isAnyPluginInterestedIn(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION)).thenReturn(true);
        when(stage.isScheduled()).thenReturn(false);
        when(stage.isReRun()).thenReturn(false);
        when(stage.getState()).thenReturn(StageState.Passed);

        stageStatusPluginNotifier.stageStatusChanged(stage);

        verify(pluginNotificationService).notifyStageStatus(stage);
    }
}
