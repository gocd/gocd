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

import com.thoughtworks.go.junit5.EnableIfH2;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class H2ServerDatabaseTest {

    private H2Database h2Database;
    private DatabaseFixture dbFixture;
    private SystemEnvironment env;

    @BeforeEach
    void copyDatabaseDirectory() throws IOException, SQLException {
        dbFixture = new DatabaseFixture();
        dbFixture.copyH2Db();

        env = dbFixture.env();

        H2Database tmpdb = new H2Database(env);
        tmpdb.startDatabase();
        tmpdb.upgrade();
        tmpdb.shutdown();

        env.setDebugMode(true);
        h2Database = new H2Database(env);
        h2Database.startDatabase();
    }

    @AfterEach
    void deleteTempDbDirectory() throws IOException, SQLException {
        h2Database.shutdown();
        try {
            dbFixture.tearDown();
        } catch (Exception e) {
            //ignore, there are sometimes the db file cannot be deleted on windows platform.
        }

    }

    @Test
    @EnableIfH2
    void shouldBeAbleToStartANewDatabase() throws SQLException {
        h2Database.startDatabase();
        assertDatabaseIsUp(h2Database);
    }

    @Test
    @EnableIfH2
    void shouldNotCrashIfStartTwice() throws SQLException {
        h2Database.startDatabase();
        h2Database.startDatabase();
        assertDatabaseIsUp(h2Database);
    }

    @Test
    @EnableIfH2
    void shouldDetectAlreadyRunningServer() throws SQLException {
        h2Database.startDatabase();

        H2Database h2Database2 = new H2Database(env);
        h2Database2.startDatabase();
        assertDatabaseIsUp(h2Database2);
    }

    private void assertDatabaseIsUp(H2Database db) throws SQLException {
        Connection connection = db.createDataSource().getConnection();
        ResultSet set = connection.getMetaData().getTables(null, null, null, null);
        assertThat(set.next()).isTrue();
    }

}
