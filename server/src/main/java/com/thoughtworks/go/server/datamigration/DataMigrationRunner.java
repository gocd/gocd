/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.datamigration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;

import static java.lang.String.format;

public class DataMigrationRunner {
    private DataMigrationRunner() {
    }

    public static void run(DataSource ds) throws SQLException {
        info("Running data migrations...");

        try (Connection cxn = ds.getConnection()) {
            exec(cxn, M001.convertPipelineSelectionsToFilters());
            exec(cxn, M002.ensureFilterStateIsNotNull());
        }

        info("Data migrations completed.");
    }

    private static void exec(Connection cxn, Migration migration) throws SQLException {
        cxn.setAutoCommit(false);

        try {
            Instant start = Instant.now();
            migration.run(cxn);
            cxn.commit();
            info("Data migration took %d ms", Duration.between(start, Instant.now()).toMillis());
        } catch (SQLException e) {
            err("Data migration failed: %s", e.getMessage());
            cxn.rollback();
            throw e;
        }
    }

    private static void info(String message, Object... tokens) {
        System.out.println(format(message, tokens));
    }

    private static void err(String message, Object... tokens) {
        System.err.println(format(message, tokens));
    }
}
