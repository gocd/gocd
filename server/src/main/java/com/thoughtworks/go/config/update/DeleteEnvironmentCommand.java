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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.merge.MergeConfigOrigin;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;

import java.util.List;
import java.util.stream.Collectors;

public class DeleteEnvironmentCommand extends EnvironmentCommand {
    public DeleteEnvironmentCommand(GoConfigService goConfigService, EnvironmentConfig environmentConfig, Username username, String actionFailed, HttpLocalizedOperationResult result) {
        super(actionFailed, environmentConfig, result, goConfigService, username);
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) {
        preprocessedConfig.getEnvironments().removeIf(envConfig -> environmentConfig.name().equals(envConfig.name()));
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        return true;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        if (!environmentConfig.isLocal()) {
            List<String> displayNames = ((MergeConfigOrigin) environmentConfig.getOrigin())
                    .stream().filter(configOrigin -> !configOrigin.isLocal())
                    .map(configOrigin -> ((RepoConfigOrigin) configOrigin).getConfigRepo().getId())
                    .collect(Collectors.toList());

            String message = String.format("Environment is partially defined in %s config repositories", displayNames);
            result.unprocessableEntity(LocalizedMessage.composite(actionFailed, message));
            return false;
        }
        return true;
    }
}
