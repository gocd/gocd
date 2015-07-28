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
import com.thoughtworks.go.server.domain.Username;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SecurityServiceTest {
    private GoConfigService goConfigService;
    private SecurityService securityService;

    @Before
    public void setUp() {
        goConfigService = mock(GoConfigService.class);
        securityService = new SecurityService(goConfigService);
    }

    @Test
    public void shouldReturnTrueIfUserIsOnlyATemplateAdmin() {
        final Username user = new Username(new CaseInsensitiveString("user"));
        when(goConfigService.isGroupAdministrator(user.getUsername())).thenReturn(false);
        when(goConfigService.isUserAdmin(user)).thenReturn(false);
        SecurityService spy = spy(securityService);
        doReturn(true).when(spy).isAuthorizedToViewAndEditTemplates(user);
        assertThat(spy.canViewAdminPage(new Username(new CaseInsensitiveString("user"))), is(true));
    }

    @Test
    public void shouldBeAbleToTellIfAUserIsAnAdmin() {
        Username username = new Username(new CaseInsensitiveString("user"));
        when(goConfigService.isUserAdmin(username)).thenReturn(Boolean.TRUE);
        assertThat(securityService.canViewAdminPage(username), is(true));
        verify(goConfigService).isUserAdmin(username);
    }

    @Test
    public void shouldBeAbleToTellIfAnUserCanViewTheAdminPage() {
        final Username user = new Username(new CaseInsensitiveString("user"));
        when(goConfigService.isGroupAdministrator(user.getUsername())).thenReturn(Boolean.TRUE);
        assertThat(securityService.canViewAdminPage(new Username(new CaseInsensitiveString("user"))), is(true));
    }

    @Test
    public void shouldBeAbleToTellIfAnUserISNotAllowedToViewTheAdminPage() {
        final Username user = new Username(new CaseInsensitiveString("user"));
        when(goConfigService.isGroupAdministrator(user.getUsername())).thenReturn(Boolean.FALSE);
        SecurityService spy = spy(securityService);
        doReturn(false).when(spy).isAuthorizedToViewAndEditTemplates(user);
        assertThat(spy.canViewAdminPage(new Username(new CaseInsensitiveString("user"))), is(false));
    }

    @Test
    public void shouldBeAbleToCreatePipelineIfUserIsSuperOrPipelineGroupAdmin() {
        final Username groupAdmin = new Username(new CaseInsensitiveString("groupAdmin"));
        when(goConfigService.isGroupAdministrator(groupAdmin.getUsername())).thenReturn(Boolean.TRUE);
        final Username admin = new Username(new CaseInsensitiveString("admin"));
        when(goConfigService.isGroupAdministrator(admin.getUsername())).thenReturn(Boolean.TRUE);
        assertThat(securityService.canCreatePipelines(new Username(new CaseInsensitiveString("groupAdmin"))), is(true));
        assertThat(securityService.canCreatePipelines(new Username(new CaseInsensitiveString("admin"))), is(true));
    }

    @Test
    public void shouldNotBeAbleToCreatePipelineIfUserIsTemplateAdmin() {
        final Username user = new Username(new CaseInsensitiveString("user"));
        when(goConfigService.isGroupAdministrator(user.getUsername())).thenReturn(false);
        when(goConfigService.isUserAdmin(user)).thenReturn(false);
        SecurityService spy = spy(securityService);
        doReturn(true).when(spy).isAuthorizedToViewAndEditTemplates(user);
        assertThat(spy.canCreatePipelines(new Username(new CaseInsensitiveString("user"))), is(false));
    }
}
