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

import com.google.common.collect.Sets;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.update.AgentsEntityConfigUpdateCommand;
import com.thoughtworks.go.config.update.AgentsUpdateCommand;
import com.thoughtworks.go.config.update.ModifyEnvironmentCommand;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.listener.AgentChangeListener;
import com.thoughtworks.go.presentation.TriStateSelection;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.util.TriState;
import com.thoughtworks.go.validation.AgentConfigsUpdateValidator;
import com.thoughtworks.go.validation.ConfigUpdateValidator;
import com.thoughtworks.go.validation.DoNothingValidator;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.List;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;
import static java.util.Arrays.asList;

/**
 * @understands how to convert persistant Agent configuration to useful objects and back
 */
@Service
public class AgentConfigService {
    private GoConfigService goConfigService;
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentConfigService.class.getName());

    @Autowired
    public AgentConfigService(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }

    public Agents agents() {
        return goConfigService.agents();
    }

    public void register(AgentChangeListener agentChangeListener) {
        goConfigService.register(agentChangeListener);
    }

    public void enableAgents(Username currentUser, AgentInstance... agentInstance) {
        disableAgents(false, currentUser, agentInstance);
    }

    public void disableAgents(Username currentUser, AgentInstance... agentInstance) {
        disableAgents(true, currentUser, agentInstance);
    }

    private void disableAgents(boolean disabled, Username currentUser, AgentInstance... instances) {
        GoConfigDao.CompositeConfigCommand command = new GoConfigDao.CompositeConfigCommand();
        ArrayList<String> uuids = new ArrayList<>();
        for (AgentInstance agentInstance : instances) {
            String uuid = agentInstance.getUuid();
            uuids.add(uuid);
            if (goConfigService.hasAgent(uuid)) {
                command.addCommand(new UpdateAgentApprovalStatus(uuid, disabled));
            } else {
                AgentConfig agentConfig = agentInstance.agentConfig();
                agentConfig.disable(disabled);
                command.addCommand(new AddAgentCommand(agentConfig));
            }
        }
        updateAgentWithoutValidations(command, currentUser);
    }

    protected static UpdateConfigCommand updateApprovalStatus(final String uuid, final Boolean isDenied) {
        return new UpdateAgentApprovalStatus(uuid, isDenied);
    }

    public void deleteAgents(Username currentUser, AgentInstance... agentInstances) {
        GoConfigDao.CompositeConfigCommand commandForDeletingAgents = commandForDeletingAgents(agentInstances);
        ArrayList<String> uuids = new ArrayList<>();
        for (AgentInstance agentInstance : agentInstances) {
            uuids.add(agentInstance.getUuid());
        }
        updateAgentWithoutValidations(commandForDeletingAgents, currentUser);
    }

    protected GoConfigDao.CompositeConfigCommand commandForDeletingAgents(AgentInstance... agentInstances) {
        GoConfigDao.CompositeConfigCommand command = new GoConfigDao.CompositeConfigCommand();
        for (AgentInstance agentInstance : agentInstances) {
            command.addCommand(deleteAgentCommand(agentInstance.getUuid()));
        }
        return command;
    }

    public static DeleteAgent deleteAgentCommand(String uuid) {
        return new DeleteAgent(uuid);
    }

    private void updateAgents(final UpdateConfigCommand command, final ConfigUpdateValidator validator, Username currentUser) {
        AgentsUpdateCommand updateCommand = new AgentsUpdateCommand(command, validator);
        goConfigService.updateConfig(updateCommand, currentUser);
    }

    public void updateAgent(UpdateConfigCommand command, String uuid, Username currentUser) {
        updateAgents(command, new AgentConfigsUpdateValidator(asList(uuid)), currentUser);
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

    private void updateAgentWithoutValidations(UpdateConfigCommand command, Username currentUser) {
        updateAgents(command, new DoNothingValidator(), currentUser);
    }


    /**
     * @understands how to delete agent
     */
    private static class DeleteAgent implements UpdateConfigCommand {
        private final String uuid;

        public DeleteAgent(String uuid) {
            this.uuid = uuid;
        }

        public CruiseConfig update(CruiseConfig cruiseConfig) {
            AgentConfig agentConfig = cruiseConfig.agents().getAgentByUuid(uuid);
            if (agentConfig.isNull()) {
                bomb("Unable to delete agent; Agent [" + uuid + "] not found.");
            }
            cruiseConfig.getEnvironments().removeAgentFromAllEnvironments(uuid);
            cruiseConfig.agents().remove(agentConfig);
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

            DeleteAgent that = (DeleteAgent) o;

            if (uuid != null ? !uuid.equals(that.uuid) : that.uuid != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return uuid != null ? uuid.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "DeleteAgent{" +
                    "uuid='" + uuid + '\'' +
                    '}';
        }
    }

    public void updateAgentIpByUuid(String uuid, String ipAddress, Username userName) {
        updateAgents(new UpdateAgentIp(uuid, ipAddress, userName), new AgentConfigsUpdateValidator(asList(uuid)), userName);
    }

    private static class UpdateAgentIp implements UpdateConfigCommand, UserAware {
        private final String uuid;
        private final String ipAddress;
        private final String userName;

        private UpdateAgentIp(String uuid, String ipAddress, Username userName) {
            this.uuid = uuid;
            this.ipAddress = ipAddress;
            this.userName = userName.getUsername().toString();
        }

        public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
            AgentConfig agentConfig = cruiseConfig.agents().getAgentByUuid(uuid);
            bombIfNull(agentConfig, "Unable to set agent ipAddress; Agent [" + uuid + "] not found.");
            agentConfig.setIpAddress(ipAddress);
            return cruiseConfig;
        }

        public ConfigModifyingUser user() {
            return new ConfigModifyingUser(userName);
        }
    }

    public void bulkUpdateAgentAttributes(final Username username, final LocalizedOperationResult result, final List<String> uuids, final List<String> resourcesToAdd, final List<String> resourcesToRemove, final List<String> environmentsToAdd, final List<String> environmentsToRemove, final TriState enable) {
        EntityConfigUpdateCommand<Agents> agentsEntityConfigUpdateCommand = new AgentsEntityConfigUpdateCommand(username, result, uuids, environmentsToAdd, environmentsToRemove, enable, resourcesToAdd, resourcesToRemove, goConfigService);

        try {
            goConfigService.updateConfig(agentsEntityConfigUpdateCommand, username);
            if(result.isSuccessful()){
                result.setMessage(LocalizedMessage.string("BULK_AGENT_UPDATE_SUCESSFUL", StringUtils.join(uuids, ", ")));
            }
        } catch (Exception e) {
            LOGGER.error("There was an error bulk updating agents", e);
            if (!result.hasMessage()) {
                result.internalServerError(LocalizedMessage.string("INTERNAL_SERVER_ERROR"));
            }
        }
    }

    public AgentConfig updateAgentAttributes(final String uuid, Username username, String hostname, String resources, String environments, TriState enable, AgentInstances agentInstances, HttpOperationResult result) {
        final GoConfigDao.CompositeConfigCommand command = new GoConfigDao.CompositeConfigCommand();
        if (!goConfigService.hasAgent(uuid) && enable.isTrue()) {
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
            command.addCommand(new UpdateResourcesCommand(uuid, new Resources(resources)));
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
        if (goConfigService.hasAgent(agentConfig.getUuid())) {
            this.updateAgentApprovalStatus(agentConfig.getUuid(), agentConfig.isDisabled(), currentUser);
        } else {
            this.addAgent(agentConfig, currentUser);
        }
    }

    @Deprecated
    public void approvePendingAgent(AgentInstance agentInstance) {
        agentInstance.enable();
        if (goConfigService.hasAgent(agentInstance.getUuid())) {
            LOGGER.warn("Registered agent with the same uuid [" + agentInstance + "] already approved.");
        } else {
            updateAgent(new AddAgentCommand(agentInstance.agentConfig()), agentInstance.getUuid(), new HttpOperationResult(), Username.ANONYMOUS);
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

        public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
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
    public void updateAgentResources(final String uuid, final Resources resources) {
        updateAgent(new UpdateResourcesCommand(uuid, resources), uuid, Username.ANONYMOUS);
    }

    public void updateAgentApprovalStatus(final String uuid, final Boolean isDenied, Username currentUser) {
        updateAgentWithoutValidations(new UpdateAgentApprovalStatus(uuid, isDenied), currentUser);
    }

    public void addAgent(AgentConfig agentConfig, Username currentUser) {
        updateAgent(new AddAgentCommand(agentConfig), agentConfig.getUuid(), currentUser);
    }

    public void modifyResources(AgentInstance[] agentInstances, List<TriStateSelection> selections, Username currentUser) {
        GoConfigDao.CompositeConfigCommand command = new GoConfigDao.CompositeConfigCommand();
        ArrayList<String> uuids = new ArrayList<>();
        for (AgentInstance agentInstance : agentInstances) {
            String uuid = agentInstance.getUuid();
            uuids.add(uuid);
            if (goConfigService.hasAgent(uuid)) {
                for (TriStateSelection selection : selections) {
                    command.addCommand(new ModifyResourcesCommand(uuid, new Resource(selection.getValue()), selection.getAction()));
                }
            }
        }
        AgentConfigsUpdateValidator validator = new AgentConfigsUpdateValidator(uuids);
        updateAgents(command, validator, currentUser);
    }

    private List<ConfigErrors> getAllErrors(Validatable v) {
        final List<ConfigErrors> allErrors = new ArrayList<>();
        new GoConfigGraphWalker(v).walk(new ErrorCollectingHandler(allErrors) {
            @Override
            public void handleValidation(Validatable validatable, ValidationContext context) {
                // do nothing here
            }
        });
        return allErrors;
    }


    public Agents findAgents(List<String> uuids) {
        return agents().filter(uuids);
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
        private final Resources resources;

        public UpdateResourcesCommand(String uuid, Resources resources) {
            this.uuid = uuid;
            this.resources = resources;
        }

        public CruiseConfig update(CruiseConfig cruiseConfig) {
            AgentConfig agentConfig = cruiseConfig.agents().getAgentByUuid(uuid);
            bombIfNull(agentConfig, "Unable to set agent resources; Agent [" + uuid + "] not found.");
            agentConfig.setResources(resources);
            return cruiseConfig;
        }
    }

    public static class ModifyResourcesCommand implements UpdateConfigCommand {
        private final String uuid;
        private final Resource resource;
        private final TriStateSelection.Action action;

        public ModifyResourcesCommand(String uuid, Resource resource, TriStateSelection.Action action) {
            this.uuid = uuid;
            this.resource = resource;
            this.action = action;
        }

        public CruiseConfig update(CruiseConfig cruiseConfig) {
            AgentConfig agentConfig = cruiseConfig.agents().getAgentByUuid(uuid);
            bombIfNull(agentConfig, "Unable to set agent resources; Agent [" + uuid + "] not found.");
            if (action.equals(TriStateSelection.Action.add)) {
                agentConfig.addResource(resource);
            } else if (action.equals(TriStateSelection.Action.remove)) {
                agentConfig.removeResource(resource);
            } else if (action.equals(TriStateSelection.Action.nochange)) {
                //do nothing
            } else {
                bomb(String.format("unsupported action '%s'", action));
            }
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

        public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
            AgentConfig agentConfig = cruiseConfig.agents().getAgentByUuid(uuid);
            bombIfNull(agentConfig, "Unable to set agent hostname; Agent [" + uuid + "] not found.");
            agentConfig.setHostName(hostname);
            return cruiseConfig;
        }

        public ConfigModifyingUser user() {
            return new ConfigModifyingUser(userName);
        }
    }

}
