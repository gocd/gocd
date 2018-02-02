/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.materialsearch.representers;

import com.thoughtworks.go.api.representers.JsonWriter;
import com.thoughtworks.go.domain.materials.MatchedRevision;
import com.thoughtworks.go.spark.RequestContext;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MatchedRevisionRepresenter {

    public static List<Map> toJSON(List<MatchedRevision> matchedRevisions, RequestContext requestContext) {
        return matchedRevisions.stream()
                .map(matchedRevision -> MatchedRevisionRepresenter.toJSON(matchedRevision, requestContext))
                .collect(Collectors.toList());
    }

    public static Map toJSON(MatchedRevision matchedRevision, RequestContext requestContext) {
        return new JsonWriter(requestContext)
                .add("revision", matchedRevision.getLongRevision())
                .addIfNotNull("user", matchedRevision.getUser())
                .addIfNotNull("date", matchedRevision.getCheckinTime())
                .add("comment", matchedRevision.getComment())
                .getAsMap();
    }

}
