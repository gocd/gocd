/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.presentation.models;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.server.presentation.models.StageConfigurationModel;

public class StageInfoAdapter implements StageConfigurationModel {
    private final StageConfig config;
    //TODO: remove it
    private StageIdentifier mostRecentStage;

    public StageInfoAdapter(StageConfig stageConfig) {
        this.config = stageConfig;
    }

    public String getName() {
        return CaseInsensitiveString.str(config.name());
    }

    public boolean isAutoApproved() {
        return !config.requiresApproval();
    }

    public boolean equals(Object o) {
        return equals(this, (StageConfigurationModel) o);
    }

    public int hashCode() {
        return (getName() + isAutoApproved()).hashCode();
    }

    public static boolean equals(StageConfigurationModel obj1, StageConfigurationModel obj2) {
        return obj1.getName().equals(obj2.getName()) && obj1.isAutoApproved() == obj2.isAutoApproved();
    }

    public void setMostRecent(StageIdentifier mostRecentStage) {
        this.mostRecentStage = mostRecentStage;
    }
}
