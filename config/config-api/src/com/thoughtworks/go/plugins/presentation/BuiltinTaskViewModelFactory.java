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

package com.thoughtworks.go.plugins.presentation;

import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.presentation.TaskViewModel;

/**
* @understands creating a view model for a built in task.
*/
public class BuiltinTaskViewModelFactory implements PluggableViewModelFactory {
    private Map<String, String> viewTemplates;

    public BuiltinTaskViewModelFactory(String taskType) {
        viewTemplates = new HashMap<>();
        viewTemplates.put("new", String.format("admin/tasks/%s/new", taskType));
        viewTemplates.put("edit", String.format("admin/tasks/%s/edit", taskType));
        viewTemplates.put("list-entry", "admin/tasks/plugin/_task_entry_value_fields.html");
    }

    public PluggableViewModel viewModelFor(Object renderable, String actionName) {
        return new TaskViewModel((Task) renderable, viewTemplates.get(actionName), Renderer.ERB);
    }
}
