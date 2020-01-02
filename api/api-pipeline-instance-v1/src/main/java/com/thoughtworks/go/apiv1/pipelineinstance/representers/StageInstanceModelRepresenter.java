/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv1.pipelineinstance.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel;

public class StageInstanceModelRepresenter {
    public static void toJSON(OutputWriter outputWriter, StageInstanceModel stageInstanceModel) {
        if (stageInstanceModel.getResult() != null) {
            outputWriter.add("result", stageInstanceModel.getResult().toString());
        }
        outputWriter.add("status", stageInstanceModel.getState().toString());
        if (stageInstanceModel.getRerunOfCounter() == null) {
            outputWriter.renderNull("rerun_of_counter");
        } else {
            outputWriter.add("rerun_of_counter", stageInstanceModel.getRerunOfCounter());
        }
        outputWriter
                .add("name", stageInstanceModel.getName())
                .add("counter", stageInstanceModel.getCounter())
                .add("scheduled", stageInstanceModel.isScheduled())
                .add("approval_type", stageInstanceModel.getApprovalType())
                .add("approved_by", stageInstanceModel.getApprovedBy())
                .add("operate_permission", stageInstanceModel.hasOperatePermission())
                .add("can_run", stageInstanceModel.getCanRun())
                .addChildList("jobs", jobsWriter -> stageInstanceModel.getBuildHistory()
                        .forEach(jobHistoryItem -> jobsWriter.addChild(jobWriter -> JobHistoryItemRepresenter.toJSON(jobWriter, jobHistoryItem))));
    }
}
