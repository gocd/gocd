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

package com.thoughtworks.go.server.messaging;


import com.thoughtworks.go.plugin.access.common.settings.GoPluginExtension;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.messaging.activemq.JMSMessageListenerAdapter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.jms.JMSException;
import java.util.ArrayList;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class PluginMessageQueueHandlerTest {
    private static final String PLUGIN_ID = "plugin-1";
    private static final String QUEUE_NAME_PREFIX = "queue";
    private GoPluginExtension extension;
    private PluginMessageQueueHandler<FooMessage> handler;
    private MessagingService messaging;
    private MyQueueFactory queueFactory;


    @Before
    public void setUp() throws Exception {
        extension = mock(GoPluginExtension.class);
        messaging = mock(MessagingService.class);

        queueFactory = new MyQueueFactory();
        handler = new PluginMessageQueueHandler<FooMessage>(extension, messaging, mock(PluginManager.class), queueFactory) {
        };
    }

    @Test
    public void shouldCreateListenerWhenAPluginLoadsUp() {
        String pluginId = PLUGIN_ID;
        String queueName = QUEUE_NAME_PREFIX + pluginId;
        when(extension.canHandlePlugin(pluginId)).thenReturn(true);
        when(messaging.addQueueListener(eq(queueName), any(GoMessageListener.class))).thenReturn(mock(JMSMessageListenerAdapter.class));
        handler.pluginLoaded(new GoPluginDescriptor(pluginId, null, null, null, null, false));

        assertThat(handler.queues.containsKey(pluginId), is(true));
        assertThat(handler.queues.get(pluginId).listeners.containsKey(pluginId), is(true));
        ArrayList<JMSMessageListenerAdapter> listeners = handler.queues.get(pluginId).listeners.get(pluginId);
        assertThat(listeners.size(), is(10));
        ArgumentCaptor<GoMessageListener> argumentCaptor = ArgumentCaptor.forClass(GoMessageListener.class);
        verify(messaging, times(10)).addQueueListener(eq(queueName), argumentCaptor.capture());
        assertThat(argumentCaptor.getValue() instanceof GoMessageListener, is(true));
    }

    @Test
    public void shouldRemoveListenerWhenAPluginIsUnloaded() throws JMSException {
        String pluginId = PLUGIN_ID;
        String queueName = QUEUE_NAME_PREFIX + pluginId;
        when(extension.canHandlePlugin(pluginId)).thenReturn(true);
        JMSMessageListenerAdapter listenerAdapter = mock(JMSMessageListenerAdapter.class);
        when(messaging.addQueueListener(eq(queueName), any(GoMessageListener.class))).thenReturn(listenerAdapter);
        GoPluginDescriptor pluginDescriptor = new GoPluginDescriptor(pluginId, null, null, null, null, false);

        handler.pluginLoaded(pluginDescriptor);
        handler.pluginUnLoaded(pluginDescriptor);

        assertThat(handler.queues.containsKey(pluginId), is(false));
        verify(listenerAdapter, times(10)).stop();
        verify(messaging, times(1)).removeQueue(queueName);
    }

    @Test
    public void shouldIgnoreOtherPluginTypesDuringLoadAndUnload() {
        String pluginId = PLUGIN_ID;
        String queueName = QUEUE_NAME_PREFIX + pluginId;
        when(extension.canHandlePlugin(pluginId)).thenReturn(false);
        GoPluginDescriptor pluginDescriptor = new GoPluginDescriptor(pluginId, null, null, null, null, false);

        handler.pluginLoaded(pluginDescriptor);
        handler.pluginUnLoaded(pluginDescriptor);

        assertThat(handler.queues.containsKey(pluginId), is(false));
        verify(messaging, never()).removeQueue(queueName);
        verify(messaging, never()).addQueueListener(any(String.class), any(GoMessageListener.class));
    }

    private class MyQueueFactory implements QueueFactory {
        @Override
        public PluginAwareMessageQueue create(GoPluginDescriptor pluginDescriptor) {
            return new PluginAwareMessageQueue(messaging, PLUGIN_ID, QUEUE_NAME_PREFIX + pluginDescriptor.id(), 10, new MyListenerFactory());
        }
    }

    private class MyListenerFactory implements ListenerFactory {
        @Override
        public GoMessageListener create() {
            return mock(GoMessageListener.class);
        }
    }

    private class FooMessage implements PluginAwareMessage {
        @Override
        public String pluginId() {
            return PLUGIN_ID;
        }
    }
}
