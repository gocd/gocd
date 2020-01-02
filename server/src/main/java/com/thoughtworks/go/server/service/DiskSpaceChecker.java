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

import com.thoughtworks.go.server.messaging.SendEmailMessage;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemEnvironment;

import java.io.File;
import java.io.IOException;

/**
 * @understands when disk space is too low to be safe on server
 */
public abstract class DiskSpaceChecker implements SchedulingChecker {
    protected final SystemEnvironment systemEnvironment;
    private final EmailSender sender;
    protected GoConfigService goConfigService;
    private final HealthStateType healthStateType;
    private boolean error = false;
    private File targetFolder;
    private String targetFolderCanicalPath;
    private final SystemDiskSpaceChecker diskSpaceChecker;
    private boolean targetExists;
    private long availableSpace;
    private volatile long lastCheckedTime;

    // TODO: will separate SchedulingChecker responsibility out
    public DiskSpaceChecker(EmailSender sender, SystemEnvironment systemEnvironment, File targetFolder,
                            GoConfigService goConfigService, HealthStateType healthStateType, final SystemDiskSpaceChecker diskSpaceChecker) {
        this.sender = sender;
        this.systemEnvironment = systemEnvironment;
        this.goConfigService = goConfigService;
        this.healthStateType = healthStateType;
        this.targetFolder = targetFolder;
        this.diskSpaceChecker = diskSpaceChecker;
    }

    protected synchronized String targetFolderCanonicalPath() {
        if (this.targetFolderCanicalPath == null) {
            targetFolderCanicalPath = getCanicalPath(this.targetFolder);
        }
        return targetFolderCanicalPath;
    }

    @Override
    public void check(OperationResult result) {
        if (timeSinceLastChecked() > systemEnvironment.getDiskSpaceCacheRefresherInterval()) {
            synchronized (this) {
                if (timeSinceLastChecked() > systemEnvironment.getDiskSpaceCacheRefresherInterval()) {
                    targetExists = targetFolder.exists();
                    availableSpace = availableSpace();
                    lastCheckedTime = System.currentTimeMillis();
                }
            }
        }
        long size = limitInMb();
        if (!targetExists) {
            result.success(healthStateType);
            return;
        }

        long limit = size * GoConstants.MEGA_BYTE;

        boolean notEnoughSpace = availableSpace < limit;
        if (notEnoughSpace) {
            if (!isInErrorState()) {
                inErrorState();

                if (sender != null) {
                    sender.sendEmail(createEmail());
                }
            }
            createFailure(result, size, availableSpace);
        }
        else {
            clearErrorState();
            result.success(healthStateType);
        }
    }

    protected long availableSpace() {
        return diskSpaceChecker.getUsableSpace(targetFolder);
    }

    private long timeSinceLastChecked() {
        return System.currentTimeMillis() - lastCheckedTime;
    }

    private String getCanicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }

    protected abstract long limitInMb();

    protected abstract void createFailure(OperationResult result, long size, long availableSpace);

    protected abstract SendEmailMessage createEmail();

    private boolean isInErrorState() {
        return error;
    }

    private void inErrorState() {
        error = true;
    }

    private void clearErrorState() {
        error = false;
    }

    protected String getAdminMail() {
        return goConfigService.adminEmail();
    }

    public OperationResult resultFor(OperationResult result) {
        return result;
    }
}
