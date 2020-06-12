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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.AgentStatus;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ElasticAgentRuntimeInfoTest {

    @Test
    public void shouldUpdateSelfForAnIdleAgent() throws Exception {
        ElasticAgentRuntimeInfo agentRuntimeInfo = new ElasticAgentRuntimeInfo(new AgentIdentifier("localhost", "127.0.0.1", "uuid"), AgentRuntimeStatus.Idle, "/foo/one", null, "42", "go.cd.elastic-agent-plugin.docker");
        agentRuntimeInfo.updateAgentVersion("old.version.1");
        agentRuntimeInfo.updateBootstrapperVersion("old.version.2");

        ElasticAgentRuntimeInfo newRuntimeInfo = new ElasticAgentRuntimeInfo(new AgentIdentifier("go02", "10.10.10.1", "uuid"), AgentStatus.Building.getRuntimeStatus(), "/foo/two", "cookie", "42", "go.cd.elastic-agent-plugin.docker");
        newRuntimeInfo.updateAgentVersion("20.5.0-2345");
        newRuntimeInfo.updateBootstrapperVersion("20.3.0-1234");

        agentRuntimeInfo.updateSelf(newRuntimeInfo);

        assertThat(agentRuntimeInfo.getBuildingInfo(), is(newRuntimeInfo.getBuildingInfo()));
        assertThat(agentRuntimeInfo.getLocation(), is(newRuntimeInfo.getLocation()));
        assertThat(agentRuntimeInfo.getUsableSpace(), is(newRuntimeInfo.getUsableSpace()));
        assertThat(agentRuntimeInfo.getOperatingSystem(), is(newRuntimeInfo.getOperatingSystem()));
        assertThat(agentRuntimeInfo.getElasticAgentId(), is(newRuntimeInfo.getElasticAgentId()));
        assertThat(agentRuntimeInfo.getElasticPluginId(), is(newRuntimeInfo.getElasticPluginId()));
        assertThat(agentRuntimeInfo.getAgentVersion(), is("20.5.0-2345"));
        assertThat(agentRuntimeInfo.getAgentBootstrapperVersion(), is("20.3.0-1234"));
    }

    @Test
    public void shouldRefreshOperatingSystemOfAgent() throws Exception {
        AgentIdentifier identifier = new AgentIdentifier("local.in", "127.0.0.1", "uuid-1");
        AgentRuntimeInfo runtimeInfo = ElasticAgentRuntimeInfo.fromAgent(identifier, AgentRuntimeStatus.Idle, "/tmp/foo", "20.3.0-1234", "20.5.0-2345");
        String os = new SystemEnvironment().getOperatingSystemCompleteName();
        assertThat(runtimeInfo.getOperatingSystem(), is(os));
    }

    @Test
    public void shouldRefreshUsableSpaceOfAgent() throws Exception {
        AgentIdentifier identifier = new AgentIdentifier("local.in", "127.0.0.1", "uuid-1");
        String workingDirectory = FileUtils.getTempDirectory().getAbsolutePath();
        AgentRuntimeInfo runtimeInfo = ElasticAgentRuntimeInfo.fromAgent(identifier, AgentRuntimeStatus.Idle, workingDirectory, "20.3.0-1234", "20.5.0-2345");
        long space = ElasticAgentRuntimeInfo.usableSpace(workingDirectory);
        assertThat(runtimeInfo.getUsableSpace(), is(space));
    }

    @Test
    public void shouldUpdateAgentAndBootstrapperVersions() {
        AgentIdentifier identifier = new AgentIdentifier("local.in", "127.0.0.1", "uuid-1");
        String workingDirectory = FileUtils.getTempDirectory().getAbsolutePath();
        AgentRuntimeInfo runtimeInfo = ElasticAgentRuntimeInfo.fromAgent(identifier, AgentRuntimeStatus.Idle, workingDirectory, "20.3.0-1234", "20.5.0-2345");

        assertThat(runtimeInfo.getAgentVersion(), is("20.5.0-2345"));
        assertThat(runtimeInfo.getAgentBootstrapperVersion(), is("20.3.0-1234"));
    }
}
