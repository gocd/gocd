/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv2.dashboard.representers;

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
            .addChild("build_cause", childWriter -> {
                BuildCauseRepresenter.toJSON(childWriter, model.getBuildCause());
            })
            .addChild("_embedded", childWriter -> {
                childWriter.addChildList("stages", getStages(model));
            });
    }

    private static Consumer<OutputLinkWriter> addLinks(PipelineInstanceModel model) {
        return linkWriter -> {
            linkWriter.addLink("self", Routes.Pipeline.instance(model.getName(), model.getCounter()))
                .addLink("compare_url", Routes.PipelineInstance.compare(model.getName(), model.getCounter() - 1, model.getCounter()))
                .addLink("history_url", Routes.Pipeline.history(model.getName()))
                .addLink("vsm_url", Routes.PipelineInstance.vsm(model.getName(), model.getCounter()));
        };
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
