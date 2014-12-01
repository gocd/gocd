package com.thoughtworks.go.plugin.access.pluggabletask;

import com.thoughtworks.go.plugin.api.task.ConsoleForJsonBasedPlugin;
import com.thoughtworks.go.plugin.api.task.EnvironmentVariables;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;

import java.io.InputStream;
import java.util.Map;

public class ConsoleWrapper implements ConsoleForJsonBasedPlugin {
    private com.thoughtworks.go.plugin.api.task.Console console;
    private EnvironmentVariables environment;

    public ConsoleWrapper(TaskExecutionContext taskExecutionContext) {
        console = taskExecutionContext.console();
        environment = taskExecutionContext.environment();
    }

    com.thoughtworks.go.plugin.api.task.Console getConsole() {
        return console;
    }

    EnvironmentVariables getEnvironment() {
        return environment;
    }

    @Override
    public void printLine(String line) {
        console.printLine(line);
    }

    @Override
    public void readErrorOf(InputStream in) {
        console.readErrorOf(in);
    }

    @Override
    public void readOutputOf(InputStream in) {
        console.readOutputOf(in);
    }

    @Override
    public void printEnvironment(Map<String, String> environment) {
        console.printEnvironment(environment, this.environment.secureEnvSpecifier());
    }
}
