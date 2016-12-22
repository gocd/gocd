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

package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.Agents;
import com.thoughtworks.go.config.EnvironmentVariablesConfig;
import com.thoughtworks.go.config.Resources;
import com.thoughtworks.go.config.elastic.ElasticProfile;

import java.util.HashMap;
import java.util.Map;

/**
 * @understands what a job needs to know to be scheduled
 */
public class DefaultSchedulingContext implements SchedulingContext {
    private final String approvedBy;
    private final Agents agents;
    private final Map<String, ElasticProfile> profiles;
    private EnvironmentVariablesConfig variables = new EnvironmentVariablesConfig();
    private boolean rerun;

    public DefaultSchedulingContext() {
        this("Unknown");
    }

    public DefaultSchedulingContext(String approvedBy) {
        this(approvedBy, new Agents());
    }

    public DefaultSchedulingContext(String approvedBy, Agents agents) {
        this(approvedBy, agents, new HashMap<String, ElasticProfile>());
    }

    public DefaultSchedulingContext(String approvedBy, Agents agents, Map<String, ElasticProfile> profiles) {
        this.approvedBy = approvedBy;
        this.agents = agents;
        this.profiles = profiles;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public Agents findAgentsMatching(Resources resources) {
        Agents found = new Agents();
        for (AgentConfig agent : agents) {
            if (agent.hasAllResources(resources) && !agent.isDisabled()) {
                found.add(agent);
            }
        }
        return found;
    }

    public EnvironmentVariablesConfig getEnvironmentVariablesConfig() {
        return variables;
    }

    public SchedulingContext overrideEnvironmentVariables(EnvironmentVariablesConfig environmentVariablesConfig) {
        DefaultSchedulingContext context = new DefaultSchedulingContext(approvedBy, new Agents(agents), profiles);
        context.variables = variables.overrideWith(environmentVariablesConfig);
        context.rerun = rerun;
        return context;
    }

    public SchedulingContext permittedAgent(String permittedAgentUuid) {
        Agents permitted = new Agents();
        for (AgentConfig agent : agents) {
            if (agent.getUuid().equals(permittedAgentUuid)) {
                permitted.add(agent);
            }
        }
        DefaultSchedulingContext context = new DefaultSchedulingContext(approvedBy, permitted, profiles);
        context.variables = variables.overrideWith(new EnvironmentVariablesConfig());
        context.rerun = rerun;
        return context;
    }

    public boolean isRerun() {
        return rerun;
    }

    public SchedulingContext rerunContext() {
        DefaultSchedulingContext context = new DefaultSchedulingContext(approvedBy, agents, profiles);
        context.variables = variables.overrideWith(new EnvironmentVariablesConfig());
        context.rerun = true;
        return context;
    }

    @Override
    public ElasticProfile getElasticProfile(String profileId) {
        return profiles.get(profileId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultSchedulingContext that = (DefaultSchedulingContext) o;

        if (agents != null ? !agents.equals(that.agents) : that.agents != null) {
            return false;
        }
        if (approvedBy != null ? !approvedBy.equals(that.approvedBy) : that.approvedBy != null) {
            return false;
        }
        if (variables != null ? !variables.equals(that.variables) : that.variables != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = approvedBy != null ? approvedBy.hashCode() : 0;
        result = 31 * result + (agents != null ? agents.hashCode() : 0);
        result = 31 * result + (variables != null ? variables.hashCode() : 0);
        return result;
    }
}
