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

package com.thoughtworks.go.config.validation;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.BuildTask;
import com.thoughtworks.go.config.FetchTask;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.domain.TaskConfigVisitor;
import static com.thoughtworks.go.util.ExceptionUtils.bombUnless;
import com.thoughtworks.go.util.FileUtil;

/**
 * @understands validating that the working folder of a task is inside sandbox
 */
public class TaskWorkingFolderValidator implements GoConfigValidator {

    public void validate(CruiseConfig cruiseConfig) throws Exception {
        cruiseConfig.accept(new TaskConfigVisitor() {
            public void visit(PipelineConfig pipelineConfig, StageConfig stageConfig, JobConfig jobConfig, Task task) {
                if (task instanceof BuildTask) {
                    validate(pipelineConfig, stageConfig, jobConfig, ((BuildTask) task).workingDirectory());
                } else if (task instanceof FetchTask) {
                    FetchTask fetchTask = (FetchTask) task;
                    validate(pipelineConfig, stageConfig, jobConfig, fetchTask.getSrc());
                    validate(pipelineConfig, stageConfig, jobConfig, fetchTask.getDest());
                }
            }

            private void validate(PipelineConfig pipelineConfig, StageConfig stageConfig, JobConfig jobConfig, String path) {
                if (path == null) {
                    return;
                }
                bombUnless(FileUtil.isFolderInsideSandbox(path),
                        String.format("Task of job '%s' in stage '%s' of pipeline '%s' has path '%s' which is outside the working directory.",
                                jobConfig.name(), stageConfig.name(), pipelineConfig.name(), path));
            }
        });
    }
}
