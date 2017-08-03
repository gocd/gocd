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

package com.thoughtworks.go.server.database;

import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class CreateChangeLogTableForBlankDb {
    private static final Logger LOG = Logger.getLogger(CreateChangeLogTableForBlankDb.class);

    private final BasicDataSource dataSource;
    private SystemEnvironment env;

    public CreateChangeLogTableForBlankDb(BasicDataSource dataSource, SystemEnvironment env) {
        this.dataSource = dataSource;
        this.env = env;
    }

    public void createChangeLog() {
        try {
            String sql = "CREATE TABLE IF NOT EXISTS changelog (" +
                    "  change_number INTEGER NOT NULL,\n" +
                    "  delta_set VARCHAR(10) NOT NULL,\n" +
                    "  start_dt TIMESTAMP NOT NULL,\n" +
                    "  complete_dt TIMESTAMP NULL,\n" +
                    "  applied_by VARCHAR(100) NOT NULL,\n" +
                    "  description VARCHAR(500) NOT NULL\n" +
                    ");\n" +
                    "\n" +
                    "ALTER TABLE changelog ADD CONSTRAINT Pkchangelog PRIMARY KEY (change_number, delta_set);";

            new JdbcTemplate(dataSource).execute(sql);

        } catch (Exception e) {
            String message = "Unable to create changelog table for database " + dataSource.getUrl() + ". The problem was: " + e.getMessage();
            if (e.getCause() != null) {
                message += ". The cause was: " + e.getCause().getMessage();
            }
            LOG.error(message, e);
            throw bomb(message, e);
        }
        LOG.info("Created changelog table");
    }


}
