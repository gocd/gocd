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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.thoughtworks.go.domain.JobIdentifier;

/**
 * @understands grouped information regarding tests that have run within a test suite
 */
public class TestSuite {
    private final SortedSet<TestInformation> tests;
    private final String name;

    public TestSuite(String name) {
        this.name = name;
        this.tests = new TreeSet<>(new Comparator<TestInformation>() {
            public int compare(TestInformation testInformation1, TestInformation testInformation2) {
                return testInformation1.getName().compareTo(testInformation2.getName());
            }
        });
    }

    public List<TestInformation> tests() {
        return new ArrayList<>(tests);
    }

    public String fullName() {
        return name;
    }


    public void addTest(String testName, TestStatus testStatus, JobIdentifier jobIdentifier) {
        TestInformation test = getTest(testName, testStatus);
        test.addJob(jobIdentifier);
    }

    private TestInformation getTest(String testName, TestStatus testStatus) {
        TestInformation test = existingTest(testName, testStatus);
        if (test != null) { return test; }

        test = new TestInformation(testName, testStatus);
        tests.add(test);
        return test;
    }

    private TestInformation existingTest(String testName, TestStatus testStatus) {
        for (TestInformation test : tests) {
            if(test.getName().equals(testName) && test.getStatus().equals(testStatus)) {
                return test;
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return tests.isEmpty();
    }

    public int countOfStatus(TestStatus status) {
        int count = 0;
        for (TestInformation test : tests) {
            if (status.equals(test.getStatus())) {
                count += 1;
            }
        }
        return count;
    }

    public boolean contains(String testName) {
       for (TestInformation test : tests) {
            if(test.getName().equals(testName)) return true;
       }
        return false;
    }

    public void removeDuplicateTestEntries(TestSuite anotherTestSuite) {
        for (TestInformation testInformation : anotherTestSuite.tests()) {
            if(tests().contains(testInformation)){
                tests.remove(testInformation);
            }
        }
    }

    public void removeTest(String testName) {
        Iterator<TestInformation> tests = this.tests.iterator();
        while (tests.hasNext()) {
            if (tests.next().getName().equals(testName)) {
                tests.remove();
            }
        }
    }
}
