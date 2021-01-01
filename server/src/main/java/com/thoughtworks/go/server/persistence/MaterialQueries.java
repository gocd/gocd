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

package com.thoughtworks.go.server.persistence;

import com.thoughtworks.go.server.dao.FeedModifier;

import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.server.dao.FeedModifier.*;

public class MaterialQueries {
    private static Map<FeedModifier, String> modificationsQueryMap;
    private static Map<FeedModifier, String> modificationsForPatternQueryMap;

    private static final String latestModification =
            "SELECT * " +
                    "FROM modifications " +
                    "WHERE materialid = :materialId " +
                    "ORDER BY id DESC " +
                    "LIMIT :size ";

    private static final String modificationsAfter =
            "SELECT * " +
                    "FROM modifications " +
                    "WHERE materialid = :materialId " +
                    "  and id < :cursor " +
                    "ORDER BY id DESC " +
                    "LIMIT :size ";

    private static final String modificationsBefore =
            "SELECT * " +
                    "FROM (SELECT * " +
                    "      FROM modifications " +
                    "      WHERE materialid = :materialId " +
                    "        and id > :cursor " +
                    "      ORDER BY id ASC " +
                    "      LIMIT :size ) as HistoryBeforeSpecifiedId " +
                    "ORDER BY id DESC";

    private static final String latestModificationForPattern =
            "SELECT * " +
                    "FROM modifications " +
                    "WHERE materialid = :materialId " +
                    "  AND (LOWER(modifications.comment) LIKE :pattern " +
                    "  OR LOWER(userName) LIKE :pattern " +
                    "  OR LOWER(revision) LIKE :pattern ) " +
                    "ORDER BY id DESC " +
                    "LIMIT :size";

    private static final String afterModificationForPattern =
            "SELECT * " +
                    "FROM modifications " +
                    "WHERE materialid = :materialId AND id < :cursor " +
                    "  AND (LOWER(modifications.comment) LIKE :pattern " +
                    "  OR LOWER(userName) LIKE :pattern " +
                    "  OR LOWER(revision) LIKE :pattern ) " +
                    "ORDER BY id DESC " +
                    "LIMIT :size";

    private static final String beforeModificationForPattern =
            "SELECT * " +
                    "FROM ( SELECT * " +
                    "    FROM modifications " +
                    "    WHERE materialid = :materialId AND id > :cursor " +
                    "      AND (LOWER(modifications.comment) LIKE :pattern " +
                    "      OR LOWER(userName) LIKE :pattern " +
                    "      OR LOWER(revision) LIKE :pattern ) " +
                    "    ORDER BY id DESC " +
                    "    LIMIT :size) as LikeMatchBeforeSpecifiedCursor " +
                    "ORDER BY id DESC";

    static {
        modificationsQueryMap = new HashMap<>();
        modificationsQueryMap.put(Latest, latestModification);
        modificationsQueryMap.put(After, modificationsAfter);
        modificationsQueryMap.put(Before, modificationsBefore);

        modificationsForPatternQueryMap= new HashMap<>();
        modificationsForPatternQueryMap.put(Latest, latestModificationForPattern);
        modificationsForPatternQueryMap.put(After, afterModificationForPattern);
        modificationsForPatternQueryMap.put(Before, beforeModificationForPattern);
    }

    public static String loadModificationQuery(FeedModifier modifier) {
        return modificationsQueryMap.get(modifier);
    }

    public static String loadModificationMatchingPatternQuery(FeedModifier modifier) {
        return modificationsForPatternQueryMap.get(modifier);
    }
}
