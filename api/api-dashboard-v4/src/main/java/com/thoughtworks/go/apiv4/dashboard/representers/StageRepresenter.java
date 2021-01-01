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
package com.thoughtworks.go.apiv4.dashboard.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel;
import com.thoughtworks.go.server.dashboard.GoDashboardPipeline;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.spark.Routes;

public class StageRepresenter {

    public static void toJSON(OutputWriter jsonOutputWriter, GoDashboardPipeline goDashboardPipeline, StageInstanceModel model, Username username, String pipelineName, String pipelineCounter) {
        jsonOutputWriter
                .addLinks(linkWriter -> {
                    linkWriter.addLink("self", Routes.Stage.self(pipelineName, pipelineCounter, model.getName(), model.getCounter()));
                })
                .add("name", model.getName())
                .add("counter", model.getCounter())
                .add("status", model.getState().name())
                .add("allow_only_on_success_of_previous_stage", goDashboardPipeline.isAllowOnlyOnSuccessOfPreviousStage(model.getName()))
                .add("can_operate", goDashboardPipeline.isStageOperator(model.getName(), username.getUsername().toString()))
                .add("approval_type", model.getApprovalTypeDescription())
                .add("approved_by", model.getApprovedBy())
                .add("scheduled_at", model.getScheduledDate());

        if (model.getState().stageResult() == StageResult.Cancelled) {
            jsonOutputWriter.add("cancelled_by", model.getCancelledBy() == null ? "GoCD" : model.getCancelledBy());
        }

        if (model.getPreviousStage() != null) {
            jsonOutputWriter.addChild("previous_stage", childWriter -> {
                StageRepresenter.toJSON(childWriter, goDashboardPipeline, model.getPreviousStage(), username, pipelineName, pipelineCounter);
            });
        }
    }
}
