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

package com.thoughtworks.go.server.database.mysql;

import com.thoughtworks.go.server.database.migration.AbstractMigratorIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@EnabledOnOs({OS.LINUX, OS.MAC})
class DatabaseMigratorMySQLTest extends AbstractMigratorIntegrationTest {
    @Container
    private final JdbcDatabaseContainer mySQLContainer = (JdbcDatabaseContainer) new MySQLContainer("mysql:8")
            .withUsername("root")
            .withPassword("")
            .withCommand("--lower-case-table-names=1");

    @Test
    void shouldMigrate() throws Exception {
        migrate(mySQLContainer, "show tables;", "root", "");
    }

}
