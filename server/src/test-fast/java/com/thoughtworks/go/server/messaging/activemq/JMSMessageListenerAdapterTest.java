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

package com.thoughtworks.go.server.messaging.activemq;

import javax.jms.MessageConsumer;

import com.thoughtworks.go.server.service.support.DaemonThreadStatsCollector;
import com.thoughtworks.go.server.messaging.GoMessage;
import com.thoughtworks.go.server.messaging.GoMessageListener;
import static org.junit.Assert.fail;

import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JMSMessageListenerAdapterTest {
    @Test public void shouldNotKillTheThreadWhenThereIsAnException() throws Exception {
        MessageConsumer consumer = mock(MessageConsumer.class);
        when(consumer.receive()).thenThrow(new RuntimeException("should swallow me"));
        GoMessageListener mockListener = new GoMessageListener() {
            public void onMessage(GoMessage message) {
                throw new UnsupportedOperationException("not implemented yet");
            }

            @Override public String toString() {
                return "test-listener";
            }
        };
        JMSMessageListenerAdapter listenerAdapter = JMSMessageListenerAdapter.startListening(consumer, mockListener, mock(DaemonThreadStatsCollector.class));
        try {
            listenerAdapter.runImpl();
        } catch (Exception e) {
            e.printStackTrace();
            fail("expected no exception get: " + e);
        }
    }
}
