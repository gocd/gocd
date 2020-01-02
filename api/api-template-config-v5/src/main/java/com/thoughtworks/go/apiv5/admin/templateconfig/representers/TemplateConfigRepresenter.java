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
package com.thoughtworks.go.apiv5.admin.templateconfig.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.apiv10.admin.shared.representers.stages.StageRepresenter;
import com.thoughtworks.go.config.PipelineTemplateConfig;
import com.thoughtworks.go.spark.Routes;

import java.util.HashMap;

public class TemplateConfigRepresenter {

    public static void toJSON(OutputWriter jsonWriter, PipelineTemplateConfig pipelineTemplateConfig) {
        jsonWriter.addLinks(linksWriter -> linksWriter
                .addLink("self", Routes.PipelineTemplateConfig.name(pipelineTemplateConfig.name().toString()))
                .addAbsoluteLink("doc", Routes.PipelineTemplateConfig.DOC)
                .addLink("find", Routes.PipelineTemplateConfig.find()));
        if (!pipelineTemplateConfig.errors().isEmpty()) {
            jsonWriter.addChild("errors", errorWriter -> {
                HashMap<String, String> errorMapping = new HashMap<>();
                new ErrorGetter(new HashMap<>()).toJSON(errorWriter, pipelineTemplateConfig);
            });
        }

        jsonWriter.add("name", pipelineTemplateConfig.name());
        writeStages(jsonWriter, pipelineTemplateConfig);

    }

    private static void writeStages(OutputWriter jsonWriter, PipelineTemplateConfig pipelineTemplateConfig) {
        if (pipelineTemplateConfig.getStages().isEmpty()) {
            jsonWriter.renderNull("stages");
        } else {
            jsonWriter.addChildList("stages", stagesWriter -> StageRepresenter.toJSONArray(stagesWriter, pipelineTemplateConfig));
        }
    }

    public static PipelineTemplateConfig fromJSON(JsonReader jsonReader) {
        PipelineTemplateConfig pipelineTemplateConfig = new PipelineTemplateConfig();
        jsonReader.readStringIfPresent("name", pipelineTemplateConfig::setName);
        setStages(jsonReader, pipelineTemplateConfig);
        return pipelineTemplateConfig;
    }

    private static void setStages(JsonReader jsonReader, PipelineTemplateConfig pipelineTemplateConfig) {
        pipelineTemplateConfig.getStages().clear();
        jsonReader.readArrayIfPresent("stages", stages -> {
            stages.forEach(stage -> {
                pipelineTemplateConfig.addStageWithoutValidityAssertion(StageRepresenter.fromJSON(new JsonReader(stage.getAsJsonObject())));
            });
        });
    }
}
