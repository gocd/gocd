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
package com.thoughtworks.go.server.messaging.activemq;

import com.thoughtworks.go.server.messaging.GoMessageListener;
import com.thoughtworks.go.server.messaging.GoMessageQueue;
import com.thoughtworks.go.server.messaging.GoMessageTopic;
import com.thoughtworks.go.server.messaging.GoTextMessage;
import com.thoughtworks.go.server.service.support.DaemonThreadStatsCollector;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.thoughtworks.go.util.TestUtils.doInterruptiblyQuietly;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class ActiveMqTest {
    public ActiveMqMessagingService messaging;

    @BeforeEach
    public void setUp() throws Exception {
        messaging = new ActiveMqMessagingService(new DaemonThreadStatsCollector(), new SystemEnvironment(), new ServerHealthService());
    }

    @AfterEach
    public void tearDown() throws Exception {
        messaging.stop();
    }

    @Test
    public void shouldBeAbleToListenForMessages() {
        GoMessageTopic<GoTextMessage> topic = new GoMessageTopic<>(messaging, "queue-name");
        final AtomicReference<GoTextMessage> receivedMessage = new AtomicReference<>();
        topic.addListener(receivedMessage::set);

        topic.post(new GoTextMessage("Hello World!"));

        await()
            .pollDelay(10, TimeUnit.MILLISECONDS)
            .timeout(2, TimeUnit.SECONDS)
            .untilAsserted(() -> assertThat(receivedMessage).hasValueSatisfying(m -> assertThat(m.getText()).isEqualTo("Hello World!")));
    }

    @Test
    public void shouldSupportCompetingConsumers() {
        HangingListener hanging = new HangingListener();
        FastListener fast1 = new FastListener();

        GoMessageQueue<GoTextMessage> queue
                = new GoMessageQueue<>(messaging, "queue-name") {
        };
        queue.addListener(hanging);
        queue.addListener(fast1);

        queue.post(new GoTextMessage("Hello World1"));
        queue.post(new GoTextMessage("Hello World2"));
        queue.post(new GoTextMessage("Hello World3"));
        queue.post(new GoTextMessage("Hello World4"));
        queue.post(new GoTextMessage("Hello World5"));

        await()
            .pollDelay(10, TimeUnit.MILLISECONDS)
            .timeout(1, TimeUnit.SECONDS)
            .untilAsserted(() -> assertThat(fast1.receivedMessages.size()).isEqualTo(4));

        hanging.finish();
    }

    @Test
    public void shouldStillReceiveMessagesIfAnExceptionIsThrown() {
        ExceptionListener exceptionListener = new ExceptionListener();

        GoMessageQueue<GoTextMessage> queue
                = new GoMessageQueue<>(messaging, "queue-name") {
        };
        queue.addListener(exceptionListener);

        queue.post(new GoTextMessage("Hello World1"));
        queue.post(new GoTextMessage("Hello World2"));
        queue.post(new GoTextMessage("Hello World3"));
        queue.post(new GoTextMessage("Hello World4"));
        queue.post(new GoTextMessage("Hello World5"));

        await()
            .pollDelay(10, TimeUnit.MILLISECONDS)
            .timeout(1, TimeUnit.SECONDS)
            .untilAsserted(() -> assertThat(exceptionListener.receivedMessages.size()).isEqualTo(5));
    }

    private static class HangingListener implements GoMessageListener<GoTextMessage> {
        private final CountDownLatch finish = new CountDownLatch(1);

        @Override
        public void onMessage(GoTextMessage message) {
            doInterruptiblyQuietly(finish::await);
        }

        public void finish() {
            finish.countDown();
        }
    }

    private static class FastListener implements GoMessageListener<GoTextMessage> {
        public List<GoTextMessage> receivedMessages = new ArrayList<>();

        @Override
        public void onMessage(GoTextMessage message) {
            receivedMessages.add(message);
        }
    }

    private static class ExceptionListener extends FastListener {
        @Override
        public void onMessage(GoTextMessage message) {
            super.onMessage(message);

            throw new RuntimeException(message.getText());
        }
    }

}
