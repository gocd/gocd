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

package com.thoughtworks.go.apiv1.commandrepository;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.ServerConfig;
import com.thoughtworks.go.config.update.UpdateCommandRepoLocationCommand;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCommandRepoLocationCommandTest {

    @Test
    void shouldUpdateCommandRepoLocation() throws Exception {
        String commandRepoLocation = "updated-location";
        UpdateCommandRepoLocationCommand updateCommandRepoLocationCommand = new UpdateCommandRepoLocationCommand(commandRepoLocation);
        CruiseConfig preprocessedConfig = new BasicCruiseConfig();
        updateCommandRepoLocationCommand.update(preprocessedConfig);

        assertThat(preprocessedConfig.server().getCommandRepositoryLocation(), is(commandRepoLocation));
    }

    @Test
    void shouldReturnTrueForValidCommandRepoLocation() throws Exception {
        String commandRepoLocation = "updated-location";
        UpdateCommandRepoLocationCommand updateCommandRepoLocationCommand = new UpdateCommandRepoLocationCommand(commandRepoLocation);
        CruiseConfig preprocessedConfig = new BasicCruiseConfig();
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setCommandRepositoryLocation(commandRepoLocation);
        preprocessedConfig.setServerConfig(serverConfig);

        assertTrue(updateCommandRepoLocationCommand.isValid(preprocessedConfig));
    }

    @Test
    void shouldReturnFalseForInvalidCommandRepoLocation() throws Exception {
        String commandRepoLocation = "";
        UpdateCommandRepoLocationCommand updateCommandRepoLocationCommand = new UpdateCommandRepoLocationCommand(commandRepoLocation);
        CruiseConfig preprocessedConfig = new BasicCruiseConfig();
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setCommandRepositoryLocation(commandRepoLocation);
        preprocessedConfig.setServerConfig(serverConfig);

        assertFalse(updateCommandRepoLocationCommand.isValid(preprocessedConfig));
        assertThat(preprocessedConfig.server().errors().on(ServerConfig.COMMAND_REPO_LOCATION), is("Command Repository Location cannot be empty"));
    }

    @Test
    void shouldClearErrorsAfterValidation() {
        CruiseConfig preprocessedConfig = new BasicCruiseConfig();
        UpdateCommandRepoLocationCommand command = new UpdateCommandRepoLocationCommand("");
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setCommandRepositoryLocation("");
        preprocessedConfig.setServerConfig(serverConfig);
        command.isValid(preprocessedConfig);

        assertFalse(preprocessedConfig.server().errors().isEmpty());

        command.clearErrors();

        assertTrue(preprocessedConfig.server().errors().isEmpty());
    }

    @Test
    void shouldAllowedContinue() {
        CruiseConfig preprocessedConfig = new BasicCruiseConfig();
        UpdateCommandRepoLocationCommand command = new UpdateCommandRepoLocationCommand("");
        
        assertTrue(command.canContinue(preprocessedConfig));
    }
}
