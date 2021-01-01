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
package com.thoughtworks.go.apiv3.stageinstance.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.PipelineRunIdInfo;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels;
import com.thoughtworks.go.spark.Routes;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class StageInstancesRepresenter {
    public static void toJSON(OutputWriter outputWriter, StageInstanceModels stageInstanceModels, PipelineRunIdInfo runIdInfo) {
        if (stageInstanceModels.isEmpty()) {
            outputWriter.addChildList("stages", emptyList());
            return;
        }
        addLinks(outputWriter, stageInstanceModels, runIdInfo);
        outputWriter.addChildList("stages", stageInstancesWriter -> stageInstanceModels.forEach(
                stageInstanceModel -> stageInstancesWriter.addChild(stageInstanceWriter -> StageInstanceRepresenter.toJSON(stageInstanceWriter, stageInstanceModel))));
    }

    private static void addLinks(OutputWriter outputWriter, StageInstanceModels stageInstanceModels, PipelineRunIdInfo runIdInfo) {
        StageInstanceModel latest = stageInstanceModels.first();
        StageInstanceModel oldest = stageInstanceModels.last();
        String previousLink = null, nextLink = null;
        if (latest.getId() != runIdInfo.getLatestRunId()) {
            previousLink = Routes.Stage.previous(latest.getPipelineName(), latest.getName(), latest.getId());
        }
        if (oldest.getId() != runIdInfo.getOldestRunId()) {
            nextLink = Routes.Stage.next(latest.getPipelineName(), latest.getName(), oldest.getId());
        }
        if (isNotBlank(previousLink) || isNotBlank(nextLink)) {
            String finalPreviousLink = previousLink;
            String finalNextLink = nextLink;
            outputWriter.addLinks(outputLinkWriter -> {
                outputLinkWriter.addLinkIfPresent("previous", finalPreviousLink);
                outputLinkWriter.addLinkIfPresent("next", finalNextLink);
            });
        }
    }
}
