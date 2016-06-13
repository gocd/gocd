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

package com.thoughtworks.go.server.sqlmigration;

import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * @understands create ARTIFACTSDELETED column in STAGES if not exist
 */
public class Migration_230001 {

    public static void createColumnIfNotExist(Connection conn) throws SQLException {
        ResultSetMetaData metaData = conn.createStatement().executeQuery("SELECT * FROM STAGES").getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String columnName = metaData.getColumnName(i);
            if (columnName.toLowerCase().equals("artifactsdeleted")) {
                return;
            }

        }
        conn.createStatement().executeUpdate("ALTER TABLE STAGES ADD COLUMN `ARTIFACTSDELETED` Boolean DEFAULT FALSE NOT NULL");
    }
}
