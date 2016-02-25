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

package com.thoughtworks.go.plugin.api.task;

import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;

/**
 * The implementation of this TaskExecutor interface will be the one which does the actual work of the task.
 *
 */
@Deprecated
//Will be moved to internal scope
public interface TaskExecutor {
    /**
     * The plugin infrastructure will call the execute() method, on the agent side, when the task needs to be run.
     *
     * @param config Has the current configuration. Contains both the keys and the values provided by the user.
     * @param taskExecutionContext Instance of {@link TaskExecutionContext}
     * @return The result of the execution (decides the status of the task)
     */
    ExecutionResult execute(TaskConfig config, TaskExecutionContext taskExecutionContext);
}
