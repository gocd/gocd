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

package com.thoughtworks.go.server.service.support.garage;

import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.CommandLineException;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class GarageService {

    private final SystemEnvironment systemEnvironment;
    static final String PROCESS_TAG = "garage-service";
    private static final String LOCALIZED_KEY = "GARAGE_MESSAGE";

    @Autowired
    public GarageService(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
    }

    public GarageData getData() {
        File configRepoDir = systemEnvironment.getConfigRepoDir();
        String configRepositorySize = getDirectorySize(configRepoDir);
        return new GarageData(configRepositorySize);
    }

    public void gc(HttpLocalizedOperationResult result) {
        CommandLine gitCheck = getGit().withArg("--version");
        InMemoryStreamConsumer consumer = new InMemoryStreamConsumer();
        if (execute(gitCheck, consumer, result)) {
            File configRepoDir = systemEnvironment.getConfigRepoDir();
            CommandLine gc = getGit().withArg("gc").withWorkingDir(configRepoDir);
            execute(gc, consumer, result);
        }
    }

    CommandLine getGit() {
        return CommandLine.createCommandLine("git");
    }

    String getDirectorySize(File configRepoDir) {
        return FileUtils.byteCountToDisplaySize(FileUtils.sizeOfDirectory(configRepoDir));
    }

    private boolean execute(CommandLine command, InMemoryStreamConsumer consumer, HttpLocalizedOperationResult result) {
        int returnValue;
        try {
            returnValue = command.run(consumer, PROCESS_TAG);
        } catch (CommandLineException e) {
            result.badRequest(LocalizedMessage.string(LOCALIZED_KEY, e.getMessage()));
            return false;
        }
        if (returnValue != 0) {
            result.badRequest(LocalizedMessage.string(LOCALIZED_KEY, consumer.getAllOutput()));
        } else {
            result.setMessage(LocalizedMessage.string(LOCALIZED_KEY, consumer.getAllOutput()));
        }
        return returnValue == 0;
    }
}
