package com.thoughtworks.go.plugin.access.pluggabletask;

import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutor;
import com.thoughtworks.go.plugin.api.task.TaskView;
import com.thoughtworks.go.plugin.infra.PluginManager;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class MessageBasedTask implements Task {
    private PluginManager pluginManager;
    private String pluginId;

    public MessageBasedTask(PluginManager pluginManager, String pluginId) {
        this.pluginManager = pluginManager;
        this.pluginId = pluginId;
    }

    @Override
    public TaskConfig config() {
        throw new NotImplementedException();
    }

    @Override
    public TaskExecutor executor() {
        throw new NotImplementedException();
    }

    @Override
    public TaskView view() {
        throw new NotImplementedException();
    }

    @Override
    public ValidationResult validate(TaskConfig configuration) {
        throw new NotImplementedException();
    }
}
