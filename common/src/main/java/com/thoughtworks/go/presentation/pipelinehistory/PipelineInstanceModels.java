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
package com.thoughtworks.go.presentation.pipelinehistory;

import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.server.util.Pagination;

import java.util.List;

public class PipelineInstanceModels extends BaseCollection<PipelineInstanceModel> {
    private Pagination pagination;

    private PipelineInstanceModels(PipelineInstanceModel... instances) {
        super(instances);
    }

    public static PipelineInstanceModels createPipelineInstanceModels(PipelineInstanceModel... instances) {
        return new PipelineInstanceModels(instances);
    }

    public static PipelineInstanceModels createPipelineInstanceModels(List<PipelineInstanceModel> instances) {
        return new PipelineInstanceModels(instances.toArray(new PipelineInstanceModel[]{}));
    }

    public PipelineInstanceModel find(String pipelineName){
        PipelineInstanceModels found = findAll(pipelineName);
        return found.isEmpty() ? null : found.get(0);
    }

    public PipelineInstanceModels findAll(String pipelineName) {
        PipelineInstanceModels found = PipelineInstanceModels.createPipelineInstanceModels();
        for (PipelineInstanceModel pipelineInstanceModel : this) {
            if(pipelineInstanceModel.getName().equalsIgnoreCase(pipelineName)){
                found.add(pipelineInstanceModel);
            }
        }
        return found;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }


}
