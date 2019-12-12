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

package com.thoughtworks.go.apiv1.internalenvironments.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.merge.MergeConfigOrigin;
import com.thoughtworks.go.config.remote.ConfigOrigin;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class MergedEnvironmentRepresenter {
    public static void toJSON(OutputWriter outputWriter, EnvironmentConfig environmentConfig, Function<String, Boolean> canUserAdministerEnvironment) {
        outputWriter.add("name", environmentConfig.name());
        outputWriter.add("can_administer", canUserAdministerEnvironment.apply(environmentConfig.name().toString()));
        addOrigin(outputWriter, environmentConfig.getOrigin());
        outputWriter.addChildList("pipelines", outputListWriter -> {
            environmentConfig.getPipelines().forEach(pipeline -> outputListWriter.addChild(writer -> EnvironmentPipelineRepresenter.toJSON(writer, pipeline, environmentConfig)));
        });
        outputWriter.addChildList("agents", outputListWriter -> {
            environmentConfig.getAgents().forEach(agent -> outputListWriter.addChild(writer -> EnvironmentAgentRepresenter.toJSON(writer, agent, environmentConfig)));
        });
        outputWriter.addChildList("environment_variables", outputListWriter -> {
            environmentConfig.getVariables().forEach(envVar -> outputListWriter.addChild(writer -> EnvironmentEnvironmentVariableRepresenter.toJSON(writer, envVar, environmentConfig)));
        });
    }

    private static void addOrigin(OutputWriter outputWriter, ConfigOrigin origin) {
        List<ConfigOrigin> origins = (origin instanceof MergeConfigOrigin) ? ((MergeConfigOrigin) origin) : Collections.singletonList(origin);

        outputWriter.addChildList("origins", childListWriter -> {
            origins.forEach(subOrigin -> childListWriter.addChild(childWriter -> EntityConfigOriginRepresenter.toJSON(childWriter, subOrigin)));
        });
    }
}
