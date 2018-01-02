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

package com.thoughtworks.go.server.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class BuildTestSuite implements Serializable {
    private float duration;

    private String name;

    private List testCases;

    public BuildTestSuite(String name, float duration) {
        this.duration = duration;
        this.name = name;
        this.testCases = null;
    }

    public int getNumberOfErrors() {
        return checkTestCases() ? getErrorTestCases().size() : 0;
    }

    public String getName() {
        return name;
    }

    public int getNumberOfFailures() {
        return checkTestCases() ? getFailingTestCases().size() : 0;
    }

    public int getNumberOfTests() {
        return checkTestCases() ? testCases.size() : 0;
    }

    public float getDurationInSeconds() {
        return duration;
    }

    public List getErrorTestCases() {
        return findTestCases(BuildTestCaseResult.ERROR);
    }

    private List findTestCases(BuildTestCaseResult filter) {
        if (checkTestCases()) {
            List cases = new ArrayList();
            for (int i = 0; i < testCases.size(); i++) {
                BuildTestCase testCase = (BuildTestCase) testCases.get(i);
                BuildTestCaseResult result = testCase.getResult();
                if (result.equals(filter)) {
                    cases.add(testCase);
                }
            }
            return cases;
        }
        return null;
    }

    public List getFailingTestCases() {
        return findTestCases(BuildTestCaseResult.FAILED);
    }

    public List getPassedTestCases() {
        return findTestCases(BuildTestCaseResult.PASSED);
    }

    public void appendTestCases(List tests) {
        this.testCases = tests;
    }

    public void addTestCase(BuildTestCase testCase) {
        if (!checkTestCases()) {
            this.testCases = new LinkedList();
        }
        this.testCases.add(testCase);
    }

    private boolean checkTestCases() {
        return this.testCases != null;
    }

    public boolean isFailed() {
        if (checkTestCases()) {
            for (Iterator iterator = testCases.iterator(); iterator.hasNext();) {
                BuildTestCase testCase = (BuildTestCase) iterator.next();
                if (testCase.getResult().equals(BuildTestCaseResult.ERROR) || testCase.getResult()
                        .equals(BuildTestCaseResult.FAILED)) {
                    return true;
                }
            }
        }
        return false;
    }
}
