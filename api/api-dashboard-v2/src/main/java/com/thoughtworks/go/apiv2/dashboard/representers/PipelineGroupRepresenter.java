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
import com.thoughtworks.go.server.dashboard.GoDashboardPipelineGroup;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.spark.RequestContext;

import java.util.Map;


public class PipelineGroupRepresenter {

    public static Map toJSON(GoDashboardPipelineGroup model, RequestContext requestContext, Username username) {
        return new JsonWriter(requestContext)

                .addDocLink("https://api.go.cd/current/#pipeline-groups")
                .addLink("self", "/api/config/pipeline_groups")

                .add("name", model.getName())
                .add("pipelines", model.allPipelineNames())
                .add("can_administer", model.canBeAdministeredBy(username.getUsername().toString())).getAsMap();
    }
}
