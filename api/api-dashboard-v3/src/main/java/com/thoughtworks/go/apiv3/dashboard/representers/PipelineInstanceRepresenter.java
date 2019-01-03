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

package com.thoughtworks.go.apiv3.dashboard.representers;

import com.thoughtworks.go.api.base.OutputLinkWriter;
import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.spark.Routes;

import java.util.function.Consumer;

public class PipelineInstanceRepresenter {

    public static void toJSON(OutputWriter jsonOutputWriter, PipelineInstanceModel model) {
        jsonOutputWriter
            .addLinks(addLinks(model))
            .add("label", model.getLabel())
            .add("counter", model.getCounter())
            .add("triggered_by", model.getApprovedByForDisplay())
            .add("scheduled_at", model.getScheduledDate())
            .addChild("_embedded", childWriter -> {
                childWriter.addChildList("stages", getStages(model));
            });
    }

    private static Consumer<OutputLinkWriter> addLinks(PipelineInstanceModel model) {
        return linkWriter -> linkWriter.addLink("self", Routes.Pipeline.instance(model.getName(), model.getCounter()));
    }

    private static Consumer<OutputListWriter> getStages(PipelineInstanceModel model) {
        return writer -> {
            model.getStageHistory().forEach(stage -> {
                writer.addChild(childWriter -> {
                    StageRepresenter.toJSON(childWriter, stage, model.getName(), String.valueOf(model.getCounter()));
                });
            });
        };
    }
}
