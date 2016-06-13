/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.database;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;

import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static org.apache.commons.io.FileUtils.deleteDirectory;

public class MigrateHsqldbToH2 implements Migration {
    public static final String BACKUP_ALREADY_EXISTS =
            "Cannot upgrade database from hsql. A backup database already directoryExists.";

    Pattern createTable = Pattern.compile("CREATE (.*) TABLE (.*?)\\(", Pattern.COMMENTS);

    private SystemEnvironment env;
    private static final String BACKUP_FILE_NAME_DATE_PATTERN = "yyyy-MM-dd-hhmm";
    public static final int LINES_PER_DOT = 2000;
    private BasicDataSource source;
    private static final Logger LOGGER = Logger.getLogger(MigrateHsqldbToH2.class);


    public MigrateHsqldbToH2(BasicDataSource source, SystemEnvironment env) {
        this.source = source;
        this.env = env;
    }

    public void migrate() {
        runScript();
    }

    private void runScript() {
        File oldHsqlScript = new File(oldHsql(), "cruise.script");
        if (oldHsql().exists()) {
            LOGGER.info(String.format("Found database at %s to be hsql. Migrating it to h2db.", oldHsqlScript.getAbsolutePath()));
            try {
                backupOldDb(dbDirectory(), oldHsql());
                if (oldHsqlScript.exists()) {
                    backupNewTemplateDb(dbDirectory(), newDb());
                    replayScript(new File(oldHsql(), "cruise.script"));
                    replayScript(new File(oldHsql(), "cruise.log"));
                }
                deleteDirectory(oldHsql());
                LOGGER.info("Finished Migrating from hsqldb to h2db");
            } catch (IOException e) {
                bomb("Could not migrate old hsqldb to h2. IOException occured.", e);
            } catch (SQLException e) {
                bomb("Could not migrate old hsqldb to h2. SQL error occurred.", e);
            }
        }
    }

    private File newDb() {
        return new File(dbDirectory(), "h2db");
    }

    private File oldHsql() {
        return new File(dbDirectory(), "hsqldb");
    }

    private File dbDirectory() {
        return env.getDbPath().getParentFile();
    }

    private void backupNewTemplateDb(File dbDirectory, File newDb) throws IOException {
        File backup = new File(dbDirectory, "h2db-template-backup-" + dateString() + ".zip");
        new ZipUtil().zip(newDb, backup, Deflater.DEFAULT_COMPRESSION);
        deleteDirectory(newDb);
        if (newDb.exists()) { bomb("Database " + newDb + " could not be deleted."); }
    }

    private void backupOldDb(File dbDirectory, File oldHsql) throws IOException {
        File backupFile = new File(dbDirectory, "hsqldb-upgrade-backup-" + dateString() + ".zip");
        if (backupFile.exists()) {
            bomb(BACKUP_ALREADY_EXISTS);
        }
        new ZipUtil().zip(oldHsql, backupFile, Deflater.DEFAULT_COMPRESSION);
    }

    private String dateString() {
        return new SimpleDateFormat(BACKUP_FILE_NAME_DATE_PATTERN).format(new Date());
    }

    private void replayScript(File scriptFile) throws SQLException, IOException {
        if (!scriptFile.exists()) { return; }

        System.out.println("Migrating hsql file: " + scriptFile.getName());
        Connection con = source.getConnection();
        Statement stmt = con.createStatement();
        stmt.executeUpdate("SET REFERENTIAL_INTEGRITY FALSE");
        LineNumberReader reader = new LineNumberReader(new FileReader(scriptFile));
        String line;
        while ((line = reader.readLine()) != null) {
            try {
                String table = null;
                Matcher matcher = createTable.matcher(line);
                if (matcher.find()) {
                    table = matcher.group(2).trim();
                }

                if (line.equals("CREATE SCHEMA PUBLIC AUTHORIZATION DBA")) { continue; }
                if (line.equals("CREATE SCHEMA CRUISE AUTHORIZATION DBA")) { continue; }
                if (line.startsWith("CREATE USER SA PASSWORD")) { continue; }
                if (line.contains("BUILDEVENT VARCHAR(255)")) {
                    line = line.replace("BUILDEVENT VARCHAR(255)", "BUILDEVENT LONGVARCHAR");
                }
                if (line.contains("COMMENT VARCHAR(4000)")) {
                    line = line.replace("COMMENT VARCHAR(4000)", "COMMENT LONGVARCHAR");
                }
                if (line.contains("CREATE MEMORY TABLE")) {
                    line = line.replace("CREATE MEMORY TABLE", "CREATE CACHED TABLE");
                }
                if (table != null && table.equals("MATERIALPROPERTIES")
                        && line.contains("VALUE VARCHAR(255),")) {
                    line = line.replace("VALUE VARCHAR(255),", "VALUE LONGVARCHAR,");
                }
                if (line.startsWith("GRANT DBA TO SA")) { continue; }
                if (line.startsWith("CONNECT USER")) { continue; }
                if (line.contains("DISCONNECT")) { continue; }
                if (line.contains("AUTOCOMMIT")) { continue; }
                stmt.executeUpdate(line);
                if (reader.getLineNumber() % LINES_PER_DOT == 0) {
                    System.out.print(".");
                    System.out.flush();
                }
                if (reader.getLineNumber() % (80 * LINES_PER_DOT) == 0) {
                    System.out.println();
                }

            } catch (SQLException e) {
                bomb("Error executing : " + line, e);
            }
        }
        stmt.executeUpdate("SET REFERENTIAL_INTEGRITY TRUE");
        stmt.executeUpdate("CHECKPOINT SYNC");
        System.out.println("\nDone.");
        reader.close();
        stmt.close();
        con.close();
    }


}
