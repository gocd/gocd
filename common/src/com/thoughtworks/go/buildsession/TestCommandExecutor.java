/*************************** GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ************************GO-LICENSE-END***********************************/
package com.thoughtworks.go.buildsession;

import com.thoughtworks.go.domain.BuildCommand;

import java.io.File;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.lang.String.format;

public class TestCommandExecutor implements BuildCommandExecutor {
    @Override
    public boolean execute(BuildCommand command, BuildSession buildSession) {

        String flag = command.getStringArg("flag");
        String left = command.getStringArg("left");

        if ("-eq".equals(flag)) {
            return left.equals(captureSubCommandOutput(command, buildSession));
        }

        if ("-neq".equals(flag)) {
            return !left.equals(captureSubCommandOutput(command, buildSession));
        }

        File target = buildSession.resolveRelativeDir(command.getWorkingDirectory(), left);
        if ("-d".equals(flag)) {
            return target.isDirectory();
        }

        if ("-nd".equals(flag)) {
            return !target.isDirectory();
        }

        if ("-f".equals(flag)) {
            return target.isFile();
        }

        if ("-nf".equals(flag)) {
            return !target.isFile();
        }

        throw bomb(format("Unknown flag %s for command: %s", flag, command));
    }

    private String captureSubCommandOutput(BuildCommand command, BuildSession buildSession) {
        BuildCommand targetCommand = command.getSubCommands().get(0);
        ConsoleCapture consoleCapture = new ConsoleCapture();
        buildSession.newTestingSession(consoleCapture).processCommand(targetCommand);
        return consoleCapture.captured();
    }
}
