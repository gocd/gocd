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

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.domain.PipelineConfigDependencyGraph;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.dd.DependencyFanInNode;
import com.thoughtworks.go.server.service.dd.FanInEventListener;
import com.thoughtworks.go.server.service.dd.FanInGraph;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import java.util.Collection;
import java.util.List;
import java.util.Queue;

@Service
public class PipelineService implements UpstreamPipelineResolver {
    private static final Logger LOGGER = Logger.getLogger(PipelineService.class);

    private TransactionTemplate transactionTemplate;
    private PipelineSqlMapDao pipelineDao;
    private StageService stageService;
    private PipelineLockService pipelineLockService;
    private PipelineTimeline pipelineTimeline;
    private MaterialRepository materialRepository;
    private final SystemEnvironment systemEnvironment;
    private final GoConfigService goConfigService;
    private MaterialConfigConverter materialConfigConverter;

    @Autowired
    public PipelineService(PipelineSqlMapDao pipelineDao, StageService stageService, PipelineLockService pipelineLockService, PipelineTimeline pipelineTimeline, MaterialRepository materialRepository,
                           TransactionTemplate transactionTemplate, SystemEnvironment systemEnvironment, GoConfigService goConfigService, MaterialConfigConverter materialConfigConverter) {
        this.pipelineDao = pipelineDao;
        this.stageService = stageService;
        this.pipelineLockService = pipelineLockService;
        this.pipelineTimeline = pipelineTimeline;
        this.materialRepository = materialRepository;
        this.transactionTemplate = transactionTemplate;
        this.systemEnvironment = systemEnvironment;
        this.goConfigService = goConfigService;
        this.materialConfigConverter = materialConfigConverter;
    }

    public Pipeline fullPipelineByBuildId(long buildId) {
        return pipelineDao.fullPipelineByBuildId(buildId);
    }

    public Pipeline pipelineWithModsByStageId(String pipelineName, long stageId) {
        return pipelineDao.pipelineWithModsByStageId(pipelineName, stageId);
    }

    public Pipeline fullPipelineById(long pipelineId) {
        return pipelineDao.loadPipeline(pipelineId);
    }

    public Pipeline mostRecentFullPipelineByName(String pipeLineName) {
        return pipelineDao.mostRecentPipeline(pipeLineName);
    }

    public StageIdentifier findLastSuccessfulStageIdentifier(String pipelineName, String stageName) {
        return pipelineDao.findLastSuccessfulStageIdentifier(pipelineName, stageName);
    }

    public Pipeline save(final Pipeline pipeline) {
        String mutexPipelineName = PipelinePauseService.mutexForPausePipeline(pipeline.getName());
        synchronized (mutexPipelineName) {
            return (Pipeline) transactionTemplate.execute(new TransactionCallback() {
                public Object doInTransaction(TransactionStatus status) {
                    if (pipeline instanceof NullPipeline) {
                        return pipeline;
                    }
                    updateCounter(pipeline);

                    Pipeline pipelineWithId = pipelineDao.save(pipeline);
                    pipelineLockService.lockIfNeeded(pipelineWithId);
                    for (Stage stage : pipeline.getStages()) {
                        stageService.save(pipelineWithId, stage);
                    }

                    pipelineTimeline.update();
                    return pipelineWithId;
                }
            });
        }
    }

    private void updateCounter(Pipeline pipeline) {
        Integer lastCount = pipelineDao.getCounterForPipeline(pipeline.getName());
        pipeline.updateCounter(lastCount);
        pipelineDao.insertOrUpdatePipelineCounter(pipeline, lastCount, pipeline.getCounter());
    }

    /**
     * @deprecated this is evil and should be removed
     */
    public Pipeline wrapBuildDetails(JobInstance job) {
        Stage stageForBuild = stageService.getStageByBuild(job.getId());
        stageForBuild.setJobInstances(new JobInstances(job));

        Pipeline pipeline = pipelineDao.pipelineByBuildIdWithMods(job.getId());
        pipeline.setStages(new Stages(stageForBuild));
        return pipeline;
    }

    public Pipeline findPipelineByCounterOrLabel(String pipelineName, String counterOrLabel) {
        return pipelineDao.findPipelineByCounterOrLabel(pipelineName, counterOrLabel);
    }

    public Pipeline fullPipelineByCounterOrLabel(String pipelineName, String counterOrLabel) {
        Pipeline pipeline = findPipelineByCounterOrLabel(pipelineName, counterOrLabel);
        pipelineDao.loadAssociations(pipeline, pipelineName);
        return pipeline;
    }

    /* TRIANGLE BEGIN */

    public MaterialRevisions getRevisionsBasedOnDependencies(PipelineConfigDependencyGraph graph, MaterialRevisions actualRevisions) {
        MaterialRevisions computedRevisions = new MaterialRevisions();
        overrideCommonMaterialRevisions(graph, actualRevisions, computedRevisions);
        copyMissingRevisions(actualRevisions, computedRevisions);
        return restoreOriginalOrder(actualRevisions, computedRevisions);
    }

    private void overrideCommonMaterialRevisions(PipelineConfigDependencyGraph graph, MaterialRevisions actualRevisions, MaterialRevisions computedRevisions) {
        Queue<PipelineConfigDependencyGraph.PipelineConfigQueueEntry> configQueue = graph.buildQueue();
        for (MaterialRevision actualRevision : actualRevisions) {
            if (actualRevision.getMaterial() instanceof DependencyMaterial && actualRevision.isChanged()) {
                DependencyMaterialRevision revision = (DependencyMaterialRevision) actualRevision.getRevision();
                for (PipelineConfigDependencyGraph.PipelineConfigQueueEntry configQueueEntry : configQueue) {
                    populateRevisionsUsingUpstream(actualRevisions, computedRevisions, revision, configQueueEntry);
                }
            }
        }
    }

    private void populateRevisionsUsingUpstream(MaterialRevisions actualRevisions, MaterialRevisions newRevisions, DependencyMaterialRevision dmr,
                                                PipelineConfigDependencyGraph.PipelineConfigQueueEntry configQueueEntry) {
        if (!configQueueEntry.containsPipelineInPath(dmr.getPipelineName())) {
            return;
        }
        for (MaterialRevision materialRevision : actualRevisions) {
            Material material = materialRevision.getMaterial();
            if (currentPipelineHasMaterial(configQueueEntry, material) && !alreadyAdded(newRevisions, material)) {
                List<PipelineConfig> paths = removePathHead(configQueueEntry);
                if (!paths.isEmpty()) {
                    MaterialRevision revision = getRevisionFor(paths, dmr, material);
                    //revision is null when an upstream is both parent and grandparent
                    if (revision != null) {
                        materialRevision.replaceModifications(revision.getModifications());
                        newRevisions.addRevision(materialRevision);
                    }
                }
            }
        }
    }

    private boolean currentPipelineHasMaterial(PipelineConfigDependencyGraph.PipelineConfigQueueEntry configQueueEntry, Material material) {
        return configQueueEntry.getNode().hasMaterial(material.config());
    }

    private List<PipelineConfig> removePathHead(PipelineConfigDependencyGraph.PipelineConfigQueueEntry configQueueEntry) {
        return configQueueEntry.pathWithoutHead();
    }

    private MaterialRevision getRevisionFor(List<PipelineConfig> path, DependencyMaterialRevision initialRevision, Material matchedMaterial) {
        Pipeline byNameAndCounter = pipelineDao.findPipelineByNameAndCounter(initialRevision.getPipelineName(), initialRevision.getPipelineCounter());
        MaterialRevisions revisions = materialRepository.findMaterialRevisionsForPipeline(byNameAndCounter.getId());
        path.remove(0);
        if (path.isEmpty()) {
            return revisions.findRevisionForFingerPrint(matchedMaterial.getFingerprint());
        }
        return getRevisionFor(path, revisions.findDependencyMaterialRevision(CaseInsensitiveString.str(path.get(0).name())), matchedMaterial);
    }

    private void copyMissingRevisions(MaterialRevisions srcRevisions, MaterialRevisions destRevisions) {
        for (MaterialRevision actualRevision : srcRevisions) {
            if (!alreadyAdded(destRevisions, actualRevision.getMaterial())) {
                destRevisions.addRevision(actualRevision);
            }
        }
    }

    private boolean alreadyAdded(MaterialRevisions newRevisions, Material material) {
        return newRevisions.containsModificationForFingerprint(material);
    }

    private MaterialRevisions restoreOriginalOrder(MaterialRevisions actualRevisions, MaterialRevisions computedRevisions) {
        MaterialRevisions orderedComputedRevisions = new MaterialRevisions();
        for (MaterialRevision actualRevision : actualRevisions) {
            orderedComputedRevisions.addRevision(computedRevisions.findRevisionFor(actualRevision.getMaterial()));
        }
        return orderedComputedRevisions;
    }

    /* TRIANGLE END */

    /* DIAMOND BEGIN */

    public MaterialRevisions getRevisionsBasedOnDependencies(MaterialRevisions actualRevisions, CruiseConfig cruiseConfig, CaseInsensitiveString pipelineName) {
        FanInGraph fanInGraph = new FanInGraph(cruiseConfig, pipelineName, materialRepository, pipelineDao, systemEnvironment, materialConfigConverter);
        final MaterialRevisions computedRevisions = fanInGraph.computeRevisions(actualRevisions, pipelineTimeline);
        fillUpNonOverridableRevisions(actualRevisions, computedRevisions);
        return restoreOriginalMaterialConfigAndMaterialOrderUsingFingerprint(actualRevisions, computedRevisions);
    }

    // This is for debugging purposes
    public String getRevisionsBasedOnDependenciesForDebug(CaseInsensitiveString pipelineName, final Integer targetIterationCount) {
        CruiseConfig cruiseConfig = goConfigService.getCurrentConfig();
        FanInGraph fanInGraph = new FanInGraph(cruiseConfig, pipelineName, materialRepository, pipelineDao, systemEnvironment, materialConfigConverter);
        final String[] iterationData = {null};
        fanInGraph.setFanInEventListener(new FanInEventListener() {
            @Override
            public void iterationComplete(int iterationCount, List<DependencyFanInNode> dependencyFanInNodes) {
                if (iterationCount == targetIterationCount) {
                    iterationData[0] = new GsonBuilder().setExclusionStrategies(getGsonExclusionStrategy()).create().toJson(dependencyFanInNodes);
                }
            }
        });
        PipelineConfig pipelineConfig = goConfigService.pipelineConfigNamed(pipelineName);
        Materials materials = materialConfigConverter.toMaterials(pipelineConfig.materialConfigs());
        MaterialRevisions actualRevisions = new MaterialRevisions();
        for (Material material : materials) {
            actualRevisions.addAll(materialRepository.findLatestModification(material));
        }
        MaterialRevisions materialRevisions = fanInGraph.computeRevisions(actualRevisions, pipelineTimeline);
        if (iterationData[0] == null) {
            iterationData[0] = new GsonBuilder().setExclusionStrategies(getGsonExclusionStrategy()).create().toJson(materialRevisions);
        }
        return iterationData[0];
    }

    private ExclusionStrategy getGsonExclusionStrategy() {
        return new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                return f.getName().equals("materialConfig") || f.getName().equals("parents") || f.getName().equals("children");
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        };
    }

    //This whole method is repeated for reporting and it does not use actual revisions for determining final revisions
    //Used in rails view
    //Do not delete
    //Ramraj ge salute
    //Srikant & Sachin
    @Deprecated
    public Collection<MaterialRevision> getRevisionsBasedOnDependenciesForReporting(CruiseConfig cruiseConfig, CaseInsensitiveString pipelineName) {
        FanInGraph fanInGraph = new FanInGraph(cruiseConfig, pipelineName, materialRepository, pipelineDao, systemEnvironment, materialConfigConverter);
        return fanInGraph.computeRevisionsForReporting(pipelineName, pipelineTimeline);
    }

    private void fillUpNonOverridableRevisions(MaterialRevisions actualRevisions, MaterialRevisions computedRevisions) {
        for (int i = 0; i < actualRevisions.numberOfRevisions(); i++) {
            MaterialRevision actualRev = actualRevisions.getMaterialRevision(i);
            MaterialRevision computedRevision = computedRevisions.findRevisionFor(actualRev.getMaterial());
            if (computedRevision == null) {
                computedRevisions.addRevision(actualRev);
            }
        }
    }


    public BuildCause buildCauseFor(String pipelineName, int pipelineCounter) {
        return pipelineDao.findBuildCauseOfPipelineByNameAndCounter(pipelineName, pipelineCounter);
    }

    private MaterialRevisions restoreOriginalMaterialConfigAndMaterialOrderUsingFingerprint(MaterialRevisions actualRevisions, MaterialRevisions computedRevisions) {
        MaterialRevisions orderedComputedRevisions = new MaterialRevisions();
        for (MaterialRevision actualRevision : actualRevisions) {
            MaterialRevision revisionForMaterial = computedRevisions.findRevisionUsingMaterialFingerprintFor(actualRevision.getMaterial());
            MaterialRevision materialRevisionWithRestoredMaterialConfig = new MaterialRevision(actualRevision.getMaterial(), revisionForMaterial.isChanged(), revisionForMaterial.getModifications());
            orderedComputedRevisions.addRevision(materialRevisionWithRestoredMaterialConfig);
        }
        return orderedComputedRevisions;
    }

    /* DIAMOND END */

    public PipelineTimeline getPipelineTimeline() {
        return pipelineTimeline;
    }
}
