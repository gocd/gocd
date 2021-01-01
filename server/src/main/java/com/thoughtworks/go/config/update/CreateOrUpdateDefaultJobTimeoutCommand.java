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

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.ServerConfig;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;

import java.util.List;

public class CreateOrUpdateDefaultJobTimeoutCommand implements EntityConfigUpdateCommand<ServerConfig> {
    private final String defaultJobTimeout;
    private ServerConfig preprocessedServerConfig;

    public CreateOrUpdateDefaultJobTimeoutCommand(String defaultJobTimeout) {
        this.defaultJobTimeout = defaultJobTimeout;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        preprocessedConfig.server().setJobTimeout(defaultJobTimeout);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        preprocessedServerConfig = preprocessedConfig.server();
        preprocessedServerConfig.validate(new ConfigSaveValidationContext(preprocessedConfig));
        List<String> jobTimeoutErrors = preprocessedServerConfig.errors().getAllOn(ServerConfig.JOB_TIMEOUT);
        if (jobTimeoutErrors != null && !jobTimeoutErrors.isEmpty()) {
            throw new GoConfigInvalidException(preprocessedConfig, jobTimeoutErrors.get(0));
        }
        return true;
    }

    @Override
    public void clearErrors() {
    }

    @Override
    public ServerConfig getPreprocessedEntityConfig() {
        return preprocessedServerConfig;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return true;
    }
}
