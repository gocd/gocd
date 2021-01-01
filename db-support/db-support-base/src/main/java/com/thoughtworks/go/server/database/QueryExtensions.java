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

package com.thoughtworks.go.server.database;

import java.util.List;

public abstract class QueryExtensions {
    public String queryFromInclusiveModificationsForPipelineRange(String pipelineName, Integer fromCounter, Integer toCounter) {
        return "WITH RECURSIVE link(id) AS ( "
                + "  SELECT id "
                + "     FROM pipelines "
                + "     WHERE name = " + getQuotedString(
                pipelineName)  // using string concatenation because Hibernate does not seem to be able to replace named or positional parameters here
                + "         AND counter >= " + fromCounter
                + "         AND counter <= " + toCounter
                + "  UNION ALL "
                + "  SELECT mod.pipelineId "
                + "     FROM link "
                + "         INNER JOIN pipelineMaterialRevisions pmr ON link.id = pmr.pipelineId "
                + "         INNER JOIN modifications mod ON pmr.toRevisionId >= mod.id and pmr.actualFromRevisionId <= mod.id AND pmr.materialId = mod.materialId "
                + "     WHERE mod.pipelineId IS NOT NULL"
                + ")"
                + "SELECT DISTINCT id FROM link WHERE id IS NOT NULL";

    }

    public String queryRelevantToLookedUpDependencyMap(List<Long> pipelineIds) {
        return "WITH RECURSIVE link(id, name, lookedUpId) AS ( "
                + "  SELECT id, name, id as lookedUpId"
                + "     FROM pipelines "
                + "     WHERE id in (" + joinWithQuotesForSql(pipelineIds.toArray()) + ") "
                + "  UNION ALL "
                + "  SELECT mod.pipelineId as id, p.name as name, link.lookedUpId as lookedUpId "
                + "     FROM link "
                + "         INNER JOIN pipelineMaterialRevisions pmr ON link.id = pmr.pipelineId "
                + "         INNER JOIN modifications mod ON pmr.toRevisionId >= mod.id and pmr.actualFromRevisionId <= mod.id AND pmr.materialId = mod.materialId "
                + "         INNER JOIN pipelines p ON mod.pipelineId = p.id "
                + "     WHERE mod.pipelineId IS NOT NULL"
                + ")"
                + "SELECT id, name, lookedUpId FROM link";
    }

    public String retrievePipelineTimeline() {
        return "SELECT p.name, p.id AS p_id, p.counter, m.modifiedtime, "
                + " (SELECT materials.fingerprint FROM materials WHERE id = m.materialId), naturalOrder, m.revision, pmr.folder, pmr.toRevisionId AS mod_id, pmr.Id as pmrid "
                + "FROM pipelines p, pipelinematerialrevisions pmr, modifications m "
                + "WHERE p.id = pmr.pipelineid "
                + "AND pmr.torevisionid = m.id "
                + "AND p.id > :pipelineId";
    }

    protected <T> String joinWithQuotesForSql(T[] array) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < array.length; i++) {
            T t = array[i];
            buffer.append(getQuotedString(t.toString()));
            if (i < array.length - 1) {
                buffer.append(',');
            }
        }
        return buffer.toString();

    }

    protected String getQuotedString(String value) {
        return String.format("'%s'", value);
    }

    public abstract boolean accepts(String url);
}
