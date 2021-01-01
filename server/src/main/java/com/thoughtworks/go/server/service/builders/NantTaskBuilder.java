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
package com.thoughtworks.go.server.service.builders;

import java.io.File;

import com.thoughtworks.go.config.NantTask;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.builder.CommandBuilder;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import com.thoughtworks.go.util.FileUtil;
import org.springframework.stereotype.Component;

@Component
public class NantTaskBuilder implements TaskBuilder<NantTask> {
    @Override
    public Builder createBuilder(BuilderFactory builderFactory, NantTask task, Pipeline pipeline, UpstreamPipelineResolver resolver) {
        File taskWorkingDirectory = new File(FileUtil.join(pipeline.defaultWorkingFolder(), task.workingDirectory()));
        String command = task.command();
        String argument = task.arguments();
        Builder cancelBuilder = builderFactory.builderFor(task.cancelTask(), pipeline, resolver);
        return new CommandBuilder(command, argument, taskWorkingDirectory, task.getConditions(), cancelBuilder, task.describe(), "BUILD FAILED");
    }
}
