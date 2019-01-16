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

package com.thoughtworks.go.server.database.h2;

import com.thoughtworks.go.server.database.migration.DatabaseMigrator;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;


class DatabaseMigratorH2Test {

    private BasicDataSource dataSource;

    @BeforeEach
    void initializeDatasource() {
        dataSource = new BasicDataSource();
        dataSource.setUrl("jdbc:h2:mem:migration-test");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
    }

    @AfterEach
    void destroyDatasource() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().execute("SHUTDOWN;");
        }
        dataSource.close();
    }

    @Test
    void shouldMigrate() throws SQLException {
        assertThat(tableList()).isEmpty();

        try (Connection connection = dataSource.getConnection()) {
            new DatabaseMigrator().migrate(connection);
        }

        // assert a few tables
        assertThat(tableList())
                .isNotEmpty()
                .contains("USERS")
                .contains("PIPELINES")
                .contains("STAGES")
                .contains("BUILDS");
    }

    private ArrayList<String> tableList() throws SQLException {
        ArrayList<String> tables = new ArrayList<>();

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.tables WHERE table_schema = 'PUBLIC' and table_type='TABLE';");
            ResultSet resultSet = statement.getResultSet();
            while (resultSet.next()) {
                String tableName = resultSet.getString("TABLE_NAME");
                tables.add(tableName);
            }
        }

        return tables;
    }
}
