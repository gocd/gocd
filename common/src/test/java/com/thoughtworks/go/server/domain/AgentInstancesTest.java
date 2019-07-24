/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.config.Agents;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.AgentStatus;
import com.thoughtworks.go.domain.NullAgentInstance;
import com.thoughtworks.go.domain.exception.MaxPendingAgentsLimitReachedException;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.helper.AgentMother;
import com.thoughtworks.go.listener.AgentStatusChangeListener;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Date;
import java.util.List;

import static com.thoughtworks.go.domain.AgentInstance.FilterBy.*;
import static com.thoughtworks.go.helper.AgentInstanceMother.*;
import static com.thoughtworks.go.server.service.AgentRuntimeInfo.fromServer;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class AgentInstancesTest {
    private AgentInstance idle;
    private AgentInstance building;
    private AgentInstance pending;
    private AgentInstance disabled;
    private AgentInstance nullInstance;
    private AgentInstance elastic;

    @Mock
    private SystemEnvironment systemEnvironment;

    private AgentStatusChangeListener listener;

    @BeforeEach
    void setUp() {
        initMocks(this);
        idle = idle(new Date(), "CCeDev01", systemEnvironment);

        AgentInstanceMother.updateOS(idle, "linux");
        building = building("buildLocator", systemEnvironment);

        AgentInstanceMother.updateOS(building, "macOS");
        pending = pending(systemEnvironment);

        AgentInstanceMother.updateOS(pending, "windows");
        disabled = disabled("10.18.5.4", systemEnvironment);

        nullInstance = AgentInstanceMother.nullInstance();

        elastic = AgentInstance.createFromAgent(AgentMother.elasticAgent(), new SystemEnvironment(), null);

        listener = mock(AgentStatusChangeListener.class);
    }

    @Nested
    class Find {
        @Test
        void shouldFindEnabledAgents() {
            AgentInstances agentInstances = sample();

            AgentInstances enabledAgents = agentInstances.findEnabledAgents();
            assertThat(enabledAgents.size(), is(3));
            assertThat(enabledAgents.findAgentAndRefreshStatus("uuid2"), is(idle));
            assertThat(enabledAgents.findAgentAndRefreshStatus("uuid3"), is(building));
            assertThat(enabledAgents.findAgentAndRefreshStatus(elastic.getUuid()), is(elastic));
        }

        @Test
        void shouldFindPendingAgents() {
            AgentInstances agentInstances = sample();

            List<Agent> pendingAgents = agentInstances.findPendingAgents(asList(idle.getUuid(),
                    pending.getUuid(),
                    building.getUuid(),
                    disabled.getUuid()));
            assertThat(pendingAgents.size(), is(1));
            assertThat(pendingAgents.get(0).getUuid(), is(pending.getUuid()));
        }

        @Test
        void shouldFindPendingAgentUUIDs() {
            AgentInstances agentInstances = sample();

            List<String> pendingAgentUUIDs = agentInstances.filterBy(asList(idle.getUuid(), pending.getUuid(), building.getUuid(), disabled.getUuid()), Pending);
            assertThat(pendingAgentUUIDs.size(), is(1));
            assertThat(pendingAgentUUIDs.get(0), is(pending.getUuid()));
        }

        @Test
        void shouldFindNullAgentUUIDs() {
            AgentInstances agentInstances = sample();
            agentInstances.add(nullInstance);

            List<String> nullAgentUUIDs = agentInstances.filterBy(asList(idle.getUuid(), pending.getUuid(), nullInstance.getUuid()), Null);
            assertThat(nullAgentUUIDs.size(), is(1));
            assertThat(nullAgentUUIDs.get(0), is(nullInstance.getUuid()));
        }

        @Test
        void shouldFindElasticAgentUUIDs() {
            AgentInstances agentInstances = sample();

            List<String> nullAgentUUIDs = agentInstances.filterBy(asList(idle.getUuid(), pending.getUuid(), elastic.getUuid()), Elastic);
            assertThat(nullAgentUUIDs.size(), is(1));
            assertThat(nullAgentUUIDs.get(0), is(elastic.getUuid()));
        }

        @Test
        void shouldFindRegisteredAgents() {
            AgentInstances agentInstances = sample();

            AgentInstances agents = agentInstances.findRegisteredAgents();
            assertThat(agents.size(), is(4));
            assertThat(agents.findAgentAndRefreshStatus("uuid2"), is(idle));
            assertThat(agents.findAgentAndRefreshStatus("uuid3"), is(building));
            assertThat(agents.findAgentAndRefreshStatus("uuid5"), is(disabled));
            assertThat(agents.findAgentAndRefreshStatus(elastic.getUuid()), is(elastic));
        }

        @Test
        void shouldFindAgentsByItHostName() {
            AgentInstance idle = idle(new Date(), "ghost-name");
            AgentInstances agentInstances = new AgentInstances(systemEnvironment, listener, idle, building());

            AgentInstance byHostname = agentInstances.findFirstByHostname("ghost-name");
            assertThat(byHostname, is(idle));
        }
    }

    @Test
    void shouldUnderstandFilteringAgentListBasedOnUuid() {
        AgentInstances instances = new AgentInstances(mock(AgentStatusChangeListener.class));

        AgentRuntimeInfo agent1 = fromServer(new Agent("uuid-1", "host-1", "192.168.1.2"), true, "/foo/bar", 100l, "linux", false);
        AgentRuntimeInfo agent2 = fromServer(new Agent("uuid-2", "host-2", "192.168.1.3"), true, "/bar/baz", 200l, "linux", false);
        AgentRuntimeInfo agent3 = fromServer(new Agent("uuid-3", "host-3", "192.168.1.4"), true, "/baz/quux", 300l, "linux", false);

        AgentInstance instance1 = AgentInstance.createFromLiveAgent(agent1, systemEnvironment, mock(AgentStatusChangeListener.class));
        instances.add(instance1);
        instances.add(AgentInstance.createFromLiveAgent(agent2, systemEnvironment, mock(AgentStatusChangeListener.class)));
        AgentInstance instance3 = AgentInstance.createFromLiveAgent(agent3, systemEnvironment, mock(AgentStatusChangeListener.class));
        instances.add(instance3);

        List<AgentInstance> agents = instances.filter(asList("uuid-1", "uuid-3"));

        assertThat(agents, hasItems(instance1, instance3));
        assertThat(agents.size(), is(2));
    }


    @Test
    void shouldReturnNullAgentsWhenHostNameIsNotFound() {
        AgentInstances agentInstances = new AgentInstances(systemEnvironment, listener, building());
        agentInstances.add(idle);
        agentInstances.add(building);

        AgentInstance byHostname = agentInstances.findFirstByHostname("not-exist");
        assertThat(byHostname, is(instanceOf(NullAgentInstance.class)));
    }

    @Test
    void shouldReturnFirstMatchedAgentsWhenHostNameHasMoreThanOneMatch() {
        AgentInstance agent = AgentInstance.createFromAgent(new Agent("uuid20", "CCeDev01", "10.18.5.20"), systemEnvironment, null);
        AgentInstance duplicatedAgent = AgentInstance.createFromAgent(new Agent("uuid21", "CCeDev01", "10.18.5.20"), systemEnvironment, null);
        AgentInstances agentInstances = new AgentInstances(systemEnvironment, listener, agent, duplicatedAgent);

        AgentInstance byHostname = agentInstances.findFirstByHostname("CCeDev01");
        assertThat(byHostname, is(agent));
    }

    @Test
    void shouldAddAgentIntoMemoryAfterAgentIsManuallyAddedInConfigFile() {
        AgentInstances agentInstances = new AgentInstances(mock(AgentStatusChangeListener.class));
        Agent agent = new Agent("uuid20", "CCeDev01", "10.18.5.20");
        agentInstances.sync(new Agents(agent));

        assertThat(agentInstances.size(), is(1));
        assertThat(agentInstances.findAgentAndRefreshStatus("uuid20").getAgent(), is(agent));
    }

    @Test
    void shouldRemoveAgentWhenAgentIsRemovedFromConfigFile() {
        AgentInstances agentInstances = new AgentInstances(systemEnvironment, listener, idle, building);

        Agents oneAgentIsRemoved = new Agents(new Agent("uuid2", "CCeDev01", "10.18.5.1"));

        agentInstances.sync(oneAgentIsRemoved);
        assertThat(agentInstances.size(), is(1));
        assertThat(agentInstances.findAgentAndRefreshStatus("uuid2"), is(idle));
        assertThat(agentInstances.findAgentAndRefreshStatus("uuid1"), is(new NullAgentInstance("uuid1")));
    }

    @Test
    void shouldSyncAgent() {
        AgentInstances agentInstances = new AgentInstances(systemEnvironment, listener, building(), idle);

        Agent agent = new Agent("uuid2", "CCeDev01", "10.18.5.1");
        agent.setDisabled(true);
        Agents oneAgentIsRemoved = new Agents(agent);

        agentInstances.sync(oneAgentIsRemoved);

        assertThat(agentInstances.findAgentAndRefreshStatus("uuid2").getStatus(), is(AgentStatus.Disabled));
    }

    @Test
    void shouldNotRemovePendingAgentDuringSync() {
        AgentInstances agentInstances = new AgentInstances(systemEnvironment, listener, building());
        agentInstances.add(pending);
        Agents agents = new Agents();

        agentInstances.sync(agents);

        assertThat(agentInstances.size(), is(1));
        assertThat(agentInstances.findAgentAndRefreshStatus("uuid4").getStatus(), is(AgentStatus.Pending));
    }

    @Test
    void agentHostnameShouldBeUnique() {
        Agent agent = new Agent("uuid2", "CCeDev01", "10.18.5.1");
        AgentInstances agentInstances = new AgentInstances(mock(AgentStatusChangeListener.class));
        agentInstances.register(fromServer(agent, false, "/var/lib", 0L, "linux"));
        agentInstances.register(fromServer(agent, false, "/var/lib", 0L, "linux"));
    }

    @Test
    void registerShouldErrorOutIfMaxPendingAgentsLimitIsReached() {
        Agent agent = new Agent("uuid2", "CCeDev01", "10.18.5.1");
        AgentInstances agentInstances = new AgentInstances(systemEnvironment, listener, pending());
        when(systemEnvironment.get(SystemEnvironment.MAX_PENDING_AGENTS_ALLOWED)).thenReturn(1);

        assertThrows(MaxPendingAgentsLimitReachedException.class,
                () -> agentInstances.register(fromServer(agent, false, "/var/lib",
                        0L, "linux")));
    }

    @Test
    void shouldRemovePendingAgentThatIsTimedOut() {
        when(systemEnvironment.getAgentConnectionTimeout()).thenReturn(-1);
        AgentInstances agentInstances = new AgentInstances(systemEnvironment, listener, pending, building, disabled);
        agentInstances.refresh();
        assertThat(agentInstances.findAgentAndRefreshStatus("uuid4"), is(instanceOf(NullAgentInstance.class)));
    }

    @Test
    void shouldSupportConcurrentOperations() {
        final AgentInstances agentInstances = new AgentInstances(mock(AgentStatusChangeListener.class));

        // register 100 agents
        for (int i = 0; i < 100; i++) {
            Agent agent = new Agent("uuid" + i, "CCeDev_" + i, "10.18.5." + i);
            agentInstances.register(fromServer(agent, false, "/var/lib", Long.MAX_VALUE, "linux"));
        }

        Agent agent = new Agent("uuid" + 200, "CCeDev_" + 200, "10.18.5." + 200);

        MaxPendingAgentsLimitReachedException e
                = assertThrows(MaxPendingAgentsLimitReachedException.class,
                () -> agentInstances.register(fromServer(agent, false, "/var/lib",
                        Long.MAX_VALUE, "linux")));
        assertThat(e.getMessage(), is("Max pending agents allowed 100, limit reached"));
    }

    private AgentInstances sample() {
        AgentInstances agentInstances = new AgentInstances(null);

        agentInstances.add(idle);
        agentInstances.add(building);
        agentInstances.add(pending);
        agentInstances.add(disabled);
        agentInstances.add(elastic);

        return agentInstances;
    }

}
