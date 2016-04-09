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
import com.thoughtworks.go.work.FakeWork;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(JMock.class)
public class WorkAssignmentsTest {
    private static final Work NO_WORK = new NoWork();
    private static final Work REAL_WORK = new FakeWork();

    private WorkAssignments assignments;
    private AgentRuntimeInfo agent;
    private Mockery context;
    private IdleAgentTopic idleAgentsTopic;
    private AgentIdentifier agentIdentifier;
    private WorkAssignedTopic assignedWorkTopic;

    @Before
    public void setup() {
        context = new ClassMockery();
        idleAgentsTopic = context.mock(IdleAgentTopic.class, "idle_topic");
        assignedWorkTopic = context.mock(WorkAssignedTopic.class, "assigned_work_topic");
        context.checking(new Expectations() {{
            one(assignedWorkTopic).addListener(with(any(WorkAssignments.class)));
        }});
        assignments = new WorkAssignments(idleAgentsTopic, assignedWorkTopic);
        agentIdentifier = new AgentIdentifier("localhost", "127.0.0.1", "uuid");
        agent = new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false);
    }

    @Test
    public void shouldDispatchIdleMessageWhenNoWork() {
        context.checking(new Expectations() {{
            one(idleAgentsTopic).post(new IdleAgentMessage(agent));
        }});
        assertThat(assignments.getWork(agent), is(NO_WORK));
    }

    @Test
    public void shouldOnlySendIdleMessageOnce() {
        context.checking(new Expectations() {{
            one(idleAgentsTopic).post(new IdleAgentMessage(agent));
        }});
        assertThat(assignments.getWork(agent), is(NO_WORK));
        assertThat(assignments.getWork(agent), is(NO_WORK));
    }

    @Test
    public void shouldGiveAgentAllocatedWork() {
        context.checking(new Expectations() {{
            one(idleAgentsTopic).post(new IdleAgentMessage(agent));
        }});
        assertThat(assignments.getWork(agent), is(NO_WORK));

        assignments.onMessage(new WorkAssignedMessage(agentIdentifier, REAL_WORK));

        assertThat(assignments.getWork(agent), is(REAL_WORK));
    }

    @Test
    public void shouldReturnNoWorkAfterWorkAllocatedOnce() {
        context.checking(new Expectations() {{
            one(idleAgentsTopic).post(new IdleAgentMessage(agent));
        }});
        assertThat(assignments.getWork(agent), is(NO_WORK));

        assignments.onMessage(new WorkAssignedMessage(agentIdentifier, REAL_WORK));
        assignments.getWork(agent);

        context.checking(new Expectations() {{
            one(idleAgentsTopic).post(new IdleAgentMessage(agent));
        }});
        assertThat(assignments.getWork(agent), is(NO_WORK));
    }

    @Test
    public void shouldReSendIdleMessageIfNoWorkAllocated() {
        context.checking(new Expectations() {{
            one(idleAgentsTopic).post(new IdleAgentMessage(agent));
        }});
        assertThat(assignments.getWork(agent), is(NO_WORK));
        assertThat(assignments.getWork(agent), is(NO_WORK));

        assignments.onMessage(new WorkAssignedMessage(agentIdentifier, NO_WORK));

        context.checking(new Expectations() {{
            one(idleAgentsTopic).post(new IdleAgentMessage(agent));
        }});
        assertThat(assignments.getWork(agent), is(NO_WORK));
    }
}
