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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.domain.valuestreammap.*;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.presentation.models.ValueStreamMapPresentationModel;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.server.valuestreammap.DownstreamInstancePopulator;
import com.thoughtworks.go.server.valuestreammap.RunStagesPopulator;
import com.thoughtworks.go.server.valuestreammap.UnrunStagesPopulator;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ValueStreamMapService {

    private final PipelineService pipelineService;
    private final MaterialRepository materialRepository;
    private final GoConfigService goConfigService;
    private final DownstreamInstancePopulator downstreamInstancePopulator;
    private final RunStagesPopulator runStagesPopulator;
    private final UnrunStagesPopulator unrunStagePopulator;
    private SecurityService securityService;
    private static final Logger LOGGER = LoggerFactory.getLogger(ValueStreamMapService.class);

    @Autowired
    public ValueStreamMapService(PipelineService pipelineService, MaterialRepository materialRepository, GoConfigService goConfigService, DownstreamInstancePopulator downstreamInstancePopulator,
                                 RunStagesPopulator runStagesPopulator, UnrunStagesPopulator unrunStagePopulator, SecurityService securityService) {
        this.pipelineService = pipelineService;
        this.materialRepository = materialRepository;
        this.goConfigService = goConfigService;
        this.downstreamInstancePopulator = downstreamInstancePopulator;
        this.runStagesPopulator = runStagesPopulator;
        this.unrunStagePopulator = unrunStagePopulator;
        this.securityService = securityService;
    }

    public ValueStreamMapPresentationModel getValueStreamMap(CaseInsensitiveString pipelineName, int counter, Username username, LocalizedOperationResult result) {
        try {
            if (!securityService.hasViewPermissionForPipeline(username, pipelineName.toString())) {
                result.forbidden(LocalizedMessage.forbiddenToViewPipeline(pipelineName), HealthStateType.general(HealthStateScope.forPipeline(pipelineName.toString())));
                return null;
            }
            ValueStreamMap valueStreamMap = buildValueStreamMap(pipelineName, counter, username, result);
            if (valueStreamMap == null) {
                return null;
            }
            return valueStreamMap.presentationModel();
        } catch (Exception e) {
            result.internalServerError("Value Stream Map of pipeline '" + pipelineName + "' with counter '" + counter + "' can not be rendered. Please check the server log for details.");
            LOGGER.error("[Value Stream Map] Pipeline {} with counter {} could not be rendered.", pipelineName, counter, e);
            return null;
        }
    }

    private ValueStreamMap buildValueStreamMap(CaseInsensitiveString pipelineName, int counter, Username username, LocalizedOperationResult result) {
        CruiseConfig cruiseConfig = goConfigService.currentCruiseConfig();
        BuildCause buildCauseForPipeline;
        try {
            pipelineName = pipelineNameWithSameCaseAsConfig(pipelineName, cruiseConfig);
            buildCauseForPipeline = pipelineService.buildCauseFor(pipelineName.toString(), counter);
        } catch (RecordNotFoundException e) {
            result.notFound("Pipeline '" + pipelineName + "' with counter '" + counter + "' not found.", HealthStateType.general(HealthStateScope.forPipeline(pipelineName.toString())));
            return null;
        }
        String label = pipelineService.findPipelineByNameAndCounter(pipelineName.toString(), counter).getLabel();
        ValueStreamMap valueStreamMap = new ValueStreamMap(pipelineName, new PipelineRevision(pipelineName.toString(), counter, label));
        Map<CaseInsensitiveString, List<PipelineConfig>> pipelineToDownstreamMap = cruiseConfig.generatePipelineVsDownstreamMap();

        traverseDownstream(pipelineName, pipelineToDownstreamMap, valueStreamMap, new ArrayList<>());
        traverseUpstream(pipelineName, buildCauseForPipeline, valueStreamMap, new ArrayList<>());

        if (valueStreamMap.hasCycle()) {
            result.notImplemented("Value Stream Map of Pipeline '" + pipelineName + "' with counter '" + counter + "' can not be rendered. Changes to the configuration have introduced complex dependencies for this instance which are not supported currently.");
            LOGGER.error("[Value Stream Map] Cyclic dependency for pipeline {} with counter {}. Graph is {}", pipelineName, counter, valueStreamMap);
            return null;
        }
        addInstanceInformationToTheGraph(valueStreamMap);
        removeRevisionsBasedOnPermissionAndCurrentConfig(valueStreamMap, username, result);

        valueStreamMap.addWarningIfBuiltFromInCompatibleRevisions();

        return valueStreamMap;
    }

    private CaseInsensitiveString pipelineNameWithSameCaseAsConfig(CaseInsensitiveString pipelineName, CruiseConfig cruiseConfig) {
        return cruiseConfig.pipelineConfigByName(pipelineName).name();
    }

    public ValueStreamMapPresentationModel getValueStreamMap(String materialFingerprint, String revision, Username username, LocalizedOperationResult result) {
        try {
            MaterialConfig materialConfig = null;
            boolean hasViewPermissionForMaterial = false;
            List<PipelineConfig> downstreamPipelines = new ArrayList<>();
            for (PipelineConfigs pipelineGroup : goConfigService.groups()) {
                boolean hasViewPermissionForGroup = securityService.hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), pipelineGroup.getGroup());
                for (PipelineConfig pipelineConfig : pipelineGroup) {
                    for (MaterialConfig currentMaterialConfig : pipelineConfig.materialConfigs()) {
                        if (currentMaterialConfig.getFingerprint().equals(materialFingerprint)) {
                            materialConfig = currentMaterialConfig;
                            if (hasViewPermissionForGroup) {
                                hasViewPermissionForMaterial = true;
                            }
                            downstreamPipelines.add(pipelineConfig);
                        }
                    }
                }
            }

            if (materialConfig == null) {
                result.notFound("Material with fingerprint '" + materialFingerprint + "' not found.", HealthStateType.general(HealthStateScope.GLOBAL));
                return null;
            }

            if (!hasViewPermissionForMaterial) {
                result.forbidden("You do not have view permissions for material with fingerprint '" + materialFingerprint + "'.", HealthStateType.general(HealthStateScope.forMaterialConfig(materialConfig)));
                return null;
            }

            MaterialInstance materialInstance = materialRepository.findMaterialInstance(materialConfig);

            if (materialInstance == null) {
                result.notFound("Material Instance with fingerprint '" + materialFingerprint + "' not found.", HealthStateType.general(HealthStateScope.forMaterialConfig(materialConfig)));
                return null;
            }

            Material material = new MaterialConfigConverter().toMaterial(materialConfig);
            Modification modification = materialRepository.findModificationWithRevision(material, revision);

            if (modification == null) {
                result.notFound("Modification '" + revision + "' for material with fingerprint '" + materialFingerprint + "' not found.", HealthStateType.general(HealthStateScope.forMaterialConfig(materialConfig)));
                return null;
            }

            ValueStreamMap valueStreamMap = buildValueStreamMap(material, materialInstance, modification, downstreamPipelines, username, result);
            if (valueStreamMap == null) {
                return null;
            }
            return valueStreamMap.presentationModel();
        } catch (Exception e) {
            result.internalServerError("Value Stream Map of material with fingerprint '" + materialFingerprint + "' with revision '" + revision + "' can not be rendered. Please check the server log for details.");
            LOGGER.error("[Value Stream Map] Material {} with revision {} could not be rendered.", materialFingerprint, revision, e);
            return null;
        }
    }

    private ValueStreamMap buildValueStreamMap(Material material, MaterialInstance materialInstance, Modification modification, List<PipelineConfig> downstreamPipelines, Username username, LocalizedOperationResult result) {
        CruiseConfig cruiseConfig = goConfigService.currentCruiseConfig();
        ValueStreamMap valueStreamMap = new ValueStreamMap(material, materialInstance, modification);
        Map<CaseInsensitiveString, List<PipelineConfig>> pipelineToDownstreamMap = cruiseConfig.generatePipelineVsDownstreamMap();

        traverseDownstream(new CaseInsensitiveString(material.getFingerprint()), downstreamPipelines, pipelineToDownstreamMap, valueStreamMap, new ArrayList<>());

        addInstanceInformationToTheGraph(valueStreamMap);
        removeRevisionsBasedOnPermissionAndCurrentConfig(valueStreamMap, username, result);
        return valueStreamMap;
    }

    private void removeRevisionsBasedOnPermissionAndCurrentConfig(ValueStreamMap valueStreamMap, Username username, LocalizedOperationResult result) {
        for (Node node : valueStreamMap.allNodes()) {
            if (node instanceof PipelineDependencyNode) {
                String pipelineName = node.getName();
                PipelineDependencyNode pipelineDependencyNode = (PipelineDependencyNode) node;

                if (!goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))) {
                    pipelineDependencyNode.setDeleted();
                } else if (!securityService.hasViewPermissionForPipeline(username, pipelineName)) {
                    pipelineDependencyNode.setNoPermission();
                }

                pipelineDependencyNode.setCanEdit(goConfigService.canEditPipeline(pipelineName, username, new HttpLocalizedOperationResult()));
                CaseInsensitiveString templateName = goConfigService.findPipelineByName(new CaseInsensitiveString(pipelineName)).getTemplateName();
                pipelineDependencyNode.setTemplateName(templateName != null ? templateName.toString() : null);
            }
        }
    }

    private void traverseUpstream(CaseInsensitiveString pipelineName, BuildCause buildCause, ValueStreamMap graph, List<MaterialRevision> visitedNodes) {
        for (MaterialRevision materialRevision : buildCause.getMaterialRevisions()) {
            Material material = materialRevision.getMaterial();
            if (material instanceof DependencyMaterial) {
                CaseInsensitiveString upstreamPipeline = ((DependencyMaterial) material).getPipelineName();
                DependencyMaterialRevision revision = (DependencyMaterialRevision) materialRevision.getRevision();

                graph.addUpstreamNode(new PipelineDependencyNode(upstreamPipeline, upstreamPipeline.toString()), new PipelineRevision(revision.getPipelineName(), revision.getPipelineCounter(), revision.getPipelineLabel()),
                        pipelineName);

                if (visitedNodes.contains(materialRevision)) {
                    continue;
                }
                visitedNodes.add(materialRevision);
                DependencyMaterialRevision dmrOfUpstreamPipeline = buildCause.getMaterialRevisions().findDependencyMaterialRevision(upstreamPipeline.toString());
                BuildCause buildCauseForUpstreamPipeline = pipelineService.buildCauseFor(dmrOfUpstreamPipeline.getPipelineName(), dmrOfUpstreamPipeline.getPipelineCounter());
                traverseUpstream(upstreamPipeline, buildCauseForUpstreamPipeline, graph, visitedNodes);
            } else {
                graph.addUpstreamMaterialNode(new SCMDependencyNode(material.getFingerprint(), material.getUriForDisplay(), materialRevision.getMaterialType()), material.getName(),
                        pipelineName, materialRevision);
            }
        }
    }

    private void traverseDownstream(CaseInsensitiveString upstreamPipelineName, Map<CaseInsensitiveString, List<PipelineConfig>> pipelineToDownstreamMap, ValueStreamMap graph, List<PipelineConfig> visitedNodes) {
        List<PipelineConfig> downstreamPipelines = pipelineToDownstreamMap.get(upstreamPipelineName);
        traverseDownstream(upstreamPipelineName, downstreamPipelines, pipelineToDownstreamMap, graph, visitedNodes);
    }

    private void traverseDownstream(CaseInsensitiveString materialId, List<PipelineConfig> downstreamPipelines, Map<CaseInsensitiveString, List<PipelineConfig>> pipelineToDownstreamMap, ValueStreamMap graph, List<PipelineConfig> visitedNodes) {
        for (PipelineConfig downstreamPipeline : downstreamPipelines) {
            graph.addDownstreamNode(new PipelineDependencyNode(downstreamPipeline.name(),
                    downstreamPipeline.name().toString()), materialId);
            if (visitedNodes.contains(downstreamPipeline)) {
                continue;
            }
            visitedNodes.add(downstreamPipeline);
            traverseDownstream(downstreamPipeline.name(), pipelineToDownstreamMap, graph, visitedNodes);
        }
    }

    private void addInstanceInformationToTheGraph(ValueStreamMap valueStreamMap) {
        downstreamInstancePopulator.apply(valueStreamMap);
        runStagesPopulator.apply(valueStreamMap);
        unrunStagePopulator.apply(valueStreamMap);
    }
}
