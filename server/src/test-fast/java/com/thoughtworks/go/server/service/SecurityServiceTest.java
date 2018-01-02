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
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.helper.PipelineTemplateConfigMother.createTemplate;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class SecurityServiceTest {
    private GoConfigService goConfigService;
    private SecurityService securityService;

    @Before
    public void setUp() {
        goConfigService = mock(GoConfigService.class);
        when(goConfigService.security()).thenReturn(new SecurityConfig());
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
    public void shouldBeAbleToViewAdminPageIfUserCanViewTemplates() {
        CaseInsensitiveString username = new CaseInsensitiveString("user");
        final Username user = new Username(username);
        CruiseConfig config = new BasicCruiseConfig();
        config.addTemplate(createTemplate("s", new Authorization(new ViewConfig(new AdminUser(username)))));
        when(goConfigService.cruiseConfig()).thenReturn(config);

        when(goConfigService.isGroupAdministrator(user.getUsername())).thenReturn(false);
        when(goConfigService.isUserAdmin(user)).thenReturn(false);

        assertThat(securityService.canViewAdminPage(user), is(true));
    }

    @Test
    public void shouldReturnFalseForViewingAdminPageForARegularUser() {
        final Username user = new Username(new CaseInsensitiveString("user"));
        CruiseConfig config = new BasicCruiseConfig();
        when(goConfigService.cruiseConfig()).thenReturn(config);
        when(goConfigService.isUserAdmin(user)).thenReturn(false);
        when(goConfigService.isGroupAdministrator(user.getUsername())).thenReturn(false);

        SecurityService spy = spy(securityService);
        doReturn(false).when(spy).isAuthorizedToViewAndEditTemplates(user);
        doReturn(false).when(spy).isAuthorizedToViewTemplates(user);

        assertThat(spy.canViewAdminPage(user), is(false));
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
        doReturn(false).when(spy).isAuthorizedToViewTemplates(user);
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

    @Test
    public void shouldSayThatAUserIsAuthorizedToEditTemplateWhenTheUserIsAnAdminOfThisTemplate() throws Exception {
        CruiseConfig config = new BasicCruiseConfig();
        CaseInsensitiveString templateName = new CaseInsensitiveString("template");
        CaseInsensitiveString templateAdminName = new CaseInsensitiveString("templateAdmin");

        GoConfigMother.enableSecurityWithPasswordFilePlugin(config);
        GoConfigMother.addUserAsSuperAdmin(config, "theSuperAdmin");
        config.addTemplate(createTemplate("template", new Authorization(new AdminsConfig(new AdminUser(templateAdminName)))));

        when(goConfigService.cruiseConfig()).thenReturn(config);
        when(goConfigService.isUserAdmin(new Username(templateAdminName))).thenReturn(false);

        assertThat(securityService.isAuthorizedToEditTemplate(templateName, new Username(templateAdminName)), is(true));
        assertThat(securityService.isAuthorizedToEditTemplate(templateName, new Username(new CaseInsensitiveString("someOtherUserWhoIsNotAnAdmin"))), is(false));
    }

    @Test
    public void shouldSayThatAUserIsAuthorizedToEditTemplateWhenTheUserIsASuperAdmin() throws Exception {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        String adminName = "theSuperAdmin";
        CaseInsensitiveString templateName = new CaseInsensitiveString("template");

        GoConfigMother.enableSecurityWithPasswordFilePlugin(cruiseConfig);
        GoConfigMother.addUserAsSuperAdmin(cruiseConfig, adminName).addTemplate(createTemplate("template"));

        when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
        when(goConfigService.isUserAdmin(new Username(new CaseInsensitiveString(adminName)))).thenReturn(true);

        assertThat(securityService.isAuthorizedToEditTemplate(templateName, new Username(new CaseInsensitiveString(adminName))), is(true));
    }

    @Test
    public void shouldSayThatAUserIsAuthorizedToViewAndEditTemplatesWhenTheUserHasPermissionsForAtLeastOneTemplate() throws Exception {
        CruiseConfig config = new BasicCruiseConfig();
        String theSuperAdmin = "theSuperAdmin";
        String templateName = "template";
        String secondTemplateName = "secondTemplate";
        CaseInsensitiveString templateAdminName = new CaseInsensitiveString("templateAdmin");
        CaseInsensitiveString secondTemplateAdminName = new CaseInsensitiveString("secondTemplateAdmin");

        GoConfigMother.enableSecurityWithPasswordFilePlugin(config);
        GoConfigMother.addUserAsSuperAdmin(config, theSuperAdmin);
        config.addTemplate(createTemplate(templateName, new Authorization(new AdminsConfig(new AdminUser(templateAdminName)))));
        config.addTemplate(createTemplate(secondTemplateName, new Authorization(new AdminsConfig(new AdminUser(secondTemplateAdminName)))));

        when(goConfigService.cruiseConfig()).thenReturn(config);
        when(goConfigService.isUserAdmin(new Username(templateAdminName))).thenReturn(false);
        when(goConfigService.isUserAdmin(new Username(secondTemplateName))).thenReturn(false);
        when(goConfigService.isUserAdmin(new Username(new CaseInsensitiveString(theSuperAdmin)))).thenReturn(true);
        when(goConfigService.isUserAdmin(new Username(new CaseInsensitiveString("someOtherUserWhoIsNotAdminOfAnyTemplates")))).thenReturn(false);

        assertThat(securityService.isAuthorizedToViewAndEditTemplates(new Username(templateAdminName)), is(true));
        assertThat(securityService.isAuthorizedToViewAndEditTemplates(new Username(secondTemplateAdminName)), is(true));
        assertThat(securityService.isAuthorizedToViewAndEditTemplates(new Username(new CaseInsensitiveString(theSuperAdmin))), is(true));
        assertThat(securityService.isAuthorizedToViewAndEditTemplates(new Username(new CaseInsensitiveString("someOtherUserWhoIsNotAdminOfAnyTemplates"))), is(false));
    }

    @Test
    public void shouldReturnTrueForSuperAdminToViewTemplateConfiguration() {
        BasicCruiseConfig cruiseConfig = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString templateName = new CaseInsensitiveString("template");
        Username admin = new Username(new CaseInsensitiveString("admin"));

        GoConfigMother.addUserAsSuperAdmin(cruiseConfig, "admin").addTemplate(createTemplate("template"));

        when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
        when(goConfigService.isUserAdmin(admin)).thenReturn(true);

        assertThat(securityService.isAuthorizedToViewTemplate(templateName, admin), is(true));
    }

    @Test
    public void shouldReturnTrueForTemplateAdminsToViewTemplateConfiguration() {
        CruiseConfig config = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString templateAdmin = new CaseInsensitiveString("templateAdmin");
        CaseInsensitiveString templateName = new CaseInsensitiveString("template");

        config.addTemplate(createTemplate("template", new Authorization(new AdminsConfig(new AdminUser(templateAdmin)))));

        when(goConfigService.cruiseConfig()).thenReturn(config);
        when(goConfigService.isUserAdmin(new Username(templateAdmin))).thenReturn(false);

        assertThat(securityService.isAuthorizedToViewTemplate(templateName, new Username(templateAdmin)), is(true));
    }

    @Test
    public void shouldReturnTrueForTemplateViewUsersToViewTemplateConfiguration() {
        CruiseConfig config = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString templateViewUser = new CaseInsensitiveString("templateView");
        CaseInsensitiveString templateName = new CaseInsensitiveString("template");

        config.addTemplate(createTemplate("template", new Authorization(new ViewConfig(new AdminUser(templateViewUser)))));

        when(goConfigService.cruiseConfig()).thenReturn(config);
        when(goConfigService.isUserAdmin(new Username(templateViewUser))).thenReturn(false);

        assertThat(securityService.isAuthorizedToViewTemplate(templateName, new Username(templateViewUser)), is(true));
    }

    @Test
    public void shouldReturnTrueForGroupAdminsToViewTemplateConfigurationByDefault() {
        CruiseConfig config = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString groupAdmin = new CaseInsensitiveString("groupAdmin");
        setUpGroupWithAuthorization(config, new Authorization(new AdminsConfig(new AdminUser(groupAdmin))));
        CaseInsensitiveString templateName = new CaseInsensitiveString("template");

        config.addTemplate(createTemplate("template"));

        when(goConfigService.cruiseConfig()).thenReturn(config);
        when(goConfigService.isUserAdmin(new Username(groupAdmin))).thenReturn(false);
        when(goConfigService.isGroupAdministrator(groupAdmin)).thenReturn(true);

        assertThat(securityService.isAuthorizedToViewTemplate(templateName, new Username(groupAdmin)), is(true));
    }

    @Test
    public void shouldReturnFalseForGroupAdminsToViewTemplateConfigurationIfDisallowed() {
        CruiseConfig config = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString groupAdmin = new CaseInsensitiveString("groupAdmin");

        setUpGroupWithAuthorization(config, new Authorization(new AdminsConfig(new AdminUser(groupAdmin))));

        CaseInsensitiveString templateName = new CaseInsensitiveString("template");

        PipelineTemplateConfig template = createTemplate("template");
        template.getAuthorization().setAllowGroupAdmins(false);
        config.addTemplate(template);

        when(goConfigService.cruiseConfig()).thenReturn(config);
        when(goConfigService.isUserAdmin(new Username(groupAdmin))).thenReturn(false);
        when(goConfigService.isGroupAdministrator(groupAdmin)).thenReturn(true);

        assertThat(securityService.isAuthorizedToViewTemplate(templateName, new Username(groupAdmin)), is(false));
    }

    @Test
    public void shouldReturnTrueForGroupAdminsWithinARoleToViewTemplate() {
        CaseInsensitiveString groupAdmin = new CaseInsensitiveString("groupAdmin");
        BasicCruiseConfig config = new BasicCruiseConfig();
        ServerConfig serverConfig = new ServerConfig(new SecurityConfig(new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")))), null);
        RoleConfig role = new RoleConfig(new CaseInsensitiveString("role1"), new RoleUser(groupAdmin));
        serverConfig.security().addRole(role);
        config.setServerConfig(serverConfig);
        GoConfigMother.enableSecurityWithPasswordFilePlugin(config);

        setUpGroupWithAuthorization(config, new Authorization(new AdminsConfig(new AdminRole(role))));

        CaseInsensitiveString templateName = new CaseInsensitiveString("template");

        config.addTemplate(createTemplate("template"));

        when(goConfigService.cruiseConfig()).thenReturn(config);
        when(goConfigService.isUserAdmin(new Username(groupAdmin))).thenReturn(false);
        when(goConfigService.isGroupAdministrator(groupAdmin)).thenReturn(true);

        assertThat(securityService.isAuthorizedToViewTemplate(templateName, new Username(groupAdmin)), is(true));
    }

    @Test
    public void shouldReturnFalseForGroupAdminsWithinARoleToVIewTemplateIfDisallowed() {
        CaseInsensitiveString groupAdmin = new CaseInsensitiveString("groupAdmin");
        BasicCruiseConfig config = new BasicCruiseConfig();
        ServerConfig serverConfig = new ServerConfig(new SecurityConfig(new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")))), null);
        RoleConfig role = new RoleConfig(new CaseInsensitiveString("role1"), new RoleUser(groupAdmin));
        serverConfig.security().addRole(role);
        config.setServerConfig(serverConfig);
        GoConfigMother.enableSecurityWithPasswordFilePlugin(config);

        setUpGroupWithAuthorization(config, new Authorization(new AdminsConfig(new AdminRole(role))));

        CaseInsensitiveString templateName = new CaseInsensitiveString("template");

        PipelineTemplateConfig template = createTemplate("template");
        template.getAuthorization().setAllowGroupAdmins(false);
        config.addTemplate(template);

        when(goConfigService.cruiseConfig()).thenReturn(config);
        when(goConfigService.isUserAdmin(new Username(groupAdmin))).thenReturn(false);
        when(goConfigService.isGroupAdministrator(groupAdmin)).thenReturn(true);

        assertThat(securityService.isAuthorizedToViewTemplate(templateName, new Username(groupAdmin)), is(false));
    }

    @Test
    public void shouldSayUserIsAuthorizedToViewTemplatesWhenTheUserHasViewPermissionsToAtLeastOneTemplate() {
        CruiseConfig config = new BasicCruiseConfig();
        String theSuperAdmin = "theSuperAdmin";
        String templateName = "template";
        String secondTemplateName = "secondTemplate";
        CaseInsensitiveString templateAdminName = new CaseInsensitiveString("templateAdmin");
        CaseInsensitiveString templateViewUser = new CaseInsensitiveString("templateViewUser");

        GoConfigMother.enableSecurityWithPasswordFilePlugin(config);
        GoConfigMother.addUserAsSuperAdmin(config, theSuperAdmin);
        config.addTemplate(createTemplate(templateName, new Authorization(new AdminsConfig(new AdminUser(templateAdminName)))));
        config.addTemplate(createTemplate(secondTemplateName, new Authorization(new ViewConfig(new AdminUser(templateViewUser)))));

        when(goConfigService.cruiseConfig()).thenReturn(config);
        when(goConfigService.isUserAdmin(new Username(templateAdminName))).thenReturn(false);
        when(goConfigService.isUserAdmin(new Username(templateViewUser))).thenReturn(false);
        when(goConfigService.isUserAdmin(new Username(new CaseInsensitiveString(theSuperAdmin)))).thenReturn(true);
        when(goConfigService.isUserAdmin(new Username(new CaseInsensitiveString("regularUser")))).thenReturn(false);

        assertThat(securityService.isAuthorizedToViewTemplates(new Username(templateAdminName)), is(true));
        assertThat(securityService.isAuthorizedToViewTemplates(new Username(templateViewUser)), is(true));
        assertThat(securityService.isAuthorizedToViewTemplates(new Username(new CaseInsensitiveString(theSuperAdmin))), is(true));
        assertThat(securityService.isAuthorizedToViewTemplates(new Username(new CaseInsensitiveString("regularUser"))), is(false));
    }


    private BasicCruiseConfig getCruiseConfigWithSecurityEnabled() {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        ServerConfig serverConfig = new ServerConfig(new SecurityConfig(new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")))), null);
        cruiseConfig.setServerConfig(serverConfig);
        GoConfigMother.enableSecurityWithPasswordFilePlugin(cruiseConfig);
        return cruiseConfig;
    }

    private void setUpGroupWithAuthorization(CruiseConfig config, Authorization authorization) {
        new GoConfigMother().addPipelineWithGroup(config, "group", "pipeline", "stage", "job");
        config.getGroups().findGroup("group").setAuthorization(authorization);
    }
}
