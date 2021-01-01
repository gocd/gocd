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
package com.thoughtworks.go.domain.cctray;

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.activity.ProjectStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/* Understands what needs to be done to keep the CCTray cache updated, when a job status changes. */
@Component
public class CcTrayJobStatusChangeHandler {
    private CcTrayCache cache;

    @Autowired
    public CcTrayJobStatusChangeHandler(CcTrayCache cache) {
        this.cache = cache;
    }

    public void call(JobInstance job) {
        cache.put(statusFor(job, new HashSet<>()));
    }

    public ProjectStatus statusFor(JobInstance job, Set<String> breakers) {
        String projectName = job.getIdentifier().ccProjectName();
        ProjectStatus existingStatusOfThisJobInCache = projectByName(projectName);
        ProjectStatus newStatus = new ProjectStatus(
                projectName,
                job.getState().cctrayActivity(),
                lastBuildStatus(existingStatusOfThisJobInCache, job),
                lastBuildLabel(existingStatusOfThisJobInCache, job),
                lastBuildTime(existingStatusOfThisJobInCache, job),
                job.getIdentifier().webUrl(),
                breakers);
        newStatus.updateViewers(existingStatusOfThisJobInCache.viewers());
        return newStatus;
    }

    private String lastBuildStatus(ProjectStatus existingStatus, JobInstance job) {
        return job.getState().isCompleted()
                ? job.getResult().toCctrayStatus()
                : existingStatus.getLastBuildStatus();
    }

    private Date lastBuildTime(ProjectStatus existingStatus, JobInstance job) {
        return job.isCompleted()
                ? job.getStartedDateFor(JobState.Completed)
                : existingStatus.getLastBuildTime();
    }

    private String lastBuildLabel(ProjectStatus existingStatus, JobInstance job) {
        return job.isCompleted()
                ? job.getIdentifier().getStageIdentifier().ccTrayLastBuildLabel()
                : existingStatus.getLastBuildLabel();
    }

    private ProjectStatus projectByName(String projectName) {
        ProjectStatus projectStatus = cache.get(projectName);
        return projectStatus == null ? new ProjectStatus.NullProjectStatus(projectName) : projectStatus;
    }
}
