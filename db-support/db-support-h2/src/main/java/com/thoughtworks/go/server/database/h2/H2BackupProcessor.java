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

package com.thoughtworks.go.server.database.h2;

import com.thoughtworks.go.server.database.BackupProcessor;
import com.thoughtworks.go.server.database.DbProperties;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class H2BackupProcessor implements BackupProcessor {

    @Override
    public void backup(File targetDir, DataSource dataSource, DbProperties dbProperties) {
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("BACKUP TO ?")) {
            File backupFile = new File(targetDir, "db.zip");
            statement.setString(1, backupFile.toString());
            statement.execute();
        } catch (SQLException e) {
            throwBackupError("H2db BACKUP TO", e.getErrorCode(), e);
        }
    }

    @Override
    public boolean accepts(String url) {
        return url == null || url.isBlank() || url.startsWith("jdbc:h2:");
    }
}
