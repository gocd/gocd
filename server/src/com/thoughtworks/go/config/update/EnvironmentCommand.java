/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.validation.EnvironmentAgentValidator;
import com.thoughtworks.go.config.validation.EnvironmentPipelineValidator;
import com.thoughtworks.go.config.validation.GoConfigValidator;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

import java.util.Arrays;
import java.util.List;

public class EnvironmentCommand {
    private Localizable.CurryableLocalizable actionFailed;
    private EnvironmentConfig config;
    private LocalizedOperationResult result;
    private final List<GoConfigValidator> VALIDATORS = Arrays.asList(
            new EnvironmentAgentValidator(),
            new EnvironmentPipelineValidator()
    );


    public EnvironmentCommand(Localizable.CurryableLocalizable actionFailed, EnvironmentConfig config, LocalizedOperationResult result) {
        this.actionFailed = actionFailed;
        this.config = config;
        this.result = result;
    }

    public boolean isValid(CruiseConfig preprocessedConfig) {
        BasicEnvironmentConfig config = (BasicEnvironmentConfig) preprocessedConfig.getEnvironments().find(this.config.name());
        config.getVariables().validate(ConfigSaveValidationContext.forChain(config));
        List<ConfigErrors> allErrors = preprocessedConfig.getAllErrors();
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

    public EnvironmentConfig getPreprocessedEntityConfig() {
        return config;
    }
}
