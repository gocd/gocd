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

import com.thoughtworks.go.config.BasicEnvironmentConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

public class AddEnvironmentCommand extends EnvironmentCommand {

    private final LocalizedOperationResult result;

    public AddEnvironmentCommand(GoConfigService goConfigService, EnvironmentConfig environmentConfig, Username username, String actionFailed, LocalizedOperationResult result) {
        super(actionFailed, environmentConfig, result, goConfigService, username);
        this.result = result;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) {
        preprocessedConfig.addEnvironment((BasicEnvironmentConfig) environmentConfig);
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return super.canContinue(cruiseConfig) && !doesEnvironmentAlreadyExist();
    }


    private boolean doesEnvironmentAlreadyExist() {
        CaseInsensitiveString environmentName = environmentConfig.name();
        if (goConfigService.hasEnvironmentNamed(environmentName)) {
            result.conflict(EntityType.Environment.alreadyExists(environmentName));
            return true;
        }
        return false;
    }
}
