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
import jakarta.jms.JMSException;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

public class PluginAwareMessageQueue<T extends PluginAwareMessage> extends GoMessageQueue<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginAwareMessageQueue.class.getName());

    private final List<JMSMessageListenerAdapter<T>> jmsListeners = new CopyOnWriteArrayList<>();

    public PluginAwareMessageQueue(MessagingService<GoMessage> messaging, String queueName, int numberOfListeners, ListenerFactory<T> listenerFactory) {
        super(messaging, queueName);

        IntStream.range(0, numberOfListeners).forEach(i -> jmsListeners.add(this.addListener(listenerFactory.create())));
    }

    @Override
    public void stop() {
        super.stop();
        for (JMSMessageListenerAdapter<T> jmsListener : jmsListeners) {
            try {
                jmsListener.stop();
            } catch (JMSException e) {
                LOGGER.warn("Unable to stop listener for {} {}, ERROR: {}", queueName, jmsListener.listenerThreadName(), e.getMessage(), e);
            }
        }
        jmsListeners.clear();
    }

    @TestOnly
    public int numberListeners() {
        return jmsListeners.size();
    }
}
