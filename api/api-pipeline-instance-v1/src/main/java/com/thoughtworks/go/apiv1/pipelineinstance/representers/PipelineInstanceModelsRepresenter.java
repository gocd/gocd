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
package com.thoughtworks.go.apiv1.pipelineinstance.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.PipelineRunIdInfo;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels;
import com.thoughtworks.go.spark.Routes;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class PipelineInstanceModelsRepresenter {
    public static void toJSON(OutputWriter outputWriter, PipelineInstanceModels pipelineInstanceModels, PipelineRunIdInfo latestAndOldestPipelineId) {
        if (pipelineInstanceModels.isEmpty()) {
            outputWriter.addChildList("pipelines", emptyList());
            return;
        }
        addLinks(outputWriter, pipelineInstanceModels, latestAndOldestPipelineId);
        outputWriter.addChildList("pipelines", pipelinesWriter -> pipelineInstanceModels.forEach(pipelineInstanceModel -> pipelinesWriter.addChild(pipelineWriter -> PipelineInstanceModelRepresenter.toJSON(pipelineWriter, pipelineInstanceModel))));
    }

    private static void addLinks(OutputWriter outputWriter, PipelineInstanceModels pipelineInstanceModels, PipelineRunIdInfo latestAndOldestPipelineId) {
        PipelineInstanceModel latest = pipelineInstanceModels.first();
        PipelineInstanceModel oldest = pipelineInstanceModels.last();
        String previousLink = null, nextLink = null;
        if (latest.getId() != latestAndOldestPipelineId.getLatestRunId()) {
            previousLink = Routes.PipelineInstance.previous(latest.getName(), latest.getId());
        }
        if (oldest.getId() != latestAndOldestPipelineId.getOldestRunId()) {
            nextLink = Routes.PipelineInstance.next(latest.getName(), oldest.getId());
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
