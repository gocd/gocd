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

package com.thoughtworks.go.domain;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;


public class Stages extends BaseCollection<Stage> implements StageContainer {

    public static final Comparator<Stage> STAGE_COMPARATOR = (stage1, stage2) -> stage1.getOrderId() - stage2.getOrderId();
    
    public Stages() {
        super();
    }

    public Stages(Collection<Stage> stages) {
        this.addAll(stages);
    }

    public Stages(Stage... stage) {
        super(Arrays.asList(stage));
    }

    public boolean hasStage(String stageName) {
        for (Stage stage : this) {
            if (stage.getName().equalsIgnoreCase(stageName)) {
                return true;
            }
        }
        return false;
    }

    public String nextStageName(String stageName) {
        this.sort(STAGE_COMPARATOR);
        int index = indexOf(byName(stageName));
        if (index > -1 && index < size() - 1) {
            return get(index + 1).getName();
        }
        return null;
    }

    public Stage byName(String name) {
        for (Stage stage : this) {
            if (name.equals(stage.getName())) {
                return stage;
            }
        }
        return new NullStage(name, new JobInstances());
    }

    public Stage byId(long stageId) {
        for (Stage stage : this) {
            if (stageId == stage.getId()) {
                return stage;
            }
        }
        throw new RuntimeException("Could not load stage with id " + stageId);
    }

    public boolean isAnyStageActive() {
        for (Stage stage : this) {
            if (stage.stageState().isActive()) {
                return true;
            }
        }
        return false;
    }

    public Stage byCounter(int counter) {
        for (Stage stage : this) {
            if (stage.getCounter() == counter) {
                return stage;
            }
        }
        throw new RuntimeException(
                "Cannot find a stage with counter '" + counter + "'."
                + " Actual stages are: " + this.toString());
    }


    public Stages latestStagesInRunOrder() {
        Stages latestRunStages = new Stages();
        for (Stage  stage: this) {
            if(stage.isLatestRun()) {
                latestRunStages.add(stage);
            }
        }
        latestRunStages.sort((s1, s2) -> new Integer(s1.getOrderId()).compareTo(s2.getOrderId()));
        return latestRunStages;
    }
}
