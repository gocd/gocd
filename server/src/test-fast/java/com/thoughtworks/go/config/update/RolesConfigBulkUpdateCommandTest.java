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
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.RoleConfig;
import com.thoughtworks.go.config.RoleUser;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RolesConfigBulkUpdateCommandTest {
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
    public void shouldUpdateExistingGoCDRoles() throws Exception {
        RoleConfig role1 = new RoleConfig("foo", new RoleUser("user1"));
        List<String> userToAdd = Collections.singletonList("user2");

        RoleConfig role2 = new RoleConfig("bar", new RoleUser("user1"));
        List<String> userToRemove = Collections.singletonList("user1");

        cruiseConfig.server().security().getRoles().add(role1);
        cruiseConfig.server().security().getRoles().add(role2);

        GoCDRolesBulkUpdateRequest request = new GoCDRolesBulkUpdateRequest(Arrays.asList(
                new GoCDRolesBulkUpdateRequest.Operation("foo", userToAdd, Collections.emptyList()),
                new GoCDRolesBulkUpdateRequest.Operation("bar", Collections.emptyList(), userToRemove)));

        RolesConfigBulkUpdateCommand command = new RolesConfigBulkUpdateCommand(request, null, goConfigService, null);

        command.update(cruiseConfig);

        RoleConfig expectedRole1 = new RoleConfig("foo", new RoleUser("user1"), new RoleUser("user2"));
        RoleConfig expectedRole2 = new RoleConfig("bar");
        assertThat(cruiseConfig.server().security().getRoles().findByName(new CaseInsensitiveString("foo")), is(equalTo(expectedRole1)));
        assertThat(cruiseConfig.server().security().getRoles().findByName(new CaseInsensitiveString("bar")), is(equalTo(expectedRole2)));
    }

    @Test
    public void currentUserShouldBeAnAdminToAddRole() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Username viewUser = mock(Username.class);

        when(goConfigService.isUserAdmin(viewUser)).thenReturn(false);

        GoCDRolesBulkUpdateRequest request = new GoCDRolesBulkUpdateRequest(Collections.emptyList());
        RolesConfigBulkUpdateCommand command = new RolesConfigBulkUpdateCommand(request, viewUser, goConfigService, result);

        assertFalse(command.canContinue(null));
        assertFalse(result.isSuccessful());
        assertThat(result.httpCode(), is(403));
    }

    @Test
    public void shouldNotContinueIfExistingRoleIsDeleted() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);

        GoCDRolesBulkUpdateRequest request = new GoCDRolesBulkUpdateRequest(Collections.singletonList(
                new GoCDRolesBulkUpdateRequest.Operation("role_that_doesnt_exist", Collections.emptyList(), Collections.emptyList())));
        RolesConfigBulkUpdateCommand command = new RolesConfigBulkUpdateCommand(request, currentUser, goConfigService, result);

        assertThatThrownBy(() -> command.update(cruiseConfig))
            .isInstanceOf(RecordNotFoundException.class)
        ;
    }
}
