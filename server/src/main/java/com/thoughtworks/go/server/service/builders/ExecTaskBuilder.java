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
package com.thoughtworks.go.server.service.builders;

import java.io.File;

import com.thoughtworks.go.config.ExecTask;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.builder.CommandBuilder;
import com.thoughtworks.go.domain.builder.CommandBuilderWithArgList;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import org.springframework.stereotype.Component;

import static com.thoughtworks.go.util.FileUtil.applyBaseDirIfRelativeAndNormalize;

@Component
public class ExecTaskBuilder implements TaskBuilder<ExecTask> {
    @Override
    public Builder createBuilder(BuilderFactory builderFactory, ExecTask task, Pipeline pipeline, UpstreamPipelineResolver pipelineResolver) {
        String workingDir = task.workingDirectory();
        String command = task.command();

        File newWorkingDir = workingDir == null ? pipeline.defaultWorkingFolder() : new File(
                applyBaseDirIfRelativeAndNormalize(pipeline.defaultWorkingFolder(), new File(workingDir)));
        Builder builder = builderFactory.builderFor(task.cancelTask(), pipeline, pipelineResolver);
        String description = task.describe();

        if (!task.getArgList().isEmpty()) {
            return new CommandBuilderWithArgList(command, task.getArgList().toStringArray(), newWorkingDir, task.getConditions(), builder, description);
        } else {
            return new CommandBuilder(command, task.getArgs(), newWorkingDir, task.getConditions(), builder, description);
        }

    }
}
