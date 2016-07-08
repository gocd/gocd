/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.helpers;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.server.service.ScheduleTestUtil;
import com.thoughtworks.go.util.GoConfigFileHelper;

import java.util.ArrayList;
import java.util.List;

public class GraphGenerator {
    private GoConfigFileHelper configHelper;
    private ScheduleTestUtil scheduleUtil;

    public GraphGenerator(GoConfigFileHelper configHelper, ScheduleTestUtil scheduleUtil) {
        this.configHelper = configHelper;
        this.scheduleUtil = scheduleUtil;
    }

    /*
        create mesh of below structure with every pipeline in L{row,col} is connected to every pipeline L{row+1,col+1}:
           +---->L11--->L12----+
          /                     \
        start--->L21--->L22---->end
          \                     /
           +---->L31--->L32----+
     */
    public PipelineConfig createMesh(PipelineConfig startNode, String endNodeName, String pipelineNameSuffix, int numberOfInstances, int numberOfNodesPerLevel, int numberOfLevels) {
        List<PipelineConfig> previousNodes = new ArrayList<PipelineConfig>();
        previousNodes.add(startNode);
        List<PipelineConfig> currentNodes = new ArrayList<PipelineConfig>();
        for (int i = 1; i <= numberOfLevels; i++) {
            for (int j = 1; j <= numberOfNodesPerLevel; j++) {
                String pipelineName = String.format("pipeline_%s_%d_%d", pipelineNameSuffix, i, j);
                PipelineConfig pipelineConfig = createPipelineWithInstances(pipelineName, previousNodes, numberOfInstances);
                currentNodes.add(pipelineConfig);
            }
            previousNodes = currentNodes;
            currentNodes = new ArrayList<PipelineConfig>();
        }
        return createPipelineWithInstances(endNodeName, previousNodes, numberOfInstances);
    }

    public PipelineConfig createPipelineWithInstances(String pipelineName, List<PipelineConfig> previousNodes, int numberOfInstances) {
        PipelineConfig pipelineConfig = getPipelineWithName(pipelineName, previousNodes);
        configHelper.addPipeline(pipelineConfig);

        createInstances(numberOfInstances, previousNodes, pipelineConfig);
        return pipelineConfig;
    }

    private PipelineConfig getPipelineWithName(String startNode, List<PipelineConfig> materials) {
        PipelineConfig pipelineConfig = PipelineMother.createPipelineConfig(startNode, MaterialConfigsMother.defaultMaterialConfigs(), "stage");
        addDependencyMaterials(pipelineConfig, materials);
        return pipelineConfig;
    }

    private void addDependencyMaterials(PipelineConfig pipelineConfig, List<PipelineConfig> pipelineDependencies) {
        for (PipelineConfig pipelineDependency : pipelineDependencies) {
            pipelineConfig.addMaterialConfig(new DependencyMaterialConfig(pipelineDependency.name(), new CaseInsensitiveString("stage")));
        }
    }

    private void createInstances(int numberOfInstances, List<PipelineConfig> previousNodes, PipelineConfig pipelineConfig) {
        for (int k = 1; k <= numberOfInstances; k++) {
            List<String> previousRevisions = new ArrayList<String>();
            previousRevisions.add("svn_1");
            int instanceCount = previousNodes.size() == 1 ? 1 : k;
            for (int x = 0; x < previousNodes.size(); x++) {
                previousRevisions.add(String.format("%s/%d/stage/1", previousNodes.get(x).name().toString(), instanceCount));
            }
            scheduleUtil.runAndPass(new ScheduleTestUtil.AddedPipeline(pipelineConfig, new DependencyMaterial(pipelineConfig.name(), new CaseInsensitiveString("stage"))),
                    previousRevisions.toArray(new String[previousRevisions.size()]));
        }
    }
}
