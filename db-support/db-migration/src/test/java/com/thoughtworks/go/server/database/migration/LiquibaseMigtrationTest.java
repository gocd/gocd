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

package com.thoughtworks.go.server.database.migration;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
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

public class LiquibaseMigtrationTest {
    private BasicDataSource dataSource;

    @BeforeEach
    void initializeDatasource() throws SQLException, LiquibaseException {
        dataSource = new BasicDataSource();
        dataSource.setUrl("jdbc:h2:mem:migration-test");
        dataSource.setDriverClassName(org.h2.Driver.class.getName());
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        migrate("liquibase.xml");
    }

    @AfterEach
    void destroyDatasource() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().execute("SHUTDOWN;");
        }

        dataSource.close();
    }

    @Test
    void shouldRemoveDataSharingRelatedTables_asPartOfMigration_2006_remove_data_sharing_tables() throws SQLException, LiquibaseException {
        //create data sharing tables for test purpose
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE DATASHARINGSETTINGS ();");
            statement.execute("CREATE TABLE USAGEDATAREPORTING ();");
        }

        verifyTableExists("DATASHARINGSETTINGS");
        verifyTableExists("USAGEDATAREPORTING");

        migrate("migrations/2006.xml");

        verifyLiquibaseTablesExists();
        verifyTableDoesNotExists("DATASHARINGSETTINGS");
        verifyTableDoesNotExists("USAGEDATAREPORTING");
    }

    private void migrate(String migration) throws SQLException, LiquibaseException {
        Connection connection = dataSource.getConnection();
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        Liquibase liquibase = new Liquibase("db-migration-scripts/" + migration, new ClassLoaderResourceAccessor(getClass().getClassLoader()), database);
        liquibase.update("test");
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

    private void verifyLiquibaseTablesExists() throws SQLException {
        verifyTableExists("DATABASECHANGELOG");
        verifyTableExists("DATABASECHANGELOGLOCK");
    }

    private void verifyTableExists(String tableName) throws SQLException {
        ArrayList<String> tables = tableList();
        assertThat(tables).contains(tableName);
    }

    private void verifyTableDoesNotExists(String tableName) throws SQLException {
        ArrayList<String> tables = tableList();
        assertThat(tables).doesNotContain(tableName);
    }
}
