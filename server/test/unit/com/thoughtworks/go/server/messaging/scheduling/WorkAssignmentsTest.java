/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.messaging.scheduling;

import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.NoWork;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.ClassMockery;
import com.thoughtworks.go.util.TimeProvider;
import com.thoughtworks.go.work.FakeWork;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class WorkAssignmentsTest {
    private static final Work NO_WORK = new NoWork();
    private static final Work REAL_WORK = new FakeWork();

    private WorkAssignments assignments;
    private AgentRuntimeInfo agent;
    private AgentIdentifier agentIdentifier;
    @Mock
    private IdleAgentTopic idleAgentsTopic;
    @Mock
    private WorkAssignedTopic assignedWorkTopic;
    @Mock
    private TimeProvider timeProvider;

    @Before
    public void setup() {
        initMocks(this);

        assignments = new WorkAssignments(idleAgentsTopic, assignedWorkTopic);
        agentIdentifier = new AgentIdentifier("localhost", "127.0.0.1", "uuid");
        agent = new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false, timeProvider);
    }

    @Test
    public void shouldDispatchIdleMessageWhenNoWork() {
        assertThat(assignments.getWork(agent), is(NO_WORK));
        verify(idleAgentsTopic, times(1)).post(new IdleAgentMessage(agent));
    }

    @Test
    public void shouldOnlySendIdleMessageOnce() {
        assertThat(assignments.getWork(agent), is(NO_WORK));
        assertThat(assignments.getWork(agent), is(NO_WORK));
        verify(idleAgentsTopic, times(1)).post(new IdleAgentMessage(agent));
    }

    @Test
    public void shouldGiveAgentAllocatedWork() {
        assertThat(assignments.getWork(agent), is(NO_WORK));

        assignments.onMessage(new WorkAssignedMessage(agentIdentifier, REAL_WORK));

        assertThat(assignments.getWork(agent), is(REAL_WORK));
        verify(idleAgentsTopic, times(1)).post(new IdleAgentMessage(agent));
    }

    @Test
    public void shouldReturnNoWorkAfterWorkAllocatedOnce() {
        Work work = assignments.getWork(agent);
        assertThat(work, is(NO_WORK));

        assignments.onMessage(new WorkAssignedMessage(agentIdentifier, REAL_WORK));
        assignments.getWork(agent);

        assertThat(assignments.getWork(agent), is(NO_WORK));
        verify(idleAgentsTopic, times(2)).post(new IdleAgentMessage(agent));
    }

    @Test
    public void shouldReSendIdleMessageIfNoWorkAllocated() {

        assertThat(assignments.getWork(agent), is(NO_WORK));
        assertThat(assignments.getWork(agent), is(NO_WORK));

        assignments.onMessage(new WorkAssignedMessage(agentIdentifier, NO_WORK));

        assertThat(assignments.getWork(agent), is(NO_WORK));
        verify(idleAgentsTopic, times(2)).post(new IdleAgentMessage(agent));
    }
}
