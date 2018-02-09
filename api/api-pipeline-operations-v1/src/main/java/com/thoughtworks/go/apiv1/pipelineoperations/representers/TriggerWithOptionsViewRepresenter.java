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

package com.thoughtworks.go.apiv1.pipelineoperations.representers;

import com.thoughtworks.go.api.representers.JsonWriter;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.spark.RequestContext;
import com.thoughtworks.go.spark.Routes;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TriggerWithOptionsViewRepresenter {
    public static Map<String, Object> toJSON(TriggerOptions triggerOptions, RequestContext requestContext) {
        List<Map<String, Object>> variablesJson = triggerOptions.getVariables().stream().map(env -> {
            JsonWriter writer = new JsonWriter(null)
                    .add("name", env.getName())
                    .add("secure", env.isSecure());

            if (!env.isSecure()) {
                writer.addIfNotNull("value", env.getDisplayValue());
            }

            return writer.getAsMap();
        }).collect(Collectors.toList());

        return new JsonWriter(requestContext)

                .addDocLink(Routes.Pipeline.DOC_TRIGGER_OPTIONS)
                .addLink("self", Routes.Pipeline.triggerOptions(triggerOptions.getPipelineInstanceModel().getName()))
                .addLink("schedule", Routes.Pipeline.schedule(triggerOptions.getPipelineInstanceModel().getName()))

                .add("variables", variablesJson)
                .add("materials", materials(triggerOptions.getPipelineInstanceModel()))
                .getAsMap();
    }

    private static List<Map<String, Object>> materials(PipelineInstanceModel pipelineInstanceModel) {
        return pipelineInstanceModel.getMaterials().stream().map((MaterialConfig material) -> {
            MaterialRevision revision = pipelineInstanceModel.findCurrentMaterialRevisionForUI(material);
            return new JsonWriter(null)
                    .add("type", material.getTypeForDisplay())
                    .add("name", material.getDisplayName())
                    .add("fingerprint", material.getFingerprint())
                    .addIfNotNull("folder", material.getFolder())
                    .add("revision", revision(material, revision))
                    .getAsMap();
        }).collect(Collectors.toList());
    }

    private static Map<String, Object> revision(MaterialConfig material, MaterialRevision revision) {
        if (revision == null) {
            return Collections.emptyMap();
        }

        String maybePipelineLabel = null;

        if (material instanceof DependencyMaterialConfig) {
            maybePipelineLabel = revision.getLatestModification().getPipelineLabel();
        }

        return new JsonWriter(null)
                .addIfNotNull("date", revision.getDateOfLatestModification())
                .addIfNotNull("user", revision.getLatestUser())
                .addIfNotNull("comment", revision.getLatestComment())
                .addIfNotNull("last_run_revision", revision.getLatestRevisionString())
                .getAsMap();
    }
}
