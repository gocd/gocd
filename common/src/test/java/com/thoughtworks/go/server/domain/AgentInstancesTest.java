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
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.service.AgentBuildingInfo;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.ElasticAgentRuntimeInfo;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.util.LinkedMultiValueMap;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.thoughtworks.go.domain.AgentInstance.FilterBy.*;
import static com.thoughtworks.go.domain.AgentInstance.createFromAgent;
import static com.thoughtworks.go.domain.AgentInstance.createFromLiveAgent;
import static com.thoughtworks.go.domain.AgentRuntimeStatus.*;
import static com.thoughtworks.go.helper.AgentInstanceMother.*;
import static com.thoughtworks.go.server.service.AgentRuntimeInfo.fromServer;
import static com.thoughtworks.go.util.SystemEnvironment.MAX_PENDING_AGENTS_ALLOWED;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
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
        void shouldFindAgentByUUID() {
            AgentInstances agentInstances = createAgentInstancesWithAgentInstanceInVariousState();
            AgentInstance idleInstance = agentInstances.findAgent(idle.getUuid());
            assertThat(idleInstance, is(idle));
        }

        @Test
        void findAgentAndRefreshStatusShouldNotRefreshStatusForPendingAgent() {
            AgentInstances agentInstances = createAgentInstancesWithAgentInstanceInVariousState();
            assertThat(pending.getRuntimeStatus(), is(Unknown));

            AgentInstance pendingInstance = agentInstances.findAgentAndRefreshStatus(pending.getUuid());
            assertThat(pendingInstance.getRuntimeStatus(), is(Unknown));
        }

        @Test
        void findAgentAndRefreshStatusShouldRefreshStatusForAnyAgentOtherThanPending() {
            AgentInstance instance = createFromAgent(building.getAgent(), systemEnvironment, mock(AgentStatusChangeListener.class));
            AgentInstances instances = new AgentInstances(systemEnvironment, mock(AgentStatusChangeListener.class), instance);
            assertThat(instance.getRuntimeStatus(), is(Missing));

            AgentInstance refreshedInstance = instances.findAgentAndRefreshStatus(instance.getUuid());
            assertThat(refreshedInstance.getRuntimeStatus(), is(LostContact));
        }

        @Test
        void shouldFindPendingAgents() {
            AgentInstances agentInstances = createAgentInstancesWithAgentInstanceInVariousState();

            List<Agent> pendingAgents = agentInstances.filterPendingAgents(asList(idle.getUuid(),
                    pending.getUuid(),
                    building.getUuid(),
                    disabled.getUuid()));
            assertThat(pendingAgents.size(), is(1));
            assertThat(pendingAgents.get(0).getUuid(), is(pending.getUuid()));
        }

        @Test
        void shouldReturnEmptyListWhenNullOrEmptyListOfUUIDsAreSpecifiedInFilterPendingAgents() {
            AgentInstances agentInstances = createAgentInstancesWithAgentInstanceInVariousState();
            List<Agent> pendingAgents = agentInstances.filterPendingAgents(emptyList());
            assertThat(pendingAgents.size(), is(0));

            pendingAgents = agentInstances.filterPendingAgents(null);
            assertThat(pendingAgents.size(), is(0));
        }

        @Test
        void shouldReturnEmptyListWhenNullOrEmptyUUIDsAreSpecifiedAsElementsInTheListOfUUIDsInFilterPendingAgents() {
            AgentInstances agentInstances = createAgentInstancesWithAgentInstanceInVariousState();
            List<Agent> pendingAgents = agentInstances.filterPendingAgents(asList(null, null, "  "));
            assertThat(pendingAgents.size(), is(0));
        }

        @Test
        void shouldFindPendingAgentUUIDs() {
            AgentInstances agentInstances = createAgentInstancesWithAgentInstanceInVariousState();

            List<String> pendingAgentUUIDs = agentInstances.filterBy(asList(idle.getUuid(), pending.getUuid(), building.getUuid(), disabled.getUuid()), Pending);
            assertThat(pendingAgentUUIDs.size(), is(1));
            assertThat(pendingAgentUUIDs.get(0), is(pending.getUuid()));
        }

        @Test
        void shouldFindNullAgentUUIDs() {
            AgentInstances agentInstances = createAgentInstancesWithAgentInstanceInVariousState();
            agentInstances.add(nullInstance);

            List<String> nullAgentUUIDs = agentInstances.filterBy(asList(idle.getUuid(), pending.getUuid(), nullInstance.getUuid()), Null);
            assertThat(nullAgentUUIDs.size(), is(1));
            assertThat(nullAgentUUIDs.get(0), is(nullInstance.getUuid()));
        }

        @Test
        void shouldFindEmptyListOfPendingUUIDsWhenInputListOfUUIDsAreSpecifiedAsNullOrEmpty() {
            AgentInstances agentInstances = createAgentInstancesWithAgentInstanceInVariousState();

            List<String> pendingAgentUUIDs = agentInstances.filterBy(emptyList(), Pending);
            assertThat(pendingAgentUUIDs.size(), is(0));

            pendingAgentUUIDs = agentInstances.filterBy(null, Pending);
            assertThat(pendingAgentUUIDs.size(), is(0));
        }

        @Test
        void shouldFindRegisteredAgents() {
            AgentInstances agentInstances = createAgentInstancesWithAgentInstanceInVariousState();

            AgentInstances agents = agentInstances.findRegisteredAgents();
            assertThat(agents.size(), is(4));
            assertThat(agents.findAgentAndRefreshStatus("uuid2"), is(idle));
            assertThat(agents.findAgentAndRefreshStatus("uuid3"), is(building));
            assertThat(agents.findAgentAndRefreshStatus("uuid5"), is(disabled));
            assertThat(agents.findAgentAndRefreshStatus(elastic.getUuid()), is(elastic));
        }

        @Test
        void shouldReturnEmptyAgentInstancesWhenThereAreNoRegisteredAgents() {
            AgentInstances agentInstances = new AgentInstances(mock(AgentStatusChangeListener.class));

            AgentInstances registeredAgentInstances = agentInstances.findRegisteredAgents();
            assertThat(registeredAgentInstances.size(), is(0));
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

            AgentRuntimeInfo runtime1 = fromServer(agent1, true, "/foo/bar", 100L, "linux");
            AgentRuntimeInfo runtime2 = fromServer(agent2, true, "/bar/baz", 200L, "linux");
            AgentRuntimeInfo runtime3 = fromServer(agent3, true, "/baz/quux", 300L, "linux");

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

            AgentRuntimeInfo runtime1 = fromServer(agent1, true, "/foo/bar", 100L, "linux");
            AgentRuntimeInfo runtime2 = fromServer(agent2, true, "/bar/baz", 200L, "linux");
            AgentRuntimeInfo runtime3 = fromServer(agent3, true, "/baz/quux", 300L, "linux");

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

    @Nested
    class Sync{
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

            Agents emptyAgentListFromDB = new Agents();

            agentInstances.syncAgentInstancesFrom(emptyAgentListFromDB);

            assertThat(agentInstances.size(), is(1));
            assertThat(agentInstances.findAgentAndRefreshStatus(pending.getUuid()).getStatus(), is(AgentStatus.Pending));
        }
    }

    @Nested
    class CancelBuild{
        @Test
        void updateAgentAboutCancelledBuildShouldSetTheRuntimeStatusToCancelled() {
            AgentStatusChangeListener mockListener = mock(AgentStatusChangeListener.class);
            AgentInstance instance = createFromAgent(building.getAgent(), systemEnvironment, mockListener);

            AgentInstances agentInstances = new AgentInstances(systemEnvironment, mockListener, instance);
            assertThat(instance.getRuntimeStatus(), is(Missing));

            agentInstances.updateAgentAboutCancelledBuild(instance.getUuid(), true);
            assertThat(instance.getRuntimeStatus(), is(Cancelled));

            assertThat(agentInstances.findAgent(instance.getUuid()).getRuntimeStatus(), is(Cancelled));
        }

        @Test
        void updateAgentAboutCancelledBuildShouldDoNothingWhenUnknownUUIDIsSpecified() {
            AgentStatusChangeListener mockListener = mock(AgentStatusChangeListener.class);
            AgentInstance instance = createFromAgent(building.getAgent(), systemEnvironment, mockListener);
            AgentInstances agentInstances = new AgentInstances(systemEnvironment, mockListener, instance);
            assertThat(instance.getRuntimeStatus(), is(Missing));

            agentInstances.updateAgentAboutCancelledBuild("this-is-unknown-agent-uuid", true);
            assertThat(instance.getRuntimeStatus(), is(Missing));

            AgentInstance agentInstance = agentInstances.findAgent(instance.getUuid());
            assertThat(agentInstance.getRuntimeStatus(), is(Missing));
        }
    }

    @Nested
    class ElasticAgent{
        @Test
        void shouldFindElasticAgentUUIDs() {
            AgentInstances agentInstances = createAgentInstancesWithAgentInstanceInVariousState();

            List<String> nullAgentUUIDs = agentInstances.filterBy(asList(idle.getUuid(), pending.getUuid(), elastic.getUuid()), Elastic);
            assertThat(nullAgentUUIDs.size(), is(1));
            assertThat(nullAgentUUIDs.get(0), is(elastic.getUuid()));
        }

        @Test
        void shouldFindElasticAgentByElasticAgentIdAndElasticPluginId(){
            AgentInstances agentInstances = createAgentInstancesWithElasticAgents();

            String elasticAgentId = "elastic-agent-id-1";
            String elasticPluginId = "go.cd.elastic-agent-plugin.docker";
            AgentInstance elasticAgentInstance = agentInstances.findElasticAgent(elasticAgentId, elasticPluginId);

            assertThat(elasticAgentInstance, is(not(nullValue())));
            assertThat(elasticAgentInstance.getAgent().getElasticAgentId(), is(elasticAgentId));
            assertThat(elasticAgentInstance.getAgent().getElasticPluginId(), is(elasticPluginId));
        }

        @Test
        void shouldReturnNullAsElasticAgentWhenThereIsNoElasticAgentWithSpecifiedElasticAgentIdAndElasticPluginId(){
            AgentInstances agentInstances = createAgentInstancesWithElasticAgents();

            String elasticAgentId = "blabla";
            String elasticPluginId = "go.cd.elastic-agent-plugin.docker";
            AgentInstance elasticAgentInstance = agentInstances.findElasticAgent(elasticAgentId, elasticPluginId);

            assertThat(elasticAgentInstance, is(nullValue()));
        }

        @Test
        void shouldThrowExceptionWhenMoreThanOneElasticAgentWithSameElasticAgentIdAndElasticPluginId(){
            AgentInstances agentInstances = createAgentInstancesWithElasticAgents();
            agentInstances.add(createElasticAgentInstance(1, "go.cd.elastic-agent-plugin.docker"));

            String elasticAgentId = "elastic-agent-id-1";
            String elasticPluginId = "go.cd.elastic-agent-plugin.docker";

            IllegalStateException e = assertThrows(IllegalStateException.class, () -> agentInstances.findElasticAgent(elasticAgentId, elasticPluginId));
            assertTrue(e.getMessage().contains("Found multiple agents with the same elastic agent id"));
        }

        @Test
        void shouldReturnMapContainingAllElasticAgentsGroupedByElasticPluginIdKey(){
            AgentInstances agentInstances = createAgentInstancesWithElasticAgents();
            LinkedMultiValueMap<String, ElasticAgentMetadata> map = agentInstances.getAllElasticAgentsGroupedByPluginId();

            assertThat(map, is(not(nullValue())));
            assertThat(map.size(), is(2));

            String pluginId1 = "go.cd.elastic-agent-plugin.docker";
            String pluginId2 = "cd.go.contrib.elasticagent.kubernetes";

            assertTrue(map.containsKey(pluginId1));
            assertTrue(map.containsKey(pluginId2));

            assertThat(map.get(pluginId1).size(), is(3));
            assertThat(map.get(pluginId2).size(), is(2));
        }

        @Test
        void shouldReturnEmptyMapOfElasticAgentsGroupedByElasticPluginIdKeyWhenThereAreNoElasticAgents(){
            AgentInstances agentInstances = new AgentInstances(mock(AgentStatusChangeListener.class));
            LinkedMultiValueMap<String, ElasticAgentMetadata> map = agentInstances.getAllElasticAgentsGroupedByPluginId();
            assertThat(map, is(not(nullValue())));
            assertThat(map.size(), is(0));
        }
    }

    @Nested
    class agentsStuckInCancel {
        @Test
        void shouldListAgentsStuckInCancel() {
            AgentInstance stuckInCancel = mock(AgentInstance.class);
            AgentInstance building = mock(AgentInstance.class);

            when(stuckInCancel.getAgent()).thenReturn(new Agent("id1"));
            when(stuckInCancel.isStuckInCancel()).thenReturn(true);
            when(building.getAgent()).thenReturn(new Agent("id2"));
            when(building.isStuckInCancel()).thenReturn(false);

            AgentInstances agentInstances = new AgentInstances(null, null, stuckInCancel, building);
            List<AgentInstance> instances = agentInstances.agentsStuckInCancel();

            assertThat(instances.size(), is(1));
            assertThat(instances.contains(stuckInCancel), is(true));
        }
    }

    @Test
    void getAllAgentsShouldReturnAllAgentInstancesInMemoryCache() {
        AgentInstances agentInstances = createAgentInstancesWithAgentInstanceInVariousState();

        AgentInstances allAgents = agentInstances.getAllAgents();
        assertThat(allAgents.size(), is(5));

        agentInstances.add(createElasticAgentInstance(999, "go.cd.elastic-agent-plugin.docker"));

        allAgents = agentInstances.getAllAgents();
        assertThat(allAgents.size(), is(6));
    }

    @Test
    void shouldBeAbleToCreateAgentInstancesWithNullArrayOfAgentInstance() {
        AgentInstances agentInstances = new AgentInstances(systemEnvironment, mock(AgentStatusChangeListener.class), null);

        assertThat(agentInstances, is(not(nullValue())));
        assertThat(agentInstances.size(), is(0));
    }

    @Test
    void registerShouldThrowExceptionWhenMaxPendingAgentsLimitIsReached() {
        Agent agent = new Agent("uuid2", "CCeDev01", "10.18.5.1");
        AgentInstances agentInstances = new AgentInstances(systemEnvironment, listener, pending());
        when(systemEnvironment.get(MAX_PENDING_AGENTS_ALLOWED)).thenReturn(1);

        MaxPendingAgentsLimitReachedException e = assertThrows(MaxPendingAgentsLimitReachedException.class,
                () -> agentInstances.register(fromServer(agent, false, "/var/lib", 0L, "linux")));
        assertThat(e.getMessage(), is("Max pending agents allowed 1, limit reached"));
    }

    @Test
    void shouldRemoveTimedOutPendingAgentsOnRefresh() {
        when(systemEnvironment.getAgentConnectionTimeout()).thenReturn(-1);
        AgentInstances agentInstances = new AgentInstances(systemEnvironment, listener, pending, building, disabled);
        assertThat(agentInstances.getAllAgents().size(), is(3));
        agentInstances.refresh();
        assertThat(agentInstances.getAllAgents().size(), is(2));
        assertThat(agentInstances.findAgentAndRefreshStatus(pending.getUuid()), is(instanceOf(NullAgentInstance.class)));
    }

    @Test
    void buildingShouldRefreshAgentInstanceAndDelegateToBuildingMethodOfAgentInstance() {
        String uuid = "uuid";

        AgentInstances agentInstances = createAgentInstancesWithAgentInstanceInVariousState();
        AgentInstances agentInstancesSpy = Mockito.spy(agentInstances);
        AgentInstance mockAgentInstance = mock(AgentInstance.class);
        when(agentInstancesSpy.findAgentAndRefreshStatus(uuid)).thenReturn(mockAgentInstance);

        AgentBuildingInfo mockAgentBuildingInfo = mock(AgentBuildingInfo.class);
        doNothing().when(mockAgentInstance).building(mockAgentBuildingInfo);

        agentInstancesSpy.building(uuid, mockAgentBuildingInfo);
        verify(mockAgentInstance).building(mockAgentBuildingInfo);
    }

    private AgentInstances createAgentInstancesWithAgentInstanceInVariousState() {
        AgentInstances agentInstances = new AgentInstances(null);

        agentInstances.add(idle);
        agentInstances.add(building);
        agentInstances.add(pending);
        agentInstances.add(disabled);
        agentInstances.add(elastic);

        return agentInstances;
    }

    private AgentInstances createAgentInstancesWithElasticAgents() {
        AgentInstances agentInstances = new AgentInstances(null);
        String pluginId1 = "go.cd.elastic-agent-plugin.docker";
        String pluginId2 = "cd.go.contrib.elasticagent.kubernetes";

        AgentInstance elasticInstance1 = createElasticAgentInstance(1, pluginId1);
        AgentInstance elasticInstance2 = createElasticAgentInstance(2, pluginId1);
        AgentInstance elasticInstance3 = createElasticAgentInstance(3, pluginId1);

        AgentInstance elasticInstance4 = createElasticAgentInstance(4, pluginId2);
        AgentInstance elasticInstance5 = createElasticAgentInstance(5, pluginId2);

        agentInstances.add(elasticInstance1);
        agentInstances.add(elasticInstance2);
        agentInstances.add(elasticInstance3);
        agentInstances.add(elasticInstance4);
        agentInstances.add(elasticInstance5);

        return agentInstances;
    }

    private AgentInstance createElasticAgentInstance(int counter, String elasticPluginId){
        String uuid = UUID.randomUUID().toString();
        String ip = "127.0.0.1";
        String host = "localhost";
        String elasticAgentId = "elastic-agent-id-" + counter;

        AgentIdentifier id = new AgentIdentifier(host, ip, uuid);
        ElasticAgentRuntimeInfo elasticRuntime = new ElasticAgentRuntimeInfo(id, Idle, "/foo/one", null, elasticAgentId, elasticPluginId);

        Agent elasticAgent = createElasticAgent(uuid, ip, elasticAgentId, elasticPluginId);
        AgentInstance elasticAgentInstance = createFromAgent(elasticAgent, new SystemEnvironment(), mock(AgentStatusChangeListener.class));

        elasticAgentInstance.update(elasticRuntime);
        return elasticAgentInstance;
    }

    private Agent createElasticAgent(String uuid, String ip, String elasticAgentId, String elasticPluginId){
        Agent elasticAgent = new Agent(uuid);
        elasticAgent.setElasticAgentId(elasticAgentId);
        elasticAgent.setElasticPluginId(elasticPluginId);
        elasticAgent.setIpaddress(ip);
        return elasticAgent;
    }

}
