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

import java.io.IOException;
import java.sql.SQLException;

import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.RunIf;
import com.thoughtworks.go.junitext.DatabaseChecker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(JunitExtRunner.class)
public class DbMigrationTest {
    private DatabaseFixture dbFixture;
    private H2Database h2Database;


    @Before
    public void setUp() throws Exception {
        dbFixture = new DatabaseFixture();
    }

    @After
    public void deleteTempDbDirectory() throws IOException, SQLException {
        h2Database.shutdown();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }
        //dbFixture.tearDown();
    }

    @Test
    @RunIf(value = DatabaseChecker.class, arguments = {DatabaseChecker.H2})
    public void testMigration86() throws IOException, SQLException {
        dbFixture.copyDeltas();
        dbFixture.copyH2Db("migration86_test_db.zip");
        h2Database = new H2Database(dbFixture.env());

        h2Database.startDatabase();
        h2Database.upgrade();

        assertPmrFromIdAndActualFromId("down", 2, 7, 7);
        assertPmrFromIdAndActualFromId("down", 3, 10, 8);
        assertPmrFromIdAndActualFromId("other", 1, 11, 11);
        assertPmrFromIdAndActualFromId("up", 1, 6, 6);
        assertPmrFromIdAndActualFromId("up", 5, 6, 6);
    }

    @Test
    @RunIf(value = DatabaseChecker.class, arguments = {DatabaseChecker.H2})
    public void testMigration_230006_user_name_case_insensitivity() throws IOException, SQLException {
        dbFixture.copyDeltas();
        dbFixture.copyH2Db("with-usernames-in-different-cases.zip");
        h2Database = new H2Database(dbFixture.env());

        h2Database.startDatabase();
        h2Database.upgrade();

        matchUserAttributesForUsername("foo", "foo@foo.com", true, "foo");
        matchUserAttributesForUsername("bar", "baz@bar.com", true, "BAR");
        matchUserAttributesForUsername("baz", "baz@baz.com", false, "baz");
        matchUserAttributesForUsername("quux", "quux@quux.com", true, "quux");
        assertThat(DatabaseFixture.query("select count(*) from users", h2Database), is(new Object[][]{{5l}}));
    }

    @Test
    @RunIf(value = DatabaseChecker.class, arguments = {DatabaseChecker.H2})
    public void testMigration_230001_should_create_column_if_not_exist() throws Exception {
        dbFixture.copyDeltas();
        dbFixture.copyH2Db("with-usernames-in-different-cases.zip");
        h2Database = new H2Database(dbFixture.env());

        h2Database.startDatabase();

        assertThat(DatabaseFixture.query("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE COLUMN_NAME='ARTIFACTSDELETED' AND TABLE_NAME='STAGES' AND TABLE_SCHEMA='PUBLIC'", h2Database),
                is(new Object[][]{{0L}}));

        h2Database.upgrade();

        assertThat(DatabaseFixture.query("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE COLUMN_NAME='ARTIFACTSDELETED' AND TABLE_NAME='STAGES' AND TABLE_SCHEMA='PUBLIC'", h2Database),
                is(new Object[][]{{1L}}));
    }

    @Test
    @RunIf(value = DatabaseChecker.class, arguments = {DatabaseChecker.H2})
    public void testMigration_230001_should_not_create_column_if_exist() throws Exception {
        dbFixture.copyDeltas();
        dbFixture.copyH2Db("with-usernames-in-different-cases.zip");
        h2Database = new H2Database(dbFixture.env());

        h2Database.startDatabase();

        DatabaseFixture.update("ALTER TABLE STAGES ADD COLUMN `ARTIFACTSDELETED` Boolean DEFAULT FALSE NOT NULL", h2Database);
        assertThat(DatabaseFixture.query("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE COLUMN_NAME='ARTIFACTSDELETED' AND TABLE_NAME='STAGES' AND TABLE_SCHEMA='PUBLIC'", h2Database),
                is(new Object[][]{{1L}}));
        try {
            h2Database.upgrade();
        }catch (Exception e) {
            fail("should not throw up");
        }
    }

    @Test
    @RunIf(value = DatabaseChecker.class, arguments = {DatabaseChecker.H2})
    public void testMigration_1501001_should_rename_pipelineselections_unselected_pipelines_to_selections() throws Exception {
        dbFixture.copyDeltas();
        dbFixture.copyH2Db("with-usernames-in-different-cases.zip");
        h2Database = new H2Database(dbFixture.env());

        h2Database.startDatabase();
        h2Database.upgrade();

        doesNotHaveColumn("PIPELINESELECTIONS", "UNSELECTEDPIPELINES");
        hasColumn("PIPELINESELECTIONS", "SELECTIONS");
    }

    @Test
    @RunIf(value = DatabaseChecker.class, arguments = {DatabaseChecker.H2})
    public void testMigration_1501001_add_column_isblacklist_to_pipelineselections() throws Exception {
        dbFixture.copyDeltas();
        dbFixture.copyH2Db("with-usernames-in-different-cases.zip");
        h2Database = new H2Database(dbFixture.env());

        h2Database.startDatabase();

        doesNotHaveColumn("PIPELINESELECTIONS", "ISBLACKLIST");

        h2Database.upgrade();

        hasColumn("PIPELINESELECTIONS", "ISBLACKLIST");
        columnHasType("PIPELINESELECTIONS", "ISBLACKLIST", "BOOLEAN");
        columnHasDefault("PIPELINESELECTIONS", "ISBLACKLIST", "TRUE");
    }

    @Test
    @RunIf(value = DatabaseChecker.class, arguments = {DatabaseChecker.H2})
    public void testMigration_1501002_should_add_comment_column_to_pipelines() throws Exception {
        dbFixture.copyDeltas();
        dbFixture.copyH2Db("with-usernames-in-different-cases.zip");

        h2Database = new H2Database(dbFixture.env());
        h2Database.startDatabase();

        doesNotHaveColumn("PIPELINES", "COMMENT");

        h2Database.upgrade();

        hasColumn("PIPELINES", "COMMENT");
    }

    private void columnHasDefault(String table, String column, String defaultValue) {
        assertThat(DatabaseFixture.query(String.format("SELECT COLUMN_DEFAULT FROM information_schema.COLUMNS WHERE COLUMN_NAME='%s' AND TABLE_NAME='%s' AND TABLE_SCHEMA='PUBLIC'", column, table), h2Database),
                is(new Object[][]{{defaultValue}}));
    }

    private void columnHasType(String table, String column, String type) {
        assertThat(DatabaseFixture.query(String.format("SELECT TYPE_NAME FROM information_schema.COLUMNS WHERE COLUMN_NAME='%s' AND TABLE_NAME='%s' AND TABLE_SCHEMA='PUBLIC'", column, table), h2Database),
                is(new Object[][]{{type}}));
    }

    private void hasColumn(String table, String column) {
        assertThat(DatabaseFixture.query(String.format("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE COLUMN_NAME='%s' AND TABLE_NAME='%s' AND TABLE_SCHEMA='PUBLIC'", column, table), h2Database),
                is(new Object[][]{{1L}}));
    }

    private void doesNotHaveColumn(String table, String column) {
        assertThat(DatabaseFixture.query(String.format("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE COLUMN_NAME='%s' AND TABLE_NAME='%s' AND TABLE_SCHEMA='PUBLIC'", column, table), h2Database),
                is(new Object[][]{{0L}}));
    }

    private void matchUserAttributesForUsername(final String username, final String email, final boolean enabled, final String actualName) {
        assertThat(DatabaseFixture.query("select name, email, enabled from users where name = '" + username + "'", h2Database), is(new Object[][]{{actualName, email, enabled}}));
    }

    private void assertPmrFromIdAndActualFromId(final String pipelineName, final int counter, final long fromId, final long actualFromId) {
        assertThat(DatabaseFixture.query(pmrFor(pipelineName, counter), h2Database), is(new Object[][]{{fromId, actualFromId}}));
    }

    private String pmrFor(final String pipelineName, final int counter) {
        return "SELECT fromRevisionId, actualFromRevisionId "
                + " FROM pipelineMaterialRevisions pmr "
                + " INNER JOIN pipelines p ON p.id = pmr.pipelineId "
                + " WHERE p.name = '" + pipelineName + "' AND p.counter = " + counter;
    }
}
