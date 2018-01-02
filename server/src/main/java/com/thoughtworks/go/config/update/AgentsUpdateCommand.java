/*
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
 */

package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.Agents;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.UpdateConfigCommand;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.validation.ConfigUpdateValidator;

public class AgentsUpdateCommand implements EntityConfigUpdateCommand<Agents> {
    private final UpdateConfigCommand command;
    private final ConfigUpdateValidator validator;
    private CruiseConfig updatedConfig;

    public AgentsUpdateCommand(UpdateConfigCommand command, ConfigUpdateValidator validator) {
        this.command = command;
        this.validator = validator;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return true;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        updatedConfig = command.update(preprocessedConfig);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        return validator.isValid(preprocessedConfig);
    }

    @Override
    public void clearErrors() {
    }

    @Override
    public Agents getPreprocessedEntityConfig() {
        return updatedConfig.agents();
    }
}

