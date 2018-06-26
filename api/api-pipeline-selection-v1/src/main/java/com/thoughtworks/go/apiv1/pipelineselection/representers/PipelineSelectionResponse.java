/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.pipelineselection.representers;

import com.thoughtworks.go.config.PipelineConfigs;

import java.util.List;

public class PipelineSelectionResponse {
    private final List<String> pipelines;
    private final boolean blacklist;
    private final List<PipelineConfigs> pipelineConfigs;

    public PipelineSelectionResponse(List<String> pipelines, boolean blacklist, List<PipelineConfigs> pipelineConfigs) {
        this.pipelines = pipelines;
        this.blacklist = blacklist;
        this.pipelineConfigs = pipelineConfigs;
    }

    public List<String> selections() {
        return pipelines;
    }

    public boolean blacklist() {
        return blacklist;
    }

    public List<PipelineConfigs> getPipelineConfigs() {
        return pipelineConfigs;
    }
}
