/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.server.service.dd;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.PipelineTimelineEntry;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.thoughtworks.go.server.service.dd.DependencyFanInNode.RevisionAlteration.ALL_OPTIONS_EXHAUSTED;
import static com.thoughtworks.go.server.service.dd.NoCompatibleUpstreamRevisionsException.doesNotHaveValidRevisions;
import static com.thoughtworks.go.server.service.dd.NoCompatibleUpstreamRevisionsException.failedToFindCompatibleRevision;

public class FanInGraph {
    private final PipelineDao pipelineDao;
    private final CruiseConfig cruiseConfig;
    private final MaterialRepository materialRepository;
    private final MaterialConfigConverter materialConfigConverter;

    private final Map<String, FanInNode<?>> nodes = new HashMap<>();
    private final Map<String, MaterialConfig> fingerprintScmMaterialMap = new HashMap<>();
    private final Map<String, DependencyMaterialConfig> fingerprintDepMaterialMap = new HashMap<>();
    private final Map<DependencyMaterialConfig, Set<String>> dependencyMaterialFingerprintMap = new HashMap<>();

    private final DependencyFanInNode root;
    private final CaseInsensitiveString pipelineName;
    private final IntSupplier maxBackTrackLimit;

    public FanInGraph(CruiseConfig cruiseConfig, CaseInsensitiveString root, MaterialRepository materialRepository, PipelineDao pipelineDao,
                      MaterialConfigConverter materialConfigConverter, IntSupplier maxBackTrackLimit) {
        this.cruiseConfig = cruiseConfig;
        this.materialRepository = materialRepository;
        this.pipelineDao = pipelineDao;
        this.pipelineName = root;
        this.maxBackTrackLimit = maxBackTrackLimit;
        this.materialConfigConverter = materialConfigConverter;

        PipelineConfig target = cruiseConfig.pipelineConfigByName(root);
        this.root = (DependencyFanInNode) FanInNode.create(new DependencyMaterialConfig(target.name(), target.first().name()));

        buildGraph(target);
    }

    private void buildGraph(PipelineConfig target) {
        nodes.put(this.root.materialConfig.getFingerprint(), this.root);
        final Set<String> scmMaterials = new HashSet<>();
        buildRestOfTheGraph(this.root, target, scmMaterials, new HashSet<>());
        dependencyMaterialFingerprintMap.put(this.root.materialConfig, scmMaterials);
    }

    private void buildRestOfTheGraph(DependencyFanInNode root, PipelineConfig target, Set<String> scmMaterialSet, Set<DependencyMaterialConfig> visitedNodes) {
        for (MaterialConfig material : target.materialConfigs()) {
            FanInNode<?> node = nodes.computeIfAbsent(material.getFingerprint(), k -> FanInNode.create(material));
            root.addChild(node);
            if (node instanceof DependencyFanInNode) {
                DependencyMaterialConfig dependencyMaterial = (DependencyMaterialConfig) material;
                fingerprintDepMaterialMap.put(dependencyMaterial.getFingerprint(), dependencyMaterial);
                handleDependencyMaterial(scmMaterialSet, dependencyMaterial, (DependencyFanInNode) node, visitedNodes);
            } else {
                handleScmMaterial(scmMaterialSet, material);
            }
        }
    }

    private void handleScmMaterial(Set<String> scmMaterialSet, MaterialConfig material) {
        final String fingerprint = material.getFingerprint();
        scmMaterialSet.add(fingerprint);
        fingerprintScmMaterialMap.put(fingerprint, material);
    }

    private void handleDependencyMaterial(Set<String> scmMaterialSet, DependencyMaterialConfig depMaterial, DependencyFanInNode node, Set<DependencyMaterialConfig> visitedNodes) {
        if (visitedNodes.contains(depMaterial)) {
            scmMaterialSet.addAll(dependencyMaterialFingerprintMap.get(depMaterial));
            return;
        }
        visitedNodes.add(depMaterial);

        final Set<String> scmMaterialFingerprintSet = new HashSet<>();
        buildRestOfTheGraph(node, cruiseConfig.pipelineConfigByName(depMaterial.getPipelineName()), scmMaterialFingerprintSet, visitedNodes);
        dependencyMaterialFingerprintMap.put(depMaterial, scmMaterialFingerprintSet);
        scmMaterialSet.addAll(scmMaterialFingerprintSet);
    }

    @TestOnly
    List<ScmMaterialConfig> getScmMaterials() {
        List<ScmMaterialConfig> scmMaterials = new ArrayList<>();
        for (FanInNode<?> node : nodes.values()) {
            if (node.materialConfig instanceof ScmMaterialConfig scmMat) {
                scmMaterials.add(scmMat);
            }
        }
        return scmMaterials;
    }

    private Map<DependencyMaterialConfig, Set<MaterialConfig>> getPipelineScmDepMap() {
        Map<DependencyMaterialConfig, Set<MaterialConfig>> dependencyMaterialListMap = new HashMap<>();

        for (Map.Entry<DependencyMaterialConfig, Set<String>> materialSetEntry : dependencyMaterialFingerprintMap.entrySet()) {
            Set<MaterialConfig> scmMaterials = new HashSet<>();
            for (String fingerprint : materialSetEntry.getValue()) {
                scmMaterials.add(fingerprintScmMaterialMap.get(fingerprint));
            }
            dependencyMaterialListMap.put(materialSetEntry.getKey(), scmMaterials);
        }

        return dependencyMaterialListMap;
    }

    public MaterialRevisions computeRevisions(MaterialRevisions actualRevisions, PipelineTimeline pipelineTimeline) {
        assertAllDirectDependenciesArePresentInInput(actualRevisions, pipelineName);

        FanInNode.ByType children = FanInNode.ByType.from(root.children);

        if (children.isAllScm()) {
            // No fanin required
            return actualRevisions;
        }

        FanInGraphContext context = contextFor(pipelineTimeline);
        root.initialize(context);

        initChildren(children.dep(), pipelineName, context);

        iterateAndMakeAllUniqueScmRevisionsForChildrenSame(children.dep(), pipelineName, context);

        List<MaterialRevision> finalRevisionsForScmChildren = createFinalRevisionsForScmChildren(root.latestPipelineTimelineEntry(context), children.scm(), children.dep());

        List<MaterialRevision> finalRevisionsForDepChildren = createFinalRevisionsForDepChildren(children.dep());

        return new MaterialRevisions(CollectionUtils.union(getMaterialsFromCurrentPipeline(finalRevisionsForScmChildren, actualRevisions), finalRevisionsForDepChildren));
    }

    private List<MaterialRevision> createFinalRevisionsForDepChildren(List<DependencyFanInNode> depChildren) {
        List<MaterialRevision> finalRevisions = new ArrayList<>();
        for (DependencyFanInNode child : depChildren) {
            final List<Modification> modifications = materialRepository.modificationFor(child.currentRevision);
            if (modifications.isEmpty()) {
                throw new NoModificationsPresentForDependentMaterialException(child.currentRevision.stageLocator());
            }
            finalRevisions.add(new MaterialRevision(materialConfigConverter.toMaterial(child.materialConfig), modifications));
        }
        return finalRevisions;
    }

    private List<MaterialRevision> createFinalRevisionsForScmChildren(PipelineTimelineEntry latestRootNodeInstance, List<RootFanInNode> scmChildren, List<DependencyFanInNode> depChildren) {
        Set<FaninScmMaterial> scmMaterialsFromDepChildren = scmMaterialsOfDepChildren(depChildren);
        List<MaterialRevision> finalRevisions = new ArrayList<>();

        for (RootFanInNode child : scmChildren) {
            child.setScmRevision(scmMaterialsFromDepChildren);

            MaterialConfig materialConfig = child.materialConfig;
            Material material = materialConfigConverter.toMaterial(materialConfig);
            MaterialRevision revision = new MaterialRevision(material);
            if (latestRootNodeInstance != null) {
                PipelineInstanceModel pipeline = pipelineDao.findPipelineHistoryByNameAndCounter(latestRootNodeInstance.getPipelineName(), latestRootNodeInstance.getCounter());
                for (MaterialRevision materialRevision : pipeline.getCurrentRevisions()) {
                    if (materialRevision.getMaterial().getFingerprint().equals(child.materialConfig.getFingerprint())) {
                        List<Modification> modificationsSince = materialRepository.findModificationsSinceAndUntil(material, materialRevision, child.scmRevisionId());
                        revision.addModifications(modificationsSince);
                        break;
                    }
                }
            }

            if (revision.getModifications().isEmpty() && child.scmRevision.isEmpty()) {
                MaterialRevisions latestRevisions = materialRepository.findLatestRevisions(new MaterialConfigs(materialConfig));
                finalRevisions.addAll(latestRevisions.getRevisions());
            } else if (revision.getModifications().isEmpty()) {
                finalRevisions.add(new MaterialRevision(material, materialRepository.findModificationWithRevision(material, child.scmRevision.get().revision())));
            } else {
                finalRevisions.add(revision);
            }
        }
        return finalRevisions;
    }

    private Set<FaninScmMaterial> scmMaterialsOfDepChildren(List<DependencyFanInNode> depChildren) {
        Set<FaninScmMaterial> allScmMaterials = new HashSet<>();
        for (DependencyFanInNode child : depChildren) {
            allScmMaterials.addAll(child.scmMaterialForCurrentRevision());
        }
        return allScmMaterials;
    }


    private void iterateAndMakeAllUniqueScmRevisionsForChildrenSame(List<DependencyFanInNode> depChildren, CaseInsensitiveString pipelineName, FanInGraphContext context) {
        StageIdFaninScmMaterialPair revisionToSet = getRevisionToSet();
        while (revisionToSet != null) {
            for (DependencyFanInNode child : depChildren) {
                final DependencyFanInNode.RevisionAlteration revisionAlteration = child.setRevisionTo(revisionToSet, context);
                if (revisionAlteration == ALL_OPTIONS_EXHAUSTED) {
                    throw failedToFindCompatibleRevision(pipelineName, child.materialConfig);
                }
            }
            revisionToSet = getRevisionToSet();
        }
    }

    private void initChildren(List<DependencyFanInNode> depChildren, CaseInsensitiveString pipelineName, FanInGraphContext context) {
        for (DependencyFanInNode child : depChildren) {
            child.populateRevisions(pipelineName, context);
        }
    }

    private void assertAllDirectDependenciesArePresentInInput(MaterialRevisions actualRevisions, CaseInsensitiveString pipelineName) {
        Set<String> actualRevFingerprints = StreamSupport
            .stream(actualRevisions.spliterator(), false)
            .map(r -> r.getMaterial().getFingerprint())
            .collect(Collectors.toSet());

        for (FanInNode<?> child : root.children) {
            //The dependency material that is not in 'passed' state will not be found in actual revisions
            if (!actualRevFingerprints.contains(child.materialConfig.getFingerprint())) {
                throw doesNotHaveValidRevisions(pipelineName, child.materialConfig);
            }
        }
    }

    private StageIdFaninScmMaterialPair getRevisionToSet() {
        List<StageIdFaninScmMaterialPair> pIdScmMaterialList = buildPipelineIdScmMaterialMap();

        Collection<StageIdFaninScmMaterialPair> scmRevisionsThatDiffer = findScmRevisionsThatDiffer(pIdScmMaterialList);

        if (!scmRevisionsThatDiffer.isEmpty()) {
            return getSmallestScmRevision(scmRevisionsThatDiffer);
        }

        return null;
    }

    private Collection<StageIdFaninScmMaterialPair> findScmRevisionsThatDiffer(List<StageIdFaninScmMaterialPair> pIdScmMaterialList) {
        for (final StageIdFaninScmMaterialPair pIdScmPair : pIdScmMaterialList) {
            List<StageIdFaninScmMaterialPair> matWithSameFingerprint = pIdScmMaterialList.stream().filter(pIdScmPair::equals).toList();
            Optional<StageIdFaninScmMaterialPair> withDifferentRevision = matWithSameFingerprint.stream()
                .filter(pair ->
                    pair.stageIdentifier() != pIdScmPair.stageIdentifier() && !pair.faninScmMaterial().revision().equals(pIdScmPair.faninScmMaterial().revision())
                ).findFirst();

            if (withDifferentRevision.isPresent()) {
                return matWithSameFingerprint;
            }
        }

        return Collections.emptyList();
    }

    private @NotNull StageIdFaninScmMaterialPair getSmallestScmRevision(Collection<StageIdFaninScmMaterialPair> scmWithDiffVersions) {
        return scmWithDiffVersions
            .stream()
            .min(Comparator.comparing(pair -> pair.faninScmMaterial().revision().date()))
            .orElseThrow(() -> new RuntimeException("Cannot find smallest SCM revision where there are none"));
    }

    private List<StageIdFaninScmMaterialPair> buildPipelineIdScmMaterialMap() {
        return root.children.stream()
            .filter(c -> c instanceof DependencyFanInNode)
            .flatMap(c -> ((DependencyFanInNode) c).getCurrentFaninScmMaterials().stream())
            .toList();
    }

    private FanInGraphContext contextFor(PipelineTimeline pipelineTimeline) {
        return new FanInGraphContext(
            fingerprintScmMaterialMap,
            pipelineTimeline,
            getPipelineScmDepMap(),
            fingerprintDepMaterialMap,
            pipelineDao,
            maxBackTrackLimit);
    }

    private Collection<MaterialRevision> getMaterialsFromCurrentPipeline(List<MaterialRevision> finalRevisionsForScmChildren, MaterialRevisions actualRevisions) {
        List<MaterialRevision> updatedRevisions = new ArrayList<>();
        for (MaterialRevision revisionsForScmChild : finalRevisionsForScmChildren) {
            MaterialRevision originalRevision = actualRevisions.findRevisionUsingMaterialFingerprintFor(revisionsForScmChild.getMaterial());
            updatedRevisions.add(new MaterialRevision(originalRevision.getMaterial(), revisionsForScmChild.isChanged(), revisionsForScmChild.getModifications()));
        }
        return updatedRevisions;
    }
}
