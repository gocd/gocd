/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.helper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.ExecTask;
import com.thoughtworks.go.config.Tasks;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.builder.CommandBuilderWithArgList;
import com.thoughtworks.go.domain.Task;

public class BuilderMother {
    public static List<Builder> createBuildersAssumingAllExecTasks(CruiseConfig config, String pipelineName, String stageName, String jobName) {
        Tasks tasks = config.jobConfigByName(pipelineName, stageName, jobName, true).getTasks();
        ArrayList<Builder> builders = new ArrayList<>();
        for (Task task : tasks) {
            builders.add(builderFor((ExecTask) task));
        }
        return builders;
    }

    public static Builder builderFor(ExecTask task) {
        Builder cancelTask = null;
        if (task.cancelTask() != null) {
            cancelTask = builderFor((ExecTask) task.cancelTask());
        }
        return new CommandBuilderWithArgList(task.command(), task.getArgList().toStringArray(),
                new File(task.workingDirectory()), task.getConditions(), cancelTask, task.describe());
    }

}
