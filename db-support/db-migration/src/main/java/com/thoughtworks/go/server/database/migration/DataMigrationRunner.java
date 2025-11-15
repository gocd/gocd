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

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;

@Slf4j
public class DataMigrationRunner {

    public static void run(Connection cxn) throws SQLException {
        log.info("Running data migrations...");

        exec(cxn, M001.convertPipelineSelectionsToFilters());
        exec(cxn, M002.ensureFilterStateIsNotNull());

        log.info("Data migrations completed.");
    }

    private static void exec(Connection cxn, Migration migration) throws SQLException {
        cxn.setAutoCommit(false);

        try {
            Instant start = Instant.now();
            migration.run(cxn);
            cxn.commit();
            log.info("Data migration took {} ms", Duration.between(start, Instant.now()).toMillis());
        } catch (SQLException e) {
            log.error("Data migration failed: {}", e.getMessage(), e);
            cxn.rollback();
            throw e;
        }
    }

}
