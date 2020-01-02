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
package com.thoughtworks.go.server.database;

import com.thoughtworks.go.database.Database;
import com.thoughtworks.go.junit5.EnableIfH2;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static com.thoughtworks.go.server.database.DatabaseFixture.assertColumnType;
import static com.thoughtworks.go.server.database.DatabaseFixture.numericQuery;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@EnableIfH2
class H2DatabaseTest {
    private H2Database h2Database;
    private DatabaseFixture dbFixture;

    @BeforeEach
    void copyDatabaseDirectory() throws IOException {
        System.setProperty("db.maxActive", "20");
        System.setProperty("db.maxIdle", "10");
        dbFixture = new DatabaseFixture();
        dbFixture.copyH2Db();
        SystemEnvironment env = dbFixture.env();
        env.setDebugMode(false);
        h2Database = new H2Database(env);
        h2Database.startDatabase();
    }

    @AfterEach
    void deleteTempDbDirectory() throws IOException, SQLException {
        h2Database.shutdown();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }
        dbFixture.tearDown();
    }

    @Test
    void shouldBeAbleToStartANewDatabase() throws SQLException {
        h2Database.startDatabase();

        Connection connection = h2Database.createDataSource().getConnection();
        ResultSet set = connection.getMetaData().getTables(null, null, null, null);
        assertThat(set.next(), is(true));
    }

    @Test
    void shouldUpgradeDatabase() throws SQLException {
        h2Database.shutdown();
        h2Database.startDatabase();
        BasicDataSource dataSource = h2Database.createDataSource();
        h2Database.upgrade();
        h2Database.startDatabase();

        dataSource = h2Database.createDataSource();
        Connection connection = dataSource.getConnection();
        ResultSet set = connection.getMetaData().getTables(null, null, null, null);

        assertThat(set.next(), is(true));
    }

    @Test
    void shouldMigrateBuildbufferToTextColumn() throws SQLException, IOException {
        h2Database.startDatabase();
        h2Database.upgrade();

        BasicDataSource dataSource = h2Database.createDataSource();
        assertColumnType(dataSource, "modifications", "comment", DatabaseFixture.LONGVARCHAR);
        assertColumnType(dataSource, "properties", "value", "VARCHAR(255)");
    }

    @Test
    void shouldPopulateModificationTablePipelineIdWithTheCorrectPipeline_CaseInSensitiveIssue() throws Exception {
        dbFixture.copyDeltas();
        dbFixture.copyH2Db("migration84_test_db.zip");
        H2Database database = new H2Database(dbFixture.env());
        database.startDatabase();

        database.upgrade();

        database.shutdown();

        database.startDatabase();

        int afterRename = numericQuery("SELECT pipelineId FROM modifications WHERE revision = 'Zoo/4/foo/1'", database);
        int beforeRename = numericQuery("SELECT pipelineId FROM modifications WHERE revision = 'zoo/1/up-stage/1'", database);

        assertThat(afterRename == beforeRename, is(false));
    }

    @Test
    void shouldUseMVCCWhenRunning() throws Exception {
        h2Database.startDatabase();
        h2Database.upgrade();

        BasicDataSource dataSource = h2Database.createDataSource();
        assertThat(dataSource.getUrl(), containsString("MVCC=TRUE"));
    }

    @Test
    void shouldBackupDatabase() throws Exception {
        File destDir = new File(".");
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        Database database = new H2Database(systemEnvironment);
        Database spy = spy(database);
        BasicDataSource dataSource = mock(BasicDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        doReturn(dataSource).when(spy).createDataSource();
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);

        spy.backup(destDir);

        verify(statement).execute(anyString());
    }

    @Test
    void shouldThrowUpWhenBackupFails() throws Exception {
        File destDir = new File(".");
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        Database database = new H2Database(systemEnvironment);
        Database spy = spy(database);
        BasicDataSource dataSource = mock(BasicDataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        doReturn(dataSource).when(spy).createDataSource();
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenThrow(new SQLException("i failed"));

        try {
            spy.backup(destDir);
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("i failed"));
        }

        verify(statement).execute(anyString());
    }

    @Test
    void shouldUseParamterizedActiveAndIdleConnections() {
        BasicDataSource dataSource = h2Database.createDataSource();
        assertThat(dataSource.getMaxTotal(), is(20));
        assertThat(dataSource.getMaxIdle(), is(10));
    }

    @Test
    @EnableIfH2
    void shouldGetDialect() {
        assertThat(h2Database.dialectForHibernate(), is(H2Database.DIALECT_H2));
    }

    @Test
    @EnableIfH2
    void shouldGetDatabaseType() {
        assertThat(h2Database.getType(), is("h2"));
    }

    @Test
    @EnableIfH2
    void shouldGetDatabaseSpecificIbatisConfigXmlLocation() {
        assertThat(h2Database.getIbatisConfigXmlLocation(), nullValue());
    }
}
