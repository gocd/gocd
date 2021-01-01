/*
 * Copyright 2021 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.presentation.pipelinehistory;

import java.util.Date;

import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import org.junit.Test;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;

public class JobHistoryItemTest {

    @Test public void shouldBeUnsuccessfullyCompletedWhenFailedOrCancelled() {
        assertThat(new JobHistoryItem("", JobState.Completed, JobResult.Failed, new Date()).hasUnsuccessfullyCompleted(),is(true));
        assertThat(new JobHistoryItem("", JobState.Completed, JobResult.Cancelled, new Date()).hasUnsuccessfullyCompleted(),is(true));
    }

    @Test public void shouldPassedWhenJobCompletesSuccessfully() {
        assertThat(new JobHistoryItem("", JobState.Completed, JobResult.Passed, new Date()).hasPassed(),is(true));
    }

    @Test public void shouldBeRunningBasedOnJobState() {
        assertThat(new JobHistoryItem("", JobState.Assigned, JobResult.Unknown, new Date()).isRunning(),is(true));
        assertThat(new JobHistoryItem("", JobState.Building, JobResult.Unknown, new Date()).isRunning(),is(true));
        assertThat(new JobHistoryItem("", JobState.Completing, JobResult.Unknown, new Date()).isRunning(),is(true));
        assertThat(new JobHistoryItem("", JobState.Preparing, JobResult.Unknown, new Date()).isRunning(),is(true));
        assertThat(new JobHistoryItem("", JobState.Scheduled, JobResult.Unknown, new Date()).isRunning(),is(true));
    }
}
