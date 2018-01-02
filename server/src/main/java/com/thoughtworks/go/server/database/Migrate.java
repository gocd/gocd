/*
 * Copyright 2017 ThoughtWorks, Inc.
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

/*
 * Copyright 2004-2010 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */

package com.thoughtworks.go.server.database;

import org.apache.commons.io.FileUtils;
import org.h2.tools.RunScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Migrate a H2 database version 1.1.x (page store not enabled) to 1.2.x (page
 * store format). This will download the H2 jar file version 1.2.127 from
 * maven.org if it doesn't exist, execute the Script tool (using Runtime.exec)
 * to create a backup.sql script, rename the old database file to *.backup,
 * created a new database (using the H2 jar file in the class path) using the
 * Script tool, and then delete the backup.sql file. Most utility methods are
 * copied from h2/src/tools/org/h2/build/BuildBase.java.
 */
public class Migrate {

    public static final String USER = "sa";
    public static final String PASSWORD = "";
    private static final File OLD_H2_FILE = new File("./historical_jars/h2-1.2.127.jar");
    private static final String TEMP_SCRIPT = "backup.sql";
    private static final String MAX_MEMORY_FOR_MIGRATION = "256m";
    private PrintStream sysOut = System.out;
    private boolean quiet;

    private static final Logger LOGGER = LoggerFactory.getLogger(Migrate.class);

    /**
     * Migrate databases. The user name and password are both "sa".
     *
     * @param args the path (default is the current directory)
     * @throws Exception if conversion fails
     */
    public static void main(String... args) throws Exception {
        new Migrate().execute(new File(args.length == 1 ? args[0] : "."), true, USER, PASSWORD, false);
    }

    /**
     * Migrate a database.
     *
     * @param file      the database file (must end with .data.db) or directory
     * @param recursive if the file parameter is in fact a directory (in which
     *                  case the directory is scanned recursively)
     * @param user      the user name of the database
     * @param password  the password
     * @param runQuiet  to run in quiet mode
     * @throws Exception if conversion fails
     */
    public void execute(File file, boolean recursive, String user, String password, boolean runQuiet) throws Exception {
        String pathToJavaExe = getJavaExecutablePath();
        this.quiet = runQuiet;
        if (file.isDirectory() && recursive) {
            for (File f : file.listFiles()) {
                execute(f, recursive, user, password, runQuiet);
            }
            return;
        }
        if (!file.getName().endsWith(".data.db")) {
            return;
        }
        LOGGER.info("Migrating the database at {} to the new format. This might take a while based on the size of your database.", file.getAbsolutePath());
        String fileNameWithoutExtension = truncateFileExtension(file.getAbsolutePath());
        File newDatabaseFile = new File(fileNameWithoutExtension + ".h2.db");
        if (newDatabaseFile.exists()) {
            LOGGER.info("Removing {} [the new database file]", newDatabaseFile.getAbsolutePath());
            FileUtils.deleteQuietly(newDatabaseFile);
        }
        if (!OLD_H2_FILE.exists()) {
            throw new IllegalStateException(String.format("h2 file %s not found, migration could not be completed successfully", OLD_H2_FILE.getAbsolutePath()));
        }
        String url = "jdbc:h2:" + fileNameWithoutExtension;
        exec(new String[]{
                pathToJavaExe,
                "-Xmx" + MAX_MEMORY_FOR_MIGRATION,
                "-cp", OLD_H2_FILE.getAbsolutePath(),
                "org.h2.tools.Script",
                "-script", TEMP_SCRIPT,
                "-url", url,
                "-user", user
        });
        file.renameTo(new File(file.getAbsoluteFile() + ".backup"));
        RunScript.execute(url, user, password, TEMP_SCRIPT, "UTF-8", true);
        new File(TEMP_SCRIPT).delete();
        LOGGER.info("Migrating the h2db to the new format is complete.");
    }

    private String truncateFileExtension(String url) {
        return url.substring(0, url.length() - ".data.db".length());
    }

    private String getJavaExecutablePath() {
        String pathToJava;
        if (File.separator.equals("\\")) {
            pathToJava = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java.exe";
        } else {
            pathToJava = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        }
        if (!new File(pathToJava).exists()) {
            // Fallback to old behaviour
            pathToJava = "java";
        }
        return pathToJava;
    }

    private int exec(String[] command) {
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("About to execute commands:");
                for (String c : command) {
                    LOGGER.debug(c);
                }
            }
            Process p = Runtime.getRuntime().exec(command);
            copyInThread(p.getInputStream(), quiet ? null : sysOut);
            copyInThread(p.getErrorStream(), quiet ? null : sysOut);
            p.waitFor();
            return p.exitValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void copyInThread(final InputStream in, final OutputStream out) {
        new Thread() {
            public void run() {
                try {
                    while (true) {
                        int x = in.read();
                        if (x < 0) {
                            return;
                        }
                        if (out != null) {
                            out.write(x);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();
    }

}
