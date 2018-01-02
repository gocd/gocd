/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.domain.activity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.thoughtworks.go.domain.JobConfigIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.NullJobInstance;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.JobStatusListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @understands jobs that are currently in progress
 */
@Component
public class JobStatusCache implements JobStatusListener {
    private ConcurrentMap<JobConfigIdentifier, JobInstance> jobs = new ConcurrentHashMap<>();
    private final StageDao stageDao;
    private static final NullJobInstance NEVER_RUN = new NullJobInstance("NEVER_RUN");

    @Autowired
    public JobStatusCache(StageDao stageDao) {
        this.stageDao = stageDao;
    }

    public void jobStatusChanged(JobInstance job) {
        cache(job);
    }

    private synchronized void cache(JobInstance newJob) {
        JobConfigIdentifier identifier = newJob.getIdentifier().jobConfigIdentifier();
        jobs.put(identifier, newJob);
        clearOldJobs(newJob);
    }

    private void clearOldJobs(JobInstance newJob) {
        Set<JobConfigIdentifier> cachedIds = new HashSet<>(jobs.keySet());
        for (JobConfigIdentifier cachedId : cachedIds) {
            JobInstance originalCached = jobs.get(cachedId);
            if ((originalCached == NEVER_RUN && !newJob.getIdentifier().jobConfigIdentifier().equals(cachedId))) {
                continue;
            }
            if (shouldBeCleared(newJob, originalCached)) {
                jobs.remove(cachedId, originalCached);
            }
        }
    }

    private boolean shouldBeCleared(JobInstance newJob, JobInstance cached) {
        return cached.isSameStageConfig(newJob) && !cached.isSamePipelineInstance(newJob);
    }

    public JobInstance currentJob(JobConfigIdentifier identifier) {
        List<JobInstance> jobs = currentJobs(identifier);
        return jobs.isEmpty() ? null : jobs.get(0);
    }

    public List<JobInstance> currentJobs(final JobConfigIdentifier identifier) {
        if (jobs.get(identifier) == NEVER_RUN) {
            return new ArrayList<>();
        }
        List<JobInstance> found = addInstances(identifier, jobs.values());
        if (found.isEmpty()) {
            found = addInstances(identifier, stageDao.mostRecentJobsForStage(identifier.getPipelineName(), identifier.getStageName()));
            if (found.isEmpty()) {
                jobs.put(identifier, NEVER_RUN);
            } else {
                for (JobInstance jobInstance : found) {
                    cache(jobInstance);
                }
            }
        }
        return found;
    }

    private List<JobInstance> addInstances(JobConfigIdentifier identifier, Collection<JobInstance> jobInstances) {
        List<JobInstance> found = new ArrayList<>();
        for (JobInstance jobInstance : jobInstances) {
            if (jobInstance != NEVER_RUN && jobInstance.matches(identifier)) {
                found.add(jobInstance);
            }
        }
        return found;
    }

    /**
     * @Deprecated Only for tests
     */
    public void clear() {
        jobs.clear();
    }
}
