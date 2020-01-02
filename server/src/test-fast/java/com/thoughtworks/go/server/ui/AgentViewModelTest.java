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
package com.thoughtworks.go.server.ui;

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.config.ResourceConfig;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.DiskSpace;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static com.thoughtworks.go.helper.AgentInstanceMother.*;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

@SuppressWarnings("deprecation")
class AgentViewModelTest {

    @Nested
    class Sort{
        @Test
        void shouldSortAgentInstanceWithDifferentStatusCorrectly() {
            AgentViewModel agentBuilding = new AgentViewModel(building());
            AgentViewModel agentCancelled = new AgentViewModel(cancelled());
            AgentViewModel agentIdle = new AgentViewModel(idle(new Date(), "CCeDev01"));
            List<AgentViewModel> sorted = sort(AgentViewModel.STATUS_COMPARATOR, agentCancelled, agentIdle, agentBuilding);
            assertThat(sorted.get(0), is(agentBuilding));
            assertThat(sorted.get(1), is(agentCancelled));
            assertThat(sorted.get(2), is(agentIdle));
        }

        @Test
        void shouldSortAgentInstanceWithDifferentHostnameCorrectly() {
            AgentViewModel agentA = new AgentViewModel(updateHostname(pending(), "A"));
            AgentViewModel agentB = new AgentViewModel(updateHostname(pending(), "B"));
            AgentViewModel agentLowerA = new AgentViewModel(updateHostname(pending(), "a"));
            List<AgentViewModel> sorted = sort(AgentViewModel.HOSTNAME_COMPARATOR, agentB, agentLowerA, agentA);
            assertThat(sorted.get(0), is(agentA));
            assertThat(sorted.get(1), is(agentLowerA));
            assertThat(sorted.get(2), is(agentB));
        }

        @Test
        void shouldSortAgentInstanceWithDifferentLocationCorrectly() {
            AgentViewModel agentInLib = new AgentViewModel(updateLocation(pending(), "/var/lib"));
            AgentViewModel agentInBin = new AgentViewModel(updateLocation(pending(), "/usr/bin"));
            AgentViewModel agentInUpcaseBin = new AgentViewModel(updateLocation(pending(), "/Usr/bin"));
            List<AgentViewModel> sorted = sort(AgentViewModel.LOCATION_COMPARATOR, agentInLib, agentInBin, agentInUpcaseBin);
            assertThat(sorted.get(0), is(agentInUpcaseBin));
            assertThat(sorted.get(1), is(agentInBin));
            assertThat(sorted.get(2), is(agentInLib));
        }

        @Test
        void shouldSortAgentInstanceWithDifferentIpAddressesCorrectly() {
            AgentViewModel agent20 = new AgentViewModel(updateIpAddress(AgentInstanceMother.disabled(), "10.12.34.20"));
            AgentViewModel agent3 = new AgentViewModel(updateIpAddress(AgentInstanceMother.disabled(), "10.12.34.3"));
            AgentViewModel agent2_12_30 = new AgentViewModel(updateIpAddress(AgentInstanceMother.disabled(), "10.2.12.30"));
            AgentViewModel agent244_243_111 = new AgentViewModel(updateIpAddress(AgentInstanceMother.disabled(), "10.244.243.111"));
            List<AgentViewModel> sorted = sort(AgentViewModel.IP_ADDRESS_COMPARATOR, agent20, agent3, agent2_12_30, agent244_243_111);
            assertThat(sorted.get(0), is(agent2_12_30));
            assertThat(sorted.get(1), is(agent3));
            assertThat(sorted.get(2), is(agent20));
            assertThat(sorted.get(3), is(agent244_243_111));
        }

        @Test
        void shouldSortAgentInstanceWithDifferentUsableSpaceCorrectly() {
            // nobody's grand daddy knows where this came from!
            List<Long> magicValuesForTimSort = asList(1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, -1L, -1L,
                    -1L, -1L, -1L, -1L, -1L, -1L, -1L, -1L, -1L, -1L, -1L, -1L, -1L, -1L, -1L,
                    -1L, -1L, -1L, -1L, -1L, 1L, 2L);
            List<AgentViewModel> avmList = new ArrayList<>();
            for (Long cur : magicValuesForTimSort) {
                avmList.add(new AgentViewModel(updateUsableSpace(pending(), cur)));
            }

            AgentsViewModel list = new AgentsViewModel(avmList.toArray(new AgentViewModel[magicValuesForTimSort.size()]));
            list.sortBy(AgentViewModel.USABLE_SPACE_COMPARATOR, SortOrder.ASC);

            isSortedByUsableSpace(list);
        }

        private void isSortedByUsableSpace(AgentsViewModel list) {
            for (int i = 0; i < list.size() - 1; i++) {
                DiskSpace first = list.get(i).freeDiskSpace();
                Long firstSpace = (Long) ReflectionUtil.getField(first, "space");
                DiskSpace second = list.get(i + 1).freeDiskSpace();
                Long secondSpace = (Long) ReflectionUtil.getField(second, "space");
                if (firstSpace > secondSpace) {
                    throw new RuntimeException("The list is not sorted!");
                }
            }
        }

        @Test
        void shouldSortAgentInstanceWithDifferentResourcesCorrectly() {
            AgentViewModel agentA = new AgentViewModel(updateResources(AgentInstanceMother.building(), "a"));
            AgentViewModel agentB = new AgentViewModel(updateResources(pending(), "b"));
            AgentViewModel agentUpperB = new AgentViewModel(updateResources(pending(), "B"));
            List<AgentViewModel> sorted = sort(AgentViewModel.RESOURCES_COMPARATOR, agentB, agentA, agentUpperB);
            assertThat(sorted.get(0), is(agentA));
            assertThat(sorted.get(1), is(agentUpperB));
            assertThat(sorted.get(2), is(agentB));
        }

        @Test
        void shouldSortAgentInstanceWithDifferentResourcesCorrectlyForMultipleResources() {
            AgentViewModel agentA = new AgentViewModel(updateResources(AgentInstanceMother.building(), "foo,bar"));
            AgentViewModel agentB = new AgentViewModel(updateResources(pending(), "blah,dfg"));
            AgentViewModel agentC = new AgentViewModel(updateResources(pending(), "goo,zoo"));
            List<AgentViewModel> sorted = sort(AgentViewModel.RESOURCES_COMPARATOR, agentC, agentA, agentB);
            assertThat(sorted.get(0), is(agentA));
            assertThat(sorted.get(1), is(agentB));
            assertThat(sorted.get(2), is(agentC));
        }


        @Test
        void shouldSortAgentInstanceWithDifferentEnvironments() {
            AgentViewModel agentA = new AgentViewModel(AgentInstanceMother.building(), "foo", "bar");
            AgentViewModel agentB = new AgentViewModel(AgentInstanceMother.building(), "blah", "dfg");
            AgentViewModel agentC = new AgentViewModel(AgentInstanceMother.building(), "goo", "zoo");
            AgentViewModel agentD = new AgentViewModel(AgentInstanceMother.building());
            AgentViewModel agentE = new AgentViewModel(AgentInstanceMother.building(), "foo", "Baz");

            List<AgentViewModel> sorted = sort(AgentViewModel.ENVIRONMENTS_COMPARATOR, agentC, agentA, agentB, agentD, agentE);
            assertThat(sorted.get(0), is(agentD));
            assertThat(sorted.get(1), is(agentA));
            assertThat(sorted.get(2), is(agentE));
            assertThat(sorted.get(3), is(agentB));
            assertThat(sorted.get(4), is(agentC));
        }


        @Test
        void shouldSortAgentOnOperatingSystem() {
            AgentViewModel linux = new AgentViewModel(updateOS(AgentInstanceMother.building(), "LINUX"));
            AgentViewModel lowercaseLinux = new AgentViewModel(updateOS(AgentInstanceMother.building(), "linux"));
            AgentViewModel sun = new AgentViewModel(updateOS(AgentInstanceMother.building(), "SUN_OS"));
            AgentViewModel osx = new AgentViewModel(updateOS(AgentInstanceMother.building(), "OSX"));
            AgentViewModel windows = new AgentViewModel(updateOS(AgentInstanceMother.building(), "WINDOWS"));
            AgentViewModel unknown = new AgentViewModel(updateOS(AgentInstanceMother.building(), "UNKNOWN"));

            List<AgentViewModel> sorted = sort(AgentViewModel.OS_COMPARATOR, osx, unknown, linux, sun, windows, lowercaseLinux);
            assertThat(sorted.get(0), is(linux));
            assertThat(sorted.get(1), is(lowercaseLinux));
            assertThat(sorted.get(2), is(osx));
            assertThat(sorted.get(3), is(sun));
            assertThat(sorted.get(4), is(unknown));
            assertThat(sorted.get(5), is(windows));
        }
    }

    @Nested
    class Misc{
        @Test
        void shouldDisplayTheRightStatusMessage() {
            AgentInstance building = AgentInstanceMother.building();
            assertThat(new AgentViewModel(updateRuntimeStatus(building, AgentRuntimeStatus.Idle)).getStatusForDisplay(), is("Idle"));
            assertThat(new AgentViewModel(updateRuntimeStatus(building, AgentRuntimeStatus.Building)).getStatusForDisplay(), is("Building"));
            assertThat(new AgentViewModel(updateRuntimeStatus(building, AgentRuntimeStatus.Cancelled)).getStatusForDisplay(), is("Building (Cancelled)"));
        }

        @Test
        void shouldMapErrors() {
            Agent agent = new Agent("uuid", "host", "IP", asList("foo", "bar"));
            agent.addError(Agent.IP_ADDRESS, "bad ip");
            agent.addError(ResourceConfig.NAME, "bad name for resource2");
            agent.addError(ResourceConfig.NAME, "bad name for resource1");
            AgentViewModel model = new AgentViewModel(AgentInstance.createFromAgent(agent, mock(SystemEnvironment.class), null));
            assertThat(model.errors().isEmpty(), is(false));
            assertThat(model.errors().on(Agent.IP_ADDRESS), is("bad ip"));

            assertThat(model.errors().getAllOn(ResourceConfig.NAME).contains("bad name for resource1"), is(true));
            assertThat(model.errors().getAllOn(ResourceConfig.NAME).contains("bad name for resource2"), is(true));
        }
    }

    private List<AgentViewModel> sort(Comparator<AgentViewModel> comparator, AgentViewModel... agentViewModels) {
        AgentsViewModel list = new AgentsViewModel(agentViewModels);
        list.sortBy(comparator, SortOrder.ASC);
        return list;
    }

}
