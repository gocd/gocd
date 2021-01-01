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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.EnvironmentsConfig;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.merge.MergeEnvironmentConfig;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;

public class UpdateEnvironmentCommand extends EnvironmentCommand {

    private final String oldEnvironmentConfigName;
    private String digest;
    private EntityHashingService hashingService;

    public UpdateEnvironmentCommand(GoConfigService goConfigService, String oldEnvironmentConfigName, EnvironmentConfig newEnvironmentConfig, Username username, String actionFailed, String digest, EntityHashingService hashingService, HttpLocalizedOperationResult result) {
        super(actionFailed, newEnvironmentConfig, result, goConfigService, username);
        this.oldEnvironmentConfigName = oldEnvironmentConfigName;
        this.digest = digest;
        this.hashingService = hashingService;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) {
        EnvironmentsConfig environments = preprocessedConfig.getEnvironments();
        EnvironmentConfig envToRemove = environments.find(new CaseInsensitiveString(oldEnvironmentConfigName));
        int index = environments.indexOf(envToRemove);
        environments.remove(index);
        environments.add(index, environmentConfig);
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return super.canContinue(cruiseConfig) && isRequestFresh(cruiseConfig);
    }

    private boolean isRequestFresh(CruiseConfig cruiseConfig) {
        EnvironmentConfig config = cruiseConfig.getEnvironments().find(new CaseInsensitiveString(oldEnvironmentConfigName));
        if(config instanceof MergeEnvironmentConfig){
            config = ((MergeEnvironmentConfig) config).getFirstEditablePart();
        }

        boolean freshRequest =  hashingService.hashForEntity(config).equals(digest);
        if (!freshRequest) {
            result.stale(EntityType.Environment.staleConfig(oldEnvironmentConfigName));
        }
        return freshRequest;
    }
}

