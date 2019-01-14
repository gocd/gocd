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
import com.thoughtworks.go.config.EnvironmentAgentsConfig;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.EnvironmentPipelinesConfig;
import com.thoughtworks.go.config.EnvironmentVariablesConfig;
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
                                environmentListWriter.addChild(propertyWriter -> EnvrironmentVariableRepresenter.toJSON(propertyWriter, environmentVariable))
                        )
                );
    }
}