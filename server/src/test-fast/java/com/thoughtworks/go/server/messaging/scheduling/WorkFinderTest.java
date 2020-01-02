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
package com.thoughtworks.go.server.messaging.scheduling;

import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.helper.AgentMother;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.NoWork;
import com.thoughtworks.go.server.perf.WorkAssignmentPerformanceLogger;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.BuildAssignmentService;
import com.thoughtworks.go.work.FakeWork;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class WorkFinderTest {
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
        workAssigner = mock(BuildAssignmentService.class);
        assignedWorkTopic = mock(WorkAssignedTopic.class, "assignedWork");
        idleAgentTopic = mock(IdleAgentTopic.class, "idleAgent");
        workAssignmentPerformanceLogger = mock(WorkAssignmentPerformanceLogger.class);

        finder = new WorkFinder(workAssigner, idleAgentTopic, assignedWorkTopic, workAssignmentPerformanceLogger);
    }

    @After
    public void tearDown() throws Exception {
        verify(idleAgentTopic).addListener(any(WorkFinder.class));
    }

    @Test
    public void shouldDoNothingIfNoWorkIsAvailable() {
        when(workAssigner.assignWorkToAgent(AGENT_1)).thenReturn(NO_WORK);
        finder.onMessage(new IdleAgentMessage(new AgentRuntimeInfo(AGENT_1, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie")));
        verify(assignedWorkTopic).post(new WorkAssignedMessage(AGENT_1, NO_WORK));
    }

    @Test
    public void shouldAssignWorkIfItIsAvailable() {
        when(workAssigner.assignWorkToAgent(AGENT_1)).thenReturn(SOME_WORK);
        finder.onMessage(new IdleAgentMessage(new AgentRuntimeInfo(AGENT_1, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie")));
        verify(assignedWorkTopic).post(new WorkAssignedMessage(AGENT_1, SOME_WORK));
    }

    @Test
    public void shouldPostNoWorkOnException() {
        final RuntimeException exception = new RuntimeException("foo");
        when(workAssigner.assignWorkToAgent(AGENT_1)).thenThrow(exception);

        try {
            finder.onMessage(new IdleAgentMessage(new AgentRuntimeInfo(AGENT_1, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie")));
        } catch (Exception e) {
            assertSame(exception, e);
        }
        verify(assignedWorkTopic).post(new WorkAssignedMessage(AGENT_1, NO_WORK));
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
