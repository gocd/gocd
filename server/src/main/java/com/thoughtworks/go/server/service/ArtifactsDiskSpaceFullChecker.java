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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.server.messaging.EmailMessageDrafter;
import com.thoughtworks.go.server.messaging.SendEmailMessage;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArtifactsDiskSpaceFullChecker extends DiskSpaceChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactsDiskSpaceFullChecker.class);
    public static final HealthStateType ARTIFACTS_DISK_FULL_ID = HealthStateType.artifactsDiskFull();

    public ArtifactsDiskSpaceFullChecker(SystemEnvironment systemEnvironment, EmailSender sender,
                                         GoConfigService goConfigService, final SystemDiskSpaceChecker diskSpaceChecker) {
        super(sender, systemEnvironment, goConfigService.artifactsDir(), goConfigService, ARTIFACTS_DISK_FULL_ID, diskSpaceChecker);
    }

    @Override
    protected void createFailure(OperationResult result, long size, long availableSpace) {
        String msg = "GoCD has less than " + size + "Mb of disk space available. Scheduling has stopped, and will resume once more than " + size + "Mb is available.";
        LOGGER.error(msg);
        result.insufficientStorage("GoCD Server has run out of artifacts disk space. Scheduling has been stopped", msg, ARTIFACTS_DISK_FULL_ID);
    }

    @Override
    protected SendEmailMessage createEmail() {
        return EmailMessageDrafter.noArtifactsDiskSpaceMessage(systemEnvironment, getAdminMail(), targetFolderCanonicalPath());
    }

    @Override
    protected long limitInMb() {
        return systemEnvironment.getArtifactReposiotryFullLimit();
    }
}
