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

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;
import com.thoughtworks.go.domain.JobIdentifier;

public class TestSuiteTest {
    private TestSuite testSuite;

    @Before public void setUp() throws Exception {
        testSuite = new TestSuite("blah-suite");
    }

    @Test
    public void shouldReturnTestOrderedAlphabetically() throws Exception {
        testSuite.addTest("testB", TestStatus.Error, new JobIdentifier("", -1, "", "", "", "job-1"));
        testSuite.addTest("testA", TestStatus.Error, new JobIdentifier("", -1, "", "", "", "job-1"));
        testSuite.addTest("testC", TestStatus.Failure, new JobIdentifier("", -1, "", "", "", "job-1"));

        List<TestInformation> tests = testSuite.tests();
        assertThat(tests.get(0).getName(),is("testA"));
        assertThat(tests.get(1).getName(),is("testB"));
        assertThat(tests.get(2).getName(),is("testC"));
    }



    @Test
    public void testContainsTestName(){
        testSuite.addTest("testB", TestStatus.Error, new JobIdentifier("", -1, "", "", "", "job-1"));
        testSuite.addTest("testA", TestStatus.Error, new JobIdentifier("", -1, "", "", "", "job-1"));
        assertThat(testSuite.contains("testA"),is(true));
        assertThat(testSuite.contains("testB"),is(true));
        assertThat(testSuite.contains("testC"),is(false));
        assertThat(testSuite.contains(""),is(false));
        assertThat(testSuite.contains(null),is(false));
    }

    @Test
    public void shouldRemoveTestsThatHaveSameContext(){
        testSuite.addTest("testB", TestStatus.Error, new JobIdentifier("", -1, "", "", "", "job-1"));
        testSuite.addTest("testA", TestStatus.Error, new JobIdentifier("", -1, "", "", "", "job-1"));

        TestSuite testSuite1 = new TestSuite("another-suite");
        testSuite1.addTest("testA", TestStatus.Error, new JobIdentifier("", 23, "", "", "", "job-1"));

        testSuite.removeDuplicateTestEntries(testSuite1);
        List<TestInformation> list = testSuite.tests();
        assertThat(list.size(),is(1));
        assertThat(list.get(0).getName(),is("testB"));
    }

    @Test
    public void shouldNotRemoveTestsThatHaveSameNameAndDiffernetContext(){
        testSuite.addTest("testB", TestStatus.Error, new JobIdentifier("", -1, "", "", "", "job-1"));
        testSuite.addTest("testA", TestStatus.Error, new JobIdentifier("", -1, "", "", "", "job-1"));

        TestSuite testSuite1 = new TestSuite("another-suite");
        testSuite1.addTest("testA", TestStatus.Error, new JobIdentifier("", 23, "", "", "", "job-2"));

        testSuite.removeDuplicateTestEntries(testSuite1);
        List<TestInformation> list = testSuite.tests();
        assertThat(list.size(),is(2));
    }
}
