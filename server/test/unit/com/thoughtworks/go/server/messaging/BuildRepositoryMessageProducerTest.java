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

package com.thoughtworks.go.server.messaging;

import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.BuildRepositoryRemoteImpl;
import com.thoughtworks.go.server.messaging.scheduling.WorkAssignments;
import com.thoughtworks.go.server.perf.WorkAssignmentPerformanceLogger;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class BuildRepositoryMessageProducerTest {
    private AgentIdentifier agentIdentifier = new AgentIdentifier("HOSTNAME", "10.0.0.1", UUID.randomUUID().toString());
    private JobIdentifier jobIdentifier = new JobIdentifier();
    private JobState assigned = JobState.Assigned;
    private JobResult passed = JobResult.Passed;

    private BuildRepositoryRemoteImpl oldImplementation;
    private WorkAssignments newImplementation;
    private BuildRepositoryMessageProducer producer;
    private static final AgentIdentifier AGENT = new AgentIdentifier("localhost", "127.0.0.1", "uuid");
    private static final AgentRuntimeInfo AGENT_INFO = new AgentRuntimeInfo(AGENT, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false);

    @Before
    public void setUp() {
        oldImplementation = mock(BuildRepositoryRemoteImpl.class);
        newImplementation = mock(WorkAssignments.class);
        WorkAssignmentPerformanceLogger workAssignmentPerformanceLogger = mock(WorkAssignmentPerformanceLogger.class);
        producer = new BuildRepositoryMessageProducer(oldImplementation, newImplementation, workAssignmentPerformanceLogger);
    }

    @Test
    public void shouldDelegatePingToTheOldImplementation() {
        producer.ping(new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false));
        verify(oldImplementation).ping(new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false));
    }

    @Test
    public void shouldDelegateReportJobStatusToTheOldImplementation() {
        producer.reportCurrentStatus(new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), jobIdentifier, assigned);
        verify(oldImplementation).reportCurrentStatus(new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), jobIdentifier, assigned);
    }

    @Test
    public void shouldDelegateReportJobResultToTheOldImplementation() {
        producer.reportCompleting(new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), jobIdentifier, passed);
        verify(oldImplementation).reportCompleting(new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false), jobIdentifier, passed);
    }

    @Test
    public void shouldDelegateIgnoreingQueryToTheOldImplementation() {
        producer.isIgnored(jobIdentifier);
        verify(oldImplementation).isIgnored(jobIdentifier);
    }

    @Test
    public void shouldUseEventDrivenImplementationByDefault() {
        producer.getWork(AGENT_INFO);
        verify(newImplementation).getWork(AGENT_INFO);
    }

    @Test
    public void shouldAllocateNewCookieForEveryGetCookieRequest() throws Exception {
        AgentIdentifier identifier = new AgentIdentifier("host", "192.168.1.1", "uuid");
        when(oldImplementation.getCookie(identifier, "/foo/bar")).thenReturn("cookie");
        assertThat(producer.getCookie(identifier, "/foo/bar"), is("cookie"));
        //should not cache
        when(oldImplementation.getCookie(identifier, "/foo/bar")).thenReturn("cookie1");
        assertThat(producer.getCookie(identifier, "/foo/bar"), is("cookie1"));
    }
}
