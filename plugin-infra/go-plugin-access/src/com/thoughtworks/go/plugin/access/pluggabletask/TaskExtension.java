package com.thoughtworks.go.plugin.access.pluggabletask;

import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.infra.ActionWithReturn;
import com.thoughtworks.go.plugin.infra.PluginManager;

import java.util.HashMap;

public class TaskExtension {

    private final String API_BASED = "API_BASED";
    private final String MESSAGE_BASED = "MESSAGE_BASED";
    protected PluginManager pluginManager;
    private final HashMap<String, TaskExtensionImpl> map;

    public TaskExtension(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
        map = new HashMap<String, TaskExtensionImpl>();
        map.put(API_BASED, new ApiBasedTaskExtension(pluginManager));
        map.put(MESSAGE_BASED, new MessageBasedTaskExtension(pluginManager));
    }

    TaskExtensionImpl getImpl(String pluginId) {
        TaskExtensionImpl extension = null;
        if (pluginManager.hasReferenceFor(Task.class, pluginId)) {
            extension = map.get(API_BASED);
        } else if (pluginManager.hasReferenceFor(GoPlugin.class, pluginId)) {
            extension = map.get(MESSAGE_BASED);
        }
        if (extension != null) return extension;
        throw new RuntimeException(String.format("Plugin should use either message-based or api-based extension. Plugin-id: %s", pluginId));
    }

    public ExecutionResult execute(String pluginId, ActionWithReturn<Task, ExecutionResult> actionWithReturn) {
        return getImpl(pluginId).execute(pluginId, actionWithReturn);
    }
}

abstract class TaskExtensionImpl {
    protected PluginManager pluginManager;

    protected TaskExtensionImpl(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    abstract ExecutionResult execute(String pluginId, ActionWithReturn<Task, ExecutionResult> actionWithReturn);
}