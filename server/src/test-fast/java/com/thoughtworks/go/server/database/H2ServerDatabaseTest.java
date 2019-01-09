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

package com.thoughtworks.go.server.database;

import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.RunIf;
import com.thoughtworks.go.junitext.DatabaseChecker;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JunitExtRunner.class)
public class H2ServerDatabaseTest {

    private H2Database h2Database;
    private DatabaseFixture dbFixture;
    private SystemEnvironment env;

    @Before
    public void copyDatabaseDirectory() throws IOException, SQLException {
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

    @After
    public void deleteTempDbDirectory() throws IOException, SQLException {
        h2Database.shutdown();
        try {
            dbFixture.tearDown();
        } catch (Exception e) {
            //ignore, there are sometimes the db file cannot be deleted on windows platform.
        }

    }

    @Test
    @RunIf(value = DatabaseChecker.class, arguments = {DatabaseChecker.H2})
    public void shouldBeAbleToStartANewDatabase() throws SQLException {
        h2Database.startDatabase();
        assertDatabaseIsUp(h2Database);
    }

    @Test
    @RunIf(value = DatabaseChecker.class, arguments = {DatabaseChecker.H2})
    public void shouldNotCrashIfStartTwice() throws SQLException {
        h2Database.startDatabase();
        h2Database.startDatabase();
        assertDatabaseIsUp(h2Database);
    }

    @Test
    @RunIf(value = DatabaseChecker.class, arguments = {DatabaseChecker.H2})
    public void shouldDetectAlreadyRunningServer() throws SQLException {
        h2Database.startDatabase();

        H2Database h2Database2 = new H2Database(env);
        h2Database2.startDatabase();
        assertDatabaseIsUp(h2Database2);
    }

    private void assertDatabaseIsUp(H2Database db) throws SQLException {
        Connection connection = db.createDataSource().getConnection();
        ResultSet set = connection.getMetaData().getTables(null, null, null, null);
        assertThat(set.next(), is(true));
    }

}
