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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.Agents;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.listener.AgentChangeListener;
import com.thoughtworks.go.presentation.TriStateSelection;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.security.Registration;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.domain.ElasticAgentMetadata;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.AgentDao;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.server.ui.AgentViewModel;
import com.thoughtworks.go.server.ui.AgentsViewModel;
import com.thoughtworks.go.server.util.UuidGenerator;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TriState;
import com.thoughtworks.go.utils.Timeout;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;

import static java.lang.String.format;


@Service
public class AgentService {

    private final SystemEnvironment systemEnvironment;
    private final AgentConfigService agentConfigService;
    private final SecurityService securityService;
    private final EnvironmentConfigService environmentConfigService;
    private final GoConfigService goConfigService;
    private final UuidGenerator uuidGenerator;
    private final ServerHealthService serverHealthService;
    private final AgentDao agentDao;

    private AgentInstances agentInstances;

    private static final Logger LOGGER = Logger.getLogger(AgentService.class);

    @Autowired
    public AgentService(AgentConfigService agentConfigService, SystemEnvironment systemEnvironment, final EnvironmentConfigService environmentConfigService,
                        final GoConfigService goConfigService, SecurityService securityService, AgentDao agentDao, UuidGenerator uuidGenerator, ServerHealthService serverHealthService,
                        final EmailSender emailSender) {
        this(agentConfigService, systemEnvironment, null, environmentConfigService, goConfigService, securityService, agentDao, uuidGenerator, serverHealthService);
        this.agentInstances = new AgentInstances(new AgentRuntimeStatus.ChangeListener() {
            public void statusUpdateRequested(AgentRuntimeInfo runtimeInfo, AgentRuntimeStatus newStatus) {
            }
        });
    }

    AgentService(AgentConfigService agentConfigService, SystemEnvironment systemEnvironment, AgentInstances agentInstances,
                 EnvironmentConfigService environmentConfigService, GoConfigService goConfigService, SecurityService securityService, AgentDao agentDao, UuidGenerator uuidGenerator,
                 ServerHealthService serverHealthService) {
        this.systemEnvironment = systemEnvironment;
        this.agentConfigService = agentConfigService;
        this.environmentConfigService = environmentConfigService;
        this.goConfigService = goConfigService;
        this.securityService = securityService;
        this.agentInstances = agentInstances;
        this.agentDao = agentDao;
        this.uuidGenerator = uuidGenerator;
        this.serverHealthService = serverHealthService;
    }

    public void initialize() {
        this.sync(this.agentConfigService.agents());
        agentConfigService.register(new AgentChangeListener(this));
    }

    public void sync(Agents agents) {
        agentInstances.sync(agents);
    }

    public List<String> getUniqueAgentNames() {
        return new ArrayList<>(agentInstances.getAllHostNames());
    }

    public List<String> getUniqueIPAddresses() {
        return new ArrayList<>(agentInstances.getAllIpAddresses());
    }

    public List<String> getUniqueAgentOperatingSystems() {
        return new ArrayList<>(agentInstances.getAllOperatingSystems());
    }

    @Deprecated // used by old code, stop using AgentViewModel
    public AgentsViewModel agents() {
        return toAgentViewModels(agentInstances.allAgents());
    }

    public Map<AgentInstance, Collection<String>> agentEnvironmentMap() {
        Map<AgentInstance, Collection<String>> allAgents = new LinkedHashMap<>();
        for (AgentInstance agentInstance : agentInstances.allAgents()) {
            allAgents.put(agentInstance, environmentConfigService.environmentsFor(agentInstance.getUuid()));
        }
        return allAgents;
    }

    public AgentsViewModel registeredAgents() {
        return toAgentViewModels(agentInstances.findRegisteredAgents());
    }

    private AgentsViewModel toAgentViewModels(AgentInstances instances) {
        AgentsViewModel agents = new AgentsViewModel();
        for (AgentInstance instance : instances) {
            agents.add(toAgentViewModel(instance));
        }
        return agents;
    }

    private AgentViewModel toAgentViewModel(AgentInstance instance) {
        return new AgentViewModel(instance, environmentConfigService.environmentsFor(instance.getUuid()));
    }

    public AgentInstances findRegisteredAgents() {
        return agentInstances.findRegisteredAgents();
    }

    private boolean isUnknownAgent(AgentInstance agentInstance, OperationResult operationResult) {
        if (agentInstance.isNullAgent()) {
            String agentNotFoundMessage = String.format("Agent '%s' not found", agentInstance.getUuid());
            operationResult.notFound("Agent not found.", agentNotFoundMessage, HealthStateType.general(HealthStateScope.GLOBAL));
            return true;
        }
        return false;
    }

    private boolean hasOperatePermission(Username username, OperationResult operationResult) {
        if (!securityService.hasOperatePermissionForAgents(username)) {
            String message = "Unauthorized to operate on agent";
            operationResult.unauthorized(message, message, HealthStateType.general(HealthStateScope.GLOBAL));
            return false;
        }
        return true;
    }

    public AgentInstance updateAgentAttributes(Username username, HttpOperationResult result, String uuid, String newHostname, String resources, String environments, TriState enable) {
        if (!hasOperatePermission(username, result)) {
            return null;
        }

        AgentInstance agentInstance = findAgent(uuid);
        if (isUnknownAgent(agentInstance, result)) {
            return null;
        }

        AgentConfig agentConfig = agentConfigService.updateAgentAttributes(uuid, username, newHostname, resources, environments, enable, agentInstances, result);
        if (agentConfig != null) {
            return AgentInstance.createFromConfig(agentConfig, systemEnvironment);
        }
        return null;
    }

    public void bulkUpdateAgentAttributes(Username username, LocalizedOperationResult result, List<String> uuids, List<String> resourcesToAdd, List<String> resourcesToRemove, List<String> environmentsToAdd, List<String> environmentsToRemove, TriState enable) {
        agentConfigService.bulkUpdateAgentAttributes(username, result, uuids, resourcesToAdd, resourcesToRemove, environmentsToAdd, environmentsToRemove, enable);
    }

    public void enableAgents(Username username, OperationResult operationResult, List<String> uuids) {
        if (!hasOperatePermission(username, operationResult)) {
            return;
        }
        List<AgentInstance> agents = new ArrayList<>();
        if (!populateAgentInstancesForUUIDs(operationResult, uuids, agents)) {
            return;
        }
        try {
            agentConfigService.enableAgents(username, agents.toArray((new AgentInstance[0])));
            operationResult.ok(String.format("Enabled %s agent(s)", uuids.size()));
        } catch (Exception e) {
            operationResult.internalServerError("Enabling agents failed:" + e.getMessage(), HealthStateType.general(HealthStateScope.GLOBAL));
        }
    }

    public void disableAgents(Username username, OperationResult operationResult, List<String> uuids) {
        if (!hasOperatePermission(username, operationResult)) {
            return;
        }
        List<AgentInstance> agents = new ArrayList<>();
        if (!populateAgentInstancesForUUIDs(operationResult, uuids, agents)) {
            return;
        }
        try {
            agentConfigService.disableAgents(username, agents.toArray(new AgentInstance[0]));
            operationResult.ok(String.format("Disabled %s agent(s)", uuids.size()));
        } catch (Exception e) {
            operationResult.internalServerError("Disabling agents failed:" + e.getMessage(), HealthStateType.general(HealthStateScope.GLOBAL));
        }
    }

    private boolean populateAgentInstancesForUUIDs(OperationResult operationResult, List<String> uuids, List<AgentInstance> agents) {
        for (String uuid : uuids) {
            AgentInstance agentInstance = findAgentAndRefreshStatus(uuid);
            if (isUnknownAgent(agentInstance, operationResult)) {
                return false;
            }
            agents.add(agentInstance);
        }
        return true;
    }

    public void deleteAgents(Username username, HttpOperationResult operationResult, List<String> uuids) {
        if (!hasOperatePermission(username, operationResult)) {
            return;
        }
        List<AgentInstance> agents = new ArrayList<>();
        if (!populateAgentInstancesForUUIDs(operationResult, uuids, agents)) {
            return;
        }

        List<AgentInstance> failedToDeleteAgents = new ArrayList<>();
        for (AgentInstance agentInstance : agents) {
            if (!agentInstance.canBeDeleted()) {
                failedToDeleteAgents.add(agentInstance);
            }
        }

        agents.removeAll(failedToDeleteAgents);

        try {
            agentConfigService.deleteAgents(username, agents.toArray(new AgentInstance[0]));

            if (failedToDeleteAgents.isEmpty()) {
                operationResult.ok(String.format("Deleted %s agent(s).", agents.size()));
            } else {
                operationResult.notAcceptable(String.format("Deleted %s agent(s). Failed to delete %s agent(s), as agent(s) might not be disabled or are still building.", agents.size(), failedToDeleteAgents.size()),
                        HealthStateType.general(HealthStateScope.GLOBAL));
            }
        } catch (Exception e) {
            operationResult.internalServerError("Deleting agents failed:" + e.getMessage(), HealthStateType.general(HealthStateScope.GLOBAL));
        }
    }

    public void modifyResources(Username username, HttpOperationResult operationResult, List<String> uuids, List<TriStateSelection> selections) {
        if (!hasOperatePermission(username, operationResult)) {
            return;
        }
        List<AgentInstance> agents = new ArrayList<>();
        if (!populateAgentInstancesForUUIDs(operationResult, uuids, agents)) {
            return;
        }
        try {
            agentConfigService.modifyResources(agents.toArray(new AgentInstance[0]), selections, username);
            operationResult.ok(String.format("Resource(s) modified on %s agent(s)", uuids.size()));
        } catch (Exception e) {
            operationResult.notAcceptable("Could not modify resources:" + e.getMessage(), HealthStateType.general(HealthStateScope.GLOBAL));
        }
    }

    public void modifyEnvironments(Username username, HttpOperationResult operationResult, List<String> uuids, List<TriStateSelection> selections) {
        if (!hasOperatePermission(username, operationResult)) {
            return;
        }
        List<AgentInstance> agents = new ArrayList<>();
        if (!populateAgentInstancesForUUIDs(operationResult, uuids, agents)) {
            return;
        }

        try {
            environmentConfigService.modifyEnvironments(agents, selections);
            operationResult.ok(String.format("Environment(s) modified on %s agent(s)", uuids.size()));
        } catch (Exception e) {
            operationResult.notAcceptable("Could not modify environments:" + e.getMessage(), HealthStateType.general(HealthStateScope.GLOBAL));
        }
    }

    public void updateRuntimeInfo(AgentRuntimeInfo info) {
        if (!info.hasCookie()) {
            LOGGER.warn(format("Agent [%s] has no cookie set", info.agentInfoDebugString()));
            throw new AgentNoCookieSetException(format("Agent [%s] has no cookie set", info.agentInfoDebugString()));
        }
        if (info.hasDuplicateCookie(agentDao.cookieFor(info.getIdentifier()))) {
            LOGGER.warn(format("Found agent [%s] with duplicate uuid. Please check the agent installation.", info.agentInfoDebugString()));
            serverHealthService.update(
                    ServerHealthState.warning(format("[%s] has duplicate unique identifier which conflicts with [%s]", info.agentInfoForDisplay(), findAgentAndRefreshStatus(info.getUUId()).agentInfoForDisplay()),
                            "Please check the agent installation. Click <a href='http://www.go.cd/documentation/user/current/faq/agent_guid_issue.html' target='_blank'>here</a> for more info.",
                            HealthStateType.duplicateAgent(HealthStateScope.forAgent(info.getCookie())), Timeout.THIRTY_SECONDS));
            throw new AgentWithDuplicateUUIDException(format("Agent [%s] has invalid cookie", info.agentInfoDebugString()));
        }
        AgentInstance agentInstance = findAgentAndRefreshStatus(info.getUUId());
        if (agentInstance.isIpChangeRequired(info.getIpAdress())) {
            AgentConfig agentConfig = agentInstance.agentConfig();
            LOGGER.warn(
                    String.format("Agent with UUID [%s] changed IP Address from [%s] to [%s]",
                            info.getUUId(), agentConfig.getIpAddress(), info.getIpAdress()));
            Username userName = agentUsername(info.getUUId(), info.getIpAdress(), agentConfig.getHostNameForDispaly());
            agentConfigService.updateAgentIpByUuid(agentConfig.getUuid(), info.getIpAdress(), userName);
        }
        agentInstances.updateAgentRuntimeInfo(info);
    }

    public Username agentUsername(String uuId, String ipAddress, String hostNameForDisplay) {
        return new Username(String.format("agent_%s_%s_%s", uuId, ipAddress, hostNameForDisplay));
    }

    public Registration requestRegistration(Username username, AgentRuntimeInfo agentRuntimeInfo) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Agent is requesting registration " + agentRuntimeInfo);
        }
        AgentInstance agentInstance = agentInstances.register(agentRuntimeInfo);
        Registration registration = agentInstance.assignCertification();
        if (agentInstance.isRegistered()) {
            agentConfigService.saveOrUpdateAgent(agentInstance, username);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("New Agent approved " + agentRuntimeInfo);
            }
        }
        return registration;
    }

    @Deprecated
    public void approve(String uuid) {
        AgentInstance agentInstance = findAgentAndRefreshStatus(uuid);
        agentConfigService.approvePendingAgent(agentInstance);
    }

    public File agentJarFile() {
        return systemEnvironment.getAgentJarFile();
    }

    public InputStream agentJarInputStream() throws FileNotFoundException {
        return new FileInputStream(agentJarFile());
    }

    public InputStream agentLauncherJarInputStream() throws FileNotFoundException {
        return new FileInputStream(SystemEnvironment.AGENT_LAUNCHER_JAR_PATH);
    }

    public void notifyJobCancelledEvent(String agentId) {
        agentInstances.updateAgentAboutCancelledBuild(agentId, true);
    }

    public AgentInstance findAgentAndRefreshStatus(String uuid) {
        return agentInstances.findAgentAndRefreshStatus(uuid);
    }

    public AgentInstance findAgent(String uuid) {
        return agentInstances.findAgent(uuid);
    }

    public void clearAll() {
        agentInstances.clearAll();
    }

    /**
     * called from spring timer
     */
    public void refresh() {
        agentInstances.refresh();
    }

    public void building(String uuid, AgentBuildingInfo agentBuildingInfo) {
        agentInstances.building(uuid, agentBuildingInfo);
    }

    public List<TriStateSelection> getResourceSelections(List<String> uuids) {
        return TriStateSelection.forAgentsResources(goConfigService.getAllResources(), agentConfigService.findAgents(uuids));
    }

    public List<TriStateSelection> getEnvironmentSelections(List<String> uuids) {
        return TriStateSelection.forAgentsEnvironmens(environmentConfigService.getEnvironments(), agentConfigService.findAgents(uuids));
    }

    public String assignCookie(AgentIdentifier identifier) {
        String cookie = uuidGenerator.randomUuid();
        agentDao.associateCookie(identifier, cookie);
        return cookie;
    }

    public AgentsViewModel filter(List<String> uuids) {
        AgentsViewModel viewModels = new AgentsViewModel();
        for (AgentInstance agentInstance : agentInstances.filter(uuids)) {
            viewModels.add(new AgentViewModel(agentInstance));
        }
        return viewModels;
    }

    public AgentViewModel findAgentViewModel(String uuid) {
        return toAgentViewModel(findAgentAndRefreshStatus(uuid));
    }

    public LinkedMultiValueMap<String, ElasticAgentMetadata> allElasticAgents() {
        return agentInstances.allElasticAgentsGroupedByPluginId();
    }

    public AgentInstance findElasticAgent(String elasticAgentId, String elasticPluginId) {
        return agentInstances.findElasticAgent(elasticAgentId, elasticPluginId);
    }

    public AgentInstances findEnabledAgents() {
        return agentInstances.findEnabledAgents();
    }

    public AgentInstances findDisabledAgents() {
        return agentInstances.findDisabledAgents();
    }
}
