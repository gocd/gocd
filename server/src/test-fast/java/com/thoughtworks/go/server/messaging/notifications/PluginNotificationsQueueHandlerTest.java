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
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.messaging.MessagingService;
import com.thoughtworks.go.server.messaging.PluginAwareMessageQueue;
import com.thoughtworks.go.server.messaging.activemq.JMSMessageListenerAdapter;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.HashMap;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluginNotificationsQueueHandlerTest {
    @Mock
    private MessagingService messagingService;
    @Mock
    private NotificationExtension notificationExtension;
    @Mock
    private PluginManager pluginManager;
    @Mock
    private SystemEnvironment systemEnvironment;
    @Mock
    private ServerHealthService serverHealthService;
    private PluginNotificationsQueueHandler handler;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        handler = new PluginNotificationsQueueHandler(messagingService, notificationExtension, pluginManager, systemEnvironment, serverHealthService);
    }

    @Test
    public void shouldCreateQueuesAndListenersForEachPlugins() {
        String pluginId1 = "plugin-1";
        String pluginId2 = "plugin-2";
        String pluginId3 = "plugin-3";
        when(notificationExtension.canHandlePlugin(pluginId1)).thenReturn(true);
        when(notificationExtension.canHandlePlugin(pluginId2)).thenReturn(false);
        when(notificationExtension.canHandlePlugin(pluginId3)).thenReturn(true);
        when(systemEnvironment.getNotificationListenerCountForPlugin(pluginId1)).thenReturn(10);
        when(systemEnvironment.getNotificationListenerCountForPlugin(pluginId3)).thenReturn(2);
        handler.pluginLoaded(getPluginDescriptor(pluginId1));
        handler.pluginLoaded(getPluginDescriptor(pluginId2));
        handler.pluginLoaded(getPluginDescriptor(pluginId3));
        assertThat(handler.getQueues().size(), is(2));
        PluginAwareMessageQueue queueForPlugin1 = handler.getQueues().get(pluginId1);
        HashMap<String, ArrayList<JMSMessageListenerAdapter>> listenersForPlugin1 = (HashMap<String, ArrayList<JMSMessageListenerAdapter>>) ReflectionUtil.getField(queueForPlugin1, "listeners");
        assertThat(listenersForPlugin1.get(pluginId1).size(), is(10));
        assertFalse(handler.getQueues().containsKey(pluginId2));
        PluginAwareMessageQueue queueForPlugin3 = handler.getQueues().get(pluginId3);
        HashMap<String, ArrayList<JMSMessageListenerAdapter>> listenersForPlugin3 = (HashMap<String, ArrayList<JMSMessageListenerAdapter>>) ReflectionUtil.getField(queueForPlugin3, "listeners");
        assertThat(listenersForPlugin3.get(pluginId3).size(), is(2));
    }

    private GoPluginDescriptor getPluginDescriptor(String pluginId) {
        return GoPluginDescriptor.builder().id(pluginId).build();
    }
}
