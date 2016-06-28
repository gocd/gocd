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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.ArrayList;

import com.thoughtworks.go.domain.JobIdentifier;

/**
 * @understands information about what caused a build to fail
 */
public class StageTestRuns {

    private LinkedList<FailingTestsInPipeline> failingTestsInPipelines = new LinkedList<>();
    private Map<Integer, FailingTestsInPipeline> failingTestsInPipelineByCounter = new HashMap<>();
    private int numberOfTests;
    private final int failureCounts;
    private final int errorCounts;

    public StageTestRuns(int numberOfTests, int failureCounts, int errorCounts) {
      this.numberOfTests = numberOfTests;
        this.failureCounts = failureCounts;
        this.errorCounts = errorCounts;
    }

    public int numberOfTests() {
        return numberOfTests;
    }

    public FailingTestsInPipeline add(int pipelineCounter, String pipelineLabel) {
        if (!failingTestsInPipelineByCounter.containsKey(pipelineCounter)) {
            FailingTestsInPipeline failingTestsInPipeline = new FailingTestsInPipeline(pipelineLabel, pipelineCounter);
            failingTestsInPipelines.add(failingTestsInPipeline);
            failingTestsInPipelineByCounter.put(pipelineCounter, failingTestsInPipeline);
        }
        return failingTestsInPipelineByCounter.get(pipelineCounter);
    }

    // assume piplines are added recent to oldest ...  maybe write a runtime assertion for this ?
    public void add(int pipelineCounter, String pipelineLabel, String suiteName, String testName, TestStatus testStatus, JobIdentifier jobIdentifier) {
        FailingTestsInPipeline pipelineForThisTestCase = add(pipelineCounter, pipelineLabel);
        FailingTestsInPipeline currentPipeline = failingTestsInPipelines.getFirst();
        if ((pipelineForThisTestCase == currentPipeline) || currentPipeline.contains(suiteName, testName)) {
            pipelineForThisTestCase.add(suiteName, testName, testStatus, jobIdentifier);
        }
    }

    public void removeDuplicateTestEntries(){
        removeTestsThatStartedFailingInPreviousPipelines();
        removeTestsThatWereFixedInInterimPipelines();
    }

    private void removeTestsThatWereFixedInInterimPipelines() {
        Iterator<FailingTestsInPipeline> pipelineIterator = failingTestsInPipelines.iterator();
        while (pipelineIterator.hasNext()) {
            FailingTestsInPipeline failingTestsInPipeline = pipelineIterator.next();
            FailingTestsInPipeline previousPipeline = previousPipeline(failingTestsInPipeline);
            if (previousPipeline != null) {
                for (TestSuite failingSuite : failingTestsInPipeline.failingSuites()) {
                    for (TestInformation failingTest : failingSuite.tests()) {
                        if (!previousPipeline.contains(failingSuite.fullName(), failingTest.getName())) {
                            removeAllTestsBeyond(failingSuite.fullName(), failingTest.getName(), previousPipeline);
                        }
                    }
                }
            }
        }
    }

    private void removeAllTestsBeyond(String suiteName, String testName, FailingTestsInPipeline beyondPipeline) {
        FailingTestsInPipeline previous = previousPipeline(beyondPipeline);
        while (previous != null) {
            previous.removeTest(suiteName, testName);
            previous = previousPipeline(previous);
        }
    }

    private void removeTestsThatStartedFailingInPreviousPipelines() {
        Iterator<FailingTestsInPipeline> pipelineIterator = failingTestsInPipelines.iterator();
        while (pipelineIterator.hasNext()) {
            FailingTestsInPipeline failingTestsInPipeline = pipelineIterator.next();
            FailingTestsInPipeline previousPipeline = previousPipeline(failingTestsInPipeline);
            if (previousPipeline != null) {
              failingTestsInPipeline.removeDuplicateTestEntries(previousPipeline);
            }
        }
    }

    private FailingTestsInPipeline previousPipeline(FailingTestsInPipeline pipeline) {
        int prevIndex = failingTestsInPipelines.indexOf(pipeline) + 1;
        if (prevIndex < failingTestsInPipelines.size()) {
            return failingTestsInPipelines.get(prevIndex);
        } else {
            return null;
        }
    }

    public List<FailingTestsInPipeline> failingTestsInPipelines() {
        return failingTestsInPipelines;
    }

    public int totalFailureCount() {
        return failureCounts;
    }

    public int totalErrorCount() {
        return errorCounts;
    }

    int count(TestStatus status) {
        int count = 0;
        for (FailingTestsInPipeline failingTestsInPipeline : failingTestsInPipelines) {
            count += failingTestsInPipeline.count(status);
        }
        return count;
    }

    public void addUser(int pipelineCounter, String pipelineLabel, String user) {
        add(pipelineCounter, pipelineLabel).addUser(user);
    }

    public List<Integer> failingCounters() {
        List<Integer> list = new ArrayList<>(failingTestsInPipelineByCounter.keySet());
        Collections.sort(list);
        return list;
    }

    public List<TestSuite> failingTestSuitesForNthPipelineRun(int n) {
        return failingTestsInPipelines.get(n).failingSuites();
    }
}
