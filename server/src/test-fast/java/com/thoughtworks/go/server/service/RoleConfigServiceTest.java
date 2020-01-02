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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.update.*;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.plugins.validators.authorization.RoleConfigurationValidator;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class RoleConfigServiceTest {
    private GoConfigService configService;
    private AuthorizationExtension extension;
    private RoleConfigurationValidator configurationValidator;
    private EntityHashingService entityHashingService;
    private RoleConfigService roleConfigService;
    private BasicCruiseConfig cruiseConfig;

    @Before
    public void setUp() throws Exception {
        configService = mock(GoConfigService.class);
        extension = mock(AuthorizationExtension.class);
        configurationValidator = mock(RoleConfigurationValidator.class);
        entityHashingService = mock(EntityHashingService.class);
        cruiseConfig = GoConfigMother.defaultCruiseConfig();

        when(configService.cruiseConfig()).thenReturn(cruiseConfig);
        when(configService.getConfigForEditing()).thenReturn(cruiseConfig);
        roleConfigService = new RoleConfigService(configService, entityHashingService, extension, configurationValidator);
    }

    @Test
    public void create_shouldAddARoleToConfig() throws Exception {
        PluginRoleConfig role = new PluginRoleConfig();
        Username admin = new Username("admin");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        roleConfigService.create(admin, role, result);

        verify(configService).updateConfig(any(RoleConfigCreateCommand.class), eq(admin));
    }

    @Test
    public void create_shouldValidatePluginRoleMetadata() throws Exception {
        PluginRoleConfig role = new PluginRoleConfig("operate", "ldap");

        cruiseConfig.server().security().securityAuthConfigs().add(new SecurityAuthConfig("ldap", "plugin_id"));

        roleConfigService.create(null, role, null);

        verify(configurationValidator).validate(role, "plugin_id");
    }

    @Test
    public void create_shouldIgnorePluginRoleMetadataValidationInAbsenceOfPlugin() throws Exception {
        PluginRoleConfig role = new PluginRoleConfig("operate", "ldap");

        roleConfigService.create(null, role, null);

        verifyZeroInteractions(configurationValidator);
    }

    @Test
    public void create_shouldIgnoreValidationForGoCDRole() throws Exception {
        RoleConfig role = new RoleConfig(new CaseInsensitiveString("operate"));

        roleConfigService.create(null, role, null);

        verifyZeroInteractions(configurationValidator);
    }

    @Test
    public void update_shouldUpdateAnExistingPluginRole() throws Exception {
        PluginRoleConfig role = new PluginRoleConfig();
        Username admin = new Username("admin");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        roleConfigService.update(admin, "md5", role, result);

        verify(configService).updateConfig(any(RoleConfigUpdateCommand.class), eq(admin));
    }

    @Test
    public void update_shouldValidatePluginRoleMetadata() throws Exception {
        PluginRoleConfig role = new PluginRoleConfig("operate", "ldap");

        cruiseConfig.server().security().securityAuthConfigs().add(new SecurityAuthConfig("ldap", "plugin_id"));

        roleConfigService.update(null, "md5", role, null);

        verify(configurationValidator).validate(role, "plugin_id");
    }

    @Test
    public void update_shouldIgnorePluginRoleMetadataValidationInAbsenceOfPlugin() throws Exception {
        PluginRoleConfig role = new PluginRoleConfig("operate", "ldap");

        roleConfigService.update(null, "md5", role, null);

        verifyZeroInteractions(configurationValidator);
    }

    @Test
    public void update_shouldIgnoreValidationForGoCDRole() throws Exception {
        RoleConfig role = new RoleConfig(new CaseInsensitiveString("operate"));

        roleConfigService.update(null, "md5", role, null);

        verifyZeroInteractions(configurationValidator);
    }

    @Test
    public void bulkUpdate_shouldUpdateGoCDRoles() {
        Username currentUser = new Username("current_user");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<String> usersToAdd = Arrays.asList("user1", "user2");
        List<String> usersToRemove = Arrays.asList("user3", "user4");
        GoCDRolesBulkUpdateRequest request = new GoCDRolesBulkUpdateRequest(Collections.singletonList(
                new GoCDRolesBulkUpdateRequest.Operation("role1", usersToAdd, usersToRemove)));
        RolesConfigBulkUpdateCommand command = new RolesConfigBulkUpdateCommand(request, currentUser,
                configService, result);

        roleConfigService.bulkUpdate(request, currentUser, result);

        verify(configService).updateConfig(command, currentUser);
    }

    @Test
    public void bulkUpdate_shouldHandleInvalidConfigException() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<String> usersToAdd = Arrays.asList("user1", "user2");
        List<String> usersToRemove = Arrays.asList("user3", "user4");
        GoCDRolesBulkUpdateRequest request = new GoCDRolesBulkUpdateRequest(Collections.singletonList(
                new GoCDRolesBulkUpdateRequest.Operation("role1", usersToAdd, usersToRemove)));

        cruiseConfig.server().security().getRoles().addError("role1", "some error");
        doThrow(new GoConfigInvalidException(cruiseConfig, ""))
                .when(configService).updateConfig(any(), any());

        roleConfigService.bulkUpdate(request, null, result);

        assertThat(result.httpCode(), is(422));
        assertThat(result.message(), containsString("some error"));
    }

    @Test
    public void bulkUpdate_shouldHandleOtherExceptions() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        GoCDRolesBulkUpdateRequest request = new GoCDRolesBulkUpdateRequest(Collections.singletonList(
                new GoCDRolesBulkUpdateRequest.Operation("role1", Collections.emptyList(), Collections.emptyList())));

        doThrow(new RuntimeException()).when(configService).updateConfig(any(), any());

        roleConfigService.bulkUpdate(request, null, result);

        assertThat(result.httpCode(), is(500));
        assertThat(result.message(), containsString("An error occurred while saving the role config. Please check the logs for more information."));
    }

    @Test
    public void delete_shouldDeleteARole() throws Exception {
        PluginRoleConfig role = new PluginRoleConfig("operate", "ldap");
        Username admin = new Username("admin");

        roleConfigService.delete(admin, role, new HttpLocalizedOperationResult());

        verify(configService).updateConfig(any(RoleConfigDeleteCommand.class), eq(admin));
    }

    @Test
    public void getRolesForUser_shouldReturnAllTheRolesForTheGivenUser() {
        Username bob = new Username("Bob");
        Username john = new Username("John");

        RoleConfig role1 = new RoleConfig(new CaseInsensitiveString("role1"));
        role1.addUser(new RoleUser(bob.getUsername()));
        role1.addUser(new RoleUser(john.getUsername()));

        RoleConfig role2 = new RoleConfig(new CaseInsensitiveString("role2"));
        role2.addUser(new RoleUser(bob.getUsername()));

        RoleConfig role3 = new RoleConfig(new CaseInsensitiveString("role3"));
        role3.addUser(new RoleUser(john.getUsername()));

        cruiseConfig.server().security().addRole(role1);
        cruiseConfig.server().security().addRole(role2);
        cruiseConfig.server().security().addRole(role3);

        ServerConfig serverConfig = new ServerConfig();
        serverConfig.security().addRole(role1);
        serverConfig.security().addRole(role2);
        serverConfig.security().addRole(role3);

        when(configService.serverConfig()).thenReturn(serverConfig);

        HashMap<Username, RolesConfig> userToRolesMap = roleConfigService.getRolesForUser(Arrays.asList(bob, john));

        assertThat(userToRolesMap.size(), is(2));

        assertThat(userToRolesMap.get(bob), hasItem(role1));
        assertThat(userToRolesMap.get(bob), hasItem(role2));
        assertThat(userToRolesMap.get(bob), not(hasItem(role3)));

        assertThat(userToRolesMap.get(john), hasItem(role1));
        assertThat(userToRolesMap.get(john), hasItem(role3));
        assertThat(userToRolesMap.get(john), not(hasItem(role2)));
    }
}
