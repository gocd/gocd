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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.AgentStatus;
import com.thoughtworks.go.listener.AgentStatusChangeListener;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.service.AgentBuildingInfo;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static org.mockito.Mockito.mock;

public class AgentInstanceMother {

    public static AgentInstance local(SystemEnvironment systemEnvironment) {
        return AgentInstance.createFromConfig(new AgentConfig("uuid-local", "localhost", "127.0.0.1"), systemEnvironment, null);
    }

    public static AgentInstance localInstance(SystemEnvironment systemEnvironment, String uuid, String hostname) {
        return AgentInstance.createFromConfig(new AgentConfig(uuid, hostname, "127.0.0.1"), systemEnvironment, null);
    }

    public static AgentInstance idle() {
        return idle(new Date(), "CCeDev01");
    }

    public static AgentInstance idleWith(String uuid) {
        final AgentInstance agentInstance = idle();
        agentInstance.syncConfig(new AgentConfig(uuid, agentInstance.getHostname(), agentInstance.getIpAddress()));
        return agentInstance;
    }

    public static AgentInstance idleWith(String uuid, String hostname, String ipAddress, String location, long space, String os, List<String> resources) {

        AgentConfig agentConfig = new AgentConfig(uuid, hostname, ipAddress);
        agentConfig.setResourceConfigs(new ResourceConfigs(resources.stream().map(ResourceConfig::new).collect(Collectors.toList())));

        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, location, "cookie");
        agentRuntimeInfo.idle();
        agentRuntimeInfo.setUsableSpace(space);
        agentRuntimeInfo.setOperatingSystem(os);

        AgentInstance agentInstance = AgentInstance.createFromLiveAgent(agentRuntimeInfo, new SystemEnvironment(), mock(AgentStatusChangeListener.class));
        agentInstance.idle();
        agentInstance.update(agentRuntimeInfo);
        agentInstance.syncConfig(agentConfig);

        return agentInstance;
    }

    public static AgentInstance idle(final Date lastHeardAt, final String hostname, SystemEnvironment systemEnvironment) {
        AgentConfig idleAgentConfig = new AgentConfig("uuid2", hostname, "10.18.5.1");
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(idleAgentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        agentRuntimeInfo.setLocation("/var/lib/foo");
        agentRuntimeInfo.idle();
        agentRuntimeInfo.setUsableSpace(10 * 1024l);
        AgentInstance agentInstance = AgentInstance.createFromLiveAgent(agentRuntimeInfo, systemEnvironment, mock(AgentStatusChangeListener.class));
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
        AgentConfig buildingAgentConfig = new AgentConfig("uuid3", "CCeDev01", "10.18.5.1", new ResourceConfigs("java"));
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(buildingAgentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        agentRuntimeInfo.busy(new AgentBuildingInfo("pipeline", buildLocator));
        AgentInstance building = AgentInstance.createFromConfig(buildingAgentConfig, systemEnvironment, mock(AgentStatusChangeListener.class));
        building.update(agentRuntimeInfo);
        return building;
    }

    public static AgentInstance pending(SystemEnvironment systemEnvironment) {
        AgentRuntimeInfo runtimeInfo = AgentRuntimeInfo.fromServer(new AgentConfig("uuid4", "CCeDev03", "10.18.5.3", new ResourceConfigs(new ResourceConfig("db"), new ResourceConfig("web"))), false,
                "/var/lib", 0L, "linux");
        AgentInstance pending = AgentInstance.createFromLiveAgent(runtimeInfo, systemEnvironment, mock(AgentStatusChangeListener.class));
        pending.pending();
        pending.update(runtimeInfo);
        pending.pending();
        return pending;
    }

    public static AgentInstance pending() {
        return pending(new SystemEnvironment());
    }

    public static AgentInstance pendingInstance() {
        AgentRuntimeInfo runtimeInfo = new AgentRuntimeInfo(new AgentIdentifier("CCeDev03", "10.18.5.3", "uuid4"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), null);
        return AgentInstance.createFromLiveAgent(runtimeInfo, new SystemEnvironment(), mock(AgentStatusChangeListener.class));
    }

    public static AgentInstance updateUuid(AgentInstance agent, String uuid) {
        agent.syncConfig(new AgentConfig(uuid, agent.getHostname(), agent.getIpAddress()));
        return agent;
    }


    public static AgentInstance updateResources(AgentInstance agentInstance, String resources) {
        agentInstance.agentConfig().setResourceConfigs(new ResourceConfigs(resources));
        return agentInstance;
    }

    public static AgentInstance updateSpace(AgentInstance agentInstance, Long freespace) {
        return updateUsableSpace(agentInstance, freespace);
    }

    public static AgentInstance updateUsableSpace(AgentInstance agentInstance, Long freespace) {
        AgentConfig agentConfig = agentInstance.agentConfig();
        agentInstance.update(AgentRuntimeInfo.fromServer(agentConfig, true, agentInstance.getLocation(), freespace, "linux"));
        return agentInstance;
    }

    public static AgentInstance updateOperatingSystem(AgentInstance agentInstance, String operatingSystem) {
        return updateOS(agentInstance, operatingSystem);
    }

    public static AgentInstance updateOS(AgentInstance agentInstance, String operatingSystem) {
        AgentConfig agentConfig = agentInstance.agentConfig();
        AgentRuntimeInfo newRuntimeInfo = AgentRuntimeInfo.fromServer(agentConfig, true, agentInstance.getLocation(), agentInstance.getUsableSpace(), operatingSystem);
        newRuntimeInfo.setStatus(agentInstance.getStatus());
        agentInstance.update(newRuntimeInfo);
        return agentInstance;
    }

    public static AgentInstance updateIpAddress(AgentInstance agentInstance, String ip) {
        agentInstance.agentConfig().setIpAddress(ip);
        return agentInstance;
    }


    public static AgentInstance updateLocation(AgentInstance agentInstance, String location) {
        AgentConfig agentConfig = agentInstance.agentConfig();
        agentInstance.update(AgentRuntimeInfo.fromServer(agentConfig, true, location, agentInstance.getUsableSpace(), "linux"));
        return agentInstance;
    }

    public static AgentInstance updateHostname(AgentInstance agentInstance, String hostname) {
        AgentConfig original = agentInstance.agentConfig();
        agentInstance.syncConfig(new AgentConfig(original.getUuid(), hostname, original.getIpAddress(), original.getResourceConfigs()));
        return agentInstance;
    }

    public static AgentInstance updateElasticAgentId(AgentInstance agentInstance, String elasticAgentId) {
        AgentConfig agentConfig = agentInstance.agentConfig();
        agentConfig.setElasticAgentId(elasticAgentId);

        agentInstance.syncConfig(agentConfig);
        return agentInstance;
    }

    public static AgentInstance updateElasticPluginId(AgentInstance agentInstance, String elasticPluginId) {
        AgentConfig agentConfig = agentInstance.agentConfig();
        agentConfig.setElasticPluginId(elasticPluginId);

        agentInstance.syncConfig(agentConfig);
        return agentInstance;
    }

    public static AgentInstance updateRuntimeStatus(AgentInstance agentInstance, AgentRuntimeStatus status) {
        AgentConfig agentConfig = agentInstance.agentConfig();
        AgentRuntimeInfo newRuntimeInfo = AgentRuntimeInfo.fromServer(agentConfig, true, agentInstance.getLocation(), agentInstance.getUsableSpace(), "linux");
        newRuntimeInfo.setRuntimeStatus(status);
        agentInstance.update(newRuntimeInfo);
        return agentInstance;
    }

    public static AgentInstance disabled() {
        return disabled("10.18.5.4");
    }

    public static AgentInstance disabled(String ip, SystemEnvironment systemEnvironment) {
        AgentInstance denied = AgentInstance.createFromConfig(new AgentConfig("uuid5", "CCeDev04", ip), systemEnvironment,
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
        AgentConfig agentConfig = new AgentConfig("1234", "localhost", "192.168.0.1");
        AgentInstance instance = AgentInstance.createFromConfig(agentConfig, new SystemEnvironment(), mock(AgentStatusChangeListener.class));
        AgentRuntimeInfo newRuntimeInfo = AgentRuntimeInfo.initialState(agentConfig);
        newRuntimeInfo.setStatus(AgentStatus.Missing);
        instance.update(newRuntimeInfo);
        return instance;
    }

    public static AgentInstance lostContact() {
        return lostContact("buildLocator");
    }

    public static AgentInstance lostContact(String buildLocator) {
        AgentConfig agentConfig = new AgentConfig("1234", "localhost", "192.168.0.1");
        AgentInstance instance = AgentInstance.createFromConfig(agentConfig, new SystemEnvironment(), mock(AgentStatusChangeListener.class));
        AgentRuntimeInfo newRuntimeInfo = AgentRuntimeInfo.initialState(agentConfig);
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
        ResourceConfig resourceConfig1 = new ResourceConfig("foo%");
        ResourceConfig resourceConfig2 = new ResourceConfig("bar$");
        AgentConfig agentConfig = new AgentConfig("uuid", "host", "IP", new ResourceConfigs(resourceConfig1, resourceConfig2));
        agentConfig.validateTree(ConfigSaveValidationContext.forChain(new BasicCruiseConfig()));
        return AgentInstance.createFromConfig(agentConfig, new SystemEnvironment(), mock(AgentStatusChangeListener.class));
    }
}
