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
package com.thoughtworks.go.server.presentation;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.util.SystemEnvironment;

import java.util.*;


public class FetchArtifactViewHelper {
    private SystemEnvironment systemEnvironment;
    private final CruiseConfig cruiseConfig;
    private final CaseInsensitiveString pipelineName;
    private final CaseInsensitiveString stageName;
    private final boolean template;

    private static final String NULL_STR = new String();

    public FetchArtifactViewHelper(SystemEnvironment systemEnvironment, CruiseConfig cruiseConfig, CaseInsensitiveString pipelineName, CaseInsensitiveString stageName, boolean template) {
        this.systemEnvironment = systemEnvironment;
        this.cruiseConfig = cruiseConfig;
        this.pipelineName = pipelineName;
        this.stageName = stageName;
        this.template = template;
    }

    private static final class JobHierarchyQueueEntry {
        final String pathFromNode;
        final CaseInsensitiveString stageName;
        final CaseInsensitiveString pipelineName;

        private JobHierarchyQueueEntry(String pathFromNode, CaseInsensitiveString pipelineName, CaseInsensitiveString stageName) {
            this.pathFromNode = pathFromNode;
            this.pipelineName = pipelineName;
            this.stageName = stageName;
        }

        public String pathFromAncestor() {
            return pathFromNode.length() > 0 ? pipelineName + PathFromAncestor.DELIMITER + pathFromNode : CaseInsensitiveString.str(pipelineName);
        }
    }

    private static final class FetchSuggestionHierarchy extends HashMap<CaseInsensitiveString, Map> {
        private void addStagesToHierarchy(CaseInsensitiveString pipelineName, List<StageConfig> currentPipelineStages, CruiseConfig cruiseConfig) {
            Map<CaseInsensitiveString, Map> stageMap = new HashMap<>();
            currentPipelineStages.forEach(stg -> {
                Map<CaseInsensitiveString, Map> jobsToArtifacts = new HashMap<>();
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

        private void populateFetchableJobHierarchyFor(Queue<JobHierarchyQueueEntry> bfsQueue, CruiseConfig cruiseConfig) {
            while (!bfsQueue.isEmpty()) {
                JobHierarchyQueueEntry entry = bfsQueue.remove();
                CaseInsensitiveString pipelineName = entry.pipelineName;
                PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(pipelineName);
                List<StageConfig> fetchableStages = new ArrayList<>();
                for (StageConfig stageConfig : pipelineConfig) {
                    fetchableStages.add(stageConfig);
                    if (entry.stageName.equals(stageConfig.name())) {
                        break;
                    }
                }
                addStagesToHierarchy(new CaseInsensitiveString(entry.pathFromAncestor()), fetchableStages, cruiseConfig);
                addMaterialsToQueue(bfsQueue, pipelineConfig, entry.pathFromAncestor());
            }
        }
    }

    public FetchSuggestionHierarchy autosuggestMap() {
        if (!systemEnvironment.get(SystemEnvironment.FETCH_ARTIFACT_AUTO_SUGGEST)) {
            return new FetchSuggestionHierarchy();
        }
        if(template && !systemEnvironment.isFetchArtifactTemplateAutoSuggestEnabled()) {
            return new FetchSuggestionHierarchy();
        }
        return fetchArtifactSuggestionsForPipeline(template ? createPipelineConfigForTemplate() : cruiseConfig.pipelineConfigByName(pipelineName), cruiseConfig);
    }

    private PipelineConfig createPipelineConfigForTemplate() {
        List<PipelineConfig> pipelineConfigs = cruiseConfig.allPipelines();
        PipelineConfig dummyPipeline = new PipelineConfig();
        for (PipelineConfig pipelineConfig : pipelineConfigs) {
            dummyPipeline.addMaterialConfig(new DependencyMaterialConfig(pipelineConfig.name(), pipelineConfig.last().name()));
        }
        PipelineTemplateConfig pipelineTemplateConfig = cruiseConfig.getTemplates().templateByName(pipelineName);
        for (StageConfig stageConfig : pipelineTemplateConfig) {
            if (stageName.equals(stageConfig.name())) {
                break;
            }
            dummyPipeline.add(stageConfig);
        }
        return dummyPipeline;
    }

    private FetchSuggestionHierarchy fetchArtifactSuggestionsForPipeline(PipelineConfig pipelineConfig, CruiseConfig cruiseConfig) {
        FetchSuggestionHierarchy hierarchy = new FetchSuggestionHierarchy();
        Queue<JobHierarchyQueueEntry> bfsQueue = new ArrayDeque<>();
        addLocalUpstreamStages(hierarchy, pipelineConfig, cruiseConfig);
        addMaterialsToQueue(bfsQueue, pipelineConfig, "");
        hierarchy.populateFetchableJobHierarchyFor(bfsQueue, cruiseConfig);
        return hierarchy;
    }

    private void addLocalUpstreamStages(FetchSuggestionHierarchy hierarchy, PipelineConfig pipelineConfig, CruiseConfig cruiseConfig) {
        List<StageConfig> currentPipelineStages = pipelineConfig.allStagesBefore(stageName);
        if (!currentPipelineStages.isEmpty()) {
            if (!template) {
                hierarchy.addStagesToHierarchy(pipelineName, currentPipelineStages, cruiseConfig);
            }
            hierarchy.addStagesToHierarchy(new CaseInsensitiveString(NULL_STR), currentPipelineStages, cruiseConfig);
        }
    }

    private static void addMaterialsToQueue(Queue<JobHierarchyQueueEntry> bfsQueue, PipelineConfig pipelineConfig, String pathFromThisPipeline) {
        for (MaterialConfig mat : pipelineConfig.materialConfigs()) {
            if (mat instanceof DependencyMaterialConfig) {
                DependencyMaterialConfig depMat = (DependencyMaterialConfig) mat;
                bfsQueue.add(new JobHierarchyQueueEntry(pathFromThisPipeline, depMat.getPipelineName(), depMat.getStageName()));
            }
        }
    }

}
