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

import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.builder.BuilderForKillAllChildTask;
import com.thoughtworks.go.domain.KillAllChildProcessTask;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import org.springframework.stereotype.Component;

@Component
public class KillAllChildProcessTaskBuilder implements TaskBuilder<KillAllChildProcessTask> {
    @Override
    public Builder createBuilder(BuilderFactory builderFactory, KillAllChildProcessTask task, Pipeline pipeline, UpstreamPipelineResolver resolver) {
        return new BuilderForKillAllChildTask();
    }
}
