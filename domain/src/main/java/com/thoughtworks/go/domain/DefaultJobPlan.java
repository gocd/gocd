/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultJobPlan implements JobPlan {

    private long jobId;
    private long pipelineId;
    private JobIdentifier identifier;

    private Resources resources;
    private List<ArtifactPlan> artifactPlans;
    private List<ArtifactPropertiesGenerator> generators;
    private String agentUuid;
    private EnvironmentVariables variables;
    private EnvironmentVariables triggerVariables;
    private ElasticProfile elasticProfile;
    private ClusterProfile clusterProfile;
    private boolean fetchMaterials = StageConfig.DEFAULT_FETCH_MATERIALS;
    private boolean cleanWorkingDir = StageConfig.DEFAULT_CLEAN_WORKING_DIR;


    //FOR IBATIS
    protected DefaultJobPlan() {
    }

    public DefaultJobPlan(Resources resources, List<ArtifactPlan> artifactPlans, List<ArtifactPropertiesGenerator> generators, long jobId,
                          JobIdentifier identifier, String agentUuid, EnvironmentVariables variables,
                          EnvironmentVariables triggerTimeVariables, ElasticProfile elasticProfile, ClusterProfile clusterProfile) {
        this.jobId = jobId;
        this.identifier = identifier;
        this.resources = resources;
        this.artifactPlans = artifactPlans;
        this.generators = generators;
        this.agentUuid = agentUuid;
        this.variables = variables;
        this.triggerVariables = triggerTimeVariables;
        this.elasticProfile = elasticProfile;
        this.clusterProfile = clusterProfile;
    }

    public String getPipelineName() {
        return identifier.getPipelineName();
    }

    public String getStageName() {
        return identifier.getStageName();
    }

    public String getName() {
        return identifier.getBuildName();
    }

    public long getJobId() {
        return jobId;
    }

    public JobIdentifier getIdentifier() {
        return identifier;
    }

    public List<ArtifactPropertiesGenerator> getPropertyGenerators() {
        return generators;
    }

    public List<ArtifactPlan> getArtifactPlans() {
        return artifactPlans;
    }

    //USED BY IBatis - do NOT add to the interface

    public List<ArtifactPropertiesGenerator> getGenerators() {
        return generators;
    }

    public Resources getResources() {
        return resources;
    }

    public void setGenerators(List<ArtifactPropertiesGenerator> generators) {
        this.generators = new ArrayList<>(generators);
    }

    public void setJobId(long jobId) {
        this.jobId = jobId;
    }

    public void setIdentifier(JobIdentifier identifier) {
        this.identifier = identifier;
    }

    public void setArtifactPlans(List<ArtifactPlan> artifactPlans) {
        this.artifactPlans = artifactPlans;
    }

    public void setResources(List<Resource> resources) {
        this.resources = new Resources(resources);
    }

    public String toString() {
        return "[JobPlan " + "identifier=" + identifier + "resources=" + resources + " artifactConfigs=" + artifactPlans +
                " generators=" + generators + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultJobPlan plan = (DefaultJobPlan) o;

        if (jobId != plan.jobId) {
            return false;
        }
        if (generators != null ? !generators.equals(plan.generators) : plan.generators != null) {
            return false;
        }
        if (identifier != null ? !identifier.equals(plan.identifier) : plan.identifier != null) {
            return false;
        }
        if (artifactPlans != null ? !artifactPlans.equals(plan.artifactPlans) : plan.artifactPlans != null) {
            return false;
        }
        if (resources != null ? !resources.equals(plan.resources) : plan.resources != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (resources != null ? resources.hashCode() : 0);
        result = 31 * result + (artifactPlans != null ? artifactPlans.hashCode() : 0);
        result = 31 * result + (generators != null ? generators.hashCode() : 0);
        result = 31 * result + (int) (jobId ^ (jobId >>> 32));
        result = 31 * result + (identifier != null ? identifier.hashCode() : 0);
        return result;
    }

    public String getAgentUuid() {
        return agentUuid;
    }

    public EnvironmentVariables getVariables() {
        return variables;
    }

    public void setVariables(EnvironmentVariables variables) {
        this.variables = new EnvironmentVariables(variables);
    }

    public long getPipelineId() {
        return pipelineId;
    }

    public void setTriggerVariables(EnvironmentVariables environmentVariables) {
        triggerVariables = new EnvironmentVariables(environmentVariables);
    }

    public boolean shouldFetchMaterials() {
        return fetchMaterials;
    }

    public void applyTo(EnvironmentVariableContext variableContext) {
        variables.addTo(variableContext);
        triggerVariables.addToIfExists(variableContext);
    }

    public void setAgentUuid(String agentUuid) {
        this.agentUuid = agentUuid;
    }

    public void setFetchMaterials(boolean fetchMaterials) {
        this.fetchMaterials = fetchMaterials;
    }

    public void setCleanWorkingDir(boolean cleanWorkingDir) {
        this.cleanWorkingDir = cleanWorkingDir;
    }

    public boolean shouldCleanWorkingDir() {
        return cleanWorkingDir;
    }

    public ElasticProfile getElasticProfile() {
        return elasticProfile;
    }

    public ClusterProfile getClusterProfile() {
        return clusterProfile;
    }

    @Override
    public boolean requiresElasticAgent() {
        return elasticProfile != null;
    }

    public void setElasticProfile(ElasticProfile elasticProfile) {
        this.elasticProfile = new ElasticProfile(elasticProfile.getId(), elasticProfile.getClusterProfileId(), elasticProfile);
    }

    @Override
    public List<ArtifactPlan> getArtifactPlansOfType(final ArtifactPlanType artifactPlanType) {
        return getArtifactPlans().stream().filter(artifactPlan -> artifactPlan.getArtifactPlanType() == artifactPlanType).collect(Collectors.toList());
    }

    @Override
    public void setClusterProfile(ClusterProfile clusterProfile) {
        if (clusterProfile != null) {
            this.clusterProfile = new ClusterProfile(clusterProfile.getId(), clusterProfile.getPluginId(), clusterProfile);
        }
    }

    @Override
    public boolean assignedToAgent() {
        return agentUuid == null;
    }
}
