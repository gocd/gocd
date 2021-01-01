/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv2.stageoperations.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.PaginationRepresenter;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels;
import com.thoughtworks.go.server.util.Pagination;

public class StageInstancesRepresenter {
    public static void toJSON(OutputWriter jsonWriter, StageInstanceModels stageInstanceModels, Pagination pagination) {
        jsonWriter.addChildList("stages", stageInstancesWriter -> stageInstanceModels.forEach(
                stageInstanceModel -> stageInstancesWriter.addChild(stageInstanceWriter -> StageInstanceRepresenter.toJSON(stageInstanceWriter, stageInstanceModel))));
        jsonWriter.addChild("pagination", paginationWriter -> PaginationRepresenter.toJSON(paginationWriter, pagination));
    }
}
