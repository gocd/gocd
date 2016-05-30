/*
 * Copyright 2015 ThoughtWorks, Inc.
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

import java.util.ArrayList;
import java.util.List;
import javax.jms.JMSException;

import com.thoughtworks.go.server.messaging.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class ActiveMqTest implements GoMessageListener {
    private GoMessage receivedMessage;
    public MessagingService messaging;

    @Before
    public void setUp() throws Exception {
        messaging = new ActiveMqMessagingService();

    }

    @After
    public void tearDown() throws JMSException {
        receivedMessage = null;
        messaging.stop();
    }

    @Test
    public void shouldBeAbleToListenForMessages() throws Exception {
        GoMessageTopic<GoTextMessage> topic
                = new GoMessageTopic<GoTextMessage>(messaging, "queue-name") {};
        topic.addListener(this);

        topic.post(new GoTextMessage("Hello World!"));

        Thread.sleep(1000);

        assertThat(((GoTextMessage) receivedMessage).getText(), is("Hello World!"));
    }

    @Test
    public void shouldSupportCompetingConsumers() throws Exception {
        HangingListener hanging = new HangingListener();
        FastListener fast1 = new FastListener();

        GoMessageQueue<GoTextMessage> queue
                = new GoMessageQueue<GoTextMessage>(messaging, "queue-name") {};
        queue.addListener(hanging);
        queue.addListener(fast1);

        queue.post(new GoTextMessage("Hello World1"));
        queue.post(new GoTextMessage("Hello World2"));
        queue.post(new GoTextMessage("Hello World3"));
        queue.post(new GoTextMessage("Hello World4"));
        queue.post(new GoTextMessage("Hello World5"));

        Thread.sleep(1000);

        assertThat(fast1.receivedMessages.size(), is(4));

        hanging.finish();
    }

    @Test
    public void shouldStillReceiveMessagesIfAnExceptionIsThrown() throws Exception {
        ExceptionListener exceptionListener = new ExceptionListener();

        GoMessageQueue<GoTextMessage> queue
                = new GoMessageQueue<GoTextMessage>(messaging, "queue-name") {};
        queue.addListener(exceptionListener);

        queue.post(new GoTextMessage("Hello World1"));
        queue.post(new GoTextMessage("Hello World2"));
        queue.post(new GoTextMessage("Hello World3"));
        queue.post(new GoTextMessage("Hello World4"));
        queue.post(new GoTextMessage("Hello World5"));

        Thread.sleep(1000);

        assertThat(exceptionListener.receivedMessages.size(), is(5));
    }

    public void onMessage(GoMessage message) {
        receivedMessage = message;
    }

    private class HangingListener implements GoMessageListener<GoTextMessage> {
        private boolean finish;

        public void onMessage(GoTextMessage message) {
            while (finish == false) {
                try {
                    Thread.sleep(20000L);
                } catch (InterruptedException e) {

                }
            }
        }

        public void finish() {
            finish  = true;
        }
    }

    private class FastListener implements GoMessageListener<GoTextMessage> {
        public List<GoTextMessage> receivedMessages = new ArrayList<GoTextMessage>();

        public void onMessage(GoTextMessage message) {
            receivedMessages.add(message);
        }
    }

    private class ExceptionListener extends FastListener {
        public void onMessage(GoTextMessage message) {
            super.onMessage(message);

            throw new RuntimeException(message.getText());
        }
    }

}
