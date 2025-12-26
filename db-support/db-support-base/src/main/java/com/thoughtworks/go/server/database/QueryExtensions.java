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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class QueryExtensions {
    public String queryFromInclusiveModificationsForPipelineRange(String pipelineName, int fromCounter, int toCounter) {
        // using string concatenation because Hibernate does not seem to be able to replace named or positional parameters here
        return """
            WITH RECURSIVE link(id) AS ( \
              SELECT id \
                 FROM pipelines \
                 WHERE name = '%s' \
                     AND counter >= %s \
                     AND counter <= %s \
              UNION ALL \
              SELECT mods.pipelineId \
                 FROM link \
                     INNER JOIN pipelineMaterialRevisions pmr ON link.id = pmr.pipelineId \
                     INNER JOIN modifications mods ON pmr.toRevisionId >= mods.id and pmr.actualFromRevisionId <= mods.id AND pmr.materialId = mods.materialId \
                 WHERE mods.pipelineId IS NOT NULL \
            )\
            SELECT DISTINCT id FROM link WHERE id IS NOT NULL"""
            .formatted(pipelineName, fromCounter, toCounter);
    }

    public String queryRelevantToLookedUpDependencyMap(List<Long> pipelineIds) {
        // using string concatenation because Hibernate does not seem to be able to replace named or positional parameters here
        return """
            WITH RECURSIVE link(id, name, lookedUpId) AS ( \
              SELECT id, name, id as lookedUpId\
                 FROM pipelines \
                 WHERE id in (%s) \
              UNION ALL \
              SELECT mods.pipelineId as id, p.name as name, link.lookedUpId as lookedUpId \
                 FROM link \
                     INNER JOIN pipelineMaterialRevisions pmr ON link.id = pmr.pipelineId \
                     INNER JOIN modifications mods ON pmr.toRevisionId >= mods.id and pmr.actualFromRevisionId <= mods.id AND pmr.materialId = mods.materialId \
                     INNER JOIN pipelines p ON mods.pipelineId = p.id \
                 WHERE mods.pipelineId IS NOT NULL\
            )\
            SELECT id, name, lookedUpId FROM link""".formatted(joinWithQuotesForSql(pipelineIds));
    }

    public String retrievePipelineTimeline() {
        return """
            SELECT p.name, p.id AS p_id, p.counter, m.modifiedtime, \
             (SELECT materials.fingerprint FROM materials WHERE id = m.materialId), naturalOrder, m.revision, pmr.toRevisionId AS mod_id, pmr.Id as pmrid \
            FROM pipelines p, pipelinematerialrevisions pmr, modifications m \
            WHERE p.id = pmr.pipelineid \
            AND pmr.torevisionid = m.id \
            AND p.id > :pipelineId""";
    }

    protected <T> String joinWithQuotesForSql(List<T> array) {
        return array.stream()
            .map(Objects::toString)
            .collect(Collectors.joining("','", "'", "'"));
    }

    public abstract boolean accepts(String url);
}
