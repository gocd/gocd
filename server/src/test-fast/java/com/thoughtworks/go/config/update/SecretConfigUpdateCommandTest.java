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
import com.thoughtworks.go.config.SecretConfig;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SecretConfigUpdateCommandTest {
    private Username currentUser;
    private GoConfigService goConfigService;
    private BasicCruiseConfig cruiseConfig;

    @Before
    public void setUp() throws Exception {
        currentUser = new Username("bob");
        goConfigService = mock(GoConfigService.class);
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
    }

    @Test
    public void shouldRaiseErrorWhenUpdatingNonExistentSecretConfig() throws Exception {
        cruiseConfig.getSecretConfigs().clear();

        SecretConfig secretConfig = new SecretConfig("foo", "docker");
        SecretConfigUpdateCommand command = new SecretConfigUpdateCommand(null, secretConfig, null, null, new HttpLocalizedOperationResult(), null, null);

        Assertions.assertThrows(RecordNotFoundException.class, () -> command.update(cruiseConfig));
    }

    @Test
    public void shouldUpdateExistingSecretConfig() throws Exception {
        SecretConfig oldConfig = new SecretConfig("foo", "docker");
        SecretConfig newConfig = new SecretConfig("foo", "aws");

        cruiseConfig.getSecretConfigs().add(oldConfig);
        SecretConfigUpdateCommand command = new SecretConfigUpdateCommand(null, newConfig, null, null, null, null, null);
        command.update(cruiseConfig);
        assertThat(cruiseConfig.getSecretConfigs().find("foo"), is(equalTo(newConfig)));
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfRequestIsNotFresh() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);

        SecretConfig oldConfig = new SecretConfig("foo", "docker");
        SecretConfig newConfig = new SecretConfig("foo", "aws");

        cruiseConfig.getSecretConfigs().add(oldConfig);

        EntityHashingService entityHashingService = mock(EntityHashingService.class);

        when(entityHashingService.hashForEntity(oldConfig)).thenReturn("digest");

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        SecretConfigUpdateCommand command = new SecretConfigUpdateCommand(goConfigService, newConfig, null, currentUser, result, entityHashingService, "bad-digest");

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.toString(), containsString("Someone has modified the configuration for"));
    }
}
