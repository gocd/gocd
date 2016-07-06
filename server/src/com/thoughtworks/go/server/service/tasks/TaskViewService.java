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

package com.thoughtworks.go.server.service.tasks;

import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.domain.NullTask;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.plugins.presentation.PluggableViewModel;
import com.thoughtworks.go.service.TaskFactory;
import com.thoughtworks.go.util.ListUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @understands providing view model to render a onCancelTask
 */
@Service
public class TaskViewService implements TaskFactory {
    private final ConfigElementImplementationRegistry registry;
    private final PluginManager pluginManager;

    @Autowired
    public TaskViewService(ConfigElementImplementationRegistry registry, PluginManager pluginManager) {
        this.registry = registry;
        this.pluginManager = pluginManager;
    }

    public List<PluggableViewModel> getTaskViewModels() {
        return getTaskViewModelsWith(new NullTask());
    }

    public PluggableViewModel getModelOfType(List<PluggableViewModel> taskViewModels, final String givenTaskType){
        return ListUtil.find(taskViewModels, new ListUtil.Condition() {
            @Override
            public <T> boolean isMet(T item) {
                return ((PluggableViewModel) item).getTaskType().equals(givenTaskType);
            }
        });
    }

    public List<PluggableViewModel> getTaskViewModelsWith(Task given) {
        List<PluggableViewModel> viewModels = new ArrayList<>();
        for (Task task : allTasks()) {
            if (task.hasSameTypeAs(given)) {
                viewModels.add(getViewModel(given, "new"));
            } else {
                viewModels.add(getViewModel(task, "new"));
            }
        }
        return viewModels;
    }

    public PluggableViewModel getViewModel(Task task, String actionName) {
        return registry.getViewModelFor(task, actionName);
    }

    public Task taskInstanceFor(String type) {
        List<Task> tasks = allTasks();
        for (Task task : tasks) {
            if (task.getTaskType().equals(type)) {
                return task;
            }
        }
        throw new RuntimeException(String.format("Could not find any task of type: %s", type));
    }

    public PluggableTask createPluggableTask(String pluginId) {
        List<PluggableTask> tasks = allPluginTasks();
        for (PluggableTask task : tasks) {
            if (task.getPluginConfiguration().getId().equals(pluginId)) {
                return task;
            }
        }
        throw new RuntimeException(String.format("Could not find any task with id: %s", pluginId));
    }

    public List<PluggableViewModel<Task>> getOnCancelTaskViewModels(Task given) {
        List<PluggableViewModel<Task>> viewModels = new ArrayList<>();
        for (Task task : allTasks()) {
            if (hasCancelTaskOfType(given, task)) {
                viewModels.add(getViewModel(given.cancelTask(), "edit"));
            } else {
                viewModels.add(getViewModel(task, "new"));
            }
        }
        return viewModels;
    }

    private boolean hasCancelTaskOfType(Task given, Task onCancelTask) {
        return given.cancelTask().hasSameTypeAs(onCancelTask);
    }

    private List<Task> allTasks() {
        List<Task> result = new ArrayList<>();
        for (Class<? extends Task> aClass : builtinTaskClasses()) {
            try {
                result.add(aClass.getConstructor().newInstance());
            } catch (Exception e) {
                throw new RuntimeException(String.format("Could not instantiate class %s of type Task", aClass.getName()), e);
            }
        }

        result.addAll(allPluginTasks());
        return result;
    }

    private Configuration getConfiguration(TaskConfig taskConfig) {
        Configuration configuration = new Configuration();
        for (Property property : taskConfig.list()) {
            configuration.addNewConfigurationWithValue(property.getKey(), property.getValue(), property.getOption(Property.SECURE));
        }
        return configuration;
    }

    private List<PluggableTask> allPluginTasks() {
        final ArrayList<PluggableTask> tasks = new ArrayList<>();
        for (final String pluginId : PluggableTaskConfigStore.store().pluginIds()) {
            GoPluginDescriptor pluginDescriptor = pluginManager.getPluginDescriptorFor(pluginId);
            TaskPreference taskPreference = PluggableTaskConfigStore.store().preferenceFor(pluginId);
            if (pluginDescriptor != null && taskPreference != null) {
                tasks.add(new PluggableTask(new PluginConfiguration(pluginId, pluginDescriptor.version()), getConfiguration(taskPreference.getConfig())));
            }
        }
        return tasks;
    }

    private List<Class<? extends Task>> builtinTaskClasses() {
        List<Class<? extends Task>> allTaskClasses = registry.implementersOf(Task.class);
        List<Class<? extends Task>> builtinTaskClasses = new ArrayList<>();

        for (Class<? extends Task> taskClass : allTaskClasses) {
            if (taskClass != PluggableTask.class) {
                builtinTaskClasses.add(taskClass);
            }
        }
        return builtinTaskClasses;
    }
}
