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

package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.*;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JobInstanceMother {
    protected JobInstanceMother() {
    }

    public static JobInstance scheduled(String jobConfigName, Date scheduleDate) {
        final JobInstance jobInstance = scheduled(jobConfigName);
        jobInstance.setScheduledDate(scheduleDate);
        return jobInstance;
    }

    public static JobInstance scheduled(String jobConfigName) {
        return scheduled(jobConfigName, new Resources());
    }

    public static JobInstance scheduled(String jobConfigName, Resources resources) {
        JobInstance instance = new JobInstance(jobConfigName);
        instance.setId(999);
        instance.setIdentifier(defaultJobIdentifier(jobConfigName));
        return instance;
    }

    public static JobInstance jobInstance(String jobConfigName, String resourceName) {
        return new JobInstance(jobConfigName
        );
    }

    public static JobInstance completed(String jobConfigName) {
        return completed(jobConfigName, JobResult.Passed);
    }

    public static JobInstance completed(String jobConfigName, JobResult result) {
        return buildEndingWithState(JobState.Completed, result, jobConfigName);
    }

    public static JobInstance completed(String jobConfigName, JobResult result, Date completedDate) {
        return completed(jobConfigName, result, completedDate, new Date());
    }

    public static JobInstance completed(String jobConfigName, JobResult result, Date completedDate, Date startbuilding) {
        JobInstance instance = new JobInstance(jobConfigName);
        instance.completing(result, completedDate);
        instance.completed(completedDate);
        instance.getTransitions().add(new JobStateTransition(JobState.Building, startbuilding));
        return instance;
    }

    public static JobInstance cancelled(String jobConfigName, Date scheduledDate, int interval) {
        JobInstance instance = new JobInstance(jobConfigName);
        instance.setIdentifier(defaultJobIdentifier(jobConfigName));
        instance.schedule();
        instance.getTransition(JobState.Scheduled).setStateChangeTime(scheduledDate);
        instance.setResult(JobResult.Cancelled);
        instance.completed(addMillis(scheduledDate, interval));
        return instance;
    }

    private static Date addMillis(Date scheduledDate, int interval) {
        return new Date(scheduledDate.getTime() + interval);
    }

    public static JobInstance passed(String jobConfigName, Date scheduledDate, int interval) {
        JobInstance instance = building(jobConfigName, scheduledDate, interval);
        instance.changeState(JobState.Completing, addMillis(scheduledDate, 4 * interval));
        instance.changeState(JobState.Completed, addMillis(scheduledDate, 5 * interval));
        instance.setResult(JobResult.Passed);
        return instance;
    }

    public static JobInstance building(String jobConfigName, Date scheduledDate, int interval) {
        JobInstance instance = new JobInstance(jobConfigName);
        instance.setIdentifier(defaultJobIdentifier(jobConfigName));
        instance.schedule();
        instance.getTransition(JobState.Scheduled).setStateChangeTime(scheduledDate);
        instance.changeState(JobState.Preparing, addMillis(scheduledDate, 2 * interval));
        instance.changeState(JobState.Building, addMillis(scheduledDate, 3 * interval));
        return instance;
    }

    public static JobInstance completed(JobInstance instance, JobResult result, Date completedDate) {
        instance.completing(result, completedDate);
        instance.completed(completedDate);
        return instance;
    }

    public static JobInstance building(String jobConfigName, Date startBuildingDate) {
        final JobInstance instance = new JobInstance(jobConfigName);
        instance.changeState(JobState.Building, startBuildingDate);
        instance.setScheduledDate(new DateTime().minusMinutes(5).toDate());
        instance.setIdentifier(defaultJobIdentifier(jobConfigName));
        return instance;
    }

    public static JobInstance building(String jobConfig) {
        final JobInstance instance = new JobInstance(jobConfig);
        setBuildingState(instance);
        instance.setIdentifier(defaultJobIdentifier(jobConfig));
        return instance;
    }

    public static JobInstance buildEndingWithState(JobState endState, JobResult result, String jobConfig) {
        final JobInstance instance = new JobInstance(jobConfig);
        instance.setAgentUuid("1234");
        instance.setId(1L);
        instance.setIdentifier(defaultJobIdentifier(jobConfig));

        instance.setState(endState);
        instance.setTransitions(new JobStateTransitions());
        DateTime now = new DateTime();
        Date scheduledDate = now.minusMinutes(5).toDate();
        instance.setScheduledDate(scheduledDate);

        List<JobState> orderedStates = orderedBuildStates();
        DateTime stateDate = new DateTime(scheduledDate);
        for (JobState stateToCompareTo : orderedStates) {
            if (endState.compareTo(stateToCompareTo) >= 0) {
                instance.changeState(stateToCompareTo, stateDate.toDate());
                stateDate = stateDate.plusMinutes(1);
            }
        }
        if (endState.equals(JobState.Completed)) {
            instance.setResult(result);
        }
        return instance;
    }

    private static List<JobState> orderedBuildStates() {
        List<JobState> jobStates = new ArrayList<JobState>();
        jobStates.add(JobState.Scheduled);
        jobStates.add(JobState.Assigned);
        jobStates.add(JobState.Preparing);
        jobStates.add(JobState.Building);
        jobStates.add(JobState.Completing);
        jobStates.add(JobState.Completed);
        return jobStates;
    }

    public static void setBuildingState(JobInstance instance) {
        instance.setAgentUuid("1234");
        DateTime now = new DateTime();
        Date scheduledDate = now.minusMinutes(5).toDate();
        instance.setScheduledDate(scheduledDate);
        setTransitionConditionally(instance, JobState.Scheduled, scheduledDate);
        setTransitionConditionally(instance, JobState.Assigned, now.minusMinutes(4).toDate());
        setTransitionConditionally(instance, JobState.Preparing, now.minusMinutes(3).toDate());
        setTransitionConditionally(instance, JobState.Building, now.minusMinutes(2).toDate());
    }

    private static void setTransitionConditionally(JobInstance instance, JobState state, Date date) {
        JobStateTransition transition = instance.getTransition(state);
        if (transition != null) {
            instance.setState(state);
        } else {
            instance.changeState(state, date);
        }
    }

    public static JobInstance assigned(String jobConfig) {
        return assignedWithAgentId(jobConfig, null);
    }

    public static JobInstance assignedWithAgentId(String jobConfig, String agentUuid) {
        final JobInstance instance = new JobInstance(jobConfig);
        instance.setIdentifier(defaultJobIdentifier(jobConfig));
        instance.setState(JobState.Assigned);
        return assignAgent(instance, agentUuid);
    }

    public static JobInstance assignAgent(JobInstance instance, String agentUuid) {
        instance.setAgentUuid(agentUuid);
        return instance;
    }

    private static JobIdentifier defaultJobIdentifier(String jobConfig) {
        return new JobIdentifier("pipeline", 1, "label-1", "stage", "1", jobConfig, -1L);
    }

    public static JobInstance failed(String jobConfigName) {
        return completed(jobConfigName, JobResult.Failed);
    }

    public static JobInstance passed(String jobConfigName) {
        return completed(jobConfigName, JobResult.Passed);
    }

    public static JobInstance cancelled(String jobConfigName) {
        return completed(jobConfigName, JobResult.Cancelled);
    }

    public static JobInstance rescheduled(String jobName, String agentUuid) {
        JobInstance instance = assignedWithAgentId(jobName, agentUuid);
        instance.setState(JobState.Rescheduled);
        instance.setIgnored(true);
        return instance;
    }

    public static JobInstance instanceForRunOnAllAgents(String pipelineName, String stageName, String jobName, String pipelineLabel, int counter) {
        String instanceName = RunOnAllAgents.CounterBasedJobNameGenerator.appendMarker(jobName, counter);
        JobInstance jobInstance = building(instanceName);
        jobInstance.setRunOnAllAgents(true);
        JobIdentifier identifier = new JobIdentifier(pipelineName, null, pipelineLabel, stageName, "1", instanceName, 0L);
        jobInstance.setIdentifier(identifier);
        return jobInstance;
    }

	public static JobInstance instanceForRunMultipleInstance(String pipelineName, String stageName, String jobName, String pipelineLabel, int counter) {
		String instanceName = RunMultipleInstance.CounterBasedJobNameGenerator.appendMarker(jobName, counter);
		JobInstance jobInstance = building(instanceName);
		jobInstance.setRunMultipleInstance(true);
		JobIdentifier identifier = new JobIdentifier(pipelineName, null, pipelineLabel, stageName, "1", instanceName, 0L);
		jobInstance.setIdentifier(identifier);
		return jobInstance;
	}

    public static JobInstance buildingInstance(String pipelineName, String stageName, String jobName,
                                               String pipelineLabel) {
        JobInstance job = building(jobName);
        JobIdentifier identifier = new JobIdentifier(pipelineName, null, pipelineLabel, stageName, "1", jobName, 0L);
        job.setIdentifier(identifier);
        return job;
    }

    public static DefaultJobPlan jobPlan(String jobName, long id) {
        return new DefaultJobPlan(new Resources(new Resource("foo"), new Resource("bar")), new ArtifactPlans(), new ArtifactPropertiesGenerators(), id, defaultJobIdentifier(jobName), null, new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null);
    }

    public static JobPlan createJobPlan(JobConfig jobConfig, JobIdentifier jobIdentifier, SchedulingContext schedulingContext) {
        return new DefaultJobPlan(jobConfig.resources(), jobConfig.artifactPlans(), jobConfig.getProperties(), -1,
                jobIdentifier, null, schedulingContext.overrideEnvironmentVariables(jobConfig.getVariables()).getEnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null);
    }
}
