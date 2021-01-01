/*
 * Copyright 2021 ThoughtWorks, Inc.
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

public enum JobState implements ViewableStatus {
    Unknown,
    Scheduled,  // Scheduled icon
    Assigned,   // Building icon
    Preparing,  // Building icon
    Building,   // Building icon
    Completing, // Building icon
    Completed,
    Discontinued, //This doesn't seem to be used anywhere. Leaving it as is but have a feeling this should die. (PS & RRR)
    Rescheduled,
    Paused,
    Waiting;

    @Override
    public String getStatus() {
        return this.toString().toLowerCase();
    }

    @Override
    public String getCruiseStatus() {
        return this.toString();
    }

    public String toLowerCase() {
        return getStatus().toLowerCase();
    }

    public boolean isAssignedOrScheduled() {
        return this == Assigned || this == Scheduled;
    }

    public boolean isBuilding() {
        return this == Building || this == Preparing || this == Completing;
    }

    public boolean isActive() {
        return isAssignedOrScheduled() || isBuilding();
    }

    public boolean isActiveOnAgent() {
        return this == Assigned || isBuilding();
    }

    public boolean isCompleted() {
        return this == Completed;
    }

    public boolean isPreparing() {
        return this == Preparing;
    }

    public String cctrayActivity() {
        return isActive() ? "Building" : "Sleeping";
    }

    public boolean isScheduled() {
        return this == Scheduled;
    }
}
