/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.web.i18n;

import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.ViewableStatus;
import org.apache.commons.lang.StringUtils;

@Deprecated
public final class CurrentStatus implements ViewableStatus {


    public static final CurrentStatus QUEUED =
            new CurrentStatus("Queued", "in build queue", JobState.Scheduled);
    public static final CurrentStatus BOOTSTRAPPING =
            new CurrentStatus("Bootstrapping", "bootstrapping", JobState.Preparing);
    public static final CurrentStatus MODIFICATIONSET =
            new CurrentStatus("ModificationSet", "checking for modifications", JobState.Preparing);
    public static final CurrentStatus BUILDING =
            new CurrentStatus("Building", "now building", JobState.Building);
    public static final CurrentStatus WAITING =
            new CurrentStatus("Waiting", "waiting for next time to build", JobState.Waiting);
    public static final CurrentStatus PAUSED = new CurrentStatus("Paused", "paused", JobState.Paused);
    public static final CurrentStatus DISCONTINUED =
            new CurrentStatus("Discontinued", "Discontinued", JobState.Discontinued);

    private static final CurrentStatus[] STATUSES =
            new CurrentStatus[]{QUEUED, BOOTSTRAPPING, MODIFICATIONSET, BUILDING, WAITING, PAUSED,
                    DISCONTINUED};

    private String status;

    private String cruiseStatus;
    private final JobState jobState;

    public JobState getBuildInstanceState() {
        return jobState;
    }

    public static CurrentStatus getProjectBuildStatus(String statusStr) {
        for (CurrentStatus status : STATUSES) {
            if (StringUtils.indexOf(statusStr, status.getCruiseStatus()) == 0) {
                return status;
            }
        }
        return DISCONTINUED;
    }

    private CurrentStatus(String status, String cruiseStatus, JobState jobState) {
        this.status = status;
        this.cruiseStatus = cruiseStatus;
        this.jobState = jobState;
    }

    public String getStatus() {
        return status;
    }

    public String getCruiseStatus() {
        return cruiseStatus;
    }

    public String toString() {
        return getCruiseStatus();
    }
}
