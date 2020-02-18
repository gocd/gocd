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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels;

public class PipelineHistoryItemMother {

    public static PipelineInstanceModel custom(String... stageNames) {
        PipelineInstanceModel pipelineInstanceModel = PipelineInstanceModel.createEmptyModel();
        pipelineInstanceModel.setStageHistory(new StageInstanceModels());
        for (String stageName : stageNames) {
            StageInstanceModel stageHistoryItem = new StageInstanceModel();
            stageHistoryItem.setName(stageName);
            pipelineInstanceModel.getStageHistory().add(stageHistoryItem);
        }
        return pipelineInstanceModel;
    }

    public static PipelineInstanceModel custom(StageInstanceModel... stages) {
        PipelineInstanceModel pipelineInstanceModel = PipelineInstanceModel.createEmptyModel();
        pipelineInstanceModel.setStageHistory(new StageInstanceModels());
        for (StageInstanceModel stage : stages) {
            pipelineInstanceModel.getStageHistory().add(stage);
        }
        return pipelineInstanceModel;
    }
}
