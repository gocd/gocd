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

package com.thoughtworks.go.server.database.h2;

import org.h2.api.Trigger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class UpdateLastTransitionedTime implements Trigger {

    private static final int STATE_TRANSITION_TIMESTAMP = 2;
    private static final int STAGE_ID = 4;

    @Override
    public void init(Connection connection, String schemaName, String triggerName, String tableName, boolean before, int type) throws SQLException {

    }

    @Override
    public void fire(Connection connection, Object[] oldRows, Object[] newRows) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE stages SET lastTransitionedTime = ? WHERE stages.id = ?")) {
            statement.setTimestamp(1, (Timestamp) newRows[STATE_TRANSITION_TIMESTAMP]);
            statement.setLong(2, (Long) newRows[STAGE_ID]);
            statement.addBatch();
            statement.execute();
        }
    }

    @Override
    public void close() {
    }

    @Override
    public void remove() {
    }
}
