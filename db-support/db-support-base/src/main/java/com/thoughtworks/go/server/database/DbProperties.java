/*
 * Copyright Thoughtworks, Inc.
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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Setter
@Accessors(fluent = true, chain = true)
@NoArgsConstructor
public class DbProperties {
    private static final String DB_EXTRA_BACKUP_ENV_PREFIX = "db.extraBackupEnv.";
    private static final String DB_CONNECTION_PROPERTIES_PREFIX = "db.connectionProperties.";

    @NonNull private String user;
    @NonNull private String url;
    @NonNull private String driver;
    private int maxIdle;
    private int maxTotal;
    @NonNull private String password;
    @NonNull private String extraBackupCommandArgs;
    private Map<String, String> extraBackupEnv;
    private Properties connectionProperties;

    public DbProperties initializeFrom(Properties properties, Function<String, String> decrypter) {
        this.url = properties.getProperty("db.url", "");
        this.user = properties.getProperty("db.user", "");
        this.driver = properties.getProperty("db.driver");
        this.maxIdle = Integer.parseInt(properties.getProperty("db.maxIdle", "32"));
        this.maxTotal = Integer.parseInt(properties.getProperty("db.maxActive", "32"));
        this.password = findPassword(properties, decrypter);
        this.extraBackupCommandArgs = properties.getProperty("db.extraBackupCommandArgs", "");

        this.connectionProperties = new Properties();
        this.extraBackupEnv = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(DB_EXTRA_BACKUP_ENV_PREFIX)) {
                extraBackupEnv.put(key.replace(DB_EXTRA_BACKUP_ENV_PREFIX, ""), properties.getProperty(key));
            } else if (key.startsWith(DB_CONNECTION_PROPERTIES_PREFIX)) {
                connectionProperties.put(key.replace(DB_CONNECTION_PROPERTIES_PREFIX, ""), properties.getProperty(key));
            }
        }
        if (isPostgres(this.url)) {
            connectionProperties.put("stringtype", "unspecified");
        }

        return this;
    }

    private boolean isPostgres(String url) {
        return url != null && url.startsWith("jdbc:postgresql:");
    }

    private String findPassword(Properties properties, Function<String, String> decrypter) {
        String password = properties.getProperty("db.password", "");
        String encryptedPassword = properties.getProperty("db.encryptedPassword", "");

        if (!encryptedPassword.isBlank()) {
            return decrypter.apply(encryptedPassword);
        } else {
            return password;
        }
    }

    public String connectionPropertiesAsString() {
        return connectionProperties.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(";"));
    }
}
