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
import com.thoughtworks.go.i18n.LocalizedMessage;
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

    public AgentsEntityConfigUpdateCommand(Username username, LocalizedOperationResult result, List<String> uuids, List<String> environmentsToAdd, List<String> environmentsToRemove, TriState state, List<String> resourcesToAdd, List<String> resourcesToRemove, GoConfigService goConfigService) {
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
        if (goConfigService.isAdministrator(username.getUsername())) {
            return true;
        }

        result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_OPERATE_AGENTS"), HealthStateType.unauthorised());
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
                result.badRequest(LocalizedMessage.string("ENV_NOT_FOUND", environmentName));
                throw new NoSuchEnvironmentException(environmentName);
            }
        }
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        List<AgentConfig> goodAgents = getValidAgents(uuids, result, preprocessedConfig.agents());
        checkElasticAgentsResourceUpdated(goodAgents, resourcesToAdd, resourcesToRemove);

        Set<CaseInsensitiveString> allEnvironmentNames = new HashSet<>(goConfigService.getEnvironments().names());

        validateEnvironment(allEnvironmentNames, environmentsToAdd, result);
        validateEnvironment(allEnvironmentNames, environmentsToRemove, result);

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
        AgentConfigsUpdateValidator validator = new AgentConfigsUpdateValidator(uuids);
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
