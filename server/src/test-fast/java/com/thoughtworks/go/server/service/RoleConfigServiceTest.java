/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.update.RoleConfigCreateCommand;
import com.thoughtworks.go.config.update.RoleConfigDeleteCommand;
import com.thoughtworks.go.config.update.RoleConfigUpdateCommand;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.plugins.validators.authorization.RoleConfigurationValidator;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Test;

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

        stub(configService.cruiseConfig()).toReturn(cruiseConfig);
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
    public void delete_shouldDeleteARole() throws Exception {
        PluginRoleConfig role = new PluginRoleConfig("operate", "ldap");
        Username admin = new Username("admin");

        roleConfigService.delete(admin, role, new HttpLocalizedOperationResult());

        verify(configService).updateConfig(any(RoleConfigDeleteCommand.class), eq(admin));
    }
}