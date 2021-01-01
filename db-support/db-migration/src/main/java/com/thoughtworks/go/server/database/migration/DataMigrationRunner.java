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
package com.thoughtworks.go.server.database.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;

public class DataMigrationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataMigrationRunner.class);

    private DataMigrationRunner() {
    }

    public static void run(Connection cxn) throws SQLException {
        LOGGER.info("Running data migrations...");

        exec(cxn, M001.convertPipelineSelectionsToFilters());
        exec(cxn, M002.ensureFilterStateIsNotNull());

        LOGGER.info("Data migrations completed.");
    }

    private static void exec(Connection cxn, Migration migration) throws SQLException {
        cxn.setAutoCommit(false);

        try {
            Instant start = Instant.now();
            migration.run(cxn);
            cxn.commit();
            LOGGER.info("Data migration took {} ms", Duration.between(start, Instant.now()).toMillis());
        } catch (SQLException e) {
            LOGGER.error("Data migration failed: {}", e.getMessage(), e);
            cxn.rollback();
            throw e;
        }
    }

}
