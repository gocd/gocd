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
import net.sf.dbdeploy.InMemory;
import net.sf.dbdeploy.database.syntax.HsqlDbmsSyntax;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ExceptionUtils.bombUnless;

public class DbDeployMigration implements Migration {
    private static final Logger LOG = LoggerFactory.getLogger(DbDeployMigration.class);

    private final BasicDataSource dataSource;
    private SystemEnvironment env;

    public DbDeployMigration(BasicDataSource dataSource, SystemEnvironment env) {
        this.dataSource = dataSource;
        this.env = env;
    }

    @Override
    public void migrate() {
        upgradeWithDbDeploy();
    }

    private void upgradeWithDbDeploy() {
        LOG.info("Upgrading database at {}. This might take a while depending on the size of the database.", dataSource);

        List<String> messages = Arrays.asList(
                StringUtils.repeat("*", "", 72),
                "WARNING: Shutting down your server at this point will lead to a database corruption. Please wait until the database upgrade completes.",
                StringUtils.repeat("*", "", 72)
        );
        for (String message : messages) {
            System.err.println(message);
            LOG.info(message);
        }

        File upgradePath = env.getDBDeltasPath();
        bombUnless(upgradePath.exists(), "Database upgrade scripts do not exist in directory " + upgradePath.getAbsolutePath());
        InMemory dbDeploy = new InMemory(dataSource, new HsqlDbmsSyntax(), upgradePath, "DDL");
        try {
            String migrationSql = dbDeploy.migrationSql();
            new JdbcTemplate(dataSource).execute(migrationSql);

            System.err.println("Database upgrade completed successfully.");
            LOG.info("Database upgrade completed.");

        } catch (Exception e) {
            String message = "Unable to create database upgrade script for database " + dataSource.getUrl() + ". The problem was: " + e.getMessage();
            if (e.getCause() != null) {
                message += ". The cause was: " + e.getCause().getMessage();
            }
            LOG.error(message, e);
            System.err.println(message);
            e.printStackTrace(System.err);
            throw bomb(message, e);
        }
    }


}
