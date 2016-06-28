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
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.thoughtworks.go.domain.JobIdentifier;

/**
 * @understands information about failing tests scoped by pipeline
 */
public class FailingTestsInPipeline {
    private final SortedMap<String, TestSuite> suites;
    private final String label;
    private List<String> users;
    private int counter;

    public FailingTestsInPipeline(String label, int counter) {
        this.label = label;
        this.counter = counter;
        suites = new TreeMap<>();
        users = new ArrayList<>();
    }

    public List<TestSuite> failingSuites() {
        return new ArrayList<>(this.suites.values());
    }


    private void removeIfEmpty(TestSuite testSuite) {
        if (testSuite.isEmpty()) {
            suites.remove(testSuite.fullName());
        }
    }

    public void add(String suiteName, String testName, TestStatus testStatus, JobIdentifier jobIdentifier) {
        TestSuite suite = getSuite(suiteName);
        suite.addTest(testName, testStatus, jobIdentifier);
    }

    private TestSuite getSuite(String suiteName) {
        if (!suites.containsKey(suiteName)) {
            suites.put(suiteName, new TestSuite(suiteName));
        }
        return suites.get(suiteName);
    }

    public boolean hasLabel(String pipelineLabel) {
        return label.equals(pipelineLabel);
    }

    public String getLabel() {
        return label;
    }

    public int errorCount() {
        return count(TestStatus.Error);
    }

    public int failureCount() {
        return count(TestStatus.Failure);
    }

    int count(TestStatus status) {
        int errorCount = 0;
        for (TestSuite testSuite : failingSuites()) {
            errorCount += testSuite.countOfStatus(status);
        }
        return errorCount;
    }

    public List<String> users() {
        return users;
    }

    public void addUser(String user) {
        users.add(user);
    }

    public boolean contains(String suiteName, String testName) {
        if (!suites.containsKey(suiteName)) {
            return false;
        }
        return getSuite(suiteName).contains(testName);
    }

    public void removeDuplicateTestEntries(FailingTestsInPipeline anotherPipeline) {
        for (TestSuite testSuite : anotherPipeline.failingSuites()) {
            TestSuite currentTestSuite = getSuite(testSuite.fullName());
            if (currentTestSuite != null) {
                currentTestSuite.removeDuplicateTestEntries(testSuite);
                removeIfEmpty(currentTestSuite);
            }
        }
    }

    public void removeTest(String suiteName, String testName) {
        TestSuite suite = suites.get(suiteName);
        if (suite != null) {
            suite.removeTest(testName);
            removeIfEmpty(suite);
        }
    }

    public int getCounter() {
        return counter;
    }
}
