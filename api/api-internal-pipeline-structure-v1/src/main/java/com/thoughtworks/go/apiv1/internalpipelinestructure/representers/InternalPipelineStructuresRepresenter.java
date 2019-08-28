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

package com.thoughtworks.go.apiv1.internalpipelinestructure.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.config.*;

import java.util.List;

public class InternalPipelineStructuresRepresenter {
    public static void toJSON(OutputWriter outputWriter, List<PipelineConfigs> groups, List<PipelineTemplateConfig> templatesList) {
        outputWriter.
                addChildList("groups", groupsWriter -> {
                    groups.forEach(group -> {
                        groupsWriter.addChild(groupWriter -> {
                            groupWriter.add("name", group.getGroup())
                                    .addChildList("pipelines",
                                            outputListWriter -> group.forEach(pipelineConfig -> {
                                                outputListWriter.addChild(pipelineWriter -> {
                                                    pipelineWriter
                                                            .add("name", pipelineConfig.name())
                                                            .addIfNotNull("template_name", pipelineConfig.getTemplateName());
                                                    renderStages(pipelineConfig, pipelineWriter);
                                                });
                                            }));
                        });
                    });
                })
                .addChildList("templates", templatesWriter -> {
                    templatesList.forEach(template -> {
                        templatesWriter.addChild(templateWriter -> {
                            templateWriter.add("name", template.name());
                            renderStages(template, templateWriter);
                        });
                    });
                });
    }

    private static void renderStages(List<StageConfig> pipelineConfig, OutputWriter pipelineWriter) {
        pipelineWriter
                .addChildList("stages", stagesWriter -> {
                    pipelineConfig.forEach(stage -> {
                        stagesWriter.addChild(stageWriter -> {
                            stageWriter.add("name", stage.name())
                                    .addChildList("jobs", stage.getJobNames());
                        });
                    });
                });
    }
}
