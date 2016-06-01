/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.valuestreammap;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.domain.NullStage;
import com.thoughtworks.go.domain.Stages;
import com.thoughtworks.go.domain.valuestreammap.*;
import com.thoughtworks.go.server.service.GoConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

@Component
public class UnrunStagesPopulator {
    private GoConfigService goConfigService;

    @Autowired
    public UnrunStagesPopulator(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }

	public void apply(ValueStreamMap valueStreamMap) {
		if (valueStreamMap.getCurrentPipeline() != null) {
			Node currentPipeline = valueStreamMap.getCurrentPipeline();
			getUnrunStagesForDownstreamGraphStartingFrom(currentPipeline, new HashSet<Node>());
		} else {
			List<Node> downstreamPipelines = valueStreamMap.getCurrentMaterial().getChildren();
			HashSet<Node> visitedNodes = new HashSet<>();
			for (Node downstreamPipeline : downstreamPipelines) {
				getUnrunStagesForDownstreamGraphStartingFrom(downstreamPipeline, visitedNodes);
			}
		}
	}

    private void getUnrunStagesForDownstreamGraphStartingFrom(Node node, HashSet<Node> visitedNodes) {
        if (visitedNodes.contains(node)) {
            return;
        }

        visitedNodes.add(node);
        populateUnrunStages(node);
        for (Node child : node.getChildren()) {
            getUnrunStagesForDownstreamGraphStartingFrom(child, visitedNodes);
        }
    }

    private void populateUnrunStages(Node node) {
        List<Revision> revisions = node.revisions();
        PipelineConfig pipelineConfig = goConfigService.getCurrentConfig().pipelineConfigByName(new CaseInsensitiveString(node.getName()));
        if (revisions.isEmpty()) {
            populateConfiguredStages(node, pipelineConfig);
        }
        for (Revision revision : revisions) {
            appendUnrunStages(pipelineConfig, (PipelineRevision) revision);
        }
    }

    private void appendUnrunStages(PipelineConfig pipelineConfig, PipelineRevision pipelineRevision) {
        Stages stages = pipelineRevision.getStages();
        StageConfig nextStage = pipelineConfig.nextStage(new CaseInsensitiveString(stages.last().getName()));
        while (nextStage != null && !stages.hasStage(nextStage.name().toString())) {
            pipelineRevision.addStage(new NullStage(nextStage.name().toString()));
            nextStage = pipelineConfig.nextStage(nextStage.name());
        }
    }

    private void populateConfiguredStages(Node node, PipelineConfig pipelineConfig) {
        UnrunPipelineRevision unrunPipelineRevision = new UnrunPipelineRevision(node.getName());
        for (StageConfig stageConfig : pipelineConfig) {
            unrunPipelineRevision.addStage(new NullStage(stageConfig.name().toString()));
        }
        node.addRevision(unrunPipelineRevision);
    }
}
