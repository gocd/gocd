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
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.PipelineTimelineEntry;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.service.NoCompatibleUpstreamRevisionsException;
import com.thoughtworks.go.util.Pair;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.thoughtworks.go.server.service.dd.DependencyFanInNode.RevisionAlteration.*;

public class DependencyFanInNode extends FanInNode {
    private static final Logger LOGGER = Logger.getLogger(DependencyFanInNode.class);

    private int totalInstanceCount = Integer.MAX_VALUE;
    private int maxBackTrackLimit = Integer.MAX_VALUE;
    private int currentCount;
    StageIdentifier currentRevision;
    private Map<StageIdentifier, Set<FaninScmMaterial>> stageIdentifierScmMaterial = new LinkedHashMap<>();
    public Set<FanInNode> children = new HashSet<>();
    private MaterialConfigs pipelineMaterials;

    public Set<? extends FaninScmMaterial> stageIdentifierScmMaterialForCurrentRevision() {
        return stageIdentifierScmMaterial.get(currentRevision);
    }

    enum RevisionAlteration {
        NOT_APPLICABLE, SAME_AS_CURRENT_REVISION, ALTERED_TO_CORRECT_REVISION, ALL_OPTIONS_EXHAUSTED, NEED_MORE_REVISIONS
    }

    DependencyFanInNode(MaterialConfig material, MaterialConfigs pipelineMaterials) {
        super(material);
        this.pipelineMaterials = pipelineMaterials;
    }

    public void populateRevisions(CaseInsensitiveString pipelineName, FanInGraphContext context) {
        initialize(context);
        fillNextRevisions(context);
        if (initRevision(context) == ALL_OPTIONS_EXHAUSTED) {
            throw NoCompatibleUpstreamRevisionsException.noValidRevisionsForUpstream(pipelineName, materialConfig);
        }

    }

    private void setCurrentRevision() {
        currentRevision = stageIdentifierScmMaterial.keySet().toArray(new StageIdentifier[0])[0];
    }

    private RevisionAlteration initRevision(FanInGraphContext context) {
        if (!stageIdentifierScmMaterial.isEmpty()) {
            setCurrentRevision();
        } else {
            return handleNeedMoreRevisions(context);
        }

        return ALTERED_TO_CORRECT_REVISION;
    }

    private RevisionAlteration handleNeedMoreRevisions(FanInGraphContext context) {
        while (hasMoreInstances()) {
            fillNextRevisions(context);
            if (!stageIdentifierScmMaterial.isEmpty()) {
                setCurrentRevision();
                return ALTERED_TO_CORRECT_REVISION;
            }
        }
        return ALL_OPTIONS_EXHAUSTED;
    }

    public RevisionAlteration setRevisionTo(StageIdFaninScmMaterialPair revisionToSet, FanInGraphContext context) {
        RevisionAlteration revisionAlteration = alterRevision(revisionToSet, context);
        while (revisionAlteration == NEED_MORE_REVISIONS) {
            fillNextRevisions(context);
            revisionAlteration = alterRevision(revisionToSet, context);
        }
        return revisionAlteration;
    }

    public void initialize(FanInGraphContext context) {
        totalInstanceCount = context.pipelineTimeline.instanceCount(((DependencyMaterialConfig) materialConfig).getPipelineName());
        maxBackTrackLimit = context.maxBackTrackLimit;
    }

    public PipelineTimelineEntry latestPipelineTimelineEntry(FanInGraphContext context) {
        if (totalInstanceCount == 0) {
            return null;
        }
        return context.pipelineTimeline.instanceFor(((DependencyMaterialConfig) materialConfig).getPipelineName(), totalInstanceCount - 1);
    }

    private void fillNextRevisions(FanInGraphContext context) {
        if (!hasMoreInstances()) {
            return;
        }
        int batchOffset = currentCount;
        for (int i = 1; i <= context.revBatchCount; ++i) {
            final Pair<StageIdentifier, List<FaninScmMaterial>> sIdScmPair = getRevisionNthFor(i + batchOffset, context);
            if (!validateAllScmRevisionsAreSameWithinAFingerprint(sIdScmPair)) {
                ++currentCount;
                if (!hasMoreInstances()) {
                    break;
                }
                continue;
            }
            validateIfRevisionMatchesTheCurrentConfigAndUpdateTheMaterialMap(context, sIdScmPair);
            if (!hasMoreInstances()) {
                break;
            }
        }
    }

    private Pair<StageIdentifier, List<FaninScmMaterial>> getRevisionNthFor(int n, FanInGraphContext context) {
        List<FaninScmMaterial> scmMaterials = new ArrayList<>();
        PipelineTimeline pipelineTimeline = context.pipelineTimeline;
        Queue<PipelineTimelineEntry.Revision> revisionQueue = new ConcurrentLinkedQueue<>();
        DependencyMaterialConfig dependencyMaterial = (DependencyMaterialConfig) materialConfig;
        PipelineTimelineEntry entry = pipelineTimeline.instanceFor(dependencyMaterial.getPipelineName(), totalInstanceCount - n);

        Set<CaseInsensitiveString> visitedNodes = new HashSet<>();

        StageIdentifier dependentStageIdentifier = dependentStageIdentifier(context, entry, CaseInsensitiveString.str(dependencyMaterial.getStageName()));
        Map<String, Modifications> modificationsMap = new HashMap<>();
        if (!StageIdentifier.NULL.equals(dependentStageIdentifier)) {
            for (String scmFingerprint: context.fingerprintScmMaterialMap.keySet()) {
                modificationsMap.put(scmFingerprint, modificationsForFingerprint(scmFingerprint, dependentStageIdentifier, context.pipelineDao));
            }
            addToRevisionQueue(entry, revisionQueue, scmMaterials, context, visitedNodes, modificationsMap);
        } else {
            return null;
        }
        while (!revisionQueue.isEmpty()) {
            PipelineTimelineEntry.Revision revision = revisionQueue.poll();
            DependencyMaterialRevision dmr = DependencyMaterialRevision.create(revision.revision, null);
            PipelineTimelineEntry pte = pipelineTimeline.getEntryFor(new CaseInsensitiveString(dmr.getPipelineName()), dmr.getPipelineCounter());
            addToRevisionQueue(pte, revisionQueue, scmMaterials, context, visitedNodes, modificationsMap);
        }

        return new Pair<>(dependentStageIdentifier, scmMaterials);
    }

    private boolean validateAllScmRevisionsAreSameWithinAFingerprint(Pair<StageIdentifier, List<FaninScmMaterial>> pIdScmPair) {
        if (pIdScmPair == null) {
            return false;
        }
        Map<FaninScmMaterial, PipelineTimelineEntry.Revision> versionsByMaterial = new HashMap<>();
        List<FaninScmMaterial> scmMaterialList = pIdScmPair.last();
        for (final FaninScmMaterial scmMaterial : scmMaterialList) {
            PipelineTimelineEntry.Revision revision = versionsByMaterial.get(scmMaterial);
            if (revision == null) {
                versionsByMaterial.put(scmMaterial, scmMaterial.revision);
            } else if (!revision.equals(scmMaterial.revision)) {
                return false;
            }
        }
        return true;
    }

    private void validateIfRevisionMatchesTheCurrentConfigAndUpdateTheMaterialMap(FanInGraphContext context, Pair<StageIdentifier, List<FaninScmMaterial>> stageIdentifierScmPair) {
        final Set<MaterialConfig> currentScmMaterials = context.pipelineScmDepMap.get(materialConfig);
        final Set<FaninScmMaterial> scmMaterials = new HashSet<>(stageIdentifierScmPair.last());
        final Set<String> currentScmFingerprint = new HashSet<>();
        for (MaterialConfig currentScmMaterial : currentScmMaterials) {
            currentScmFingerprint.add(currentScmMaterial.getFingerprint());
        }
        final Set<String> scmMaterialsFingerprint = new HashSet<>();
        for (FaninScmMaterial scmMaterial : scmMaterials) {
            scmMaterialsFingerprint.add(scmMaterial.fingerprint);
        }
        final Collection commonMaterials = CollectionUtils.intersection(currentScmFingerprint, scmMaterialsFingerprint);
        if (commonMaterials.size() == scmMaterials.size() && commonMaterials.size() == currentScmMaterials.size()) {
            stageIdentifierScmMaterial.put(stageIdentifierScmPair.first(), scmMaterials);
            ++currentCount;
        } else {
            Collection disjunctionWithConfig = CollectionUtils.disjunction(currentScmFingerprint, commonMaterials);
            Collection disjunctionWithInstance = CollectionUtils.disjunction(scmMaterialsFingerprint, commonMaterials);

            LOGGER.warn(String.format("[Fan-in] - Incompatible materials for %s. Config: %s. Instance: %s.", stageIdentifierScmPair.first().getStageLocator(), disjunctionWithConfig, disjunctionWithInstance));

            //This is it. We will not go beyond this revision in history
            totalInstanceCount = currentCount;
        }
    }

    private StageIdentifier dependentStageIdentifier(FanInGraphContext context, PipelineTimelineEntry entry, final String stageName) {
        return context.pipelineDao.latestPassedStageIdentifier(entry.getId(), stageName);
    }

    private void addToRevisionQueue(PipelineTimelineEntry entry, Queue<PipelineTimelineEntry.Revision> revisionQueue, List<FaninScmMaterial> scmMaterials,
                                    FanInGraphContext context, Set<CaseInsensitiveString> visitedNodes, Map<String, Modifications> modificationsMap) {
        for (Map.Entry<String, List<PipelineTimelineEntry.Revision>> revisionList : entry.revisions().entrySet()) {
            String fingerprint = revisionList.getKey();
            PipelineTimelineEntry.Revision revision = revisionList.getValue().get(0);
            if (isScmMaterial(fingerprint, context)) {
                Modifications modifications = modificationsMap.get(fingerprint);
                DependencyFanInNode node = childByPipelineName(this, new CaseInsensitiveString(entry.getPipelineName()));
                FaninScmMaterial scmMaterial = new FaninScmMaterial(fingerprint, revision);
                if (!scmMaterials.contains(scmMaterial) || node.changeExistsInNodeOrChildren(modifications, fingerprint)) {
                    scmMaterials.add(scmMaterial);
                }
                continue;
            }

            if (isDependencyMaterial(fingerprint, context) && !visitedNodes.contains(new CaseInsensitiveString(revision.revision))) {
                revisionQueue.add(revision);
                visitedNodes.add(new CaseInsensitiveString(revision.revision));
            }
        }
    }

    private boolean isDependencyMaterial(String fingerprint, FanInGraphContext context) {
        return context.fingerprintDepMaterialMap.containsKey(fingerprint);
    }

    private boolean isScmMaterial(String fingerprint, FanInGraphContext context) {
        return context.fingerprintScmMaterialMap.containsKey(fingerprint);
    }

    private boolean hasMoreInstances() {
        if (currentCount > maxBackTrackLimit) {
            throw new MaxBackTrackLimitReachedException(materialConfig);
        }
        return currentCount < totalInstanceCount;
    }

    private RevisionAlteration alterRevision(StageIdFaninScmMaterialPair revisionToSet, FanInGraphContext context) {
        if (currentRevision == revisionToSet.stageIdentifier) {
            return RevisionAlteration.SAME_AS_CURRENT_REVISION;
        }
        if (!stageIdentifierScmMaterial.get(currentRevision).contains(revisionToSet.faninScmMaterial)) {
            return RevisionAlteration.NOT_APPLICABLE;
        }
        List<StageIdentifier> stageIdentifiers = new ArrayList<>(stageIdentifierScmMaterial.keySet());
        int currentRevIndex = stageIdentifiers.indexOf(currentRevision);
        for (int i = currentRevIndex; i < stageIdentifiers.size(); i++) {
            final StageIdentifier key = stageIdentifiers.get(i);
            final List<FaninScmMaterial> materials = new ArrayList<>(stageIdentifierScmMaterial.get(key));
            final int index = materials.indexOf(revisionToSet.faninScmMaterial);
            if (index == -1) {
                return ALL_OPTIONS_EXHAUSTED;
            }
            final FaninScmMaterial faninScmMaterial = materials.get(index);
            if (faninScmMaterial.revision.equals(revisionToSet.faninScmMaterial.revision)) {
                currentRevision = key;
                return ALTERED_TO_CORRECT_REVISION;
            }
            if (faninScmMaterial.revision.lessThan(revisionToSet.faninScmMaterial.revision)) {
                currentRevision = key;
                return ALTERED_TO_CORRECT_REVISION;
            }
        }

        if (!hasMoreInstances()) {
            return ALL_OPTIONS_EXHAUSTED;
        }
        return NEED_MORE_REVISIONS;
    }

    public List<StageIdFaninScmMaterialPair> getCurrentFaninScmMaterials() {
        List<StageIdFaninScmMaterialPair> stageIdScmPairs = new ArrayList<>();
        Set<FaninScmMaterial> faninScmMaterials = stageIdentifierScmMaterial.get(currentRevision);
        for (FaninScmMaterial faninScmMaterial : faninScmMaterials) {
            StageIdFaninScmMaterialPair pIdScmPair = new StageIdFaninScmMaterialPair(currentRevision, faninScmMaterial);
            stageIdScmPairs.add(pIdScmPair);
        }
        return stageIdScmPairs;
    }

    public boolean changeExistsInNodeOrChildren(Modifications modifications, String scmFingerprint) {
        MaterialConfig materialConfig = pipelineMaterials.getByNonUniqueFingerPrint(scmFingerprint);
        boolean changeExistsForNode = materialConfig != null && !modifications.shouldBeIgnoredByFilterIn(materialConfig);
        return changeExistsForNode || changeExistsInAnyOfChildren(modifications, scmFingerprint);
    }

    public boolean changeExistsInAnyOfChildren(Modifications modifications, String scmFingerprint) {
        for (FanInNode child: children) {
            if (child instanceof DependencyFanInNode &&
                    ((DependencyFanInNode)child).changeExistsInNodeOrChildren(modifications, scmFingerprint)) {
                return true;
            }
        }
        return false;
    }

    public Modifications modificationsForFingerprint(String fingerprint, StageIdentifier stageId, PipelineDao pipelineDao) {
        return modificationsForFingerprint(fingerprint, stageId, pipelineDao, new ArrayList<>());
    }

    private Modifications modificationsForFingerprint(String fingerprint, StageIdentifier stageId,
                                                     PipelineDao pipelineDao, List<DependencyFanInNode> visitedNodes) {
        PipelineInstanceModel pipeline = pipelineDao.findPipelineHistoryByNameAndCounter(stageId.getPipelineName(), stageId.getPipelineCounter());
        MaterialRevisions materialRevisions = pipeline.getBuildCause().getMaterialRevisions();
        MaterialRevision scmRevision = materialRevisions.findRevisionForFingerPrint(fingerprint);
        if (scmRevision != null) {
            return scmRevision.getModifications();
        }
        for (FanInNode child: children) {
            if (child instanceof DependencyFanInNode && !visitedNodes.contains(child)) {
                DependencyFanInNode dependencyNode = (DependencyFanInNode) child;
                visitedNodes.add(dependencyNode);
                MaterialRevision revision = materialRevisions.findRevisionFor(child.materialConfig);
                if (revision != null) {
                    DependencyMaterialRevision nodeRevision = (DependencyMaterialRevision) revision.getRevision();
                    StageIdentifier stageIdentifier = new StageIdentifier(nodeRevision.getPipelineName(), nodeRevision.getPipelineCounter(),
                            nodeRevision.getStageName(), String.valueOf(nodeRevision.getStageCounter()));
                    Modifications modifications = dependencyNode.modificationsForFingerprint(fingerprint, stageIdentifier, pipelineDao, visitedNodes);
                    if (modifications != null) {
                        return modifications;
                    }
                }
            }
        }
        return null;
    }


    public DependencyFanInNode childByPipelineName(DependencyFanInNode node, CaseInsensitiveString pipelineName) {
        return childByPipelineName(node, pipelineName, new ArrayList<>());
    }

    private DependencyFanInNode childByPipelineName(DependencyFanInNode node, CaseInsensitiveString pipelineName, List<DependencyFanInNode> visitedNodes) {
        if (((DependencyMaterialConfig) node.materialConfig).getPipelineName().equals(pipelineName)) {
            return node;
        }
        for (FanInNode child: node.children) {
            if (child instanceof DependencyFanInNode && !visitedNodes.contains(child)) {
                visitedNodes.add(node);
                DependencyFanInNode childMatchingPipelineName = childByPipelineName((DependencyFanInNode) child, pipelineName, visitedNodes);
                if (childMatchingPipelineName != null) {
                    return childMatchingPipelineName;
                }
            }
        }
        return null;
    }
}
