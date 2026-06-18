/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.config.policy.*;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.thoughtworks.go.config.CaseInsensitiveString.cis;
import static com.thoughtworks.go.helper.PipelineTemplateConfigMother.createTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class SecurityServiceTest {
    private GoConfigService goConfigService;
    private SecurityService securityService;

    @BeforeEach
    public void setUp() {
        goConfigService = mock(GoConfigService.class);
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        when(goConfigService.security()).thenReturn(new SecurityConfig());
        securityService = new SecurityService(goConfigService);
    }

    @Test
    public void shouldReturnTrueIfUserIsOnlyATemplateAdmin() {
        final Username user = new Username(cis("user"));
        when(goConfigService.isGroupAdministrator(user.getUsername())).thenReturn(false);
        when(goConfigService.isUserAdmin(user)).thenReturn(false);
        SecurityService spy = spy(securityService);
        doReturn(true).when(spy).isAuthorizedToEditTemplates(user);
        assertThat(spy.canViewSomeAdminPage(new Username(cis("user")))).isTrue();
    }

    @Test
    public void shouldBeAbleToViewAdminPageIfUserCanViewTemplates() {
        CaseInsensitiveString username = cis("user");
        final Username user = new Username(username);
        CruiseConfig config = new BasicCruiseConfig();
        config.addTemplate(createTemplate("s", new Authorization(new ViewConfig(new AdminUser(username)))));
        when(goConfigService.cruiseConfig()).thenReturn(config);

        when(goConfigService.isGroupAdministrator(user.getUsername())).thenReturn(false);
        when(goConfigService.isUserAdmin(user)).thenReturn(false);

        assertThat(securityService.canViewSomeAdminPage(user)).isTrue();
    }

    @Test
    public void shouldReturnFalseForViewingAdminPageForARegularUser() {
        final Username user = new Username(cis("user"));
        CruiseConfig config = new BasicCruiseConfig();
        when(goConfigService.cruiseConfig()).thenReturn(config);
        when(goConfigService.isUserAdmin(user)).thenReturn(false);
        when(goConfigService.isGroupAdministrator(user.getUsername())).thenReturn(false);
        when(goConfigService.isSecurityEnabled()).thenReturn(true);

        SecurityService spy = spy(securityService);
        doReturn(false).when(spy).isAuthorizedToEditTemplates(user);
        doReturn(false).when(spy).isAuthorizedToViewTemplates(user);

        assertThat(spy.canViewSomeAdminPage(user)).isFalse();
    }

    @Test
    public void shouldBeAbleToTellIfAUserIsAnAdmin() {
        Username username = new Username(cis("user"));
        when(goConfigService.isUserAdmin(username)).thenReturn(Boolean.TRUE);
        when(goConfigService.isSecurityEnabled()).thenReturn(true);
        assertThat(securityService.canViewSomeAdminPage(username)).isTrue();
        verify(goConfigService).isUserAdmin(username);
    }

    @Test
    public void shouldBeAbleToTellIfAnUserCanViewTheAdminPage() {
        final Username user = new Username(cis("user"));
        when(goConfigService.isGroupAdministrator(user.getUsername())).thenReturn(Boolean.TRUE);
        assertThat(securityService.canViewSomeAdminPage(new Username(cis("user")))).isTrue();
    }

    @Test
    public void shouldBeAbleToTellIfAnUserISNotAllowedToViewTheAdminPage() {
        final Username user = new Username(cis("user"));
        when(goConfigService.isGroupAdministrator(user.getUsername())).thenReturn(Boolean.FALSE);
        when(goConfigService.isSecurityEnabled()).thenReturn(true);
        SecurityService spy = spy(securityService);
        doReturn(false).when(spy).isAuthorizedToEditTemplates(user);
        doReturn(false).when(spy).isAuthorizedToViewTemplates(user);
        assertThat(spy.canViewSomeAdminPage(new Username(cis("user")))).isFalse();
    }

    @Test
    public void shouldBeAbleToCreatePipelineIfUserIsSuperOrPipelineGroupAdmin() {
        final Username groupAdmin = new Username(cis("groupAdmin"));
        when(goConfigService.isGroupAdministrator(groupAdmin.getUsername())).thenReturn(Boolean.TRUE);
        final Username admin = new Username(cis("admin"));
        when(goConfigService.isGroupAdministrator(admin.getUsername())).thenReturn(Boolean.TRUE);
        assertThat(securityService.canCreatePipelines(new Username(cis("groupAdmin")))).isTrue();
        assertThat(securityService.canCreatePipelines(new Username(cis("admin")))).isTrue();
    }

    @Test
    public void shouldNotBeAbleToCreatePipelineIfUserIsTemplateAdmin() {
        final Username user = new Username(cis("user"));
        when(goConfigService.isGroupAdministrator(user.getUsername())).thenReturn(false);
        when(goConfigService.isUserAdmin(user)).thenReturn(false);
        when(goConfigService.isSecurityEnabled()).thenReturn(true);
        SecurityService spy = spy(securityService);
        doReturn(true).when(spy).isAuthorizedToEditTemplates(user);
        assertThat(spy.canCreatePipelines(new Username(cis("user")))).isFalse();
    }

    @Test
    public void shouldSayThatAUserIsAuthorizedToEditTemplateWhenTheUserIsAnAdminOfThisTemplate() {
        CruiseConfig config = new BasicCruiseConfig();
        CaseInsensitiveString templateName = cis("template");
        CaseInsensitiveString templateAdminName = cis("templateAdmin");

        GoConfigMother.enableSecurityWithPasswordFilePlugin(config);
        GoConfigMother.addUserAsSuperAdmin(config, "theSuperAdmin");
        config.addTemplate(createTemplate("template", new Authorization(new AdminsConfig(new AdminUser(templateAdminName)))));

        when(goConfigService.cruiseConfig()).thenReturn(config);
        when(goConfigService.isUserAdmin(new Username(templateAdminName))).thenReturn(false);

        assertThat(securityService.isAuthorizedToEditTemplate(templateName, new Username(templateAdminName))).isTrue();
        assertThat(securityService.isAuthorizedToEditTemplate(templateName, new Username(cis("someOtherUserWhoIsNotAnAdmin")))).isFalse();
    }

    @Test
    public void shouldSayThatAUserIsAuthorizedToEditTemplateWhenTheUserIsASuperAdmin() {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        String adminName = "theSuperAdmin";
        CaseInsensitiveString templateName = cis("template");

        GoConfigMother.enableSecurityWithPasswordFilePlugin(cruiseConfig);
        GoConfigMother.addUserAsSuperAdmin(cruiseConfig, adminName).addTemplate(createTemplate("template"));

        when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
        when(goConfigService.isUserAdmin(new Username(cis(adminName)))).thenReturn(true);

        assertThat(securityService.isAuthorizedToEditTemplate(templateName, new Username(cis(adminName)))).isTrue();
    }

    @Test
    public void shouldSayThatAUserIsAuthorizedToEditTemplatesWhenTheUserHasPermissionsForAtLeastOneTemplate() {
        CruiseConfig config = new BasicCruiseConfig();
        String theSuperAdmin = "theSuperAdmin";
        String templateName = "template";
        String secondTemplateName = "secondTemplate";
        CaseInsensitiveString templateAdminName = cis("templateAdmin");
        CaseInsensitiveString secondTemplateAdminName = cis("secondTemplateAdmin");

        GoConfigMother.enableSecurityWithPasswordFilePlugin(config);
        GoConfigMother.addUserAsSuperAdmin(config, theSuperAdmin);
        config.addTemplate(createTemplate(templateName, new Authorization(new AdminsConfig(new AdminUser(templateAdminName)))));
        config.addTemplate(createTemplate(secondTemplateName, new Authorization(new AdminsConfig(new AdminUser(secondTemplateAdminName)))));

        when(goConfigService.cruiseConfig()).thenReturn(config);
        when(goConfigService.isUserAdmin(new Username(templateAdminName))).thenReturn(false);
        when(goConfigService.isUserAdmin(new Username(secondTemplateName))).thenReturn(false);
        when(goConfigService.isUserAdmin(new Username(cis(theSuperAdmin)))).thenReturn(true);
        when(goConfigService.isUserAdmin(new Username(cis("someOtherUserWhoIsNotAdminOfAnyTemplates")))).thenReturn(false);

        assertThat(securityService.isAuthorizedToEditTemplates(new Username(templateAdminName))).isTrue();
        assertThat(securityService.isAuthorizedToEditTemplates(new Username(secondTemplateAdminName))).isTrue();
        assertThat(securityService.isAuthorizedToEditTemplates(new Username(cis(theSuperAdmin)))).isTrue();
        assertThat(securityService.isAuthorizedToEditTemplates(new Username(cis("someOtherUserWhoIsNotAdminOfAnyTemplates")))).isFalse();
    }

    @Test
    public void shouldReturnTrueForSuperAdminToViewTemplateConfiguration() {
        BasicCruiseConfig cruiseConfig = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString templateName = cis("template");
        Username admin = new Username(cis("admin"));

        GoConfigMother.addUserAsSuperAdmin(cruiseConfig, "admin").addTemplate(createTemplate("template"));

        when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
        when(goConfigService.isUserAdmin(admin)).thenReturn(true);

        assertThat(securityService.isAuthorizedToViewTemplate(templateName, admin)).isTrue();
    }

    @Test
    public void shouldReturnTrueForTemplateAdminsToViewTemplateConfiguration() {
        CruiseConfig config = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString templateAdmin = cis("templateAdmin");
        CaseInsensitiveString templateName = cis("template");

        config.addTemplate(createTemplate("template", new Authorization(new AdminsConfig(new AdminUser(templateAdmin)))));

        when(goConfigService.cruiseConfig()).thenReturn(config);
        when(goConfigService.isUserAdmin(new Username(templateAdmin))).thenReturn(false);

        assertThat(securityService.isAuthorizedToViewTemplate(templateName, new Username(templateAdmin))).isTrue();
    }

    @Test
    public void shouldReturnTrueForTemplateViewUsersToViewTemplateConfiguration() {
        CruiseConfig config = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString templateViewUser = cis("templateView");
        CaseInsensitiveString templateName = cis("template");

        config.addTemplate(createTemplate("template", new Authorization(new ViewConfig(new AdminUser(templateViewUser)))));

        when(goConfigService.cruiseConfig()).thenReturn(config);
        when(goConfigService.isUserAdmin(new Username(templateViewUser))).thenReturn(false);

        assertThat(securityService.isAuthorizedToViewTemplate(templateName, new Username(templateViewUser))).isTrue();
    }

    @Test
    public void shouldReturnTrueForGroupAdminsToViewTemplateConfigurationByDefault() {
        CruiseConfig config = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString groupAdmin = cis("groupAdmin");
        setUpGroupWithAuthorization(config, new Authorization(new AdminsConfig(new AdminUser(groupAdmin))));
        CaseInsensitiveString templateName = cis("template");

        config.addTemplate(createTemplate("template"));

        when(goConfigService.cruiseConfig()).thenReturn(config);
        when(goConfigService.isUserAdmin(new Username(groupAdmin))).thenReturn(false);
        when(goConfigService.isGroupAdministrator(groupAdmin)).thenReturn(true);

        assertThat(securityService.isAuthorizedToViewTemplate(templateName, new Username(groupAdmin))).isTrue();
    }

    @Test
    public void shouldReturnFalseForGroupAdminsToViewTemplateConfigurationIfDisallowed() {
        CruiseConfig config = getCruiseConfigWithSecurityEnabled();
        CaseInsensitiveString groupAdmin = cis("groupAdmin");

        setUpGroupWithAuthorization(config, new Authorization(new AdminsConfig(new AdminUser(groupAdmin))));

        CaseInsensitiveString templateName = cis("template");

        PipelineTemplateConfig template = createTemplate("template");
        template.getAuthorization().setAllowGroupAdmins(false);
        config.addTemplate(template);

        when(goConfigService.cruiseConfig()).thenReturn(config);
        when(goConfigService.isUserAdmin(new Username(groupAdmin))).thenReturn(false);
        when(goConfigService.isGroupAdministrator(groupAdmin)).thenReturn(true);

        assertThat(securityService.isAuthorizedToViewTemplate(templateName, new Username(groupAdmin))).isFalse();
    }

    @Test
    public void shouldReturnTrueForGroupAdminsWithinARoleToViewTemplate() {
        CaseInsensitiveString groupAdmin = cis("groupAdmin");
        BasicCruiseConfig config = new BasicCruiseConfig();
        ServerConfig serverConfig = new ServerConfig(new SecurityConfig(new AdminsConfig(new AdminUser(cis("admin")))), null);
        RoleConfig role = new RoleConfig(cis("role1"), new RoleUser(groupAdmin));
        serverConfig.security().addRole(role);
        config.setServerConfig(serverConfig);
        GoConfigMother.enableSecurityWithPasswordFilePlugin(config);

        setUpGroupWithAuthorization(config, new Authorization(new AdminsConfig(new AdminRole(role))));

        CaseInsensitiveString templateName = cis("template");

        config.addTemplate(createTemplate("template"));

        when(goConfigService.cruiseConfig()).thenReturn(config);
        when(goConfigService.isUserAdmin(new Username(groupAdmin))).thenReturn(false);
        when(goConfigService.isGroupAdministrator(groupAdmin)).thenReturn(true);

        assertThat(securityService.isAuthorizedToViewTemplate(templateName, new Username(groupAdmin))).isTrue();
    }

    @Test
    public void shouldReturnFalseForGroupAdminsWithinARoleToVIewTemplateIfDisallowed() {
        CaseInsensitiveString groupAdmin = cis("groupAdmin");
        BasicCruiseConfig config = new BasicCruiseConfig();
        ServerConfig serverConfig = new ServerConfig(new SecurityConfig(new AdminsConfig(new AdminUser(cis("admin")))), null);
        RoleConfig role = new RoleConfig(cis("role1"), new RoleUser(groupAdmin));
        serverConfig.security().addRole(role);
        config.setServerConfig(serverConfig);
        GoConfigMother.enableSecurityWithPasswordFilePlugin(config);

        setUpGroupWithAuthorization(config, new Authorization(new AdminsConfig(new AdminRole(role))));

        CaseInsensitiveString templateName = cis("template");

        PipelineTemplateConfig template = createTemplate("template");
        template.getAuthorization().setAllowGroupAdmins(false);
        config.addTemplate(template);

        when(goConfigService.cruiseConfig()).thenReturn(config);
        when(goConfigService.isUserAdmin(new Username(groupAdmin))).thenReturn(false);
        when(goConfigService.isGroupAdministrator(groupAdmin)).thenReturn(true);

        assertThat(securityService.isAuthorizedToViewTemplate(templateName, new Username(groupAdmin))).isFalse();
    }

    @Test
    public void shouldSayUserIsAuthorizedToViewTemplatesWhenTheUserHasViewPermissionsToAtLeastOneTemplate() {
        CruiseConfig config = new BasicCruiseConfig();
        String theSuperAdmin = "theSuperAdmin";
        String templateName = "template";
        String secondTemplateName = "secondTemplate";
        CaseInsensitiveString templateAdminName = cis("templateAdmin");
        CaseInsensitiveString templateViewUser = cis("templateViewUser");

        GoConfigMother.enableSecurityWithPasswordFilePlugin(config);
        GoConfigMother.addUserAsSuperAdmin(config, theSuperAdmin);
        config.addTemplate(createTemplate(templateName, new Authorization(new AdminsConfig(new AdminUser(templateAdminName)))));
        config.addTemplate(createTemplate(secondTemplateName, new Authorization(new ViewConfig(new AdminUser(templateViewUser)))));

        when(goConfigService.cruiseConfig()).thenReturn(config);
        when(goConfigService.isUserAdmin(new Username(templateAdminName))).thenReturn(false);
        when(goConfigService.isUserAdmin(new Username(templateViewUser))).thenReturn(false);
        when(goConfigService.isUserAdmin(new Username(cis(theSuperAdmin)))).thenReturn(true);
        when(goConfigService.isUserAdmin(new Username(cis("regularUser")))).thenReturn(false);

        assertThat(securityService.isAuthorizedToViewTemplates(new Username(templateAdminName))).isTrue();
        assertThat(securityService.isAuthorizedToViewTemplates(new Username(templateViewUser))).isTrue();
        assertThat(securityService.isAuthorizedToViewTemplates(new Username(cis(theSuperAdmin)))).isTrue();
        assertThat(securityService.isAuthorizedToViewTemplates(new Username(cis("regularUser")))).isFalse();
    }

    @Test
    public void shouldAllowUserToAccessAgentStatusReportPageWhenAllowViewPermissionIsDefined() {
        String elasticAgentProfileId = "elastic-profile-id";
        String clusterProfileId = "cluster-profile-id";
        Username bob = new Username("Bob" + UUID.randomUUID());
        RoleConfig roleConfig = new RoleConfig(cis("elastic-profile-users"), new RoleUser(bob.getUsername()));
        Policy policy = new Policy();
        policy.add(new Allow("view", "elastic_agent_profile", elasticAgentProfileId));
        roleConfig.setPolicy(policy);
        when(goConfigService.isSecurityEnabled()).thenReturn(true);
        when(goConfigService.rolesForUser(bob.getUsername())).thenReturn(new RolesConfig(roleConfig));

        assertThat(securityService.doesUserHasPermissions(bob, SupportedAction.VIEW, SupportedEntity.ELASTIC_AGENT_PROFILE, elasticAgentProfileId, clusterProfileId)).isTrue();
    }

    @Test
    public void shouldDenyUserToAccessAgentStatusReportPageWhenDenyViewPermissionIsDefined() {
        String elasticAgentProfileId = "elastic-profile-id";
        String clusterProfileId = "cluster-profile-id";
        Username bob = new Username("Bob" + UUID.randomUUID());
        RoleConfig roleConfig = new RoleConfig(cis("elastic-profile-users"), new RoleUser(bob.getUsername()));
        Policy policy = new Policy();
        policy.add(new Deny("view", "elastic_agent_profile", elasticAgentProfileId));
        roleConfig.setPolicy(policy);
        when(goConfigService.isSecurityEnabled()).thenReturn(true);
        when(goConfigService.rolesForUser(bob.getUsername())).thenReturn(new RolesConfig(roleConfig));

        assertThat(securityService.doesUserHasPermissions(bob, SupportedAction.VIEW, SupportedEntity.ELASTIC_AGENT_PROFILE, elasticAgentProfileId, clusterProfileId)).isFalse();
    }

    @Test
    public void shouldDenyUserToAccessAgentStatusReportPageWhenNoViewPermissionIsDefined() {
        String elasticAgentProfileId = "elastic-profile-id";
        String clusterProfileId = "cluster-profile-id";
        Username bob = new Username("Bob" + UUID.randomUUID());
        when(goConfigService.isSecurityEnabled()).thenReturn(true);
        when(goConfigService.rolesForUser(bob.getUsername())).thenReturn(new RolesConfig());

        assertThat(securityService.doesUserHasPermissions(bob, SupportedAction.VIEW, SupportedEntity.ELASTIC_AGENT_PROFILE, elasticAgentProfileId, clusterProfileId)).isFalse();
    }

    private BasicCruiseConfig getCruiseConfigWithSecurityEnabled() {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        ServerConfig serverConfig = new ServerConfig(new SecurityConfig(new AdminsConfig(new AdminUser(cis("admin")))), null);
        cruiseConfig.setServerConfig(serverConfig);
        GoConfigMother.enableSecurityWithPasswordFilePlugin(cruiseConfig);
        return cruiseConfig;
    }

    private void setUpGroupWithAuthorization(CruiseConfig config, Authorization authorization) {
        new GoConfigMother().addPipelineWithGroup(config, "group", "pipeline", "stage", "job");
        config.getGroups().findGroup("group").setAuthorization(authorization);
    }
}
