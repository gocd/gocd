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

import com.thoughtworks.go.helper.JobInstanceMother;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

import static com.thoughtworks.go.domain.StageState.*;
import static com.thoughtworks.go.helper.JobInstanceMother.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class JobInstancesTest {
    @Test
    public void shouldFilterByStatus() {
        final JobInstance instance1 = new JobInstance("test"
        );
        final JobInstance instance2 = new JobInstance("test2"
        );
        instance2.setState(JobState.Assigned);
        JobInstances instances = new JobInstances(instance1, instance2);
        JobInstances actual = instances.filterByState(JobState.Assigned);
        assertThat(actual.size(), is(1));
        assertThat(actual.get(0).getState(), is(JobState.Assigned));
    }

    @Test
    public void shouldGetMostRecentCompletedBuild() {
        JobInstances jobInstances = mixedBuilds();
        JobInstance mostRecentCompleted = jobInstances.mostRecentCompleted();
        assertThat(mostRecentCompleted, is(jobInstances.get(2)));
    }

    @Test
    public void shouldGetMostRecentPassedBuild() {
        JobInstances jobInstances = mixedBuilds();
        JobInstance mostRecent = jobInstances.mostRecentPassed();
        assertThat(mostRecent, is(jobInstances.get(1)));
    }

    @Test
    public void shouldGetMostRecentPassedWhenBuilding() {
        JobInstances jobInstances = new JobInstances(passed("passed"), building("unit"));
        JobInstance mostRecent = jobInstances.mostRecentPassed();
        assertThat(mostRecent.getName(), is("passed"));
    }

    @Test
    public void shouldGetMostRecentPassedBuildIfThereAreFailedBuilds() {
        JobInstances jobInstances = new JobInstances(failed("foo"), passed("foo"));
        JobInstance mostRecent = jobInstances.mostRecentPassed();
        assertThat(mostRecent, is(jobInstances.get(1)));
    }

    private JobInstances mixedBuilds() {
        JobInstances instances = new JobInstances();
        JobInstance assigned = JobInstanceMother.assigned("test");
        instances.add(assigned);
        JobInstance oldest = passed("oldest");
        oldest.completed(new Date());
        instances.add(oldest);
        JobInstance newest = failed("newest");
        newest.completed(new Date());
        instances.add(newest);
        instances.add(JobInstance.NULL);
        JobInstance scheduled = scheduled("redHerring");
        instances.add(scheduled);
        return instances;
    }

    @Test
    public void shouldReturnNullObjectWhenNoMostRecentPassedInstance() {
        JobInstance actual = new JobInstances().mostRecentPassed();
        assertThat(actual.isNull(), is(true));
    }

    @Test public void shouldReturnStatusBuildingWhenAnyBuildsAreBuilding() {
        JobInstances builds = new JobInstances();
        builds.add(completed("passports", JobResult.Passed));
        builds.add(completed("visas", JobResult.Cancelled));
        builds.add(scheduled("flights"));
        assertThat(builds.stageState(), is(Building));
    }

    @Test public void jobShouldBeCancelledWhenNoActiveBuildAndHaveAtLeastOneCancelledJob() {
        JobInstances builds = new JobInstances();
        builds.add(completed("passports", JobResult.Passed));
        builds.add(completed("passports-failed", JobResult.Failed));
        builds.add(completed("visas", JobResult.Cancelled));
        builds.add(completed("flights", JobResult.Cancelled));
        assertThat(builds.stageState(), is(Cancelled));
    }

    @Test public void shouldReturnStatusFailingWhenAnyPlansHaveFailedAndNotAllAreCompleted() {
        JobInstances builds = new JobInstances();
        builds.add(completed("passports", JobResult.Failed));
        builds.add(completed("visas", JobResult.Passed));
        builds.add(scheduled("flights"));
        assertThat(builds.stageState(), is(Failing));
    }

    @Test
    public void shouldReturnLatestTransitionDate() {
        Date expectedLatest = date(3908, 10, 12);
        Date actualLatest = new JobInstances(
                completed(completed("job1"), JobResult.Failed, expectedLatest),
                completed(completed("job1"), JobResult.Failed, date(3908, 10, 11)),
                completed(completed("job1"), JobResult.Failed, date(3908, 10, 5))).latestTransitionDate();
        assertThat(actualLatest,is(expectedLatest));
    }

    private Date date(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}
