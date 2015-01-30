/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.cctray;

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.NullStage;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.activity.ProjectStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/* Understands what needs to be done to keep the CCTray cache updated, when a stage status changes. */
@Component
public class CcTrayStageStatusChangeHandler {
    private final CcTrayCache cache;
    private final CcTrayJobStatusChangeHandler jobStatusChangeHandler;
    private final CcTrayBreakersCalculator breakersCalculator;

    @Autowired
    public CcTrayStageStatusChangeHandler(CcTrayCache cache, CcTrayJobStatusChangeHandler jobStatusChangeHandler, CcTrayBreakersCalculator breakersCalculator) {
        this.cache = cache;
        this.jobStatusChangeHandler = jobStatusChangeHandler;
        this.breakersCalculator = breakersCalculator;
    }

    public void call(Stage stage) {
        if (stage instanceof NullStage) {
            return;
        }

        String projectName = stage.getIdentifier().ccProjectName();
        Set<String> breakers = breakersCalculator.calculateFor(stage);
        cacheStage(stage, projectName, breakers);

        for (JobInstance jobInstance : stage.getJobInstances()) {
            Set<String> jobBreakers = jobInstance.getResult() == JobResult.Failed ? breakers : new HashSet<String>();
            jobStatusChangeHandler.updateForJob(jobInstance, jobBreakers);
        }
    }

    private void cacheStage(Stage stage, String projectName, Set<String> breakers) {
        cache.replace(projectName, new ProjectStatus(
                projectName,
                stage.stageState().cctrayActivity(),
                lastBuildStatus(projectName, stage),
                lastBuildLabel(projectName, stage),
                lastBuildTime(projectName, stage),
                stage.getIdentifier().webUrl(), breakers));
    }

    private String lastBuildStatus(String projectName, Stage stage) {
        return stage.stageState().completed()
                ? stage.stageState().cctrayStatus()
                : projectByName(projectName).getLastBuildStatus();
    }

    private Date lastBuildTime(String projectName, Stage stage) {
        return stage.stageState().completed()
                ? stage.completedDate()
                : projectByName(projectName).getLastBuildTime();
    }

    private String lastBuildLabel(String projectName, Stage stage) {
        return stage.stageState().completed()
                ? stage.getIdentifier().ccTrayLastBuildLabel()
                : projectByName(projectName).getLastBuildLabel();
    }

    private ProjectStatus projectByName(String projectName) {
        ProjectStatus projectStatus = cache.get(projectName);
        return projectStatus == null ? new ProjectStatus.NullProjectStatus(projectName) : projectStatus;
    }
}
