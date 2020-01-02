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

import ch.qos.logback.classic.Level;
import com.thoughtworks.go.server.messaging.MultiplexingQueueProcessor.Action;
import com.thoughtworks.go.util.LogFixture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import static com.thoughtworks.go.util.LogFixture.logFixtureFor;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class MultiplexingQueueProcessorTest {
    private MultiplexingQueueProcessor queueProcessor;

    @Before
    public void setUp() throws Exception {
        queueProcessor = new MultiplexingQueueProcessor("queue1");
    }

    @Test
    public void shouldMultiplexActionsFromDifferentThreadsOnToHandlersOnASingleThread() throws Exception {
        ThreadNameAccumulator t1NameAccumulator = new ThreadNameAccumulator();
        Thread t1 = setupNewThreadToAddActionIn(t1NameAccumulator);

        ThreadNameAccumulator t2NameAccumulator = new ThreadNameAccumulator();
        Thread t2 = setupNewThreadToAddActionIn(t2NameAccumulator);

        queueProcessor.start();
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        waitForProcessingToHappen();

        assertThat(t1NameAccumulator.threadOfCall, is(not(nullValue())));
        assertThat(t1NameAccumulator.threadOfCall, is(not(t1NameAccumulator.threadOfQueueAdd)));
        assertThat(t1NameAccumulator.threadOfCall, is(not(Thread.currentThread().getName())));

        assertThat(t2NameAccumulator.threadOfCall, is(not(nullValue())));
        assertThat(t2NameAccumulator.threadOfCall, is(not(t2NameAccumulator.threadOfQueueAdd)));
        assertThat(t2NameAccumulator.threadOfCall, is(not(Thread.currentThread().getName())));

        assertThat(t1NameAccumulator.threadOfCall, is(t2NameAccumulator.threadOfCall));
    }

    @Test
    public void shouldNotAllowTheQueueProcessorToBeStartedMultipleTimes() throws Exception {
        queueProcessor.start();

        try {
            queueProcessor.start();
            fail("Should have failed to start queue processor a second time.");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("Cannot start queue processor for queue1 multiple times."));
        }
    }

    @Test(timeout = 5000)
    public void shouldLogAndIgnoreAnyActionsWhichFail() throws Exception {
        Action successfulAction1 = mock(Action.class);
        Action successfulAction2 = mock(Action.class);

        Action failingAction = mock(Action.class);
        doThrow(new RuntimeException("Ouch. Failed.")).when(failingAction).call();

        try (LogFixture logFixture = logFixtureFor(MultiplexingQueueProcessor.class, Level.WARN)) {
            queueProcessor.add(successfulAction1);
            queueProcessor.add(failingAction);
            queueProcessor.add(successfulAction2);
            queueProcessor.start();
            while (!queueProcessor.queue.isEmpty()) {
                waitForProcessingToHappen(100);
            }

            synchronized (logFixture) {
                assertThat(logFixture.contains(Level.WARN, "Failed to handle action in queue1 queue"), is(true));
                assertThat(logFixture.getLog(), containsString("Ouch. Failed."));
            }
        }

        verify(successfulAction1).call();
        verify(successfulAction2).call();
    }

    @Test
    public void shouldProcessAllActionsInOrderOfThemBeingAdded() throws Exception {
        Action action1 = mock(Action.class);
        Action action2 = mock(Action.class);
        Action action3 = mock(Action.class);

        queueProcessor.add(action1);
        queueProcessor.add(action2);
        queueProcessor.add(action3);

        queueProcessor.start();
        waitForProcessingToHappen();

        InOrder inOrder = inOrder(action1, action2, action3);
        inOrder.verify(action1).call();
        inOrder.verify(action2).call();
        inOrder.verify(action3).call();
    }

    private Thread setupNewThreadToAddActionIn(final ThreadNameAccumulator threadNameAccumulator) {
        return new Thread() {
            @Override
            public void run() {
                threadNameAccumulator.threadOfQueueAdd = Thread.currentThread().getName();

                queueProcessor.add(new Action() {
                    @Override
                    public void call() {
                        threadNameAccumulator.threadOfCall = Thread.currentThread().getName();
                    }

                    @Override
                    public String description() {
                        return "some-action";
                    }
                });
            }
        };
    }

    private void waitForProcessingToHappen() throws InterruptedException {
        waitForProcessingToHappen(1000); /* Prevent potential race, of queue not being processed. Being a little lazy. :( */
    }

    private void waitForProcessingToHappen(int time) throws InterruptedException {
        Thread.sleep(time);
    }

    private class ThreadNameAccumulator {
        String threadOfCall;
        String threadOfQueueAdd;
    }
}
