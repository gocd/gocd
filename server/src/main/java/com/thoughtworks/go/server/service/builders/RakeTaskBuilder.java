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

import com.thoughtworks.go.config.RakeTask;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.builder.CommandBuilder;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import com.thoughtworks.go.util.FileUtil;
import org.springframework.stereotype.Component;

@Component
public class RakeTaskBuilder implements TaskBuilder<RakeTask> {
    @Override
    public Builder createBuilder(BuilderFactory builderFactory, RakeTask task, Pipeline pipeline, UpstreamPipelineResolver resolver) {
        String newWorkingDir = FileUtil.join(pipeline.defaultWorkingFolder(), task.workingDirectory());
        String argument = task.arguments();
        Builder builder = builderFactory.builderFor(task.cancelTask(), pipeline, resolver);
        return new CommandBuilder("rake", argument, new File(newWorkingDir), task.getConditions(), builder, task.describe());
    }
}
