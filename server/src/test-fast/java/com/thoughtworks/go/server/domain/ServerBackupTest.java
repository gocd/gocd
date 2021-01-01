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
package com.thoughtworks.go.server.domain;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class ServerBackupTest {

    @Test
    void shouldBeSuccessfulIfStatusCompleted() {
        assertThat(new ServerBackup("path", new Date(), "admin", "").isSuccessful()).isFalse();
        assertThat(new ServerBackup("path", new Date(), "admin", BackupStatus.ERROR, "", 123).isSuccessful()).isFalse();
        assertThat(new ServerBackup("path", new Date(), "admin", BackupStatus.COMPLETED, "", 123).isSuccessful()).isTrue();
    }

    @Test
    void shouldMarkCompleted() {
        ServerBackup backup = new ServerBackup("path", new Date(), "admin", "");
        assertThat(backup.getStatus()).isNotEqualTo(BackupStatus.COMPLETED);

        backup.markCompleted();
        assertThat(backup.getStatus()).isEqualTo(BackupStatus.COMPLETED);
        assertThat(backup.getMessage()).isEmpty();
    }

    @Test
    void shouldMarkError() {
        ServerBackup backup = new ServerBackup("path", new Date(), "admin", "");
        assertThat(backup.getStatus()).isNotEqualTo(BackupStatus.ERROR);
        assertThat(backup.hasFailed()).isFalse();

        backup.markError("boom");

        assertThat(backup.getStatus()).isEqualTo(BackupStatus.ERROR);
        assertThat(backup.getMessage()).isEqualTo("boom");
        assertThat(backup.hasFailed()).isTrue();
    }

    @Test
    void shouldSetProgressStatus() {
        ServerBackup backup = new ServerBackup("path", new Date(), "admin", "");
        assertThat(backup.getBackupProgressStatus()).hasValue(BackupProgressStatus.STARTING);

        backup.setProgressStatus(BackupProgressStatus.BACKUP_CONFIG);

        assertThat(backup.getBackupProgressStatus()).hasValue(BackupProgressStatus.BACKUP_CONFIG);
        assertThat(backup.getMessage()).isEqualTo(BackupProgressStatus.BACKUP_CONFIG.getMessage());
        assertThat(backup.getStatus()).isEqualTo(BackupStatus.IN_PROGRESS);
    }
}
