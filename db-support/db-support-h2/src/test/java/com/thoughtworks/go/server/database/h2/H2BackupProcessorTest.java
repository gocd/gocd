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

package com.thoughtworks.go.server.database.h2;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class H2BackupProcessorTest {

    @Test
    void shouldTakeBackup(@TempDir Path tempDir) throws SQLException {
        File originalDir = tempDir.resolve("originalDb").toFile();
        originalDir.mkdirs();
        File backupDir = tempDir.resolve("backupDir").toFile();
        backupDir.mkdirs();

        assertThat(backupDir.listFiles()).isNullOrEmpty();
        try (BasicDataSource ds = new BasicDataSource()) {
            ds.setDriverClassName(org.h2.Driver.class.getName());
            ds.setUrl("jdbc:h2:" + new File(originalDir, "test").getAbsoluteFile());
            ds.setUsername("sa");
            ds.setPassword("");

            new H2BackupProcessor().backup(backupDir, ds, null);
        }

        assertThat(backupDir.listFiles()).containsExactly(new File(backupDir, "db.zip"));
    }
}
