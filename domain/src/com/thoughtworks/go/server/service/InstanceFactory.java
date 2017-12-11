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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.util.Clock;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InstanceFactory {

    public Pipeline createPipelineInstance(PipelineConfig pipelineConfig, BuildCause buildCause, SchedulingContext context, String md5, Clock clock) {
        buildCause.assertMaterialsMatch(pipelineConfig.materialConfigs());
        buildCause.assertPipelineConfigAndMaterialRevisionMatch(pipelineConfig);
        return new Pipeline(CaseInsensitiveString.str(pipelineConfig.name()), pipelineConfig.getLabelTemplate(), buildCause, createStageInstance(pipelineConfig.first(), context, md5, clock));
    }

    public Stage createStageInstance(PipelineConfig pipelineConfig, CaseInsensitiveString stageName, SchedulingContext context, String md5, Clock clock) {
        StageConfig stageConfig = pipelineConfig.findBy(stageName);
        if (stageConfig == null) {
            throw new StageNotFoundException(pipelineConfig.name(), stageName);
        }
        return createStageInstance(stageConfig, context, md5, clock);
    }

    public Stage createStageInstance(StageConfig stageConfig, SchedulingContext context, String md5, Clock clock) {
        return new Stage(CaseInsensitiveString.str(stageConfig.name()), createJobInstances(stageConfig, context, clock),
                context.getApprovedBy(), stageConfig.approvalType(), stageConfig.isFetchMaterials(), stageConfig.isCleanWorkingDir(), md5, clock);
    }

    public Stage createStageForRerunOfJobs(Stage stage, List<String> jobNames, SchedulingContext context, StageConfig stageConfig, Clock clock, String latestMd5) {
        Stage newStage = stage.createClone();
        newStage.prepareForRerunOf(context, latestMd5);
        createRerunJobs(newStage, jobNames, context, stageConfig, clock);
        return newStage;
    }

    public JobInstances createJobInstance(CaseInsensitiveString stageName, JobConfig jobConfig, SchedulingContext context, Clock clock, JobType.JobNameGenerator jobNameGenerator) {
        JobInstances instances = new JobInstances();
        createJobType(jobConfig.isRunOnAllAgents(), jobConfig.isRunMultipleInstanceType()).createJobInstances(instances, context, jobConfig, CaseInsensitiveString.str(stageName), jobNameGenerator, clock, this);
        return instances;
    }

    private JobInstances createJobInstances(StageConfig stageConfig, SchedulingContext context, Clock clock) {
        JobInstances instances = new JobInstances();
        for (JobConfig jobConfig : stageConfig.getJobs()) {
            JobType.JobNameGenerator nameGenerator = null;
            if (jobConfig.isRunOnAllAgents()) {
                nameGenerator = new RunOnAllAgents.CounterBasedJobNameGenerator(CaseInsensitiveString.str(jobConfig.name()));
            } else if (jobConfig.isRunMultipleInstanceType()) {
                nameGenerator = new RunMultipleInstance.CounterBasedJobNameGenerator(CaseInsensitiveString.str(jobConfig.name()));
            }
            JobInstances configInstances = createJobInstance(stageConfig.name(), jobConfig, context, clock, nameGenerator);
            instances.addAll(configInstances);
        }
        return instances;
    }

    private void createRerunJobs(Stage newStage, List<String> jobNames, SchedulingContext context, StageConfig stageConfig, Clock clock) {
        for (String jobName : jobNames) {
            JobInstances jobInstances = newStage.getJobInstances();
            JobInstance oldJob = jobInstances.getByName(jobName);
            jobInstances.remove(oldJob);
            createJobType(oldJob.isRunOnAllAgents(), oldJob.isRunMultipleInstance()).createRerunInstances(oldJob, jobInstances, context, stageConfig, clock, this);
        }
    }

    private JobType createJobType(boolean runOnAllAgents, boolean runMultipleInstances) {
        if (runOnAllAgents) {
            return new RunOnAllAgents();
        }
        if (runMultipleInstances) {
            return new RunMultipleInstance();
        }
        return new SingleJobInstance();
    }

    public void reallyCreateJobInstance(JobConfig config, JobInstances jobs, String uuid, String jobName, boolean runOnAllAgents, boolean runMultipleInstance, SchedulingContext context, final Clock clock) {
        JobInstance instance = new JobInstance(jobName, clock);
        instance.setPlan(createJobPlan(config, context));
        instance.setAgentUuid(uuid);
        instance.setRunOnAllAgents(runOnAllAgents);
        instance.setRunMultipleInstance(runMultipleInstance);
        jobs.add(instance);
    }

    public JobPlan createJobPlan(JobConfig config, SchedulingContext context) {
        JobIdentifier identifier = new JobIdentifier();
        String elasticProfileId = config.getElasticProfileId();
        ElasticProfile elasticProfile = null;
        if (elasticProfileId != null) {
            elasticProfile = context.getElasticProfile(elasticProfileId);
        }
        return new DefaultJobPlan(config.resources(), config.artifactPlans(), config.getProperties(), -1, identifier, null, context.overrideEnvironmentVariables(config.getVariables()).getEnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), elasticProfile);
    }
}
