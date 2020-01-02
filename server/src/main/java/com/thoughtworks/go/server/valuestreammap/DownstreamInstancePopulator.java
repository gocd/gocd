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
package com.thoughtworks.go.server.valuestreammap;

import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.PipelineIdentifier;
import com.thoughtworks.go.domain.valuestreammap.Node;
import com.thoughtworks.go.domain.valuestreammap.PipelineRevision;
import com.thoughtworks.go.domain.valuestreammap.Revision;
import com.thoughtworks.go.domain.valuestreammap.ValueStreamMap;
import com.thoughtworks.go.server.dao.PipelineDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DownstreamInstancePopulator {
    private PipelineDao pipelineDao;

    @Autowired
    public DownstreamInstancePopulator(PipelineDao pipelineDao) {
        this.pipelineDao = pipelineDao;
    }

	public void apply(ValueStreamMap valueStreamMap) {
		if (valueStreamMap.getCurrentPipeline() != null) {
			Node currentPipeline = valueStreamMap.getCurrentPipeline();
			populateRevisionsForAllChildrenOf(currentPipeline, new HashSet<>());
		} else {
			Node currentMaterial = valueStreamMap.getCurrentMaterial();
			MaterialInstance currentMaterialInstance = valueStreamMap.getCurrentMaterialInstance();
			populateRevisionsFor(currentMaterial, currentMaterialInstance, new HashSet<>());
		}
	}

	private void populateRevisionsFor(Node currentMaterial, MaterialInstance currentMaterialInstance, HashSet<Revision> visitedRevisions) {
		String revision = currentMaterial.revisions().get(0).getRevisionString();
		List<Node> downstreamPipelines = currentMaterial.getChildren();
		for (Node downstreamPipeline : downstreamPipelines) {
			List<PipelineIdentifier> pipelineIdentifiers = pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial(downstreamPipeline.getName(), currentMaterialInstance, revision);
			addRevisionsToNode(downstreamPipeline, pipelineIdentifiers);
			populateRevisionsForAllChildrenOf(downstreamPipeline, visitedRevisions);
		}
	}

    private void populateRevisionsForAllChildrenOf(Node node, Set<Revision> visitedRevisions) {
        for (Revision revision : node.revisions()) {
            if (visitedRevisions.contains(revision)) {
                continue;
            }
            visitedRevisions.add(revision);
            for (Node child : node.getChildren()) {
                List<PipelineIdentifier> pipelineIdentifiers = pipelineDao.getPipelineInstancesTriggeredWithDependencyMaterial(child.getName(), ((PipelineRevision) revision).getPipelineIdentifier());
				addRevisionsToNode(child, pipelineIdentifiers);
                populateRevisionsForAllChildrenOf(child, visitedRevisions);
            }
        }
    }

	private void addRevisionsToNode(Node node, List<PipelineIdentifier> pipelineIdentifiers) {
		for (PipelineIdentifier pipelineIdentifier : pipelineIdentifiers) {
			node.addRevision(new PipelineRevision(pipelineIdentifier));
		}
	}
}
