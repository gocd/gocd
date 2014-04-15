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

package com.thoughtworks.go.server.presentation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PathFromAncestor;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineTemplateConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;

public class FetchArtifactViewHelper {
    private final CruiseConfig cruiseConfig;
    private final CaseInsensitiveString pipelineName;
    private final CaseInsensitiveString stageName;
    private final boolean template;

    private static final String NULL_STR = new String();

    public FetchArtifactViewHelper(CruiseConfig cruiseConfig, CaseInsensitiveString pipelineName, CaseInsensitiveString stageName, boolean template) {
        this.cruiseConfig = cruiseConfig;
        this.pipelineName = pipelineName;
        this.stageName = stageName;
        this.template = template;
    }

    private static final class JobHirarchyQueueEntry {
        final String pathFromNode;
        final CaseInsensitiveString stageName;
        final CaseInsensitiveString pipelineName;

        private JobHirarchyQueueEntry(String pathFromNode, CaseInsensitiveString pipelineName, CaseInsensitiveString stageName) {
            this.pathFromNode = pathFromNode;
            this.pipelineName = pipelineName;
            this.stageName = stageName;
        }

        public String pathFromAncestor() {
            return pathFromNode.length() > 0 ? pipelineName + PathFromAncestor.DELIMITER + pathFromNode : CaseInsensitiveString.str(pipelineName);
        }
    }

    private static final class FetchSuggestionHirarchy extends HashMap<CaseInsensitiveString, Map<CaseInsensitiveString, List<CaseInsensitiveString>>>{
        private void addStagesToHirarchy(CaseInsensitiveString pipelineName, List<StageConfig> currentPipelineStages) {
            Map<CaseInsensitiveString, List<CaseInsensitiveString>> stageMap = new HashMap<CaseInsensitiveString, List<CaseInsensitiveString>>();
            for (StageConfig stg : currentPipelineStages) {
                stageMap.put(stg.name(), stg.getJobs().names());
            }
            put(pipelineName, stageMap);
        }

        private void populateFetchableJobHirarchyFor(Queue<JobHirarchyQueueEntry> bfsQueue, CruiseConfig cruiseConfig) {
            while (! bfsQueue.isEmpty()) {
                JobHirarchyQueueEntry entry = bfsQueue.remove();
                CaseInsensitiveString pipelineName = entry.pipelineName;
                PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(pipelineName);
                List<StageConfig> fetchableStages = new ArrayList<StageConfig>();
                for (StageConfig stageConfig : pipelineConfig) {
                    fetchableStages.add(stageConfig);
                    if (entry.stageName.equals(stageConfig.name())) {
                        break;
                    }
                }
                addStagesToHirarchy(new CaseInsensitiveString(entry.pathFromAncestor()), fetchableStages);
                addMaterialsToQueue(bfsQueue, pipelineConfig, entry.pathFromAncestor());
            }
        }
    }

    public FetchSuggestionHirarchy autosuggestMap() {
        return fetchArtifactSuggestionsForPipeline(template ? createPipelineConfigForTemplate() : cruiseConfig.pipelineConfigByName(pipelineName));
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

    private FetchSuggestionHirarchy fetchArtifactSuggestionsForPipeline(PipelineConfig pipelineConfig) {
        FetchSuggestionHirarchy hirarchy = new FetchSuggestionHirarchy();
        Queue<JobHirarchyQueueEntry> bfsQueue = new ArrayDeque<JobHirarchyQueueEntry>();
        addLocalUpstreamStages(hirarchy, pipelineConfig);
        HashSet<DependencyMaterialConfig> handled = new HashSet<DependencyMaterialConfig>();
        addMaterialsToQueue(bfsQueue, pipelineConfig, "");
        hirarchy.populateFetchableJobHirarchyFor(bfsQueue, cruiseConfig);
        return hirarchy;
    }

    private void addLocalUpstreamStages(FetchSuggestionHirarchy hirarchy, PipelineConfig pipelineConfig) {
        List<StageConfig> currentPipelineStages = pipelineConfig.allStagesBefore(stageName);
        if (! currentPipelineStages.isEmpty()) {
            if (! template) {
                hirarchy.addStagesToHirarchy(pipelineName, currentPipelineStages);
            }
            hirarchy.addStagesToHirarchy(new CaseInsensitiveString(NULL_STR), currentPipelineStages);
        }
    }

    private static void addMaterialsToQueue(Queue<JobHirarchyQueueEntry> bfsQueue, PipelineConfig pipelineConfig, String pathFromThisPipeline) {
        for (MaterialConfig mat : pipelineConfig.materialConfigs()) {
            if (mat instanceof DependencyMaterialConfig) {
                DependencyMaterialConfig depMat = (DependencyMaterialConfig) mat;
                bfsQueue.add(new JobHirarchyQueueEntry(pathFromThisPipeline, depMat.getPipelineName(), depMat.getStageName()));
            }
        }
    }

}
