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
import com.thoughtworks.go.util.GoConstants;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class StageInstanceModelTest {

    @Test
    public void shouldUnderstandPreviousStageState() {
        StageInstanceModel item = new StageInstanceModel("foo", "10", JobHistory.withJob("unit", JobState.Assigned, JobResult.Unknown, new Date()));
        StageInstanceModel previous = new StageInstanceModel("foo", "1", JobHistory.withJob("unit", JobState.Completed, JobResult.Passed, new Date()));
        assertThat(item.hasPreviousStage(), is(false));
        item.setPreviousStage(previous);
        assertThat(item.hasPreviousStage(), is(true));
        assertThat(item.getPreviousStage(), is(previous));
    }

    @Test
    public void shouldBeAutoApproved() throws Exception {
        StageInstanceModel stageHistoryItem = new StageInstanceModel();
        stageHistoryItem.setApprovalType(GoConstants.APPROVAL_SUCCESS);
        assertThat(stageHistoryItem.isAutoApproved(), is(true));
    }

    @Test
    public void shouldBeManualApproved() throws Exception {
        StageInstanceModel stageHistoryItem = new StageInstanceModel();
        stageHistoryItem.setApprovalType(GoConstants.APPROVAL_MANUAL);
        assertThat(stageHistoryItem.isAutoApproved(), is(false));
    }

    @Test
    public void shouldReturnNullIfJobHistoryIsBlank() throws Exception {
        StageInstanceModel stageHistoryItem = new StageInstanceModel();
        stageHistoryItem.setBuildHistory(new JobHistory());
        assertThat(stageHistoryItem.getScheduledDate(), is(nullValue()));
    }

    @Test
    public void shouldReturnDateIfJobHistoryIsNotBlank() throws Exception {
        StageInstanceModel stageHistoryItem = new StageInstanceModel();
        JobHistory jobHistory = new JobHistory();
        Date date = new Date(1367472329111L);
        jobHistory.addJob("jobName", JobState.Building, JobResult.Passed, date);
        stageHistoryItem.setBuildHistory(jobHistory);
        assertThat(stageHistoryItem.getScheduledDate(), is(date));
    }
}
