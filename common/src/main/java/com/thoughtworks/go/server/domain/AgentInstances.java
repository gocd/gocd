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
import com.thoughtworks.go.listener.AgentStatusChangeListener;
import com.thoughtworks.go.server.service.AgentBuildingInfo;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.LinkedMultiValueMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.StreamSupport.stream;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class AgentInstances implements Iterable<AgentInstance> {
    private SystemEnvironment systemEnvironment;

    private Map<String, AgentInstance> agentInstanceMap = new ConcurrentHashMap<>();

    private AgentStatusChangeListener agentStatusChangeListener;

    public AgentInstances(AgentStatusChangeListener listener) {
        this.agentStatusChangeListener = listener;
        this.systemEnvironment = new SystemEnvironment();
    }

    public AgentInstances(SystemEnvironment sysEnv, AgentStatusChangeListener listener, AgentInstance... agentInstances) {
        this(listener);
        this.systemEnvironment = sysEnv;
        Stream.of(agentInstances).forEach(this::add);
    }

    public void add(AgentInstance agent) {
        agentInstanceMap.put(agent.getAgent().getUuid(), agent);
    }

    public void updateAgentAboutCancelledBuild(String agentUuid, boolean isCancelled) {
        AgentInstance agentInstance = findAgentAndRefreshStatus(agentUuid);

        if (agentInstance != null && isCancelled) {
            agentInstance.cancel();
        }
    }

    public AgentInstance findAgentAndRefreshStatus(String uuid) {
        AgentInstance agentInstance = loadAgentInstance(uuid);
        agentInstance.refresh();
        return agentInstance;
    }

    public AgentInstance findAgent(String uuid) {
        return loadAgentInstance(uuid);
    }

    public AgentInstance loadAgentInstance(String uuid) {
        if (isBlank(uuid)) {
            return new NullAgentInstance(uuid);
        }
        AgentInstance agentInstance = agentInstanceMap.get(uuid);
        return agentInstance == null ? new NullAgentInstance(uuid) : agentInstance;
    }

    public void removeAgent(String uuid) {
        agentInstanceMap.remove(uuid);
    }

    public void clearAll() {
        agentInstanceMap.clear();
    }

    public Collection<AgentInstance> values() {
        return agentInstanceMap.values();
    }

    public AgentInstances sort() {
        AgentInstances agents = new AgentInstances(agentStatusChangeListener);
        Collection<AgentInstance> agentInstances = currentInstances();
        agentInstances.forEach(agents::add);
        return agents;
    }

    public AgentInstances findRegisteredAgents() {
        this.refresh();
        AgentInstances registered = new AgentInstances(agentStatusChangeListener);
        synchronized (agentInstanceMap) {
            stream(this.spliterator(), false).filter(agentInstance -> agentInstance.getStatus().isRegistered())
                                                    .forEach(registered::add);
        }
        return registered;
    }

    public AgentInstances findDisabledAgents() {
        AgentInstances agentInstances = new AgentInstances(agentStatusChangeListener);
        currentInstances().stream().filter(AgentInstance::isDisabled)
                                   .forEach(agentInstances::add);
        return agentInstances;
    }

    public AgentInstances findEnabledAgents() {
        AgentInstances agentInstances = new AgentInstances(agentStatusChangeListener);
        currentInstances().stream().filter(agentInstance -> agentInstance.getStatus().isEnabled())
                                   .forEach(agentInstances::add);
        return agentInstances;
    }

    @Override
    public Iterator<AgentInstance> iterator() {
        return currentInstances().iterator();
    }

    public boolean isEmpty() {
        return agentInstanceMap.isEmpty();
    }

    public AgentInstance findFirstByHostname(String hostname) {
        for (AgentInstance agentInstance : currentInstances()) {
            if (agentInstance.getAgent().getHostname().equals(hostname)) {
                return agentInstance;
            }
        }

        return new NullAgentInstance("");
    }

    public Integer size() {
        return agentInstanceMap.size();

    }

    public void refresh() {
        currentInstances().forEach(AgentInstance::refresh);
        agentsToRemove().forEach(agentInstance -> removeAgent(agentInstance.getAgent().getUuid()));
    }

    private List<AgentInstance> agentsToRemove() {
        return stream(this.spliterator(), false).filter(AgentInstance::canRemove)
                                                       .collect(Collectors.toList());
    }

    private Collection<AgentInstance> currentInstances() {
        return new TreeSet<>(agentInstanceMap.values());
    }

    public void sync(Agents agentsFromConfig) {
        for (Agent agent : agentsFromConfig) {
            String uuid = agent.getUuid();
            if (agentInstanceMap.containsKey(uuid)) {
                agentInstanceMap.get(uuid).syncConfig(agent);
            } else {
                agentInstanceMap.put(uuid, AgentInstance.createFromAgent(agent, new SystemEnvironment(), agentStatusChangeListener));
            }
        }

        synchronized (agentInstanceMap) {
            List<String> uuids = new ArrayList<>();
            for (String uuid : agentInstanceMap.keySet()) {
                AgentInstance instance = agentInstanceMap.get(uuid);
                if (!agentsFromConfig.hasAgent(uuid) && !(instance.getStatus() == AgentStatus.Pending)) {
                    uuids.add(uuid);
                }
            }

            uuids.forEach(agentInstanceMap::remove);
        }
    }

    public boolean hasAgent(String uuid) {
        return !(findAgentAndRefreshStatus(uuid) instanceof NullAgentInstance);
    }

    public AgentInstance register(AgentRuntimeInfo info) {
        AgentInstance agentInstance = findAgentAndRefreshStatus(info.getUUId());
        if (!agentInstance.isRegistered()) {
            if (isMaxPendingAgentsLimitReached()) {
                throw new MaxPendingAgentsLimitReachedException(systemEnvironment.get(SystemEnvironment.MAX_PENDING_AGENTS_ALLOWED));
            }
            agentInstance = AgentInstance.createFromLiveAgent(info, systemEnvironment, agentStatusChangeListener);
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
        return stream(this.spliterator(), false).filter(agentInstance -> uuids.contains(agentInstance.getUuid()))
                                                       .collect(Collectors.toList());
    }

    public LinkedMultiValueMap<String, ElasticAgentMetadata> allElasticAgentsGroupedByPluginId() {
        LinkedMultiValueMap<String, ElasticAgentMetadata> map = new LinkedMultiValueMap<>();

        for (Map.Entry<String, AgentInstance> entry : agentInstanceMap.entrySet()) {
            AgentInstance agentInstance = entry.getValue();
            if (agentInstance.isElastic()) {
                ElasticAgentMetadata metadata = agentInstance.elasticAgentMetadata();
                map.add(metadata.elasticPluginId(), metadata);
            }
        }

        return map;
    }

    public AgentInstance findElasticAgent(final String elasticAgentId, final String elasticPluginId) {
        Collection<AgentInstance> values = agentInstanceMap.values().stream().filter(agentInstance -> {
            if (!agentInstance.isElastic()) {
                return false;
            }

            ElasticAgentMetadata elasticAgentMetadata = agentInstance.elasticAgentMetadata();
            return elasticAgentMetadata.elasticAgentId().equals(elasticAgentId) && elasticAgentMetadata.elasticPluginId().equals(elasticPluginId);

        }).collect(Collectors.toList());

        if (values.size() == 0) {
            return null;
        }

        if (values.size() > 1) {
            Collection<String> uuids = values.stream().map(AgentInstance::getUuid).collect(Collectors.toList());
            throw new IllegalStateException(String.format("Found multiple agents with the same elastic agent id [%s]", StringUtils.join(uuids, ", ")));
        }

        return values.iterator().next();
    }

    public List<Agent> findPendingAgents(List<String> uuids) {
        return uuids.stream().map(this::findAgent).filter(AgentInstance::isPending)
                             .map(agentInstance -> agentInstance.getAgent().deepClone())
                             .collect(Collectors.toList());
    }

    public List<String> findPendingAgentUUIDs(List<String> uuids) {
        return uuids.stream().map(this::findAgent)
                             .filter(AgentInstance::isPending)
                             .map(AgentInstance::getUuid)
                             .collect(Collectors.toList());
    }

    public List<String> findElasticAgentUUIDs(List<String> uuids) {
        return uuids.stream().map(this::findAgent)
                             .filter(AgentInstance::isElastic)
                             .map(AgentInstance::getUuid)
                             .collect(Collectors.toList());
    }

    public List<String> findNullAgentUUIDs(List<String> uuids) {
        return uuids.stream().map(this::findAgent)
                             .filter(AgentInstance::isNullAgent)
                             .map(AgentInstance::getUuid)
                             .collect(Collectors.toList());
    }
}
