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

import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels;
import com.thoughtworks.go.server.presentation.models.PipelineHistoryGroups;
import com.thoughtworks.go.server.presentation.models.PipelineInstanceGroupModel;

public class PipelineHistoryGroupingUtil {

    public PipelineHistoryGroups createGroups(PipelineInstanceModels pipelineHistory) {
        PipelineHistoryGroups groups = new PipelineHistoryGroups();
        for (PipelineInstanceModel pipelineInstanceModel : pipelineHistory) {
            if (!groups.isEmpty() && groups.last().hasSameStagesAs(pipelineInstanceModel)) {
                groups.last().getPipelineInstances().add(pipelineInstanceModel);
            } else {
                groups.add(createGroupFor(pipelineInstanceModel));
            }
        }
        return groups;
    }

    private PipelineInstanceGroupModel createGroupFor(PipelineInstanceModel pipelineInstanceModel) {
        PipelineInstanceGroupModel group = new PipelineInstanceGroupModel();
        group.getPipelineInstances().add(pipelineInstanceModel);
        group.getStages().addAll(pipelineInstanceModel.getStageHistory());
        return group;
    }

}