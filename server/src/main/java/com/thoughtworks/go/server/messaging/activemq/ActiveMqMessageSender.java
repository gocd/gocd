/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.messaging.activemq;

import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.JMSException;

import com.thoughtworks.go.server.messaging.GoMessage;
import com.thoughtworks.go.server.messaging.MessageSender;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class ActiveMqMessageSender implements MessageSender {
    private Session session;
    private MessageProducer producer;

    public ActiveMqMessageSender(Session session, MessageProducer producer) {
        this.session = session;
        this.producer = producer;
    }

    @Override
    public void sendMessage(GoMessage message) {
        try {
            producer.send(session.createObjectMessage(message));
        } catch (JMSException e) {
            throw bomb(e);
        }
    }

    @Override
    public void sendMessage(GoMessage goMessage, long timeToLive) {
        try {
            producer.send(session.createObjectMessage(goMessage), producer.getDeliveryMode(), producer.getPriority(), timeToLive);
        } catch (JMSException e) {
            throw bomb(e);
        }
    }

    @Override
    public void sendText(String message) {
        try {
            producer.send(session.createTextMessage(message));
        } catch (JMSException e) {
            throw bomb(e);
        }
    }
}
