/*
 * Copyright 2021 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.database.migration;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.repeat;

@Slf4j
public class DbDeploySchemaVerifier {

    public void verify(Connection connection, String configDir) throws SQLException {
        log.debug("Checking if DB contains the changelog table from dbdeploy.");

        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet result = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (result.next()) {
                String tableName = result.getString("TABLE_NAME");
                if (tableName.equalsIgnoreCase("changelog")) {
                    String msg = "It appears that you are upgrading your GoCD server and the current database is not compatible with GoCD 20.5.0 and above." +
                            "Please see https://docs.gocd.org/20.5.0/installation/upgrade_to_gocd_20.5.0.html for instructions on upgrading this instance of GoCD.";
                    throwFormattedError(msg);
                    return;
                }
            }
        }

        String postgresPropertiesFile = "postgresqldb.properties";
        log.debug("Checking if config directory contains 'postgresqldb.properties' files.");

        if (new File(configDir, postgresPropertiesFile).exists()) {
            String msg = "It appears that your old PostgreSQL database addon configurations exists at 'postgresqldb.properties'. " +
                    "Please see https://docs.gocd.org/20.5.0/installation/upgrade_to_gocd_20.5.0.html for instructions on upgrading this instance of GoCD. " +
                    "Once you've upgraded, please remove the 'postgresqldb.properties' file and restart GoCD.";

            throwFormattedError(msg);
        }

    }

    private void throwFormattedError(String msg) {
        List<String> messages = Arrays.asList(repeat("*", "", 72), msg, repeat("*", "", 72));

        for (String message : messages) {
            System.err.println(message);
            log.info(message);
        }

        throw new RuntimeException(String.join("\n", messages));
    }
}
