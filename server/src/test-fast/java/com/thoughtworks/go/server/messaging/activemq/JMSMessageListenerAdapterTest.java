/*
 * Copyright 2024 Thoughtworks, Inc.
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

import ch.qos.logback.classic.Level;
import com.thoughtworks.go.server.messaging.GoMessage;
import com.thoughtworks.go.server.messaging.GoMessageListener;
import com.thoughtworks.go.server.service.support.DaemonThreadStatsCollector;
import com.thoughtworks.go.serverhealth.HealthStateLevel;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.LogFixture;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;

import static com.thoughtworks.go.serverhealth.HealthStateLevel.ERROR;
import static com.thoughtworks.go.util.LogFixture.logFixtureFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@MockitoSettings
public class JMSMessageListenerAdapterTest {

    @Mock
    private MessageConsumer consumer;

    @Mock
    private SystemEnvironment systemEnvironment;

    @Mock
    private ServerHealthService serverHealthService;

    @Mock(stubOnly = true)
    private DaemonThreadStatsCollector daemonThreadStatsCollector;

    private GoMessageListener mockListener;

    @BeforeEach
    public void setUp() throws Exception {
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

    @AfterEach
    public void tearDown() throws Exception {
        // We must reset the consumer to ensure it returns null and the threads exit or they will go-on forever
        // creating a memory leak on the mock invocations
        reset(consumer);
    }

    @Test
    public void shouldNotKillTheThreadWhenThereIsAnException() throws Exception {
        when(consumer.receive()).thenThrow(new RuntimeException("should swallow me"));

        daemonThreadStatsCollector = mock(DaemonThreadStatsCollector.class);
        JMSMessageListenerAdapter listenerAdapter = JMSMessageListenerAdapter.startListening(consumer, mockListener, daemonThreadStatsCollector, systemEnvironment, serverHealthService);
        listenerAdapter.runImpl();

        verify(consumer, atLeastOnce()).receive();
    }

    @Test
    public void shouldBackOffForABitIfAJMSExceptionHappens() throws JMSException {
        when(consumer.receive()).thenThrow(new JMSException("should back off for a bit after this"));
        when(systemEnvironment.get(SystemEnvironment.JMS_LISTENER_BACKOFF_TIME_IN_MILLIS)).thenReturn(3000L);

        try (LogFixture logFixture = logFixtureFor(JMSMessageListenerAdapter.class, Level.DEBUG)) {
            JMSMessageListenerAdapter listenerAdapter = JMSMessageListenerAdapter.startListening(consumer, mockListener, daemonThreadStatsCollector, systemEnvironment, serverHealthService);

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

        try (LogFixture logFixture = logFixtureFor(JMSMessageListenerAdapter.class, Level.DEBUG)) {
            JMSMessageListenerAdapter listenerAdapter = JMSMessageListenerAdapter.startListening(consumer, mockListener, daemonThreadStatsCollector, systemEnvironment, serverHealthService);

            final long startTime = System.nanoTime();
            listenerAdapter.runImpl();
            final long endTime = System.nanoTime();

            assertFalse(logFixture.contains(Level.WARN, "Backing off for a few seconds"));
            assertThat(endTime - startTime, lessThan(1000 * 1000 * 1000L));

            verify(serverHealthService, never()).update(any(ServerHealthState.class));
            verify(systemEnvironment, never()).get(SystemEnvironment.JMS_LISTENER_BACKOFF_TIME_IN_MILLIS);
        }
    }

    private ServerHealthState matchesServerHealthMessage(final HealthStateLevel expectedLevel, String expectedPartOfMessage) {
        return argThat(new ArgumentMatcher<>() {
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
