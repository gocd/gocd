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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.work.DefaultGoPublisher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DefaultJobPlan implements JobPlan {

    private long jobId;
    private long pipelineId;
    private JobIdentifier identifier;

    private Resources resources;
    private ArtifactPlans plans;
    private ArtifactPropertiesGenerators generators;
    private String agentUuid;
    private EnvironmentVariablesConfig variables;
    private EnvironmentVariablesConfig triggerVariables;
    private ElasticProfile elasticProfile;
    private boolean fetchMaterials = StageConfig.DEFAULT_FETCH_MATERIALS;
    private boolean cleanWorkingDir = StageConfig.DEFAULT_CLEAN_WORKING_DIR;


    //FOR IBATIS
    protected DefaultJobPlan() {
    }

    public DefaultJobPlan(Resources resources, ArtifactPlans plans,
                          ArtifactPropertiesGenerators generators, long jobId,
                          JobIdentifier identifier, String agentUuid, EnvironmentVariablesConfig variables, EnvironmentVariablesConfig triggerTimeVariables, ElasticProfile elasticProfile) {
        this.jobId = jobId;
        this.identifier = identifier;
        this.resources = resources;
        this.plans = plans;
        this.generators = generators;
        this.agentUuid = agentUuid;
        this.variables = variables;
        this.triggerVariables = triggerTimeVariables;
        this.elasticProfile = elasticProfile;
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

    public boolean match(List<Resource> resources) {
        return resources.containsAll(this.resources);
    }

    public long getJobId() {
        return jobId;
    }

    public JobIdentifier getIdentifier() {
        return identifier;
    }

    public void publishArtifacts(DefaultGoPublisher goPublisher, File workingDirectory) {
        ArtifactPlans mergedPlans = mergePlansForTest();

        List<ArtifactPlan> failedArtifact = new ArrayList<>();
        for (ArtifactPlan artifactPlan : mergedPlans) {
            try {
                artifactPlan.publish(goPublisher, workingDirectory);
            } catch (Exception e) {
                failedArtifact.add(artifactPlan);
            }
        }
        if (!failedArtifact.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (ArtifactPlan artifactPlan : failedArtifact) {
                artifactPlan.printSrc(builder);
            }
            throw new RuntimeException(String.format("[%s] Uploading finished. Failed to upload %s", GoConstants.PRODUCT_NAME, builder));
        }
    }

    private ArtifactPlans mergePlansForTest() {
        TestArtifactPlan testArtifactPlan = null;
        final ArtifactPlans mergedPlans = new ArtifactPlans();
        for (ArtifactPlan artifactPlan : plans) {
            if (artifactPlan.getArtifactType().isTest()) {
                if (testArtifactPlan == null) {
                    testArtifactPlan = new TestArtifactPlan(artifactPlan);
                    mergedPlans.add(testArtifactPlan);
                } else {
                    testArtifactPlan.add(artifactPlan);
                }
            } else {
                mergedPlans.add(artifactPlan);
            }
        }
        return mergedPlans;
    }

    public List<ArtifactPropertiesGenerator> getPropertyGenerators() {
        return generators;
    }

    public List<ArtifactPlan> getArtifactPlans() {
        return plans;
    }

    //USED BY IBatis - do NOT add to the interface

    public List<ArtifactPropertiesGenerator> getGenerators() {
        return generators;
    }

    public List<ArtifactPlan> getPlans() {
        return plans;
    }

    public List<Resource> getResources() {
        return resources;
    }


    public void setGenerators(List<ArtifactPropertiesGenerator> generators) {
        this.generators = new ArtifactPropertiesGenerators(generators);
    }

    public void setJobId(long jobId) {
        this.jobId = jobId;
    }

    public void setIdentifier(JobIdentifier identifier) {
        this.identifier = identifier;
    }

    public void setPlans(List<ArtifactPlan> plans) {
        this.plans = new ArtifactPlans(plans);
    }

    public void setResources(List<Resource> resources) {
        this.resources = new Resources(resources);
    }

    public String toString() {
        return "[JobPlan " + "identifier=" + identifier + "resources=" + resources + " plans=" + plans +
                " generators=" + generators + "]";
    }

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
        if (plans != null ? !plans.equals(plan.plans) : plan.plans != null) {
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
        result = 31 * result + (plans != null ? plans.hashCode() : 0);
        result = 31 * result + (generators != null ? generators.hashCode() : 0);
        result = 31 * result + (int) (jobId ^ (jobId >>> 32));
        result = 31 * result + (identifier != null ? identifier.hashCode() : 0);
        return result;
    }

    public String getAgentUuid() {
        return agentUuid;
    }

    public EnvironmentVariablesConfig getVariables() {
        return variables;
    }

    public void setVariables(EnvironmentVariablesConfig variables) {
        this.variables = variables;
    }

    public long getPipelineId() {
        return pipelineId;
    }

    public void setTriggerVariables(EnvironmentVariablesConfig environmentVariablesConfig) {
        triggerVariables = environmentVariablesConfig;
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

    @Override
    public boolean requiresElasticAgent() {
        return elasticProfile != null;
    }

    public void setElasticProfile(ElasticProfile elasticProfile) {
        this.elasticProfile = elasticProfile;
    }

    @Override
    public boolean assignedToAgent() {
        return agentUuid == null;
    }
}
