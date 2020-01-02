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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.AgentStatus;
import com.thoughtworks.go.domain.NullAgentInstance;
import com.thoughtworks.go.listener.AgentStatusChangeListener;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.service.AgentBuildingInfo;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.thoughtworks.go.domain.AgentInstance.createFromLiveAgent;
import static com.thoughtworks.go.server.service.AgentRuntimeInfo.fromServer;
import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;

public class AgentInstanceMother {

    public static AgentInstance local(SystemEnvironment systemEnvironment) {
        return AgentInstance.createFromAgent(new Agent("uuid-local", "localhost", "127.0.0.1"), systemEnvironment, null);
    }

    public static AgentInstance localInstance(SystemEnvironment systemEnvironment, String uuid, String hostname) {
        return AgentInstance.createFromAgent(new Agent(uuid, hostname, "127.0.0.1"), systemEnvironment, null);
    }

    public static AgentInstance idle() {
        return idle(new Date(), "CCeDev01");
    }

    public static AgentInstance nullInstance() {
        return new NullAgentInstance("null-agent-instance" + UUID.randomUUID());
    }

    public static AgentInstance idleWith(String uuid) {
        final AgentInstance agentInstance = idle();
        agentInstance.syncAgentFrom(new Agent(uuid, agentInstance.getHostname(), agentInstance.getIpAddress()));
        return agentInstance;
    }

    public static AgentInstance disabledWith(String uuid) {
        final AgentInstance disabled = disabled();
        disabled.syncAgentFrom(new Agent(uuid, disabled.getHostname(), disabled.getIpAddress()));
        return disabled;
    }

    public static AgentInstance idleWith(String uuid, String hostname, String ipAddress, String location, long space, String os, List<String> resourceList) {

        Agent agent = new Agent(uuid, hostname, ipAddress);
        agent.setResourcesFromList(resourceList);

        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, location, "cookie");
        agentRuntimeInfo.idle();
        agentRuntimeInfo.setUsableSpace(space);
        agentRuntimeInfo.setOperatingSystem(os);

        AgentInstance agentInstance = createFromLiveAgent(agentRuntimeInfo, new SystemEnvironment(), mock(AgentStatusChangeListener.class));
        agentInstance.idle();
        agentInstance.update(agentRuntimeInfo);
        agentInstance.syncAgentFrom(agent);

        return agentInstance;
    }

    public static AgentInstance idle(final Date lastHeardAt, final String hostname, SystemEnvironment systemEnvironment) {
        Agent idleAgentConfig = new Agent("uuid2", hostname, "10.18.5.1");
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(idleAgentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        agentRuntimeInfo.setLocation("/var/lib/foo");
        agentRuntimeInfo.idle();
        agentRuntimeInfo.setUsableSpace(10 * 1024l);
        AgentInstance agentInstance = createFromLiveAgent(agentRuntimeInfo, systemEnvironment, mock(AgentStatusChangeListener.class));
        agentInstance.idle();
        agentInstance.update(agentRuntimeInfo);
        ReflectionUtil.setField(agentInstance, "lastHeardTime", lastHeardAt);
        return agentInstance;

    }

    public static AgentInstance idle(final Date lastHeardAt, final String hostname) {
        return idle(lastHeardAt, hostname, new SystemEnvironment());
    }

    public static AgentInstance building() {
        return building("buildLocator");
    }

    public static AgentInstance building(String buildLocator) {
        return building(buildLocator, new SystemEnvironment());
    }

    public static AgentInstance building(String buildLocator, SystemEnvironment systemEnvironment) {
        Agent buildingAgentConfig = new Agent("uuid3", "CCeDev01", "10.18.5.1", singletonList("java"));
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(buildingAgentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        agentRuntimeInfo.busy(new AgentBuildingInfo("pipeline", buildLocator));
        AgentInstance building = AgentInstance.createFromAgent(buildingAgentConfig, systemEnvironment, mock(AgentStatusChangeListener.class));
        building.update(agentRuntimeInfo);
        return building;
    }

    public static AgentInstance pending(SystemEnvironment systemEnvironment) {
        Agent agent = new Agent("uuid4", "CCeDev03", "10.18.5.3", asList("db", "web"));

        AgentRuntimeInfo runtimeInfo = fromServer(agent, false,"/var/lib", 0L, "linux");
        AgentInstance agentInstance = createFromLiveAgent(runtimeInfo, systemEnvironment, mock(AgentStatusChangeListener.class));

        agentInstance.pending();
        agentInstance.update(runtimeInfo);
        agentInstance.pending();
        return agentInstance;
    }

    public static AgentInstance pending() {
        return pending(new SystemEnvironment());
    }

    public static AgentInstance pendingInstance() {
        AgentRuntimeInfo runtimeInfo = new AgentRuntimeInfo(new AgentIdentifier("CCeDev03", "10.18.5.3", "uuid4"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), null);
        return createFromLiveAgent(runtimeInfo, new SystemEnvironment(), mock(AgentStatusChangeListener.class));
    }

    public static AgentInstance updateUuid(AgentInstance agent, String uuid) {
        agent.syncAgentFrom(new Agent(uuid, agent.getHostname(), agent.getIpAddress()));
        return agent;
    }


    public static AgentInstance updateResources(AgentInstance agentInstance, String commaSeparatedResources) {
        agentInstance.getAgent().setResources(commaSeparatedResources);
        return agentInstance;
    }

    public static AgentInstance updateSpace(AgentInstance agentInstance, Long freespace) {
        return updateUsableSpace(agentInstance, freespace);
    }

    public static AgentInstance updateUsableSpace(AgentInstance agentInstance, Long freespace) {
        Agent agent = agentInstance.getAgent();
        agentInstance.update(fromServer(agent, true, agentInstance.getLocation(), freespace, "linux"));
        return agentInstance;
    }

    public static AgentInstance updateOperatingSystem(AgentInstance agentInstance, String operatingSystem) {
        return updateOS(agentInstance, operatingSystem);
    }

    public static AgentInstance updateOS(AgentInstance agentInstance, String operatingSystem) {
        Agent agent = agentInstance.getAgent();
        AgentRuntimeInfo newRuntimeInfo = fromServer(agent, true, agentInstance.getLocation(), agentInstance.getUsableSpace(), operatingSystem);
        newRuntimeInfo.setStatus(agentInstance.getStatus());
        agentInstance.update(newRuntimeInfo);
        return agentInstance;
    }

    public static AgentInstance updateIpAddress(AgentInstance agentInstance, String ip) {
        agentInstance.getAgent().setIpaddress(ip);
        return agentInstance;
    }


    public static AgentInstance updateLocation(AgentInstance agentInstance, String location) {
        Agent agent = agentInstance.getAgent();
        agentInstance.update(fromServer(agent, true, location, agentInstance.getUsableSpace(), "linux"));
        return agentInstance;
    }

    public static AgentInstance updateHostname(AgentInstance agentInstance, String hostname) {
        Agent original = agentInstance.getAgent();
        agentInstance.syncAgentFrom(new Agent(original.getUuid(), hostname, original.getIpaddress(), original.getResourcesAsList()));
        return agentInstance;
    }

    public static AgentInstance updateElasticAgentId(AgentInstance agentInstance, String elasticAgentId) {
        Agent agent = agentInstance.getAgent();
        agent.setElasticAgentId(elasticAgentId);

        agentInstance.syncAgentFrom(agent);
        return agentInstance;
    }

    public static AgentInstance updateElasticPluginId(AgentInstance agentInstance, String elasticPluginId) {
        Agent agent = agentInstance.getAgent();
        agent.setElasticPluginId(elasticPluginId);

        agentInstance.syncAgentFrom(agent);
        return agentInstance;
    }

    public static AgentInstance updateRuntimeStatus(AgentInstance agentInstance, AgentRuntimeStatus status) {
        Agent agent = agentInstance.getAgent();
        AgentRuntimeInfo newRuntimeInfo = fromServer(agent, true, agentInstance.getLocation(), agentInstance.getUsableSpace(), "linux");
        newRuntimeInfo.setRuntimeStatus(status);
        agentInstance.update(newRuntimeInfo);
        return agentInstance;
    }

    public static AgentInstance disabled() {
        return disabled("10.18.5.4");
    }

    public static AgentInstance disabled(String ip, SystemEnvironment systemEnvironment) {
        AgentInstance denied = AgentInstance.createFromAgent(new Agent("uuid5", "CCeDev04", ip), systemEnvironment,
                mock(AgentStatusChangeListener.class));
        denied.enable();
        denied.deny();
        return denied;
    }

    public static AgentInstance disabled(String ip) {
        return disabled(ip, new SystemEnvironment());
    }


    public static AgentInstance cancelled() {
        return cancel(building());
    }

    public static AgentInstance cancelled(String buildLocator) {
        return cancel(building(buildLocator));
    }

    public static AgentInstance cancel(AgentInstance building) {
        building.cancel();
        return building;
    }

    public static AgentInstance missing() {
        Agent agent = new Agent("1234", "localhost", "192.168.0.1");
        AgentInstance instance = AgentInstance.createFromAgent(agent, new SystemEnvironment(), mock(AgentStatusChangeListener.class));
        AgentRuntimeInfo newRuntimeInfo = AgentRuntimeInfo.initialState(agent);
        newRuntimeInfo.setStatus(AgentStatus.Missing);
        instance.update(newRuntimeInfo);
        return instance;
    }

    public static AgentInstance lostContact() {
        return lostContact("buildLocator");
    }

    public static AgentInstance lostContact(String buildLocator) {
        Agent agent = new Agent("1234", "localhost", "192.168.0.1");
        AgentInstance instance = AgentInstance.createFromAgent(agent, new SystemEnvironment(), mock(AgentStatusChangeListener.class));
        AgentRuntimeInfo newRuntimeInfo = AgentRuntimeInfo.initialState(agent);
        newRuntimeInfo.setStatus(AgentStatus.LostContact);
        newRuntimeInfo.setUsableSpace(1000L);
        newRuntimeInfo.setBuildingInfo(new AgentBuildingInfo("buildInfo", buildLocator));
        instance.update(newRuntimeInfo);
        return instance;
    }


    public static AgentInstance idle(String hostname) {
        return updateHostname(idle(new Date(), "CCeDev01"), hostname);
    }

    public static AgentInstance agentWithConfigErrors() {
        Agent agent = new Agent("uuid", "host", "IP", asList("foo%","bar$"));
        agent.validate();
        return AgentInstance.createFromAgent(agent, new SystemEnvironment(), mock(AgentStatusChangeListener.class));
    }
}
