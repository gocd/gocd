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

import com.thoughtworks.go.server.database.h2.DefaultH2DataSource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbcp2.BasicDataSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Function;

@Slf4j
public class ConnectionManager {
    @Getter(lazy = true)
    private final BasicDataSource dataSourceInstance = createDataSource();

    @Getter(lazy = true)
    private final BackupProcessor backupProcessor = createBackupProcessor();

    @Getter(lazy = true)
    private final QueryExtensions queryExtensions = createQueryExtensions();

    @Getter(lazy = true)
    private final DbProperties dbProperties = createDbProperties();

    private final Properties systemProperties;
    private final File configDir;
    private final Function<String, String> decrypter;

    public ConnectionManager(Properties systemProperties, File configDir, Function<String, String> decrypter) {
        this.systemProperties = systemProperties;
        this.configDir = configDir;
        this.decrypter = decrypter;
    }

    public void backup(File targetDir) {
        getBackupProcessor().backup(targetDir, getDataSourceInstance(), getDbProperties());
    }

    private BasicDataSource createDataSource() {
        final DbProperties dbProperties = getDbProperties();
        BasicDataSource basicDataSource = new BasicDataSource();

        if (dbProperties.url().isBlank()) {
            return DefaultH2DataSource.defaultH2DataSource(basicDataSource, dbProperties);
        }

        basicDataSource.setDriverClassName(dbProperties.driver());
        basicDataSource.setUrl(dbProperties.url());
        basicDataSource.setUsername(dbProperties.user());
        basicDataSource.setPassword(dbProperties.password());

        basicDataSource.setMaxTotal(dbProperties.maxTotal());
        basicDataSource.setMaxIdle(dbProperties.maxIdle());
        basicDataSource.setConnectionProperties(dbProperties.connectionPropertiesAsString());
        return basicDataSource;
    }

    private BackupProcessor createBackupProcessor() {
        log.debug("Loading backup processor");
        for (BackupProcessor backupProcessor : ServiceLoader.load(BackupProcessor.class)) {
            log.debug("Checking if {} supports database URL {}", backupProcessor, getDbProperties().url());
            if (backupProcessor.accepts(getDbProperties().url())) {
                log.info("Done loading backup processor, found {}", backupProcessor);
                return backupProcessor;
            }
        }
        throw new RuntimeException("A backup processor was not found for the specified database URL.");
    }

    private QueryExtensions createQueryExtensions() {
        log.debug("Loading query extensions");
        for (QueryExtensions queryExtensions : ServiceLoader.load(QueryExtensions.class)) {
            log.debug("Checking if {} supports database URL {}", queryExtensions, getDbProperties().url());
            if (queryExtensions.accepts(getDbProperties().url())) {
                log.info("Done loading query extensions, found {}", queryExtensions);
                return queryExtensions;
            }
        }
        throw new RuntimeException("A query extension was not found for the specified database URL.");
    }

    private DbProperties createDbProperties() {
        Properties propertiesFromConfigFile = new Properties();
        propertiesFromConfigFile.putAll(systemProperties);

        File configFile = dbConfigFile(propertiesFromConfigFile, configDir);
        if (configFile != null && configFile.exists()) {
            log.info("Loading database config from file {}", configFile);
            try (FileInputStream is = new FileInputStream(configFile)) {
                propertiesFromConfigFile.load(is);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            log.warn("The file {} specified by `go.db.config` does not exist.", dbConfigFile(systemProperties, configDir));
        }
        return new DbProperties().initializeFrom(propertiesFromConfigFile, decrypter);
    }

    private static File dbConfigFile(Properties properties, File configDir) {
        if (dbConfigFilePath(properties).isBlank()) {
            return null;
        }
        return new File(configDir, dbConfigFilePath(properties));
    }

    private static String dbConfigFilePath(Properties properties) {
        return properties.getProperty("go.db.config", "db.properties");
    }
}
