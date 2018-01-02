/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.EnvironmentsConfig;
import com.thoughtworks.go.config.UpdateConfigCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateEnvironmentsCommand implements UpdateConfigCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateEnvironmentsCommand.class);

    private String uuid;
    private String environments;

    public UpdateEnvironmentsCommand(String uuid, String environments) {
        this.uuid = uuid;
        this.environments = environments;
    }

    public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
        EnvironmentsConfig allEnvironments = cruiseConfig.getEnvironments();
        for (String environment : environments.split(",")) {
            String environmentName = environment.trim();
            if (allEnvironments.hasEnvironmentNamed(new CaseInsensitiveString(environmentName))) {
                allEnvironments.named(new CaseInsensitiveString(environmentName)).addAgent(uuid);
            } else {
                LOGGER.warn("[Agent Auto Registration] Agent with uuid {} could not be assigned to environment {} as it does not exist.", uuid, environmentName);
            }
        }
        return cruiseConfig;
    }
}