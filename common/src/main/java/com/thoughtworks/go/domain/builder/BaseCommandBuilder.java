/*
 * Copyright 2024 Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.domain.builder;

import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.util.command.*;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.Charset;

public abstract class BaseCommandBuilder extends Builder {
    private static final Logger LOG = LoggerFactory.getLogger(BaseCommandBuilder.class);

    protected String command;
    protected File workingDir;
    protected String errorString = "";

    public BaseCommandBuilder(RunIfConfigs conditions, Builder cancelBuilder, String description, String command,
                              File workingDir) {
        super(conditions, cancelBuilder, description);
        this.command = command;
        this.workingDir = workingDir;
    }

    @Override
    public void build(DefaultGoPublisher publisher, EnvironmentVariableContext environmentVariableContext, TaskExtension taskExtension, ArtifactExtension artifactExtension, PluginRequestProcessorRegistry pluginRequestProcessorRegistry, Charset consoleLogCharset) {

        if (!workingDir.isDirectory()) {
            String message = "Working directory \"" + workingDir.getAbsolutePath() + "\" is not a directory!";
            publisher.taggedConsumeLine(DefaultGoPublisher.ERR, message);
            LOG.warn(message);
            throw new IllegalStateException(message);
        }

        ExecScript execScript = new ExecScript(errorString);
        CommandLine commandLine = buildCommandLine();
        commandLine.withEncoding(consoleLogCharset);

        //TODO: Clean up this code and re-use the CommandLine code
        try {
            CompositeConsumer consumer = new CompositeConsumer(publisher, execScript);
            commandLine.runScript(execScript, consumer, environmentVariableContext, null);

            if (SUCCESS_EXIT_CODE != execScript.getExitCode()) {
                setExitCode(execScript.getExitCode());
            }

            if (execScript.foundError()) {
                // detected the error string in the command output
                String message = "Build failed. Command " + this.command + " reported [" + errorString + "].";
                LOG.warn(message);
                throw new CommandLineException(message);
            } else if (SUCCESS_EXIT_CODE != execScript.getExitCode()) {
                String message = "return code is " + execScript.getExitCode();
                LOG.warn(message);
                throw new CommandLineException(message);
            }
        } catch (CommandLineException ex) {
            LOG.warn("exec error");
            LOG.warn("Could not execute command: " + commandLine.toStringForDisplay());
            throw ex;
        }
    }

    protected CommandLine buildCommandLine() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return CommandLine.createCommandLine("cmd")
                    .withWorkingDir(workingDir)
                    .withArgs("/s", "/c", "\"")
                    .withArg(translateToWindowsPath(this.command))
                    .withArgs(argList())
                    .withArg("\"");
        } else {
            return CommandLine.createCommandLine(this.command)
                    .withWorkingDir(workingDir)
                    .withArgs(argList());
        }
    }

    protected abstract String[] argList();

    @Override
    public String toString() {
        return "BaseCommandBuilder{" +
                "command='" + command + '\'' +
                ", workingDir=" + workingDir +
                ", errorString='" + errorString + '\'' +
                "} " + super.toString();
    }

    private String translateToWindowsPath(String command) {
        return StringUtils.replace(command, "/", "\\");
    }
}
