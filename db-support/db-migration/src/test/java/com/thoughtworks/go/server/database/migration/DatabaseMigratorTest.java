/*
 * Copyright 2024 Thoughtworks, Inc.
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

package com.thoughtworks.go.server.database.migration;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.exception.LiquibaseException;
import liquibase.exception.LockException;
import org.h2.jdbc.JdbcConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseMigratorTest {
    @TempDir
    private Path tempDir;
    @Mock
    private Liquibase liquibase;
    private DatabaseMigrator migrator;

    @BeforeEach
    void setUp() {
        this.migrator = new DatabaseMigrator() {
            @Override
            Liquibase newLiquibaseFor(Database database) {
                return liquibase;
            }
        };
    }

    @Test
    public void shouldRunMigrationOnRequestedConnection() throws Exception {

        try (MockedStatic<DataMigrationRunner> migration = mockStatic(DataMigrationRunner.class); Connection connection = dummyH2Connection()) {
            migrator.migrate(connection);

            verify(liquibase).update();
            migration.verify(() -> DataMigrationRunner.run(connection));
        }
    }

    @Test
    public void shouldRaiseWrappedLockExceptionWhenDatabaseLocked() throws Exception {

        try (MockedStatic<DataMigrationRunner> migration = mockStatic(DataMigrationRunner.class); Connection connection = dummyH2Connection()) {
            doThrow(new LockException("Database locked")).when(liquibase).update();

            assertThatThrownBy(() -> migrator.migrate(connection))
                .isExactlyInstanceOf(SQLException.class)
                .hasMessageContaining("Unable to migrate the database, as it is currently locked.")
                .hasRootCauseExactlyInstanceOf(LockException.class)
                .hasRootCauseMessage("Database locked");

            migration.verifyNoInteractions();
        }
    }

    @Test
    public void shouldRaiseWrappedExceptionForOtherErrors() throws Exception {

        try (MockedStatic<DataMigrationRunner> migration = mockStatic(DataMigrationRunner.class); Connection connection = dummyH2Connection()) {
            doThrow(new LiquibaseException("Liquibase error")).when(liquibase).update();

            assertThatThrownBy(() -> migrator.migrate(connection))
                .isExactlyInstanceOf(SQLException.class)
                .hasMessageContaining("Unable to migrate the database")
                .hasRootCauseExactlyInstanceOf(LiquibaseException.class)
                .hasRootCauseMessage("Liquibase error");

            migration.verifyNoInteractions();
        }
    }

    private JdbcConnection dummyH2Connection() throws SQLException {
        return new JdbcConnection("jdbc:h2:" + tempDir.resolve("testdb"), new Properties());
    }
}
