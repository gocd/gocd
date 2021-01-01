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
package com.thoughtworks.go.apiv1.admin.pipelinegroups.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.Authorization;
import com.thoughtworks.go.config.BasicPipelineConfigs;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.spark.Routes;

public class PipelineGroupRepresenter {
    public static void toJSON(OutputWriter jsonWriter, PipelineConfigs group) {
        jsonWriter.addLinks(linksWriter -> linksWriter
                .addLink("self", Routes.PipelineGroupsAdmin.name(group.getGroup()))
                .addAbsoluteLink("doc", Routes.PipelineGroupsAdmin.DOC)
                .addLink("find", Routes.PipelineGroupsAdmin.find())
        );
        jsonWriter.add("name", group.getGroup());
        jsonWriter.addChild("authorization", childWriter -> AuthorizationRepresenter.toJSON(childWriter, group.getAuthorization()));
        jsonWriter.addChildList("pipelines", pipelinesWriter -> PipelineConfigSummaryRepresenter.toJSON(pipelinesWriter, group.getPipelines()));
    }

    public static PipelineConfigs fromJSON(JsonReader jsonReader) {
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs();

        jsonReader.readStringIfPresent("name", pipelineConfigs::setGroup);
        if (jsonReader.hasJsonObject("authorization")) {
            Authorization authorization = AuthorizationRepresenter.fromJSON(jsonReader.readJsonObject("authorization"));
            pipelineConfigs.setAuthorization(authorization);
        }

        return pipelineConfigs;
    }
}
