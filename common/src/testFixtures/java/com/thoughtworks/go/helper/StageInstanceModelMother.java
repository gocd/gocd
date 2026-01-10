/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.presentation.pipelinehistory.JobHistory;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel;
import com.thoughtworks.go.util.GoConstants;

import static org.assertj.core.api.Assertions.assertThat;

public class StageInstanceModelMother {

    public static StageInstanceModel custom(String stageName, boolean isAutoApproved) {
        StageInstanceModel stageHistoryItem = new StageInstanceModel();
        stageHistoryItem.setName(stageName);
        if (isAutoApproved) {
            stageHistoryItem.setApprovalType(GoConstants.APPROVAL_SUCCESS);
        } else {
            stageHistoryItem.setApprovalType(GoConstants.APPROVAL_MANUAL);
        }
        assertThat(stageHistoryItem.isAutoApproved()).isEqualTo(isAutoApproved);
        return stageHistoryItem;
    }

    public static StageInstanceModel fromStage(Stage stage) {
        StageInstanceModel stageInstanceModel = new StageInstanceModel(stage.getName(), String.valueOf(stage.getCounter()), stage.getResult(), stage.getIdentifier());
        stageInstanceModel.setApprovalType(stage.getApprovalType());
        stageInstanceModel.setApprovedBy(stage.getApprovedBy());
        stageInstanceModel.setRerunOfCounter(stage.getRerunOfCounter());
        JobHistory jobHistory = new JobHistory();
        for (JobInstance jobInstance : stage.getJobInstances()) {
            jobHistory.addJob(jobInstance.getName(), jobInstance.getState(), jobInstance.getResult(), jobInstance.getScheduledDate());
        }
        stageInstanceModel.setBuildHistory(jobHistory);
        return stageInstanceModel;
    }
}