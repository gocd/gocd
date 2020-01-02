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
package com.thoughtworks.go.apiv1.admin.pipelinegroups.representers;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.spark.Routes;

public class PipelineGroupsRepresenter {
    public static void toJSON(OutputWriter jsonWriter, PipelineGroups pipelineGroups) {
        jsonWriter.addLinks(linksWriter -> linksWriter
                .addLink("self", Routes.PipelineGroupsAdmin.BASE)
                .addAbsoluteLink("doc", Routes.PipelineGroupsAdmin.DOC)
                .addLink("find", Routes.PipelineGroupsAdmin.find())
        );
        jsonWriter.addChild("_embedded", childWriter -> {
            childWriter.addChildList("groups", groupsWriter -> writePipelineGroups(groupsWriter, pipelineGroups));
        });
    }

    private static void writePipelineGroups(OutputListWriter jsonWriter, PipelineGroups groups) {
        groups.forEach(group -> jsonWriter.addChild(groupWriter -> PipelineGroupRepresenter.toJSON(groupWriter, group)));
    }
}
