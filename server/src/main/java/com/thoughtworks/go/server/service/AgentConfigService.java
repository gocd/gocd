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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.update.AgentsUpdateValidator;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.NullAgent;
import com.thoughtworks.go.listener.AgentChangeListener;
import com.thoughtworks.go.server.domain.Agent;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.AgentDao;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.util.TriState;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.thoughtworks.go.i18n.LocalizedMessage.entityConfigValidationFailed;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;

/**
 * @understands how to convert persistant Agent configuration to useful objects and back
 */
@Service
public class AgentConfigService {
    private GoConfigService goConfigService;
    private AgentDao agentDao;
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentConfigService.class.getName());

    @Autowired
    public AgentConfigService(GoConfigService goConfigService, AgentDao agentDao) {
        this.goConfigService = goConfigService;
        this.agentDao = agentDao;
    }

    public Agents agents() {
        return new Agents(agentDao.getAllAgentConfigs());
    }

    public AgentConfig agentByUuid(String uuid) {
        AgentConfig agentConfig = agentDao.agentConfigByUuid(uuid);
        if(agentConfig == null){
            agentConfig = NullAgent.createNullAgent();
        }
        return agentConfig;
    }

    public void register(AgentChangeListener agentChangeListener) {
        agentDao.registerListener(agentChangeListener);
    }

    public void disableAgents(Username currentUser, AgentInstance... agentInstance) {
        disableAgents(true, currentUser, agentInstance);
    }

    private void disableAgents(boolean disabled, Username currentUser, AgentInstance... instances) {
        List<String> uuids = Arrays.stream(instances).map(instance -> instance.getUuid()).collect(Collectors.toList());
        agentDao.changeDisabled(uuids, disabled);
    }

    public void deleteAgents(Username currentUser, AgentInstance... agentInstances) {
        ArrayList<String> uuids = new ArrayList<>();
        for (AgentInstance agentInstance : agentInstances) {
            uuids.add(agentInstance.getUuid());
        }
        List<Agent> allAgents = agentDao.getAllAgents(uuids);
        if (allAgents.size() != uuids.size()) {
            List<String> uuidsOfAgentsInDatabase = allAgents.stream().map(agent -> agent.getUuid()).collect(Collectors.toList());
            List<String> nonExistentAgentIds = uuids.stream().filter(uuid -> !uuidsOfAgentsInDatabase.contains(uuid)).collect(Collectors.toList());
//            if (nonExistentAgentIds != null) {
//                bomb("Unable to delete agent; Agent [" + uuid + "] not found.");
//            }
        }

        agentDao.bulkSoftDelete(uuids);
    }

    public void updateAgentIpByUuid(String uuid, String ipAddress, Username userName) {
        Agent agent = agentDao.agentByUuid(uuid);
        bombIfNull(agent, "Unable to set agent ipAddress; Agent [" + uuid + "] not found.");
        agent.setIpaddress(ipAddress);
        saveOrUpdate(agent, null);
    }

    public void bulkUpdateAgentAttributes(AgentInstances agentInstances, final Username username, final LocalizedOperationResult result,
                                          final List<String> uuids, EnvironmentConfigService environmentConfigService,
                                          final List<String> resourcesToAdd, final List<String> resourcesToRemove,
                                          final List<String> environmentsToAdd, final List<String> environmentsToRemove,
                                          final TriState enable) {
        AgentsUpdateValidator agentsUpdateValidator = new AgentsUpdateValidator(agentInstances,
                username, result, uuids, environmentsToAdd, environmentsToRemove, enable, resourcesToAdd, resourcesToRemove, goConfigService);
        try {
            if (agentsUpdateValidator.canContinue()) {
                agentsUpdateValidator.validate();
                agentDao.bulkUpdateAttributes(uuids, resourcesToAdd, resourcesToRemove, environmentsToAdd, environmentsToRemove, enable, agentInstances);
                result.setMessage("Updated agent(s) with uuid(s): [" + StringUtils.join(uuids, ", ") + "].");
            }
        } catch (Exception e) {
            LOGGER.error("There was an error bulk updating agents", e);
            if (e instanceof GoConfigInvalidException && !result.hasMessage()) {
                result.unprocessableEntity(entityConfigValidationFailed(Agents.class.getAnnotation(ConfigTag.class).value(), StringUtils.join(uuids, ","), e.getMessage()));
            } else if (!result.hasMessage()) {
                result.internalServerError("Server error occured. Check log for details.");
            }
        }
    }

    public boolean hasAgent(String uuid) {
        return agentDao.agentByUuid(uuid) != null;
    }

    public AgentConfig updateAgentAttributes(final String uuid, Username username, String hostname, String resources, String environments, TriState enable, AgentInstances agentInstances, HttpOperationResult result) {
        AgentConfig agentConfig;
        if (!hasAgent(uuid) && enable.isTrue()) {
            AgentInstance agentInstance = agentInstances.findAgent(uuid);
            agentConfig = agentInstance.agentConfig();
        } else {
            agentConfig = agentDao.agentConfigByUuid(uuid);
        }

        if (enable.isTrue()) {
            agentConfig.enable();
        }

        if (enable.isFalse()) {
            agentConfig.disable();
        }

        if (hostname != null) {
            agentConfig.setHostName(hostname);
        }

        if (resources != null) {
            agentConfig.setResources(new ResourceConfigs(resources));
        }

        if (environments != null) {
            agentConfig.setEnvironments(environments);
        }

        try {
            saveOrUpdate(agentConfig, username);

            if (!agentConfig.errors().isEmpty()) {
                result.unprocessibleEntity("Updating agent failed:", "", HealthStateType.general(HealthStateScope.GLOBAL));
            }
        }catch (Exception e){
            result.internalServerError("Updating agent failed: " + e.getMessage(), HealthStateType.general(HealthStateScope.GLOBAL));
            return null;
        }

        return agentConfig;
    }

    public void saveOrUpdateAgent(AgentInstance agentInstance, Username currentUser) {
        AgentConfig agentConfig = agentInstance.agentConfig();
        if (agentDao.agentByUuid(agentConfig.getUuid()) != null) {
            this.updateAgentApprovalStatus(agentConfig.getUuid(), agentConfig.isDisabled(), currentUser);
        } else {
            saveOrUpdate(agentConfig, currentUser);
        }
    }

    @Deprecated
    public void approvePendingAgent(AgentInstance agentInstance) {
        agentInstance.enable();
        if (hasAgent(agentInstance.getUuid())) {
            LOGGER.warn("Registered agent with the same uuid [{}] already approved.", agentInstance);
        } else {
            saveOrUpdate(agentInstance.agentConfig(), null);
        }
    }

    public void registerAgent(AgentConfig agentConfig, String agentAutoRegisterResources, String agentAutoRegisterEnvironments, HttpOperationResult result) {
        agentConfig.setResourceConfigs(new ResourceConfigs(agentAutoRegisterResources));
        agentConfig.setEnvironments(agentAutoRegisterEnvironments);
        saveOrUpdate(agentConfig, null);
    }

    public void updateEnvironments(EnvironmentConfig newEnvironmentConfig, List<String> environmentToAddToAgents, List<String> environmentToRemoveFromAgents, HttpLocalizedOperationResult result) {
        try {
            agentDao.bulkUpdateEnvironments(Arrays.asList(newEnvironmentConfig.name().toString()), environmentToAddToAgents, environmentToRemoveFromAgents);
        } catch (Exception e) {
            result.unprocessableEntity(String.format("Failed to update environments %s", e.getMessage()));
        }
    }

    public void updateAgentApprovalStatus(final String uuid, final Boolean isDenied, Username currentUser) {
        Agent agent = agentDao.agentByUuid(uuid);
        agent.setDisabled(isDenied);
        saveOrUpdate(agent, null);
    }

    public void saveOrUpdate(AgentConfig agentConfig, Username currentUser) {
        agentConfig.validate(null);
        if (agentConfig.errors().isEmpty()) {
            agentDao.saveOrUpdate(agentConfig);
        }
    }

    public void saveOrUpdate(Agent agent, Username currentUser) {
        agent.validate(null);
        if (agent.errors().isEmpty()) {
            agentDao.saveOrUpdate(agent);
        }
    }


    public List<String> allAgentUuids() {
        return agentDao.allAgentUuids();
    }
}
