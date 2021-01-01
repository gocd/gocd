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
package com.thoughtworks.go.apiv1.pipelineoperations.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.spark.Routes;

import java.util.function.Consumer;

public class TriggerWithOptionsViewRepresenter {
    public static void toJSON(OutputWriter writer, TriggerOptions triggerOptions) {
        PipelineInstanceModel pipelineInstanceModel = triggerOptions.getPipelineInstanceModel();
        writer
            .addLinks(outputLinkWriter -> outputLinkWriter.addAbsoluteLink("doc", Routes.Pipeline.DOC_TRIGGER_OPTIONS)
                .addLink("self", Routes.Pipeline.triggerOptions(pipelineInstanceModel.getName()))
                .addLink("schedule", Routes.Pipeline.schedule(pipelineInstanceModel.getName())))
            .addChildList("variables", outputListWriter -> triggerOptions.getVariables().forEach(env ->
                outputListWriter.addChild(envWriter -> {
                    envWriter
                        .add("name", env.getName())
                        .add("secure", env.isSecure());

                    if (!env.isSecure()) {
                        envWriter.add("value", env.getValue());
                    }
                })
            ))
            .addChildList("materials", outputListWriter -> pipelineInstanceModel.getMaterials()
                .forEach(material -> outputListWriter.addChild(material(material, pipelineInstanceModel.findCurrentMaterialRevisionForUI(material)))));
    }

    private static Consumer<OutputWriter> material(MaterialConfig material, MaterialRevision revision) {
        return materialWriter ->
            materialWriter.add("type", material.getTypeForDisplay())
                .add("name", material.getDisplayName())
                .add("fingerprint", material.getFingerprint())
                .addIfNotNull("folder", material.getFolder())
                .addChild("revision", materialRevision(revision));
    }

    private static Consumer<OutputWriter> materialRevision(MaterialRevision revision) {
        return revisionWriter -> {
            if (revision != null) {
                revisionWriter.addIfNotNull("date", revision.getDateOfLatestModification())
                        .addIfNotNull("user", revision.getLatestUser())
                        .addIfNotNull("comment", revision.getLatestComment())
                        .addIfNotNull("last_run_revision", revision.getLatestRevisionString());
            }
        };
    }
}
