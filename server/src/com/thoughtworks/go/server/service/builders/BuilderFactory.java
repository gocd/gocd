/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service.builders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.config.AntTask;
import com.thoughtworks.go.config.ExecTask;
import com.thoughtworks.go.config.FetchTask;
import com.thoughtworks.go.config.NantTask;
import com.thoughtworks.go.config.RakeTask;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.KillAllChildProcessTask;
import com.thoughtworks.go.domain.NullTask;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BuilderFactory {
    private final Map<Class, TaskBuilder> taskBuilderMap = new HashMap<>();

    @Autowired
    public BuilderFactory(AntTaskBuilder antTaskBuilder, ExecTaskBuilder execTaskBuilder, NantTaskBuilder nantTaskBuilder, RakeTaskBuilder rakeTaskBuilder,
                          PluggableTaskBuilderCreator pluggableTaskBuilderCreator, KillAllChildProcessTaskBuilder killAllChildProcessTaskBuilder, FetchTaskBuilder fetchTaskBuilder,
                          NullTaskBuilder nullTaskBuilder) {
        taskBuilderMap.put(AntTask.class, antTaskBuilder);
        taskBuilderMap.put(ExecTask.class, execTaskBuilder);
        taskBuilderMap.put(NantTask.class, nantTaskBuilder);
        taskBuilderMap.put(RakeTask.class, rakeTaskBuilder);
        taskBuilderMap.put(PluggableTask.class, pluggableTaskBuilderCreator);
        taskBuilderMap.put(KillAllChildProcessTask.class, killAllChildProcessTaskBuilder);
        taskBuilderMap.put(FetchTask.class, fetchTaskBuilder);
        taskBuilderMap.put(NullTask.class, nullTaskBuilder);
    }

    public List<Builder> buildersForTasks(Pipeline pipeline, List<Task> tasks, UpstreamPipelineResolver resolver) {
        ArrayList<Builder> builders = new ArrayList<>();
        for (Task task : tasks) {
            builders.add(builderFor(task, pipeline, resolver));
        }
        return builders;
    }

    public Builder builderFor(Task task, Pipeline pipeline, UpstreamPipelineResolver resolver) {
        return getBuilderImplementation(task).createBuilder(this, task, pipeline, resolver);
    }

    private TaskBuilder getBuilderImplementation(Task task) {
        if(!taskBuilderMap.containsKey(task.getClass()))
            throw new RuntimeException("Unexpected type of task: " + task.getClass());

        return taskBuilderMap.get(task.getClass());
    }
}
