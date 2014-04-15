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

package com.thoughtworks.go.server.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.util.FileUtil;

public class MergeDatabase {
    private static final int ID_OFFSET = 10000000;
    private Connection mergedConn;
    private Connection fromConn;
    private Map<Long, Long> usernameIdMap = new HashMap<Long, Long>();
    private Map<String, Long> existingUsernames = new HashMap<String, Long>();
    private PrintWriter progress;
    private static final String PROGRESS_FILE = "/home/cruise/import-progress.txt";

    public static void main(String[] args) throws Exception {
//        H2Database h2 = new H2Database(new SystemEnvironment("/home/cruise/projects/cruise-dbs/merged/db/h2db", "1213"));
//        h2.startDatabase();
//        h2.upgrade();
//
//        H2Database h2 = new H2Database(new SystemEnvironment("/home/cruise/projects/cruise-dbs/mingle/db/h2db", "1213"));
//        h2.startDatabase();
//        h2.upgrade();

        new MergeDatabase().run();
    }

    public MergeDatabase() throws Exception {
        mergedConn = DriverManager.getConnection("jdbc:h2:/home/cruise/projects/cruise-dbs/merged/db/h2db/cruise", "sa", null);
        printScalar(mergedConn, "SELECT COUNT(*) FROM pipelines");
        printScalar(mergedConn, "SELECT COUNT(*) FROM materials");
        printScalar(mergedConn, "SELECT MAX(id) FROM buildStateTransitions");

        fromConn = DriverManager.getConnection("jdbc:h2:/home/cruise/projects/cruise-dbs/mingle/db/h2db/cruise", "sa", null);
        printScalar(fromConn, "SELECT MAX(id) FROM buildStateTransitions");
    }

    private void run() throws Exception {
        try {
//            print(fromConn.getMetaData().getTables("CRUISE", "PUBLIC", null, null));
            loadUsers();
            importTables();
        } finally {
            closeQuietly(mergedConn);
            closeQuietly(fromConn);
            if (progress != null)
                progress.close();   
        }

    }

    private void loadUsers() throws SQLException {
        ResultSet rs = mergedConn.createStatement().executeQuery("SELECT * FROM users");
        while (rs.next()) {
            existingUsernames.put(rs.getString("name"), rs.getLong("id"));
        }
        rs.close();

        rs = fromConn.createStatement().executeQuery("SELECT * FROM users");
        while (rs.next()) {
            long id = rs.getLong("id");
            String name = rs.getString("name");
            Long newId = id;
            if (existingUsernames.containsKey(name)) {
                newId = existingUsernames.get(name);
            } else {
                newId += ID_OFFSET;
                existingUsernames.put(name, id);
            }
            usernameIdMap.put(id, newId);
        }
        rs.close();
    }

    private void importTables() throws Exception {
//        mergedConn.setAutoCommit(false);
        List<String> tables = tablesToImport();
        for (int i = 0; i < tables.size(); i++) {
            String table = tables.get(i);
            importTable(i, table);
            tableImported(table);
        }
    }

    private void tableImported(String table) throws SQLException {
//        mergedConn.commit();
        progress.println(table);
        progress.flush();
    }

    private void importTable(int i, String table) throws Exception {
        System.out.println("table = " + table);
        long offset = 0; //table.equalsIgnoreCase("resources") ? 410000 : 0;
        while (true) {
            System.out.println("  offset = " + offset);
            long count = importPage(table, offset);
            if (count == 0) {
                break;
            }
            offset += count;
        }
    }

    private long importPage(String table, long offset) throws Exception {
        ResultSet rs = fromConn.createStatement().executeQuery("SELECT * FROM " + table + " LIMIT 10000 OFFSET " + offset);
        ResultSetMetaData metaData = rs.getMetaData();
        long importedCount = 0;
        while(rs.next()) {
            insertRow(table, rs, metaData);
            importedCount++;
        }
        rs.close();
        return importedCount;
    }

    private void insertRow(String table, ResultSet rs, ResultSetMetaData metaData) throws Exception {
        String insertSql = "INSERT INTO " + table + " VALUES (";
        for (int j = 1; j <= metaData.getColumnCount(); j++) {
            insertSql = insertSql + "?";
            if (j < metaData.getColumnCount()) {
                insertSql = insertSql + ", ";
            }
        }
        insertSql = insertSql + ")";
        PreparedStatement ps = mergedConn.prepareStatement(insertSql);

        try {
            for (int j = 1; j <= metaData.getColumnCount(); j++) {
                String colName = metaData.getColumnName(j);
                int colType = metaData.getColumnType(j);
                String colTypeName = metaData.getColumnTypeName(j);
                Object value = rs.getObject(j);
                if (value == null) {
                    ps.setNull(j, Types.VARCHAR);
                } else if (colType == Types.BINARY) {
                    ps.setNull(j, Types.BINARY);
                } else {
                    if (colName.equalsIgnoreCase("userId")) {
                        value = usernameIdMap.get(rs.getLong(j));
                    } else if (colTypeName.equalsIgnoreCase("CLOB")) {
                        BufferedReader reader = new BufferedReader(rs.getCharacterStream(j));
                        String line;
                        StringBuilder content = new StringBuilder();
                        while ((line = reader.readLine()) != null) {
                            content.append(line);
                        }
                        value = content.toString();
                    } else if (colTypeName.contains("CHAR")) {
                        String s = (String) value;
                        value = s.substring(0, Math.min(1000, s.length()));
                    } else if (colName.endsWith("ID") && !colName.endsWith("UUID") && !colName.equals("ORDERID")) {
                        Long id = rs.getLong(j);
                        id += ID_OFFSET;
                        value = id;
                    }
                    ps.setString(j, value.toString());
                }
            }
            if (!skipRow(rs, table)) {
                int count = ps.executeUpdate();
                if (count != 1) {
                    throw new RuntimeException("update count should be 1, got: " + count);
                }
            }
        } catch (Exception e) {
            printRow(rs);
            if (!ignoreErrors(rs, table));
                throw e;
        }
    }

    private boolean ignoreErrors(ResultSet rs, String table) {
        return table.equalsIgnoreCase("resources");
    }

    private boolean skipRow(ResultSet rs, String table) throws SQLException {
        return (table.equalsIgnoreCase("users") && userWithSamenameExists(rs.getString("name")));
    }

    private boolean userWithSamenameExists(String name) {
        return existingUsernames.containsKey(name);
    }

    private List<String> tablesToImport() throws IOException {
        List<String> tables = new ArrayList<String>();
        tables.add("pipelines");
        tables.add("materials");
        tables.add("modifications");
        tables.add("pipelineMaterialRevisions");
        tables.add("modifiedFiles");
        tables.add("users");
        //tables.add("notificationfilters");
        tables.add("stages");
        //tables.add("buildCauseBuffer");
        //tables.add("pipelineLabelCounts");
        tables.add("builds");
        tables.add("resources");
        tables.add("buildStateTransitions");
        tables.add("properties");
        tables.add("artifactPlans");
        tables.add("artifactPropertiesGenerator");
        tables.add("environmentVariables");

        if (new File(PROGRESS_FILE).exists()) {
            List<String> done = FileUtil.readLines(new FileInputStream(PROGRESS_FILE));
            for (int i = 0; i < done.size(); i++) {
                String s = done.get(i);
                System.out.println("skipping table: " + s);
                tables.remove(s);
            }
        }
        progress = new PrintWriter(new FileWriter(PROGRESS_FILE, true));
        return tables;
    }

    private void print(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            System.out.print(metaData.getColumnName(i) + "\t");
        }
        System.out.println("");
        
        while(rs.next()) {
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                System.out.print(rs.getObject(i) + "\t");
            }
            System.out.println("");
        }
    }

    private void printRow(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            System.out.print(metaData.getColumnName(i) + "\t");
        }
        System.out.println("");

        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            System.out.print(rs.getObject(i) + "\t");
        }
        System.out.println("");
    }

    private void printScalar(Connection to, String sql) throws SQLException {
        ResultSet rs = to.createStatement().executeQuery(sql);
        while(rs.next()) {
            System.out.println(rs.getLong(1));
        }
    }

    private void closeQuietly(Connection to) {
        if (to != null) {
            try {
                to.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
