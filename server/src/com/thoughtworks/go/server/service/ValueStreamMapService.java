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

import com.thoughtworks.go.config.*;
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
	private final MaterialRepository materialRepository;
    private final GoConfigService goConfigService;
    private final DownstreamInstancePopulator downstreamInstancePopulator;
    private final RunStagesPopulator runStagesPopulator;
    private final UnrunStagesPopulator unrunStagePopulator;
    private SecurityService securityService;
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(ValueStreamMapService.class);

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

        valueStreamMap.addWarningIfBuiltFromInCompatibleRevisions();

        return valueStreamMap;
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
				result.notFound(LocalizedMessage.string("MATERIAL_CONFIG_WITH_FINGERPRINT_NOT_FOUND", materialFingerprint), HealthStateType.general(HealthStateScope.GLOBAL));
				return null;
			}

			if (!hasViewPermissionForMaterial) {
				result.unauthorized(LocalizedMessage.cannotViewMaterial(materialFingerprint), HealthStateType.general(HealthStateScope.forMaterialConfig(materialConfig)));
				return null;
			}

			MaterialInstance materialInstance = materialRepository.findMaterialInstance(materialConfig);

			if (materialInstance == null) {
				result.notFound(LocalizedMessage.string("MATERIAL_INSTANCE_WITH_FINGERPRINT_NOT_FOUND", materialFingerprint), HealthStateType.general(HealthStateScope.forMaterialConfig(materialConfig)));
				return null;
			}

			Material material = new MaterialConfigConverter().toMaterial(materialConfig);
			Modification modification = materialRepository.findModificationWithRevision(material, revision);

			if (modification == null) {
				result.notFound(LocalizedMessage.string("MATERIAL_MODIFICATION_NOT_FOUND", materialFingerprint, revision), HealthStateType.general(HealthStateScope.forMaterialConfig(materialConfig)));
				return null;
			}

			ValueStreamMap valueStreamMap = buildValueStreamMap(material, materialInstance, modification, downstreamPipelines, username);
			if (valueStreamMap == null) {
				return null;
			}
			return valueStreamMap.presentationModel();
		} catch (Exception e) {
			result.internalServerError(LocalizedMessage.string("VSM_INTERNAL_SERVER_ERROR_FOR_MATERIAL", materialFingerprint, revision));
			LOGGER.error(String.format("[Value Stream Map] Material %s with revision %s could not be rendered.", materialFingerprint, revision), e);
			return null;
		}
	}

	private ValueStreamMap buildValueStreamMap(Material material, MaterialInstance materialInstance, Modification modification, List<PipelineConfig> downstreamPipelines, Username username) {
		CruiseConfig cruiseConfig = goConfigService.currentCruiseConfig();
		ValueStreamMap valueStreamMap = new ValueStreamMap(material, materialInstance, modification);
		Map<String, List<PipelineConfig>> pipelineToDownstreamMap = cruiseConfig.generatePipelineVsDownstreamMap();

		traverseDownstream(material.getFingerprint(), downstreamPipelines, pipelineToDownstreamMap, valueStreamMap, new ArrayList<PipelineConfig>());

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

                graph.addUpstreamNode(new PipelineDependencyNode(upstreamPipeline, upstreamPipeline), new PipelineRevision(revision.getPipelineName(), revision.getPipelineCounter(), revision.getPipelineLabel()),
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
                        pipelineName, materialRevision);
            }
        }
    }

    private void traverseDownstream(String upstreamPipelineName, Map<String, List<PipelineConfig>> pipelineToDownstreamMap, ValueStreamMap graph, List<PipelineConfig> visitedNodes) {
        List<PipelineConfig> downstreamPipelines = pipelineToDownstreamMap.get(upstreamPipelineName);
		traverseDownstream(upstreamPipelineName, downstreamPipelines, pipelineToDownstreamMap, graph, visitedNodes);
	}

	private void traverseDownstream(String materialId, List<PipelineConfig> downstreamPipelines, Map<String, List<PipelineConfig>> pipelineToDownstreamMap, ValueStreamMap graph, List<PipelineConfig> visitedNodes) {
		for (PipelineConfig downstreamPipeline : downstreamPipelines) {
			String downstreamPipelineName = downstreamPipeline.name().toString();
			graph.addDownstreamNode(new PipelineDependencyNode(downstreamPipelineName, downstreamPipelineName), materialId);
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
