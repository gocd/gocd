/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.Agents;
import com.thoughtworks.go.config.ResourceConfigs;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.util.TriState;

import java.util.List;

import static com.thoughtworks.go.domain.AgentInstance.FilterBy.*;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.join;

public class AgentsUpdateValidator {
    private AgentInstances agentInstances;
    private final List<String> uuids;
    private final TriState state;
    private final List<String> resourcesToAdd;
    private final List<String> resourcesToRemove;
    public Agents agents;

    public AgentsUpdateValidator(AgentInstances agentInstances, List<String> uuids, TriState state,
                                 List<String> resourcesToAdd, List<String> resourcesToRemove) {
        this.agentInstances = agentInstances;
        this.uuids = uuids;
        this.state = state;

        this.resourcesToAdd = actualOrEmptyList(resourcesToAdd);
        this.resourcesToRemove = actualOrEmptyList(resourcesToRemove);
    }

    public void validate() {
        bombWhenAgentsDoesNotExist();
        bombWhenElasticAgentResourcesAreUpdated();
        bombWhenResourceNamesToAddAreInvalid();
        bombWhenAnyOperationOnPendingAgents();
    }

    private void bombWhenResourceNamesToAddAreInvalid() {
        ResourceConfigs resourceConfigs = new ResourceConfigs(commaSeparate(resourcesToAdd));

        resourceConfigs.validate(null);

        if (!resourceConfigs.errors().isEmpty()) {
            throw new UnprocessableEntityException("Validations failed for bulk update of agents. Error(s): " + resourceConfigs.errors());
        }
    }

    private void bombWhenAgentsDoesNotExist() {
        List<String> notFoundUUIDs = agentInstances.filterBy(uuids, Null);

        if (!isEmpty(notFoundUUIDs)) {
            throw new RecordNotFoundException(EntityType.Agent, notFoundUUIDs);
        }
    }

    private void bombWhenAnyOperationOnPendingAgents() {
        List<String> pendingUUIDs = agentInstances.filterBy(uuids, Pending);

        if (isEmpty(pendingUUIDs)) {
            return;
        }

        if (state.isTrue() || state.isFalse()) {
            return;
        }

        throw new BadRequestException(format("Pending agents [%s] must be explicitly enabled or disabled when performing any operations on them.", commaSeparate(pendingUUIDs)));
    }

    private void bombWhenElasticAgentResourcesAreUpdated() {
        if (resourcesAreNotUpdated()) {
            return;
        }

        List<String> elasticAgentUUIDs = agentInstances.filterBy(uuids, Elastic);
        if (isEmpty(elasticAgentUUIDs)) {
            return;
        }

        throw new BadRequestException(format("Resources on elastic agents with uuids [%s] can not be updated.", commaSeparate(elasticAgentUUIDs)));
    }

    private boolean resourcesAreNotUpdated() {
        return resourcesToAdd.isEmpty() && resourcesToRemove.isEmpty();
    }

    private String commaSeparate(List<String> listOfStrs) {
        return join(listOfStrs, ", ");
    }

    private List<String> actualOrEmptyList(List<String> list) {
        return (list == null ? emptyList() : list);
    }
}
