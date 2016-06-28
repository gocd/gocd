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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Comparator;

import com.thoughtworks.go.domain.JobIdentifier;

/**
 * @understands information about a test run
 */
public class TestInformation {
    private final String testName;
    private final TestStatus testStatus;
    private final Set<JobIdentifier> jobIdentifiers;

    public TestInformation(String testName, TestStatus testStatus) {
        this.testName = testName;
        this.testStatus = testStatus;
        jobIdentifiers = new HashSet<>();
    }

    public TestStatus getStatus() {
        return testStatus;
    }

    public String getName() {
        return testName;
    }

    public List<String> getJobNames() {
        List<String> jobNames = new ArrayList<>();
        for (JobIdentifier jobIdentifier : jobIdentifiers) {
            jobNames.add(jobIdentifier.getBuildName());
        }
        return sort(jobNames);
    }

    private List<String> sort(List<String> jobNames) {
        Collections.sort(jobNames);
        return jobNames;
    }

    public void addJob(JobIdentifier jobIdentifier) {
        jobIdentifiers.add(jobIdentifier);
    }


    public List<JobIdentifier> getJobs() {
        List<JobIdentifier> ids = new ArrayList<>(jobIdentifiers);
        sortJobIdentifiers(ids);
        return ids;
    }

    private void sortJobIdentifiers(List<JobIdentifier> ids) {
        Collections.sort(ids, new Comparator<JobIdentifier>() {
            public int compare(JobIdentifier first, JobIdentifier second) {
                return first.getBuildName().compareTo(second.getBuildName());
            }
        });
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TestInformation that = (TestInformation) o;

        return equals(that);
    }

    private boolean equals(TestInformation that) {

        if (testName != null ? !testName.equals(that.testName) : that.testName != null) {
            return false;
        }
        if (testStatus != that.testStatus) {
            return false;
        }

        if(this.jobIdentifiers.size()!=that.jobIdentifiers.size()) return false;
        for (JobIdentifier jobIdentifier : jobIdentifiers) {
            if(!that.containsJobWithName(jobIdentifier.getBuildName())) return false;
        }

        return true;
    }

    private boolean containsJobWithName(String buildName) {
        for (JobIdentifier jobIdentifier : jobIdentifiers) {
            if(jobIdentifier.getBuildName().equals(buildName)) return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = testName != null ? testName.hashCode() : 0;
        result = 31 * result + (testStatus != null ? testStatus.hashCode() : 0);
        result = 31 * result + (jobIdentifiers != null ? jobIdentifiers.hashCode() : 0);
        return result;
    }
}
