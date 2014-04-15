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

import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.presentation.MissingPluggableTaskViewModel;
import com.thoughtworks.go.presentation.PluggableTaskViewModel;
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference;

import java.util.HashMap;
import java.util.Map;

/**
 * @understands creating a view model for a pluggable task.
 */
public class PluggableTaskViewModelFactory implements PluggableViewModelFactory<PluggableTask> {
    private Map<String, String> viewTemplates = new HashMap<String, String>();

    public PluggableTaskViewModelFactory() {
        viewTemplates.put("new", "admin/tasks/pluggable_task/new");
        viewTemplates.put("edit", "admin/tasks/pluggable_task/edit");
        viewTemplates.put("list-entry", "admin/tasks/pluggable_task/_list_entry.html");
    }

    public PluggableViewModel<PluggableTask> viewModelFor(final PluggableTask pluggableTask, String actionName) {
        if (PluggableTaskConfigStore.store().hasPreferenceFor(pluggableTask.getPluginConfiguration().getId())) {
            TaskPreference taskPreference = PluggableTaskConfigStore.store().preferenceFor(pluggableTask.getPluginConfiguration().getId());
            return new PluggableTaskViewModel(pluggableTask, viewTemplates.get(actionName), Renderer.ERB, taskPreference.getView().displayValue(), taskPreference.getView().template());
        }
        return new MissingPluggableTaskViewModel(pluggableTask, viewTemplates.get(actionName), Renderer.ERB);
    }
}
