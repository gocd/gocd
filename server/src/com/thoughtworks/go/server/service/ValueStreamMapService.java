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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineNotFoundException;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.domain.valuestreammap.*;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.presentation.models.ValueStreamMapPresentationModel;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.server.valuestreammap.DownstreamInstancePopulator;
import com.thoughtworks.go.server.valuestreammap.RunStagesPopulator;
import com.thoughtworks.go.server.valuestreammap.UnrunStagesPopulator;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ValueStreamMapService {

    private final PipelineService pipelineService;
    private final GoConfigService goConfigService;
    private final DownstreamInstancePopulator downstreamInstancePopulator;
    private final RunStagesPopulator runStagesPopulator;
    private final UnrunStagesPopulator unrunStagePopulator;
    private SecurityService securityService;
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(ValueStreamMapService.class);

    @Autowired
    public ValueStreamMapService(PipelineService pipelineService, GoConfigService goConfigService, DownstreamInstancePopulator downstreamInstancePopulator,
                                 RunStagesPopulator runStagesPopulator, UnrunStagesPopulator unrunStagePopulator, SecurityService securityService) {
        this.pipelineService = pipelineService;
        this.goConfigService = goConfigService;
        this.downstreamInstancePopulator = downstreamInstancePopulator;
        this.runStagesPopulator = runStagesPopulator;
        this.unrunStagePopulator = unrunStagePopulator;
        this.securityService = securityService;
    }

    public ValueStreamMapPresentationModel getValueStreamMap(String pipelineName, int counter, Username username, LocalizedOperationResult result) {
        try {
            if (!securityService.hasViewPermissionForPipeline(username, pipelineName)) {
                result.unauthorized(LocalizedMessage.cannotViewPipeline(pipelineName), HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
                return null;
            }
            ValueStreamMap valueStreamMap = buildValueStreamMap(pipelineName,counter, username, result);
            if (valueStreamMap == null) {
                return null;
            }
            return valueStreamMap.presentationModel();
        } catch (Exception e) {
            result.internalServerError(LocalizedMessage.string("VSM_INTERNAL_SERVER_ERROR", pipelineName, counter));
            LOGGER.error(String.format("[Value Stream Map] Pipeline %s with counter %s could not be rendered.", pipelineName, counter), e);
            return null;
        }
    }

    private ValueStreamMap buildValueStreamMap(String pipelineName, int counter, Username username, LocalizedOperationResult result) {
        CruiseConfig cruiseConfig = goConfigService.currentCruiseConfig();
        BuildCause buildCauseForPipeline;
        try {
            pipelineName = pipelineNameWithSameCaseAsConfig(pipelineName, cruiseConfig);
            buildCauseForPipeline = pipelineService.buildCauseFor(pipelineName, counter);
        } catch (PipelineNotFoundException e) {
            result.notFound(LocalizedMessage.string("PIPELINE_WITH_COUNTER_NOT_FOUND", pipelineName, counter), HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
            return null;
        }
        String label = pipelineService.findPipelineByCounterOrLabel(pipelineName, String.valueOf(counter)).getLabel();
        ValueStreamMap valueStreamMap = new ValueStreamMap(pipelineName, new PipelineRevision(pipelineName, counter, label));
        Map<String, List<PipelineConfig>> pipelineToDownstreamMap = cruiseConfig.generatePipelineVsDownstreamMap();

        traverseDownstream(pipelineName, pipelineToDownstreamMap, valueStreamMap, new ArrayList<PipelineConfig>());
        traverseUpstream(pipelineName, buildCauseForPipeline, valueStreamMap, new ArrayList<MaterialRevision>());

        if (valueStreamMap.hasCycle()) {
            result.notImplemented(LocalizedMessage.string("VSM_CYCLIC_DEPENDENCY",pipelineName,counter));
            LOGGER.error(String.format("[Value Stream Map] Cyclic dependency for pipeline %s with counter %s. Graph is %s", pipelineName, counter, valueStreamMap));
            return null;
        }
        addInstanceInformationToTheGraph(valueStreamMap);
        removeRevisionsBasedOnPermissionAndCurrentConfig(valueStreamMap, username);
        return valueStreamMap;
    }

    private String pipelineNameWithSameCaseAsConfig(String pipelineName, CruiseConfig cruiseConfig) {
        return cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName)).name().toString();
    }

    private void removeRevisionsBasedOnPermissionAndCurrentConfig(ValueStreamMap valueStreamMap, Username username) {
        for (Node node : valueStreamMap.allNodes()) {
            if (node instanceof PipelineDependencyNode) {
                String pipelineName = node.getName();
                PipelineDependencyNode pipelineDependencyNode = (PipelineDependencyNode) node;

                if (!goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))) {
                    pipelineDependencyNode.setDeleted();
                } else if (!securityService.hasViewPermissionForPipeline(username, pipelineName)) {
                    pipelineDependencyNode.setNoPermission();
                }
            }
        }
    }

    private void traverseUpstream(String pipelineName, BuildCause buildCause, ValueStreamMap graph, List<MaterialRevision> visitedNodes) {
        for (MaterialRevision materialRevision : buildCause.getMaterialRevisions()) {
            Material material = materialRevision.getMaterial();
            if (material instanceof DependencyMaterial) {
                String upstreamPipeline = ((DependencyMaterial) material).getPipelineName().toString();
                DependencyMaterialRevision revision = (DependencyMaterialRevision) materialRevision.getRevision();

                graph.addUpstreamNode(new PipelineDependencyNode(upstreamPipeline, upstreamPipeline),new PipelineRevision(revision.getPipelineName(), revision.getPipelineCounter(), revision.getPipelineLabel()),
                        pipelineName);

                if (visitedNodes.contains(materialRevision)) {
                    continue;
                }
                visitedNodes.add(materialRevision);
                DependencyMaterialRevision dmrOfUpstreamPipeline = buildCause.getMaterialRevisions().findDependencyMaterialRevision(upstreamPipeline);
                BuildCause buildCauseForUpstreamPipeline = pipelineService.buildCauseFor(dmrOfUpstreamPipeline.getPipelineName(), dmrOfUpstreamPipeline.getPipelineCounter());
                traverseUpstream(upstreamPipeline, buildCauseForUpstreamPipeline, graph, visitedNodes);
            } else {
                graph.addUpstreamMaterialNode(new SCMDependencyNode(material.getFingerprint(), material.getUriForDisplay(), materialRevision.getMaterialType()), material.getName(),
                        materialRevision.getModifications(), pipelineName);
            }
        }
    }

    private void traverseDownstream(String upstreamPipelineName, Map<String, List<PipelineConfig>> pipelineToDownstreamMap, ValueStreamMap graph, List<PipelineConfig> visitedNodes) {
        List<PipelineConfig> downstreamPipelines = pipelineToDownstreamMap.get(upstreamPipelineName);
        for (PipelineConfig downstreamPipeline : downstreamPipelines) {
            String downstreamPipelineName = downstreamPipeline.name().toString();
            graph.addDownstreamNode(new PipelineDependencyNode(downstreamPipelineName, downstreamPipelineName), upstreamPipelineName);
            if (visitedNodes.contains(downstreamPipeline)) {
                continue;
            }
            visitedNodes.add(downstreamPipeline);
            traverseDownstream(downstreamPipelineName, pipelineToDownstreamMap, graph, visitedNodes);
        }
    }

    private void addInstanceInformationToTheGraph(ValueStreamMap valueStreamMap) {
        downstreamInstancePopulator.apply(valueStreamMap);
        runStagesPopulator.apply(valueStreamMap);
        unrunStagePopulator.apply(valueStreamMap);
    }
}
