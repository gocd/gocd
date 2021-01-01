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
package com.thoughtworks.go.apiv1.buildcause.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.spark.Routes;

public class PipelineDependencyModificationRepresenter {

    public static void toJSON(OutputWriter jsonOutputWriter, Modification model, DependencyMaterialRevision latestRevision) {
        jsonOutputWriter
            .addLinks((linksWriter) -> {
                linksWriter
                    .addLink("vsm", Routes.PipelineInstance.vsm(
                        latestRevision.getPipelineName(),
                        latestRevision.getPipelineCounter()))
                    .addLink("stage_details_url", Routes.Stage.stageDetailTab(
                        latestRevision.getPipelineName(),
                        latestRevision.getPipelineCounter(),
                        latestRevision.getStageName(),
                        latestRevision.getStageCounter()));
            })
            .addIfNotNull("revision", model.getRevision())
            .addIfNotNull("modified_time", model.getModifiedTime())
            .addIfNotNull("pipeline_label", model.getPipelineLabel());
    }
}
