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

package com.thoughtworks.go.domain.builder;

import java.io.File;

import com.thoughtworks.go.domain.BuildLogElement;
import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.util.DateUtils;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.CheckedCommandLineException;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.CompositeConsumer;
import com.thoughtworks.go.util.command.CruiseControlException;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.util.command.ExecScript;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.apache.log4j.Logger;

public abstract class BaseCommandBuilder extends Builder {
    private static final Logger LOG = Logger.getLogger(BaseCommandBuilder.class);

    protected String command;
    protected File workingDir;
    protected String errorString = "";

    public BaseCommandBuilder(RunIfConfigs conditions, Builder cancelBuilder, String description, String command,
                              File workingDir) {
        super(conditions, cancelBuilder, description);
        this.command = command;
        this.workingDir = workingDir;
    }

    public void build(BuildLogElement buildLogElement, DefaultGoPublisher publisher, EnvironmentVariableContext environmentVariableContext, SystemEnvironment systemEnvironment, TaskExtension taskExtension)
            throws CruiseControlException {
        final long startTime = System.currentTimeMillis();
        final File realWorkingDir = systemEnvironment.resolveAgentWorkingDirectory(workingDir);

        if (!realWorkingDir.isDirectory()) {
            String message = "Working directory \"" + realWorkingDir.getAbsolutePath() + "\" is not a directory!";
            publisher.consumeLine(message);
            setBuildError(buildLogElement, message);
            throw new CruiseControlException(message);
        }

        ExecScript execScript = new ExecScript(errorString);
        CommandLine commandLine = buildCommandLine(realWorkingDir);

        // mimic Ant target/task logging
        buildLogElement.setBuildLogHeader(command);

        //TODO: Clean up this code and re-use the CommandLine code
        try {
            CompositeConsumer consumer = new CompositeConsumer(publisher, execScript);
            commandLine.runScript(execScript, consumer, environmentVariableContext, null);
            setBuildDuration(startTime, buildLogElement);

            if (execScript.foundError()) {
                // detected the error string in the command output
                String message = "Build failed. Command " + this.command + " reported ["
                        + errorString + "].";
                setBuildError(buildLogElement, message);
                buildLogElement.setTaskError();
                throw new CruiseControlException(message);

            } else if (execScript.getExitCode() != 0) {
                String message = "return code is " + execScript.getExitCode();
                setBuildError(buildLogElement, message);
                throw new CruiseControlException(message);
            }
        } catch (CheckedCommandLineException ex) {
            setBuildError(buildLogElement, "exec error");
            setTaskError(buildLogElement, "Could not execute command: " + commandLine.toStringForDisplay());
            throw ex;
        }
    }

    protected abstract CommandLine buildCommandLine(File realWorkingDirectory);

    private void setBuildDuration(long startTime, BuildLogElement buildLogElement) {
        final long endTime = System.currentTimeMillis();
        buildLogElement.setBuildDuration(DateUtils.getDurationAsString((endTime - startTime)));
    }

    private void setTaskError(BuildLogElement buildLogElement, String errorMessage) {
        LOG.warn(errorMessage);
        buildLogElement.setTaskError(errorMessage);
    }

    private void setBuildError(BuildLogElement buildLogElement, String errorMessage) {
        LOG.warn(errorMessage);
        buildLogElement.setBuildError(errorMessage);
    }

    @Override public String toString() {
        return "BaseCommandBuilder{" +
                "command='" + command + '\'' +
                ", workingDir=" + workingDir +
                ", errorString='" + errorString + '\'' +
                "} " + super.toString();
    }
}
