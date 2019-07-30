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
import static com.thoughtworks.go.domain.AgentInstance.createFromAgent;
import static com.thoughtworks.go.domain.AgentInstance.createFromLiveAgent;
import static com.thoughtworks.go.helper.AgentInstanceMother.*;
import static com.thoughtworks.go.server.service.AgentRuntimeInfo.fromServer;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        elastic = createFromAgent(AgentMother.elasticAgent(), new SystemEnvironment(), null);

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
    }

    @Nested
    class FindFirstByHostName{
        @Test
        void findFirstByHostnameShouldReturnAgentInstanceMatchingSpecifiedHostname() {
            AgentInstance idle = idle(new Date(), "ghost-name");
            AgentInstances agentInstances = new AgentInstances(systemEnvironment, listener, idle, building());
            assertThat(agentInstances.findFirstByHostname("ghost-name"), is(idle));
        }

        @Test
        void findFirstByHostnameShouldReturnNullAgentInstanceWhenHostnameIsNotFound() {
            AgentInstances agentInstances = new AgentInstances(systemEnvironment, listener, building());
            agentInstances.add(idle);
            agentInstances.add(building);

            AgentInstance byHostname = agentInstances.findFirstByHostname("this-hsotname-does-not-exist");
            assertThat(byHostname, is(instanceOf(NullAgentInstance.class)));
        }

        @Test
        void findFirstByHostnameShouldReturnFirstMatchedAgentInstanceWhenHostnameHasMoreThanOneMatch() {
            String hostname = "CCeDev01";
            AgentInstance agentInstance = createFromAgent(new Agent("uuid20", hostname, "10.18.5.20"), systemEnvironment, null);
            AgentInstance duplicateAgentInstance = createFromAgent(new Agent("uuid21", hostname, "10.18.5.20"), systemEnvironment, null);
            AgentInstances agentInstances = new AgentInstances(systemEnvironment, listener, agentInstance, duplicateAgentInstance);

            assertThat(agentInstances.findFirstByHostname(hostname), is(agentInstance));
        }
    }

    @Nested
    class Filter {
        @Test
        void shouldFilterAgentInstancesBasedOnListOfUUIDs() {
            AgentInstances instances = new AgentInstances(mock(AgentStatusChangeListener.class));

            Agent agent1 = new Agent("uuid-1", "host-1", "192.168.1.2");
            Agent agent2 = new Agent("uuid-2", "host-2", "192.168.1.3");
            Agent agent3 = new Agent("uuid-3", "host-3", "192.168.1.4");

            AgentRuntimeInfo runtime1 = fromServer(agent1, true, "/foo/bar", 100L, "linux", false);
            AgentRuntimeInfo runtime2 = fromServer(agent2, true, "/bar/baz", 200L, "linux", false);
            AgentRuntimeInfo runtime3 = fromServer(agent3, true, "/baz/quux", 300L, "linux", false);

            AgentInstance instance1 = createFromLiveAgent(runtime1, systemEnvironment, mock(AgentStatusChangeListener.class));
            AgentInstance instance2 = createFromLiveAgent(runtime2, systemEnvironment, mock(AgentStatusChangeListener.class));
            AgentInstance instance3 = createFromLiveAgent(runtime3, systemEnvironment, mock(AgentStatusChangeListener.class));

            instances.add(instance1);
            instances.add(instance2);
            instances.add(instance3);

            List<String> uuids = asList("uuid-1", "uuid-3");
            List<AgentInstance> filteredInstances = instances.filter(uuids);

            assertThat(filteredInstances, hasItems(instance1, instance3));
            assertThat(filteredInstances.size(), is(2));
        }

        @Test
        void shouldFilterAgentInstancesBasedOnNullOrEmptyListOfUUIDs() {
            AgentStatusChangeListener mockListener = mock(AgentStatusChangeListener.class);
            AgentInstances instances = new AgentInstances(mockListener);

            Agent agent1 = new Agent("uuid-1", "host-1", "192.168.1.2");
            Agent agent2 = new Agent("uuid-2", "host-2", "192.168.1.3");
            Agent agent3 = new Agent("uuid-3", "host-3", "192.168.1.4");

            AgentRuntimeInfo runtime1 = fromServer(agent1, true, "/foo/bar", 100L, "linux", false);
            AgentRuntimeInfo runtime2 = fromServer(agent2, true, "/bar/baz", 200L, "linux", false);
            AgentRuntimeInfo runtime3 = fromServer(agent3, true, "/baz/quux", 300L, "linux", false);

            AgentInstance instance1 = createFromLiveAgent(runtime1, systemEnvironment, mockListener);
            AgentInstance instance2 = createFromLiveAgent(runtime2, systemEnvironment, mockListener);
            AgentInstance instance3 = createFromLiveAgent(runtime3, systemEnvironment, mockListener);

            instances.add(instance1);
            instances.add(instance2);
            instances.add(instance3);

            List<AgentInstance> filteredInstances = instances.filter(emptyList());
            assertThat(filteredInstances.size(), is(0));

            filteredInstances = instances.filter(null);
            assertThat(filteredInstances.size(), is(0));
        }
    }

    @Test
    void shouldSyncAgentInstancesFromAgentsInDB() {
        AgentStatusChangeListener mockListener = mock(AgentStatusChangeListener.class);
        AgentInstances agentInstances = new AgentInstances(mockListener);

        String uuid = "uuid1";
        Agent agentInMemory = new Agent(uuid, "originalHostname", "10.18.5.20");
        agentInstances.add(createFromAgent(agentInMemory, systemEnvironment, mockListener));

        Agent agentFromDB = new Agent(uuid, "updatedHostname", "10.10.5.20");
        agentInstances.syncAgentInstancesFrom(new Agents(agentFromDB));

        assertThat(agentInstances.size(), is(1));
        assertThat(agentInstances.findAgentAndRefreshStatus(uuid).getAgent(), is(agentFromDB));
    }

    @Test
    void shouldBeAbleToCreateAgentInstancesWithNullArrayOfAgentInstance() {
        AgentInstances agentInstances = new AgentInstances(systemEnvironment, mock(AgentStatusChangeListener.class), null);

        assertThat(agentInstances, is(not(nullValue())));
        assertThat(agentInstances.size(), is(0));
    }

    @Test
    void shouldSyncAgentInstancesFromAgentsInDBWhenAgentIsRemovedFromDB() {
        AgentInstances agentInstancesWithIdleAndBuildingAgents = new AgentInstances(systemEnvironment, listener, idle, building);
        assertThat(agentInstancesWithIdleAndBuildingAgents.size(), is(2));

        Agents agentListFromDBWithIdleAgentRemoved = new Agents(building.getAgent());
        agentInstancesWithIdleAndBuildingAgents.syncAgentInstancesFrom(agentListFromDBWithIdleAgentRemoved);

        assertThat(agentInstancesWithIdleAndBuildingAgents.size(), is(1));
        assertThat(agentInstancesWithIdleAndBuildingAgents.findAgentAndRefreshStatus(building.getUuid()), is(building));
        assertThat(agentInstancesWithIdleAndBuildingAgents.findAgentAndRefreshStatus(idle.getUuid()), is(new NullAgentInstance(idle.getUuid())));
    }

    @Test
    void shouldSyncAgentInstancesFromAgentsInDBWhenNewAgentIsCreatedInDB() {
        AgentInstances agentInstances = new AgentInstances(systemEnvironment, listener, idle, building);
        assertThat(agentInstances.size(), is(2));

        String newUUID = "new-newAgentInDB-id";
        Agent newAgentInDB = new Agent(newUUID, "CCeDev01", "10.18.5.1");
        newAgentInDB.setDisabled(true);
        Agents agentsFromDBWithNewAgent = new Agents(idle.getAgent(), building.getAgent(), newAgentInDB);

        agentInstances.syncAgentInstancesFrom(agentsFromDBWithNewAgent);
        assertThat(agentInstances.size(), is(3));
        assertThat(agentInstances.findAgentAndRefreshStatus(newUUID).getStatus(), is(AgentStatus.Disabled));
    }

    @Test
    void shouldNotRemovePendingAgentDuringSync() {
        AgentInstances agentInstances = new AgentInstances(systemEnvironment, listener, building());
        agentInstances.add(pending);
        Agents agents = new Agents();

        agentInstances.syncAgentInstancesFrom(agents);

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
