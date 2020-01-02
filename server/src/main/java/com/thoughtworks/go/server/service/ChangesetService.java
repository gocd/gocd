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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.domain.PipelineConfigDependencyGraph;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.ui.ModificationForPipeline;
import com.thoughtworks.go.server.web.PipelineRevisionRange;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.thoughtworks.go.serverhealth.HealthStateScope.forPipeline;
import static com.thoughtworks.go.serverhealth.HealthStateType.general;

@Service
public class ChangesetService {
    private PipelineSqlMapDao pipelineDao;
    private MaterialRepository materialRepository;
    private final GoConfigService goConfigService;
    private SecurityService securityService;

    @Autowired
    public ChangesetService(SecurityService securityService, PipelineSqlMapDao pipelineDao, MaterialRepository materialRepository, GoConfigService goConfigService) {
        this.securityService = securityService;
        this.pipelineDao = pipelineDao;
        this.materialRepository = materialRepository;
        this.goConfigService = goConfigService;
    }

    public List<MaterialRevision> revisionsBetween(List<PipelineRevisionRange> pipelineRevisionRanges, Username username, HttpLocalizedOperationResult result) {
        ArrayList<MaterialRevision> revisions = new ArrayList<>();
        for (PipelineRevisionRange pipelineRevisionRange : pipelineRevisionRanges) {
            DependencyMaterialRevision fromDmr = DependencyMaterialRevision.create(pipelineRevisionRange.getFromRevision(), null);
            DependencyMaterialRevision toDmr = DependencyMaterialRevision.create(pipelineRevisionRange.getToRevision(), null);
            revisions.addAll(revisionsBetween(pipelineRevisionRange.getPipelineName(), fromDmr.getPipelineCounter(), toDmr.getPipelineCounter(), username, result, false));
        }
        return deduplicateMaterialRevisionsForCommonMaterials(revisions);
    }

    public List<MaterialRevision> revisionsBetween(String pipelineName, Integer fromCounter, Integer toCounter, Username username, HttpLocalizedOperationResult result,
                                                   boolean showBisect) {
        if (!securityService.hasViewPermissionForPipeline(username, pipelineName)) {
            result.forbidden(EntityType.Pipeline.forbiddenToView(pipelineName, username.getUsername()), HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
            return new ArrayList<>();
        }

        if (!goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))) {
            result.notFound(EntityType.Pipeline.notFoundMessage(pipelineName), general(forPipeline(pipelineName)));
            return new ArrayList<>();
        }

        if (fromCounter.equals(toCounter)) {
            fromCounter -= 1;
        }

        if (fromCounter < 0 || toCounter <= 0) {
            result.badRequest("Pipeline counters should be positive.");
            return new ArrayList<>();
        }

        if (fromCounter > toCounter) {
            return revisionsBetween(pipelineName, toCounter, fromCounter, username, result, showBisect);
        }

        if (!showBisect) {
            if (fromCounter != 0) {
                Pipeline toPipeline = pipelineDao.findPipelineByNameAndCounter(pipelineName, toCounter);
                Pipeline fromPipeline = pipelineDao.findPipelineByNameAndCounter(pipelineName, fromCounter);
                if (toPipeline.isBisect() || fromPipeline.isBisect()) {
                    return new ArrayList<>();
                }
            }
        }

        List<MaterialRevision> allMaterialRevisions = modificationsPerMaterialBetween(pipelineName, fromCounter, toCounter);
        return filterReachableFingerprintHolders(allMaterialRevisions, materialRevision -> materialRevision.getMaterial().getFingerprint(), pipelineName, username);
    }

    private <T> List<T> filterReachableFingerprintHolders(List<T> allFingerprintHolders, final FingerprintLoader<T> fingerprintLoader, String pipelineName, Username username) {
        PipelineConfigDependencyGraph graph = goConfigService.upstreamDependencyGraphOf(pipelineName);
        Set<String> allMaterialFingerprints = graph.allMaterialFingerprints();
        Set<String> reachableMaterialfingerprints = populateReachableFingerprints(graph, username);
        return filterFingerprintHolders(allFingerprintHolders, reachableMaterialfingerprints, allMaterialFingerprints, fingerprintLoader);
    }

    private Set<String> populateReachableFingerprints(PipelineConfigDependencyGraph graph, Username username) {
        Set<String> fingerprints = new HashSet<>();
        populateViewableMaterialsStartingAt(graph, username, fingerprints);
        return fingerprints;
    }

    private interface FingerprintLoader<T> {
        String getFingerprint(T t);
    }

    private <T> List<T> filterFingerprintHolders(List<T> fingerprintHolders, Set<String> reachableFingerprints, Set<String> allMaterialFingerprints, FingerprintLoader<T> fingerprintLoader) {
        List<T> results = new ArrayList<>();
        for (T fingerprintHolder : fingerprintHolders) {
            String fingerprint = fingerprintLoader.getFingerprint(fingerprintHolder);
            if (reachableFingerprints.contains(fingerprint)) {
                results.add(fingerprintHolder);
            } else {
                if (!allMaterialFingerprints.contains(fingerprint)) {
                    results.add(fingerprintHolder);
                }
            }
        }
        return results;
    }

    private void populateViewableMaterialsStartingAt(PipelineConfigDependencyGraph graph, Username username, Set<String> fingerprints) {
        for (MaterialConfig materialConfig : graph.getCurrent().materialConfigs()) {
            fingerprints.add(materialConfig.getFingerprint());
        }
        for (PipelineConfigDependencyGraph upstream : graph.getUpstreamDependencies()) {
            if (canView(username, upstream.getCurrent())) {
                populateViewableMaterialsStartingAt(upstream, username, fingerprints);
            }
        }
    }

    private boolean canView(Username username, PipelineConfig pipeline) {
        return securityService.hasViewPermissionForPipeline(username, CaseInsensitiveString.str(pipeline.name()));
    }

    private List<MaterialRevision> modificationsPerMaterialBetween(String pipelineName, Integer fromCounter, Integer toCounter) {
        List<Modification> modifications = materialRepository.getModificationsForPipelineRange(pipelineName, fromCounter, toCounter);
        return deduplicateRevisionsForMaterial(modifications);
    }

    private List<MaterialRevision> deduplicateMaterialRevisionsForCommonMaterials(List<MaterialRevision> materialRevisions) {
        List<Modification> modificationsWithDuplicates = new ArrayList<>();
        for (MaterialRevision revision : materialRevisions) {
            for (Modification modification : revision.getModifications()) {
                if (!modificationsWithDuplicates.contains(modification)) {//change this with a better data-structure so lookup is not O(n)
                    modificationsWithDuplicates.add(modification);
                }
            }

        }
        return deduplicateRevisionsForMaterial(modificationsWithDuplicates);
    }

    private List<MaterialRevision> deduplicateRevisionsForMaterial(Collection<Modification> modifications) {
        Map<Material, Modifications> grouped = groupModsByMaterial(modifications);
        return toMaterialRevisionList(grouped);
    }

    Map<Material, Modifications> groupModsByMaterial(Collection<Modification> modifications) {
        Map<Material, Modifications> grouped = new LinkedHashMap<>();
        for (Modification modification : modifications) {
            Material material = modification.getMaterialInstance().toOldMaterial(null, null, null);
            Modifications mods = mapContainsMaterialWithFingerprint(grouped, material.getFingerprint());
            if (mods == null) {
                mods = new Modifications();
                grouped.put(material, mods);
            }
            mods.add(modification);
        }
        return grouped;
    }

    private Modifications mapContainsMaterialWithFingerprint(Map<Material, Modifications> grouped, String fingerPrint) {
        for (Material material : grouped.keySet()) {
            if (material.getFingerprint().equals(fingerPrint)) {
                return grouped.get(material);
            }
        }
        return null;
    }

    private List<MaterialRevision> toMaterialRevisionList(Map<Material, Modifications> map) {
        List<MaterialRevision> materialRevisionsAcrossPipelines = new ArrayList<>();
        for (Map.Entry<Material, Modifications> materialToModifications : map.entrySet()) {
            Modifications modifications = new Modifications(new ArrayList<>(materialToModifications.getValue()));
            materialRevisionsAcrossPipelines.add(new MaterialRevision(materialToModifications.getKey(), modifications));
        }
        return materialRevisionsAcrossPipelines;
    }

    public Map<Long, List<ModificationForPipeline>> modificationsOfPipelines(List<Long> pipelineIds, String pipelineName, Username username) {
        Map<Long, List<ModificationForPipeline>> modificationsForPipelineIds = materialRepository.findModificationsForPipelineIds(pipelineIds);

        PipelineConfigDependencyGraph graph = goConfigService.upstreamDependencyGraphOf(pipelineName);
        Set<String> allMaterialFingerprints = graph.allMaterialFingerprints();
        Set<String> reachableMaterialfingerprints = populateReachableFingerprints(graph, username);
        FingerprintLoader<ModificationForPipeline> loader = ModificationForPipeline::getMaterialFingerprint;

        for (Map.Entry<Long, List<ModificationForPipeline>> pipelineIdAndModifications : modificationsForPipelineIds.entrySet()) {
            List<ModificationForPipeline> visibleModifications = filterFingerprintHolders(pipelineIdAndModifications.getValue(), reachableMaterialfingerprints, allMaterialFingerprints, loader);
            pipelineIdAndModifications.setValue(visibleModifications);
        }
        return modificationsForPipelineIds;
    }
}
