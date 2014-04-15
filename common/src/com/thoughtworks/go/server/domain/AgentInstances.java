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

package com.thoughtworks.go.server.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.Agents;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.AgentStatus;
import com.thoughtworks.go.domain.NullAgentInstance;
import com.thoughtworks.go.server.service.AgentBuildingInfo;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.SystemEnvironment;

public class AgentInstances implements Iterable<AgentInstance> {


    private Map<String, AgentInstance> agentInstances = new ConcurrentHashMap<String, AgentInstance>();
    private AgentRuntimeStatus.ChangeListener changeListener;

    public AgentInstances(AgentRuntimeStatus.ChangeListener changeListener) {
        this.changeListener = changeListener;
    }

    public AgentInstances(AgentRuntimeStatus.ChangeListener changeListener, AgentInstance... agentInstances) {
        this(changeListener);
        for (AgentInstance agentInstance : agentInstances) {
            this.add(agentInstance);
        }
    }

    public void add(AgentInstance agent) {
        agentInstances.put(agent.agentConfig().getUuid(), agent);
    }

    public AgentInstances allVirtualAgents() {
        AgentInstances virtual = new AgentInstances(changeListener);
        for (AgentInstance agent : currentInstances()) {
            agent.addToVirtuals(virtual);
        }
        return virtual;
    }

    public void saveVirtualAgent(AgentInstance agent) {
        agentInstances.put(agent.agentConfig().getUuid(), agent);
    }

    public void updateAgentAboutCancelledBuild(String agentUuid, boolean isCancelled) {
        AgentInstance agentInstance = findAgentAndRefreshStatus(agentUuid);
        if (agentInstance != null && isCancelled) {
            agentInstance.cancel();
        }
    }

    public AgentConfig getFirstAgentByHostname(String hostname) {
        for (AgentInstance agent : currentInstances()) {
            if (hostname.equals(agent.agentConfig().getHostname())) {
                return agent.agentConfig();
            }
        }
        return null;
    }

    public AgentInstance findAgentAndRefreshStatus(String uuid) {
        AgentInstance agentInstance = loadAgentInstance(uuid);
        agentInstance.refresh(changeListener);
        return agentInstance;
    }

    public AgentInstance findAgent(String uuid) {
        return loadAgentInstance(uuid);
    }

    public AgentInstance loadAgentInstance(String uuid) {
        AgentInstance agentInstance = agentInstances.get(uuid);
        return agentInstance == null ? new NullAgentInstance(uuid) : agentInstance;
    }

    public void removeAgent(String uuid) {
        agentInstances.remove(uuid);
    }

    public void clearAll() {
        agentInstances.clear();
    }

    public AgentInstances findPhysicalAgents() {
        AgentInstances physicalAgents = new AgentInstances(changeListener);
        for (AgentInstance agent : currentInstances()) {
            agent.addToPhysical(physicalAgents);
        }
        return physicalAgents;
    }

    public AgentInstances findRegisteredAgents() {
        this.refresh();
        AgentInstances registered = new AgentInstances(changeListener);
        synchronized (agentInstances) {
            for (AgentInstance agentInstance : this) {
                agentInstance.addToRegistered(registered);
            }
        }
        return registered;
    }

    public AgentInstances findAgents(AgentStatus status) {
        AgentInstances found = new AgentInstances(changeListener);
        for (AgentInstance agent : currentInstances()) {
            agent.addTo(found, status);
        }
        return found;
    }

    public int agentCount(AgentStatus status) {
        return findAgents(status).size();
    }

    public AgentInstances findEnabledAgents() {
        AgentInstances agentInstances = new AgentInstances(changeListener);
        for (AgentInstance agentInstance : currentInstances()) {
            agentInstance.addToEnabled(agentInstances);
        }
        return agentInstances;
    }

    public Iterator<AgentInstance> iterator() {
        return currentInstances().iterator();
    }

    public boolean isEmpty() {
        return agentInstances.isEmpty();
    }

    public AgentInstance findFirstByHostname(String hostname) {
        for (AgentInstance agentInstance : currentInstances()) {
            if (agentInstance.agentConfig().getHostname().equals(hostname)) {
                return agentInstance;
            }
        }
        return new NullAgentInstance("");
    }

    public Integer size() {
        return agentInstances.size();

    }

    public void refresh() {
        for (AgentInstance instance : currentInstances()) {
            instance.refresh(this.changeListener);
        }
        for (AgentInstance agentInstance : agentsToRemove()) {
            removeAgent(agentInstance.agentConfig().getUuid());
        }
    }

    private List<AgentInstance> agentsToRemove() {
           List<AgentInstance> agentsToRemove = new ArrayList<AgentInstance>();
           for (AgentInstance instance : this) {
               instance.checkForRemoval(agentsToRemove);
           }
           return agentsToRemove;
       }


    private Collection<AgentInstance> currentInstances() {
        return new TreeSet<AgentInstance>(agentInstances.values());
    }

    public void sync(Agents agentsFromConfig) {
        for (AgentConfig agentInConfig : agentsFromConfig) {
            String uuid = agentInConfig.getUuid();
            if (agentInstances.containsKey(uuid)) {
                agentInstances.get(uuid).syncConfig(agentInConfig);
            } else {
                agentInstances.put(uuid, AgentInstance.createFromConfig(agentInConfig, new SystemEnvironment()));
            }
        }

        synchronized (agentInstances) {
            List<String> uuids = new ArrayList<String>();
            for (String uuid : agentInstances.keySet()) {
                AgentInstance instance = agentInstances.get(uuid);
                if (!agentsFromConfig.hasAgent(uuid) && !(instance.getStatus() == AgentStatus.Pending)) {
                    uuids.add(uuid);
                }
            }

            for (String uuid : uuids) {
                agentInstances.remove(uuid);
            }
        }
    }

    public boolean hasAgent(String uuid) {
        return !(findAgentAndRefreshStatus(uuid) instanceof NullAgentInstance);
    }

    public AgentInstance register(AgentRuntimeInfo info) {
        AgentInstance agentInstance = findAgentAndRefreshStatus(info.getUUId());
        if (!agentInstance.isRegistered()) {
            agentInstance = AgentInstance.createFromLiveAgent(info, new SystemEnvironment());
            this.add(agentInstance);
        }
        agentInstance.update(info);
        return agentInstance;
    }

    public void updateAgentRuntimeInfo(AgentRuntimeInfo info) {
        AgentInstance instance = this.findAgentAndRefreshStatus(info.getUUId());
        instance.update(info);
    }

    public int numberOfActiveRemoteAgents() {
        int count = 0;
        for (AgentInstance instance : currentInstances()) {
            if (instance.isActiveRemoteAgent()) {
                count++;
            }
        }
        return count;
    }

    public void building(String uuid, AgentBuildingInfo agentBuildingInfo) {
        findAgentAndRefreshStatus(uuid).building(agentBuildingInfo);
    }

    public int numberOf(AgentStatus status) {
        int total = 0;
        for (AgentInstance agentInstance : currentInstances()) {
            if (agentInstance.getStatus().equals(status)) {
                total += 1;
            }
        }
        return total;
    }


    public List<AgentInstance> filter(List<String> uuids) {
        ArrayList<AgentInstance> filtered = new ArrayList<AgentInstance>();
        for (AgentInstance agentInstance : this) {
            if (uuids.contains(agentInstance.getUuid())) {
                filtered.add(agentInstance);
            }
        }
        return filtered;
    }

    public Set<String> getAllHostNames() {
        Set<String> names = new HashSet<String>();
        for (AgentInstance agentInstance : agentInstances.values()) {
            names.add(agentInstance.getHostname());
        }
        return names;
    }

    public Set<String> getAllIpAddresses() {
        Set<String> ips = new HashSet<String>();
        for (AgentInstance agentInstance : agentInstances.values()) {
            ips.add(agentInstance.getIpAddress());
        }
        return ips;
    }

    public Set<String> getAllOperatingSystems() {
        Set<String> osList = new HashSet<String>();
        for (AgentInstance agentInstance : agentInstances.values()) {
            osList.add(agentInstance.getOperatingSystem());
        }
        return osList;
    }
}