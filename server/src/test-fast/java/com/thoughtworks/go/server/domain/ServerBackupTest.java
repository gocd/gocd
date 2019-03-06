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

package com.thoughtworks.go.server.domain;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class ServerBackupTest {

    @Test
    void shouldBeSuccessfulIfStatusCompleted() {
        assertThat(new ServerBackup("path", new Date(), "admin", "").isSuccessful(), is(false));
        assertThat(new ServerBackup("path", new Date(), "admin", BackupStatus.ERROR, "", 123).isSuccessful(), is(false));
        assertThat(new ServerBackup("path", new Date(), "admin", BackupStatus.COMPLETED, "", 123).isSuccessful(), is(true));
    }

    @Test
    void shouldMarkCompleted() {
        ServerBackup backup = new ServerBackup("path", new Date(), "admin", "");
        assertThat(backup.getStatus(), not(BackupStatus.COMPLETED));

        backup.markCompleted();
        assertThat(backup.getStatus(), is(BackupStatus.COMPLETED));
        assertThat(backup.getMessage(), is(""));
    }

    @Test
    void shouldMarkError() {
        ServerBackup backup = new ServerBackup("path", new Date(), "admin", "");
        assertThat(backup.getStatus(), not(BackupStatus.ERROR));
        assertThat(backup.hasFailed(), is(false));

        backup.markError("boom");

        assertThat(backup.getStatus(), is(BackupStatus.ERROR));
        assertThat(backup.getMessage(), is("boom"));
        assertThat(backup.hasFailed(), is(true));
    }
}
