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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.Agents;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.exceptions.ElasticAgentsResourceUpdateException;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.InvalidPendingAgentOperationException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.util.TriState;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.thoughtworks.go.i18n.LocalizedMessage.forbiddenToEdit;
import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;

public class AgentsUpdateValidator {
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

    public AgentsUpdateValidator(AgentInstances agentInstances, Username username, LocalizedOperationResult result,
                                 List<String> uuids, List<String> environmentsToAdd, List<String> environmentsToRemove,
                                 TriState state, List<String> resourcesToAdd, List<String> resourcesToRemove,
                                 GoConfigService goConfigService) {
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

    public boolean canContinue() {
        if (!isAuthorized()) {
            return false;
        }

        if (isAnyOperationPerformedOnAgents()) {
            return true;
        }

        result.badRequest("No Operation performed on agents.");
        return false;
    }

    public void validate() throws Exception {
        Set<CaseInsensitiveString> allEnvironmentNames = new HashSet<>(goConfigService.getEnvironments().names());

        // validate all inputs
        validatePresenceOfEnvironments(allEnvironmentNames, environmentsToAdd);
        validatePresenceOfEnvironments(allEnvironmentNames, environmentsToRemove);

        validatePresenceOfAgentUuidsInConfig();
        checkIfResourcesAreBeingUpdatedOnElasticAgents();

        validatePresenceOfAgentUuidsInConfig();
        checkIfResourcesAreBeingUpdatedOnElasticAgents();
        List<AgentConfig> pendingAgents = findPendingAgents();
        validateOperationOnPendingAgents(pendingAgents);
    }

    private void validatePresenceOfAgentUuidsInConfig() {
        List<String> unknownUUIDs = new ArrayList<>();

        for (String uuid : uuids) {
            if (agentInstances.findAgent(uuid).isNullAgent()) {
                unknownUUIDs.add(uuid);
            }
        }
        if (!unknownUUIDs.isEmpty()) {
            result.badRequest(EntityType.Agent.notFoundMessage(unknownUUIDs));
            throw new RecordNotFoundException(EntityType.Agent, unknownUUIDs);
        }
    }

    private void validatePresenceOfEnvironments(Set<CaseInsensitiveString> allEnvironmentNames, List<String> environmentsToOperate) {
        for (String environment : environmentsToOperate) {
            CaseInsensitiveString environmentName = new CaseInsensitiveString(environment);
            if (!allEnvironmentNames.contains(environmentName)) {
                result.badRequest(EntityType.Environment.notFoundMessage(environmentName));
                throw new RecordNotFoundException(EntityType.Environment, environmentName);
            }
        }
    }

    private boolean isAuthorized() {
        if (goConfigService.isAdministrator(username.getUsername())) {
            return true;
        }
        result.forbidden(forbiddenToEdit(), forbidden());
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
            result.badRequest("Pending agents [" + StringUtils.join(pendingAgentUuids, ", ") + "] must be explicitly enabled or disabled when performing any operations on them.");
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

        result.badRequest("Resources on elastic agents with uuids [" + StringUtils.join(elasticAgentUUIDs, ", ") + "] can not be updated.");
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
}
