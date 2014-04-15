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

package com.thoughtworks.go.server.presentation.models;

import java.util.Collection;
import java.util.Map;

import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels;

public class PipelineInstanceGroupModel {
    private StageConfigurationModels config;
    private PipelineInstanceModels pipelineInstances;
    private Map<String, StageIdentifier> latestStages;

    public void setLatestStages(Map<String, StageIdentifier> latestStages) {
        this.latestStages = latestStages;
    }

    public Map<String, StageIdentifier> getLatestStages() {
        return latestStages;
    }

    public PipelineInstanceGroupModel() {
        this(new StageConfigurationModels());
    }

    public PipelineInstanceGroupModel(StageConfigurationModels config) {
        this.config = config;
        pipelineInstances = PipelineInstanceModels.createPipelineInstanceModels();
    }

    @Deprecated
    public StageConfigurationModels getConfig() {
        return config;
    }

    public Collection<StageConfigurationModel> getStages() {
        return config;
    }

    public PipelineInstanceModels getPipelineInstances() {
        return pipelineInstances;
    }

    public boolean hasSameStagesAs(PipelineInstanceModel pipelineInstanceModel) {
        StageInstanceModels stageHistory = pipelineInstanceModel.getStageHistory();
        if (config.size() != stageHistory.size()) {
            return false;
        }
        for (int i = 0; i < config.size(); i++) {
            if (!equals(config.get(i), stageHistory.get(i))) {
                return false;
            }
        }
        return true;
    }

    public boolean match(PipelineConfig pipelineConfig) {
        return config.match(pipelineConfig);
    }

    private boolean equals(StageConfigurationModel obj1, StageConfigurationModel obj2) {
        return obj1.getName().equals(obj2.getName()) && obj1.isAutoApproved() == obj2.isAutoApproved();
    }

    public void add(PipelineInstanceModel instance) {
        pipelineInstances.add(instance);
    }

    public PipelineInstanceModel findPipelineInstance(String pipelineLabel) {
        if("latest".equals(pipelineLabel) && !pipelineInstances.isEmpty()){
            return pipelineInstances.first();
        }
        
        for (PipelineInstanceModel instance : pipelineInstances) {
            if (instance.getLabel().equals(pipelineLabel)) {
                return instance;
            }
        }
        return null;
    }
}
