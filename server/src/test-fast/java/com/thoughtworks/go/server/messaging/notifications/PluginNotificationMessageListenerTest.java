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

import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

public class PluginNotificationMessageListenerTest {
    @Test
    public void shouldNotifyPluginOnMessage() {
        NotificationExtension notificationExtension = mock(NotificationExtension.class);
        ServerHealthService serverHealthService = mock(ServerHealthService.class);
        PluginNotificationMessageListener listener = new PluginNotificationMessageListener(notificationExtension, serverHealthService);

        PluginNotificationMessage message = new PluginNotificationMessage("pid", "request-name", "data");
        when(notificationExtension.notify(message.pluginId(), message.getRequestName(), message.getData())).thenReturn(new Result());
        listener.onMessage(message);

        verify(serverHealthService).removeByScope(HealthStateScope.aboutPlugin(message.pluginId()));
        verify(notificationExtension).notify("pid", "request-name", message.getData());
    }

    @Test
    public void shouldAddErrorReturnedByPluginToHealthMessage() {
        NotificationExtension notificationExtension = mock(NotificationExtension.class);
        ServerHealthService serverHealthService = mock(ServerHealthService.class);
        PluginNotificationMessageListener listener = new PluginNotificationMessageListener(notificationExtension, serverHealthService);

        PluginNotificationMessage message = new PluginNotificationMessage("pid", "request-name", "data");
        Result result = new Result();
        result.withErrorMessages(asList(new String[]{"error message 1", "error message 2"}));
        when(notificationExtension.notify(message.pluginId(), message.getRequestName(), message.getData())).thenReturn(result);
        ArgumentCaptor<ServerHealthState> argumentCaptor = ArgumentCaptor.forClass(ServerHealthState.class);
        listener.onMessage(message);

        verify(serverHealthService).update(argumentCaptor.capture());
        ServerHealthState serverHealthState = argumentCaptor.getValue();
        assertThat(serverHealthState.isSuccess(), is(false));
        assertThat(serverHealthState.getMessage(), is("Notification update failed for plugin: pid"));
        assertThat(serverHealthState.getDescription(), is("error message 1, error message 2"));
        verify(notificationExtension).notify("pid", "request-name", message.getData());
    }

    @Test
    public void shouldHandleExceptionDuringPluginNotificationCorrectly() {
        NotificationExtension notificationExtension = mock(NotificationExtension.class);
        ServerHealthService serverHealthService = mock(ServerHealthService.class);
        PluginNotificationMessageListener listener = new PluginNotificationMessageListener(notificationExtension, serverHealthService);

        PluginNotificationMessage message = new PluginNotificationMessage("pid", "request-name", "data");
        when(notificationExtension.notify(message.pluginId(), message.getRequestName(), message.getData())).thenThrow(new RuntimeException("error!"));
        ArgumentCaptor<ServerHealthState> argumentCaptor = ArgumentCaptor.forClass(ServerHealthState.class);
        listener.onMessage(message);

        verify(serverHealthService).update(argumentCaptor.capture());
        ServerHealthState serverHealthState = argumentCaptor.getValue();
        assertThat(serverHealthState.isSuccess(), is(false));
        assertThat(serverHealthState.getMessage(), is("Notification update failed for plugin: pid"));
        assertThat(serverHealthState.getDescription(), is("error!"));
        verify(notificationExtension).notify("pid", "request-name", message.getData());
    }

}
