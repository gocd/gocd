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
package com.thoughtworks.go.server.messaging;

import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.utils.Timeout;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashSet;
import java.util.Set;

import static com.thoughtworks.go.utils.Assertions.assertWillHappen;
import static org.hamcrest.Matchers.is;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class GoTopicTest {
    @Autowired private MessagingService messageService;


    @BeforeClass
    public static void beforeClass() {
        GoConfigFileHelper configFileHelper = new GoConfigFileHelper();
    }

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

        assertWillHappen(listener1.receivedMessage, is(expectedMessages), Timeout.FIVE_SECONDS);
        assertWillHappen(listener2.receivedMessage, is(expectedMessages), Timeout.FIVE_SECONDS);
    }

    class StubGoMessageListener implements GoMessageListener<GoMessage> {
        Set<String> receivedMessage = new HashSet<>();

        @Override
        public void onMessage(GoMessage message) {
            receivedMessage.add(((GoTextMessage) message).getText());
        }
    }

}
