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

package com.thoughtworks.go.server.messaging;

import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.utils.Timeout;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.thoughtworks.go.utils.Assertions.assertWillHappen;
import static org.hamcrest.core.Is.is;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class GoQueueTest {
    @Autowired private MessagingService messageService;
    Set<String> receivedMessage = Collections.synchronizedSet(new HashSet<String>());

    @BeforeClass
    public static void beforeClass() {
        GoConfigFileHelper configFileHelper = new GoConfigFileHelper();
    }

    @Test
    public void shouldNotifyOneListenerForEachMessage() throws InterruptedException {
        GoMessageQueue<GoMessage> queue = new GoMessageQueue<>(messageService, "TestQueue");

        int numberOfListeners = 2;
        int numberOfMessages = numberOfListeners + 2;

        for (int i = 0; i < numberOfListeners; i++) {
            queue.addListener(new StubGoMessageListener(i));
        }

        Set<String> expectMessages = new ConcurrentHashSet<>();
        for (int i = 0; i < numberOfMessages; i++) {
            String text = "Message-" + i;
            queue.post(new GoTextMessage(text));
            expectMessages.add(text);
        }

        assertWillHappen(receivedMessage, is(expectMessages), Timeout.FIVE_SECONDS);
    }


    class StubGoMessageListener implements GoMessageListener<GoMessage> {
        private int id;

        public StubGoMessageListener(int id) {
            this.id = id;
        }

        public void onMessage(GoMessage message) {
            String text = ((GoTextMessage) message).getText();
//            System.out.println(
//                    "Listener - " + id + " thread id=" + Thread.currentThread().getJobId() + " Got message: " + text);

            receivedMessage.add(text);

            if (id == 0) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}
