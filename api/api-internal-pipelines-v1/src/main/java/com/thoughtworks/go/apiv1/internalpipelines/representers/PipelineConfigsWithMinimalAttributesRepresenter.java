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

package com.thoughtworks.go.apiv1.internalpipelines.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.spark.Routes;

import java.util.List;
import java.util.stream.Collectors;

public class PipelineConfigsWithMinimalAttributesRepresenter {
    public static void toJSON(OutputWriter outputWriter, List<PipelineConfigs> listOfPipelineConfigs) {
        List<PipelineConfig> pipelines = listOfPipelineConfigs.stream().flatMap(pipelineConfigs -> pipelineConfigs.getPipelines().stream()).collect(Collectors.toList());

        outputWriter.addLinks(linksWriter -> {
            linksWriter.addLink("self", Routes.Pipeline.INTERNAL_BASE);
        }).addChild("_embedded", childWriter -> {
            childWriter.addChildList("pipelines", pipelineConfigWriter -> {
                pipelines.forEach(pipeline -> {
                    pipelineConfigWriter.addChild(innerChildWriter -> {
                        PipelineConfigWithMinimalAttributesRepresenter.toJSON(innerChildWriter, pipeline);
                    });
                });
            });
        });
    }
}