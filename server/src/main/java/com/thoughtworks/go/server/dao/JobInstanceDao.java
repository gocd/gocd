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
package com.thoughtworks.go.server.dao;

import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.server.service.JobInstanceService;
import com.thoughtworks.go.server.ui.SortOrder;

import java.util.List;

/**
 * @understands how to retrieve and save jobInstances on the db
 */
public interface JobInstanceDao {

    List<JobPlan> orderedScheduledBuilds();

    JobInstances latestCompletedJobs(String pipelineName, String stageName, String jobConfigName, int count);

    int getJobHistoryCount(String pipelineName, String stageName, String jobName);

    JobInstances findJobHistoryPage(String pipelineName, String stageName, String jobConfigName, int count, int offset);

    JobInstance save(long stageId, JobInstance jobInstance);

    JobInstance updateAssignedInfo(JobInstance jobInstance);

    JobInstance updateStateAndResult(JobInstance jobInstance);

    void ignore(JobInstance job);

    JobInstance getLatestInProgressBuildByAgentUuid(String uuid);

    JobInstances findHungJobs(List<String> liveAgentIdList);

    JobInstance buildByIdWithTransitions(long id);

    JobInstance buildById(long buildId);

    List<ActiveJob> activeJobs();

    JobInstance mostRecentJobWithTransitions(JobIdentifier jobIdentifier);

    void save(long id, JobPlan plan);

    JobPlan loadPlan(long id);

    JobIdentifier findOriginalJobIdentifier(StageIdentifier stageIdentifier, String jobName);

    List<JobIdentifier> getBuildingJobs();

    List<JobInstance> completedJobsOnAgent(String uuid, JobInstanceService.JobHistoryColumns jobHistoryColumns, SortOrder order, int offset, int limit);

    int totalCompletedJobsOnAgent(String uuid);

    boolean isJobCompleted(JobIdentifier jobIdentifier);

    List<JobInstance> getRunningJobs();

    JobInstance findJobInstance(String pipelineName, String stageName, String jobName, int pipelineCounter, int stageCounter);

    JobInstances findDetailedJobHistoryViaCursor(String pipelineName, String stageName, String jobConfigName, FeedModifier feedModifier, long cursor, Integer pageSize);

    PipelineRunIdInfo getOldestAndLatestJobInstanceId(String pipelineName, String stageName, String jobConfigName);
}
