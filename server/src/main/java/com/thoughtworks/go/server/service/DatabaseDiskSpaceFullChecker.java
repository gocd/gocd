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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.server.messaging.EmailMessageDrafter;
import com.thoughtworks.go.server.messaging.SendEmailMessage;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class DatabaseDiskSpaceFullChecker extends DiskSpaceChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseDiskSpaceFullChecker.class);

    public static final HealthStateType DATABASE_DISK_FULL_ID = HealthStateType.databaseDiskFull();

    public DatabaseDiskSpaceFullChecker(EmailSender sender, SystemEnvironment systemEnvironment,
                                        GoConfigService goConfigService, final SystemDiskSpaceChecker diskSpaceChecker) {
        // TODO
        super(sender, systemEnvironment, new File(SystemEnvironment.DB_BASE_DIR), goConfigService, DATABASE_DISK_FULL_ID, diskSpaceChecker);
    }

    @Override
    protected long limitInMb() {
        return systemEnvironment.getDatabaseDiskSpaceFullLimit();
    }

    @Override
    protected void createFailure(OperationResult result, long size, long availableSpace) {
        String msg = "GoCD has less than " + size + "Mb of disk space available. Scheduling has stopped, and will resume once more than " + size + "Mb is available.";
        LOGGER.error(msg);
        result.insufficientStorage("GoCD Server has run out of database disk space. Scheduling has been stopped", msg, DATABASE_DISK_FULL_ID);
    }

    @Override
    protected SendEmailMessage createEmail() {
        return EmailMessageDrafter.noDatabaseDiskSpaceMessage(systemEnvironment, getAdminMail(), targetFolderCanonicalPath());
    }
}
