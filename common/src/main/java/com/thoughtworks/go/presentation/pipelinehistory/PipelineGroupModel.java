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
package com.thoughtworks.go.presentation.pipelinehistory;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.domain.PipelinePauseInfo;

/**
 * @understands group level aggregation of active pipelines
 */
public class PipelineGroupModel {
    private String name;
    private List<PipelineModel> pipelineModels = new ArrayList<>();

    public PipelineGroupModel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void add(PipelineModel pipelineModel) {
        PipelineModel model = getExistingPipelineModelOrCacheThisOneAndGetItBack(new PipelineModel(pipelineModel, false));
        for (PipelineInstanceModel pipelineInstanceModel : pipelineModel.getActivePipelineInstances()) {
            model.addPipelineInstance(pipelineInstanceModel);
        }
    }

    public List<PipelineModel> getPipelineModels() {
        return new ArrayList<>(pipelineModels);
    }

    public PipelineModel pipelineModelForPipelineName(String pipelineName, boolean canForce, boolean canOperate, PipelinePauseInfo pipelinePauseInfo) {
        return getExistingPipelineModelOrCacheThisOneAndGetItBack(new PipelineModel(pipelineName, canForce, canOperate, pipelinePauseInfo));
    }

    public boolean containsPipeline(String pipelineName) {
        return getPipelineModel(pipelineName) != null;
    }

    public PipelineModel getPipelineModel(String pipelineName) {
        for (PipelineModel pipelineModel : pipelineModels) {
            if (pipelineModel.getName().equalsIgnoreCase(pipelineName)) {
                return pipelineModel;
            }
        }
        return null;
    }

    public void remove(PipelineModel pipelineModel) {
        pipelineModels.remove(pipelineModel);
    }

    private PipelineModel getExistingPipelineModelOrCacheThisOneAndGetItBack(PipelineModel model) {
        if (!containsPipeline(model.getName())) {
            pipelineModels.add(model);
        }
        return getPipelineModel(model.getName());
    }
}
