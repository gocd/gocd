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

package com.thoughtworks.go.apiv2.dashboard.representers;

import com.thoughtworks.go.api.representers.JsonWriter;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.spark.RequestContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BuildCauseRepresenter {

    public static Map toJSON(BuildCause model, RequestContext requestContext) {
        return new JsonWriter(requestContext)
                .add("approver", model.getApprover())
                .add("is_forced", model.isForced())
                .add("trigger_message", model.getBuildCauseMessage())
                .add("material_revisions", getMaterialRevisions(model, requestContext)).getAsMap();
    }

    private static List<Map> getMaterialRevisions(BuildCause model, RequestContext requestContext) {
        List<Map> materialRevisions = new ArrayList<>();
        model.getMaterialRevisions().forEach(materialRevision -> {
            materialRevisions.add(MaterialRevisionRepresenter.toJSON(materialRevision, requestContext));
        });
        return materialRevisions;
    }

}
