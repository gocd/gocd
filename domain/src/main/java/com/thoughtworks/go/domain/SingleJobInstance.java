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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.config.JobTypeConfig;
import com.thoughtworks.go.config.SingleJobTypeConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.server.service.InstanceFactory;
import com.thoughtworks.go.util.Clock;

/**
 * @understands how to match job instances associated Jobs that have a single instance
 */
public class SingleJobInstance implements JobType {
    private final JobTypeConfig jobTypeConfig = new SingleJobTypeConfig();

    @Override
    public boolean isInstanceOf(String jobInstanceName, boolean ignoreCase, String jobConfigName) {
        return jobTypeConfig.isInstanceOf(jobInstanceName, ignoreCase, jobConfigName);
    }

    @Override
    public void createJobInstances(JobInstances jobs, SchedulingContext context, JobConfig jobConfig, String stageName, final JobNameGenerator nameGenerator, final Clock clock,
                                   InstanceFactory instanceFactory) {
        instanceFactory.reallyCreateJobInstance(jobConfig, jobs, null, CaseInsensitiveString.str(jobConfig.name()), false, false, context, clock);
    }

    @Override
    public void createRerunInstances(JobInstance oldJob, JobInstances jobInstances, SchedulingContext context, StageConfig stageConfig, final Clock clock, InstanceFactory instanceFactory) {
        String jobName = oldJob.getName();
        JobConfig jobConfig = stageConfig.jobConfigByInstanceName(jobName, true);
        if (jobConfig == null) {
            throw new CannotRerunJobException(jobName,  "Configuration for job doesn't exist.");
        }
		if (jobConfig.isRunMultipleInstanceType()) {
			String runType = "'run multiple instance'";
			throw new CannotRerunJobException(jobName, "Run configuration for job has been changed to " + runType + ".");
		}
        RunOnAllAgents.CounterBasedJobNameGenerator nameGenerator = new RunOnAllAgents.CounterBasedJobNameGenerator(CaseInsensitiveString.str(jobConfig.name()));
        JobInstances instances = instanceFactory.createJobInstance(stageConfig.name(), jobConfig, context, clock, nameGenerator);
        for (JobInstance instance : instances) {
            instance.setRerun(true);
        }
        jobInstances.addAll(instances);
    }
}
