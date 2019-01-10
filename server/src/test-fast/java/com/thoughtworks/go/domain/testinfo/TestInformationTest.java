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

import java.util.Arrays;

import com.thoughtworks.go.domain.JobIdentifier;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class TestInformationTest {

    @Test
    public void testShouldSortJobIdentifiers() {
        TestInformation info = new TestInformation("test", TestStatus.Error);
        JobIdentifier first = new JobIdentifier(null, 1, null, null, null, "job1");
        JobIdentifier second = new JobIdentifier(null, 1, null, null, null, "job");

        info.addJob(first);
        info.addJob(second);

        assertThat(info.getJobs(), is(Arrays.asList(second, first)));
    }

    @Test
    public void equalsTrueWhenStatusSameNameSameAndJobIdentifiersHaveSameNames() {
        TestInformation info1 = new TestInformation("test", TestStatus.Error);
        info1.addJob(new JobIdentifier(null, 1, null, null, null, "job1"));
        info1.addJob(new JobIdentifier(null, 2, null, null, null, "job"));

        TestInformation info2 = new TestInformation("test", TestStatus.Error);
        info2.addJob(new JobIdentifier(null, 3, null, null, null, "job1"));
        info2.addJob(new JobIdentifier(null, 4, null, null, null, "job"));

        assertThat(info1, is(info2));
    }

    @Test
    public void equalsFalseWhenStatusDifferentNameSameAndJobIdentifiersHaveSameNames() {
        TestInformation info1 = new TestInformation("test", TestStatus.Error);
        info1.addJob(new JobIdentifier(null, 1, null, null, null, "job1"));
        info1.addJob(new JobIdentifier(null, 2, null, null, null, "job"));

        TestInformation info2 = new TestInformation("test", TestStatus.Failure);
        info2.addJob(new JobIdentifier(null, 3, null, null, null, "job1"));
        info2.addJob(new JobIdentifier(null, 4, null, null, null, "job"));

        assertThat(info1, not(info2));
    }

    @Test
    public void equalsFalseWhenStatusSameNameSameAndJobIdentifiersHaveDifferentNames() {
        TestInformation testInformation1 = new TestInformation("testA", TestStatus.Error);
        testInformation1.addJob(new JobIdentifier("", -1, "", "", "", "job-1"));

        TestInformation testInformation2 = new TestInformation("testA", TestStatus.Error);
        testInformation2.addJob(new JobIdentifier("", -1, "", "", "", "job-2"));

        assertThat(testInformation1, not(testInformation2));
        assertThat(testInformation2, not(testInformation1));
        assertThat(Arrays.asList(testInformation1),not(hasItem(testInformation2)));
    }

    @Test
    public void equalsFalseWhenStatusSameNameDifferentAndJobIdentifiersHaveSameNames() {
        TestInformation info1 = new TestInformation("test", TestStatus.Error);
        info1.addJob(new JobIdentifier(null, 1, null, null, null, "job1"));
        info1.addJob(new JobIdentifier(null, 2, null, null, null, "job"));

        TestInformation info2 = new TestInformation("anotherTest", TestStatus.Error);
        info2.addJob(new JobIdentifier(null, 3, null, null, null, "job1"));
        info2.addJob(new JobIdentifier(null, 4, null, null, null, "job"));

        assertThat(info1, not(info2));
        assertThat(info2, not(info1));
    }

    @Test
    public void equalsFalseWhenStatusSameNameSameAndJobIdentifiersHaveDifferentNamesButSameLength() {
        TestInformation info1 = new TestInformation("test", TestStatus.Error);
        info1.addJob(new JobIdentifier(null, 1, null, null, null, "job1"));
        info1.addJob(new JobIdentifier(null, 2, null, null, null, "job"));

        TestInformation info2 = new TestInformation("test", TestStatus.Error);
        info2.addJob(new JobIdentifier(null, 3, null, null, null, "job1"));
        info2.addJob(new JobIdentifier(null, 4, null, null, null, "job33"));

        assertThat(info1, not(info2));
    }

    @Test
    public void equalsFalseWhenStatusSameNameSameAndJobIdentifiersAreASubset() {
        TestInformation info1 = new TestInformation("test", TestStatus.Error);
        info1.addJob(new JobIdentifier(null, 1, null, null, null, "job1"));
        info1.addJob(new JobIdentifier(null, 2, null, null, null, "job"));

        TestInformation info2 = new TestInformation("test", TestStatus.Error);
        info2.addJob(new JobIdentifier(null, 3, null, null, null, "job1"));
        info2.addJob(new JobIdentifier(null, 4, null, null, null, "job"));
        info2.addJob(new JobIdentifier(null, 4, null, null, null, "job33"));

        assertThat(info1, not(info2));
    }

}
