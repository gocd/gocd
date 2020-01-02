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

import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;

import java.io.Serializable;
import java.util.List;

/**
 * @understands how to perform a job. This is the persistent version of the JobConfig
 */
public interface JobPlan extends Serializable {
    String getPipelineName();

    String getStageName();

    String getName();

    long getJobId();

    JobIdentifier getIdentifier();

    List<ArtifactPlan> getArtifactPlans();

    Resources getResources();

    String getAgentUuid();

    EnvironmentVariables getVariables();

    void applyTo(EnvironmentVariableContext variableContext);

    void setVariables(EnvironmentVariables variables);

    long getPipelineId();

    boolean shouldFetchMaterials();

    void setFetchMaterials(boolean fetchMaterials);

    void setCleanWorkingDir(boolean cleanWorkingDir);

    boolean shouldCleanWorkingDir();

    ElasticProfile getElasticProfile();

    ClusterProfile getClusterProfile();

    boolean requiresElasticAgent();

    boolean assignedToAgent();

    void setElasticProfile(ElasticProfile elasticProfile);

    List<ArtifactPlan> getArtifactPlansOfType(final ArtifactPlanType file);

    void setClusterProfile(ClusterProfile clusterProfile);
}
