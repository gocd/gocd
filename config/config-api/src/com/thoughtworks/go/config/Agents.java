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

package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.NullAgent;
import org.apache.commons.lang.StringUtils;

import java.util.*;

import static java.util.Arrays.asList;

@ConfigTag("agents")
@ConfigCollection(AgentConfig.class)
public class Agents extends ArrayList<AgentConfig> implements Validatable {
    private ConfigErrors errors = new ConfigErrors();

    public Agents() {
        super();
    }

    public Agents(List<AgentConfig> agentConfigs) {
        if (agentConfigs != null) {
            this.addAll(agentConfigs);
        }
    }

    public Agents(AgentConfig... agentConfigs) {
        this.addAll(asList(agentConfigs));
    }

    public Agents(Collection<AgentConfig> agentConfigs) {
        this.addAll(agentConfigs);
    }

    public AgentConfig getAgentByUuid(String uuid) {
        for (AgentConfig agentConfig : this) {
            if (StringUtils.equals(agentConfig.getUuid(), uuid)) {
                return agentConfig;
            }
        }
        return NullAgent.createNullAgent(uuid);
    }

    public boolean hasAgent(String uuid) {
        return !getAgentByUuid(uuid).isNull();
    }

    public boolean add(AgentConfig newAgentConfig) {
        if (contains(newAgentConfig)) {
            throw new IllegalArgumentException("Agent with same UUID already exists: " + newAgentConfig);
        }
        return super.add(newAgentConfig);
    }

    public Set<String> acceptedUuids() {
        HashSet<String> uuids = new HashSet<>();
        for (AgentConfig agentConfig : this) {
            uuids.add(agentConfig.getUuid());
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

    public void validate(ValidationContext validationContext) {
        boolean validity = true;
        for (AgentConfig agentConfig : this) {
            agentConfig.validate(validationContext);
            validity =agentConfig.errors().isEmpty() && validity;
        }
    }

    public ConfigErrors errors() {
        return errors;
    }

    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }

}
