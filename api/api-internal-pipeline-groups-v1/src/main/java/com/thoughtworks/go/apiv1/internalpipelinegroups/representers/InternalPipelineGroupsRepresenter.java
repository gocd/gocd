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
package com.thoughtworks.go.apiv1.internalpipelinegroups.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.apiv1.internalpipelinegroups.models.PipelineGroupsViewModel;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;

import java.util.Collection;

public class InternalPipelineGroupsRepresenter {
    public static void toJSON(OutputWriter outputWriter, PipelineGroupsViewModel pipelineGroupsViewModel) {
        outputWriter.
                addChildList("groups", groupsWriter -> {
                    pipelineGroupsViewModel.getPipelineGroups().forEach(group -> {
                        groupsWriter.addChild(groupWriter -> {
                            groupWriter.add("name", group.getGroup())
                                    .addChildList("pipelines",
                                            outputListWriter -> group.forEach(pipelineConfig -> {
                                                outputListWriter.addChild(pipelineWriter -> {
                                                    pipelineWriter.add("name", pipelineConfig.name());
                                                    writeOrigin(pipelineWriter, pipelineConfig.getOrigin());
                                                    renderEnvironment(pipelineWriter, pipelineConfig, pipelineGroupsViewModel);
                                                });
                                            }));
                        });
                    });
                });
    }

    private static void renderEnvironment(OutputWriter pipelineWriter, PipelineConfig pipelineConfig, PipelineGroupsViewModel environments) {
        EnvironmentConfig envForPipeline = environments.environmentFor(pipelineConfig.name());
        if (envForPipeline != null) {
            pipelineWriter.add("environment", envForPipeline.name());
        } else {
            pipelineWriter.renderNull("environment");
        }
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
}
