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
    private List<String> registeredAgentsUuids;

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
        if (!goConfigService.isAdministrator(username.getUsername())) {
            result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_OPERATE_AGENTS"), HealthStateType.unauthorised());
            return false;
        }

        if (isAnyOperationPerformedOnAgents()) {
            return true;
        }
        result.badRequest(LocalizedMessage.string("NO_OPERATION_PERFORMED_ON_AGENTS"));
        return false;
    }

    private List<AgentConfig> getValidAgents(List<String> uuids, LocalizedOperationResult result, Agents agents) throws NoSuchAgentException {
        List<AgentConfig> goodAgents = new ArrayList<>();
        List<String> unknownAgents = new ArrayList<>();

        for (String uuid : uuids) {
            AgentConfig agent = agents.getAgentByUuid(uuid);
            if (!agent.isNull()) {
                goodAgents.add(agent);
            } else {
                unknownAgents.add(uuid);
            }
        }
        if (!unknownAgents.isEmpty()) {
            result.badRequest(LocalizedMessage.string("AGENTS_WITH_UUIDS_NOT_FOUND", unknownAgents));
            throw new NoSuchAgentException(unknownAgents);
        }

        return goodAgents;
    }

    private void validateEnvironment(Set<CaseInsensitiveString> allEnvironmentNames, List<String> environmentsToOperate, LocalizedOperationResult result) throws NoSuchEnvironmentException {
        for (String environment : environmentsToOperate) {
            CaseInsensitiveString environmentName = new CaseInsensitiveString(environment);
            if (!allEnvironmentNames.contains(environmentName)) {
                result.badRequest(LocalizedMessage.string("RESOURCE_NOT_FOUND", "Environment", environmentName));
                throw new NoSuchEnvironmentException(environmentName);
            }
        }
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {

        Set<CaseInsensitiveString> allEnvironmentNames = new HashSet<>(goConfigService.getEnvironments().names());
        validateEnvironment(allEnvironmentNames, environmentsToAdd, result);
        validateEnvironment(allEnvironmentNames, environmentsToRemove, result);

        List<AgentConfig> pendingAgents = getAllPendingAgents();
        List<String> pendingAgentUuids = getPendingAgentUuids(pendingAgents);
        registeredAgentsUuids = getRegisteredAgentUuids(pendingAgentUuids);

        List<AgentConfig> goodAgents = getValidAgents(registeredAgentsUuids, result, preprocessedConfig.agents());
        checkElasticAgentsResourceUpdated(goodAgents, resourcesToAdd, resourcesToRemove);

        if (hasValidOperationOnPendingAgent(pendingAgents)) {
            preprocessedConfig.agents().addAll(pendingAgents);
            registeredAgentsUuids.addAll(pendingAgentUuids);
            goodAgents.addAll(pendingAgents);
        }

        for (AgentConfig agentConfig : goodAgents) {
            if (state.isFalse()) {
                agentConfig.disable();
            }

            if (state.isTrue()) {
                agentConfig.enable();
            }

            for (String r : resourcesToAdd) {
                agentConfig.addResource(new Resource(r));
            }

            for (String r : resourcesToRemove) {
                agentConfig.removeResource(new Resource(r));
            }

            for (String environment : environmentsToAdd) {
                EnvironmentConfig environmentConfig = preprocessedConfig.getEnvironments().find(new CaseInsensitiveString(environment));
                if (environmentConfig != null) {
                    environmentConfig.addAgentIfNew(agentConfig.getUuid());
                }
            }

            for (String environment : environmentsToRemove) {
                EnvironmentConfig environmentConfig = preprocessedConfig.getEnvironments().find(new CaseInsensitiveString(environment));
                if (environmentConfig != null) {
                    environmentConfig.removeAgent(agentConfig.getUuid());
                }
            }
        }
    }

    private List<String> getRegisteredAgentUuids(List<String> pendingAgentUuids) {
        List<String> agents = new ArrayList<>(uuids);
        agents.removeAll(pendingAgentUuids);
        return agents;
    }

    private boolean isAnyOperationPerformedOnAgents() {
        return !resourcesToAdd.isEmpty() || !resourcesToRemove.isEmpty() || !environmentsToAdd.isEmpty()
                || !environmentsToRemove.isEmpty() || state.isTrue() || state.isFalse();
    }

    private List<AgentConfig> getAllPendingAgents() {
        ArrayList<AgentConfig> pendingAgents = new ArrayList<>();
        for (AgentInstance agentInstance : agentInstances.findPendingAgents()) {
            if (uuids.contains(agentInstance.getUuid())) {
                pendingAgents.add(agentInstance.agentConfig());
            }
        }
        return pendingAgents;
    }

    private boolean hasValidOperationOnPendingAgent(List<AgentConfig> pendingAgents) {
        ArrayList<String> pendingAgentUuids = getPendingAgentUuids(pendingAgents);
        return !pendingAgents.isEmpty() && isRequestedActionValidOnPendingAgent(pendingAgentUuids);
    }

    private boolean isRequestedActionValidOnPendingAgent(ArrayList<String> pendingAgentUuids) {
        if (state.isTrue() || state.isFalse()) {
            return true;
        }
        result.badRequest(LocalizedMessage.string("PENDING_AGENT_INVALID_OPERATION", pendingAgentUuids));
        return false;
    }

    private ArrayList<String> getPendingAgentUuids(List<AgentConfig> pendingAgents) {
        ArrayList<String> pendingAgentUUIDs = new ArrayList<>();
        for (AgentConfig agentConfig : pendingAgents) {
            pendingAgentUUIDs.add(agentConfig.getUuid());
        }
        return pendingAgentUUIDs;
    }

    private void checkElasticAgentsResourceUpdated(List<AgentConfig> uuids, List<String> resourcesToAdd, List<String> resourcesToRemove) throws ElasticAgentsResourceUpdateException {
        ArrayList<String> elasticAgentUUIDs = findAllElasticAgentUuids(uuids);

        if (elasticAgentUUIDs.isEmpty()) {
            return;
        }

        if (resourcesToAdd.isEmpty() && resourcesToRemove.isEmpty()) {
            return;
        }

        result.badRequest(LocalizedMessage.string("CAN_NOT_UPDATE_RESOURCES_ON_ELASTIC_AGENT", elasticAgentUUIDs));
        throw new ElasticAgentsResourceUpdateException(elasticAgentUUIDs);
    }

    private ArrayList<String> findAllElasticAgentUuids(List<AgentConfig> uuids) {
        ArrayList<String> elasticAgentUUIDs = new ArrayList<>();
        for (AgentConfig agent : uuids) {
            if (agent.isElastic()) {
                elasticAgentUUIDs.add(agent.getUuid());
            }
        }
        return elasticAgentUUIDs;
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        agents = preprocessedConfig.agents();
        AgentConfigsUpdateValidator validator = new AgentConfigsUpdateValidator(registeredAgentsUuids);
        return validator.isValid(preprocessedConfig);
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
