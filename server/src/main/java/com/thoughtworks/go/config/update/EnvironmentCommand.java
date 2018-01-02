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

import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.domain.AllConfigErrors;
import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

public class EnvironmentCommand {
    private Localizable.CurryableLocalizable actionFailed;
    private EnvironmentConfig config;
    private LocalizedOperationResult result;

    public EnvironmentCommand(Localizable.CurryableLocalizable actionFailed, EnvironmentConfig config, LocalizedOperationResult result) {
        this.actionFailed = actionFailed;
        this.config = config;
        this.result = result;
    }

    public boolean isValid(CruiseConfig preprocessedConfig) {
        EnvironmentConfig config = preprocessedConfig.getEnvironments().find(this.config.name());
        boolean isValid = config.validateTree(ConfigSaveValidationContext.forChain(preprocessedConfig), preprocessedConfig);

        if (!isValid) {
            String allErrors = new AllConfigErrors(preprocessedConfig.getAllErrors()).asString();
            result.unprocessableEntity(actionFailed.addParam(allErrors));
        }

        return isValid;
    }

    public EnvironmentConfig getPreprocessedEntityConfig() {
        return config;
    }
}
