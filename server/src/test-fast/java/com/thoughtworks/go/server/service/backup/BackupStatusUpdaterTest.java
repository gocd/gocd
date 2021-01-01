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
package com.thoughtworks.go.server.service.backup;

import com.thoughtworks.go.server.domain.BackupProgressStatus;
import com.thoughtworks.go.server.domain.BackupStatus;
import com.thoughtworks.go.server.domain.ServerBackup;
import com.thoughtworks.go.server.persistence.ServerBackupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class BackupStatusUpdaterTest {

    @Mock
    private ServerBackupRepository serverBackupRepository;

    @BeforeEach
    void setUp() {
        initMocks(this);
    }

    @Test
    void shouldUpdateMessageOnStepUpdate() {
        ServerBackup serverBackup = new ServerBackup("path", new Date(), "admin", "a message");
        BackupStatusUpdater backupStatusUpdater = new BackupStatusUpdater(serverBackup, serverBackupRepository);

        backupStatusUpdater.updateStep(BackupProgressStatus.BACKUP_DATABASE);

        verify(serverBackupRepository).update(serverBackup);
        assertThat(serverBackup.getStatus()).isEqualTo(BackupStatus.IN_PROGRESS);
        assertThat(serverBackup.getBackupProgressStatus()).hasValue(BackupProgressStatus.BACKUP_DATABASE);
        assertThat(serverBackup.getMessage()).isEqualTo(BackupProgressStatus.BACKUP_DATABASE.getMessage());
    }

    @Test
    void shouldUpdateError() {
        ServerBackup serverBackup = new ServerBackup("path", new Date(), "admin", "a message");
        BackupStatusUpdater backupStatusUpdater = new BackupStatusUpdater(serverBackup, serverBackupRepository);

        backupStatusUpdater.error("boom");

        verify(serverBackupRepository).update(serverBackup);
        assertThat(serverBackup.getMessage()).isEqualTo("boom");
        assertThat(serverBackup.getStatus()).isEqualTo(BackupStatus.ERROR);
    }

    @Test
    void shouldMarkCompleted() {
        ServerBackup serverBackup = new ServerBackup("path", new Date(), "admin", "a message");
        BackupStatusUpdater backupStatusUpdater = new BackupStatusUpdater(serverBackup, serverBackupRepository);

        backupStatusUpdater.completed("Backup was generated successfully.");

        verify(serverBackupRepository).update(serverBackup);
        assertThat(serverBackup.getMessage()).isEqualTo("Backup was generated successfully.");
        assertThat(serverBackup.isSuccessful()).isTrue();
    }
}
