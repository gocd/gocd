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

import com.thoughtworks.go.config.*;
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
import org.mockito.Matchers;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RoleConfigCommandTest {
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
        PluginRoleConfig role = new PluginRoleConfig("blackbird", "ldap");
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        RoleConfigCommand command = new StubCommand(goConfigService, role, extension, currentUser, result);
        assertThat(cruiseConfig.server().security().securityAuthConfigs().find("foo"), nullValue());

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.toString(), containsString("UNAUTHORIZED_TO_EDIT"));
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsAuthorized() throws Exception {
        PluginRoleConfig role = new PluginRoleConfig("blackbird", "ldap");
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        RoleConfigCommand command = new StubCommand(goConfigService, role, extension, currentUser, result);
        assertThat(cruiseConfig.server().security().securityAuthConfigs().find("ldap"), nullValue());

        assertThat(command.canContinue(cruiseConfig), is(true));
        assertThat(result.httpCode(), is(200));
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfUserIsGroupAdmin() throws Exception {
        PluginRoleConfig role = new PluginRoleConfig("blackbird", "ldap");
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        when(goConfigService.isGroupAdministrator(currentUser)).thenReturn(true);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        RoleConfigCommand command = new StubCommand(goConfigService, role, extension, currentUser, result);
        assertThat(cruiseConfig.server().security().securityAuthConfigs().find("foo"), nullValue());

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.toString(), containsString("UNAUTHORIZED_TO_EDIT"));
    }

    @Test
    public void isValid_shouldValidateTheUpdatedRoleConfig() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        PluginRoleConfig pluginRoleConfig = new PluginRoleConfig(null, "ldap");
        cruiseConfig.server().security().addRole(pluginRoleConfig);

        RoleConfigCommand command = new StubCommand(goConfigService, pluginRoleConfig, extension, currentUser, result);

        assertFalse(command.isValid(cruiseConfig));
        assertThat(pluginRoleConfig.errors().size(), is(2));
        assertThat(pluginRoleConfig.errors().get("name").get(0), is("Invalid role name name 'null'. This must be " +
                "alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
        assertThat(pluginRoleConfig.errors().get("authConfigId").get(0), is("No such security auth configuration present for id: `ldap`"));
    }

    @Test
    public void isValid_shouldValidationRolesWithNonUniqueNamesAcrossPluginType() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        PluginRoleConfig pluginRoleConfig = new PluginRoleConfig("test", "ldap");
        cruiseConfig.server().security().addRole(pluginRoleConfig);
        cruiseConfig.server().security().addRole(new RoleConfig(new CaseInsensitiveString("test")));

        RoleConfigCommand command = new StubCommand(goConfigService, pluginRoleConfig, extension, currentUser, result);

        assertFalse(command.isValid(cruiseConfig));
        assertThat(pluginRoleConfig.errors().size(), is(2));
        assertThat(pluginRoleConfig.errors().get("name").get(0), is("Role names should be unique. Role with the same name exists."));
    }

    @Test
    public void shouldPassValidationForValidRole() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        PluginRoleConfig pluginRoleConfig = new PluginRoleConfig("foo", "ldap");
        cruiseConfig.server().security().addRole(pluginRoleConfig);
        cruiseConfig.server().security().securityAuthConfigs().add(new SecurityAuthConfig("ldap", "cd.go.ldap"));
        when(extension.validateRoleConfiguration(eq("cd.go.ldap"), Matchers.<Map<String, String>>any())).thenReturn(new ValidationResult());

        RoleConfigCommand command = new StubCommand(goConfigService, pluginRoleConfig, extension, currentUser, result);
        boolean isValid = command.isValid(cruiseConfig);
        assertTrue(isValid);
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsAdmin() throws Exception {
        PluginRoleConfig role = new PluginRoleConfig("blackbird", "ldap");
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        RoleConfigCommand command = new StubCommand(goConfigService, role, extension, currentUser, result);
        assertThat(cruiseConfig.server().security().securityAuthConfigs().find("ldap"), nullValue());

        assertThat(command.canContinue(cruiseConfig), is(true));
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsGroupAdmin() throws Exception {
        PluginRoleConfig role = new PluginRoleConfig("blackbird", "ldap");

        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        when(goConfigService.isGroupAdministrator(currentUser.getUsername())).thenReturn(true);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        RoleConfigCommand command = new StubCommand(goConfigService, role, extension, currentUser, result);
        assertThat(cruiseConfig.server().security().securityAuthConfigs().find("ldap"), nullValue());

        assertThat(command.canContinue(cruiseConfig), is(false));
    }

    private class StubCommand extends RoleConfigCommand {

        public StubCommand(GoConfigService goConfigService, Role role, AuthorizationExtension extension, Username currentUser, LocalizedOperationResult result) {
            super(goConfigService, role, currentUser, result);
        }

        @Override
        public void update(CruiseConfig preprocessedConfig) throws Exception {

        }
    }
}