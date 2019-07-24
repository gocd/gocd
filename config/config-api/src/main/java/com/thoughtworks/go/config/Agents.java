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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.NullAgent;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static java.util.Arrays.asList;

public class Agents extends ArrayList<Agent> implements Validatable {
    private ConfigErrors errors = new ConfigErrors();
    private String elasticAgentId = "elasticAgentId";

    public Agents() {
        super();
    }

    public Agents(List<Agent> agentConfigs) {
        if (agentConfigs != null) {
            this.addAll(agentConfigs);
        }
    }

    public Agents(Agent... agentConfigs) {
        this.addAll(asList(agentConfigs));
    }

    public Agents(Collection<Agent> agentConfigs) {
        this.addAll(agentConfigs);
    }

    public Agent getAgentByUuid(String uuid) {
        for (Agent agent : this) {
            if (StringUtils.equals(agent.getUuid(), uuid)) {
                return agent;
            }
        }
        return NullAgent.createNullAgent(uuid);
    }

    public boolean hasAgent(String uuid) {
        return !getAgentByUuid(uuid).isNull();
    }

    @Override
    public boolean add(Agent newAgentConfig) {
        if (contains(newAgentConfig)) {
            throw new IllegalArgumentException("Agent with same UUID already exists: " + newAgentConfig);
        }
        return super.add(newAgentConfig);
    }

    public Set<String> acceptedUuids() {
        HashSet<String> uuids = new HashSet<>();
        for (Agent agent : this) {
            uuids.add(agent.getUuid());
        }
        return uuids;
    }

    public Agents filter(List<String> uuids) {
        Agents agents = new Agents();
        for (String uuid : uuids) {
            agents.add(getAgentByUuid(uuid));
        }
        return agents;
    }

    @Override
    public void validate(ValidationContext validationContext) {
        boolean validity = validateDuplicateElasticAgentIds();
        for (Agent agent : this) {
            agent.validate();
            validity = !agent.hasErrors() && validity;
        }
    }

    private boolean validateDuplicateElasticAgentIds() {
        HashMap<String, String> elasticAgentIdToUUIDMap = new HashMap<>();
        for (Agent agent : this) {

            if (!agent.isElastic()) {
                continue;
            }

            if (elasticAgentIdToUUIDMap.containsKey(agent.getElasticAgentId())) {
                Agent duplicatedAgentConfig = this.getAgentByUuid(elasticAgentIdToUUIDMap.get(agent.getElasticAgentId()));
                String error = String.format("Duplicate ElasticAgentId found for agents [%s, %s]", duplicatedAgentConfig.getUuid(), agent.getUuid());
                agent.addError(elasticAgentId, error);
                duplicatedAgentConfig.addError("elasticAgentId", error);
                return false;
            }

            elasticAgentIdToUUIDMap.put(agent.getElasticAgentId(), agent.getUuid());
        }

        return true;
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }

    public List<ConfigErrors> getAllErrors() {
        return ErrorCollector.getAllErrors(this);
    }

}
