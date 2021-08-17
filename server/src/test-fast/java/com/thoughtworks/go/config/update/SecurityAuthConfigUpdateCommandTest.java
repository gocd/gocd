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
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SecurityAuthConfigUpdateCommandTest {
    private Username currentUser;
    private GoConfigService goConfigService;
    private BasicCruiseConfig cruiseConfig;

    @BeforeEach
    public void setUp() throws Exception {
        currentUser = new Username("bob");
        goConfigService = mock(GoConfigService.class);
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
    }

    @Test
    public void shouldRaiseErrorWhenUpdatingNonExistentProfile() throws Exception {
        cruiseConfig.server().security().securityAuthConfigs().clear();
        SecurityAuthConfigUpdateCommand command = new SecurityAuthConfigUpdateCommand(null, new SecurityAuthConfig("foo", "ldap"), null, null, new HttpLocalizedOperationResult(), null, null);
        assertThatThrownBy(() -> command.update(cruiseConfig)).isInstanceOf(RecordNotFoundException.class);
    }

    @Test
    public void shouldUpdateExistingProfile() throws Exception {
        SecurityAuthConfig oldAuthConfig = new SecurityAuthConfig("foo", "ldap");
        SecurityAuthConfig newAuthConfig = new SecurityAuthConfig("foo", "github");

        cruiseConfig.server().security().securityAuthConfigs().add(oldAuthConfig);
        SecurityAuthConfigUpdateCommand command = new SecurityAuthConfigUpdateCommand(null, newAuthConfig, null, null, null, null, null);
        command.update(cruiseConfig);
        assertThat(cruiseConfig.server().security().securityAuthConfigs().find("foo"), is(equalTo(newAuthConfig)));
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfRequestIsNotFresh() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);

        SecurityAuthConfig oldAuthConfig = new SecurityAuthConfig("foo", "ldap");
        SecurityAuthConfig newAuthConfig = new SecurityAuthConfig("foo", "github");

        cruiseConfig.server().security().securityAuthConfigs().add(oldAuthConfig);

        EntityHashingService entityHashingService = mock(EntityHashingService.class);

        when(entityHashingService.hashForEntity(oldAuthConfig)).thenReturn("digest");

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        SecurityAuthConfigUpdateCommand command = new SecurityAuthConfigUpdateCommand(goConfigService, newAuthConfig, null, currentUser, result, entityHashingService, "bad-digest");

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.toString(), containsString("Someone has modified the configuration for"));;
    }
}
