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

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.thoughtworks.go.helper.JobInstanceMother;
import static com.thoughtworks.go.helper.JobInstanceMother.building;
import static com.thoughtworks.go.helper.JobInstanceMother.completed;
import static com.thoughtworks.go.helper.JobInstanceMother.failed;
import static com.thoughtworks.go.helper.JobInstanceMother.passed;
import static com.thoughtworks.go.helper.JobInstanceMother.scheduled;
import com.thoughtworks.go.util.ClassMockery;
import com.thoughtworks.go.util.FileUtil;
import static org.hamcrest.Matchers.is;
import org.hamcrest.core.Is;
import org.jmock.Expectations;
import static org.jmock.Expectations.equal;
import org.junit.After;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class JobInstancesTest {
    private ClassMockery context = new ClassMockery();
    private File artifactsRoot;

    @After public void tearDown() {
        FileUtil.deleteFolder(artifactsRoot);
    }

    @Test
    public void shouldFilterByStatus() {
        final JobInstance instance1 = new JobInstance("test"
        );
        final JobInstance instance2 = new JobInstance("test2"
        );
        instance2.setState(JobState.Assigned);
        JobInstances instances = new JobInstances(instance1, instance2);
        JobInstances actual = instances.filterByState(JobState.Assigned);
        assertThat(actual.size(), is(equal(1)));
        assertThat(actual.get(0).getState(), is(equal(JobState.Assigned)));
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
        assertThat(builds.stageState(), Is.is(StageState.Building));
    }

    @Test public void jobShouldBeCancelledWhenNoActiveBuildAndHaveAtLeastOneCancelledJob() {
        JobInstances builds = new JobInstances();
        builds.add(completed("passports", JobResult.Passed));
        builds.add(completed("passports-failed", JobResult.Failed));
        builds.add(completed("visas", JobResult.Cancelled));
        builds.add(completed("flights", JobResult.Cancelled));
        assertThat(builds.stageState(), Is.is(StageState.Cancelled));
    }

    @Test public void shouldReturnStatusFailingWhenAnyPlansHaveFailedAndNotAllAreCompleted() {
        JobInstances builds = new JobInstances();
        builds.add(completed("passports", JobResult.Failed));
        builds.add(completed("visas", JobResult.Passed));
        builds.add(scheduled("flights"));
        assertThat(builds.stageState(), Is.is(StageState.Failing));
    }

    @Test public void shouldReturnAggregatestacktraces() throws Exception {
        JobInstances builds = new JobInstances();
        final JobInstance withStackTrace = context.mock(JobInstance.class, "withStackTrace");
        final JobInstance withoutStackTrace = context.mock(JobInstance.class, "withoutStackTrace");
        builds.add(withStackTrace);
        builds.add(withoutStackTrace);
        context.checking(new Expectations() {
            {
                one(withStackTrace).getStacktrace();
                will(returnValue("stacktrace"));
                one(withStackTrace).getName();
                will(returnValue("name"));
                one(withoutStackTrace).getStacktrace();
                will(returnValue(""));
                one(withoutStackTrace).getName();
                will(returnValue("name1"));
            }

        });
        List<JobInstance> stacktraces = builds.withNonEmptyStacktrace();
        assertThat(stacktraces.size(), is(1));
    }

    @Test public void shouldReturnAggregateBuildError() throws Exception {
        JobInstances builds = new JobInstances();
        final JobInstance withError = context.mock(JobInstance.class, "withError");
        final JobInstance withoutErrors = context.mock(JobInstance.class, "withoutErrors");
        builds.add(withError);
        builds.add(withoutErrors);
        context.checking(new Expectations() {
            {
                one(withError).getBuildError();
                will(returnValue("errors"));
                one(withError).getName();
                will(returnValue("name"));
                one(withoutErrors).getBuildError();
                will(returnValue(""));
                one(withoutErrors).getName();
                will(returnValue("name1"));
            }

        });
        List<JobInstance> jobErrors = builds.withNonEmptyBuildErrors();
        assertThat(jobErrors.size(), is(1));
    }

    @Test
    public void shouldReturnLatestTransitionDate() {
        Date expectedLatest = date(2008, 10, 12);
        Date actualLatest = new JobInstances(
                completed(completed("job1"), JobResult.Failed, expectedLatest),
                completed(completed("job1"), JobResult.Failed, date(2008, 10, 11)),
                completed(completed("job1"), JobResult.Failed, date(2008, 10, 5))).latestTransitionDate();
        assertThat(actualLatest,is(expectedLatest));
    }

    private Date date(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}
