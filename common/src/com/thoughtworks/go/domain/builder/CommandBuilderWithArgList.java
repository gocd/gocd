/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.util.SystemUtil;
import com.thoughtworks.go.util.command.CommandLine;
import org.apache.commons.lang.StringUtils;

public class CommandBuilderWithArgList extends BaseCommandBuilder {
    private String[] args;

    public CommandBuilderWithArgList(String command, String[] args, File workingDir, RunIfConfigs conditions,
                                     Builder cancelBuilder, String description) {
        super(conditions, cancelBuilder, description, command, workingDir);
        this.args = args;
    }

    protected CommandLine buildCommandLine() {
        CommandLine command = null;
        if (SystemUtil.isWindows()) {
            command = CommandLine.createCommandLine("cmd").withWorkingDir(workingDir);
            command.withArg("/c");
            command.withArg(translateToWindowsPath(this.command));
        }
        else {
            command = CommandLine.createCommandLine(this.command).withWorkingDir(workingDir);
        }
        for (String arg : args) {
            command.withArg(arg);
        }
        return command;
    }

    private String translateToWindowsPath(String command) {
        return StringUtils.replace(command, "/", "\\");
    }

    @Override
    public BuildCommand buildCommand() {
        BuildCommand exec = BuildCommand.exec(this.command, args);
        if (workingDir != null) {
            exec.setWorkingDirectory(workingDir.getPath());
        }
        return exec;
    }
}
