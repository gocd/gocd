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

import com.thoughtworks.go.plugin.api.GoPluginApiMarker;
import com.thoughtworks.go.plugin.api.annotation.UsedOnGoAgent;
import com.thoughtworks.go.plugin.api.annotation.UsedOnGoServer;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;

/**
 * Task interface is the starting point for the task plugin.
 */
@GoPluginApiMarker
@Deprecated
public interface Task {
    /**
     * Specifies the configuration accepted and expected for the task. It has a list of configuration
     * properties (keys), with an optional default value each.
     *
     * @return an instance of {@link com.thoughtworks.go.plugin.api.task.TaskConfig}
     */
    @UsedOnGoServer
    @UsedOnGoAgent
    TaskConfig config();

    /**
     * The executor is the part of the plugin which actually does the work. It is an interface which
     * has a method, which will be called by the plugin infrastructure, with enough information
     * about the configuration and environment, when the task needs to be run.
     *
     * @return an implementation of {@link com.thoughtworks.go.plugin.api.task.TaskExecutor}
     */
    @UsedOnGoAgent
    TaskExecutor executor();

    /**
     * The implementation of TaskView returned by this method defines the view of the plugin, both in
     * the task type dropdown of the job configuration UI as well as the modal box for the task.
     *
     * @return an implementation of {@link com.thoughtworks.go.plugin.api.task.TaskView}
     */
    @UsedOnGoAgent
    TaskView view();

    /**
     * Checks if a given task configuration is valid. This is called during the "Save" action of the
     * task configuration UI, and not during the configuration XML save.
     *
     * @param configuration Task configuration which needs to be validated.
     *
     * @return an instance of {@link com.thoughtworks.go.plugin.api.response.validation.ValidationResult},
     * which has any errors that need to be shown on the UI. The key against which the error is created
     * should be the same as the one in the configuration.
     */
    @UsedOnGoServer
    ValidationResult validate(TaskConfig configuration);
}
