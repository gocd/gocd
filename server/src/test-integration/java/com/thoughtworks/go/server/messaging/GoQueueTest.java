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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
    "classpath:/applicationContext-global.xml",
    "classpath:/applicationContext-dataLocalAccess.xml",
    "classpath:/testPropertyConfigurer.xml",
    "classpath:/spring-all-servlet.xml",
})
public class GoQueueTest {
    @Autowired
    private MessagingService<GoMessage> messageService;
    Set<String> receivedMessage = Collections.synchronizedSet(new HashSet<>());

    @Test
    public void shouldNotifyOneListenerForEachMessage() {
        GoMessageQueue<GoMessage> queue = new GoMessageQueue<>(messageService, "TestQueue");

        int numberOfListeners = 2;
        int numberOfMessages = numberOfListeners + 2;

        for (int i = 0; i < numberOfListeners; i++) {
            queue.addListener(new StubGoMessageListener(i));
        }

        Set<String> expectMessages = ConcurrentHashMap.newKeySet();
        for (int i = 0; i < numberOfMessages; i++) {
            String text = "Message-" + i;
            queue.post(new GoTextMessage(text));
            expectMessages.add(text);
        }

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> assertThat(receivedMessage).isEqualTo(expectMessages));
    }


    class StubGoMessageListener implements GoMessageListener<GoMessage> {
        private final int id;

        public StubGoMessageListener(int id) {
            this.id = id;
        }

        @Override
        public void onMessage(GoMessage message) {
            String text = ((GoTextMessage) message).getText();
            receivedMessage.add(text);

            if (id == 0) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

        }
    }
}
