/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.Role;
import com.thoughtworks.go.config.RoleUser;
import com.thoughtworks.go.helper.GoConfigMother;
import org.junit.Test;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RoleServiceTest {

    @Test
    public void shouldReturnNumberOfUsersInARole() {
        GoConfigService goConfigService = mock(GoConfigService.class);
        CruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("some_pipeline");
        RoleUser roleUser1 = new RoleUser(new CaseInsensitiveString("go_user_1"));
        RoleUser roleUser2 = new RoleUser(new CaseInsensitiveString("go_user_2"));
        cruiseConfig.server().security().addRole(new Role(new CaseInsensitiveString("go_role"), roleUser1, roleUser2));
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);

        RoleService service = new RoleService(goConfigService);

        assertThat(service.usersInRole("go_role").size(), is(2));

        assertThat(service.usersInRole("go_role"), hasItems(roleUser1, roleUser2));
    }
}
