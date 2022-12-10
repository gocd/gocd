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
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.ui.LoggerUIService;
import lombok.extern.slf4j.Slf4j;

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
            Liquibase liquibase = new Liquibase("db-migration-scripts/liquibase.xml", new ClassLoaderResourceAccessor(getClass().getClassLoader()), database);
            liquibase.update(new Contexts());

            System.err.println("INFO: Database upgrade completed successfully.");
            log.info("Database upgrade completed successfully.");

            DataMigrationRunner.run(connection);
        } catch (LiquibaseException e) {
            String message = "Unable to create database upgrade script for database. The problem was: " + e.getMessage();
            if (e.getCause() != null) {
                message += ". The cause was: " + e.getCause().getMessage();
            }
            log.error(message, e);
            System.err.println(message);
            e.printStackTrace(System.err);
            throw new SQLException("Unable to migrate the database", e);
        }
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
