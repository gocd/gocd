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

package com.thoughtworks.go.server.messaging.plugin;

import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.domain.StageState;
import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class StageStatusPluginNotifierTest {
    @Mock
    private NotificationPluginRegistry notificationPluginRegistry;
    @Mock
    private PluginNotificationQueue pluginNotificationQueue;

    private ArgumentCaptor<PluginNotificationMessage> pluginNotificationMessage;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        pluginNotificationMessage = new ArgumentCaptor<PluginNotificationMessage>();
    }

    @Test
    public void shouldNotifyInterestedPluginsCorrectly() throws Exception {
        when(notificationPluginRegistry.isAnyPluginInterestedIn(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION)).thenReturn(true);
        doNothing().when(pluginNotificationQueue).post(pluginNotificationMessage.capture());

        StageStatusPluginNotifier stageStatusPluginNotifier = new StageStatusPluginNotifier(notificationPluginRegistry, pluginNotificationQueue);
        Stage stage = new Stage();
        stage.setIdentifier(new StageIdentifier("pipeline-name", 1, "stage-name", "1"));
        ReflectionUtil.setField(stage, "state", StageState.Failed);
        ReflectionUtil.setField(stage, "result", StageResult.Failed);
        Date createDate = new SimpleDateFormat(StageStatusPluginNotifier.DATE_PATTERN).parse("2011-07-13T19:43:37.100Z");
        stage.setCreatedTime(new Timestamp(createDate.getTime()));
        Date lastTransitionedDate = new SimpleDateFormat(StageStatusPluginNotifier.DATE_PATTERN).parse("2011-07-13T19:43:38.100Z");
        stage.setLastTransitionedTime(new Timestamp(lastTransitionedDate.getTime()));
        stageStatusPluginNotifier.stageStatusChanged(stage);

        PluginNotificationMessage message = pluginNotificationMessage.getValue();
        assertThat(message.getRequestName(), is(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION));
        assertThat(message.getRequestData(), is(stageStatusPluginNotifier.createRequestDataMap(stage)));
    }
}