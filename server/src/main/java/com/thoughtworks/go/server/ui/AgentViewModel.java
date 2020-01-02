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

import com.thoughtworks.go.config.ResourceConfig;
import com.thoughtworks.go.config.ResourceConfigs;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.util.comparator.AlphaAsciiComparator;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * @understands agent information for the UI
 */
@Deprecated
public class AgentViewModel implements Comparable<AgentViewModel>{
    private AgentInstance agentInstance;
    private Set<String> environments;

    public AgentViewModel(AgentInstance agentInstance) {
        this(agentInstance, new HashSet<>());
    }

    public AgentViewModel(AgentInstance agentInstance, Collection<String> environments) {
        this.agentInstance = agentInstance;
        this.environments = new TreeSet<>(environments);
    }

    public AgentViewModel(AgentInstance agentInstance, String...  environments) {
        this(agentInstance, Arrays.asList(environments));
    }

    public String getHostname() {
        return agentInstance.getHostname();
    }

    public String getIpAddress() {
        return agentInstance.getIpAddress();
    }

    public String getLocation() {
        return agentInstance.getLocation();
    }

    public DiskSpace freeDiskSpace() {
        return agentInstance.freeDiskSpace();
    }

    public List<String> getResources() {
        return resources().resourceNames();
    }

    public ResourceConfigs resources() {
        return agentInstance.getResourceConfigs();
    }

    public AgentStatus getStatus() {
        return agentInstance.getStatus();
    }

    public AgentRuntimeStatus getRuntimeStatus(){
        return agentInstance.getRuntimeStatus();
    }

    public AgentConfigStatus getAgentConfigStatus(){
        return agentInstance.getAgentConfigStatus();
    }

    public String getStatusForDisplay() {
        return isCancelled() ? "Building (Cancelled)" : getStatus().toString();
    }

    public String buildLocator(){
        return agentInstance.getBuildLocator();
    }

    public Date getLastHeardTime(){
        return agentInstance.getLastHeardTime();
    }

    public String getUuid(){
        return agentInstance.getUuid();
    }

    public boolean isBuilding(){
        return agentInstance.isBuilding();
    }

    public boolean isCancelled(){
        return agentInstance.isCancelled();
    }

    public boolean isEnabled(){
        return !agentInstance.isDisabled();
    }

    public static Comparator<AgentViewModel> STATUS_COMPARATOR = Comparator.comparing(AgentViewModel::getStatus);

    public static Comparator<AgentViewModel> HOSTNAME_COMPARATOR = (agentInstance1, agentInstance2) -> new AlphaAsciiComparator().compare(agentInstance1.getHostname(), agentInstance2.getHostname());

    public static Comparator<AgentViewModel> IP_ADDRESS_COMPARATOR = Comparator.comparing(agentInstance1 -> IpAddress.create(agentInstance1.getIpAddress()));

    public static Comparator<AgentViewModel> LOCATION_COMPARATOR = Comparator.comparing(AgentViewModel::getLocation);

    public static Comparator<AgentViewModel> USABLE_SPACE_COMPARATOR = Comparator.comparing(AgentViewModel::freeDiskSpace);

    public static Comparator<AgentViewModel> RESOURCES_COMPARATOR = Comparator.comparing(AgentViewModel::resources);

    public static Comparator<AgentViewModel> ENVIRONMENTS_COMPARATOR = (agentInstance1, agentInstance2) -> new AlphaAsciiComparator().compare(StringUtils.join(agentInstance1.getEnvironments().toArray()), StringUtils.join(agentInstance2.getEnvironments().toArray()));

    public static Comparator<AgentViewModel> OS_COMPARATOR= (agentInstance1, agentInstance2) -> new AlphaAsciiComparator().compare(agentInstance1.getOperatingSystem(), agentInstance2.getOperatingSystem());

    @Override
    public int compareTo(AgentViewModel other) {
        return this.agentInstance.compareTo(other.agentInstance);
    }

    public Set<String> getEnvironments() {
        return environments;
    }

    @Override public String toString() {
        return "hostname= " + agentInstance.getHostname() +
                " location = " + agentInstance.getLocation() +
                " environments = " + environments +
                " resources= " + getResources().toString() +
                " os= " + getOperatingSystem() +
                " status = " + getStatus() +
                " ip = " + getIpAddress();
    }

    public String getOperatingSystem() {
        return agentInstance.getOperatingSystem();
    }

    public boolean isNullAgent() {
        return agentInstance.isNullAgent();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AgentViewModel that = (AgentViewModel) o;
        return Objects.equals(agentInstance, that.agentInstance)
                && Objects.equals(environments, that.environments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agentInstance, environments);
    }

    public ConfigErrors errors() {
        ConfigErrors configErrors = new ConfigErrors();
        configErrors.addAll(agentInstance.getAgent().errors());
        for (ResourceConfig resourceConfig : agentInstance.getResourceConfigs()) {
            configErrors.addAll(resourceConfig.errors());
        }
        return configErrors;
    }
}
