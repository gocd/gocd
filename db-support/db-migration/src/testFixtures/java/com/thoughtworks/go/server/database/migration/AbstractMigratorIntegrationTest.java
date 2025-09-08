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

import org.assertj.db.type.AssertDbConnection;
import org.assertj.db.type.AssertDbConnectionFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.db.api.Assertions.assertThat;

public abstract class AbstractMigratorIntegrationTest {
    protected void migrate(JdbcDatabaseContainer<?> container, String listTableQuery, String username, String password) throws SQLException {
        AssertDbConnection source = AssertDbConnectionFactory.of(container.getJdbcUrl(), username, password).create();
        assertThat(source.request(listTableQuery).build()).hasNumberOfRows(0);

        try (Connection connection = container.createConnection("")) {
            new DatabaseMigrator().migrate(connection);
        }

        assertThat(source.request(listTableQuery).build()).hasNumberOfRowsGreaterThan(10);
    }

}
