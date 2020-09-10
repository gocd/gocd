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
package com.thoughtworks.go.server.service.backup;

import com.thoughtworks.go.server.domain.BackupProgressStatus;
import com.thoughtworks.go.server.domain.ServerBackup;
import com.thoughtworks.go.server.persistence.ServerBackupRepository;

public class BackupStatusUpdater implements BackupUpdateListener {
    private final ServerBackup serverBackup;
    private final ServerBackupRepository serverBackupRepository;

    public BackupStatusUpdater(ServerBackup serverBackup, ServerBackupRepository serverBackupRepository) {
        this.serverBackup = serverBackup;
        this.serverBackupRepository = serverBackupRepository;
    }

    @Override
    public void updateStep(BackupProgressStatus status) {
        serverBackup.setProgressStatus(status);
        this.serverBackupRepository.update(serverBackup);
    }

    @Override
    public void error(String message) {
        serverBackup.markError(message);
        this.serverBackupRepository.update(serverBackup);
    }

    @Override
    public void completed(String message) {
        serverBackup.markCompleted();
        serverBackup.setMessage(message);
        this.serverBackupRepository.update(serverBackup);
    }
}
