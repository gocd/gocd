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
package com.thoughtworks.go.server.presentation;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;

import java.util.*;

import static com.thoughtworks.go.config.CaseInsensitiveString.cis;
import static java.util.stream.Collectors.toCollection;

public class FetchArtifactViewHelper {
    private final CruiseConfig cruiseConfig;
    private final CaseInsensitiveString pipelineName;
    private final CaseInsensitiveString stageName;
    private final boolean template;
    private final PermissionResolver permissions;

    private static final String NULL_STR = "";

    public FetchArtifactViewHelper(CruiseConfig cruiseConfig, CaseInsensitiveString pipelineName, CaseInsensitiveString stageName, boolean template, PermissionResolver permissions) {
        this.cruiseConfig = cruiseConfig;
        this.pipelineName = pipelineName;
        this.stageName = stageName;
        this.template = template;
        this.permissions = permissions;
    }

    @FunctionalInterface
    public interface PermissionResolver {
        boolean canView(CaseInsensitiveString pipelineName);
    }

    public FetchSuggestionHierarchy autosuggestMap() {
        return fetchArtifactSuggestionsForPipeline(template ? createPipelineConfigForTemplate(pipelineName) : cruiseConfig.pipelineConfigByName(pipelineName));
    }

    private record JobHierarchyQueueEntry(String pathFromNode, CaseInsensitiveString pipelineName, CaseInsensitiveString stageName) {
        public String pathFromAncestor() {
            return !pathFromNode.isEmpty() ? pipelineName + PathFromAncestor.DELIMITER + pathFromNode : CaseInsensitiveString.str(pipelineName);
        }
    }

    public final class FetchSuggestionHierarchy extends HashMap<CaseInsensitiveString, Map<CaseInsensitiveString, ?>> {
        private void addStagesToHierarchy(CaseInsensitiveString pipelineName, List<StageConfig> currentPipelineStages) {
            Map<CaseInsensitiveString, Map<CaseInsensitiveString, ?>> stageMap = new HashMap<>(currentPipelineStages.size());
            currentPipelineStages.forEach(stg -> {
                Map<CaseInsensitiveString, Map<String, ?>> jobsToArtifacts = new HashMap<>(stg.getJobs().size());
                stg.getJobs().forEach(job -> {
                    List<PluggableArtifactConfig> pluggableArtifactConfigs = job.artifactTypeConfigs().getPluggableArtifactConfigs();
                    Map<String, String> artifactToPlugins = new HashMap<>();
                    pluggableArtifactConfigs.forEach(pluggableArtifactConfig -> {
                        ArtifactStore store = cruiseConfig.getArtifactStores().find(pluggableArtifactConfig.getStoreId());
                        artifactToPlugins.put(pluggableArtifactConfig.getId(), store == null ? null : store.getPluginId());
                    });
                    jobsToArtifacts.put(job.name(), artifactToPlugins);
                });
                stageMap.put(stg.name(), jobsToArtifacts);
            });
            put(pipelineName, stageMap);
        }

        private void populateFetchableJobHierarchyFor(Queue<JobHierarchyQueueEntry> bfsQueue) {
            while (!bfsQueue.isEmpty()) {
                JobHierarchyQueueEntry entry = bfsQueue.remove();
                PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(entry.pipelineName);
                if (permissions.canView(entry.pipelineName)) {
                    addStagesToHierarchy(cis(entry.pathFromAncestor()), pipelineConfig.allStagesUpTo(entry.stageName).toList());
                }
                addMaterialsToQueue(bfsQueue, pipelineConfig, entry.pathFromAncestor());
            }
        }
    }

    private PipelineConfig createPipelineConfigForTemplate(CaseInsensitiveString templateName) {
        return new PipelineConfig(cis(NULL_STR),
            cruiseConfig.allPipelines().stream()
                .filter(p -> !p.isEmpty())
                .map(p -> new DependencyMaterialConfig(p.name(), p.getLast().name()))
                .collect(toCollection(MaterialConfigs::new)),
            cruiseConfig.getTemplateByName(templateName).stream()
                .filter(stageConfig -> !stageName.equals(stageConfig.name())).toArray(StageConfig[]::new)
        );
    }

    private FetchSuggestionHierarchy fetchArtifactSuggestionsForPipeline(PipelineConfig pipelineConfig) {
        FetchSuggestionHierarchy hierarchy = new FetchSuggestionHierarchy();
        Queue<JobHierarchyQueueEntry> bfsQueue = new ArrayDeque<>();
        addLocalUpstreamStages(hierarchy, pipelineConfig);
        addMaterialsToQueue(bfsQueue, pipelineConfig, "");
        hierarchy.populateFetchableJobHierarchyFor(bfsQueue);
        return hierarchy;
    }

    private void addLocalUpstreamStages(FetchSuggestionHierarchy hierarchy, PipelineConfig pipelineConfig) {
        List<StageConfig> currentPipelineStages = pipelineConfig.allStagesBefore(stageName).toList();
        if (!currentPipelineStages.isEmpty()) {
            if (!template) {
                hierarchy.addStagesToHierarchy(pipelineName, currentPipelineStages);
            }
            hierarchy.addStagesToHierarchy(cis(NULL_STR), currentPipelineStages);
        }
    }

    private static void addMaterialsToQueue(Queue<JobHierarchyQueueEntry> bfsQueue, PipelineConfig pipelineConfig, String pathFromThisPipeline) {
        for (MaterialConfig mat : pipelineConfig.materialConfigs()) {
            if (mat instanceof DependencyMaterialConfig depMat) {
                bfsQueue.add(new JobHierarchyQueueEntry(pathFromThisPipeline, depMat.getPipelineName(), depMat.getStageName()));
            }
        }
    }

}
