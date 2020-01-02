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

import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class DatabaseFixture {
    private File tmpDb;

    //This is what h2 reports for a text column
    static final String LONGVARCHAR = "VARCHAR(2147483647)";
    private File templatesDir;

    public DatabaseFixture() {
        this.tmpDb = TestFileUtil.createTempFolder("h2database" + System.currentTimeMillis());
        this.templatesDir = new File("db", "dbtemplate");
    }

    public SystemEnvironment env() throws IOException {
        return new SystemEnvironment(new File(tmpDb, "h2db").getCanonicalPath(), "9845");
    }

    public void copyH2Db() {
        copyDb(new File(templatesDir(), "h2db"));
    }

    public void copyDb(File dir) {
        try {
            if (tmpDb.exists()) {
                deleteDirectory(tmpDb);
                tmpDb.mkdir();
            }
            copyDirectory(dir, new File(tmpDb, "h2db"));
            copyDeltas();
        } catch (IOException e) {
            throw new RuntimeException("Could not set up database for tests", e);
        }
    }

    public void copyDeltas() throws IOException {
        copyDirectory(new File(migrationsDir(), "h2deltas"), new File(tmpDb, "h2deltas"));
    }

    private File migrationsDir() {
        return new File("db/migrate");
    }

    public void tearDown() {
        try {
            deleteDirectory(tmpDb);
        } catch (Exception e) {
            //ignore
        }
    }

    public File templatesDir() {
        return templatesDir;
    }

    public void copyH2Db(String oldDb) throws IOException {
        File h2DbDir = new File(tmpDb, "h2db");
        if (h2DbDir.exists()) {
            h2DbDir.delete();
            h2DbDir.mkdirs();
        }
        new ZipUtil().unzip(new File(templatesDir(), oldDb), h2DbDir);
    }

    public static void assertColumnType(BasicDataSource dataSource, String tableName, String columnName,
                                        String expected) {
        try (
                Connection connection = dataSource.getConnection();
                ResultSet set = connection.getMetaData().getColumns(null, null, null, null)
        ) {
            while (set.next()) {
                if (set.getString("TABLE_NAME").equalsIgnoreCase(tableName) &&
                        set.getString("COLUMN_NAME").equalsIgnoreCase(columnName)) {
                    String typeName = set.getString("TYPE_NAME");
                    int typeWidth = set.getInt("COLUMN_SIZE");
                    String type = typeName + "(" + typeWidth + ")";
                    assertThat("Expected " + columnName + " to be " + expected + " type but was " + type,
                            type, is(expected));
                    return;
                }
            }
            Assert.fail("Column " + columnName + " does not exist");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static int numericQuery(String query, H2Database h2Database) {
        return ((Long) query(query, h2Database)[0][0]).intValue();
    }

    public static Object[][] query(String query, H2Database h2Database) {
        BasicDataSource source = h2Database.createDataSource();
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            con = source.getConnection();
            stmt = con.createStatement();
            rs = stmt.executeQuery(query);
            int columnCount = rs.getMetaData().getColumnCount();
            List<Object[]> objects = new ArrayList<>();
            while (rs.next()) {
                Object[] values = new Object[columnCount];
                for (int i = 0; i < values.length; i++) {
                    values[i] = rs.getObject(i+1);
                }
                objects.add(values);
            }
            return objects.toArray(new Object[0][0]);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                assert stmt != null;
                stmt.close();
                con.close();
                assert rs != null;
                rs.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static int update(String query, H2Database h2Database) {
        BasicDataSource source = h2Database.createDataSource();
        Connection con = null;
        Statement stmt = null;
        try {
            con = source.getConnection();
            stmt = con.createStatement();
            return stmt.executeUpdate(query);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                assert stmt != null;
                stmt.close();
                con.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
