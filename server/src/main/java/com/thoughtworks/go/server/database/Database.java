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

import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.database.migration.DatabaseMigrator;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

@Component
public class Database {

    private final ConnectionManager connectionManager;

    @Autowired
    public Database(SystemEnvironment systemEnvironment) {
        this.connectionManager = new ConnectionManager(System.getProperties(), systemEnvironment.configDir(), decryptionFunction());
    }

    private static Function<String, String> decryptionFunction() {
        return cipherText -> {
            try {
                return new GoCipher().decrypt(cipherText);
            } catch (CryptoException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Bean(name = "goDataSource")
    public BasicDataSource getDataSource() throws SQLException {
        BasicDataSource dataSource = connectionManager.getDataSourceInstance();
        try (Connection connection = dataSource.getConnection()) {
            new DatabaseMigrator().migrate(connection);
        }

        return dataSource;
    }

    public void backup(File targetDir) {
        try {
            connectionManager.backup(targetDir);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public QueryExtensions getQueryExtensions() {
        return connectionManager.getQueryExtensions();
    }
}
