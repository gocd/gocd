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
package com.thoughtworks.go.apiv3.environments.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.EnvironmentVariableRepresenter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.spark.Routes;

import static com.thoughtworks.go.api.representers.EnvironmentVariableRepresenter.fromJSONArray;

public class EnvironmentRepresenter {
    public static void toJSON(OutputWriter outputWriter, EnvironmentConfig envConfig) {
        String envName = envConfig.name().toString();
        EnvironmentPipelinesConfig envPipelineConfig = envConfig.getPipelines();
        EnvironmentVariablesConfig envVarsConfig = envConfig.getVariables();

        outputWriter
            .addLinks(linksWriter -> linksWriter
                .addLink("self", Routes.Environments.name(envName))
                .addAbsoluteLink("doc", Routes.Environments.DOC)
                .addLink("find", Routes.Environments.find()))
            .add("name", envName)
            .addChildList("pipelines",
                    pipelinesWriter -> envPipelineConfig.forEach(
                        pipelineConfig -> pipelinesWriter.addChild(pipelineWriter -> PipelineRepresenter.toJSON(pipelineWriter, pipelineConfig))))
            .addChildList("environment_variables",
                    envVarListWriter -> envVarsConfig.forEach(
                            envVar -> envVarListWriter.addChild(envVarWriter -> EnvironmentVariableRepresenter.toJSON(envVarWriter, envVar)))
            );
    }

    public static EnvironmentConfig fromJSON(JsonReader jsonReader) {
        String envName = jsonReader.getString("name");
        BasicEnvironmentConfig envConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(envName));

        jsonReader.readArrayIfPresent("pipelines", pipelines -> pipelines.forEach(pipeline -> {
            String pipelineName = pipeline.getAsJsonObject().get("name").getAsString();
            envConfig.addPipeline(new CaseInsensitiveString(pipelineName));
        }));

        fromJSONArray(jsonReader).stream().forEach(envConfig::addEnvironmentVariable);
        return envConfig;
    }
}