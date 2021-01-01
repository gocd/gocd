/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.AgentInstance.FilterBy;
import com.thoughtworks.go.domain.AgentStatus;
import com.thoughtworks.go.domain.NullAgentInstance;
import com.thoughtworks.go.domain.exception.MaxPendingAgentsLimitReachedException;
import com.thoughtworks.go.listener.AgentStatusChangeListener;
import com.thoughtworks.go.server.service.AgentBuildingInfo;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static com.thoughtworks.go.domain.AgentInstance.createFromAgent;
import static com.thoughtworks.go.util.SystemEnvironment.MAX_PENDING_AGENTS_ALLOWED;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.join;

public class AgentInstances implements Iterable<AgentInstance> {
    private SystemEnvironment systemEnvironment;

    private Map<String, AgentInstance> uuidToAgentInstanceMap = new ConcurrentHashMap<>();

    private AgentStatusChangeListener agentStatusChangeListener;

    public AgentInstances(AgentStatusChangeListener listener) {
        this.agentStatusChangeListener = listener;
        this.systemEnvironment = new SystemEnvironment();
    }

    public AgentInstances(SystemEnvironment sysEnv, AgentStatusChangeListener listener, AgentInstance... agentInstances) {
        this(listener);
        this.systemEnvironment = sysEnv;

        if (agentInstances != null) {
            Stream.of(agentInstances).forEach(this::add);
        }
    }

    public void add(AgentInstance agent) {
        uuidToAgentInstanceMap.put(agent.getAgent().getUuid(), agent);
    }

    public void updateAgentAboutCancelledBuild(String uuid, boolean isCancelled) {
        AgentInstance agentInstance = findAgentAndRefreshStatus(uuid);
        if (isCancelled) {
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
        AgentInstance agentInstance = uuidToAgentInstanceMap.get(uuid);
        return agentInstance == null ? new NullAgentInstance(uuid) : agentInstance;
    }

    public void removeAgent(String uuid) {
        uuidToAgentInstanceMap.remove(uuid);
    }

    public void clearAll() {
        uuidToAgentInstanceMap.clear();
    }

    public AgentInstances getAllAgents() {
        AgentInstances allAgentInstances = new AgentInstances(agentStatusChangeListener);
        currentInstances().forEach(allAgentInstances::add);
        return allAgentInstances;
    }

    public AgentInstances findRegisteredAgents() {
        this.refresh();
        AgentInstances registeredInstances = new AgentInstances(agentStatusChangeListener);

        synchronized (uuidToAgentInstanceMap) {
            stream(this.spliterator(), false)
                    .filter(agentInstance -> agentInstance.getStatus().isRegistered())
                    .forEach(registeredInstances::add);
        }

        return registeredInstances;
    }

    @Override
    public Iterator<AgentInstance> iterator() {
        return currentInstances().iterator();
    }

    public boolean isEmpty() {
        return uuidToAgentInstanceMap.isEmpty();
    }

    public Integer size() {
        return uuidToAgentInstanceMap.size();
    }

    public void refresh() {
        currentInstances().forEach(AgentInstance::refresh);
        getRemovableAgents().forEach(agentInstance -> removeAgent(agentInstance.getAgent().getUuid()));
    }

    public List<AgentInstance> agentsStuckInCancel() {
        return currentInstances().stream().filter(AgentInstance::isStuckInCancel).collect(toList());
    }

    public void syncAgentInstancesFrom(Agents agentsFromDB) {
        for (Agent agentFromDB : agentsFromDB) {
            String uuid = agentFromDB.getUuid();
            if (uuidToAgentInstanceMap.containsKey(uuid)) {
                uuidToAgentInstanceMap.get(uuid).syncAgentFrom(agentFromDB);
            } else {
                AgentInstance newAgent = createFromAgent(agentFromDB, new SystemEnvironment(), agentStatusChangeListener);
                uuidToAgentInstanceMap.put(uuid, newAgent);
            }
        }

        synchronized (uuidToAgentInstanceMap) {
            List<String> uuids = new ArrayList<>();
            for (String uuid : uuidToAgentInstanceMap.keySet()) {
                AgentInstance instance = uuidToAgentInstanceMap.get(uuid);
                if (!(instance.getStatus() == AgentStatus.Pending)) {
                    if (!agentsFromDB.hasAgent(uuid)) {
                        uuids.add(uuid);
                    }
                }
            }
            uuids.forEach(uuidToAgentInstanceMap::remove);
        }
    }

    public boolean hasAgent(String uuid) {
        AgentInstance agentInstance = findAgentAndRefreshStatus(uuid);
        return !(agentInstance instanceof NullAgentInstance);
    }

    public AgentInstance register(AgentRuntimeInfo runtimeInfo) {
        AgentInstance agentInstance = findAgentAndRefreshStatus(runtimeInfo.getUUId());
        if (!agentInstance.isRegistered()) {
            if (isMaxPendingAgentsLimitReached()) {
                throw new MaxPendingAgentsLimitReachedException(systemEnvironment.get(MAX_PENDING_AGENTS_ALLOWED));
            }
            agentInstance = AgentInstance.createFromLiveAgent(runtimeInfo, systemEnvironment, agentStatusChangeListener);
            this.add(agentInstance);
        }
        agentInstance.update(runtimeInfo);
        return agentInstance;
    }

    public void updateAgentRuntimeInfo(AgentRuntimeInfo runtimeInfo) {
        AgentInstance agentInstance = this.findAgentAndRefreshStatus(runtimeInfo.getUUId());
        agentInstance.update(runtimeInfo);
    }

    public void building(String uuid, AgentBuildingInfo agentBuildingInfo) {
        findAgentAndRefreshStatus(uuid).building(agentBuildingInfo);
    }

    public List<AgentInstance> filter(List<String> uuids) {
        if (CollectionUtils.isEmpty(uuids)) {
            return emptyList();
        }

        return stream(this.spliterator(), false)
                .filter(agentInstance -> uuids.contains(agentInstance.getUuid()))
                .collect(toList());
    }

    public LinkedMultiValueMap<String, ElasticAgentMetadata> getAllElasticAgentsGroupedByPluginId() {
        LinkedMultiValueMap<String, ElasticAgentMetadata> map = new LinkedMultiValueMap<>();

        for (Map.Entry<String, AgentInstance> entry : uuidToAgentInstanceMap.entrySet()) {
            AgentInstance agentInstance = entry.getValue();
            if (agentInstance.isElastic()) {
                ElasticAgentMetadata metadata = agentInstance.elasticAgentMetadata();
                map.add(metadata.elasticPluginId(), metadata);
            }
        }

        return map;
    }

    public AgentInstance findElasticAgent(final String elasticAgentId, final String elasticPluginId) {
        Collection<AgentInstance> agentInstances = uuidToAgentInstanceMap.values();

        List<AgentInstance> matchingElasticInstances = agentInstances.stream()
                .filter(agentInstance -> agentInstance.isElastic()
                        && agentInstance.elasticAgentMetadata().elasticAgentId().equals(elasticAgentId)
                        && agentInstance.elasticAgentMetadata().elasticPluginId().equals(elasticPluginId))
                .collect(toList());

        if (CollectionUtils.isEmpty(matchingElasticInstances)) {
            return null;
        }

        if (matchingElasticInstances.size() > 1) {
            Collection<String> uuids = matchingElasticInstances.stream().map(AgentInstance::getUuid).collect(toList());
            throw new IllegalStateException(format("Found multiple agents with the same elastic agent id [%s]", join(uuids, ", ")));
        }

        return matchingElasticInstances.iterator().next();
    }

    public List<Agent> filterPendingAgents(List<String> uuids) {
        return (CollectionUtils.isEmpty(uuids) ? new ArrayList<String>() : uuids)
                .stream()
                .map(this::findAgent)
                .filter(this::isPendingAndNotNullInstance)
                .map(agentInstance -> agentInstance.getAgent().deepClone())
                .collect(toList());
    }

    public List<String> filterBy(List<String> uuids, FilterBy filter) {
        return (CollectionUtils.isEmpty(uuids) ? new ArrayList<String>() : uuids)
                .stream()
                .map(this::findAgent)
                .filter(agentInstance -> agentInstance.matches(filter))
                .map(AgentInstance::getUuid)
                .collect(toList());
    }

    private boolean isPendingAndNotNullInstance(AgentInstance agentInstance) {
        return agentInstance.isPending() && !agentInstance.isNullAgent();
    }

    private List<AgentInstance> getRemovableAgents() {
        return stream(this.spliterator(), false)
                .filter(AgentInstance::canRemove)
                .collect(toList());
    }

    private Collection<AgentInstance> currentInstances() {
        return new TreeSet<>(uuidToAgentInstanceMap.values());
    }

    private boolean isMaxPendingAgentsLimitReached() {
        Integer maxPendingAgentsAllowed = systemEnvironment.get(MAX_PENDING_AGENTS_ALLOWED);
        int pendingAgentsCount = this.size() - findRegisteredAgents().size();
        return pendingAgentsCount >= maxPendingAgentsAllowed;
    }
}
