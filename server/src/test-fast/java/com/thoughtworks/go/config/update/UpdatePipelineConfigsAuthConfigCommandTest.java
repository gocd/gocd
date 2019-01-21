/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.exceptions.PipelineGroupNotFoundException;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class UpdatePipelineConfigsAuthConfigCommandTest {
    @Mock
    private EntityHashingService entityHashingService;

    @Mock
    private SecurityService securityService;

    private PipelineConfigs pipelineConfigs;
    private Authorization authorization;
    private Username user;
    private BasicCruiseConfig cruiseConfig;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        initMocks(this);
        authorization = new Authorization(new AdminsConfig(new AdminUser(new CaseInsensitiveString("user"))));
        pipelineConfigs = new BasicPipelineConfigs("group", authorization);
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.setGroup(new PipelineGroups(pipelineConfigs));
        cruiseConfig.server().security().addRole(new RoleConfig("validRole"));
        user = new Username(new CaseInsensitiveString("user"));
    }

    @Test
    public void shouldReplaceOnlyPipelineConfigsAuthorizationWhileUpdatingTheTemplate() throws Exception {
        Authorization newAuthorization = new Authorization(new AdminsConfig(new AdminRole(new CaseInsensitiveString("foo"))));

        UpdatePipelineConfigsAuthCommand command = new UpdatePipelineConfigsAuthCommand("group", newAuthorization, new HttpLocalizedOperationResult(), user, "md5", entityHashingService, securityService);
        command.update(cruiseConfig);

        assertThat(cruiseConfig.hasPipelineGroup(pipelineConfigs.getGroup()), is(true));
        Authorization expectedTemplateAuthorization = cruiseConfig.findGroup(pipelineConfigs.getGroup()).getAuthorization();
        assertNotEquals(expectedTemplateAuthorization, authorization);
        assertThat(expectedTemplateAuthorization, is(newAuthorization));
    }

    @Test
    public void commandShouldBeValid_whenRoleIsValid() {
        Authorization newAuthorization = new Authorization(new AdminsConfig(new AdminRole(new CaseInsensitiveString("validRole"))));

        UpdatePipelineConfigsAuthCommand command = new UpdatePipelineConfigsAuthCommand("group", newAuthorization, new HttpLocalizedOperationResult(), user, "md5", entityHashingService, securityService);
        command.isValid(cruiseConfig);
        assertThat(authorization.getAllErrors(), is(Collections.emptyList()));
    }

    @Test
    public void commandShouldCopyOverErrors_whenRoleIsInvalid() {
        Authorization newAuthorization = new Authorization(new AdminsConfig(new AdminRole(new CaseInsensitiveString("invalidRole"))));

        UpdatePipelineConfigsAuthCommand command = new UpdatePipelineConfigsAuthCommand("group", newAuthorization, new HttpLocalizedOperationResult(), user, "md5", entityHashingService, securityService);
        assertFalse(command.isValid(cruiseConfig));

        assertThat(newAuthorization.getAllErrors().get(0).getAllOn("roles"), is(Arrays.asList("Role \"invalidRole\" does not exist.")));
    }

    @Test
    public void commandShouldThrowExceptionAndReturnUnprocessableEntityResult_whenValidatingWithNullGroupName() {
        Authorization newAuthorization = new Authorization(new AdminsConfig(new AdminRole(new CaseInsensitiveString("validRole"))));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        UpdatePipelineConfigsAuthCommand command = new UpdatePipelineConfigsAuthCommand(null, newAuthorization, result, user, "md5", entityHashingService, securityService);

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Group name cannot be null.");
        command.isValid(cruiseConfig);

        assertThat(result.httpCode(), is(HttpStatus.SC_UNPROCESSABLE_ENTITY));
        assertThat(result.message(), is("The group is invalid. Attribute 'name' cannot be null."));
    }

    @Test
    public void commandShouldThrowIllegalArgumentException_whenValidatingWithBlankGroupName() {
        Authorization newAuthorization = new Authorization(new AdminsConfig(new AdminRole(new CaseInsensitiveString("validRole"))));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        UpdatePipelineConfigsAuthCommand command = new UpdatePipelineConfigsAuthCommand("  ", newAuthorization, result, user, "md5", entityHashingService, securityService);

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Group name cannot be null.");
        command.isValid(cruiseConfig);
    }

    @Test
    public void commandShouldThrowPipelineNotFoundException_whenValidatingWithNonExistentGroup() {
        Authorization newAuthorization = new Authorization(new AdminsConfig(new AdminRole(new CaseInsensitiveString("validRole"))));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        UpdatePipelineConfigsAuthCommand command = new UpdatePipelineConfigsAuthCommand("newGroup", newAuthorization, result, user, "md5", entityHashingService, securityService);

        thrown.expect(PipelineGroupNotFoundException.class);
        command.isValid(cruiseConfig);
    }

    @Test
    public void commandShouldContinue_whenRequestIsFreshAndUserIsAuthorized() {
        when(securityService.isUserAdminOfGroup(user, "group")).thenReturn(true);
        when(entityHashingService.md5ForEntity(pipelineConfigs)).thenReturn("md5");

        Authorization newAuthorization = new Authorization(new AdminsConfig(new AdminRole(new CaseInsensitiveString("validRole"))));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        UpdatePipelineConfigsAuthCommand command = new UpdatePipelineConfigsAuthCommand("group", newAuthorization, result, user, "md5", entityHashingService, securityService);

        assertTrue(command.canContinue(cruiseConfig));
    }

    @Test
    public void commandShouldNotContinue_whenRequestIsNotFresh() {
        when(securityService.isUserAdminOfGroup(user, "group")).thenReturn(true);
        when(entityHashingService.md5ForEntity(pipelineConfigs)).thenReturn("md5-old");

        Authorization newAuthorization = new Authorization(new AdminsConfig(new AdminRole(new CaseInsensitiveString("validRole"))));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        UpdatePipelineConfigsAuthCommand command = new UpdatePipelineConfigsAuthCommand("group", newAuthorization, result, user, "md5", entityHashingService, securityService);

        assertFalse(command.canContinue(cruiseConfig));
        assertThat(result.httpCode(), is(HttpStatus.SC_PRECONDITION_FAILED));
    }

    @Test
    public void commandShouldNotContinue_whenUserUnauthorized() {
        when(securityService.isUserAdminOfGroup(user, "group")).thenReturn(false);
        when(entityHashingService.md5ForEntity(pipelineConfigs)).thenReturn("md5");

        Authorization newAuthorization = new Authorization(new AdminsConfig(new AdminRole(new CaseInsensitiveString("validRole"))));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        UpdatePipelineConfigsAuthCommand command = new UpdatePipelineConfigsAuthCommand("group", newAuthorization, result, user, "md5", entityHashingService, securityService);

        assertFalse(command.isUserAuthorized());
        assertThat(result.httpCode(), is(HttpStatus.SC_FORBIDDEN));
    }
}
