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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.server.service.InstanceFactory;
import com.thoughtworks.go.util.Clock;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @understands how to match job instances associated with run-on-all-agents Jobs
 */
public class RunOnAllAgents implements JobType {
    private static final Pattern CONFIG_NAME_PATTERN = Pattern.compile("^(.+?)-" + RunOnAllAgentsJobTypeConfig.MARKER + "-\\d+$");
    private final JobTypeConfig jobTypeConfig = new RunOnAllAgentsJobTypeConfig();

    @Override
    public boolean isInstanceOf(String jobInstanceName, boolean ignoreCase, String jobConfigName) {
        return jobTypeConfig.isInstanceOf(jobInstanceName, ignoreCase, jobConfigName);
    }

    @Override
    public void createRerunInstances(JobInstance oldJob, JobInstances jobInstances, SchedulingContext context, StageConfig stageConfig, final Clock clock, InstanceFactory instanceFactory) {
        context = context.permittedAgent(oldJob.getAgentUuid());
        String configName = translateToConfigName(oldJob.getName());
        JobConfig jobConfig = stageConfig.jobConfigByConfigName(new CaseInsensitiveString(configName));
        if (jobConfig == null) {
            throw new CannotRerunJobException(configName,  "Configuration for job doesn't exist.");
        }
		if (jobConfig.isRunMultipleInstanceType()) {
			String runType = "'run multiple instance'";
			throw new CannotRerunJobException(configName, "Run configuration for job has been changed to " + runType + ".");
		}
        String newJobName = jobConfig.isRunOnAllAgents() ? oldJob.getName() : CaseInsensitiveString.str(jobConfig.name());
        JobInstances instances = instanceFactory.createJobInstance(stageConfig.name(), jobConfig, context, clock, new IdentityNameGenerator(newJobName));
        for (JobInstance instance : instances) {
            instance.setAgentUuid(oldJob.getAgentUuid());//will always have one job
            instance.setRerun(true);
            if (jobInstances.hasJobNamed(newJobName)) {
                throw new IllegalArgumentException(String.format("Cannot schedule multiple instances of job named '%s'.", newJobName));
            }
        }
        jobInstances.addAll(instances);
    }

    private String translateToConfigName(String jobName) {
        Matcher matcher = CONFIG_NAME_PATTERN.matcher(jobName);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            throw new IllegalArgumentException(String.format("Job name %s does not match the pattern for run on all agents job", jobName));
        }

    }

    @Override
    public void createJobInstances(JobInstances jobs, SchedulingContext context, JobConfig config, String stageName, final JobNameGenerator nameGenerator, final Clock clock,
                                   InstanceFactory instanceFactory) {
        Collection<Agent> agents = context.findAgentsMatching(config.resourceConfigs());
        int counter = 0;
        for (Agent agent : agents) {
            instanceFactory.reallyCreateJobInstance(config, jobs, agent.getUuid(), nameGenerator.generateName(++counter), true, false, context, clock);
        }
        if (counter == 0) {
            throw new CannotScheduleException(String.format("Could not find matching agents to run job [%s] of stage [%s].", config.name(), stageName), stageName);
        }
    }

    private static class IdentityNameGenerator implements JobType.JobNameGenerator {
        private final String name;

        private IdentityNameGenerator(String name) {
            this.name = name;
        }

        @Override
        public String generateName(int counter) {
            return name;
        }
    }

    public static class CounterBasedJobNameGenerator implements JobNameGenerator {
        private String name;

        public CounterBasedJobNameGenerator(String name) {
            this.name = name;
        }

        @Override
        public String generateName(final int counter) {
            return appendMarker(name, counter);
        }

        public static String appendMarker(final String name, int counter) {
            return String.format("%s-%s-%s", name, RunOnAllAgentsJobTypeConfig.MARKER, counter);
        }
    }
}
