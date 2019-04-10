/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.persistence;

import java.util.Date;
import java.util.Optional;

import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.BackupStatus;
import com.thoughtworks.go.server.domain.ServerBackup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml",
        "classpath:WEB-INF/spring-all-servlet.xml",
})
public class ServerBackupRepositoryTest {

    @Autowired ServerBackupRepository repository;
    @Autowired
    DatabaseAccessHelper dbHelper;

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
    }

    @Test
    public void shouldReturnTheLastSuccessfulBackupTime() {
        Date time = new Date();
        Date completedBackupTime = new Date(time.getTime() + 10000);

        ServerBackup completedBackup = new ServerBackup("file_path", completedBackupTime, "user", "", BackupStatus.COMPLETED);
        repository.save(completedBackup);

        Date inProgressBackupTime = new Date(time.getTime() + 20000);
        ServerBackup inProgressBackup = new ServerBackup("file_path", inProgressBackupTime, "user", "in progress backup", BackupStatus.IN_PROGRESS);
        repository.save(inProgressBackup);

        Optional<ServerBackup> actualBackup = repository.lastSuccessfulBackup();

        assertThat(actualBackup).hasValue(completedBackup);
    }

    @Test
    public void shouldReturnTheUserNameWhichTriggeredTheLastBackup() {
        Date time = new Date();
        repository.save(new ServerBackup("file_path", time, "loser", "", BackupStatus.COMPLETED));
        repository.save(new ServerBackup("file_path", time, "new_loser", ""));

        assertThat(new ServerBackup("file_path", time, "loser", "", BackupStatus.COMPLETED)).isEqualTo(repository.lastSuccessfulBackup());
    }

    @Test
    public void shouldReturnNullWhenIdIsNull() {
        assertThat(repository.getBackup(null)).isNull();
    }

    @Test
    public void shouldReturnServerBackById() {
        ServerBackup serverBackup = repository.save(new ServerBackup("file_path", new Date(), "loser", ""));

        assertThat(serverBackup).isEqualTo(repository.getBackup(serverBackup.getId()));
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
        ServerBackup inProgressBackup = new ServerBackup("file_path1", new Date(), "loser", "", BackupStatus.IN_PROGRESS);
        ServerBackup completedBackup = new ServerBackup("file_path3", new Date(), "loser", "", BackupStatus.COMPLETED);
        repository.save(inProgressBackup);
        repository.save(completedBackup);

        repository.markInProgressBackupsAsAborted("Backup was aborted.");

        ServerBackup abortedBackup = repository.getBackup(inProgressBackup.getId()).get();
        assertThat(abortedBackup.getStatus()).isEqualTo(BackupStatus.ABORTED);
        assertThat(abortedBackup.getMessage()).isEqualTo("Backup was aborted.");
        assertThat(repository.getBackup(completedBackup.getId())).isEqualTo(completedBackup);
    }

    @Test
    public void shouldReturnNullWhenThereAreNoEntries() {
        assertThat(repository.lastSuccessfulBackup()).isNull();
    }
}
