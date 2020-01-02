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

import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.builder.pluggableTask.PluggableTaskBuilder;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PluggableTaskBuilderCreator implements TaskBuilder<PluggableTask> {

    @Autowired
    public PluggableTaskBuilderCreator() {
    }

    @Override
    public Builder createBuilder(BuilderFactory builderFactory, PluggableTask task, Pipeline pipeline, UpstreamPipelineResolver resolver) {
        Builder cancelBuilder = builderFactory.builderFor(task.cancelTask(), pipeline, resolver);
        return new PluggableTaskBuilder(task.getConditions(), cancelBuilder,
                task, "Plugin with ID: " + task.getPluginConfiguration().getId(),
                pipeline.defaultWorkingFolder().getPath());
    }
}
