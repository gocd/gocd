/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.server.messaging.*;
import com.thoughtworks.go.server.service.support.DaemonThreadStatsCollector;
import com.thoughtworks.go.utils.Assertions;
import com.thoughtworks.go.utils.Timeout;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.jms.JMSException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml",
        "classpath:testPropertyConfigurer.xml"
})
public class ActiveMqTest {
    public MessagingService messaging;

    @Before
    public void setUp() throws Exception {
        messaging = new ActiveMqMessagingService(new DaemonThreadStatsCollector());
    }

    @After
    public void tearDown() throws JMSException {
        messaging.stop();
    }

    @Test
    public void shouldBeAbleToListenForMessages() throws Exception {
        GoMessageTopic<GoTextMessage> topic = new GoMessageTopic<GoTextMessage>(messaging, "queue-name") {};
        GoodListener listener = new GoodListener();
        topic.addListener(listener);

        topic.post(new GoTextMessage("Hello World!"));

        waitForReceivedMessages(listener);

        assertThat(listener.receivedMessages().get(0).getText(), is("Hello World!"));
    }

    @Test
    public void shouldSupportCompetingConsumers() throws Exception {
        HangingListener hanging = new HangingListener();
        GoodListener fastListener = new GoodListener();

        GoMessageQueue<GoTextMessage> queue = new GoMessageQueue<GoTextMessage>(messaging, "queue-name") {};
        queue.addListener(hanging);
        queue.addListener(fastListener);

        queue.post(new GoTextMessage("Hello World1"));
        queue.post(new GoTextMessage("Hello World2"));
        queue.post(new GoTextMessage("Hello World3"));
        queue.post(new GoTextMessage("Hello World4"));
        queue.post(new GoTextMessage("Hello World5"));

        waitForReceivedMessages(fastListener);

        assertThat(fastListener.receivedMessages.size(), is(4));

        hanging.finish();
    }

    @Test
    public void shouldStillReceiveMessagesIfAnExceptionIsThrown() throws Exception {
        ExceptionListener exceptionListener = new ExceptionListener();

        GoMessageQueue<GoTextMessage> queue = new GoMessageQueue<GoTextMessage>(messaging, "queue-name") {};
        queue.addListener(exceptionListener);

        queue.post(new GoTextMessage("Hello World1"));
        queue.post(new GoTextMessage("Hello World2"));
        queue.post(new GoTextMessage("Hello World3"));
        queue.post(new GoTextMessage("Hello World4"));
        queue.post(new GoTextMessage("Hello World5"));

        waitForReceivedMessages(exceptionListener);

        assertThat(exceptionListener.receivedMessages().size(), is(5));
    }

    private interface TestListener extends GoMessageListener<GoTextMessage> {
        List<GoTextMessage> receivedMessages();
    }

    private class HangingListener implements TestListener {
        private List<GoTextMessage> receivedMessages = new ArrayList<>();
        private boolean finish;

        public void onMessage(GoTextMessage message) {
            while (!finish) {
                try {
                    Thread.sleep(20000L);
                    receivedMessages.add(message);
                } catch (InterruptedException ignored) {
                }
            }
        }

        public void finish() {
            finish  = true;
        }

        @Override
        public List<GoTextMessage> receivedMessages() {
            return receivedMessages;
        }
    }

    private class GoodListener implements TestListener {
        private List<GoTextMessage> receivedMessages = new ArrayList<>();

        public void onMessage(GoTextMessage message) {
            receivedMessages.add(message);
        }

        @Override
        public List<GoTextMessage> receivedMessages() {
            return receivedMessages;
        }
    }

    private class ExceptionListener extends GoodListener {
        public void onMessage(GoTextMessage message) {
            super.onMessage(message);
            throw new RuntimeException(message.getText());
        }
    }

    private void waitForReceivedMessages(TestListener listener) {
        Assertions.waitUntil(Timeout.FIVE_SECONDS, new Assertions.Predicate() {
            @Override
            public String toString() {
                return "Wait for message to be received";
            }

            @Override
            public boolean call() throws Exception {
                return listener.receivedMessages() != null && listener.receivedMessages().size() > 0;
            }
        });
    }
}
