/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.ui;

import java.util.*;

import com.thoughtworks.go.config.Resources;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.util.comparator.AlphaAsciiComparator;
import info.aduna.text.NumericStringComparator;
import org.apache.commons.lang.StringUtils;

/**
 * @understands agent information for the UI
 */
public class AgentViewModel implements Comparable<AgentViewModel>{
    static final String MISSING_AGENT_BOOTSTRAPPER_VERSION = "Unknown";
    static final String OLDER_AGENT_BOOTSTRAPPER_VERSION = "Older";
    private AgentInstance agentInstance;
    private Set<String> environments;

    public AgentViewModel(AgentInstance agentInstance) {
        this(agentInstance, new HashSet<String>());
    }

    public AgentViewModel(AgentInstance agentInstance, Collection<String> environments) {
        this.agentInstance = agentInstance;
        this.environments = new TreeSet<String>(environments);
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

    public String getBootstrapperVersion() {
        if(agentInstance.isMissing()){
            return MISSING_AGENT_BOOTSTRAPPER_VERSION;
        }
        if(agentInstance.getAgentLauncherVersion() == null && !agentInstance.isMissing()){
            return OLDER_AGENT_BOOTSTRAPPER_VERSION;
        }
        return agentInstance.getAgentLauncherVersion();
    }

    public List<String> getResources() {
        return resources().resourceNames();
    }

    public Resources resources() {
        return agentInstance.getResources();
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

    public static Comparator<AgentViewModel> STATUS_COMPARATOR = new Comparator<AgentViewModel>() {
        public int compare(AgentViewModel agentInstance1, AgentViewModel agentInstance2) {
            return agentInstance1.getStatus().compareTo(agentInstance2.getStatus());
        }
    };

    public static Comparator<AgentViewModel> HOSTNAME_COMPARATOR = new Comparator<AgentViewModel>() {
        public int compare(AgentViewModel agentInstance1, AgentViewModel agentInstance2) {
            return new AlphaAsciiComparator().compare(agentInstance1.getHostname(), agentInstance2.getHostname());
        }
    };

    public static Comparator<AgentViewModel> IP_ADDRESS_COMPARATOR = new Comparator<AgentViewModel>() {
        public int compare(AgentViewModel agentInstance1, AgentViewModel agentInstance2) {
            return IpAddress.create(agentInstance1.getIpAddress()).compareTo(IpAddress.create(agentInstance2.getIpAddress()));
        }
    };

    public static Comparator<AgentViewModel> LOCATION_COMPARATOR = new Comparator<AgentViewModel>() {
        public int compare(AgentViewModel agentInstance1, AgentViewModel agentInstance2) {
            return agentInstance1.getLocation().compareTo(agentInstance2.getLocation());
        }
    };

    public static Comparator<AgentViewModel> USABLE_SPACE_COMPARATOR = new Comparator<AgentViewModel>() {
        public int compare(AgentViewModel agentInstance1, AgentViewModel agentInstance2) {
            return agentInstance1.freeDiskSpace().compareTo(agentInstance2.freeDiskSpace());
        }
    };

    public static Comparator<AgentViewModel> RESOURCES_COMPARATOR = new Comparator<AgentViewModel>() {
        public int compare(AgentViewModel agentInstance1, AgentViewModel agentInstance2) {
            return agentInstance1.resources().compareTo(agentInstance2.resources());
        }
    };

    public static Comparator<AgentViewModel> ENVIRONMENTS_COMPARATOR = new Comparator<AgentViewModel>() {
        public int compare(AgentViewModel agentInstance1, AgentViewModel agentInstance2) {
            return new AlphaAsciiComparator().compare(StringUtils.join(agentInstance1.getEnvironments().toArray()), StringUtils.join(agentInstance2.getEnvironments().toArray()));
        }
    };

    public static Comparator<AgentViewModel> OS_COMPARATOR=new Comparator<AgentViewModel>() {
        public int compare(AgentViewModel agentInstance1, AgentViewModel agentInstance2) {
            return new AlphaAsciiComparator().compare(agentInstance1.getOperatingSystem(), agentInstance2.getOperatingSystem());
        }
    };

    public static Comparator<AgentViewModel> BOOTSTRAPPER_VERSION_COMPARATOR=new Comparator<AgentViewModel>() {
        public int compare(AgentViewModel agentInstance1, AgentViewModel agentInstance2) {
            return new NumericStringComparator().compare(agentInstance1.getBootstrapperVersion(), agentInstance2.getBootstrapperVersion());
        }
    };

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
                " ip = " + getIpAddress() +
                " boostrapperVersion = " + getBootstrapperVersion();
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

        if (agentInstance != null ? !agentInstance.equals(that.agentInstance) : that.agentInstance != null) {
            return false;
        }
        if (environments != null ? !environments.equals(that.environments) : that.environments != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = agentInstance != null ? agentInstance.hashCode() : 0;
        result = 31 * result + (environments != null ? environments.hashCode() : 0);
        return result;
    }

    public boolean needsUpgrade() {
        return agentInstance.needsUpgrade();
    }
}
