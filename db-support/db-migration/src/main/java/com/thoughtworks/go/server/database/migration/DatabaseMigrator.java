/*
 * Copyright 2023 Thoughtworks, Inc.
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

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.exception.LockException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.ui.LoggerUIService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.repeat;

@Slf4j
public class DatabaseMigrator {

    public void migrate(Connection connection) throws SQLException {
        try {
            log.info("Upgrading database, this might take a while depending on the size of the database.");

            List<String> messages = List.of(
                    repeat("*", "", 72),
                    "WARNING: Shutting down your server at this point will lead to a database corruption. Please wait until the database upgrade completes.",
                    repeat("*", "", 72)
            );
            for (String message : messages) {
                System.err.println(message);
                log.info(message);
            }

            disableLiquibaseConsoleLogging();

            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            newLiquibaseFor(database).update(new Contexts());

            System.err.println("INFO: Database upgrade completed successfully.");
            log.info("Database upgrade completed successfully.");

            DataMigrationRunner.run(connection);
        } catch (LockException e) {
            String message = "Unable to migrate the database, as it is currently locked. A previous GoCD start-up may have been interrupted during migration, and you may need to " +
                "1) validate no GoCD instances are running, " +
                "2) check the DB health looks OK, " +
                "3) unlock by connecting directly to the database and running the command noted at https://docs.liquibase.com/concepts/tracking-tables/databasechangeloglock-table.html, " +
                "4) restarting GoCD.";
            log.error("{} The problem was: [{}] cause: [{}]", message, ExceptionUtils.getMessage(e), ExceptionUtils.getRootCauseMessage(e), e);
            throw new SQLException(message, e);
        } catch (LiquibaseException e) {
            String message = "Unable to migrate the database.";
            log.error("{} The problem was: [{}] cause: [{}]", message, ExceptionUtils.getMessage(e), ExceptionUtils.getRootCauseMessage(e), e);
            throw new SQLException(message, e);
        }
    }

    Liquibase newLiquibaseFor(Database database) {
        return new Liquibase("db-migration-scripts/liquibase.xml", new ClassLoaderResourceAccessor(getClass().getClassLoader()), database);
    }

    private static void disableLiquibaseConsoleLogging() {
        // See https://github.com/liquibase/liquibase/issues/2396
        try {
            Scope.enter(Map.of(Scope.Attr.ui.name(), new LoggerUIService()));
        } catch (Exception e) {
            log.error("Failed to disable liquibase console logging. Continuing anyway...", e);
        }
    }
}
