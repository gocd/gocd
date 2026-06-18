/*
 * Copyright Thoughtworks, Inc.
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

/* Understands what needs to be done to keep the CCTray cache updated, when a job status changes. */
@Component
public class CcTrayJobStatusChangeHandler {
    private final CcTrayCache cache;

    @Autowired
    public CcTrayJobStatusChangeHandler(CcTrayCache cache) {
        this.cache = cache;
    }

    public void call(JobInstance job) {
        cache.put(statusFor(job, Collections.emptySet()));
    }

    public ProjectStatus statusFor(JobInstance job, Set<String> breakers) {
        return statusFor(job, Integer.MAX_VALUE, breakers);
    }

    public ProjectStatus statusFor(JobInstance job, int stageOrderId, Set<String> breakers) {
        ProjectStatus.Key key = new ProjectStatus.Key(job.getIdentifier());
        ProjectStatus existingStatus = cache.getOrDefault(key, stageOrderId);
        ProjectStatus newStatus = new ProjectStatus(
            key,
            stageOrderId != Integer.MAX_VALUE ? stageOrderId : existingStatus.stageOrder(),
            job.getState().cctrayActivity(),
            lastBuildStatus(job, existingStatus),
            lastBuildLabel(job, existingStatus),
            lastBuildTime(job, existingStatus),
            job.getIdentifier().webPathAfterContext(),
            breakers
        );
        newStatus.updateViewers(existingStatus.viewers());
        return newStatus;
    }

    private String lastBuildStatus(JobInstance job, ProjectStatus existingStatus) {
        return job.getState().isCompleted()
                ? job.getResult().toCctrayStatus()
                : existingStatus.getLastBuildStatus();
    }

    private Date lastBuildTime(JobInstance job, ProjectStatus existingStatus) {
        return job.isCompleted()
                ? job.getStartedDateFor(JobState.Completed)
                : existingStatus.getLastBuildTime();
    }

    private String lastBuildLabel(JobInstance job, ProjectStatus existingStatus) {
        return job.isCompleted()
                ? job.getIdentifier().getStageIdentifier().ccTrayLastBuildLabel()
                : existingStatus.getLastBuildLabel();
    }

}
