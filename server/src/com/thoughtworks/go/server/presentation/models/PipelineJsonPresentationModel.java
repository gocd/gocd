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

import com.thoughtworks.go.domain.PipelinePauseInfo;
import com.thoughtworks.go.util.json.Json;
import com.thoughtworks.go.util.json.JsonAware;
import com.thoughtworks.go.util.json.JsonList;
import com.thoughtworks.go.util.json.JsonMap;

//TODO: Rename to PipelineActivity
public class PipelineJsonPresentationModel implements JsonAware {
    private final String groupName;
    private String name;
    private PipelinePauseInfo pauseInfo;
    private boolean forcedBuild;
    private boolean canForce;
    private List<StageJsonPresentationModel> stages;
    private boolean canPause;

    public PipelineJsonPresentationModel(String groupName, String pipelineName, PipelinePauseInfo pauseInfo,
                                         boolean forcedBuild,
                                         List<StageJsonPresentationModel> stages) {
        this.groupName = groupName;
        this.name = pipelineName;
        this.pauseInfo = pauseInfo;
        this.forcedBuild = forcedBuild;
        this.stages = stages;
    }

    public Json toJson() {
        JsonMap pipelineJson = new JsonMap();
        pipelineJson.put("name", name);
        pipelineJson.put("group", groupName);
        pipelineJson.put("paused", String.valueOf(pauseInfo.isPaused()));
        pipelineJson.put("pauseCause", pauseInfo.getPauseCause());
        pipelineJson.put("pauseBy", pauseInfo.getPauseBy());
        pipelineJson.put("stages", stagesJson(getStages()));
        pipelineJson.put("forcedBuild", String.valueOf(forcedBuild));
        pipelineJson.put("canForce", String.valueOf(canForce));
        pipelineJson.put("canPause", String.valueOf(canPause));
        return pipelineJson;
    }

    private JsonList stagesJson(List<StageJsonPresentationModel> stagesJson) {
        JsonList infos = new JsonList();
        for (StageJsonPresentationModel stageJson : stagesJson) {
            infos.add(stageJson.toJson());
        }
        return infos;
    }

    public String getName() {
        return name;
    }

    public void setCanForce(boolean canForce) {
        this.canForce = canForce;
    }

    public List<StageJsonPresentationModel> getStages() {
        return stages;
    }

    public int getStageCount() {
        return stages.size();
    }

    public StageJsonPresentationModel getLatestStage(int index) {
        return stages.get(index);
    }

    public boolean getCanForce() {
        return canForce;
    }

    public String getGroupName() {
        return groupName;
    }

    public boolean getCanPause() {
        return canPause;
    }

    public void setCanPause(boolean canPause) {
        this.canPause = canPause;
    }
}
