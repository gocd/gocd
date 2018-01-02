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

package com.thoughtworks.go.server.messaging;

import com.thoughtworks.go.server.messaging.activemq.JMSMessageListenerAdapter;

public class GoMessageTopic<T extends GoMessage> implements GoMessageChannel<T> {
    private MessagingService messaging;
    private String topic;
    private MessageSender sender;

    public GoMessageTopic(MessagingService messaging, String topic) {
        this.messaging = messaging;
        this.topic = topic;        
    }

    protected MessageSender sender() {
        if (sender == null) {
            sender = messaging.createSender(topic);
        }
        return sender;
    }

    public JMSMessageListenerAdapter addListener(GoMessageListener<T> listener) {
        return messaging.addListener(topic, listener);
    }

    public void sendText(String message) {
        sender().sendText(message);
    }

    public void post(T message) {
        sender().sendMessage(message);
    }
}