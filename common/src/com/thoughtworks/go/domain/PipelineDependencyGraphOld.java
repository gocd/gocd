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

package com.thoughtworks.go.domain;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel;

/**
 * @understands the upstream dependencies of a given pipeline instance
 */
public class PipelineDependencyGraphOld {
    private final PipelineInstanceModel pipelineInstanceModel;
    private final PipelineInstanceModels dependencies;

    public PipelineDependencyGraphOld(PipelineInstanceModel pipelineInstanceModel, PipelineInstanceModels dependencies) {
        this.pipelineInstanceModel = pipelineInstanceModel;
        this.dependencies = dependencies;
    }


    public PipelineInstanceModel pipeline() {
        return pipelineInstanceModel;
    }

     public PipelineInstanceModels dependencies() {
        return dependencies;
    }

    public boolean hasDependent(String pipelineName) {
        return dependencies.find(pipelineName) != null;
    }

    public void addDependent(PipelineInstanceModel pipelineInstanceModel) {
        dependencies.add(pipelineInstanceModel);
    }

    public String dependencyRevisionFor(PipelineInstanceModel downStream) {
        return upstreamStage(downStream).locator();
    }

    private StageInstanceModel upstreamStage(PipelineInstanceModel downStream) {
        DependencyMaterialConfig downstreamMaterial = dependencyMaterialFor(downStream);
        if (downstreamMaterial == null) {
            return null;
        }
        return pipeline().stage(CaseInsensitiveString.str(downstreamMaterial.getStageName()));
    }

    public DependencyMaterialConfig dependencyMaterialFor(PipelineInstanceModel downStream) {
        return downStream.findDependencyMaterial(new CaseInsensitiveString(pipeline().getName()));
    }

    public Boolean hasUpStreamRevisionFor(PipelineInstanceModel downStream) {
        StageInstanceModel instanceModel = upstreamStage(downStream);
        return instanceModel != null && StageResult.Passed.equals(instanceModel.getResult());
    }

    public static interface Filterer {
        boolean filterPipeline(String pipelineName);
    }

    public Map<String, TreeSet<PipelineInstanceModel>> groupedDependencies(){
        Map<String, TreeSet<PipelineInstanceModel>> pipelineInstanceModelses = new LinkedHashMap<>();
        for (PipelineInstanceModel dependency : dependencies) {
            if (!pipelineInstanceModelses.containsKey(dependency.getName())) {
                pipelineInstanceModelses.put(dependency.getName(), new TreeSet<>(new Comparator<PipelineInstanceModel>() {
                    public int compare(PipelineInstanceModel me, PipelineInstanceModel her) {
                        return her.getCounter().compareTo(me.getCounter());
                    }
                }));
            }
            pipelineInstanceModelses.get(dependency.getName()).add(dependency);
        }
        return pipelineInstanceModelses;
    }

    public void filterDependencies(Filterer filterer) {
        for (Iterator it = dependencies().iterator(); it.hasNext();) {
            PipelineInstanceModel pipelineInstanceModel = (PipelineInstanceModel) it.next();
            if (!filterer.filterPipeline(pipelineInstanceModel.getName())) {
                it.remove();
            }
        }
    }
}
