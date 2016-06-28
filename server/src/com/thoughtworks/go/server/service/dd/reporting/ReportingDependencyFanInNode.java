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

package com.thoughtworks.go.server.service.dd.reporting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.domain.PipelineTimelineEntry;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.util.Pair;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;


public class ReportingDependencyFanInNode extends ReportingFanInNode {
    private static List<Class<? extends MaterialConfig>> DEPENDENCY_NODE_TYPES = new ArrayList<>();
    public Set<ReportingFanInNode> children = new HashSet<>();
    StageIdentifier currentRevision;
    private int totalInstanceCount = Integer.MAX_VALUE;
    private int currentCount;
    private Map<StageIdentifier, Set<ReportingFaninScmMaterial>> stageIdentifierScmMaterial = new LinkedHashMap<>();

    ReportingDependencyFanInNode(MaterialConfig material) {
        super(material);
        for (Class<? extends MaterialConfig> clazz : DEPENDENCY_NODE_TYPES) {
            if (clazz.isAssignableFrom(material.getClass())) {
                return;
            }
        }
        throw new RuntimeException("Not a valid root node material type");
    }

    public void populateRevisions(ReportingFanInGraphContext context) {
        initialize(context);
        context.out.println("Total Instances: " + totalInstanceCount);
        context.out.println();
        fillNextRevisions(context);
    }

    private void setCurrentRevision() {
        currentRevision = stageIdentifierScmMaterial.keySet().toArray(new StageIdentifier[0])[0];
    }

    public void initialize(ReportingFanInGraphContext context) {
        totalInstanceCount = context.pipelineTimeline.instanceCount(((DependencyMaterialConfig) materialConfig).getPipelineName());
    }

    private void fillNextRevisions(ReportingFanInGraphContext context) {
        if (!hasMoreInstances()) {
            return;
        }

        final Pair<StageIdentifier, List<ReportingFaninScmMaterial>> sIdScmPair = getRevisionNthFor(1, context);

        validateIfRevisionMatchesTheCurrentConfig(context, sIdScmPair);

        if (!validateAllScmRevisionsAreSameWithinAFingerprint(sIdScmPair)) {
            context.out.println("Latest Revision Is Inconsistent ");
        }

        context.out.println();
    }

    private void printCurrentAndOldSCMs(ReportingFanInGraphContext context, Set<MaterialConfig> currentScmMaterials, List<ReportingFaninScmMaterial> scmMaterials,
                                        Pair<StageIdentifier, List<ReportingFaninScmMaterial>> stageIdentifierScmPair) {
        context.out.println();
        context.out.println("----");
        context.out.println("SCM Materials in config:");
        context.out.println(currentScmMaterials);
        final Set<MaterialConfig> scmMaterialsInRev = new HashSet<>();
        for (ReportingFaninScmMaterial scmMaterial : scmMaterials) {
            final MaterialConfig scm = context.fingerprintScmMaterialMap.get(scmMaterial.fingerprint);
            scmMaterialsInRev.add(scm);
        }
        context.out.println("----");
        context.out.println("SCM Materials in Latest Revision:");
        context.out.println(scmMaterialsInRev);
        context.out.println("----");
        context.out.println("Latest Revision of Material:");
        context.out.println(stageIdentifierScmPair.first());
        context.out.println("----");
    }

    private void validateIfRevisionMatchesTheCurrentConfig(ReportingFanInGraphContext context, Pair<StageIdentifier, List<ReportingFaninScmMaterial>> stageIdentifierScmPair) {
        if (stageIdentifierScmPair == null) {
            return;
        }

        final Set<MaterialConfig> currentScmMaterials = context.pipelineScmDepMap.get(materialConfig);
        final List<ReportingFaninScmMaterial> scmMaterials = stageIdentifierScmPair.last();

        printCurrentAndOldSCMs(context, currentScmMaterials, scmMaterials, stageIdentifierScmPair);

        final List<ReportingFaninScmMaterial> setOfRevisions = new ArrayList<>();
        for (final ReportingFaninScmMaterial scmMaterial : scmMaterials) {
            ReportingFaninScmMaterial mat = (ReportingFaninScmMaterial) CollectionUtils.find(setOfRevisions, new Predicate() {
                @Override
                public boolean evaluate(Object obj) {
                    if (obj == null) {
                        return false;
                    }
                    ReportingFaninScmMaterial mat = (ReportingFaninScmMaterial) obj;
                    return scmMaterial.fingerprint.equals(mat.fingerprint)
                            && scmMaterial.revision.equals(mat.revision);
                }
            });
            if (mat == null) {
                setOfRevisions.add(scmMaterial);
            }
        }

        context.out.println("SCM Revisions of Latest Revision of Material:");
        context.out.println(setOfRevisions);
    }

    private Pair<StageIdentifier, List<ReportingFaninScmMaterial>> getRevisionNthFor(int n, ReportingFanInGraphContext context) {
        List<ReportingFaninScmMaterial> scmMaterials = new ArrayList<>();
        PipelineTimeline pipelineTimeline = context.pipelineTimeline;
        Queue<PipelineTimelineEntry.Revision> revisionQueue = new ConcurrentLinkedQueue<>();
        DependencyMaterialConfig dependencyMaterial = (DependencyMaterialConfig) materialConfig;
        PipelineTimelineEntry entry = pipelineTimeline.instanceFor(dependencyMaterial.getPipelineName(), totalInstanceCount - n);

        StageIdentifier dependentStageIdentifier = dependentStageIdentifier(context, entry, CaseInsensitiveString.str(dependencyMaterial.getStageName()));
        if (!StageIdentifier.NULL.equals(dependentStageIdentifier)) {
            addToRevisionQueue(entry, revisionQueue, scmMaterials, context);
        } else {
            return null;
        }
        while (!revisionQueue.isEmpty()) {
            PipelineTimelineEntry.Revision revision = revisionQueue.poll();
            DependencyMaterialRevision dmr = DependencyMaterialRevision.create(revision.revision, null);
            PipelineTimelineEntry pte = pipelineTimeline.getEntryFor(new CaseInsensitiveString(dmr.getPipelineName()), dmr.getPipelineCounter());
            addToRevisionQueue(pte, revisionQueue, scmMaterials, context);
        }

        return new Pair<>(dependentStageIdentifier, scmMaterials);
    }

    private boolean validateAllScmRevisionsAreSameWithinAFingerprint(Pair<StageIdentifier, List<ReportingFaninScmMaterial>> pIdScmPair) {
        if (pIdScmPair == null) {
            return false;
        }
        List<ReportingFaninScmMaterial> scmMaterialList = pIdScmPair.last();
        for (final ReportingFaninScmMaterial scmMaterial : scmMaterialList) {
            Collection<ReportingFaninScmMaterial> scmMaterialOfSameFingerprint = CollectionUtils.select(scmMaterialList, new Predicate() {
                @Override
                public boolean evaluate(Object o) {
                    return scmMaterial.equals(o);
                }
            });

            for (ReportingFaninScmMaterial faninScmMaterial : scmMaterialOfSameFingerprint) {
                if (!faninScmMaterial.revision.equals(scmMaterial.revision)) {
                    return false;
                }
            }
        }
        return true;
    }

    private StageIdentifier dependentStageIdentifier(ReportingFanInGraphContext context, PipelineTimelineEntry entry, final String stageName) {
        return context.pipelineDao.latestPassedStageIdentifier(entry.getId(), stageName);
    }

    private void addToRevisionQueue(PipelineTimelineEntry entry, Queue<PipelineTimelineEntry.Revision> revisionQueue, List<ReportingFaninScmMaterial> scmMaterials,
                                    ReportingFanInGraphContext context) {
        printPipelineTimelineEntry(entry, context);

        for (Map.Entry<String, List<PipelineTimelineEntry.Revision>> revisionList : entry.revisions().entrySet()) {
            String fingerprint = revisionList.getKey();
            PipelineTimelineEntry.Revision revision = revisionList.getValue().get(0);
            if (isScmMaterial(fingerprint, context)) {
                scmMaterials.add(new ReportingFaninScmMaterial(fingerprint, revision));
                continue;
            }

            if (isDependencyMaterial(fingerprint, context)) {
                revisionQueue.add(revision);
            }
        }
    }

    private void printPipelineTimelineEntry(PipelineTimelineEntry entry, ReportingFanInGraphContext context) {
        context.out.println(
                "Pipeline-Timeline-Entry: Id: " + entry.getId() + ", Pipeline-Name: " + entry.getPipelineName() + ", Counter: " + entry.getCounter() + ", Natural-Order: " + entry.naturalOrder());
        Iterator it = entry.revisions().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, List<PipelineTimelineEntry.Revision>> pairs = (Map.Entry) it.next();
            context.out.println("Flyweight: " + pairs.getKey() + " - " + pairs.getValue());
        }
        context.out.println("***");
    }

    private boolean isDependencyMaterial(String fingerprint, ReportingFanInGraphContext context) {
        return context.fingerprintDepMaterialMap.containsKey(fingerprint);
    }

    private boolean isScmMaterial(String fingerprint, ReportingFanInGraphContext context) {
        return context.fingerprintScmMaterialMap.containsKey(fingerprint);
    }

    private boolean hasMoreInstances() {
        return currentCount < totalInstanceCount;
    }

    enum RevisionAlteration {
        NOT_APPLICABLE, SAME_AS_CURRENT_REVISION, ALTERED_TO_CORRECT_REVISION, ALL_OPTIONS_EXHAUSTED,
        NEED_MORE_REVISIONS
    }

    static {
        DEPENDENCY_NODE_TYPES.add(DependencyMaterialConfig.class);
    }
}
