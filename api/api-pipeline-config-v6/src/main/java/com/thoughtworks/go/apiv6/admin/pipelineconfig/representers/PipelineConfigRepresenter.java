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

package com.thoughtworks.go.apiv6.admin.pipelineconfig.representers;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.apiv6.admin.pipelineconfig.representers.materials.MaterialRepresenter;
import com.thoughtworks.go.apiv6.admin.pipelineconfig.representers.trackingtool.TrackingToolRepresenter;
import com.thoughtworks.go.apiv6.shared.representers.EnvironmentVariableRepresenter;
import com.thoughtworks.go.apiv6.shared.representers.configorigin.ConfigRepoOriginRepresenter;
import com.thoughtworks.go.apiv6.shared.representers.configorigin.ConfigXmlOriginRepresenter;
import com.thoughtworks.go.apiv6.shared.representers.stages.StageRepresenter;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.spark.Routes;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PipelineConfigRepresenter {

    public static void toJSON(OutputWriter jsonWriter, PipelineConfig pipelineConfig) {
        jsonWriter.addLinks(linksWriter -> linksWriter
                .addLink("self", Routes.PipelineConfig.name(pipelineConfig.getName().toString()))
                .addAbsoluteLink("doc", Routes.PipelineConfig.DOC)
                .addLink("find", Routes.PipelineConfig.find()));
        if (!pipelineConfig.getAllErrors().isEmpty()) {
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
        writeOrigin(jsonWriter, pipelineConfig.getOrigin());
        jsonWriter.addChildList("parameters", getParams(pipelineConfig.getParams()));
        jsonWriter.addChildList("environment_variables", envVarsWriter -> EnvironmentVariableRepresenter.toJSON(envVarsWriter, pipelineConfig.getVariables()));
        jsonWriter.addChildList("materials", getMaterials(pipelineConfig.materialConfigs()));
        if (pipelineConfig.getStages().isEmpty()) {
            jsonWriter.renderNull("stages");
        } else {
            jsonWriter.addChildList("stages", getStages(pipelineConfig));
        }
        if ((pipelineConfig.getTrackingTool() != null && pipelineConfig.getTrackingTool().isDefined()) || pipelineConfig.getMingleConfig().isDefined()) {
            jsonWriter.addChild("tracking_tool", trackingToolWriter -> TrackingToolRepresenter.toJSON(trackingToolWriter, pipelineConfig));
        } else {
            jsonWriter.renderNull("tracking_tool");
        }
        if (pipelineConfig.getTimer() == null) {
            jsonWriter.renderNull("timer");
        } else {
            jsonWriter.addChild("timer", timerWriter -> TimerRepresenter.toJSON(timerWriter, pipelineConfig.getTimer()));
        }
    }

    private static Consumer<OutputListWriter> getMaterials(MaterialConfigs materialConfigs) {
        return materialsWriter -> {
            materialConfigs.forEach(materialConfig -> {
                materialsWriter.addChild(materialWriter -> MaterialRepresenter.toJSON(materialWriter, materialConfig));
            });
        };
    }

    private static Consumer<OutputListWriter> getParams(ParamsConfig params) {
        return paramsWriter -> {
            params.forEach(param -> {
                paramsWriter.addChild(paramWriter -> ParamRepresenter.toJSON(paramWriter, param));
            });
        };
    }

    private static Consumer<OutputListWriter> getStages(PipelineConfig pipelineConfig) {
        return stagesWriter -> {
            pipelineConfig.getStages().forEach(stage -> {
                stagesWriter.addChild(stageWriter -> StageRepresenter.toJSON(stageWriter, stage));
            });
        };
    }

    private static void writeOrigin(OutputWriter jsonWriter, ConfigOrigin origin) {
        if (origin instanceof FileConfigOrigin) {
            jsonWriter.addChild("origin", originWriter -> ConfigXmlOriginRepresenter.toJSON(originWriter, (FileConfigOrigin) origin));
        } else {
            jsonWriter.addChild("origin", originWriter -> ConfigRepoOriginRepresenter.toJSON(originWriter, (RepoConfigOrigin) origin));
        }
    }

    public static PipelineConfig fromJSON(JsonReader jsonReader, Map<String, Object> options) {
        if (jsonReader == null) {
            return null;
        }
        PipelineConfig pipelineConfig = new PipelineConfig();
        jsonReader.readStringIfPresent("label_template", pipelineConfig::setLabelTemplate);
        jsonReader.readStringIfPresent("lock_behavior", pipelineConfig::setLockBehaviorIfNecessary);
        jsonReader.readStringIfPresent("name", pipelineConfig::setName);
        jsonReader.readCaseInsensitiveStringIfPresent("template", pipelineConfig::setTemplateName);
        pipelineConfig.setOrigin(new FileConfigOrigin());
        setParameters(jsonReader, pipelineConfig);
        setEnvironmentVariables(jsonReader, pipelineConfig);
        setMaterials(jsonReader, pipelineConfig, options);
        setStages(jsonReader, pipelineConfig);
        setTrackingTool(jsonReader, pipelineConfig);
        setTimer(jsonReader, pipelineConfig);
        return pipelineConfig;
    }

    private static void setTimer(JsonReader jsonReader, PipelineConfig pipelineConfig) {
        if (jsonReader.hasJsonObject("timer")) {
            TimerConfig timer = TimerRepresenter.fromJSON(jsonReader.readJsonObject("timer"));
            pipelineConfig.setTimer(timer);
        }
    }

    private static void setTrackingTool(JsonReader jsonReader, PipelineConfig pipelineConfig) {
        if (jsonReader.hasJsonObject("tracking_tool")) {
            Object trackingTool = TrackingToolRepresenter.fromJSON(jsonReader.readJsonObject("tracking_tool"));
            if (trackingTool instanceof MingleConfig) {
                pipelineConfig.setMingleConfig((MingleConfig) trackingTool);
            }
            else if (trackingTool instanceof TrackingTool) {
                pipelineConfig.setTrackingTool((TrackingTool) trackingTool);
            }
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

    private static void setMaterials(JsonReader jsonReader, PipelineConfig pipelineConfig, Map<String, Object> options) {
        MaterialConfigs materialConfigs = new MaterialConfigs();
        jsonReader.readArrayIfPresent("materials", materials -> {
            materials.forEach(material -> {
                materialConfigs.add(MaterialRepresenter.fromJSON(new JsonReader(material.getAsJsonObject()), options));
            });
        });
        pipelineConfig.setMaterialConfigs(materialConfigs);
    }

    private static void setParameters(JsonReader jsonReader, PipelineConfig pipelineConfig) {
        ParamsConfig paramConfigs = new ParamsConfig();
        jsonReader.readArrayIfPresent("parameters", params -> {
            params.forEach(param -> {
                paramConfigs.add(ParamRepresenter.fromJSON(new JsonReader(param.getAsJsonObject())));
            });
        });
        pipelineConfig.setParams(paramConfigs);
    }

    private static void setEnvironmentVariables(JsonReader jsonReader, PipelineConfig pipelineConfig) {
        EnvironmentVariablesConfig environmentVariableConfigs = new EnvironmentVariablesConfig();
        jsonReader.readArrayIfPresent("environment_variables", variables -> {
            variables.forEach(variable -> {
                try {
                    environmentVariableConfigs.add(EnvironmentVariableRepresenter.fromJSON(new JsonReader(variable.getAsJsonObject())));
                } catch (InvalidCipherTextException e) {
                    e.printStackTrace();
                }
            });
        });
        pipelineConfig.setVariables(environmentVariableConfigs);
    }
}
