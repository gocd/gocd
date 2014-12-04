package com.thoughtworks.go.plugin.access.pluggabletask;

import com.thoughtworks.go.plugin.api.task.JobConsoleLogger;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;

public class JobConsoleLoggerInternal extends JobConsoleLogger {
    private JobConsoleLoggerInternal() {
        super();
    }

    public static void setContext(TaskExecutionContext taskExecutionContext) {
        JobConsoleLogger.context = taskExecutionContext;
    }

    public static void unsetContext() {
        JobConsoleLogger.context = null;
    }
}
