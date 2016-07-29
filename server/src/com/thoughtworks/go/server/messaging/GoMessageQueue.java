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

import com.thoughtworks.go.server.messaging.activemq.JMSMessageListenerAdapter;

public class GoMessageQueue<T extends GoMessage> implements GoMessageChannel<T> {
    private MessagingService messaging;
    protected String queueName;
    private MessageSender queueSender;

    public GoMessageQueue(MessagingService messaging, String queueName) {
        this.messaging = messaging;
        this.queueName = queueName;
    }

    protected MessageSender sender() {
        if (queueSender == null) {
            queueSender = messaging.createQueueSender(queueName);
        }
        return queueSender;
    }

    public JMSMessageListenerAdapter addListener(GoMessageListener<T> listener) {
        return messaging.addQueueListener(queueName, listener);
    }

    public void post(T message) {
        sender().sendMessage(message);
    }

    public void stop() {
        messaging.removeQueue(queueName);
    }
}
