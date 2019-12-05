/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv2.stageoperations.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel;

public class StageInstanceRepresenter {
    public static void toJSON(OutputWriter jsonWriter, StageInstanceModel stageInstanceModel) {
        jsonWriter.add("id", stageInstanceModel.getId());
        jsonWriter.add("name", stageInstanceModel.getName());
        jsonWriter.add("counter", stageInstanceModel.getCounter());
        jsonWriter.add("approval_type", stageInstanceModel.getApprovalType());
        jsonWriter.add("approved_by", stageInstanceModel.getApprovedBy());
        if (stageInstanceModel.getResult() != null) {
            jsonWriter.add("result", stageInstanceModel.getResult().toString());
            if (stageInstanceModel.getResult() == StageResult.Cancelled) {
                jsonWriter.add("cancelled_by", stageInstanceModel.getCancelledBy() == null? "GoCD" : stageInstanceModel.getCancelledBy());
            }
        }
        if (stageInstanceModel.getRerunOfCounter() == null) {
            jsonWriter.add("rerun_of_counter", (String) null);
        }
        else {
            jsonWriter.add("rerun_of_counter", stageInstanceModel.getRerunOfCounter());
        }

        if (stageInstanceModel.getIdentifier() != null) {
            jsonWriter.add("pipeline_name", stageInstanceModel.getPipelineName());
            jsonWriter.add("pipeline_counter", stageInstanceModel.getPipelineCounter());
        }
        jsonWriter.addChildList("jobs", jobsWriter -> stageInstanceModel.getBuildHistory().forEach(
                jobHistoryItem -> jobsWriter.addChild(
                        jobInstanceWriter -> JobHistoryItemRepresenter.toJSON(jobInstanceWriter, jobHistoryItem))));
    }
}
