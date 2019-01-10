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

package com.thoughtworks.go.domain.testinfo;

import java.util.List;
import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import com.thoughtworks.go.domain.JobIdentifier;

public class StageTestRunsTest {

    @Test
    public void shouldReturnAllPipelineCounters() throws Exception {
        StageTestRuns stageTestRuns = new StageTestRuns(999, 0, 0);
        JobIdentifier jobIdentifier = new JobIdentifier(null, 1, null, null, null, "job1");
        stageTestRuns.add(18, "1.8", "testSuite1", "testName1", TestStatus.Failure, jobIdentifier);
        stageTestRuns.add(17, "1.7", "testSuite1", "testName1", TestStatus.Failure, jobIdentifier);
        stageTestRuns.add(16, "1.6", "testSuite1", "testName1", TestStatus.Failure, jobIdentifier);
        stageTestRuns.removeDuplicateTestEntries();
        assertThat(stageTestRuns.failingCounters(), is(Arrays.asList(16, 17, 18)));
    }

    @Test
    public void removeDuplicateTestEntriesEliminatesSusequentDuplicateFailures() throws Exception {
        StageTestRuns stageTestRuns = new StageTestRuns(999, 0, 0);
        JobIdentifier jobIdentifier = new JobIdentifier(null, 1, null, null, null, "job1");
        stageTestRuns.add(18, "1.8", "testSuite1", "testName1", TestStatus.Failure, jobIdentifier);
        stageTestRuns.add(17, "1.7", "testSuite1", "testName1", TestStatus.Failure, jobIdentifier);
        stageTestRuns.add(16, "1.6", "testSuite1", "testName1", TestStatus.Failure, jobIdentifier);
        stageTestRuns.removeDuplicateTestEntries();

        assertThat(stageTestRuns.failingTestsInPipelines().size(), is(3));
        FailingTestsInPipeline pipeline8 = stageTestRuns.failingTestsInPipelines().get(0);
        FailingTestsInPipeline pipeline7 = stageTestRuns.failingTestsInPipelines().get(1);
        FailingTestsInPipeline pipeline6 = stageTestRuns.failingTestsInPipelines().get(2);

        assertThat(pipeline8.failingSuites().size(), is(0));
        assertThat(pipeline7.failingSuites().size(), is(0));
        assertThat(pipeline6.failingSuites().size(), is(1));
    }

    @Test
    public void removeDuplicateTestEntriesDoesNotEliminatesSusequentDuplicateFailuresWhenJobsAreNotIdentical() throws Exception {
        StageTestRuns stageTestRuns = new StageTestRuns(999, 0, 0);
        JobIdentifier job1 = new JobIdentifier(null, 1, null, null, null, "job1");
        JobIdentifier job2 = new JobIdentifier(null, 1, null, null, null, "job2");
        stageTestRuns.add(18, "1.8", "testSuite1", "testName1", TestStatus.Failure, job1);
        stageTestRuns.add(18, "1.8", "testSuite1", "testName1", TestStatus.Failure, job2);
        stageTestRuns.add(17, "1.7", "testSuite1", "testName1", TestStatus.Failure, job2);
        stageTestRuns.add(16, "1.6", "testSuite1", "testName1", TestStatus.Failure, job1);
        stageTestRuns.removeDuplicateTestEntries();

        FailingTestsInPipeline pipeline8 = stageTestRuns.failingTestsInPipelines().get(0);
        FailingTestsInPipeline pipeline7 = stageTestRuns.failingTestsInPipelines().get(1);
        FailingTestsInPipeline pipeline6 = stageTestRuns.failingTestsInPipelines().get(2);

        assertThat(pipeline8.failingSuites().size(), is(1));
        assertThat(pipeline8.failingSuites().get(0).tests().get(0).getJobNames().size(), is(2));
        assertThat(pipeline8.failingSuites().get(0).tests().get(0).getJobNames().get(0), is("job1"));
        assertThat(pipeline8.failingSuites().get(0).tests().get(0).getJobNames().get(1), is("job2"));

        assertThat(pipeline7.failingSuites().size(), is(1));
        assertThat(pipeline7.failingSuites().get(0).tests().get(0).getJobNames().size(), is(1));
        assertThat(pipeline7.failingSuites().get(0).tests().get(0).getJobNames().get(0), is("job2"));

        assertThat(pipeline6.failingSuites().size(), is(1));
        assertThat(pipeline6.failingSuites().get(0).tests().get(0).getJobNames().size(), is(1));
        assertThat(pipeline6.failingSuites().get(0).tests().get(0).getJobNames().get(0), is("job1"));
    }

    @Test
    public void removeDuplicateTestEntriesFromNonAdjacentRuns() throws Exception {
        StageTestRuns stageTestRuns = new StageTestRuns(999, 0, 0);
        JobIdentifier jobIdentifier = new JobIdentifier(null, 1, null, null, null, "job1");
        stageTestRuns.add(10, "1.0", "suite1", "test1-2", TestStatus.Failure, jobIdentifier);
        stageTestRuns.add(10, "1.0", "suite1", "test1-1", TestStatus.Error, jobIdentifier);
        stageTestRuns.add(11, "1.1", "suite1", "test1-1", TestStatus.Error, jobIdentifier);
        stageTestRuns.add(9, "0.9", "suite1", "test1-2", TestStatus.Failure, jobIdentifier);
        stageTestRuns.removeDuplicateTestEntries();

        FailingTestsInPipeline pipeline10 = stageTestRuns.failingTestsInPipelines().get(0);
        FailingTestsInPipeline pipeline11 = stageTestRuns.failingTestsInPipelines().get(1);
        FailingTestsInPipeline pipeline09 = stageTestRuns.failingTestsInPipelines().get(2);
        assertThat(pipeline10.failingSuites().size(), is(1));
        assertThat(pipeline10.failingSuites().get(0).tests().size(), is(1));
        assertThat(pipeline11.failingSuites().size(), is(1));
        assertThat(pipeline11.failingSuites().get(0).tests().size(), is(1));
        assertThat(pipeline09.failingSuites().size(), is(0));


    }

    @Test
    public void removeDuplicateTestEntriesShouldRemoveFailuresThatHaveBeenFixedInSubsequentPipelines() throws Exception {
        StageTestRuns stageTestRuns = new StageTestRuns(999, 0, 0);
        JobIdentifier jobIdentifier = new JobIdentifier(null, 1, null, null, null, "job1");
        stageTestRuns.add(18, "1.8", "testSuite1", "testName0", TestStatus.Failure, jobIdentifier);
        stageTestRuns.add(17, "1.7", "testSuite1", "testName0", TestStatus.Failure, jobIdentifier);
        stageTestRuns.add(17, "1.7", "testSuite1", "testName1", TestStatus.Failure, jobIdentifier);
        stageTestRuns.removeDuplicateTestEntries();

        assertThat(stageTestRuns.failingTestsInPipelines().size(), is(2));
        FailingTestsInPipeline pipeline8 = stageTestRuns.failingTestsInPipelines().get(0);
        FailingTestsInPipeline pipeline7 = stageTestRuns.failingTestsInPipelines().get(1);

        assertThat(pipeline8.failingSuites().size(), is(0));

        assertThat(pipeline7.failingSuites().size(), is(1));
        assertThat(pipeline7.failingSuites().get(0).tests().size(), is(1));
        assertThat(pipeline7.failingSuites().get(0).tests().get(0).getName(), is("testName0"));
    }

    @Test
    public void shouldReturnTheTotalNumberOfErrorsAndFailuresRollingUpFailuresInMultipleJobsToSingleFailure() throws Exception {
        StageTestRuns stageTestRuns = new StageTestRuns(999, 3, 2);
        JobIdentifier jobIdentifier1 = new JobIdentifier(null, 1, null, null, null, "job1");
        JobIdentifier jobIdentifier2 = new JobIdentifier(null, 1, null, null, null, "job2");
        stageTestRuns.add(18, "lbl", "testSuite1", "testName1", TestStatus.Failure, jobIdentifier1);
        stageTestRuns.add(18, "lbl", "testSuite1", "testName1", TestStatus.Failure, jobIdentifier2);
        stageTestRuns.add(18, "lbl", "testSuite1", "testName2", TestStatus.Error, jobIdentifier1);
        stageTestRuns.add(18, "lbl", "testSuite1", "testName3", TestStatus.Failure, jobIdentifier1);
        stageTestRuns.add(18, "lbl", "testSuite1", "testName4", TestStatus.Error, jobIdentifier1);
        stageTestRuns.add(18, "lbl", "testSuite1", "testName5", TestStatus.Failure, jobIdentifier1);
        stageTestRuns.add(17, "lbl", "testSuite1", "testName4", TestStatus.Error, jobIdentifier1);
        stageTestRuns.add(17, "lbl", "testSuite1", "testName5", TestStatus.Failure, jobIdentifier1);
        stageTestRuns.add(16, "lbl", "testSuite1", "testName6", TestStatus.Failure, jobIdentifier1);
        stageTestRuns.removeDuplicateTestEntries();

        assertThat(stageTestRuns.count(TestStatus.Error), is(2));
        assertThat(stageTestRuns.count(TestStatus.Failure), is(3));
    }

    @Test
    public void shouldBeAbletoAddUserToANonExistentPipelineInstance() {
        StageTestRuns stageTestRuns = new StageTestRuns(999, 0, 0);
        stageTestRuns.addUser(3, "3", "jj");
        List<FailingTestsInPipeline> pipelines = stageTestRuns.failingTestsInPipelines();
        assertThat(pipelines.size(), is(1));
        assertThat(pipelines.get(0).getLabel(), is("3"));
        assertThat(pipelines.get(0).users().get(0), is("jj"));
    }
}
