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

package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.ElasticAgentsResourceUpdateException;
import com.thoughtworks.go.config.exceptions.InvalidPendingAgentOperationException;
import com.thoughtworks.go.config.exceptions.NoSuchAgentException;
import com.thoughtworks.go.config.exceptions.NoSuchEnvironmentException;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.util.TriState;
import com.thoughtworks.go.validation.AgentConfigsUpdateValidator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AgentsEntityConfigUpdateCommand implements EntityConfigUpdateCommand<Agents> {
    private AgentInstances agentInstances;
    private final Username username;
    private final LocalizedOperationResult result;
    private final List<String> uuids;
    private final List<String> environmentsToAdd;
    private final List<String> environmentsToRemove;
    private final TriState state;
    private final List<String> resourcesToAdd;
    private final List<String> resourcesToRemove;
    private GoConfigService goConfigService;
    public Agents agents;

    public AgentsEntityConfigUpdateCommand(AgentInstances agentInstances, Username username, LocalizedOperationResult result, List<String> uuids, List<String> environmentsToAdd, List<String> environmentsToRemove, TriState state, List<String> resourcesToAdd, List<String> resourcesToRemove, GoConfigService goConfigService) {
        this.agentInstances = agentInstances;
        this.username = username;
        this.result = result;
        this.uuids = uuids;
        this.environmentsToAdd = environmentsToAdd;
        this.environmentsToRemove = environmentsToRemove;
        this.state = state;
        this.resourcesToAdd = resourcesToAdd;
        this.resourcesToRemove = resourcesToRemove;
        this.goConfigService = goConfigService;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        if (!isAuthorized()) {
            return false;
        }

        if (isAnyOperationPerformedOnAgents()) {
            return true;
        }

        result.badRequest(LocalizedMessage.string("NO_OPERATION_PERFORMED_ON_AGENTS"));
        return false;
    }

    @Override
    public void update(CruiseConfig modifiedConfig) throws Exception {
        Set<CaseInsensitiveString> allEnvironmentNames = new HashSet<>(goConfigService.getEnvironments().names());

        // validate all inputs
        validatePresenceOfEnvironments(allEnvironmentNames, environmentsToAdd);
        validatePresenceOfEnvironments(allEnvironmentNames, environmentsToRemove);

        validatePresenceOfAgentUuidsInConfig();
        checkIfResourcesAreBeingUpdatedOnElasticAgents();

        List<AgentConfig> pendingAgents = findPendingAgents();
        validateOperationOnPendingAgents(pendingAgents);

        // add pending agents to the config
        modifiedConfig.agents().addAll(pendingAgents);

        // update all agents specified by uuids
        Agents agents = modifiedConfig.agents().filter(uuids);
        for (AgentConfig agentConfig : agents) {
            if (state.isFalse()) {
                agentConfig.disable();
            }

            if (state.isTrue()) {
                agentConfig.enable();
            }

            for (String r : resourcesToAdd) {
                agentConfig.addResourceConfig(new ResourceConfig(r));
            }

            for (String r : resourcesToRemove) {
                agentConfig.removeResource(new ResourceConfig(r));
            }

            for (String environment : environmentsToAdd) {
                EnvironmentConfig environmentConfig = modifiedConfig.getEnvironments().find(new CaseInsensitiveString(environment));
                if (environmentConfig != null) {
                    environmentConfig.addAgentIfNew(agentConfig.getUuid());
                }
            }

            for (String environment : environmentsToRemove) {
                EnvironmentConfig environmentConfig = modifiedConfig.getEnvironments().find(new CaseInsensitiveString(environment));
                if (environmentConfig != null) {
                    environmentConfig.removeAgent(agentConfig.getUuid());
                }
            }
        }
    }

    private void validatePresenceOfAgentUuidsInConfig() throws NoSuchAgentException {
        List<String> unknownUUIDs = new ArrayList<>();

        for (String uuid : uuids) {
            if (agentInstances.findAgent(uuid).isNullAgent()) {
                unknownUUIDs.add(uuid);
            }
        }
        if (!unknownUUIDs.isEmpty()) {
            result.badRequest(LocalizedMessage.string("RESOURCE_NOT_FOUND", "Agents", unknownUUIDs));
            throw new NoSuchAgentException(unknownUUIDs);
        }
    }

    private void validatePresenceOfEnvironments(Set<CaseInsensitiveString> allEnvironmentNames, List<String> environmentsToOperate) throws NoSuchEnvironmentException {
        for (String environment : environmentsToOperate) {
            CaseInsensitiveString environmentName = new CaseInsensitiveString(environment);
            if (!allEnvironmentNames.contains(environmentName)) {
                result.badRequest(LocalizedMessage.string("RESOURCE_NOT_FOUND", "Environment", environmentName));
                throw new NoSuchEnvironmentException(environmentName);
            }
        }
    }

    private boolean isAuthorized() {
        if (goConfigService.isAdministrator(username.getUsername())) {
            return true;
        }
        result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT"), HealthStateType.unauthorised());
        return false;
    }

    private boolean isAnyOperationPerformedOnAgents() {
        return !resourcesToAdd.isEmpty() || !resourcesToRemove.isEmpty() || !environmentsToAdd.isEmpty()
                || !environmentsToRemove.isEmpty() || state.isTrue() || state.isFalse();
    }


    private List<AgentConfig> findPendingAgents() {
        List<AgentConfig> pendingAgents = new ArrayList<>();
        for (String uuid : uuids) {
            AgentInstance agent = agentInstances.findAgent(uuid);
            if (agent.isPending()) {
                pendingAgents.add(agent.agentConfig().deepClone());
            }
        }
        return pendingAgents;
    }

    private void validateOperationOnPendingAgents(List<AgentConfig> pendingAgents) throws InvalidPendingAgentOperationException {
        if (pendingAgents.isEmpty()) {
            return;
        }

        List<String> pendingAgentUuids = getPendingAgentUuids(pendingAgents);
        if (!(state.isTrue() || state.isFalse())) {
            result.badRequest(LocalizedMessage.string("PENDING_AGENT_INVALID_OPERATION", pendingAgentUuids));
            throw new InvalidPendingAgentOperationException(pendingAgentUuids);
        }
    }

    private List<String> getPendingAgentUuids(List<AgentConfig> pendingAgents) {
        List<String> pendingAgentUuids = new ArrayList<>();
        for (AgentConfig pendingAgent : pendingAgents) {
            pendingAgentUuids.add(pendingAgent.getUuid());
        }
        return pendingAgentUuids;
    }

    private void checkIfResourcesAreBeingUpdatedOnElasticAgents() throws ElasticAgentsResourceUpdateException {
        if (resourcesToAdd.isEmpty() && resourcesToRemove.isEmpty()) {
            return;
        }

        List<String> elasticAgentUUIDs = findAllElasticAgentUuids(uuids);
        if (elasticAgentUUIDs.isEmpty()) {
            return;
        }

        result.badRequest(LocalizedMessage.string("CAN_NOT_UPDATE_RESOURCES_ON_ELASTIC_AGENT", elasticAgentUUIDs));
        throw new ElasticAgentsResourceUpdateException(elasticAgentUUIDs);
    }

    private List<String> findAllElasticAgentUuids(List<String> uuids) {
        ArrayList<String> elasticAgentUUIDs = new ArrayList<>();
        for (String uuid : uuids) {
            if (agentInstances.findAgent(uuid).isElastic()) {
                elasticAgentUUIDs.add(uuid);
            }
        }
        return elasticAgentUUIDs;
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        agents = preprocessedConfig.agents();
        AgentConfigsUpdateValidator validator = new AgentConfigsUpdateValidator(uuids);
        boolean isValid = validator.isValid(preprocessedConfig);
        if (!isValid) {
           result.unprocessableEntity(LocalizedMessage.string("BULK_AGENT_UPDATE_FAILED", agents.getAllErrors()));
        }
        return isValid;
    }

    @Override
    public void clearErrors() {
        BasicCruiseConfig.clearErrors(agents);
    }

    @Override
    public Agents getPreprocessedEntityConfig() {
        return agents;
    }
}
