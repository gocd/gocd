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

package com.thoughtworks.go.server.database.pg;

import com.thoughtworks.go.server.database.QueryExtensions;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class PostgresqlQueryExtensions extends QueryExtensions {
    @Override
    public String retrievePipelineTimeline() {
        // we run a `CAST` because otherwise hibernate is unable to understand the `citext` datatype
        return "SELECT CAST(p.name AS VARCHAR), p.id AS p_id, p.counter, m.modifiedtime, "
                + " (SELECT CAST(materials.fingerprint AS VARCHAR) FROM materials WHERE id = m.materialId), naturalOrder, m.revision, pmr.folder, pmr.toRevisionId AS mod_id, pmr.Id as pmrid "
                + "FROM pipelines p, pipelinematerialrevisions pmr, modifications m "
                + "WHERE p.id = pmr.pipelineid "
                + "AND pmr.torevisionid = m.id "
                + "AND p.id > :pipelineId";
    }

    @Override
    public boolean accepts(String url) {
        return isNotBlank(url) && url.startsWith("jdbc:postgresql:");
    }
}
