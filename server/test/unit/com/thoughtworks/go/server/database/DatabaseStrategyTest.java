/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.database;

import java.sql.SQLException;

import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatabaseStrategyTest {

    private SystemEnvironment systemEnvironment;
    private DatabaseStrategy databaseStrategy;
    private DummyDbProvider database;

    @Before
    public void setUp() throws Exception {
        systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.getDatabaseProvider()).thenReturn(DummyDbProvider.class.getCanonicalName());
        databaseStrategy = new DatabaseStrategy(systemEnvironment);
        database = ((DummyDbProvider) databaseStrategy.getDatabase());
    }

    @Test
    public void shouldStartDatabase() {
        databaseStrategy.startDatabase();
        assertThat(database.startDatabase, is(true));
    }

    @Test
    public void shouldCreateDataSource() {
        databaseStrategy.createDataSource();
        assertThat(database.createDataSource, is(true));
    }

    @Test
    public void shouldUpgradeDatabase() throws SQLException {
        databaseStrategy.upgrade();
        assertThat(database.upgrade, is(true));
    }

    @Test
    public void shouldShutdownDatabase() throws SQLException {
        databaseStrategy.shutdown();
        assertThat(database.shutdown, is(true));
    }

    @Test
    public void shouldBackupDatabase() throws SQLException {
        databaseStrategy.backup(null);
        assertThat(database.backup, is(true));
    }

    @Test
    public void shouldGetIbatisConfigXmlLocation() throws SQLException {
        databaseStrategy.getIbatisConfigXmlLocation();
        assertThat(database.getIbatisConfigXmlLocation, is(true));
    }

    @Test
    public void shouldGetQueryExtensions() throws SQLException {
        databaseStrategy.getQueryExtensions();
        assertThat(database.getQueryExtensions, is(true));
    }
}

