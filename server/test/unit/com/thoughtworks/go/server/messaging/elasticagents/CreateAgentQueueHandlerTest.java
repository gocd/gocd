/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.messaging.elasticagents;


import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginRegistry;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.messaging.GoMessageListener;
import com.thoughtworks.go.server.messaging.MessagingService;
import com.thoughtworks.go.server.messaging.activemq.JMSMessageListenerAdapter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.jms.JMSException;
import java.util.ArrayList;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class CreateAgentQueueHandlerTest {
    private ElasticAgentExtension elasticAgentExtension;
    private CreateAgentQueueHandler handler;
    private MessagingService messaging;

    @Before
    public void setUp() throws Exception {
        elasticAgentExtension = mock(ElasticAgentExtension.class);
        messaging = mock(MessagingService.class);
        handler = new CreateAgentQueueHandler(messaging, mock(ElasticAgentPluginRegistry.class), elasticAgentExtension, mock(PluginManager.class));
    }

    @Test
    public void shouldCreateListenerWhenAPluginLoadsUp() {
        String pluginId = "plugin-1";
        String queueName = CreateAgentQueueHandler.QUEUE_NAME_PREFIX + pluginId;
        when(elasticAgentExtension.canHandlePlugin(pluginId)).thenReturn(true);
        when(messaging.addQueueListener(eq(queueName), any(GoMessageListener.class))).thenReturn(mock(JMSMessageListenerAdapter.class));
        handler.pluginLoaded(new GoPluginDescriptor(pluginId, null, null, null, null, false));

        assertThat(handler.queues.containsKey(pluginId), is(true));
        assertThat(handler.listeners.containsKey(pluginId), is(true));
        ArrayList<JMSMessageListenerAdapter> listeners = handler.listeners.get(pluginId);
        assertThat(listeners.size(), is(1));
        ArgumentCaptor<GoMessageListener> argumentCaptor = ArgumentCaptor.forClass(GoMessageListener.class);
        verify(messaging, times(1)).addQueueListener(eq(queueName), argumentCaptor.capture());
        GoMessageListener listener = argumentCaptor.getValue();
        assertThat(listener instanceof CreateAgentListener, is(true));
    }

    @Test
    public void shouldRemoveListenerWhenAPluginIsUnloaded() throws JMSException {
        String pluginId = "plugin-1";
        String queueName = CreateAgentQueueHandler.QUEUE_NAME_PREFIX + pluginId;
        when(elasticAgentExtension.canHandlePlugin(pluginId)).thenReturn(true);
        JMSMessageListenerAdapter listenerAdapter = mock(JMSMessageListenerAdapter.class);
        when(messaging.addQueueListener(eq(queueName), any(GoMessageListener.class))).thenReturn(listenerAdapter);
        GoPluginDescriptor pluginDescriptor = new GoPluginDescriptor(pluginId, null, null, null, null, false);
        handler.pluginLoaded(pluginDescriptor);

        handler.pluginUnLoaded(pluginDescriptor);

        assertThat(handler.queues.containsKey(pluginId), is(false));
        assertThat(handler.listeners.containsKey(pluginId), is(false));
        ArrayList<JMSMessageListenerAdapter> listeners = handler.listeners.get(pluginId);
        assertThat(listeners, is(nullValue()));
        verify(listenerAdapter).stop();
        verify(messaging, times(1)).removeQueue(queueName);
    }

    @Test
    public void shouldIgnoreOtherPluginTypesDuringLoadAndUnload() {
        String pluginId = "plugin-1";
        String queueName = CreateAgentQueueHandler.QUEUE_NAME_PREFIX + pluginId;
        when(elasticAgentExtension.canHandlePlugin(pluginId)).thenReturn(false);
        GoPluginDescriptor pluginDescriptor = new GoPluginDescriptor(pluginId, null, null, null, null, false);
        handler.pluginLoaded(pluginDescriptor);

        handler.pluginUnLoaded(pluginDescriptor);

        assertThat(handler.queues.containsKey(pluginId), is(false));
        assertThat(handler.listeners.containsKey(pluginId), is(false));
        assertThat(handler.listeners.get(pluginId), is(nullValue()));
        verify(messaging, never()).removeQueue(queueName);
        verify(messaging, never()).addQueueListener(any(String.class), any(GoMessageListener.class));
    }
}