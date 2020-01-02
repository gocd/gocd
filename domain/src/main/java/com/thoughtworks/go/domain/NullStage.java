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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.util.TimeProvider;

public final class NullStage extends Stage {

    public NullStage(String stageName, JobInstances nullBuilds) {
        super(stageName, nullBuilds, null, null, null, new TimeProvider());
    }

    public NullStage(String stageName) {
        this(stageName, new JobInstances());
    }

    public static NullStage createNullStage(StageConfig stageConfig) {
        JobInstances nullBuilds = new JobInstances();
        for (JobConfig plan : stageConfig.allBuildPlans()) {
            nullBuilds.add(new NullJobInstance(CaseInsensitiveString.str(plan.name())));
        }
        NullStage nullStage = new NullStage(CaseInsensitiveString.str(stageConfig.name()), nullBuilds);
        nullStage.setPipelineId(10l);
        return nullStage;
    }

    public String buildLabel() {
        return "Unknown";
    }

    @Override
    public Stage mostRecent(Stage instance) {
        return instance;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public String stageLocator() {
        return "NULLSTAGE";
    }

    @Override
    public String stageLocatorForDisplay() {
        return stageLocator();
    }

    @Override
    public StageState stageState() {
        return StageState.Unknown;
    }

    @Override
    public StageState getState() {
        return StageState.Unknown;
    }

    @Override
    public boolean equals(Object o) {
        if (getClass() != o.getClass()) {
            return false;
        }
        Stage that = (Stage) o;
        return getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }


}
