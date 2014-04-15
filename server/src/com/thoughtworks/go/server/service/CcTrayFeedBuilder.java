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

package com.thoughtworks.go.server.service;

import java.util.ArrayList;

import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.domain.PipelineGroupVisitor;
import com.thoughtworks.go.domain.activity.CcTrayStatus;
import com.thoughtworks.go.domain.activity.ProjectStatus;

public class CcTrayFeedBuilder implements PipelineGroupVisitor {
    private CcTrayStatus ccTrayStatus;
    private final ArrayList<ProjectStatus> result;

    public CcTrayFeedBuilder(CcTrayStatus ccTrayStatus) {
        this.ccTrayStatus = ccTrayStatus;
        this.result = new ArrayList<ProjectStatus>();
    }

    public void visit(PipelineConfigs group) {
        for (PipelineConfig pipelineConfig : group) {
            for (StageConfig stageConfig : pipelineConfig) {

                String stageProjectName = String.format("%s :: %s", pipelineConfig.name(), stageConfig.name());

                ccTrayStatus.dumpProject(stageProjectName, result);

                for (JobConfig jobConfig : stageConfig.allBuildPlans()) {
                    String jobProjectName = String.format("%s :: %s :: %s", pipelineConfig.name(),
                            stageConfig.name(), jobConfig.name());
                    ccTrayStatus.dumpProject(jobProjectName, result);
                }
            }

        }
    }

    public ArrayList<ProjectStatus> getResult() {
        return result;
    }
}
