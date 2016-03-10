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
import com.thoughtworks.go.helper.AgentMother;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.NoWork;
import com.thoughtworks.go.server.perf.WorkAssignmentPerformanceLogger;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.BuildAssignmentService;
import com.thoughtworks.go.util.ClassMockery;
import com.thoughtworks.go.work.FakeWork;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(JMock.class)
public class WorkFinderTest {
    private Mockery context;
    private BuildAssignmentService workAssigner;
    private static final AgentIdentifier AGENT_1 = new AgentIdentifier("localhost", "127.0.0.1", "uuid");
    private static final FakeWork SOME_WORK = new FakeWork();
    private static final NoWork NO_WORK = new NoWork();
    private WorkAssignedTopic assignedWorkTopic;
    private WorkFinder finder;
    private IdleAgentTopic idleAgentTopic;
    private WorkAssignmentPerformanceLogger workAssignmentPerformanceLogger;

    @Before
    public void before() {
        context = new ClassMockery();
        workAssigner = context.mock(BuildAssignmentService.class);
        assignedWorkTopic = context.mock(WorkAssignedTopic.class, "assignedWork");
        idleAgentTopic = context.mock(IdleAgentTopic.class, "idleAgent");
        workAssignmentPerformanceLogger = mock(WorkAssignmentPerformanceLogger.class);

        context.checking(new Expectations() {{
            one(idleAgentTopic).addListener(with(any(WorkFinder.class)));
        }});
        finder = new WorkFinder(workAssigner, idleAgentTopic, assignedWorkTopic, workAssignmentPerformanceLogger);
    }

    @Test
    public void shouldDoNothingIfNoWorkIsAvailable() {
        context.checking(new Expectations() {{
            one(workAssigner).assignWorkToAgent(AGENT_1);
            will(returnValue(NO_WORK));
            one(assignedWorkTopic).post(new WorkAssignedMessage(AGENT_1, NO_WORK));
        }});
        finder.onMessage(new IdleAgentMessage(new AgentRuntimeInfo(AGENT_1, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false)));
    }

    @Test
    public void shouldAssignWorkIfItIsAvailable() {
        context.checking(new Expectations() {{
            one(workAssigner).assignWorkToAgent(AGENT_1);
            will(returnValue(SOME_WORK));
            one(assignedWorkTopic).post(new WorkAssignedMessage(AGENT_1, SOME_WORK));
        }});
        finder.onMessage(new IdleAgentMessage(new AgentRuntimeInfo(AGENT_1, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false)));
    }

    @Test
    public void shouldPostNoWorkOnException() {
        final RuntimeException exception = new RuntimeException("foo");
        context.checking(new Expectations() {{
            one(workAssigner).assignWorkToAgent(AGENT_1);
            will(throwException(exception));
            one(assignedWorkTopic).post(new WorkAssignedMessage(AGENT_1, NO_WORK));
        }});

        try {
            finder.onMessage(new IdleAgentMessage(new AgentRuntimeInfo(AGENT_1, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false)));
        } catch (Exception e) {
            assertSame(exception, e);
        }
    }

    @Test
    public void shouldReturnNoWorkInCaseOfAnErrorIsThrown() {
        BuildAssignmentService assigner = mock(BuildAssignmentService.class);
        IdleAgentTopic idleTopic = mock(IdleAgentTopic.class);
        WorkAssignedTopic assignedTopic = mock(WorkAssignedTopic.class);
        WorkFinder finder = new WorkFinder(assigner, idleTopic, assignedTopic, workAssignmentPerformanceLogger);
        AgentRuntimeInfo runtimeInfo = AgentRuntimeInfo.initialState(AgentMother.approvedAgent());
        when(assigner.assignWorkToAgent(runtimeInfo.getIdentifier())).thenThrow(new OutOfMemoryError("test error for martians"));
        try {
            finder.onMessage(new IdleAgentMessage(runtimeInfo));
            fail("should have propagated error");
        } catch (OutOfMemoryError e) {
            String message = e.getMessage();
            if (message != null && message.equals("test error for martians")) {
                //expected
            } else {
                throw e;
            }
        }
        verify(assignedTopic).post(new WorkAssignedMessage(runtimeInfo.getIdentifier(), new NoWork()));
    }
}
