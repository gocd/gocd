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

package com.thoughtworks.go.apiv1.compare.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;

public class PipelineInstanceModelRepresenter {
    public static void toJSON(OutputWriter outputWriter, PipelineInstanceModel pipelineInstance) {
        outputWriter
                .add("name", pipelineInstance.getName())
                .add("counter", pipelineInstance.getCounter())
                .add("label", pipelineInstance.getLabel())
                .add("natural_order", pipelineInstance.getNaturalOrder())
                .add("comment", pipelineInstance.getComment())
                .addInMillisIfNotNull("scheduled_date", pipelineInstance.getScheduledDate())
                .addChild("build_cause", causeWriter -> BuildCauseRepresenter.toJSON(causeWriter, pipelineInstance.getBuildCause()))
                .addChildList("stages", stagesWriter -> pipelineInstance.getStageHistory()
                        .forEach(stageInstanceModel -> stagesWriter.addChild(stageWriter -> StageInstanceModelRepresenter.toJSON(stageWriter, stageInstanceModel))));
    }
}
