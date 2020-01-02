/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.util.TimeProvider;

import java.util.Date;

import static com.thoughtworks.go.util.GoConstants.NEVER;

public class NullJobInstance extends JobInstance {
    public NullJobInstance(String name) {
        super(name, new TimeProvider());
    }

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public JobState getState() {
        return JobState.Unknown;
    }

    @Override
    public JobResult getResult() {
        return JobResult.Unknown;
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public Date getStartedDateFor(JobState jobState) {
        return NEVER;
    }

    @Override
    public JobState currentStatus() {
        return JobState.Waiting;
    }

    @Override
    public String displayStatusWithResult() {
        return getState().toLowerCase();
    }

    @Override
    public JobInstance mostRecentPassed(JobInstance champion) {
        return champion;
    }

    @Override
    public String buildLocator() {
        return "NULLJOB";
    }

    @Override
    public String buildLocatorForDisplay() {
        return buildLocator();
    }
}
