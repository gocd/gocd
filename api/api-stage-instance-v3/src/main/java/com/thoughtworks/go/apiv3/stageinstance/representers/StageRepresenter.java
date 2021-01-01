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
package com.thoughtworks.go.apiv3.stageinstance.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageResult;

public class StageRepresenter {

    public static void toJSON(OutputWriter jsonWriter, Stage stage) {
        jsonWriter.add("name", stage.getName());
        jsonWriter.add("counter", stage.getCounter());
        jsonWriter.add("approval_type", stage.getApprovalType());
        jsonWriter.add("approved_by", stage.getApprovedBy());
        jsonWriter.add("scheduled_at", stage.getCreatedTime().getTime());
        jsonWriter.add("last_transitioned_time", stage.getLastTransitionedTime().getTime());
        if (stage.getResult() != null) {
            jsonWriter.add("result", stage.getResult().toString());
            if (stage.getResult() == StageResult.Cancelled) {
                jsonWriter.add("cancelled_by", stage.getCancelledBy() == null? "GoCD" : stage.getCancelledBy());
            }
        }
        if (stage.getRerunOfCounter() == null) {
            jsonWriter.add("rerun_of_counter", (String) null);
        }
        else {
            jsonWriter.add("rerun_of_counter", stage.getRerunOfCounter());
        }
        jsonWriter.add("fetch_materials", stage.shouldFetchMaterials());
        jsonWriter.add("clean_working_directory", stage.shouldCleanWorkingDir());
        jsonWriter.add("artifacts_deleted", stage.isArtifactsDeleted());
        if (stage.getIdentifier() != null) {
         jsonWriter.add("pipeline_name", stage.getIdentifier().getPipelineName());
         jsonWriter.add("pipeline_counter", stage.getIdentifier().getPipelineCounter());
        }
        jsonWriter.addChildList("jobs", jobsWriter -> stage.getJobInstances().forEach(
                jobInstance -> jobsWriter.addChild(
                        jobInstanceWriter -> JobInstanceRepresenter.toJSON(jobInstanceWriter, jobInstance))));

    }
}
