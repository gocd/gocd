/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.notificationdata;

import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.buildcause.BuildCause;

import java.io.Serializable;
import java.util.List;

public class StageNotificationData implements Serializable {
    private Stage stage;
    private BuildCause buildCause;
    private String pipelineGroup;
    private List<String> stages;

    public StageNotificationData(Stage stage, BuildCause buildCause, String pipelineGroup, List<String> stages) {
        this.stage = stage;
        this.buildCause = buildCause;
        this.pipelineGroup = pipelineGroup;
        this.stages = stages;
    }

    public Stage getStage() {
        return stage;
    }

    public BuildCause getBuildCause() {
        return buildCause;
    }

    public String getPipelineGroup() {
        return pipelineGroup;
    }

    public List<String> getStages() {
        return stages;
    }
}
