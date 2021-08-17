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
package com.thoughtworks.go.server.messaging.scheduling;

import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.NoWork;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.work.FakeWork;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class WorkAssignmentsTest {
    private static final Work NO_WORK = new NoWork();
    private static final Work REAL_WORK = new FakeWork();

    private WorkAssignments assignments;
    private AgentRuntimeInfo agent;
    private IdleAgentTopic idleAgentsTopic;
    private AgentIdentifier agentIdentifier;
    private WorkAssignedTopic assignedWorkTopic;

    @BeforeEach
    public void setup() {
        idleAgentsTopic = mock(IdleAgentTopic.class, "idle_topic");
        assignedWorkTopic = mock(WorkAssignedTopic.class, "assigned_work_topic");
        assignments = new WorkAssignments(idleAgentsTopic, assignedWorkTopic);
        agentIdentifier = new AgentIdentifier("localhost", "127.0.0.1", "uuid");
        agent = new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
    }

    @AfterEach
    public void tearDown() throws Exception {
        verify(assignedWorkTopic).addListener(any(WorkAssignments.class));
    }

    @Test
    public void shouldDispatchIdleMessageWhenNoWork() {
        assertThat(assignments.getWork(agent), is(NO_WORK));
        verify(idleAgentsTopic).post(new IdleAgentMessage(agent));
    }

    @Test
    public void shouldOnlySendIdleMessageOnce() {
        assertThat(assignments.getWork(agent), is(NO_WORK));
        assertThat(assignments.getWork(agent), is(NO_WORK));
        verify(idleAgentsTopic).post(new IdleAgentMessage(agent));
    }

    @Test
    public void shouldGiveAgentAllocatedWork() {
        assertThat(assignments.getWork(agent), is(NO_WORK));

        assignments.onMessage(new WorkAssignedMessage(agentIdentifier, REAL_WORK));
        verify(idleAgentsTopic).post(new IdleAgentMessage(agent));

        assertThat(assignments.getWork(agent), is(REAL_WORK));
    }

    @Test
    public void shouldReturnNoWorkAfterWorkAllocatedOnce() {
        assertThat(assignments.getWork(agent), is(NO_WORK));

        assignments.onMessage(new WorkAssignedMessage(agentIdentifier, REAL_WORK));
        assignments.getWork(agent);
        verify(idleAgentsTopic).post(new IdleAgentMessage(agent));

        assertThat(assignments.getWork(agent), is(NO_WORK));
        verify(idleAgentsTopic, times(2)).post(new IdleAgentMessage(agent));
    }

    @Test
    public void shouldReSendIdleMessageIfNoWorkAllocated() {
        assertThat(assignments.getWork(agent), is(NO_WORK));
        assertThat(assignments.getWork(agent), is(NO_WORK));
        verify(idleAgentsTopic).post(new IdleAgentMessage(agent));

        assignments.onMessage(new WorkAssignedMessage(agentIdentifier, NO_WORK));

        assertThat(assignments.getWork(agent), is(NO_WORK));
        verify(idleAgentsTopic, times(2)).post(new IdleAgentMessage(agent));
    }
}
