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

import java.util.List;

import com.google.gson.annotations.Expose;
import com.thoughtworks.go.config.CaseInsensitiveString;

public class PipelineViewModel implements Comparable<PipelineViewModel> {
    @Expose
    private String name;

    private final CaseInsensitiveString caseInsensitiveNameForComparison;

    @Expose
    private List<StageViewModel> stages;

    public PipelineViewModel(String pipelineName, List<StageViewModel> stageModels) {
        this.name = pipelineName;
        this.caseInsensitiveNameForComparison = new CaseInsensitiveString(pipelineName);
        this.stages = stageModels;
    }

    public String name() {
        return name;
    }

    @Override
    public int compareTo(PipelineViewModel that) {
        return caseInsensitiveNameForComparison.compareTo(that.caseInsensitiveNameForComparison);
    }

    public static class StageViewModel {
        @Expose
        private String stageName;

        public StageViewModel(String stageName) {
            this.stageName = stageName;
        }
    }
}
