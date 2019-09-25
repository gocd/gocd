/*
 * Copyright 2019 ThoughtWorks, Inc.
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

public class UpdateCommandRepoLocationCommand implements EntityConfigUpdateCommand<ServerConfig> {
    private String commandRepoLocation;
    private ServerConfig preprocessedServerConfig;

    public UpdateCommandRepoLocationCommand(String commandRepoLocation) {
        this.commandRepoLocation = commandRepoLocation;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        preprocessedConfig.server().setCommandRepositoryLocation(commandRepoLocation);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        preprocessedServerConfig = preprocessedConfig.server();

        preprocessedServerConfig.validate(new ConfigSaveValidationContext(preprocessedConfig));
        return preprocessedServerConfig.errors().isEmpty();
    }

    @Override
    public void clearErrors() {
        BasicCruiseConfig.clearErrors(preprocessedServerConfig);
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
