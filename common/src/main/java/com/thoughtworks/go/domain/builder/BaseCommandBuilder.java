/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

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
    public void build(DefaultGoPublisher publisher, EnvironmentVariableContext environmentVariableContext, TaskExtension taskExtension, ArtifactExtension artifactExtension, PluginRequestProcessorRegistry pluginRequestProcessorRegistry, String consoleLogCharset)
            throws CruiseControlException {

        if (!workingDir.isDirectory()) {
            String message = "Working directory \"" + workingDir.getAbsolutePath() + "\" is not a directory!";
            publisher.taggedConsumeLine(DefaultGoPublisher.ERR, message);
            setBuildError(message);
            throw new CruiseControlException(message);
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
                String message = "Build failed. Command " + this.command + " reported ["
                        + errorString + "].";
                setBuildError(message);
                throw new CruiseControlException(message);
            } else if (SUCCESS_EXIT_CODE != execScript.getExitCode()) {
                String message = "return code is " + execScript.getExitCode();
                setBuildError(message);
                throw new CruiseControlException(message);
            }
        } catch (CheckedCommandLineException ex) {
            setBuildError("exec error");
            setTaskError("Could not execute command: " + commandLine.toStringForDisplay());
            throw ex;
        }
    }

    protected abstract CommandLine buildCommandLine();

    private void setTaskError(String errorMessage) {
        LOG.warn(errorMessage);
    }

    private void setBuildError(String errorMessage) {
        LOG.warn(errorMessage);
    }

    @Override
    public String toString() {
        return "BaseCommandBuilder{" +
                "command='" + command + '\'' +
                ", workingDir=" + workingDir +
                ", errorString='" + errorString + '\'' +
                "} " + super.toString();
    }
}
