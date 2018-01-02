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

import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluginNotificationServiceTest {
    public static final String PLUGIN_ID_1 = "plugin-id-1";
    public static final String PLUGIN_ID_2 = "plugin-id-2";
    public static final String PLUGIN_ID_3 = "plugin-id-3";

    public static final String PIPELINE_STATUS = "pipeline-status";
    public static final String STAGE_STATUS = "stage-status";
    public static final Map<String,String> REQUEST_BODY = new HashMap();

    @Mock
    private NotificationExtension notificationExtension;
    @Mock
    private NotificationPluginRegistry notificationPluginRegistry;
    @Mock
    private ServerHealthService serverHealthService;

    private ArgumentCaptor<ServerHealthState> serverHealthState;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        REQUEST_BODY.put("key", "value");
        serverHealthState = ArgumentCaptor.forClass(ServerHealthState.class);
    }

    @Test
    public void shouldNotifyInterestedPluginsCorrectly() throws Exception {
        Result result = new Result();
        result.withSuccessMessages("success message");
        when(notificationPluginRegistry.getPluginsInterestedIn(PIPELINE_STATUS)).thenReturn(new LinkedHashSet<>(asList(PLUGIN_ID_1, PLUGIN_ID_2)));
        when(notificationPluginRegistry.getPluginsInterestedIn(STAGE_STATUS)).thenReturn(new LinkedHashSet<>(asList(PLUGIN_ID_3)));

        when(notificationExtension.notify(PLUGIN_ID_1, PIPELINE_STATUS, REQUEST_BODY)).thenReturn(result);
        when(notificationExtension.notify(PLUGIN_ID_2, PIPELINE_STATUS, REQUEST_BODY)).thenReturn(result);

        PluginNotificationService pluginNotificationService = new PluginNotificationService(notificationExtension, notificationPluginRegistry, serverHealthService);
        pluginNotificationService.notifyPlugins(new PluginNotificationMessage(PIPELINE_STATUS, REQUEST_BODY));

        verify(notificationExtension).notify(PLUGIN_ID_1, PIPELINE_STATUS, REQUEST_BODY);
        verify(notificationExtension).notify(PLUGIN_ID_2, PIPELINE_STATUS, REQUEST_BODY);
        verify(notificationExtension, never()).notify(PLUGIN_ID_3, PIPELINE_STATUS, REQUEST_BODY);
        verify(serverHealthService, times(2)).removeByScope(any(HealthStateScope.class));
    }

    @Test
    public void shouldHandleErrorDuringPluginNotificationCorrectly() throws Exception {
        Result result = new Result();
        result.withErrorMessages(asList(new String[]{"message 1", "message 2"}));

        when(notificationPluginRegistry.getPluginsInterestedIn(PIPELINE_STATUS)).thenReturn(new LinkedHashSet<>(asList(PLUGIN_ID_1)));

        when(notificationExtension.notify(PLUGIN_ID_1, PIPELINE_STATUS, REQUEST_BODY)).thenReturn(result);
        when(serverHealthService.update(serverHealthState.capture())).thenReturn(null);

        PluginNotificationService pluginNotificationService = new PluginNotificationService(notificationExtension, notificationPluginRegistry, serverHealthService);
        pluginNotificationService.notifyPlugins(new PluginNotificationMessage(PIPELINE_STATUS, REQUEST_BODY));

        verify(notificationExtension).notify(PLUGIN_ID_1, PIPELINE_STATUS, REQUEST_BODY);
        assertThat(serverHealthState.getValue().getMessage(), is("Notification update failed for plugin: plugin-id-1"));
        assertThat(serverHealthState.getValue().getDescription(), is("message 1, message 2"));
        verify(serverHealthService, never()).removeByScope(any(HealthStateScope.class));
    }

    @Test
    public void shouldHandleExceptionDuringPluginNotificationCorrectly() throws Exception {
        when(notificationPluginRegistry.getPluginsInterestedIn(PIPELINE_STATUS)).thenReturn(new LinkedHashSet<>(asList(PLUGIN_ID_1)));
        when(notificationExtension.notify(PLUGIN_ID_1, PIPELINE_STATUS, REQUEST_BODY)).thenThrow(new RuntimeException("error!"));
        when(serverHealthService.update(serverHealthState.capture())).thenReturn(null);

        PluginNotificationService pluginNotificationService = new PluginNotificationService(notificationExtension, notificationPluginRegistry, serverHealthService);
        pluginNotificationService.notifyPlugins(new PluginNotificationMessage(PIPELINE_STATUS, REQUEST_BODY));

        verify(notificationExtension).notify(PLUGIN_ID_1, PIPELINE_STATUS, REQUEST_BODY);
        assertThat(serverHealthState.getValue().getMessage(), is("Notification update failed for plugin: plugin-id-1"));
        assertThat(serverHealthState.getValue().getDescription(), is("error!"));
        verify(serverHealthService, never()).removeByScope(any(HealthStateScope.class));
    }
}
