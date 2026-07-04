/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.domain.cctray;

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.NullStage;
import com.thoughtworks.go.domain.Stage;
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

        cache.replaceForStage(stage.getIdentifier().getPipelineName(), stage.getName(), statusesOfStageAndItsJobsFor(stage));
    }

    public List<ProjectStatus> statusesOfStageAndItsJobsFor(Stage stage) {
        List<ProjectStatus> statuses = new ArrayList<>();

        Set<String> breakers = breakersCalculator.calculateFor(stage);
        ProjectStatus stageStatus = getStageStatus(stage, breakers);
        statuses.add(stageStatus);

        for (JobInstance jobInstance : stage.getJobInstances()) {
            Set<String> jobBreakers = jobInstance.getResult() == JobResult.Failed ? breakers : Collections.emptySet();
            statuses.add(jobStatusChangeHandler.statusFor(jobInstance, stageStatus.stageOrder(), jobBreakers));
        }
        return statuses;
    }

    private ProjectStatus getStageStatus(Stage stage, Set<String> breakers) {
        ProjectStatus.Key key = new ProjectStatus.Key(stage.getIdentifier());
        ProjectStatus existingStatus = cache.getOrDefault(key, stage.getOrderId());

        ProjectStatus newStatus = new ProjectStatus(
            key,
            stage.getOrderId(),
            stage.stageState().cctrayActivity(),
            lastBuildStatus(stage, existingStatus),
            lastBuildLabel(stage, existingStatus),
            lastBuildTime(stage, existingStatus),
            stage.getIdentifier().webPathAfterContext(),
            breakers);
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
}
