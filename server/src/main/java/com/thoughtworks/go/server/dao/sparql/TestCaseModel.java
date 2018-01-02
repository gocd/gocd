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

package com.thoughtworks.go.server.dao.sparql;

import com.thoughtworks.go.domain.JobIdentifier;

/**
* @understands test case run in a job
*/
public class TestCaseModel {
    private final JobIdentifier jobIdentifier;
    private final String testSuiteName;
    private final String testName;
    private final Boolean isError;

    public TestCaseModel(JobIdentifier jobIdentifier, String testSuiteName, String testName, Boolean error) {
        isError = error;
        this.testName = testName;
        this.jobIdentifier = jobIdentifier;
        this.testSuiteName = testSuiteName;
    }

    public JobIdentifier getJobIdentifier() {
        return jobIdentifier;
    }

    public String getTestSuiteName() {
        return testSuiteName;
    }

    public String getTestName() {
        return testName;
    }

    public Boolean isError() {
        return isError;
    }
}
