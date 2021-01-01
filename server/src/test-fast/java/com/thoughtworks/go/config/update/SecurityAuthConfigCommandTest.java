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
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatchers;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SecurityAuthConfigCommandTest {
    private Username currentUser;
    private GoConfigService goConfigService;
    private BasicCruiseConfig cruiseConfig;


    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private AuthorizationExtension extension;

    @Before
    public void setUp() throws Exception {
        currentUser = new Username("bob");
        goConfigService = mock(GoConfigService.class);
        extension = mock(AuthorizationExtension.class);
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfUserIsUnauthorized() throws Exception {
        SecurityAuthConfig securityAuthConfig = new SecurityAuthConfig("blackbird", "ldap");
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        SecurityAuthConfigCommand command = new SecurityAuthConfigCommandTest.StubCommand(goConfigService, securityAuthConfig, extension, currentUser, result);
        assertThat(cruiseConfig.server().security().securityAuthConfigs().find("foo"), nullValue());

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.message(), is(EntityType.SecurityAuthConfig.forbiddenToEdit(securityAuthConfig.getId(), currentUser.getUsername())));
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsAuthorized() throws Exception {
        SecurityAuthConfig securityAuthConfig = new SecurityAuthConfig("blackbird", "ldap");
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        SecurityAuthConfigCommand command = new SecurityAuthConfigCommandTest.StubCommand(goConfigService, securityAuthConfig, extension, currentUser, result);
        assertThat(cruiseConfig.server().security().securityAuthConfigs().find("ldap"), nullValue());

        assertThat(command.canContinue(cruiseConfig), is(true));
        assertThat(result.httpCode(), is(200));
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfUserIsGroupAdmin() throws Exception {
        SecurityAuthConfig securityAuthConfig = new SecurityAuthConfig("blackbird", "ldap");
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        when(goConfigService.isGroupAdministrator(currentUser)).thenReturn(true);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        SecurityAuthConfigCommand command = new SecurityAuthConfigCommandTest.StubCommand(goConfigService, securityAuthConfig, extension, currentUser, result);
        assertThat(cruiseConfig.server().security().securityAuthConfigs().find("foo"), nullValue());

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.message(), is(EntityType.SecurityAuthConfig.forbiddenToEdit(securityAuthConfig.getId(), currentUser.getUsername())));
    }

    @Test
    public void shouldValidateWithErrorIfNameIsNull() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        SecurityAuthConfig securityAuthConfig = new SecurityAuthConfig(null, "ldap");
        cruiseConfig.server().security().securityAuthConfigs().add(securityAuthConfig);

        SecurityAuthConfigCommand command = new SecurityAuthConfigCommandTest.StubCommand(goConfigService, securityAuthConfig, extension, currentUser, result);
        thrown.expectMessage(EntityType.SecurityAuthConfig.idCannotBeBlank());
        command.isValid(cruiseConfig);
    }

    @Test
    public void shouldPassValidationIfNameIsNotNull() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        SecurityAuthConfig securityAuthConfig = new SecurityAuthConfig("foo", "ldap");
        cruiseConfig.server().security().securityAuthConfigs().add(securityAuthConfig);
        when(extension.validateAuthConfig(eq("ldap"), ArgumentMatchers.anyMap())).thenReturn(new ValidationResult());

        SecurityAuthConfigCommand command = new SecurityAuthConfigCommandTest.StubCommand(goConfigService, securityAuthConfig, extension, currentUser, result);
        boolean isValid = command.isValid(cruiseConfig);
        assertTrue(isValid);
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsAdmin() throws Exception {
        SecurityAuthConfig securityAuthConfig = new SecurityAuthConfig("blackbird", "ldap");
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        SecurityAuthConfigCommand command = new SecurityAuthConfigCommandTest.StubCommand(goConfigService, securityAuthConfig, extension, currentUser, result);
        assertThat(cruiseConfig.server().security().securityAuthConfigs().find("ldap"), nullValue());

        assertThat(command.canContinue(cruiseConfig), is(true));
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsGroupAdmin() throws Exception {
        SecurityAuthConfig securityAuthConfig = new SecurityAuthConfig("blackbird", "ldap");

        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        when(goConfigService.isGroupAdministrator(currentUser.getUsername())).thenReturn(true);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        SecurityAuthConfigCommand command = new SecurityAuthConfigCommandTest.StubCommand(goConfigService, securityAuthConfig, extension, currentUser, result);
        assertThat(cruiseConfig.server().security().securityAuthConfigs().find("ldap"), nullValue());

        assertThat(command.canContinue(cruiseConfig), is(false));
    }

    private class StubCommand extends SecurityAuthConfigCommand {


        public StubCommand(GoConfigService goConfigService, SecurityAuthConfig newSecurityAuthConfig, AuthorizationExtension extension, Username currentUser, LocalizedOperationResult result) {
            super(goConfigService, newSecurityAuthConfig, extension, currentUser, result);
        }

        @Override
        public void update(CruiseConfig preprocessedConfig) throws Exception {

        }

        @Override
        public boolean isValid(CruiseConfig preprocessedConfig) {
            return isValidForCreateOrUpdate(preprocessedConfig);
        }
    }
}
