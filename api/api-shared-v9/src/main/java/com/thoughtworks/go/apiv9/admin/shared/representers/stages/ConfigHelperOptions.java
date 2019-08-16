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

package com.thoughtworks.go.apiv9.admin.shared.representers.stages;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.materials.PasswordDeserializer;

public class ConfigHelperOptions {
    private final CruiseConfig cruiseConfig;
    private final PasswordDeserializer passwordDeserializer;

    public ConfigHelperOptions(CruiseConfig cruiseConfig, PasswordDeserializer passwordDeserializer) {
        this.cruiseConfig = cruiseConfig;
        this.passwordDeserializer = passwordDeserializer;
    }

    public CruiseConfig getCruiseConfig() {
        return cruiseConfig;
    }

    public PasswordDeserializer getPasswordDeserializer() {
        return passwordDeserializer;
    }
}
