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

import java.util.*;

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

        cache.putAll(statusesOfStageAndItsJobsFor(stage));
    }

    public List<ProjectStatus> statusesOfStageAndItsJobsFor(Stage stage) {
        ArrayList<ProjectStatus> statuses = new ArrayList<>();

        String projectName = stage.getIdentifier().ccProjectName();
        Set<String> breakers = breakersCalculator.calculateFor(stage);
        statuses.add(getStageStatus(stage, projectName, breakers));

        for (JobInstance jobInstance : stage.getJobInstances()) {
            Set<String> jobBreakers = jobInstance.getResult() == JobResult.Failed ? breakers : new HashSet<String>();
            statuses.add(jobStatusChangeHandler.statusFor(jobInstance, jobBreakers));
        }
        return statuses;
    }

    private ProjectStatus getStageStatus(Stage stage, String projectName, Set<String> breakers) {
        ProjectStatus existingStatus = projectByName(projectName);

        ProjectStatus newStatus = new ProjectStatus(
                projectName,
                stage.stageState().cctrayActivity(),
                lastBuildStatus(stage, existingStatus),
                lastBuildLabel(stage, existingStatus),
                lastBuildTime(stage, existingStatus),
                stage.getIdentifier().webUrl(), breakers);
        newStatus.updateViewers(existingStatus.viewers());

        return newStatus;
    }

    private String lastBuildStatus(Stage stage, ProjectStatus existingStatus) {
        return stage.stageState().completed()
                ? stage.stageState().cctrayStatus()
                : existingStatus.getLastBuildStatus();
    }

    private Date lastBuildTime(Stage stage, ProjectStatus existingStatus) {
        return stage.stageState().completed()
                ? stage.completedDate()
                : existingStatus.getLastBuildTime();
    }

    private String lastBuildLabel(Stage stage, ProjectStatus existingStatus) {
        return stage.stageState().completed()
                ? stage.getIdentifier().ccTrayLastBuildLabel()
                : existingStatus.getLastBuildLabel();
    }

    private ProjectStatus projectByName(String projectName) {
        ProjectStatus projectStatus = cache.get(projectName);
        return projectStatus == null ? new ProjectStatus.NullProjectStatus(projectName) : projectStatus;
    }
}
