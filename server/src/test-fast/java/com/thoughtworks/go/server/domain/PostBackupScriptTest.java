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

import com.thoughtworks.go.server.service.BackupService;
import com.thoughtworks.go.util.command.CommandLine;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class PostBackupScriptTest {

    @Test
    void shouldCreateCommandLineForSuccessfulBackupInitiatedViaTimer() {
        Date time = new Date(1535551235962L);
        ServerBackup backup = new ServerBackup("/foo/bar/backupDir/backup_someTimeStamp", time, null, "");
        PostBackupScript postBackupScript = new PostBackupScript("upload-to-s3", BackupService.BackupInitiator.TIMER, null, backup, "/foo/bar/backupDir", time);
        CommandLine commandLine = postBackupScript.commandLine();
        assertThat(commandLine.getExecutable()).isEqualTo("upload-to-s3");
        assertThat(commandLine.getArguments()).isEmpty();
        assertThat(commandLine.getWorkingDirectory()).isNull();
        assertThat(commandLine.env())
                .hasSize(5)
                .containsEntry("GOCD_BACKUP_STATUS", "success")
                .containsEntry("GOCD_BACKUP_BASE_DIR", "/foo/bar/backupDir")
                .containsEntry("GOCD_BACKUP_PATH", "/foo/bar/backupDir/backup_someTimeStamp")
                .containsEntry("GOCD_BACKUP_TIMESTAMP", "2018-08-29T14:00:35Z")
                .containsEntry("GOCD_BACKUP_INITIATED_VIA", "TIMER");
    }

    @Test
    void shouldCreateCommandLineForBackupThatErroredWhenInitiatedViaTimer() {
        Date time = new Date(1535551235962L);
        PostBackupScript postBackupScript = new PostBackupScript("upload-to-s3", BackupService.BackupInitiator.TIMER, null, null, null, time);
        CommandLine commandLine = postBackupScript.commandLine();
        assertThat(commandLine.getExecutable()).isEqualTo("upload-to-s3");
        assertThat(commandLine.getArguments()).isEmpty();
        assertThat(commandLine.getWorkingDirectory()).isNull();
        assertThat(commandLine.env())
                .hasSize(3)
                .containsEntry("GOCD_BACKUP_STATUS", "failure")
                .containsEntry("GOCD_BACKUP_INITIATED_VIA", "TIMER")
                .containsEntry("GOCD_BACKUP_TIMESTAMP", "2018-08-29T14:00:35Z");
    }

    @Test
    void shouldCreateCommandLineForSuccessfulBackupInitiatedByUser() {
        Date time = new Date(1535551235962L);
        ServerBackup backup = new ServerBackup("/foo/bar/backupDir/backup_someTimeStamp", time, "bob", "");
        PostBackupScript postBackupScript = new PostBackupScript("upload-to-s3", BackupService.BackupInitiator.USER, "bob", backup, "/foo/bar/backupDir", time);
        CommandLine commandLine = postBackupScript.commandLine();
        assertThat(commandLine.getExecutable()).isEqualTo("upload-to-s3");
        assertThat(commandLine.getArguments()).isEmpty();
        assertThat(commandLine.getWorkingDirectory()).isNull();
        assertThat(commandLine.env())
                .hasSize(5)
                .containsEntry("GOCD_BACKUP_STATUS", "success")
                .containsEntry("GOCD_BACKUP_BASE_DIR", "/foo/bar/backupDir")
                .containsEntry("GOCD_BACKUP_PATH", "/foo/bar/backupDir/backup_someTimeStamp")
                .containsEntry("GOCD_BACKUP_TIMESTAMP", "2018-08-29T14:00:35Z")
                .containsEntry("GOCD_BACKUP_INITIATED_BY_USER", "bob");
    }

    @Test
    void shouldCreateCommandLineForBackupThatErroredWhenInitiatedByUser() {
        Date time = new Date(1535551235962L);
        PostBackupScript postBackupScript = new PostBackupScript("upload-to-s3", BackupService.BackupInitiator.USER, "bob", null, null, time);
        CommandLine commandLine = postBackupScript.commandLine();
        assertThat(commandLine.getExecutable()).isEqualTo("upload-to-s3");
        assertThat(commandLine.getArguments()).isEmpty();
        assertThat(commandLine.getWorkingDirectory()).isNull();
        assertThat(commandLine.env())
                .hasSize(3)
                .containsEntry("GOCD_BACKUP_STATUS", "failure")
                .containsEntry("GOCD_BACKUP_INITIATED_BY_USER", "bob")
                .containsEntry("GOCD_BACKUP_TIMESTAMP", "2018-08-29T14:00:35Z");
    }

    @Test
    void shouldReturnFalseAndNotBlowUpIfExecutionOfCommandFails() {
        Date time = new Date(1535551235962L);
        String command = RandomStringUtils.randomAlphabetic(32);
        ServerBackup backup = new ServerBackup("/foo/bar/backupDir/backup_someTimeStamp", time, "bob", "");
        PostBackupScript postBackupScript = new PostBackupScript(command, BackupService.BackupInitiator.USER, "bob", backup, "/foo/bar/backupDir", time);
        assertThat(postBackupScript.execute()).isFalse();
    }

    @Test
    void shouldReturnTrueIfExecutionOfCommandSucceeds() {
        Date time = new Date(1535551235962L);
        String command = "jcmd"; // provided by the JDK
        ServerBackup backup = new ServerBackup("/foo/bar/backupDir/backup_someTimeStamp", time, "bob", "");
        PostBackupScript postBackupScript = new PostBackupScript(command, BackupService.BackupInitiator.USER, "bob", backup, "/foo/bar/backupDir", time);
        assertThat(postBackupScript.execute()).isTrue();
    }
}
