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

package com.thoughtworks.go.util;

import javax.sql.DataSource;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.*;

/**
 * Utilities class providing some convenience methods for debugging DB queries
 */
public class TestQueryUtil {
    public static void logQueryResultToFile(DataSource dataSource, String path, String sqlStatement, Object... boundVariables) {
        try {
            Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sqlStatement);

            bindValues(statement, boundVariables);

            writeResultSetToFile(path, statement.executeQuery());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void writeResultSetToFile(String path, ResultSet results) throws SQLException {
        ResultSetMetaData metaData = results.getMetaData();

        int columnCount = metaData.getColumnCount();

        try {
            BufferedWriter bw = Files.newBufferedWriter(Paths.get(path), StandardOpenOption.CREATE);

            bw.write("**************** SQL DEBUG ****************\n");
            bw.write("*******************************************\n");
            bw.write("*******************************************\n");
            while (results.next()) {
                StringBuilder row = new StringBuilder();
                for (int i = 1; i <= columnCount; i++) {
                    if (i > 1) row.append(", ");
                    String columnValue = results.getString(i);
                    row.append(String.format("%s: %s", metaData.getColumnName(i), columnValue));
                }
                bw.write(row.toString() + "\n");
            }
            bw.write("*******************************************\n");
            bw.write("*******************************************\n");
            bw.write("*******************************************\n");
            bw.write("\n");
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static PreparedStatement bindValues(PreparedStatement statement, Object[] bindings) throws SQLException {
        int idx = 1;
        for (Object binding : bindings) {
            statement.setObject(idx, binding);
            idx++;
        }

        return statement;
    }
}
