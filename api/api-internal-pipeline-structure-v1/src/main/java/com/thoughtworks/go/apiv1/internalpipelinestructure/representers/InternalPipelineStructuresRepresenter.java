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
package com.thoughtworks.go.apiv1.internalpipelinestructure.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.config.TemplatesConfig;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.PipelineGroups;

import java.util.Collections;
import java.util.List;

public class InternalPipelineStructuresRepresenter {
    public static void toJSON(OutputWriter outputWriter, PipelineGroups groups, TemplatesConfig templatesList) {
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
                                                    writeOrigin(pipelineWriter, pipelineConfig.getOrigin());
                                                    if (pipelineConfig.hasTemplate()) {
                                                        renderStages(templatesList.templateByName(pipelineConfig.getTemplateName()), pipelineWriter);
                                                    } else {
                                                        renderStages(pipelineConfig, pipelineWriter);
                                                    }
                                                });
                                            }));
                        });
                    });
                })
                .addChildList("templates", templatesWriter -> {
                    templatesList.forEach(template -> {
                        templatesWriter.addChild(templateWriter -> {
                            templateWriter
                                    .add("name", template.name())
                                    .addChildList("parameters", template.referredParams().getNames());
                            renderStages(template, templateWriter);
                        });
                    });
                });
    }

    private static void writeOrigin(OutputWriter jsonWriter, ConfigOrigin origin) {
        if (origin instanceof FileConfigOrigin) {
            jsonWriter.addChild("origin", originWriter -> originWriter.add("type", "gocd"));
        } else if (origin instanceof RepoConfigOrigin) {
            jsonWriter.addChild("origin", originWriter -> {
                originWriter.add("type", "config_repo");
                originWriter.add("id", ((RepoConfigOrigin) origin).getConfigRepo().getId());
            });
        }
    }

    private static void renderStages(List<StageConfig> pipelineConfig, OutputWriter pipelineWriter) {
        if (pipelineConfig == null) {
            // perhaps current user does not have access to view stages
            pipelineWriter.addChildList("stages", Collections.emptyList());
            return;
        }
        pipelineWriter
                .addChildList("stages", stagesWriter -> {
                    pipelineConfig.forEach(stage -> {
                        stagesWriter.addChild(stageWriter -> {
                            stageWriter.add("name", stage.name())
                                    .addChildList("jobs", (jobsWriter) -> {
                                        stage.getJobs().forEach(job -> {
                                            jobsWriter.addChild(jobWriter -> {
                                                jobWriter.add("name", job.name())
                                                        .add("is_elastic", job.usesElasticAgent());
                                            });

                                        });
                                    });
                        });
                    });
                });
    }
}
