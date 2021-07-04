/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import javax.jms.JMSException;
import javax.jms.MessageConsumer;

import ch.qos.logback.classic.Level;
import com.thoughtworks.go.server.service.support.DaemonThreadStatsCollector;
import com.thoughtworks.go.server.messaging.GoMessage;
import com.thoughtworks.go.server.messaging.GoMessageListener;

import static com.thoughtworks.go.serverhealth.HealthStateLevel.ERROR;
import static com.thoughtworks.go.util.LogFixture.logFixtureFor;

import com.thoughtworks.go.serverhealth.HealthStateLevel;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.LogFixture;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class JMSMessageListenerAdapterTest {

    private MessageConsumer consumer;
    private GoMessageListener mockListener;
    private SystemEnvironment systemEnvironment;
    private ServerHealthService serverHealthService;

    @BeforeEach
    public void setUp() throws Exception {
        consumer = mock(MessageConsumer.class);
        systemEnvironment = mock(SystemEnvironment.class);
        serverHealthService = mock(ServerHealthService.class);

        mockListener = new GoMessageListener() {
            @Override
            public void onMessage(GoMessage message) {
                throw new UnsupportedOperationException("not implemented yet");
            }

            @Override
            public String toString() {
                return "test-listener";
            }
        };
    }

    @Test
    public void shouldNotKillTheThreadWhenThereIsAnException() throws Exception {
        when(consumer.receive()).thenThrow(new RuntimeException("should swallow me"));

        JMSMessageListenerAdapter listenerAdapter = JMSMessageListenerAdapter.startListening(consumer, mockListener, mock(DaemonThreadStatsCollector.class), systemEnvironment, serverHealthService);
        try {
            listenerAdapter.runImpl();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Expected no exception. Got: " + e);
        }
    }

    @Test
    public void shouldBackOffForABitIfAJMSExceptionHappens() throws JMSException {
        when(consumer.receive()).thenThrow(new JMSException("should back off for a bit after this"));
        when(systemEnvironment.get(SystemEnvironment.JMS_LISTENER_BACKOFF_TIME)).thenReturn(3000);

        try (LogFixture logFixture = logFixtureFor(JMSMessageListenerAdapter.class, Level.DEBUG)) {
            JMSMessageListenerAdapter listenerAdapter = JMSMessageListenerAdapter.startListening(consumer, mockListener, mock(DaemonThreadStatsCollector.class), systemEnvironment, serverHealthService);

            final long startTime = System.nanoTime();
            listenerAdapter.runImpl();
            final long endTime = System.nanoTime();

            assertTrue(logFixture.contains(Level.WARN, "Backing off for a few seconds"));
            assertThat(endTime - startTime, greaterThan(2000 * 1000 * 1000L));
            assertThat(endTime - startTime, lessThan(4000 * 1000 * 1000L));

            verify(serverHealthService, atLeastOnce()).update(matchesServerHealthMessage(ERROR, "Message queue closed"));
        }
    }

    @Test
    public void shouldNotBackOffAfterNonJMSExceptionHappens() throws JMSException {
        when(consumer.receive()).thenThrow(new RuntimeException("should NOT back off after this"));
        when(systemEnvironment.get(SystemEnvironment.JMS_LISTENER_BACKOFF_TIME)).thenThrow(new RuntimeException("Should not have needed listener backoff time"));

        try (LogFixture logFixture = logFixtureFor(JMSMessageListenerAdapter.class, Level.DEBUG)) {
            JMSMessageListenerAdapter listenerAdapter = JMSMessageListenerAdapter.startListening(consumer, mockListener, mock(DaemonThreadStatsCollector.class), systemEnvironment, serverHealthService);

            final long startTime = System.nanoTime();
            listenerAdapter.runImpl();
            final long endTime = System.nanoTime();

            assertFalse(logFixture.contains(Level.WARN, "Backing off for a few seconds"));
            assertThat(endTime - startTime, lessThan(1000 * 1000 * 1000L));

            verify(serverHealthService, never()).update(any(ServerHealthState.class));
        }
    }

    private ServerHealthState matchesServerHealthMessage(final HealthStateLevel expectedLevel, String expectedPartOfMessage) {
        return argThat(new ArgumentMatcher<ServerHealthState>() {
            @Override
            public boolean matches(ServerHealthState argument) {
                return argument.getLogLevel().equals(expectedLevel) && argument.getMessage().contains(expectedPartOfMessage);
            }

            @Override
            public String toString() {
                return "a server health message of level " + expectedLevel + " with message containing " + expectedPartOfMessage;
            }
        });
    }

}
