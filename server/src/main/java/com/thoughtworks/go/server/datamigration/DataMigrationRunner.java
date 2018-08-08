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

public class DataMigrationRunner {
    private DataMigrationRunner() {
    }

    public static void run(DataSource ds) throws SQLException {
        System.out.println("Running data migrations...");

        try (Connection cxn = ds.getConnection()) {
            exec(cxn, M001.convertPipelineSelectionsToFilters());
            exec(cxn, M002.ensureFilterStateIsNotNull());
        }

        System.out.println("Data migrations completed.");
    }

    private static void exec(Connection cxn, Migration migration) throws SQLException {
        cxn.setAutoCommit(false);

        try {
            migration.run(cxn);
            cxn.commit();
        } catch (SQLException e) {
            System.err.println("Migration failed!");
            cxn.rollback();
            throw e;
        }
    }
}
