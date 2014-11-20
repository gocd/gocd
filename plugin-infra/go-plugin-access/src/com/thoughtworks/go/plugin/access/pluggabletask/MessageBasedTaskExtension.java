package com.thoughtworks.go.plugin.access.pluggabletask;

import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.infra.ActionWithReturn;
import com.thoughtworks.go.plugin.infra.PluginManager;

class MessageBasedTaskExtension extends TaskExtensionImpl {
    MessageBasedTaskExtension(PluginManager pluginManager) {
        super(pluginManager);
    }

    @Override
    public ExecutionResult execute(String pluginId, ActionWithReturn<Task, ExecutionResult> actionWithReturn) {
        final MessageBasedTask task = new MessageBasedTask(pluginManager, pluginId);
        return actionWithReturn.execute(task, pluginManager.getPluginDescriptorFor(pluginId));
    }
}