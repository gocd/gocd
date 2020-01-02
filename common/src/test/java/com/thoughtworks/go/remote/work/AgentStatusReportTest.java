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
package com.thoughtworks.go.remote.work;

import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class AgentStatusReportTest {
    private AgentIdentifier agentIdentifier;
    private EnvironmentVariableContext environmentVariableContext;
    private GoArtifactsManipulatorStub artifactManipulator;
    private BuildRepositoryRemoteStub buildRepository;
    private AgentRuntimeInfo agentRuntimeInfo;

    @Before
    public void before() {
        agentIdentifier = new AgentIdentifier("localhost", "127.0.0.1", "uuid");
        environmentVariableContext = new EnvironmentVariableContext();
        artifactManipulator = new GoArtifactsManipulatorStub();
        buildRepository = new BuildRepositoryRemoteStub();
        this.agentRuntimeInfo = new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
    }

    @After
    public void tearDown() {
        new SystemEnvironment().clearProperty("serviceUrl");
    }

    @Test
    public void shouldReportIdleWhenAgentRunningNoWork() {
        NoWork work = new NoWork();
        work.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, agentRuntimeInfo, null, null, null, null, null));
        assertThat(agentRuntimeInfo, is(new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie")));
    }

    @Test
    public void shouldReportIdleWhenAgentCancelledNoWork() {
        NoWork work = new NoWork();
        work.cancel(environmentVariableContext, agentRuntimeInfo);
        assertThat(agentRuntimeInfo, is(new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie")));
    }

    @Test
    public void shouldReportIdleWhenAgentRunningDeniedWork() {
        Work work = new DeniedAgentWork("uuid");
        work.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, agentRuntimeInfo, null, null, null, null, null));
        assertThat(agentRuntimeInfo, is(new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie")));
    }

    @Test
    public void shouldReportIdleWhenAgentCancelledDeniedWork() {
        Work work = new DeniedAgentWork("uuid");
        work.cancel(environmentVariableContext, agentRuntimeInfo);
        assertThat(agentRuntimeInfo, is(new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie")));
    }

    @Test
    public void shouldNotChangeWhenAgentRunningUnregisteredAgentWork() {
        Work work = new UnregisteredAgentWork("uuid");
        try {
            work.doWork(environmentVariableContext, new AgentWorkContext(agentIdentifier, buildRepository, artifactManipulator, agentRuntimeInfo, null, null, null, null, null));
        } catch (Exception e) {
        }
        assertThat(agentRuntimeInfo, is(new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie")));
    }

    @Test
    public void shouldNotChangeIdleWhenAgentCancelledUnregisteredAgentWork() {
        Work work = new UnregisteredAgentWork("uuid");
        try {
            work.cancel(environmentVariableContext, agentRuntimeInfo);
        } catch (Exception e) {
        }
        assertThat(agentRuntimeInfo, is(new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie")));
    }
}
