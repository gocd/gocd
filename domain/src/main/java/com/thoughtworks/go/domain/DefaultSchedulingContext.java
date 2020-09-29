/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.config.Agents;
import com.thoughtworks.go.config.EnvironmentVariablesConfig;
import com.thoughtworks.go.config.ResourceConfigs;
import com.thoughtworks.go.config.elastic.ClusterProfile;
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
    private final Map<String, ClusterProfile> clusterProfiles;
    private final String elasticProfileIdAtPipelineConfig;
    private final String elasticProfileIdAtStageConfig;
    private EnvironmentVariablesConfig variables = new EnvironmentVariablesConfig();
    private boolean rerun;

    public DefaultSchedulingContext() {
        this("Unknown");
    }

    public DefaultSchedulingContext(String approvedBy) {
        this(approvedBy, new Agents());
    }

    public DefaultSchedulingContext(String approvedBy, Agents agents) {
        this(approvedBy, agents, new HashMap<>());
    }

    public DefaultSchedulingContext(String approvedBy, Agents agents, Map<String, ElasticProfile> profiles) {
        this(approvedBy, agents, profiles, new HashMap<>(), null, null);
    }

    public DefaultSchedulingContext(String approvedBy, Agents agents, Map<String, ElasticProfile> profiles, Map<String, ClusterProfile> clusterProfiles, String elasticProfileIdAtPipelineConfig, String elasticProfileIdAtStageConfig) {
        this.approvedBy = approvedBy;
        this.agents = agents;
        this.profiles = profiles;
        this.clusterProfiles = clusterProfiles;
        this.elasticProfileIdAtPipelineConfig = elasticProfileIdAtPipelineConfig;
        this.elasticProfileIdAtStageConfig = elasticProfileIdAtStageConfig;
    }

    @Override
    public String getApprovedBy() {
        return approvedBy;
    }

    @Override
    public Agents findAgentsMatching(ResourceConfigs resourceConfigs) {
        Agents found = new Agents();
        for (Agent agent : agents) {
            if (agent.hasAllResources(resourceConfigs.resourceNames()) && !agent.isDisabled()) {
                found.add(agent);
            }
        }
        return found;
    }

    @Override
    public EnvironmentVariablesConfig getEnvironmentVariablesConfig() {
        return variables;
    }

    @Override
    public SchedulingContext overrideEnvironmentVariables(EnvironmentVariablesConfig environmentVariablesConfig) {
        DefaultSchedulingContext context = new DefaultSchedulingContext(approvedBy, new Agents(agents), profiles, clusterProfiles, elasticProfileIdAtPipelineConfig, elasticProfileIdAtStageConfig);
        context.variables = variables.overrideWith(environmentVariablesConfig);
        context.rerun = rerun;
        return context;
    }

    @Override
    public SchedulingContext permittedAgent(String permittedAgentUuid) {
        Agents permitted = new Agents();
        for (Agent agent : agents) {
            if (agent.getUuid().equals(permittedAgentUuid)) {
                permitted.add(agent);
            }
        }
        DefaultSchedulingContext context = new DefaultSchedulingContext(approvedBy, permitted, profiles, clusterProfiles, elasticProfileIdAtPipelineConfig, elasticProfileIdAtStageConfig);
        context.variables = variables.overrideWith(new EnvironmentVariablesConfig());
        context.rerun = rerun;
        return context;
    }

    @Override
    public boolean isRerun() {
        return rerun;
    }

    @Override
    public SchedulingContext rerunContext() {
        DefaultSchedulingContext context = new DefaultSchedulingContext(approvedBy, agents, profiles, clusterProfiles, elasticProfileIdAtPipelineConfig, elasticProfileIdAtStageConfig);
        context.variables = variables.overrideWith(new EnvironmentVariablesConfig());
        context.rerun = true;
        return context;
    }

    @Override
    public ElasticProfile getElasticProfile(String profileId) {
        return profiles.get(profileId);
    }

    @Override
    public ClusterProfile getClusterProfile(String clusterProfileId) {
        return clusterProfiles.get(clusterProfileId);
    }

    @Override
    public String getElasticProfileIdAtPipelineConfig() {
        return elasticProfileIdAtPipelineConfig;
    }

    @Override
    public String getElasticProfileIdAtStageConfig() {
        return elasticProfileIdAtStageConfig;
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
