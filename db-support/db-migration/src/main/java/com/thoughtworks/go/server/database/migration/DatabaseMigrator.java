/*
 * Copyright Thoughtworks, Inc.
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
import liquibase.Scope;
import liquibase.UpdateSummaryOutputEnum;
import liquibase.analytics.configuration.AnalyticsArgs;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.exception.LockException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.ui.LoggerUIService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.event.Level;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static org.slf4j.event.Level.INFO;
import static org.slf4j.event.Level.WARN;

@Slf4j
public class DatabaseMigrator {

    public void migrate(Connection connection) throws SQLException {
        try {
            log.info("Upgrading database, this might take a while depending on the size of the database.");

            logBoth(WARN, "Shutting down your server at this point may lead to database corruption. Please wait until the database upgrade completes.");
            migrateSchema("liquibase.xml", connection);
            logBoth(INFO, "Database schema upgrade completed successfully.");

            migrateData(connection);
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

    @VisibleForTesting
    void migrateSchema(String changeLogFile, Connection connection) throws LiquibaseException {
        configureLiquibase();
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        Liquibase liquibase = newLiquibaseFor(changeLogFile, database);
        liquibase.update();
    }

    @VisibleForTesting
    Liquibase newLiquibaseFor(String migration, Database database) {
        Liquibase liquibase = new Liquibase("db-migration-scripts/" + migration, new ClassLoaderResourceAccessor(getClass().getClassLoader()), database);
        liquibase.setShowSummaryOutput(UpdateSummaryOutputEnum.LOG);
        return liquibase;
    }

    private static void logBoth(Level level, String message) {
        System.out.printf("%s: %s%n", level, message);
        log.info(message);
    }

    private static void configureLiquibase() {
        try {
            Scope.enter(Map.of(AnalyticsArgs.ENABLED.getKey(), false));
        } catch (Exception e) {
            log.warn("Failed to disable liquibase analytics. Continuing anyway...", e);
        }

        // See https://github.com/liquibase/liquibase/issues/2396
        try {
            Scope.enter(Map.of(Scope.Attr.ui.name(), new LoggerUIService()));
        } catch (Exception e) {
            log.warn("Failed to disable liquibase console logging. Continuing anyway...", e);
        }
    }

    private void migrateData(Connection connection) throws SQLException {
        DataMigrationRunner.run(connection);
    }
}
