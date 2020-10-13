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
package com.thoughtworks.go.apiv12.admin.shared.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.EnvironmentVariableRepresenter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.apiv12.admin.shared.representers.configorigin.ConfigRepoOriginRepresenter;
import com.thoughtworks.go.apiv12.admin.shared.representers.configorigin.ConfigXmlOriginRepresenter;
import com.thoughtworks.go.apiv12.admin.shared.representers.materials.MaterialsRepresenter;
import com.thoughtworks.go.apiv12.admin.shared.representers.stages.ConfigHelperOptions;
import com.thoughtworks.go.apiv12.admin.shared.representers.stages.StageRepresenter;
import com.thoughtworks.go.apiv12.admin.shared.representers.trackingtool.TrackingToolRepresenter;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.TimerConfig;
import com.thoughtworks.go.config.TrackingTool;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.spark.Routes;

import java.util.HashMap;

public class PipelineConfigRepresenter {

    public static void toJSON(OutputWriter jsonWriter, PipelineConfig pipelineConfig, String groupName) {
        jsonWriter.addLinks(linksWriter -> linksWriter
                .addLink("self", Routes.PipelineConfig.name(pipelineConfig.getName().toString()))
                .addAbsoluteLink("doc", Routes.PipelineConfig.DOC)
                .addLink("find", Routes.PipelineConfig.find()));
        // This is needed for the case when there are no materials defined. Refer to pipeline_config_representer.rb#152
        pipelineConfig.errors().addAll(pipelineConfig.materialConfigs().errors());
        if (!pipelineConfig.errors().isEmpty()) {
            jsonWriter.addChild("errors", errorWriter -> {
                HashMap<String, String> errorMapping = new HashMap<>();
                errorMapping.put("labelTemplate", "label_template");
                errorMapping.put("params", "parameters");
                errorMapping.put("variables", "environment_variables");
                errorMapping.put("trackingTool", "tracking_tool");

                new ErrorGetter(errorMapping).toJSON(errorWriter, pipelineConfig);
            });
        }

        jsonWriter.add("label_template", pipelineConfig.getLabelTemplate());
        jsonWriter.add("lock_behavior", pipelineConfig.getLockBehavior());
        jsonWriter.add("name", pipelineConfig.name());
        jsonWriter.add("template", pipelineConfig.getTemplateName());
        jsonWriter.add("group", groupName);
        writeOrigin(jsonWriter, pipelineConfig.getOrigin());
        jsonWriter.addChildList("parameters", paramsWriter -> ParamRepresenter.toJSONArray(paramsWriter, pipelineConfig.getParams()));
        jsonWriter.addChildList("environment_variables", envVarsWriter -> EnvironmentVariableRepresenter.toJSON(envVarsWriter, pipelineConfig.getVariables()));
        jsonWriter.addChildList("materials", materialsWriter -> MaterialsRepresenter.toJSONArray(materialsWriter, pipelineConfig.materialConfigs()));
        writeStages(jsonWriter, pipelineConfig);
        writeTrackingTool(jsonWriter, pipelineConfig);
        writeTimer(jsonWriter, pipelineConfig.getTimer());

    }

    private static void writeTrackingTool(OutputWriter jsonWriter, PipelineConfig pipelineConfig) {
        if (pipelineConfig.getTrackingTool() != null) {
            jsonWriter.addChild("tracking_tool", trackingToolWriter -> TrackingToolRepresenter.toJSON(trackingToolWriter, pipelineConfig));
        } else {
            jsonWriter.renderNull("tracking_tool");
        }
    }

    private static void writeTimer(OutputWriter jsonWriter, TimerConfig timer) {
        if (timer == null) {
            jsonWriter.renderNull("timer");
        } else {
            jsonWriter.addChild("timer", timerWriter -> TimerRepresenter.toJSON(timerWriter, timer));
        }
    }

    private static void writeStages(OutputWriter jsonWriter, PipelineConfig pipelineConfig) {
        if (pipelineConfig.getStages().isEmpty()) {
            jsonWriter.renderNull("stages");
        } else {
            jsonWriter.addChildList("stages", stagesWriter -> StageRepresenter.toJSONArray(stagesWriter, pipelineConfig));
        }
    }

    private static void writeOrigin(OutputWriter jsonWriter, ConfigOrigin origin) {
        if (origin instanceof FileConfigOrigin) {
            jsonWriter.addChild("origin", originWriter -> ConfigXmlOriginRepresenter.toJSON(originWriter, (FileConfigOrigin) origin));
        } else {
            jsonWriter.addChild("origin", originWriter -> ConfigRepoOriginRepresenter.toJSON(originWriter, (RepoConfigOrigin) origin));
        }
    }

    public static PipelineConfig fromJSON(JsonReader jsonReader, ConfigHelperOptions options) {
        PipelineConfig pipelineConfig = new PipelineConfig();
        jsonReader.readStringIfPresent("label_template", pipelineConfig::setLabelTemplate);
        jsonReader.readStringIfPresent("lock_behavior", pipelineConfig::setLockBehaviorIfNecessary);
        jsonReader.readStringIfPresent("name", pipelineConfig::setName);
        jsonReader.readCaseInsensitiveStringIfPresent("template", pipelineConfig::setTemplateName);
        pipelineConfig.setOrigin(new FileConfigOrigin());
        pipelineConfig.setParams(ParamRepresenter.fromJSONArray(jsonReader));
        pipelineConfig.setVariables(EnvironmentVariableRepresenter.fromJSONArray(jsonReader));
        pipelineConfig.setMaterialConfigs(MaterialsRepresenter.fromJSONArray(jsonReader, options));
        setStages(jsonReader, pipelineConfig);
        setTrackingTool(jsonReader, pipelineConfig);
        jsonReader.optJsonObject("timer").ifPresent(timerJsonReader -> {
            pipelineConfig.setTimer(TimerRepresenter.fromJSON(timerJsonReader));
        });
        return pipelineConfig;
    }

    private static void setTrackingTool(JsonReader jsonReader, PipelineConfig pipelineConfig) {
        if (jsonReader.hasJsonObject("tracking_tool")) {
            Object trackingTool = TrackingToolRepresenter.fromJSON(jsonReader.readJsonObject("tracking_tool"));
            pipelineConfig.setTrackingTool((TrackingTool) trackingTool);
        }
    }

    private static void setStages(JsonReader jsonReader, PipelineConfig pipelineConfig) {
        pipelineConfig.getStages().clear();
        jsonReader.readArrayIfPresent("stages", stages -> {
            stages.forEach(stage -> {
                pipelineConfig.addStageWithoutValidityAssertion(StageRepresenter.fromJSON(new JsonReader(stage.getAsJsonObject())));
            });
        });
    }
}
