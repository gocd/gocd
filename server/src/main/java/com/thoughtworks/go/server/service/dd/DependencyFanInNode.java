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
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.domain.PipelineTimelineEntry;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.util.Pair;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import static com.thoughtworks.go.server.service.dd.DependencyFanInNode.RevisionAlteration.*;

class DependencyFanInNode extends FanInNode<DependencyMaterialConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyFanInNode.class);
    private static final int REVISION_BUFFER_SIZE = 5;

    final Set<FanInNode<?>> children = new HashSet<>();

    StageIdentifier currentRevision;
    private final Map<StageIdentifier, Set<FaninScmMaterial>> scmMaterialsByStageId = new LinkedHashMap<>();

    private Supplier<Integer> maxBackTrackLimit = () -> Integer.MAX_VALUE;

    private int totalInstanceCount = Integer.MAX_VALUE;
    private int currentCount;

    DependencyFanInNode(DependencyMaterialConfig material) {
        super(material);
    }

    void initialize(FanInGraphContext context) {
        totalInstanceCount = context.pipelineTimeline().instanceCount(materialConfig.getPipelineName());
        maxBackTrackLimit = context.maxBackTrackLimit();
    }

    Set<? extends FaninScmMaterial> scmMaterialForCurrentRevision() {
        return scmMaterialsByStageId.get(currentRevision);
    }

    void addChild(FanInNode<?> child) {
        children.add(child);
        child.parents.add(this);
    }

    enum RevisionAlteration {
        NOT_APPLICABLE, SAME_AS_CURRENT_REVISION, ALTERED_TO_CORRECT_REVISION, ALL_OPTIONS_EXHAUSTED, NEED_MORE_REVISIONS
    }

    void populateRevisions(CaseInsensitiveString pipelineName, FanInGraphContext context) {
        initialize(context);
        fillNextRevisions(context);
        if (initRevision(context) == ALL_OPTIONS_EXHAUSTED) {
            throw NoCompatibleUpstreamRevisionsException.noValidRevisionsForUpstream(pipelineName, materialConfig);
        }
    }

    private boolean resetCurrentRevision() {
        return scmMaterialsByStageId.keySet().stream()
            .findFirst()
            .map(stageId -> currentRevision = stageId)
            .isPresent();
    }

    private RevisionAlteration initRevision(FanInGraphContext context) {
        if (resetCurrentRevision()) {
            return ALTERED_TO_CORRECT_REVISION;
        }
        return handleNeedMoreRevisions(context);
    }

    private RevisionAlteration handleNeedMoreRevisions(FanInGraphContext context) {
        while (hasMoreInstances()) {
            fillNextRevisions(context);
            if (resetCurrentRevision()) {
                return ALTERED_TO_CORRECT_REVISION;
            }
        }
        return ALL_OPTIONS_EXHAUSTED;
    }

    RevisionAlteration setRevisionTo(StageIdFaninScmMaterialPair revisionToSet, FanInGraphContext context) {
        RevisionAlteration revisionAlteration = alterRevision(revisionToSet);
        while (revisionAlteration == NEED_MORE_REVISIONS) {
            fillNextRevisions(context);
            revisionAlteration = alterRevision(revisionToSet);
        }
        return revisionAlteration;
    }

    PipelineTimelineEntry latestPipelineTimelineEntry(FanInGraphContext context) {
        if (totalInstanceCount == 0) {
            return null;
        }
        return context.pipelineTimeline().instanceFor(materialConfig.getPipelineName(), totalInstanceCount - 1);
    }

    private void fillNextRevisions(FanInGraphContext context) {
        if (!hasMoreInstances()) {
            return;
        }
        int batchOffset = currentCount;
        for (int i = 1; i <= REVISION_BUFFER_SIZE; ++i) {
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

    private @Nullable Pair<StageIdentifier, List<FaninScmMaterial>> getRevisionNthFor(int n, FanInGraphContext context) {
        List<FaninScmMaterial> scmMaterials = new ArrayList<>();
        PipelineTimeline pipelineTimeline = context.pipelineTimeline();
        Queue<PipelineTimelineEntry.Revision> revisionQueue = new ConcurrentLinkedQueue<>();
        PipelineTimelineEntry entry = pipelineTimeline.instanceFor(materialConfig.getPipelineName(), totalInstanceCount - n);

        Set<CaseInsensitiveString> visitedNodes = new HashSet<>();

        StageIdentifier dependentStageIdentifier = dependentStageIdentifier(context, entry, CaseInsensitiveString.str(materialConfig.getStageName()));
        if (!StageIdentifier.NULL.equals(dependentStageIdentifier)) {
            addToRevisionQueue(entry, revisionQueue, scmMaterials, context, visitedNodes);
        } else {
            return null;
        }
        while (!revisionQueue.isEmpty()) {
            PipelineTimelineEntry.Revision revision = revisionQueue.poll();
            DependencyMaterialRevision dmr = DependencyMaterialRevision.create(revision.revision, null);
            PipelineTimelineEntry pte = pipelineTimeline.getEntryFor(new CaseInsensitiveString(dmr.getPipelineName()), dmr.getPipelineCounter());
            addToRevisionQueue(pte, revisionQueue, scmMaterials, context, visitedNodes);
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
                versionsByMaterial.put(scmMaterial, scmMaterial.revision());
            } else if (!revision.equals(scmMaterial.revision())) {
                return false;
            }
        }
        return true;
    }

    private void validateIfRevisionMatchesTheCurrentConfigAndUpdateTheMaterialMap(FanInGraphContext context, Pair<StageIdentifier, List<FaninScmMaterial>> stageIdentifierScmPair) {
        final Set<MaterialConfig> currentScmMaterials = context.pipelineScmDepMap().get(materialConfig);
        final Set<FaninScmMaterial> scmMaterials = new HashSet<>(stageIdentifierScmPair.last());
        final Set<String> currentScmFingerprint = new HashSet<>();
        for (MaterialConfig currentScmMaterial : currentScmMaterials) {
            currentScmFingerprint.add(currentScmMaterial.getFingerprint());
        }
        final Set<String> scmMaterialsFingerprint = new HashSet<>();
        for (FaninScmMaterial scmMaterial : scmMaterials) {
            scmMaterialsFingerprint.add(scmMaterial.fingerprint());
        }
        final Collection<?> commonMaterials = CollectionUtils.intersection(currentScmFingerprint, scmMaterialsFingerprint);
        if (commonMaterials.size() == scmMaterials.size() && commonMaterials.size() == currentScmMaterials.size()) {
            scmMaterialsByStageId.put(stageIdentifierScmPair.first(), scmMaterials);
            ++currentCount;
        } else {
            Collection<?> disjunctionWithConfig = CollectionUtils.disjunction(currentScmFingerprint, commonMaterials);
            Collection<?> disjunctionWithInstance = CollectionUtils.disjunction(scmMaterialsFingerprint, commonMaterials);

            LOGGER.warn("[Fan-in] - Incompatible materials for {}. Config: {}. Instance: {}.", stageIdentifierScmPair.first().getStageLocator(), disjunctionWithConfig, disjunctionWithInstance);

            //This is it. We will not go beyond this revision in history
            totalInstanceCount = currentCount;
        }
    }

    private StageIdentifier dependentStageIdentifier(FanInGraphContext context, PipelineTimelineEntry entry, final String stageName) {
        return context.pipelineDao().latestPassedStageIdentifier(entry.getId(), stageName);
    }

    private void addToRevisionQueue(PipelineTimelineEntry entry, Queue<PipelineTimelineEntry.Revision> revisionQueue, List<FaninScmMaterial> scmMaterials,
                                    FanInGraphContext context, Set<CaseInsensitiveString> visitedNodes) {
        for (Map.Entry<String, List<PipelineTimelineEntry.Revision>> revisionList : entry.revisions().entrySet()) {
            String fingerprint = revisionList.getKey();
            PipelineTimelineEntry.Revision revision = revisionList.getValue().get(0);
            if (context.isScmMaterial(fingerprint)) {
                scmMaterials.add(new FaninScmMaterial(fingerprint, revision));
                continue;
            }

            if (context.isDependencyMaterial(fingerprint) && !visitedNodes.contains(new CaseInsensitiveString(revision.revision))) {
                revisionQueue.add(revision);
                visitedNodes.add(new CaseInsensitiveString(revision.revision));
            }
        }
    }

    private boolean hasMoreInstances() {
        if (currentCount > maxBackTrackLimit.get()) {
            throw new MaxBackTrackLimitReachedException(materialConfig, maxBackTrackLimit);
        }
        return currentCount < totalInstanceCount;
    }

    private RevisionAlteration alterRevision(StageIdFaninScmMaterialPair revisionToSet) {
        if (currentRevision == revisionToSet.stageIdentifier()) {
            return SAME_AS_CURRENT_REVISION;
        }
        if (!scmMaterialForCurrentRevision().contains(revisionToSet.faninScmMaterial())) {
            return NOT_APPLICABLE;
        }
        List<StageIdentifier> stageIdentifiers = new ArrayList<>(scmMaterialsByStageId.keySet());
        int currentRevIndex = stageIdentifiers.indexOf(currentRevision);
        for (int i = currentRevIndex; i < stageIdentifiers.size(); i++) {
            final StageIdentifier key = stageIdentifiers.get(i);
            final List<FaninScmMaterial> materials = new ArrayList<>(scmMaterialsByStageId.get(key));
            final int index = materials.indexOf(revisionToSet.faninScmMaterial());
            if (index == -1) {
                return ALL_OPTIONS_EXHAUSTED;
            }
            final FaninScmMaterial faninScmMaterial = materials.get(index);
            if (faninScmMaterial.revision().equals(revisionToSet.faninScmMaterial().revision())) {
                currentRevision = key;
                return ALTERED_TO_CORRECT_REVISION;
            }
            if (faninScmMaterial.revision().lessThan(revisionToSet.faninScmMaterial().revision())) {
                currentRevision = key;
                return ALTERED_TO_CORRECT_REVISION;
            }
        }

        if (!hasMoreInstances()) {
            return ALL_OPTIONS_EXHAUSTED;
        }
        return NEED_MORE_REVISIONS;
    }

    List<StageIdFaninScmMaterialPair> getCurrentFaninScmMaterials() {
        return scmMaterialForCurrentRevision()
            .stream()
            .map(material -> new StageIdFaninScmMaterialPair(currentRevision, material))
            .toList();
    }
}
