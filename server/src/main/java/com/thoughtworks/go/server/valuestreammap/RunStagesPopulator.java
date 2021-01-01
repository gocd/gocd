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
package com.thoughtworks.go.server.valuestreammap;

import com.thoughtworks.go.domain.Stages;
import com.thoughtworks.go.domain.valuestreammap.Node;
import com.thoughtworks.go.domain.valuestreammap.ValueStreamMap;
import com.thoughtworks.go.domain.valuestreammap.PipelineRevision;
import com.thoughtworks.go.domain.valuestreammap.Revision;
import com.thoughtworks.go.server.dao.StageDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RunStagesPopulator {
    private final StageDao stageDao;

    @Autowired
    public RunStagesPopulator(StageDao stageDao) {
        this.stageDao = stageDao;
    }

    public void apply(ValueStreamMap graph) {
        for (Node node : graph.allNodes()) {
            for (Revision revision : node.revisions()) {
                if (revision instanceof PipelineRevision) {
                    PipelineRevision pipelineRevision = (PipelineRevision) revision;
                    Stages latestStages = latestRunStagesForRevsion(pipelineRevision);
                    pipelineRevision.addStages(latestStages);
                }
            }
        }
    }

    private Stages latestRunStagesForRevsion(PipelineRevision pipelineRevision) {
        Stages allStages = stageDao.findAllStagesFor(pipelineRevision.getPipelineName(), pipelineRevision.getCounter());
        return allStages.latestStagesInRunOrder();
    }
}
