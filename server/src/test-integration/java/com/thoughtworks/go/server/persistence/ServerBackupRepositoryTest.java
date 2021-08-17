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
package com.thoughtworks.go.server.persistence;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;

import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.BackupStatus;
import com.thoughtworks.go.server.domain.ServerBackup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class ServerBackupRepositoryTest {

    @Autowired ServerBackupRepository repository;
    @Autowired
    DatabaseAccessHelper dbHelper;

    @BeforeEach
    public void setup() throws Exception {
        dbHelper.onSetUp();
    }

    @AfterEach
    public void teardown() throws Exception {
        dbHelper.onTearDown();
    }

    @Test
    public void shouldReturnTheLastSuccessfulBackupTime() {
        Date time = new Date();
        Timestamp completedBackupTime = new Timestamp(time.getTime() + 10000);

        ServerBackup completedBackup = new ServerBackup("file_path", completedBackupTime, "user", "", BackupStatus.COMPLETED);
        repository.save(completedBackup);

        Timestamp inProgressBackupTime = new Timestamp(time.getTime() + 20000);
        ServerBackup inProgressBackup = new ServerBackup("file_path", inProgressBackupTime, "user", "in progress backup", BackupStatus.IN_PROGRESS);
        repository.save(inProgressBackup);

        Optional<ServerBackup> actualBackup = repository.lastSuccessfulBackup();

        assertThat(actualBackup).hasValue(completedBackup);
    }

    @Test
    public void shouldReturnTheUserNameWhichTriggeredTheLastBackup() {
        Timestamp time = getTime();
        ServerBackup completedBackup = repository.save(new ServerBackup("file_path", time, "loser", "", BackupStatus.COMPLETED));
        repository.save(new ServerBackup("file_path", time, "new_loser", ""));

        assertThat(repository.lastSuccessfulBackup()).hasValue(completedBackup);
    }

    @Test
    public void shouldReturnEmptyWhenIdIsNull() {
        assertThat(repository.getBackup(null)).isEmpty();
    }

    @Test
    public void shouldReturnServerBackById() {
        ServerBackup serverBackup = repository.save(new ServerBackup("file_path", getTime(), "loser", ""));

        assertThat(repository.getBackup(serverBackup.getId())).hasValue(serverBackup);
    }

    @Test
    public void shouldUpdateServerBackup() {
        ServerBackup serverBackup = new ServerBackup("file_path", new Date(), "loser", "");
        repository.save(serverBackup);
        serverBackup.setMessage("new message");
        repository.update(serverBackup);

        assertThat(repository.getBackup(serverBackup.getId()).get().getMessage()).isEqualTo("new message");
    }

    @Test
    public void shouldMarkIncompleteServerBackupsAsAborted() {
        ServerBackup inProgressBackup = new ServerBackup("file_path1", getTime(), "loser", "", BackupStatus.IN_PROGRESS);
        ServerBackup completedBackup = new ServerBackup("file_path3", getTime(), "loser", "", BackupStatus.COMPLETED);
        repository.save(inProgressBackup);
        repository.save(completedBackup);

        repository.markInProgressBackupsAsAborted("Backup was aborted.");

        ServerBackup abortedBackup = repository.getBackup(inProgressBackup.getId()).get();
        assertThat(abortedBackup.getStatus()).isEqualTo(BackupStatus.ABORTED);
        assertThat(abortedBackup.getMessage()).isEqualTo("Backup was aborted.");
        assertThat(repository.getBackup(completedBackup.getId())).hasValue(completedBackup);
    }

    @Test
    public void shouldReturnNullWhenThereAreNoEntries() {
        assertThat(repository.lastSuccessfulBackup()).isEmpty();
    }

    private Timestamp getTime() {
        return new Timestamp(new Date().getTime());
    }
}
