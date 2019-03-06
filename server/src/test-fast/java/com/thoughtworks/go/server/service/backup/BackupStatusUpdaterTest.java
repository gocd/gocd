/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import com.thoughtworks.go.server.domain.BackupStatus;
import com.thoughtworks.go.server.domain.ServerBackup;
import com.thoughtworks.go.server.persistence.ServerBackupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
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

        backupStatusUpdater.updateStep("new message");

        verify(serverBackupRepository).update(serverBackup);
        assertThat(serverBackup.getMessage(), is("new message"));
    }

    @Test
    void shouldUpdateError() {
        ServerBackup serverBackup = new ServerBackup("path", new Date(), "admin", "a message");
        BackupStatusUpdater backupStatusUpdater = new BackupStatusUpdater(serverBackup, serverBackupRepository);

        backupStatusUpdater.error("boom");

        verify(serverBackupRepository).update(serverBackup);
        assertThat(serverBackup.getMessage(), is("boom"));
        assertThat(serverBackup.getStatus(), is(BackupStatus.ERROR));
    }

    @Test
    void shouldMarkCompleted() {
        ServerBackup serverBackup = new ServerBackup("path", new Date(), "admin", "a message");
        BackupStatusUpdater backupStatusUpdater = new BackupStatusUpdater(serverBackup, serverBackupRepository);

        backupStatusUpdater.completed();

        verify(serverBackupRepository).update(serverBackup);
        assertThat(serverBackup.getMessage(), is("Backup was generated successfully."));
        assertThat(serverBackup.isSuccessful(), is(true));
    }
}
