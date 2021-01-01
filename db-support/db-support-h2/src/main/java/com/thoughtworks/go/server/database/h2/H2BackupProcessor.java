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

package com.thoughtworks.go.server.database.h2;

import com.thoughtworks.go.server.database.BackupProcessor;
import com.thoughtworks.go.server.database.DbProperties;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class H2BackupProcessor implements BackupProcessor {

    @Override
    public void backup(File targetDir, DataSource dataSource, DbProperties dbProperties) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            File dbBackupFile = new File(targetDir, "db.zip");
            statement.execute(String.format("BACKUP TO '%s'", dbBackupFile));
        }
    }

    @Override
    public boolean accepts(String url) {
        return isBlank(url) || url.startsWith("jdbc:h2:");
    }
}
