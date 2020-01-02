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
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.serverhealth.HealthStateLevel;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.thoughtworks.go.server.service.DatabaseDiskSpaceFullChecker.DATABASE_DISK_FULL_ID;

public class DatabaseDiskSpaceWarningChecker extends DiskSpaceChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseDiskSpaceWarningChecker.class);
    private final ServerHealthService serverHealthService;

    public DatabaseDiskSpaceWarningChecker(EmailSender sender, SystemEnvironment systemEnvironment,
                                           GoConfigService goConfigService, final SystemDiskSpaceChecker diskSpaceChecker, ServerHealthService serverHealthService) {
        super(sender, systemEnvironment, systemEnvironment.getDbFolder(), goConfigService, DATABASE_DISK_FULL_ID, diskSpaceChecker);
        this.serverHealthService = serverHealthService;
    }

    @Override
    protected long limitInMb() {
        return systemEnvironment.getDatabaseDiskSpaceWarningLimit();
    }

    @Override
    protected void createFailure(OperationResult result, long size, long availableSpace) {
        String msg = "GoCD has less than " + size + "M of disk space available to it.";
        LOGGER.warn(msg);
        result.warning("GoCD Server's database is running on low disk space", msg, DATABASE_DISK_FULL_ID);
    }

    @Override
    protected SendEmailMessage createEmail() {
        return EmailMessageDrafter.lowDatabaseDiskSpaceMessage(systemEnvironment, getAdminMail(), targetFolderCanonicalPath());
    }

    @Override
    public void check(OperationResult result) {
        if (!serverHealthService.containsError(DATABASE_DISK_FULL_ID, HealthStateLevel.ERROR)) {
            super.check(result);
        }
    }
}
