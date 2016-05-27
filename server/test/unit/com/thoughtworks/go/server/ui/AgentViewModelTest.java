/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.server.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.Resource;
import com.thoughtworks.go.config.Resources;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.DiskSpace;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Test;

import static com.thoughtworks.go.helper.AgentInstanceMother.building;
import static com.thoughtworks.go.helper.AgentInstanceMother.cancelled;
import static com.thoughtworks.go.helper.AgentInstanceMother.idle;
import static com.thoughtworks.go.helper.AgentInstanceMother.pending;
import static com.thoughtworks.go.helper.AgentInstanceMother.updateAgentLauncherVersion;
import static com.thoughtworks.go.helper.AgentInstanceMother.updateHostname;
import static com.thoughtworks.go.helper.AgentInstanceMother.updateIpAddress;
import static com.thoughtworks.go.helper.AgentInstanceMother.updateLocation;
import static com.thoughtworks.go.helper.AgentInstanceMother.updateOS;
import static com.thoughtworks.go.helper.AgentInstanceMother.updateResources;
import static com.thoughtworks.go.helper.AgentInstanceMother.updateRuntimeStatus;
import static com.thoughtworks.go.helper.AgentInstanceMother.updateUsableSpace;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;

public class AgentViewModelTest {

    @Test public void shouldSortAgentInstanceWithDifferentStatusCorrectly(){
        AgentViewModel agentBuilding = new AgentViewModel(building());
        AgentViewModel agentCancelled = new AgentViewModel(cancelled());
        AgentViewModel agentIdle = new AgentViewModel(idle(new Date(), "CCeDev01"));
        List<AgentViewModel> sorted = sort(AgentViewModel.STATUS_COMPARATOR, agentCancelled, agentIdle, agentBuilding);
        assertThat(sorted.get(0), is(agentBuilding));
        assertThat(sorted.get(1), is(agentCancelled));
        assertThat(sorted.get(2), is(agentIdle));
    }

    @Test public void shouldSortAgentInstanceWithDifferentHostnameCorrectly(){
        AgentViewModel agentA = new AgentViewModel(updateHostname(pending(), "A"));
        AgentViewModel agentB = new AgentViewModel(updateHostname(pending(), "B"));
        AgentViewModel agentLowerA = new AgentViewModel(updateHostname(pending(), "a"));
        List<AgentViewModel> sorted = sort(AgentViewModel.HOSTNAME_COMPARATOR, agentB, agentLowerA, agentA);
        assertThat(sorted.get(0), is(agentA));
        assertThat(sorted.get(1), is(agentLowerA));
        assertThat(sorted.get(2), is(agentB));
    }

    @Test public void shouldSortAgentInstanceWithDifferentLocationCorrectly(){
        AgentViewModel agentInLib = new AgentViewModel(updateLocation(pending(), "/var/lib"));
        AgentViewModel agentInBin = new AgentViewModel(updateLocation(pending(), "/usr/bin"));
        AgentViewModel agentInUpcaseBin = new AgentViewModel(updateLocation(pending(), "/Usr/bin"));
        List<AgentViewModel> sorted = sort(AgentViewModel.LOCATION_COMPARATOR, agentInLib, agentInBin, agentInUpcaseBin);
        assertThat(sorted.get(0), is(agentInUpcaseBin));
        assertThat(sorted.get(1), is(agentInBin));
        assertThat(sorted.get(2), is(agentInLib));
    }

    @Test public void shouldSortAgentInstanceWithDifferentIpAddressesCorrectly(){
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
    public void shouldSortAgentInstanceWithDifferentUsableSpaceCorrectly() {
        // nobody's grand daddy knows where this came from!
        List<Long> magicValuesForTimSort = Arrays.asList(1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, -1L, -1L,
                -1L, -1L, -1L, -1L, -1L, -1L, -1L, -1L, -1L, -1L, -1L, -1L, -1L, -1L, -1L,
                -1L, -1L, -1L, -1L, -1L, 1L, 2L);
        List<AgentViewModel> avmList = new ArrayList<AgentViewModel>();
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

    @Test public void shouldSortAgentInstanceWithDifferentResourcesCorrectly(){
        AgentViewModel agentA = new AgentViewModel(updateResources(AgentInstanceMother.building(), "a"));
        AgentViewModel agentB = new AgentViewModel(updateResources(pending(), "b"));
        AgentViewModel agentUpperB = new AgentViewModel(updateResources(pending(), "B"));
        List<AgentViewModel> sorted = sort(AgentViewModel.RESOURCES_COMPARATOR, agentB, agentA, agentUpperB);
        assertThat(sorted.get(0), is(agentA));
        assertThat(sorted.get(1), is(agentUpperB));
        assertThat(sorted.get(2), is(agentB));
    }

    @Test public void shouldSortAgentInstanceWithDifferentResourcesCorrectlyForMultipleResources(){
        AgentViewModel agentA = new AgentViewModel(updateResources(AgentInstanceMother.building(), "foo,bar"));
        AgentViewModel agentB = new AgentViewModel(updateResources(pending(), "blah,dfg"));
        AgentViewModel agentC = new AgentViewModel(updateResources(pending(), "goo,zoo"));
        List<AgentViewModel> sorted = sort(AgentViewModel.RESOURCES_COMPARATOR, agentC,agentA, agentB);
        assertThat(sorted.get(0), is(agentA));
        assertThat(sorted.get(1), is(agentB));
        assertThat(sorted.get(2), is(agentC));
    }


    @Test public void shouldSortAgentInstanceWithDifferentEnvironments(){
        AgentViewModel agentA = new AgentViewModel(AgentInstanceMother.building(), "foo", "bar");
        AgentViewModel agentB = new AgentViewModel(AgentInstanceMother.building(), "blah", "dfg");
        AgentViewModel agentC = new AgentViewModel(AgentInstanceMother.building(), "goo", "zoo");
        AgentViewModel agentD = new AgentViewModel(AgentInstanceMother.building());
        AgentViewModel agentE = new AgentViewModel(AgentInstanceMother.building(), "foo", "Baz");

        List<AgentViewModel> sorted = sort(AgentViewModel.ENVIRONMENTS_COMPARATOR, agentC,agentA, agentB, agentD, agentE);
        assertThat(sorted.get(0), is(agentD));
        assertThat(sorted.get(1), is(agentA));
        assertThat(sorted.get(2), is(agentE));
        assertThat(sorted.get(3), is(agentB));
        assertThat(sorted.get(4), is(agentC));
    }


    @Test public void shouldSortAgentOnOperatingSystem(){
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

    @Test
    public void shouldSortAgentOnAgentBootstrapperVersion(){
        AgentViewModel v12dot3 = new AgentViewModel(updateAgentLauncherVersion(AgentInstanceMother.building(), "12.3"));
        AgentViewModel v12dot3Another = new AgentViewModel(updateAgentLauncherVersion(AgentInstanceMother.building(), "12.3"));
        AgentViewModel v2dot4 = new AgentViewModel(updateAgentLauncherVersion(AgentInstanceMother.building(), "2.4"));
        AgentViewModel v12dot2 = new AgentViewModel(updateAgentLauncherVersion(AgentInstanceMother.building(), "12.2"));
        AgentViewModel v12dot5 = new AgentViewModel(updateAgentLauncherVersion(AgentInstanceMother.building(), "12.5"));
        AgentViewModel unknown = new AgentViewModel(updateAgentLauncherVersion(AgentInstanceMother.building(), "unknown"));

        List<AgentViewModel> sorted = sort(AgentViewModel.BOOTSTRAPPER_VERSION_COMPARATOR, v12dot3, v12dot3Another, v2dot4, v12dot2, v12dot5, unknown);
        assertThat(sorted.get(0), is(v2dot4));
        assertThat(sorted.get(1), is(v12dot2));
        assertThat(sorted.get(2), is(v12dot3));
        assertThat(sorted.get(3), is(v12dot3Another));
        assertThat(sorted.get(4), is(v12dot5));
        assertThat(sorted.get(5), is(unknown));
    }

    @Test
    public void shouldDisplayTheRightStatusMessage() {
        AgentInstance building = AgentInstanceMother.building();
        assertThat(new AgentViewModel(updateRuntimeStatus(building, AgentRuntimeStatus.Idle)).getStatusForDisplay(), is("Idle"));
        assertThat(new AgentViewModel(updateRuntimeStatus(building, AgentRuntimeStatus.Building)).getStatusForDisplay(), is("Building"));
        assertThat(new AgentViewModel(updateRuntimeStatus(building, AgentRuntimeStatus.Cancelled)).getStatusForDisplay(), is("Building (Cancelled)"));
    }

    @Test
    public void shouldShowUnknownBoostrapperVersionWhenAgentIsMissing() {
        AgentInstance missing = AgentInstanceMother.missing();
        AgentViewModel missingViewModel = new AgentViewModel(missing);
        assertThat(missingViewModel.getBootstrapperVersion(), is(AgentViewModel.MISSING_AGENT_BOOTSTRAPPER_VERSION));
    }

    @Test
    public void shouldShowAppropriateBoostrapperVersionForAgentsGtEq12dot3() {
        AgentInstance building = AgentInstanceMother.building();
        AgentInstanceMother.updateAgentLauncherVersion(building, "12.5");
        AgentViewModel buildingViewModel = new AgentViewModel(building);
        assertThat(buildingViewModel.getBootstrapperVersion(), is(building.getAgentLauncherVersion()));
        AgentInstanceMother.updateAgentLauncherVersion(building, "12.3");
        buildingViewModel = new AgentViewModel(building);
        assertThat(buildingViewModel.getBootstrapperVersion(), is(building.getAgentLauncherVersion()));
    }

    @Test
    public void shouldShowOlderBoostrapperVersionForAgentsLt12dot3() {
        AgentInstance building = AgentInstanceMother.building();
        AgentViewModel buildingViewModel = new AgentViewModel(building);
        assertThat(buildingViewModel.getBootstrapperVersion(), is(AgentViewModel.OLDER_AGENT_BOOTSTRAPPER_VERSION));
    }

    @Test
    public void shouldIndicateThatAgentNeedsUpgrade() {
        AgentInstance agentInstance = AgentInstanceMother.updateAgentLauncherVersion(AgentInstanceMother.building(), null);
        AgentViewModel modelWithNullLauncherVersion = new AgentViewModel(agentInstance);
        assertThat(modelWithNullLauncherVersion.needsUpgrade(), is(true));

        AgentInstanceMother.updateAgentLauncherVersion(agentInstance, "");
        AgentViewModel modelWithEmptyLauncherVersion = new AgentViewModel(agentInstance);
        assertThat(modelWithEmptyLauncherVersion.needsUpgrade(), is(true));
    }

    @Test
    public void shouldIndicateThatAgentDoesNOTNeedUpgrade() {
        AgentInstance agentInstance = AgentInstanceMother.updateAgentLauncherVersion(AgentInstanceMother.building(), "12.3");
        AgentViewModel model = new AgentViewModel(agentInstance);
        assertThat(model.needsUpgrade(), is(false));
    }

    @Test
    public void shouldIndicateThatAgentDoesNOTNeedUpgrade_WhenMissing() {
        AgentViewModel model = new AgentViewModel(AgentInstanceMother.missing());
        assertThat(model.needsUpgrade(), is(false));
    }

    @Test
    public void shouldMapErrors(){
        Resource resource1 = new Resource("foo");
        Resource resource2 = new Resource("bar");
        AgentConfig agentConfig = new AgentConfig("uuid", "host", "IP", new Resources(resource1, resource2));
        agentConfig.addError(AgentConfig.IP_ADDRESS, "bad ip");
        resource1.addError(Resource.NAME, "bad name for resource1");
        resource2.addError(Resource.NAME, "bad name for resource2");
        AgentViewModel model = new AgentViewModel(AgentInstance.createFromConfig(agentConfig, mock(SystemEnvironment.class)));
        assertThat(model.errors().isEmpty(), is(false));
        assertThat(model.errors().on(AgentConfig.IP_ADDRESS), is("bad ip"));
        assertThat(model.errors().getAllOn(Resource.NAME).contains("bad name for resource1"), is(true));
        assertThat(model.errors().getAllOn(Resource.NAME).contains("bad name for resource2"), is(true));
    }

    private List<AgentViewModel> sort(Comparator<AgentViewModel> comparator, AgentViewModel... agentViewModels) {
        AgentsViewModel list = new AgentsViewModel(agentViewModels);
        list.sortBy(comparator, SortOrder.ASC);
        return list;
    }

}
