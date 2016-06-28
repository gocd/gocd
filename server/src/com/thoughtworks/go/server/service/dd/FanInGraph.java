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
import com.thoughtworks.go.server.service.NoCompatibleUpstreamRevisionsException;
import com.thoughtworks.go.server.service.NoModificationsPresentForDependentMaterialException;
import com.thoughtworks.go.util.Pair;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;

import java.util.*;

import static com.thoughtworks.go.server.service.dd.DependencyFanInNode.RevisionAlteration.ALL_OPTIONS_EXHAUSTED;

public class FanInGraph {
    private static final int REVISION_BUFFER_SIZE = 5;
    private static final int DEFAULT_BACK_TRACK_LIMIT = 100;

    private final PipelineDao pipelineDao;
    private final CruiseConfig cruiseConfig;
    private final MaterialRepository materialRepository;
    private MaterialConfigConverter materialConfigConverter;

    private final Map<String, FanInNode> nodes = new HashMap<>();
    private final Map<String, MaterialConfig> fingerprintScmMaterialMap = new HashMap<>();
    private final Map<String, DependencyMaterialConfig> fingerprintDepMaterialMap = new HashMap<>();
    private final Map<DependencyMaterialConfig, Set<String>> dependencyMaterialFingerprintMap = new HashMap<>();

    private final DependencyFanInNode root;
    private final CaseInsensitiveString pipelineName;
    private final SystemEnvironment systemEnvironment;
    private FanInEventListener fanInEventListener;

    public FanInGraph(CruiseConfig cruiseConfig, CaseInsensitiveString root, MaterialRepository materialRepository, PipelineDao pipelineDao, SystemEnvironment systemEnvironment,
                      MaterialConfigConverter materialConfigConverter) {
        this.cruiseConfig = cruiseConfig;
        this.materialRepository = materialRepository;
        this.pipelineDao = pipelineDao;
        this.pipelineName = root;
        this.systemEnvironment = systemEnvironment;
        this.materialConfigConverter = materialConfigConverter;

        PipelineConfig target = cruiseConfig.pipelineConfigByName(root);
        this.root = (DependencyFanInNode) FanInNodeFactory.create(new DependencyMaterialConfig(target.name(), target.get(0).name()));

        buildGraph(target);
    }

    private void buildGraph(PipelineConfig target) {
        nodes.put(this.root.materialConfig.getFingerprint(), this.root);
        final Set<String> scmMaterials = new HashSet<>();
        buildRestOfTheGraph(this.root, target, scmMaterials, new HashSet<DependencyMaterialConfig>());
        dependencyMaterialFingerprintMap.put((DependencyMaterialConfig) this.root.materialConfig, scmMaterials);
    }

    private void buildRestOfTheGraph(DependencyFanInNode root, PipelineConfig target, Set<String> scmMaterialSet, Set<DependencyMaterialConfig> visitedNodes) {
        for (MaterialConfig material : target.materialConfigs()) {
            FanInNode node = createNode(material);
            root.children.add(node);
            node.parents.add(root);
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

    private FanInNode createNode(MaterialConfig material) {
        FanInNode node = nodes.get(material.getFingerprint());
        if (node == null) {
            node = FanInNodeFactory.create(material);
            nodes.put(material.getFingerprint(), node);
        }
        return node;
    }

    @Deprecated
    public void setFanInEventListener(FanInEventListener fanInEventListener) {
        this.fanInEventListener = fanInEventListener;
    }

    //Used in test Only
    List<ScmMaterialConfig> getScmMaterials() {
        List<ScmMaterialConfig> scmMaterials = new ArrayList<>();
        for (FanInNode node : nodes.values()) {
            if (node.materialConfig instanceof ScmMaterialConfig) {
                scmMaterials.add((ScmMaterialConfig) node.materialConfig);
            }
        }
        return scmMaterials;
    }

    public Map<DependencyMaterialConfig, Set<MaterialConfig>> getPipelineScmDepMap() {
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

        Pair<List<RootFanInNode>, List<DependencyFanInNode>> scmAndDepMaterialsChildren = getScmAndDepMaterialsChildren();
        List<RootFanInNode> scmChildren = scmAndDepMaterialsChildren.first();
        List<DependencyFanInNode> depChildren = scmAndDepMaterialsChildren.last();

        if (depChildren.isEmpty()) {
            //No fanin required all are SCMs
            return actualRevisions;
        }

        FanInGraphContext context = buildContext(pipelineTimeline);
        root.initialize(context);

        initChildren(depChildren, pipelineName, context);

        if (fanInEventListener != null) {
            fanInEventListener.iterationComplete(0, depChildren);
        }

        iterateAndMakeAllUniqueScmRevisionsForChildrenSame(depChildren, pipelineName, context);

        List<MaterialRevision> finalRevisionsForScmChildren = createFinalRevisionsForScmChildren(root.latestPipelineTimelineEntry(context), scmChildren, depChildren);

        List<MaterialRevision> finalRevisionsForDepChildren = createFinalRevisionsForDepChildren(depChildren);

        return new MaterialRevisions(CollectionUtils.union(getMaterialsFromCurrentPipeline(finalRevisionsForScmChildren, actualRevisions), finalRevisionsForDepChildren));
    }

    //This whole method is repeated for reporting and it does not use actual revisions for determining final revisions
    //Used in rails view
    //Do not delete
    //Ramraj ge salute
    //Srikant & Sachin
    @Deprecated
    public Collection<MaterialRevision> computeRevisionsForReporting(CaseInsensitiveString pipelineName, PipelineTimeline pipelineTimeline) {
        Pair<List<RootFanInNode>, List<DependencyFanInNode>> scmAndDepMaterialsChildren = getScmAndDepMaterialsChildren();
        List<RootFanInNode> scmChildren = scmAndDepMaterialsChildren.first();
        List<DependencyFanInNode> depChildren = scmAndDepMaterialsChildren.last();

        if (depChildren.isEmpty()) {
            //No fanin required all are SCMs
            return null;
        }

        FanInGraphContext context = buildContext(pipelineTimeline);
        root.initialize(context);

        initChildren(depChildren, pipelineName, context);

        iterateAndMakeAllUniqueScmRevisionsForChildrenSame(depChildren, pipelineName, context);

        List<MaterialRevision> finalRevisionsForScmChildren = createFinalRevisionsForScmChildren(root.latestPipelineTimelineEntry(context), scmChildren, depChildren);

        List<MaterialRevision> finalRevisionsForDepChildren = createFinalRevisionsForDepChildren(depChildren);

        return CollectionUtils.union(finalRevisionsForScmChildren, finalRevisionsForDepChildren);
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
                        List<Modification> modificationsSince = materialRepository.findModificationsSinceAndUptil(material, materialRevision, child.scmRevision);
                        revision.addModifications(modificationsSince);
                        break;
                    }
                }
            }

            if (revision.getModifications().isEmpty() && child.scmRevision == null) {
                MaterialRevisions latestRevisions = materialRepository.findLatestRevisions(new MaterialConfigs(materialConfig));
                finalRevisions.addAll(latestRevisions.getRevisions());
                continue;
            }

            if (revision.getModifications().isEmpty()) {
                revision = new MaterialRevision(material, materialRepository.findModificationWithRevision(material, child.scmRevision.revision));
            }

            finalRevisions.add(revision);
        }
        return finalRevisions;
    }

    private Set<FaninScmMaterial> scmMaterialsOfDepChildren(List<DependencyFanInNode> depChildren) {
        Set<FaninScmMaterial> allScmMaterials = new HashSet<>();
        for (DependencyFanInNode child : depChildren) {
            allScmMaterials.addAll(child.stageIdentifierScmMaterialForCurrentRevision());
        }
        return allScmMaterials;
    }


    private Pair<List<RootFanInNode>, List<DependencyFanInNode>> getScmAndDepMaterialsChildren() {
        List<RootFanInNode> scmMaterials = new ArrayList<>();
        List<DependencyFanInNode> depMaterials = new ArrayList<>();
        for (FanInNode child : root.children) {
            if (child instanceof RootFanInNode) {
                scmMaterials.add((RootFanInNode) child);
            } else {
                depMaterials.add((DependencyFanInNode) child);
            }
        }
        return new Pair<>(scmMaterials, depMaterials);
    }

    private void iterateAndMakeAllUniqueScmRevisionsForChildrenSame(List<DependencyFanInNode> depChildren, CaseInsensitiveString pipelineName, FanInGraphContext context) {
        StageIdFaninScmMaterialPair revisionToSet = getRevisionToSet();
        int i = 1;
        while (revisionToSet != null) {
            for (DependencyFanInNode child : depChildren) {
                final DependencyFanInNode.RevisionAlteration revisionAlteration = child.setRevisionTo(revisionToSet, context);
                if (revisionAlteration == ALL_OPTIONS_EXHAUSTED) {
                    throw NoCompatibleUpstreamRevisionsException.failedToFindCompatibleRevision(pipelineName, child.materialConfig);
                }
            }

            if (fanInEventListener != null) {
                fanInEventListener.iterationComplete(i, depChildren);
            }

            i++;
            revisionToSet = getRevisionToSet();
        }
    }

    private void initChildren(List<DependencyFanInNode> depChildren, CaseInsensitiveString pipelineName, FanInGraphContext context) {
        for (DependencyFanInNode child : depChildren) {
            child.populateRevisions(pipelineName, context);
        }
    }

    private void assertAllDirectDependenciesArePresentInInput(MaterialRevisions actualRevisions, CaseInsensitiveString pipelineName) {
        Collection<String> actualRevFingerprints = CollectionUtils.collect(actualRevisions.iterator(), new Transformer() {
            @Override
            public Object transform(Object actualRevision) {
                return ((MaterialRevision) actualRevision).getMaterial().getFingerprint();
            }
        });

        for (FanInNode child : root.children) {
            //The dependency material that is not in 'passed' state will not be found in actual revisions
            if (!actualRevFingerprints.contains(child.materialConfig.getFingerprint())) {
                throw NoCompatibleUpstreamRevisionsException.doesNotHaveValidRevisions(pipelineName, child.materialConfig);
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
            final Collection<StageIdFaninScmMaterialPair> matWithSameFingerprint = CollectionUtils.select(pIdScmMaterialList, new Predicate() {
                @Override
                public boolean evaluate(Object o) {
                    return pIdScmPair.equals(o);
                }
            });

            boolean diffRevFound = false;
            for (StageIdFaninScmMaterialPair pair : matWithSameFingerprint) {
                if (pair.stageIdentifier == pIdScmPair.stageIdentifier) {
                    continue;
                }
                if (pair.faninScmMaterial.revision.equals(pIdScmPair.faninScmMaterial.revision)) {
                    continue;
                }
                diffRevFound = true;
                break;
            }

            if (diffRevFound) {
                return matWithSameFingerprint;
            }
        }

        return Collections.EMPTY_LIST;
    }

    private StageIdFaninScmMaterialPair getSmallestScmRevision(Collection<StageIdFaninScmMaterialPair> scmWithDiffVersions) {
        ArrayList<StageIdFaninScmMaterialPair> materialPairList = new ArrayList<>(scmWithDiffVersions);
        Collections.sort(materialPairList, new Comparator<StageIdFaninScmMaterialPair>() {
            @Override
            public int compare(StageIdFaninScmMaterialPair pair1, StageIdFaninScmMaterialPair pair2) {
                final PipelineTimelineEntry.Revision rev1 = pair1.faninScmMaterial.revision;
                final PipelineTimelineEntry.Revision rev2 = pair2.faninScmMaterial.revision;
                return rev1.date.compareTo(rev2.date);
            }
        });
        return materialPairList.get(0);
    }

    private List<StageIdFaninScmMaterialPair> buildPipelineIdScmMaterialMap() {
        List<StageIdFaninScmMaterialPair> stageIdScmPairs = new ArrayList<>();
        for (FanInNode child : root.children) {
            if (child instanceof DependencyFanInNode) {
                stageIdScmPairs.addAll(((DependencyFanInNode) child).getCurrentFaninScmMaterials());
            }
        }
        return stageIdScmPairs;
    }

    private FanInGraphContext buildContext(PipelineTimeline pipelineTimeline) {
        FanInGraphContext context = new FanInGraphContext();
        context.revBatchCount = REVISION_BUFFER_SIZE;
        context.pipelineTimeline = pipelineTimeline;
        context.fingerprintScmMaterialMap = fingerprintScmMaterialMap;
        context.pipelineScmDepMap = getPipelineScmDepMap();
        context.fingerprintDepMaterialMap = fingerprintDepMaterialMap;
        context.pipelineDao = pipelineDao;
        context.maxBackTrackLimit = systemEnvironment.get(SystemEnvironment.RESOLVE_FANIN_MAX_BACK_TRACK_LIMIT);
        return context;
    }

    private Collection getMaterialsFromCurrentPipeline(List<MaterialRevision> finalRevisionsForScmChildren, MaterialRevisions actualRevisions) {
        List<MaterialRevision> updatedRevisions = new ArrayList<>();
        for (MaterialRevision revisionsForScmChild : finalRevisionsForScmChildren) {
            MaterialRevision originalRevision = actualRevisions.findRevisionUsingMaterialFingerprintFor(revisionsForScmChild.getMaterial());
            updatedRevisions.add(new MaterialRevision(originalRevision.getMaterial(), revisionsForScmChild.isChanged(), revisionsForScmChild.getModifications()));
        }
        return updatedRevisions;
    }
}
