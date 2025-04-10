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

import java.util.HashSet;
import java.util.Set;
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
public class GoTopicTest {
    @Autowired private MessagingService<GoMessage> messageService;

    @Test
    public void shouldNotifyAllListeners() {
        GoMessageTopic<GoMessage> topic = new GoMessageTopic<>(messageService,
                "TestTopic-All");

        int numberOfMessages = 2;
        Set<String> expectedMessages = new HashSet<>();

        StubGoMessageListener listener1 = new StubGoMessageListener();
        topic.addListener(listener1);
        StubGoMessageListener listener2 = new StubGoMessageListener();
        topic.addListener(listener2);

        for (int i = 0; i < numberOfMessages; i++) {
            String message = "message-" + i;
            topic.post(new GoTextMessage(message));
            expectedMessages.add(message);
        }

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> assertThat(listener1.receivedMessage).isEqualTo(expectedMessages));
        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> assertThat(listener2.receivedMessage).isEqualTo(expectedMessages));
    }

    static class StubGoMessageListener implements GoMessageListener<GoMessage> {
        Set<String> receivedMessage = new HashSet<>();

        @Override
        public void onMessage(GoMessage message) {
            receivedMessage.add(((GoTextMessage) message).getText());
        }
    }

}
