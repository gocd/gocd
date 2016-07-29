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

package com.thoughtworks.go.server.domain;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.Agents;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.AgentStatus;
import com.thoughtworks.go.domain.NullAgentInstance;
import com.thoughtworks.go.domain.exception.MaxPendingAgentsLimitReachedException;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class AgentInstancesTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private AgentInstance idle;
    private AgentInstance building;
    private AgentInstance pending;
    private AgentInstance disabled;
    private AgentInstance local;
    @Mock
    private SystemEnvironment systemEnvironment;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        idle = AgentInstanceMother.idle(new Date(), "CCeDev01", systemEnvironment);
        AgentInstanceMother.updateOS(idle, "linux");
        building = AgentInstanceMother.building("buildLocator", systemEnvironment);
        AgentInstanceMother.updateOS(building, "macOS");
        pending = AgentInstanceMother.pending(systemEnvironment);
        AgentInstanceMother.updateOS(pending, "windows");
        disabled = AgentInstanceMother.disabled("10.18.5.4", systemEnvironment);
        local = AgentInstanceMother.local(systemEnvironment);
    }

    @Test
    public void shouldUnderstandFilteringAgentListBasedOnUuid() {
        AgentInstances instances = new AgentInstances(null);

        AgentRuntimeInfo agent1 = AgentRuntimeInfo.fromServer(new AgentConfig("uuid-1", "host-1", "192.168.1.2"), true, "/foo/bar", 100l, "linux", false);
        AgentRuntimeInfo agent2 = AgentRuntimeInfo.fromServer(new AgentConfig("uuid-2", "host-2", "192.168.1.3"), true, "/bar/baz", 200l, "linux", false);
        AgentRuntimeInfo agent3 = AgentRuntimeInfo.fromServer(new AgentConfig("uuid-3", "host-3", "192.168.1.4"), true, "/baz/quux", 300l, "linux", false);

        AgentInstance instance1 = AgentInstance.createFromLiveAgent(agent1, systemEnvironment);
        instances.add(instance1);
        instances.add(AgentInstance.createFromLiveAgent(agent2, systemEnvironment));
        AgentInstance instance3 = AgentInstance.createFromLiveAgent(agent3, systemEnvironment);
        instances.add(instance3);

        List<AgentInstance> agents = instances.filter(Arrays.asList("uuid-1", "uuid-3"));

        assertThat(agents, hasItems(instance1, instance3));
        assertThat(agents.size(), is(2));
    }

    @Test
    public void shouldFindEnabledAgents() {
        AgentInstances agentInstances = sample();

        AgentInstances enabledAgents = agentInstances.findEnabledAgents();
        assertThat(enabledAgents.size(), is(2));
        assertThat(enabledAgents.findAgentAndRefreshStatus("uuid2"), is(idle));
        assertThat(enabledAgents.findAgentAndRefreshStatus("uuid3"), is(building));
    }

    @Test
    public void shouldFindRegisteredAgents() {
        AgentInstances agentInstances = sample();

        AgentInstances agents = agentInstances.findRegisteredAgents();
        assertThat(agents.size(), is(3));
        assertThat(agents.findAgentAndRefreshStatus("uuid2"), is(idle));
        assertThat(agents.findAgentAndRefreshStatus("uuid3"), is(building));
        assertThat(agents.findAgentAndRefreshStatus("uuid5"), is(disabled));
    }

    @Test
    public void shouldFindAgentsByItHostName() throws Exception {
        AgentInstance idle = AgentInstanceMother.idle(new Date(), "ghost-name");
        AgentInstances agentInstances = new AgentInstances(null, systemEnvironment, idle, AgentInstanceMother.building());

        AgentInstance byHostname = agentInstances.findFirstByHostname("ghost-name");
        assertThat(byHostname, is(idle));
    }

    @Test
    public void shouldReturnNullAgentsWhenHostNameIsNotFound() throws Exception {
        AgentInstances agentInstances = new AgentInstances(null, systemEnvironment, AgentInstanceMother.building());
        agentInstances.add(idle);
        agentInstances.add(building);

        AgentInstance byHostname = agentInstances.findFirstByHostname("not-exist");
        assertThat(byHostname, is(instanceOf(NullAgentInstance.class)));
    }

    @Test
    public void shouldReturnFirstMatchedAgentsWhenHostNameHasMoreThanOneMatch() throws Exception {
        AgentInstance agent = AgentInstance.createFromConfig(new AgentConfig("uuid20", "CCeDev01", "10.18.5.20"), systemEnvironment);
        AgentInstance duplicatedAgent = AgentInstance.createFromConfig(new AgentConfig("uuid21", "CCeDev01", "10.18.5.20"), systemEnvironment);
        AgentInstances agentInstances = new AgentInstances(null, systemEnvironment, agent, duplicatedAgent);

        AgentInstance byHostname = agentInstances.findFirstByHostname("CCeDev01");
        assertThat(byHostname, is(agent));
    }

    @Test
    public void shouldAddAgentIntoMemoryAfterAgentIsManuallyAddedInConfigFile() throws Exception {
        AgentInstances agentInstances = new AgentInstances(null);
        AgentConfig agentConfig = new AgentConfig("uuid20", "CCeDev01", "10.18.5.20");
        agentInstances.sync(new Agents(agentConfig));

        assertThat(agentInstances.size(), is(1));
        assertThat(agentInstances.findAgentAndRefreshStatus("uuid20").agentConfig(), is(agentConfig));
    }

    @Test
    public void shouldRemoveAgentWhenAgentIsRemovedFromConfigFile() throws Exception {
        AgentInstances agentInstances = new AgentInstances(null, systemEnvironment, idle, building);

        Agents oneAgentIsRemoved = new Agents(new AgentConfig("uuid2", "CCeDev01", "10.18.5.1"));

        agentInstances.sync(oneAgentIsRemoved);
        assertThat(agentInstances.size(), is(1));
        assertThat(agentInstances.findAgentAndRefreshStatus("uuid2"), is(idle));
        assertThat(agentInstances.findAgentAndRefreshStatus("uuid1"), is((AgentInstance) new NullAgentInstance("uuid1")));
    }

    @Test
    public void shouldSyncAgent() throws Exception {
        AgentInstances agentInstances = new AgentInstances(null, systemEnvironment, AgentInstanceMother.building(), idle);

        AgentConfig agentConfig = new AgentConfig("uuid2", "CCeDev01", "10.18.5.1");
        agentConfig.setDisabled(true);
        Agents oneAgentIsRemoved = new Agents(agentConfig);

        agentInstances.sync(oneAgentIsRemoved);

        assertThat(agentInstances.findAgentAndRefreshStatus("uuid2").getStatus(), is(AgentStatus.Disabled));
    }

    @Test
    public void shouldNotRemovePendingAgentDuringSync() throws Exception {
        AgentInstances agentInstances = new AgentInstances(null, systemEnvironment, AgentInstanceMother.building());
        agentInstances.add(pending);
        Agents agents = new Agents();

        agentInstances.sync(agents);

        assertThat(agentInstances.size(), is(1));
        assertThat(agentInstances.findAgentAndRefreshStatus("uuid4").getStatus(), is(AgentStatus.Pending));
    }

    @Test
    public void agentHostnameShouldBeUnique() {
        AgentConfig agentConfig = new AgentConfig("uuid2", "CCeDev01", "10.18.5.1");
        AgentInstances agentInstances = new AgentInstances(null);
        agentInstances.register(AgentRuntimeInfo.fromServer(agentConfig, false, "/var/lib", 0L, "linux", false));
        agentInstances.register(AgentRuntimeInfo.fromServer(agentConfig, false, "/var/lib", 0L, "linux", false));
    }

    @Test(expected = MaxPendingAgentsLimitReachedException.class)
    public void registerShouldErrorOutIfMaxPendingAgentsLimitIsReached() {
        AgentConfig agentConfig = new AgentConfig("uuid2", "CCeDev01", "10.18.5.1");
        AgentInstances agentInstances = new AgentInstances(null, systemEnvironment, AgentInstanceMother.pending());
        when(systemEnvironment.get(SystemEnvironment.MAX_PENDING_AGENTS_ALLOWED)).thenReturn(1);

        agentInstances.register(AgentRuntimeInfo.fromServer(agentConfig, false, "/var/lib", 0L, "linux", false));
    }

    @Test
    public void shouldRemovePendingAgentThatIsTimedOut() {
        when(systemEnvironment.getAgentConnectionTimeout()).thenReturn(-1);
        AgentInstances agentInstances = new AgentInstances(null, systemEnvironment, pending, building, disabled);
        agentInstances.refresh();
        assertThat(agentInstances.findAgentAndRefreshStatus("uuid4"), is(instanceOf(NullAgentInstance.class)));
    }

    @Test
    public void shouldSupportConcurrentOperations() throws Exception {
        final AgentInstances agentInstances = new AgentInstances(null);

        // register 100 agents
        for (int i = 0; i < 100; i++) {
            AgentConfig agentConfig = new AgentConfig("uuid" + i, "CCeDev_" + i, "10.18.5." + i);
            agentInstances.register(AgentRuntimeInfo.fromServer(agentConfig, false, "/var/lib", Long.MAX_VALUE, "linux", false));
        }

        thrown.expect(MaxPendingAgentsLimitReachedException.class);
        thrown.expectMessage("Max pending agents allowed 100, limit reached");
        AgentConfig agentConfig = new AgentConfig("uuid" + 200, "CCeDev_" + 200, "10.18.5." + 200);
        agentInstances.register(AgentRuntimeInfo.fromServer(agentConfig, false, "/var/lib", Long.MAX_VALUE, "linux", false));
    }

    @Test
    public void shouldReturnAllAgentHostnames() {
        AgentInstances agentInstances = sample();
        Set<String> names = agentInstances.getAllHostNames();
        assertThat(names, hasItems("CCeDev01", "CCeDev03", "CCeDev04"));
    }

    @Test
    public void shouldReturnAllAgentIPAddresses() {
        AgentInstances agentInstances = sample();

        Set<String> names = agentInstances.getAllIpAddresses();
        assertThat(names, hasItems("10.18.5.1", "10.18.5.3", "10.18.5.4"));
    }

    @Test
    public void shouldReturnAllAgentOperatingSystems() {
        AgentInstances agentInstances = sample();

        Set<String> names = agentInstances.getAllOperatingSystems();
        assertThat(names, hasItems("linux", "macOS", "windows"));
    }

    private AgentInstances sample() {
        AgentInstances agentInstances = new AgentInstances(null);
        agentInstances.add(idle);
        agentInstances.add(building);
        agentInstances.add(pending);
        agentInstances.add(disabled);
        return agentInstances;
    }

    private static class AgentAdder implements Runnable {
        private final AgentInstances agentInstances;
        private boolean stop;

        public static AgentAdder startAdding(AgentInstances agentInstances) {
            AgentAdder agentAdder = new AgentAdder(agentInstances);
            Thread thread = new Thread(agentAdder);
            thread.setDaemon(true);
            thread.start();
            return agentAdder;
        }

        private AgentAdder(AgentInstances agentInstances) {
            this.agentInstances = agentInstances;
        }

        public void run() {
            int count = 0;
            while (!stop) {
                AgentConfig agentConfig = new AgentConfig("uuid" + count, "CCeDev_" + count, "10.18.5." + count);
                agentInstances.register(AgentRuntimeInfo.fromServer(agentConfig, false, "/var/lib", Long.MAX_VALUE, "linux", false));
                count++;
            }
        }

        public void stop() {
            this.stop = true;
        }
    }

}
