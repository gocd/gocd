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

import java.util.List;

/**
 * @understands history of pipelines run within an environment
 */
public class Environment {
    private final String name;
    private List<PipelineModel> pipelinesModels;

    public Environment(String name, List<PipelineModel> pipelineModels) {
        this.name = name;
        pipelinesModels = pipelineModels;
    }

    public List<PipelineModel> getPipelineModels() {
        return pipelinesModels;
    }

    public String getName() {
        return name;
    }

    public boolean hasNewRevisions() {
        for (PipelineModel pipelinesModel : pipelinesModels) {
            if (pipelinesModel.hasNewRevisions()) {
                return true;
            }
        }
        return false;
    }
}
