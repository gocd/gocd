/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv6.agents.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.server.service.AgentBuildingInfo;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Arrays.copyOfRange;

public class BuildDetailsRepresenter {
    public static void toJSON(OutputWriter outputWriter, AgentBuildingInfo buildingInfo) {
        outputWriter
                .addLinks(linksWriter -> {
                    linksWriter.addLink("job", jobLink(buildingInfo));
                    linksWriter.addLink("stage", stageLink(buildingInfo));
                    linksWriter.addLink("pipeline", pipelineLink(buildingInfo));
                })
                .add("pipeline_name", buildingInfo.getPipelineName())
                .add("stage_name", buildingInfo.getStageName())
                .add("job_name", buildingInfo.getJobName());
    }

    private static String jobLink(AgentBuildingInfo buildingInfo) {
        return format("/tab/build/detail/%s", buildingInfo.getBuildLocator());
    }

    private static String pipelineLink(AgentBuildingInfo buildingInfo) {
        return format("/pipeline/activity/%s", buildingInfo.getPipelineName());
    }

    private static String stageLink(AgentBuildingInfo buildingInfo) {
        final String[] parts = buildingInfo.getBuildLocator().split("/");
        final String stageLocator = join("/", copyOfRange(parts, 0, parts.length - 1));
        return format("/pipelines/%s", stageLocator);
    }
}
