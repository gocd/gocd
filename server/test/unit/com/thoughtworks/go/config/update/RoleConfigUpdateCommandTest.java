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

package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.RoleConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.RoleNotFoundException;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RoleConfigUpdateCommandTest {
    private Username currentUser;
    private GoConfigService goConfigService;
    private BasicCruiseConfig cruiseConfig;
    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Before
    public void setUp() throws Exception {
        currentUser = new Username("bob");
        goConfigService = mock(GoConfigService.class);
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
    }

    @Test
    public void shouldRaiseErrorWhenUpdatingNonExistentRole() throws Exception {
        cruiseConfig.server().security().getRoles().clear();
        RoleConfigCommand command = new RoleConfigUpdateCommand(null, new RoleConfig(new CaseInsensitiveString("foo")), null, null, new HttpLocalizedOperationResult(), null, null);
        thrown.expect(RoleNotFoundException.class);
        command.update(cruiseConfig);
    }

    @Test
    public void shouldUpdateExistingRole() throws Exception {
        PluginRoleConfig oldRole = new PluginRoleConfig("foo", "ldap");
        PluginRoleConfig updatedRole = new PluginRoleConfig("foo", "github");

        cruiseConfig.server().security().getRoles().add(oldRole);
        RoleConfigCommand command = new RoleConfigUpdateCommand(null, updatedRole, null, null, null, null, null);
        command.update(cruiseConfig);
        assertThat(cruiseConfig.server().security().getRoles().findByName(new CaseInsensitiveString("foo")), is(equalTo(updatedRole)));
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfRequestIsNotFresh() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);

        PluginRoleConfig oldRole = new PluginRoleConfig("foo", "ldap");
        PluginRoleConfig updatedRole = new PluginRoleConfig("foo", "github");

        cruiseConfig.server().security().getRoles().add(oldRole);

        EntityHashingService entityHashingService = mock(EntityHashingService.class);

        when(entityHashingService.md5ForEntity(oldRole)).thenReturn("md5");

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        RoleConfigCommand command = new RoleConfigUpdateCommand(goConfigService, updatedRole, null, currentUser, result, entityHashingService, "bad-md5");

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.toString(), containsString("STALE_RESOURCE_CONFIG"));
    }
}