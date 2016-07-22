/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.validation.EnvironmentAgentValidator;
import com.thoughtworks.go.config.validation.EnvironmentPipelineValidator;
import com.thoughtworks.go.config.validation.GoConfigValidator;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;

import java.util.Arrays;
import java.util.List;

public class AddEnvironmentCommand implements EntityConfigUpdateCommand<EnvironmentConfig> {

    private final GoConfigService goConfigService;
    private final BasicEnvironmentConfig environmentConfig;
    private final Username user;
    private final LocalizedOperationResult result;
    private final List<GoConfigValidator> VALIDATORS = Arrays.asList(
            new EnvironmentAgentValidator(),
            new EnvironmentPipelineValidator()
    );

    public AddEnvironmentCommand(GoConfigService goConfigService, BasicEnvironmentConfig environmentConfig, Username user, LocalizedOperationResult result) {
        this.goConfigService = goConfigService;
        this.environmentConfig = environmentConfig;
        this.user = user;
        this.result = result;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        preprocessedConfig.addEnvironment(environmentConfig);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        BasicEnvironmentConfig config = (BasicEnvironmentConfig) preprocessedConfig.getEnvironments().find(environmentConfig.name());
        config.getVariables().validate(ConfigSaveValidationContext.forChain(config));
        List<ConfigErrors> allErrors = preprocessedConfig.getAllErrors();
        Localizable.CurryableLocalizable actionFailed = LocalizedMessage.string("ENV_ADD_FAILED");
        if (!allErrors.isEmpty()) {
            result.badRequest(actionFailed.addParam(allErrors.get(0).firstError()));
            return false;
        }

        for (GoConfigValidator validator : VALIDATORS) {
            try {
                validator.validate(preprocessedConfig);
            } catch (Exception e) {
                result.badRequest(actionFailed.addParam(e.getMessage()));
                return false;
            }
        }
        return true;
    }

    @Override
    public void clearErrors() {
        BasicCruiseConfig.clearErrors(environmentConfig);
    }

    @Override
    public EnvironmentConfig getPreprocessedEntityConfig() {
        return environmentConfig;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        if (!goConfigService.isUserAdmin(user)) {
            Localizable noPermission = LocalizedMessage.string("NO_PERMISSION_TO_ADD_ENVIRONMENT", user.getDisplayName());
            result.unauthorized(noPermission, HealthStateType.unauthorised());
            return false;
        }
        CaseInsensitiveString environmentName = environmentConfig.name();
        if (goConfigService.hasEnvironmentNamed(environmentName)) {
            result.conflict(LocalizedMessage.string("CANNOT_ADD_ENV_ALREADY_EXISTS", environmentName));
            return false;
        }
        return true;
    }
}