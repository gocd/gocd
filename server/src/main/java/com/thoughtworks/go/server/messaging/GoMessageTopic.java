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

public class GoMessageTopic<T extends GoMessage> implements GoMessageChannel<T> {
    private final MessagingService<T> messaging;
    private final String topic;
    private MessageSender sender;

    @SuppressWarnings("unchecked")
    public GoMessageTopic(MessagingService<GoMessage> messaging, String topic) {
        this.messaging = (MessagingService<T>) messaging;
        this.topic = topic;
    }

    protected MessageSender sender() {
        if (sender == null) {
            sender = messaging.createSender(topic);
        }
        return sender;
    }

    @Override
    public JMSMessageListenerAdapter<T> addListener(GoMessageListener<T> listener) {
        return messaging.addListener(topic, listener);
    }

    @Override
    public void post(T message) {
        sender().sendMessage(message);
    }
}
