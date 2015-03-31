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
package com.thoughtworks.go.plugin.access.artifactcleanup;

import java.util.ArrayList;
import java.util.List;

public class ArtifactExtensionStageInstance {

    private long id;

    private String pipelineName;

    private int pipelineCounter;

    private String stageName;

    private String stageCounter;

    private List<String> includePaths = new ArrayList<String>();

    private List<String> excludePaths = new ArrayList<String>();

    public ArtifactExtensionStageInstance(long id, String pipelineName, int pipelineCounter, String stageName, String stageCounter) {
        this.id = id;
        this.pipelineName = pipelineName;
        this.pipelineCounter = pipelineCounter;
        this.stageName = stageName;
        this.stageCounter = stageCounter;
    }

    public long getId() {
        return id;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public int getPipelineCounter() {
        return pipelineCounter;
    }

    public String getStageName() {
        return stageName;
    }

    public String getStageCounter() {
        return stageCounter;
    }

    public List<String> getIncludePaths() {
        return includePaths;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

}
