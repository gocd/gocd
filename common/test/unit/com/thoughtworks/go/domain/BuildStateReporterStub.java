/*************************** GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ************************GO-LICENSE-END***********************************/
package com.thoughtworks.go.domain;

import com.thoughtworks.go.buildsession.BuildStateReporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BuildStateReporterStub implements BuildStateReporter {

    private List<JobState> reportedBuildStatus;
    private List<JobResult> reportedBuildResult;

    public BuildStateReporterStub() {
        reportedBuildStatus = Collections.synchronizedList(new ArrayList<JobState>());
        reportedBuildResult = Collections.synchronizedList(new ArrayList<JobResult>());
    }

    @Override
    public void reportBuildStatus(String buildId, JobState buildState) {
        reportedBuildStatus.add(buildState);
    }

    @Override
    public void reportCompleted(String buildId, JobResult buildResult) {
        reportedBuildStatus.add(JobState.Completed);
        reportedBuildResult.add(buildResult);
    }

    @Override
    public void reportCompleting(String buildId, JobResult buildResult) {
        reportedBuildResult.add(buildResult);
    }

    public List<JobState> status() {
        return reportedBuildStatus;
    }

    public List<JobResult> results() {
        return reportedBuildResult;
    }

    public JobResult singleResult() {
        if(results().size() != 1) {
            throw new RuntimeException("Expect single result but was: " + results());
        }
        return results().get(0);
    }
}
