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

import com.thoughtworks.go.domain.JobIdentifier;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

public class FailingTestsInPipelineTest {

    private FailingTestsInPipeline failingTests;

    @Before public void setUp() throws Exception {
        failingTests = new FailingTestsInPipeline("1.2", 2);
    }

    @Test
    public void shouldOrderSuitesAlphabetically() throws Exception {
        failingTests.add("suiteB", "testA", TestStatus.Error, new JobIdentifier("", -1, "", "", "", "job-1"));
        failingTests.add("suiteA", "testA", TestStatus.Error, new JobIdentifier("", -1, "", "", "", "job-1"));
        failingTests.add("suiteZ", "testA", TestStatus.Error, new JobIdentifier("", -1, "", "", "", "job-1"));
        failingTests.add("suiteD", "testA", TestStatus.Error, new JobIdentifier("", -1, "", "", "", "job-1"));

        List<TestSuite> suites = failingTests.failingSuites();
        assertThat(suites.size(), is(4));
        assertThat(suites.get(0).fullName(), is("suiteA"));
        assertThat(suites.get(1).fullName(), is("suiteB"));
        assertThat(suites.get(2).fullName(), is("suiteD"));
        assertThat(suites.get(3).fullName(), is("suiteZ"));
    }


    @Test
    public void shouldRemoveSuiteIfNoTestsAfterDuplicateRemoval() {
        failingTests.add("suiteA", "testA", TestStatus.Error, new JobIdentifier("", 3, "", "", "", "job-1"));
        failingTests.add("suiteB", "testB", TestStatus.Error, new JobIdentifier("", 3, "", "", "", "job-1"));
        FailingTestsInPipeline failingTests1 = new FailingTestsInPipeline("1.3", 3);
        failingTests1.add("suiteA", "testA", TestStatus.Error, new JobIdentifier("", 4, "", "", "", "job-1"));
        failingTests.removeDuplicateTestEntries(failingTests1);
        List<TestSuite> failingSuites = failingTests.failingSuites();
        assertThat(failingSuites.size(), is(1));
        assertThat(failingSuites.get(0).fullName(), is("suiteB"));
    }

    @Test
    public void shouldRemoveDuplicate(){
        failingTests.add("suiteA", "testA", TestStatus.Error, new JobIdentifier("", 3, "", "", "", "job-1"));
        failingTests.add("suiteA", "testB", TestStatus.Error, new JobIdentifier("", 3, "", "", "", "job-1"));
        FailingTestsInPipeline failingTests1 = new FailingTestsInPipeline("1.3", 3);
        failingTests1.add("suiteA", "testA", TestStatus.Error, new JobIdentifier("", 4, "", "", "", "job-1"));
        failingTests1.add("suiteA", "testB", TestStatus.Error, new JobIdentifier("", 4, "", "", "", "job-2"));
        failingTests.removeDuplicateTestEntries(failingTests1);
        assertThat(failingTests.failingSuites().size(),is(1));
        List<TestInformation> tests = failingTests.failingSuites().get(0).tests();
        assertThat(tests.size(),is(1));
        assertThat(tests.get(0).getName(),is("testB"));
    }

    @Test
    public void shouldUnderstandPipelineCounter() throws Exception {
        FailingTestsInPipeline pipeline = new FailingTestsInPipeline("label-10", 10);
        assertThat(pipeline.getCounter(), is(10));
    }
}
