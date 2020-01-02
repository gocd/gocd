/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config.validation;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.SystemEnvironment;

import java.io.File;

import static com.thoughtworks.go.util.SystemEnvironment.COMMAND_REPOSITORY_DIRECTORY;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class CommandRepositoryLocationValidator implements GoConfigValidator {

    private SystemEnvironment systemEnvironment;

    public CommandRepositoryLocationValidator(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
    }


    @Override
    public void validate(CruiseConfig cruiseConfig) throws Exception {
        String taskRepositoryLocation = cruiseConfig.server().getCommandRepositoryLocation();
        if (taskRepositoryLocation == null || isEmpty(taskRepositoryLocation.trim())) {
            throw new Exception("Command Repository Location cannot be empty");
        }
        if(taskRepositoryLocation.startsWith("/") || taskRepositoryLocation.startsWith("\\") || taskRepositoryLocation.contains("~") || taskRepositoryLocation.contains(":")){
            throw new Exception("Invalid Repository Location");
        }
        String taskRepositoryRootLocation = systemEnvironment.get(COMMAND_REPOSITORY_DIRECTORY);
        File taskRepositoryRootDirectory = new File(taskRepositoryRootLocation);
        File repository = new File(taskRepositoryRootLocation, taskRepositoryLocation);
        if (!FileUtil.isChildOf(taskRepositoryRootDirectory, repository)) {
            throw new Exception(String.format("Invalid  Repository Location, repository should be a subdirectory under %s", taskRepositoryRootDirectory.getAbsolutePath()));
        }
    }
}
