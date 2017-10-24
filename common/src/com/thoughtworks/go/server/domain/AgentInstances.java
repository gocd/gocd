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
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.AgentStatus;
import com.thoughtworks.go.domain.NullAgentInstance;
import com.thoughtworks.go.domain.exception.MaxPendingAgentsLimitReachedException;
import com.thoughtworks.go.server.service.AgentBuildingInfo;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.ListUtil;
import com.thoughtworks.go.util.MapUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.util.LinkedMultiValueMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.thoughtworks.go.util.ListUtil.join;

public class AgentInstances implements Iterable<AgentInstance> {


    private SystemEnvironment systemEnvironment;
    private Map<String, AgentInstance> agentInstances = new ConcurrentHashMap<>();
    private AgentRuntimeStatus.ChangeListener changeListener;

    public AgentInstances(AgentRuntimeStatus.ChangeListener changeListener) {
        this.changeListener = changeListener;
        this.systemEnvironment = new SystemEnvironment();
    }

    public AgentInstances(AgentRuntimeStatus.ChangeListener changeListener, SystemEnvironment systemEnvironment, AgentInstance... agentInstances) {
        this(changeListener);
        this.systemEnvironment = systemEnvironment;
        for (AgentInstance agentInstance : agentInstances) {
            this.add(agentInstance);
        }
    }

    public void add(AgentInstance agent) {
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

    public AgentInstances allAgents() {
        AgentInstances agents = new AgentInstances(changeListener);
        for (AgentInstance agent : currentInstances()) {
            agents.add(agent);
        }
        return agents;
    }

    public AgentInstances findRegisteredAgents() {
        this.refresh();
        AgentInstances registered = new AgentInstances(changeListener);
        synchronized (agentInstances) {
            for (AgentInstance agentInstance : this) {
                if (agentInstance.getStatus().isRegistered()) {
                    registered.add(agentInstance);
                }
            }
        }
        return registered;
    }

    public AgentInstances findDisabledAgents() {
        AgentInstances agentInstances = new AgentInstances(changeListener);
        for (AgentInstance agentInstance : currentInstances()) {
            if (agentInstance.isDisabled()){
                agentInstances.add(agentInstance);
            }
        }
        return agentInstances;
    }

    public AgentInstances findEnabledAgents() {
        AgentInstances agentInstances = new AgentInstances(changeListener);
        for (AgentInstance agentInstance : currentInstances()) {
            if (agentInstance.getStatus().isEnabled()) {
                agentInstances.add(agentInstance);
            }
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
        List<AgentInstance> agentsToRemove = new ArrayList<>();
        for (AgentInstance instance : this) {
            instance.checkForRemoval(agentsToRemove);
        }
        return agentsToRemove;
    }

    private Collection<AgentInstance> currentInstances() {
        return new TreeSet<>(agentInstances.values());
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
            List<String> uuids = new ArrayList<>();
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
            if(isMaxPendingAgentsLimitReached()) {
                throw new MaxPendingAgentsLimitReachedException(systemEnvironment.get(SystemEnvironment.MAX_PENDING_AGENTS_ALLOWED));
            }
            agentInstance = AgentInstance.createFromLiveAgent(info, systemEnvironment);
            this.add(agentInstance);
        }
        agentInstance.update(info);
        return agentInstance;
    }

    private boolean isMaxPendingAgentsLimitReached() {
        Integer maxPendingAgentsAllowed = systemEnvironment.get(SystemEnvironment.MAX_PENDING_AGENTS_ALLOWED);
        int pendingAgentsCount = this.size() - findRegisteredAgents().size();

        return pendingAgentsCount >= maxPendingAgentsAllowed;
    }



    public void updateAgentRuntimeInfo(AgentRuntimeInfo info) {
        AgentInstance instance = this.findAgentAndRefreshStatus(info.getUUId());
        instance.update(info);
    }

    public void building(String uuid, AgentBuildingInfo agentBuildingInfo) {
        findAgentAndRefreshStatus(uuid).building(agentBuildingInfo);
    }

    public List<AgentInstance> filter(List<String> uuids) {
        ArrayList<AgentInstance> filtered = new ArrayList<>();
        for (AgentInstance agentInstance : this) {
            if (uuids.contains(agentInstance.getUuid())) {
                filtered.add(agentInstance);
            }
        }
        return filtered;
    }

    public Set<String> getAllHostNames() {
        Set<String> names = new HashSet<>();
        for (AgentInstance agentInstance : agentInstances.values()) {
            names.add(agentInstance.getHostname());
        }
        return names;
    }

    public Set<String> getAllIpAddresses() {
        Set<String> ips = new HashSet<>();
        for (AgentInstance agentInstance : agentInstances.values()) {
            ips.add(agentInstance.getIpAddress());
        }
        return ips;
    }

    public Set<String> getAllOperatingSystems() {
        Set<String> osList = new HashSet<>();
        for (AgentInstance agentInstance : agentInstances.values()) {
            osList.add(agentInstance.getOperatingSystem());
        }
        return osList;
    }

    public LinkedMultiValueMap<String, ElasticAgentMetadata> allElasticAgentsGroupedByPluginId() {
        LinkedMultiValueMap<String, ElasticAgentMetadata> map = new LinkedMultiValueMap<>();

        for (Map.Entry<String, AgentInstance> entry : agentInstances.entrySet()) {
            AgentInstance agentInstance = entry.getValue();
            if (agentInstance.isElastic()) {
                ElasticAgentMetadata metadata = agentInstance.elasticAgentMetadata();
                map.add(metadata.elasticPluginId(), metadata);
            }
        }

        return map;
    }

    public AgentInstance findElasticAgent(final String elasticAgentId, final String elasticPluginId) {
        Collection<AgentInstance> values = MapUtil.filterValues(agentInstances, new MapUtil.Predicate<AgentInstance>() {
            public boolean apply(AgentInstance agentInstance) {
                if (!agentInstance.isElastic()) {
                    return false;
                }

                ElasticAgentMetadata elasticAgentMetadata = agentInstance.elasticAgentMetadata();
                return elasticAgentMetadata.elasticAgentId().equals(elasticAgentId) && elasticAgentMetadata.elasticPluginId().equals(elasticPluginId);

            }
        });


        if (values.size() == 0) {
            return null;
        }
        if (values.size() > 1) {
            Collection<String> uuids = ListUtil.map(values, new ListUtil.Transformer<AgentInstance, String>() {
                @Override
                public String transform(AgentInstance input) {
                    return input.getUuid();
                }
            });
            throw new IllegalStateException(String.format("Found multiple agents with the same elastic agent id [%s]", join(uuids)));
        }

        return values.iterator().next();
    }
}
