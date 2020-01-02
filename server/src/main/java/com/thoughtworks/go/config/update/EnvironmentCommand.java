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

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.domain.AllConfigErrors;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;


public abstract class EnvironmentCommand implements EntityConfigUpdateCommand<EnvironmentConfig> {
    protected String actionFailed;
    protected EnvironmentConfig environmentConfig;
    protected LocalizedOperationResult result;
    protected GoConfigService goConfigService;
    protected Username username;

    public EnvironmentCommand(String actionFailed, EnvironmentConfig environmentConfig, LocalizedOperationResult result, GoConfigService goConfigService, Username username) {
        this.actionFailed = actionFailed;
        this.environmentConfig = environmentConfig;
        this.result = result;
        this.goConfigService = goConfigService;
        this.username = username;
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        EnvironmentConfig config = preprocessedConfig.getEnvironments().find(this.environmentConfig.name());
        boolean isValid = config.validateTree(ConfigSaveValidationContext.forChain(preprocessedConfig), preprocessedConfig);

        if (!isValid) {
            String allErrors = new AllConfigErrors(preprocessedConfig.getAllErrors()).asString();
            result.unprocessableEntity(LocalizedMessage.composite(actionFailed, allErrors));
        }

        return isValid;
    }

    @Override
    public void clearErrors() {
        BasicCruiseConfig.clearErrors(environmentConfig);
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return true;
    }

    @Override
    public EnvironmentConfig getPreprocessedEntityConfig() {
        return environmentConfig;
    }
}
