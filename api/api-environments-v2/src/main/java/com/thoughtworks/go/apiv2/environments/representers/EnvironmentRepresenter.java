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

package com.thoughtworks.go.apiv2.environments.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.spark.Routes;

public class EnvironmentRepresenter {
    public static void toJSON(OutputWriter outputWriter, EnvironmentConfig environmentViewModel) {
        String envName = environmentViewModel.name().toString();
        EnvironmentPipelinesConfig pipelines = environmentViewModel.getPipelines();
        EnvironmentAgentsConfig agentsViewModel = environmentViewModel.getAgents();
        EnvironmentVariablesConfig environmentVariableConfigs = environmentViewModel.getVariables();

        outputWriter
                .addLinks(linksWriter -> linksWriter
                        .addLink("self", Routes.Environments.name(envName))
                        .addAbsoluteLink("doc", Routes.Environments.DOC)
                        .addLink("find", Routes.Environments.find()))

                .add("name", envName)
                .addChildList("agents", agentListWriter ->
                        agentsViewModel.forEach(agent ->
                                agentListWriter.addChild(propertyWriter -> AgentRepresenter.toJSON(propertyWriter, agent))
                        )
                )
                .addChildList("pipelines", listWriter ->
                        pipelines.forEach(pipelineConfig ->
                                listWriter.addChild(propertyWriter -> PipelineRepresenter.toJSON(propertyWriter, pipelineConfig))))
                .addChildList("environment_variables", environmentListWriter ->
                        environmentVariableConfigs.forEach(environmentVariable ->
                                environmentListWriter.addChild(propertyWriter -> EnvironmentVariableRepresenter.toJSON(propertyWriter, environmentVariable))
                        )
                );
    }

    public static EnvironmentConfig fromJSON(JsonReader jsonReader) {
        BasicEnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(jsonReader.getString("name")));
        jsonReader.readArrayIfPresent("agents", array -> array.forEach(element -> environmentConfig.addAgent(element.getAsJsonObject().get("uuid").getAsString())));

        jsonReader.readArrayIfPresent("pipelines", array -> array.forEach(element -> environmentConfig.addPipeline(new CaseInsensitiveString(element.getAsJsonObject().get("name").getAsString()))));


        jsonReader.readArrayIfPresent("environment_variables",
                array -> array.forEach(envVar -> {
                        String name = envVar.getAsJsonObject().get("name").getAsString();
                        String value = envVar.getAsJsonObject().get("value").getAsString();
                        boolean secure = envVar.getAsJsonObject().get("secure").getAsBoolean();
                        environmentConfig.addEnvironmentVariable(new EnvironmentVariableConfig(new GoCipher(), name, value, secure));
                    }
                )
        );

        return environmentConfig;
    }
}