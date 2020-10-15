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

package com.thoughtworks.go.server.database;

import com.thoughtworks.go.server.database.migration.DatabaseMigrator;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.mockito.Mockito.*;

public class DatabaseTest {
    @AfterEach
    void tearDown() {
        new SystemEnvironment().clearProperty("go.server.mode");
    }

    @Test
    void shouldNotUpgradeDatabaseWhenServerIsInStandyMode() throws SQLException {
        SystemEnvironment systemEnvironment = new SystemEnvironment();
        DatabaseMigrator databaseMigrator = mock(DatabaseMigrator.class);
        ConnectionManager connectionManager = new ConnectionManager(System.getProperties(), systemEnvironment.configDir(), s -> null);

        systemEnvironment.setProperty("go.server.mode", "standby");

        Database database = new Database(systemEnvironment, connectionManager, databaseMigrator);

        database.getDataSource();

        verify(databaseMigrator, times(0)).migrate(any());
    }
}
