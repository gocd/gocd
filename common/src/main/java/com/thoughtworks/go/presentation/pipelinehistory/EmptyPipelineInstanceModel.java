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

import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Revision;

/**
 * @understands empty pipeline instance for scenarios where there is no instance to be shown
 */
public class EmptyPipelineInstanceModel extends PipelineInstanceModel {
    EmptyPipelineInstanceModel(String pipelineName, BuildCause withEmptyModifications, StageInstanceModels stageHistory) {
        super(pipelineName, -1, "unknown", withEmptyModifications, stageHistory);
        setCounter(0);
        setId(-1);
    }

    @Override public boolean hasHistoricalData() {
        return false;
    }

    @Override public Revision getCurrentRevision(String requestedMaterialName) {
        return UNKNOWN_REVISION;
    }    
}
