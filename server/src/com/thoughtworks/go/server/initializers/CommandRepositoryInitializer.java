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

package com.thoughtworks.go.server.initializers;

import com.thoughtworks.go.server.domain.Version;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipInputStream;

import static com.thoughtworks.go.util.SystemEnvironment.DEFAULT_COMMAND_SNIPPETS_ZIP;
import static com.thoughtworks.go.util.SystemEnvironment.VERSION_FILE_IN_DEFAULT_COMMAND_REPOSITORY;

@Component
public class CommandRepositoryInitializer implements Initializer {
    private static final Logger LOG = Logger.getLogger(CommandRepositoryInitializer.class);
    private SystemEnvironment systemEnvironment;
    private ZipUtil zipUtil;
    private ServerHealthService serverHealthService;

    @Autowired
    public CommandRepositoryInitializer(SystemEnvironment systemEnvironment, ZipUtil zipUtil, ServerHealthService serverHealthService) {
        this.systemEnvironment = systemEnvironment;
        this.zipUtil = zipUtil;
        this.serverHealthService = serverHealthService;
    }

    @Override
    public void initialize() {
        File defaultDirectory = systemEnvironment.getDefaultCommandRepository();

        try {
            if (shouldUsePackagedRepository(defaultDirectory)) {
                usePackagedCommandRepository(getPackagedRepositoryZipStream(), defaultDirectory);
            }
        } catch (Exception e) {
            String message = "Unable to upgrade command repository located at " + defaultDirectory.getAbsolutePath() + ". Message: " + e.getMessage();
            serverHealthService.update(ServerHealthState.error("Command Repository", message, HealthStateType.commandRepositoryUpgradeIssue()));
            LOG.error("[Command Repository] " + message, e);
        }
    }

    private boolean shouldUsePackagedRepository(File defaultDirectory) throws IOException {
        if (!defaultDirectory.exists()) {
            return true;
        }
        Version packagedVersion = getPackagedVersion();
        Version existingVersion = getExistingVersion(defaultDirectory);
        return packagedVersion.isAtHigherVersionComparedTo(existingVersion);
    }

    void usePackagedCommandRepository(ZipInputStream zipInputStream, File defaultDirectory) throws IOException {
        FileUtil.deleteDirectoryNoisily(defaultDirectory);
        zipUtil.unzip(zipInputStream, defaultDirectory);
    }

    private Version getPackagedVersion() throws IOException {
        try (ZipInputStream zipInputStream = getPackagedRepositoryZipStream()) {
            return new Version(zipUtil.getFileContentInsideZip(zipInputStream, systemEnvironment.get(VERSION_FILE_IN_DEFAULT_COMMAND_REPOSITORY)));
        }
    }

    Version getExistingVersion(File defaultCommandRepositoryDirectory) throws IOException {
        File file = new File(defaultCommandRepositoryDirectory, systemEnvironment.get(VERSION_FILE_IN_DEFAULT_COMMAND_REPOSITORY));
        if (!file.exists()) {
            return Version.belowAllVersions();
        }
        return new Version(FileUtils.readFileToString(file));
    }

    ZipInputStream getPackagedRepositoryZipStream() {
        return new ZipInputStream(this.getClass().getResourceAsStream(systemEnvironment.get(DEFAULT_COMMAND_SNIPPETS_ZIP)));
    }

}
