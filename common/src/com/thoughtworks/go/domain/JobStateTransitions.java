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

package com.thoughtworks.go.domain;

import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class JobStateTransitions extends BaseLinkedSet<JobStateTransition> {


    public JobStateTransitions(JobStateTransition... list) {
        for (JobStateTransition jobStateTransition : list) {
            this.add(jobStateTransition);
        }
    }

    public JobStateTransitions() {

    }

    public JobStateTransitions(List<JobStateTransition> list) {
        super(list);
    }

    public JobStateTransition byState(JobState state) {
        for (JobStateTransition jobStateTransition : this) {
            if (jobStateTransition.getCurrentState().equals(state)) {
                return jobStateTransition;
            }
        }
        return null;
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        if (o.getClass()!=this.getClass()) return false;
        JobStateTransitions that = (JobStateTransitions) o;
        return new HashSet<>(this).equals(new HashSet<>(that));
    }

    public Date latestTransitionDate() {
        Date mostRecent = null;
        for (JobStateTransition transition : this) {
            if (mostRecent == null || transition.getStateChangeTime().after(mostRecent)) {
                mostRecent = transition.getStateChangeTime();
            }
        }
        return mostRecent;
    }

    public long latestTransitionId() {
        long latest = JobStateTransition.NOT_PERSISTED;
        for (JobStateTransition transition : this) {
            if (transition.getId() > latest) {
                latest = transition.getId();
            }
        }
        return latest;
    }

    public void resetTransitionIds() {
        for (JobStateTransition jobStateTransition : this) {
            jobStateTransition.setId(-1);
        }
    }
}
