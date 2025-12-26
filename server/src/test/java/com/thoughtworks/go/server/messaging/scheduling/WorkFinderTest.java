/*
 * Copyright Thoughtworks, Inc.
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class WorkFinderTest {
    private static final AgentIdentifier AGENT_1 = new AgentIdentifier("localhost", "127.0.0.1", "uuid");
    private static final FakeWork SOME_WORK = new FakeWork();
    private static final NoWork NO_WORK = new NoWork();
    private BuildAssignmentService workAssigner;
    private WorkAssignedTopic assignedWorkTopic;
    private WorkFinder finder;
    private IdleAgentTopic idleAgentTopic;
    private WorkAssignmentPerformanceLogger workAssignmentPerformanceLogger;

    @BeforeEach
    public void before() {
        workAssigner = mock(BuildAssignmentService.class);
        assignedWorkTopic = mock(WorkAssignedTopic.class, "assignedWork");
        idleAgentTopic = mock(IdleAgentTopic.class, "idleAgent");
        workAssignmentPerformanceLogger = mock(WorkAssignmentPerformanceLogger.class);

        finder = new WorkFinder(workAssigner, idleAgentTopic, assignedWorkTopic, workAssignmentPerformanceLogger);
    }

    @AfterEach
    public void tearDown() {
        verify(idleAgentTopic).addListener(any());
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

        assertThatThrownBy(() -> finder.onMessage(new IdleAgentMessage(new AgentRuntimeInfo(AGENT_1, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"))))
            .isSameAs(exception);
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

        assertThatThrownBy(() -> finder.onMessage(new IdleAgentMessage(runtimeInfo)))
                .isInstanceOf(OutOfMemoryError.class)
                .hasMessage("test error for martians");

        verify(assignedTopic).post(new WorkAssignedMessage(runtimeInfo.getIdentifier(), new NoWork()));
    }
}
