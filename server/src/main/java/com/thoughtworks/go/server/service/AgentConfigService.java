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

import com.google.common.collect.Sets;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.update.AgentsEntityConfigUpdateCommand;
import com.thoughtworks.go.config.update.AgentsUpdateCommand;
import com.thoughtworks.go.config.update.ModifyEnvironmentCommand;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.listener.AgentChangeListener;
import com.thoughtworks.go.presentation.TriStateSelection;
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
import com.thoughtworks.go.validation.AgentConfigsUpdateValidator;
import com.thoughtworks.go.validation.ConfigUpdateValidator;
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
import static java.util.Arrays.asList;

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
        return agentDao.agentConfigByUuid(uuid);
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

    private void updateAgents(final UpdateConfigCommand command, final ConfigUpdateValidator validator, Username currentUser) {
        AgentsUpdateCommand updateCommand = new AgentsUpdateCommand(command, validator);
        goConfigService.updateConfig(updateCommand, currentUser);
    }

    public AgentConfig updateAgent(UpdateConfigCommand command, String uuid, HttpOperationResult result, Username currentUser) {
        AgentConfigsUpdateValidator validator = new AgentConfigsUpdateValidator(asList(uuid));
        try {
            updateAgents(command, validator, currentUser);
            result.ok(String.format("Updated agent with uuid %s.", uuid));
        } catch (Exception e) {
            if (e instanceof GoConfigInvalidException) {
                result.unprocessibleEntity("Updating agent failed:", e.getMessage(), HealthStateType.general(HealthStateScope.GLOBAL));
                GoConfigInvalidException goConfigInvalidException = (GoConfigInvalidException) e;
                return goConfigInvalidException.getCruiseConfig().agents().getAgentByUuid(uuid);
            } else {
                result.internalServerError("Updating agent failed: " + e.getMessage(), HealthStateType.general(HealthStateScope.GLOBAL));
                return null;
            }
        }
        return goConfigService.agents().getAgentByUuid(uuid);
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
        AgentsEntityConfigUpdateCommand agentsEntityConfigUpdateCommand = new AgentsEntityConfigUpdateCommand(agentInstances,
                username, result, uuids, environmentConfigService, environmentsToAdd, environmentsToRemove, enable,
                resourcesToAdd, resourcesToRemove, goConfigService);
        try {
            if (agentsEntityConfigUpdateCommand.canContinue(goConfigService.cruiseConfig())) {
                agentsEntityConfigUpdateCommand.validate();
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
        final GoConfigDao.CompositeConfigCommand command = new GoConfigDao.CompositeConfigCommand();
        if (!hasAgent(uuid) && enable.isTrue()) {
            AgentInstance agentInstance = agentInstances.findAgent(uuid);
            AgentConfig agentConfig = agentInstance.agentConfig();
            command.addCommand(new AddAgentCommand(agentConfig));
        }

        if (enable.isTrue()) {
            command.addCommand(new UpdateAgentApprovalStatus(uuid, false));
        }

        if (enable.isFalse()) {
            command.addCommand(new UpdateAgentApprovalStatus(uuid, true));
        }

        if (hostname != null) {
            command.addCommand(new UpdateAgentHostname(uuid, hostname, username.getUsername().toString()));
        }

        if (resources != null) {
            command.addCommand(new UpdateResourcesCommand(uuid, new ResourceConfigs(resources)));
        }

        if (environments != null) {
            Set<String> existingEnvironments = goConfigService.getCurrentConfig().getEnvironments().environmentsForAgent(uuid);
            Set<String> newEnvironments = new HashSet<>(asList(environments.split(",")));

            Set<String> environmentsToRemove = Sets.difference(existingEnvironments, newEnvironments);
            Set<String> environmentsToAdd = Sets.difference(newEnvironments, existingEnvironments);

            for (String environmentToRemove : environmentsToRemove) {
                command.addCommand(new ModifyEnvironmentCommand(uuid, environmentToRemove, TriStateSelection.Action.remove));
            }

            for (String environmentToAdd : environmentsToAdd) {
                command.addCommand(new ModifyEnvironmentCommand(uuid, environmentToAdd, TriStateSelection.Action.add));
            }
        }

        return updateAgent(command, uuid, result, username);
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

    public void updateEnvironments(EnvironmentConfig newEnvironmentConfig, List<String> oldAgentsUuids, List<String> newAgentsUuids, HttpLocalizedOperationResult result) {
        List<String> environmentToRemoveFromAgents = oldAgentsUuids.stream().filter(uuid -> !newAgentsUuids.contains(uuid)).collect(Collectors.toList());
        List<String> environmentToAddToAgents = newAgentsUuids.stream().filter(uuid -> !oldAgentsUuids.contains(uuid)).collect(Collectors.toList());
        try {
            if (!environmentToAddToAgents.isEmpty()) {
                agentDao.bulkAddEnvironments(environmentToAddToAgents, Arrays.asList(newEnvironmentConfig.name().toString()));
            }
            if (!environmentToRemoveFromAgents.isEmpty()) {
                agentDao.bulkRemoveEnvironments(environmentToRemoveFromAgents, Arrays.asList(newEnvironmentConfig.name().toString()));
            }
        } catch (Exception e) {
            result.unprocessableEntity(String.format("Failed to update environments %s", e.getMessage()));
        }
    }

    /**
     * @understands how to add an agent to the config file
     */
    public static class AddAgentCommand implements UpdateConfigCommand {
        private final AgentConfig agentConfig;

        public AddAgentCommand(AgentConfig agentConfig) {
            this.agentConfig = agentConfig;
        }

        @Override
        public CruiseConfig update(CruiseConfig cruiseConfig) {
            cruiseConfig.agents().add(agentConfig);
            return cruiseConfig;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            AddAgentCommand that = (AddAgentCommand) o;

            if (agentConfig != null ? !agentConfig.equals(that.agentConfig) : that.agentConfig != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return agentConfig != null ? agentConfig.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "AddAgentcommand{" +
                    "agentConfig=" + agentConfig +
                    '}';
        }

    }

    @Deprecated
    public void updateAgentResources(final String uuid, final ResourceConfigs resourceConfigs) {
        Agent agent = agentDao.agentByUuid(uuid);
        agent.setResources(resourceConfigs);
        saveOrUpdate(agent, null);
//        updateAgent(new UpdateResourcesCommand(uuid, resourceConfigs), uuid, Username.ANONYMOUS);
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

    public void modifyResources(AgentInstance[] agentInstances, List<TriStateSelection> selections, Username currentUser) {
        GoConfigDao.CompositeConfigCommand command = new GoConfigDao.CompositeConfigCommand();
        List<String> uuids = Arrays.stream(agentInstances).map(agentInstance -> agentInstance.getUuid()).collect(Collectors.toList());
        List<String> resourcesToAdd;
        List<String> resourcesToRemove;
        resourcesToAdd = selections.stream()
                .filter(selection -> selection.getAction() == TriStateSelection.Action.add)
                .map(selection -> selection.getValue())
                .collect(Collectors.toList());
        resourcesToRemove = selections.stream()
                .filter(selection -> selection.getAction() == TriStateSelection.Action.remove)
                .map(selection -> selection.getValue())
                .collect(Collectors.toList());
        agentDao.bulkUpdateAttributes(uuids, resourcesToAdd, resourcesToRemove, Collections.emptyList(), Collections.emptyList(), TriState.UNSET, null);
        AgentConfigsUpdateValidator validator = new AgentConfigsUpdateValidator(uuids);
        updateAgents(command, validator, currentUser);
    }


    /**
     * @understands how to update the agent approval status
     */
    public static class UpdateAgentApprovalStatus implements UpdateConfigCommand {
        private final String uuid;
        private final Boolean denied;

        public UpdateAgentApprovalStatus(String uuid, Boolean denied) {
            this.uuid = uuid;
            this.denied = denied;
        }

        @Override
        public CruiseConfig update(CruiseConfig cruiseConfig) {
            AgentConfig agentConfig = cruiseConfig.agents().getAgentByUuid(uuid);
            bombIfNull(agentConfig, "Unable to set agent approval status; Agent [" + uuid + "] not found.");
            agentConfig.setDisabled(denied);
            return cruiseConfig;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            UpdateAgentApprovalStatus that = (UpdateAgentApprovalStatus) o;

            if (denied != null ? !denied.equals(that.denied) : that.denied != null) {
                return false;
            }
            if (uuid != null ? !uuid.equals(that.uuid) : that.uuid != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = uuid != null ? uuid.hashCode() : 0;
            result = 31 * result + (denied != null ? denied.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "UpdateAgentApprovalStatus{" +
                    "uuid='" + uuid + '\'' +
                    ", denied=" + denied +
                    '}';
        }
    }

    public static class UpdateResourcesCommand implements UpdateConfigCommand {
        private final String uuid;
        private final ResourceConfigs resourceConfigs;

        public UpdateResourcesCommand(String uuid, ResourceConfigs resourceConfigs) {
            this.uuid = uuid;
            this.resourceConfigs = resourceConfigs;
        }

        @Override
        public CruiseConfig update(CruiseConfig cruiseConfig) {
            AgentConfig agentConfig = cruiseConfig.agents().getAgentByUuid(uuid);
            bombIfNull(agentConfig, "Unable to set agent resources; Agent [" + uuid + "] not found.");
            agentConfig.setResourceConfigs(resourceConfigs);
            return cruiseConfig;
        }
    }

    public static class UpdateAgentHostname implements UpdateConfigCommand, UserAware {
        private final String uuid;
        private final String hostname;
        private final String userName;

        public UpdateAgentHostname(String uuid, String hostname, String userName) {
            this.uuid = uuid;
            this.hostname = hostname;
            this.userName = userName;
        }

        @Override
        public CruiseConfig update(CruiseConfig cruiseConfig) {
            AgentConfig agentConfig = cruiseConfig.agents().getAgentByUuid(uuid);
            bombIfNull(agentConfig, "Unable to set agent hostname; Agent [" + uuid + "] not found.");
            agentConfig.setHostName(hostname);
            return cruiseConfig;
        }

        @Override
        public ConfigModifyingUser user() {
            return new ConfigModifyingUser(userName);
        }
    }

    public List<String> allAgentUuids() {
        return agentDao.allAgentUuids();
    }
}
