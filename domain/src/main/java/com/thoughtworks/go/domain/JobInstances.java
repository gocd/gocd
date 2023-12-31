/*
 * Copyright 2024 Thoughtworks, Inc.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class JobInstances extends BaseCollection<JobInstance> {
    private static final Comparator<JobInstance> JOB_INSTANCE_NAME_COMPARATOR = Comparator.comparing(JobInstance::getName);

    public JobInstances() {
        super();
    }

    public JobInstances(Collection<? extends JobInstance> jobInstances) {
        this.addAll(jobInstances);
    }

    public JobInstances(JobInstance... jobInstance) {
        super(Arrays.asList(jobInstance));
    }

    public JobInstance getByName(String name) {
        for (JobInstance thisInstance : this) {
            if (name.equals(thisInstance.getName())) {
                return thisInstance;
            }
        }
        throw bomb("Does not contain plan with name " + name);
    }

    public boolean hasJobNamed(String name) {
        for (JobInstance thisInstance : this) {
            if (name.equalsIgnoreCase(thisInstance.getName())) {
                return true;
            }
        }
        return false;
    }

    public JobInstances filterByState(JobState state) {
        JobInstances filtered = new JobInstances();
        for (JobInstance instance : this) {
            if (state == instance.getState()) {
                filtered.add(instance);
            }
        }
        return filtered;
    }

    public JobInstances filterByResult(JobResult... results) {
        JobInstances filtered = new JobInstances();
        for (JobInstance instance : this) {
            if (Arrays.asList(results).contains(instance.getResult())) {
                filtered.add(instance);
            }
        }
        return filtered;
    }

    public JobInstance mostRecentPassed() {
        JobInstance mostRecent = NullJobInstance.NAMELESS;
        for (JobInstance instance : this) {
            mostRecent = instance.mostRecentPassed(mostRecent);
        }
        return mostRecent;
    }

    public JobInstance mostRecentCompleted() {
        JobInstance mostRecent = NullJobInstance.NAMELESS;
        for (JobInstance instance : this) {
            mostRecent = instance.mostRecentCompleted(mostRecent);
        }
        return mostRecent;
    }

    public StageState stageState() {
        return StageState.findByBuilds(this);
    }

    public JobStateTransitions stateTransitions() {
        final JobStateTransitions transitions = new JobStateTransitions();
        for (JobInstance job : this) {
            transitions.addAll(job.getTransitions());
        }
        return transitions;
    }

    public JobInstances sortByName() {
        this.sort(JOB_INSTANCE_NAME_COMPARATOR);
        return this;
    }

    public Date latestTransitionDate() {
        Date mostRecent = null;
        for (JobInstance jobInstance : this) {
            if (mostRecent == null || jobInstance.latestTransitionDate().after(mostRecent)) {
                mostRecent = jobInstance.latestTransitionDate();
            }
        }
        return mostRecent;
    }

    public long latestTransitionId() {
        long latest = JobStateTransition.NOT_PERSISTED;
        for (JobInstance jobInstance : this) {
            long id = jobInstance.latestTransitionId();
            if (id > latest) {
                latest = id;
            }
        }
        return latest;
    }

    public void resetJobsIds() {
        for (JobInstance jobInstance : this) {
            jobInstance.resetForCopy();
        }
    }
}
