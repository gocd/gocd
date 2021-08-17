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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreatePipelineConfigsCommandTest {
    @Mock
    private SecurityService securityService;

    private Username user;
    private BasicCruiseConfig cruiseConfig;

    @BeforeEach
    public void setup() {
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.server().security().addRole(new RoleConfig("validRole"));
        user = new Username(new CaseInsensitiveString("user"));
    }

    @Test
    public void shouldCreatePipelineConfigs() {
        Authorization authorization = new Authorization(new AdminsConfig(new AdminRole(new CaseInsensitiveString("foo"))));
        PipelineConfigs newPipelineConfigs = new BasicPipelineConfigs("group", authorization);

        CreatePipelineConfigsCommand command = new CreatePipelineConfigsCommand(newPipelineConfigs, user, new HttpLocalizedOperationResult(), securityService);
        command.update(cruiseConfig);

        assertThat(cruiseConfig.hasPipelineGroup("group"), is(true));
        assertThat(cruiseConfig.findGroup("group").getAuthorization(), is(authorization));
    }

    @Test
    public void commandShouldBeValid_whenRoleIsValid() {
        Authorization authorization = new Authorization(new AdminsConfig(new AdminRole(new CaseInsensitiveString("validRole"))));
        PipelineConfigs newPipelineConfigs = new BasicPipelineConfigs("group", authorization);

        CreatePipelineConfigsCommand command = new CreatePipelineConfigsCommand(newPipelineConfigs, user, new HttpLocalizedOperationResult(), securityService);
        command.isValid(cruiseConfig);
        assertThat(authorization.getAllErrors(), is(Collections.emptyList()));
        assertThat(newPipelineConfigs.errors().getAll(), is(Collections.emptyList()));
    }

    @Test
    public void commandShouldBeValid_withoutAuthorization() {
        PipelineConfigs newPipelineConfigs = new BasicPipelineConfigs("group", null);

        CreatePipelineConfigsCommand command = new CreatePipelineConfigsCommand(newPipelineConfigs, user, new HttpLocalizedOperationResult(), securityService);
        command.isValid(cruiseConfig);
        assertThat(newPipelineConfigs.errors().getAll(), is(Collections.emptyList()));
    }

    @Test
    public void commandShouldNotBeValid_whenRoleIsInvalid() {
        Authorization authorization = new Authorization(new AdminsConfig(new AdminRole(new CaseInsensitiveString("invalidRole"))));
        PipelineConfigs newPipelineConfigs = new BasicPipelineConfigs("group", authorization);

        CreatePipelineConfigsCommand command = new CreatePipelineConfigsCommand(newPipelineConfigs, user, new HttpLocalizedOperationResult(), securityService);
        assertFalse(command.isValid(cruiseConfig));

        assertThat(authorization.getAllErrors().get(0).getAllOn("roles"), is(Arrays.asList("Role \"invalidRole\" does not exist.")));
    }

    @Test
    public void commandShouldNotBeValid_whenGroupNameInvalid() {
        PipelineConfigs newPipelineConfigs = new BasicPipelineConfigs("@group", null);

        CreatePipelineConfigsCommand command = new CreatePipelineConfigsCommand(newPipelineConfigs, user, new HttpLocalizedOperationResult(), securityService);
        assertFalse(command.isValid(cruiseConfig));

        assertThat(newPipelineConfigs.errors().on("group"), is("Invalid group name '@group'. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void commandShouldThrowExceptionAndReturnUnprocessableEntityResult_whenValidatingWithNullGroupName() {
        Authorization authorization = new Authorization(new AdminsConfig(new AdminRole(new CaseInsensitiveString("validRole"))));
        PipelineConfigs newPipelineConfigs = new BasicPipelineConfigs(null, authorization);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        CreatePipelineConfigsCommand command = new CreatePipelineConfigsCommand(newPipelineConfigs, user, result, securityService);

        assertThatThrownBy(() -> command.isValid(cruiseConfig))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Group name cannot be null.");

        assertThat(result.httpCode(), is(HttpStatus.SC_UNPROCESSABLE_ENTITY));
        assertThat(result.message(), is("The group is invalid. Attribute 'name' cannot be null."));
    }

    @Test
    public void commandShouldThrowIllegalArgumentException_whenValidatingWithBlankGroupName() {
        Authorization authorization = new Authorization(new AdminsConfig(new AdminRole(new CaseInsensitiveString("validRole"))));
        PipelineConfigs newPipelineConfigs = new BasicPipelineConfigs("  ", authorization);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        CreatePipelineConfigsCommand command = new CreatePipelineConfigsCommand(newPipelineConfigs, user, result, securityService);

        assertThatThrownBy(() -> command.isValid(cruiseConfig))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Group name cannot be null.");
    }

    @Test
    public void commandShouldContinue_whenUserIsAuthorized() {
        when(securityService.isUserAdmin(user)).thenReturn(true);

        Authorization authorization = new Authorization(new AdminsConfig(new AdminRole(new CaseInsensitiveString("validRole"))));
        PipelineConfigs newPipelineConfigs = new BasicPipelineConfigs("group", authorization);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        CreatePipelineConfigsCommand command = new CreatePipelineConfigsCommand(newPipelineConfigs, user, result, securityService);

        assertTrue(command.canContinue(cruiseConfig));
    }

    @Test
    public void commandShouldNotContinue_whenUserUnauthorized() {
        when(securityService.isUserAdmin(user)).thenReturn(false);

        Authorization authorization = new Authorization(new AdminsConfig(new AdminRole(new CaseInsensitiveString("validRole"))));
        PipelineConfigs newPipelineConfigs = new BasicPipelineConfigs("group", authorization);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        CreatePipelineConfigsCommand command = new CreatePipelineConfigsCommand(newPipelineConfigs, user, result, securityService);

        assertFalse(command.canContinue(cruiseConfig));
        assertThat(result.httpCode(), is(HttpStatus.SC_FORBIDDEN));
    }
}
