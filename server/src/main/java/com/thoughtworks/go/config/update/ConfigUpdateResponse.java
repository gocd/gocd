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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.ConfigAwareUpdate;
import com.thoughtworks.go.config.ConfigSaveState;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.Validatable;

public final class ConfigUpdateResponse {
    private final CruiseConfig cruiseConfig;
    private final Validatable node;
    private final Validatable subject;
    private final ConfigSaveState configSaveState;
    private final CruiseConfig configAfter;

    public ConfigUpdateResponse(CruiseConfig cruiseConfig, Validatable node, Validatable subject, ConfigAwareUpdate updateCommand, ConfigSaveState configSaveState) {
        this.cruiseConfig = cruiseConfig;
        this.node = node;
        this.subject = subject;
        this.configSaveState = configSaveState;
        this.configAfter = updateCommand.configAfter();
    }

    public CruiseConfig getCruiseConfig() {
        return cruiseConfig;
    }

    public Validatable getNode() {
        return node;
    }

    public CruiseConfig configAfterUpdate() {
        return configAfter;
    }

    public Validatable getSubject() {
        return subject;
    }

    public Boolean wasMerged() {
        return ConfigSaveState.MERGED.equals(configSaveState);
    }
}
