/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.sqlmigration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.h2.api.Trigger;
import org.apache.log4j.Logger;

public class Migration_86 implements Trigger {
    private static final Logger LOGGER = Logger.getLogger(Migration_86.class);

    private PreparedStatement updateStmt;
    private PreparedStatement nextModificationIdStmt;
    private Long totalCount;

    public class PMRRecord {
        private final long materialId;
        private final String pipelineName;

        public PMRRecord(long materialId, String pipelineName) {
            this.materialId = materialId;
            this.pipelineName = pipelineName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            PMRRecord pmrRecord = (PMRRecord) o;

            if (materialId != pmrRecord.materialId) {
                return false;
            }
            if (pipelineName != null ? !pipelineName.equals(pmrRecord.pipelineName) : pmrRecord.pipelineName != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (materialId ^ (materialId >>> 32));
            result = 31 * result + (pipelineName != null ? pipelineName.hashCode() : 0);
            return result;
        }
    }

    Map<PMRRecord, Long> map = new HashMap<>();

    public void init(Connection connection, String schemaName, String triggerName, String tableName, boolean before, int type) throws SQLException {
        totalCount = executeScalar(connection, "SELECT COUNT(*) FROM pipelineMaterialRevisions");

        updateStmt = connection.prepareStatement(
                  " UPDATE pipelineMaterialRevisions pmr"
                + " SET actualFromRevisionId = ? "
                + " WHERE pmr.Id = ?");

        nextModificationIdStmt = connection.prepareStatement(
                  " SELECT TOP 1 id "
                + " FROM modifications "
                + " WHERE id > ? AND materialId = ?"
                + " ORDER BY id");
    }

    public void fire(Connection connection, Object[] oldRows, Object[] newRows) throws SQLException {
        LOGGER.info(String.format("migrating %s rows", totalCount));

        int count = 0;
        ResultSet resultSet = queryPipelineMaterialRevisions(connection);
        while(resultSet.next()) {
            long pmrId = resultSet.getLong("Id");
            long fromRevision = resultSet.getLong("fromRevisionId");
            long materialId = resultSet.getLong("materialId");
            String materialType = resultSet.getString("materialType");
            String pipelineName = resultSet.getString("pipelineName");

            PMRRecord pmrRecord = new PMRRecord(materialId, pipelineName);
            long lastSeenFromRevision = fromRevision;
            if ("DependencyMaterial".equals(materialType) && map.containsKey(pmrRecord)) {
                lastSeenFromRevision = nextModificationId(map.get(pmrRecord), materialId);
            }
            fireUpdateQuery(pmrId, lastSeenFromRevision);
            map.put(pmrRecord, fromRevision);

            logCount(count++);
        }
    }

    private void logCount(int count) {
        if (count % 5000 == 0) {
            LOGGER.info(String.format("    %s done", count));
        }
    }

    private Long nextModificationId(long prevModificationId, long materialId) throws SQLException {
        nextModificationIdStmt.setLong(1, prevModificationId);
        nextModificationIdStmt.setLong(2, materialId);
        return executeScalar(nextModificationIdStmt, prevModificationId);
    }

    private void fireUpdateQuery(long pmrId, long lastSeenPmrId) throws SQLException {
        updateStmt.setLong(1, lastSeenPmrId);
        updateStmt.setLong(2, pmrId);
        updateStmt.executeUpdate();
    }

    private ResultSet queryPipelineMaterialRevisions(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        String sql = "SELECT"
                + "    pmr.Id AS Id,"
                + "    pmr.fromRevisionId AS fromRevisionId,"
                + "    pmr.materialId AS materialId,"
                + "    m.type AS materialType,"
                + "    p.name AS pipelineName"
                + " FROM"
                + "    pipelines p"
                + "    INNER JOIN pipelineMaterialRevisions pmr ON pmr.pipelineId = p.id"
                + "    INNER JOIN materials m ON m.id = pmr.materialId"
                + " ORDER BY"
                + "    p.name, m.id, pmr.id";
        return statement.executeQuery(sql);
    }

    private static Long executeScalar(Connection connection, String sql) throws SQLException {
        ResultSet rs = null;
        try {
            rs = connection.createStatement().executeQuery(sql);
            rs.next();
            return rs.getLong(1);
        } finally {
            closeQuietly(rs);
        }
    }

    private static Long executeScalar(PreparedStatement ps, Long defaultValue) throws SQLException {
        ResultSet rs = null;
        try {
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
            return defaultValue;
        } finally {
            closeQuietly(rs);
        }
    }

    public static void closeQuietly(ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            // ignore
        }
    }

    public void close() throws SQLException {
    }

    public void remove() throws SQLException {
    }
}
