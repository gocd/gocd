/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.server.messaging.activemq.JMSMessageListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class PluginAwareMessageQueue<T extends PluginAwareMessage> extends GoMessageQueue<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginAwareMessageQueue.class.getName());

    protected final Map<String, List<JMSMessageListenerAdapter<T>>> listeners = new HashMap<>();
    private final String pluginId;


    public PluginAwareMessageQueue(MessagingService<GoMessage> messaging, String pluginId, String queueName, Integer numberOfListeners, ListenerFactory<T> listenerFactory) {
        super(messaging, queueName);
        this.pluginId = pluginId;
        for (int i = 0; i < numberOfListeners; i++) {
            JMSMessageListenerAdapter<T> listenerAdapter = this.addListener(listenerFactory.create());
            if (!listeners.containsKey(pluginId)) {
                this.listeners.put(pluginId, new ArrayList<>());
            }
            this.listeners.get(pluginId).add(listenerAdapter);
        }
    }

    @Override
    public void stop() {
        super.stop();
        List<JMSMessageListenerAdapter<T>> listenerAdapters = listeners.get(pluginId);
        for (JMSMessageListenerAdapter<T> listenerAdapter : listenerAdapters) {
            try {
                listenerAdapter.stop();
            } catch (JMSException e) {
                LOGGER.error("Unable to stop listener for {} {}, ERROR: {}", queueName, listenerAdapter.thread.getName(), e.getMessage(), e);
                bomb(e);
            } finally {
                this.listeners.remove(pluginId);
            }
        }
    }
}
