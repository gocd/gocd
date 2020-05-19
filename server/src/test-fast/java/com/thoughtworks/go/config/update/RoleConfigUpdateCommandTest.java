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
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.RoleConfig;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RoleConfigUpdateCommandTest {
    private Username currentUser;
    private GoConfigService goConfigService;
    private BasicCruiseConfig cruiseConfig;
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private EntityHashingService entityHashingService;

    @Before
    public void setUp() throws Exception {
        currentUser = new Username("bob");
        goConfigService = mock(GoConfigService.class);
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
        entityHashingService = mock(EntityHashingService.class);
    }

    @Test
    public void shouldUpdateExistingRole() throws Exception {
        PluginRoleConfig oldRole = new PluginRoleConfig("foo", "ldap");
        PluginRoleConfig updatedRole = new PluginRoleConfig("foo", "github");

        cruiseConfig.server().security().getRoles().add(oldRole);
        RoleConfigCommand command = new RoleConfigUpdateCommand(null, updatedRole, null, null, null, null);
        command.update(cruiseConfig);
        assertThat(cruiseConfig.server().security().getRoles().findByName(new CaseInsensitiveString("foo")), is(equalTo(updatedRole)));
    }

    @Test
    public void currentUserShouldBeAnAdminToAddRole() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Username viewUser = new Username("view");

        when(goConfigService.isUserAdmin(viewUser)).thenReturn(false);

        RoleConfigUpdateCommand command = new RoleConfigUpdateCommand(goConfigService, new RoleConfig("some-role"), viewUser, result, mock(EntityHashingService.class), "digest");

        assertFalse(command.canContinue(null));
        assertFalse(result.isSuccessful());
        assertThat(result.httpCode(), is(403));
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfRequestIsNotFresh() {
        PluginRoleConfig oldRole = new PluginRoleConfig("foo", "ldap");
        PluginRoleConfig updatedRole = new PluginRoleConfig("foo", "github");

        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);
        cruiseConfig.server().security().getRoles().add(oldRole);
        when(entityHashingService.hashForEntity(oldRole)).thenReturn("digest");

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        RoleConfigCommand command = new RoleConfigUpdateCommand(goConfigService, updatedRole, currentUser, result, entityHashingService, "bad-digest");

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.message(), is(EntityType.Role.staleConfig(updatedRole.getName())));
    }

    @Test
    public void shouldNotContinueIfExistingRoleIsDeleted() throws Exception {
        PluginRoleConfig updatedRole = new PluginRoleConfig("foo", "github");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);

        RoleConfigCommand command = new RoleConfigUpdateCommand(goConfigService, updatedRole, currentUser, result, entityHashingService, "bad-digest");

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertFalse(result.isSuccessful());
        assertThat(result.httpCode(), is(404));
    }
}
