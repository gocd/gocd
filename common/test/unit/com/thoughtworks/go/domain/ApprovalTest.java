/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import org.apache.commons.collections.map.SingletonMap;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ApprovalTest {

    public static final String DEFAULT_GROUP = "defaultGroup";

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void shouldNotAssignType() throws Exception {
        Approval approval = new Approval();
        approval.setConfigAttributes(new SingletonMap(Approval.TYPE, Approval.SUCCESS));
        assertThat(approval.getType(), is(Approval.SUCCESS));
        approval.setConfigAttributes(new HashMap());
        assertThat(approval.getType(), is(Approval.SUCCESS));

        approval.setConfigAttributes(new SingletonMap(Approval.TYPE, Approval.MANUAL));
        assertThat(approval.getType(), is(Approval.MANUAL));
        approval.setConfigAttributes(new HashMap());
        assertThat(approval.getType(), is(Approval.MANUAL));
    }

    @Test
    public void shouldValidateApprovalType() throws Exception {
        Approval approval = new Approval();
        approval.setConfigAttributes(new SingletonMap(Approval.TYPE, "not-manual-or-success"));
        assertThat(approval.getType(), is("not-manual-or-success"));
        approval.validate(ConfigSaveValidationContext.forChain(new BasicCruiseConfig(), new BasicPipelineConfigs()));
        assertThat(approval.errors().firstError(), is("You have defined approval type as 'not-manual-or-success'. Approval can only be of the type 'manual' or 'success'."));
    }

    @Test
    public void shouldFailValidateWhenUsersWithoutOperatePermissionOnGroupAreAuthorizedToApproveStage_WithPipelineConfigSaveValidationContext() {
        CruiseConfig cruiseConfig = cruiseConfigWithSecurity(
                new Role(new CaseInsensitiveString("role"), new RoleUser(new CaseInsensitiveString("first")), new RoleUser(new CaseInsensitiveString("second"))), new AdminUser(
                        new CaseInsensitiveString("admin")));

        addUserAndRoleToDefaultGroup(cruiseConfig, "user", "role");

        PipelineConfig pipeline = cruiseConfig.find(DEFAULT_GROUP, 0);
        StageConfig stage = pipeline.get(0);
        StageConfigMother.addApprovalWithUsers(stage, "not-present");
        Approval approval = stage.getApproval();

        approval.validate(PipelineConfigSaveValidationContext.forChain(true, DEFAULT_GROUP, cruiseConfig, pipeline, stage));

        AdminUser user = approval.getAuthConfig().getUsers().get(0);
        assertThat(user.errors().isEmpty(), is(false));
        assertThat(user.errors().on("name"), is("User \"not-present\" who is not authorized to operate pipeline group can not be authorized to approve stage"));
    }

    @Test
    public void shouldPassValidateWhenNoPermissionAreSetupOnGroupAndUserIsAuthorizedToApproveStage_WithPipelineConfigSaveValidationContext() {
        CruiseConfig cruiseConfig = cruiseConfigWithSecurity(
                new Role(new CaseInsensitiveString("role"),
                        new RoleUser(new CaseInsensitiveString("first")),
                        new RoleUser(new CaseInsensitiveString("second"))),
                new AdminUser(
                        new CaseInsensitiveString("admin")));

        PipelineConfig pipeline = cruiseConfig.find(DEFAULT_GROUP, 0);
        StageConfig stage = pipeline.get(0);
        StageConfigMother.addApprovalWithUsers(stage, "not-present");
        Approval approval = stage.getApproval();

        approval.validate(PipelineConfigSaveValidationContext.forChain(true, DEFAULT_GROUP, cruiseConfig, pipeline, stage));

        assertNoErrors(approval.getAuthConfig().getUsers().get(0));
    }

    @Test
    public void shouldPassValidateWhenARoleIsAdminOnGroupAndThatRoleIsAuthorizedToApproveStage_WithPipelineConfigSaveValidationContext() {
        CruiseConfig cruiseConfig = cruiseConfigWithSecurity(
                new Role(new CaseInsensitiveString("role"),
                        new RoleUser(new CaseInsensitiveString("first")),
                        new RoleUser(new CaseInsensitiveString("second"))),
                new AdminUser(new CaseInsensitiveString("admin")));

        addUserAsOperatorToDefaultGroup(cruiseConfig, "user");
        addRoleAsAdminToDefaultGroup(cruiseConfig, "role");

        PipelineConfig pipeline = cruiseConfig.find(DEFAULT_GROUP, 0);
        StageConfig stage = pipeline.get(0);
        StageConfigMother.addApprovalWithRoles(stage, "role");
        Approval approval = stage.getApproval();

        approval.validate(PipelineConfigSaveValidationContext.forChain(true, DEFAULT_GROUP, cruiseConfig, pipeline, stage));

        assertNoErrors(approval.getAuthConfig().getRoles().get(0));
    }

    @Test
    public void shouldReturnDisplayNameForApprovalType() {
        Approval approval = Approval.automaticApproval();
        assertThat(approval.getDisplayName(), is("On Success"));
        approval = Approval.manualApproval();
        assertThat(approval.getDisplayName(), is("Manual"));
    }

    @Test
    public void shouldOverwriteExistingUsersWhileSettingNewUsers() {
        Approval approval = Approval.automaticApproval();
        approval.getAuthConfig().add(new AdminUser(new CaseInsensitiveString("sachin")));
        approval.getAuthConfig().add(new AdminRole(new CaseInsensitiveString("admin")));

        List names = new ArrayList();
        names.add(nameMap("awesome_shilpa"));
        names.add(nameMap("youth"));
        names.add(nameMap(""));

        List roles = new ArrayList();
        roles.add(nameMap("role1"));
        roles.add(nameMap("role2"));
        roles.add(nameMap(""));


        approval.setOperatePermissions(names, roles);

        assertThat(approval.getAuthConfig().size(), is(4));
        assertThat(approval.getAuthConfig(), hasItem((Admin) new AdminUser(new CaseInsensitiveString("awesome_shilpa"))));
        assertThat(approval.getAuthConfig(), hasItem((Admin) new AdminUser(new CaseInsensitiveString("youth"))));
        assertThat(approval.getAuthConfig(), hasItem((Admin) new AdminRole(new CaseInsensitiveString("role1"))));
        assertThat(approval.getAuthConfig(), hasItem((Admin) new AdminRole(new CaseInsensitiveString("role2"))));
    }

    @Test
    public void shouldClearAllPermissions() {
        Approval approval = Approval.automaticApproval();
        approval.getAuthConfig().add(new AdminUser(new CaseInsensitiveString("sachin")));
        approval.getAuthConfig().add(new AdminRole(new CaseInsensitiveString("admin")));

        approval.removeOperatePermissions();

        assertThat(approval.getAuthConfig().isEmpty(), is(true));
    }

    @Test
    public void shouldClearAllPermissionsWhenTheAttributesAreNull() {
        Approval approval = Approval.automaticApproval();
        approval.getAuthConfig().add(new AdminUser(new CaseInsensitiveString("sachin")));
        approval.getAuthConfig().add(new AdminRole(new CaseInsensitiveString("admin")));

        approval.setOperatePermissions(null, null);

        assertThat(approval.getAuthConfig().isEmpty(), is(true));
    }

    @Test
    public void validate_shouldNotAllow_UserInApprovalListButNotInOperationList() {
        CruiseConfig cruiseConfig = cruiseConfigWithSecurity(
                new Role(new CaseInsensitiveString("role"), new RoleUser(new CaseInsensitiveString("first")), new RoleUser(new CaseInsensitiveString("second"))), new AdminUser(
                new CaseInsensitiveString("admin")));

        PipelineConfigs group = addUserAndRoleToDefaultGroup(cruiseConfig, "user", "role");
        PipelineConfig pipeline = cruiseConfig.find(DEFAULT_GROUP, 0);
        StageConfig stage = pipeline.get(0);
        StageConfigMother.addApprovalWithUsers(stage, "not-present");
        Approval approval = stage.getApproval();

        approval.validate(ConfigSaveValidationContext.forChain(cruiseConfig, group, pipeline, stage));

        AdminUser user = approval.getAuthConfig().getUsers().get(0);
        assertThat(user.errors().isEmpty(), is(false));
        assertThat(user.errors().on("name"), is("User \"not-present\" who is not authorized to operate pipeline group can not be authorized to approve stage"));
    }

    @Test
    public void validate_shouldNotAllowRoleInApprovalListButNotInOperationList() throws Exception {
        CruiseConfig cruiseConfig = cruiseConfigWithSecurity(
                new Role(new CaseInsensitiveString("role"), new RoleUser(new CaseInsensitiveString("first")), new RoleUser(new CaseInsensitiveString("second"))), new AdminUser(
                new CaseInsensitiveString("admin")));

        PipelineConfigs group = addUserAndRoleToDefaultGroup(cruiseConfig, "user", "role");
        PipelineConfig pipeline = cruiseConfig.find(DEFAULT_GROUP, 0);
        StageConfig stage = pipeline.get(0);
        StageConfigMother.addApprovalWithRoles(stage, "not-present");
        Approval approval = stage.getApproval();

        approval.validate(ConfigSaveValidationContext.forChain(cruiseConfig, group, pipeline, stage));

        AdminRole user = approval.getAuthConfig().getRoles().get(0);
        assertThat(user.errors().isEmpty(), is(false));
        assertThat(user.errors().on("name"), is("Role \"not-present\" who is not authorized to operate pipeline group can not be authorized to approve stage"));
    }

    @Test
    public void validate_shouldAllowUserWhoseRoleHasOperatePermission() throws Exception {
        CruiseConfig cruiseConfig = cruiseConfigWithSecurity(
                new Role(new CaseInsensitiveString("role"), new RoleUser(new CaseInsensitiveString("first")), new RoleUser(new CaseInsensitiveString("second"))), new AdminUser(
                        new CaseInsensitiveString("admin")));

        PipelineConfigs group = addUserAndRoleToDefaultGroup(cruiseConfig, "user", "role");
        PipelineConfig pipeline = cruiseConfig.find(DEFAULT_GROUP, 0);
        StageConfig stage = pipeline.get(0);
        StageConfigMother.addApprovalWithUsers(stage, "first");
        Approval approval = stage.getApproval();

        approval.validate(ConfigSaveValidationContext.forChain(cruiseConfig, group, pipeline, stage));

        assertNoErrors(approval.getAuthConfig().getUsers().get(0));
    }

    @Test
    public void validate_shouldAllowUserWhoIsDefinedInGroup() throws Exception {
        CruiseConfig cruiseConfig = cruiseConfigWithSecurity(
                new Role(new CaseInsensitiveString("role"), new RoleUser(new CaseInsensitiveString("first")), new RoleUser(new CaseInsensitiveString("second"))), new AdminUser(
                new CaseInsensitiveString("admin")));

        PipelineConfigs group = addUserAndRoleToDefaultGroup(cruiseConfig, "user", "role");
        PipelineConfig pipeline = cruiseConfig.find(DEFAULT_GROUP, 0);
        StageConfig stage = pipeline.get(0);
        StageConfigMother.addApprovalWithUsers(stage, "user");
        Approval approval = stage.getApproval();

        approval.validate(ConfigSaveValidationContext.forChain(cruiseConfig, group, pipeline, stage));

        assertNoErrors(approval.getAuthConfig().getUsers().get(0));
    }

    @Test
    public void validate_shouldAllowUserWhenSecurityIsNotDefinedInGroup() throws Exception {
        CruiseConfig cruiseConfig = cruiseConfigWithSecurity(
                new Role(new CaseInsensitiveString("role"), new RoleUser(new CaseInsensitiveString("first")), new RoleUser(new CaseInsensitiveString("second"))), new AdminUser(
                new CaseInsensitiveString("admin")));

        PipelineConfigs group = cruiseConfig.findGroup(DEFAULT_GROUP);
        PipelineConfig pipeline = cruiseConfig.find(DEFAULT_GROUP, 0);
        StageConfig stage = pipeline.get(0);
        StageConfigMother.addApprovalWithUsers(stage, "user");
        Approval approval = stage.getApproval();

        approval.validate(ConfigSaveValidationContext.forChain(cruiseConfig, group, pipeline, stage));

        assertNoErrors(approval.getAuthConfig().getUsers().get(0));
    }

    @Test
    public void validate_shouldAllowAdminToOperateOnAStage() throws Exception {
        CruiseConfig cruiseConfig = cruiseConfigWithSecurity(
                new Role(new CaseInsensitiveString("role"), new RoleUser(new CaseInsensitiveString("first")), new RoleUser(new CaseInsensitiveString("second"))), new AdminUser(
                new CaseInsensitiveString("admin")));

        PipelineConfigs group = addUserAndRoleToDefaultGroup(cruiseConfig, "user", "role");
        PipelineConfig pipeline = cruiseConfig.find(DEFAULT_GROUP, 0);
        StageConfig stage = pipeline.get(0);
        StageConfigMother.addApprovalWithUsers(stage, "admin");
        Approval approval = stage.getApproval();

        approval.validate(ConfigSaveValidationContext.forChain(cruiseConfig, group, pipeline, stage));

        assertNoErrors(approval.getAuthConfig().getUsers().get(0));
    }

    @Test
    public void shouldShowBugWhichAllowsAUserWithoutOperatePermissionToOperateAStage() throws Exception {
        CruiseConfig cruiseConfig = cruiseConfigWithSecurity(
                new Role(new CaseInsensitiveString("role"),
                        new RoleUser(new CaseInsensitiveString("first")),
                        new RoleUser(new CaseInsensitiveString("second"))),
                new AdminUser(new CaseInsensitiveString("admin")));

        addRoleAsAdminToDefaultGroup(cruiseConfig, "role");

        PipelineConfig pipeline = cruiseConfig.find(DEFAULT_GROUP, 0);
        StageConfig stage = pipeline.get(0);
        StageConfigMother.addApprovalWithUsers(stage, "first", "some-other-user-who-is-not-operate-authorized");
        Approval approval = stage.getApproval();
        

        approval.validate(PipelineConfigSaveValidationContext.forChain(true, DEFAULT_GROUP, cruiseConfig, pipeline, stage));

        assertNoErrors(approval.getAuthConfig().getUsers().get(0));
        /* https://github.com/gocd/gocd/pull/1779#issuecomment-170161521 */
        assertNoErrors(approval.getAuthConfig().getUsers().get(1));
    }

    @Test
    public void validate_shouldNotTryAndValidateWhenWithinTemplate() throws Exception {
        CruiseConfig cruiseConfig = cruiseConfigWithSecurity(
                new Role(new CaseInsensitiveString("role"), new RoleUser(new CaseInsensitiveString("first")), new RoleUser(new CaseInsensitiveString("second"))), new AdminUser(
                new CaseInsensitiveString("admin")));

        PipelineConfigs group = addUserAndRoleToDefaultGroup(cruiseConfig, "user", "role");
        PipelineConfig pipeline = cruiseConfig.find(DEFAULT_GROUP, 0);
        StageConfig stage = pipeline.get(0);
        StageConfigMother.addApprovalWithUsers(stage, "not-present");
        Approval approval = stage.getApproval();

        approval.validate(ConfigSaveValidationContext.forChain(cruiseConfig, new TemplatesConfig(), stage));
        assertNoErrors(approval.getAuthConfig().getUsers().get(0));
    }

    @Test
    public void shouldValidateTree(){
        Approval approval = new Approval(new AuthConfig(new AdminRole(new CaseInsensitiveString("role"))));
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.server().security().adminsConfig().addRole(new AdminRole(new CaseInsensitiveString("super-admin")));
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("p1"), new MaterialConfigs());
        cruiseConfig.addPipeline("g1", pipelineConfig);
        
        assertThat(approval.validateTree(PipelineConfigSaveValidationContext.forChain(true, "g1", cruiseConfig, pipelineConfig)), is(false));
        assertThat(approval.getAuthConfig().errors().isEmpty(), is(false));
        assertThat(approval.getAuthConfig().errors().firstError(), is("Role \"role\" does not exist."));
    }

    private CruiseConfig cruiseConfigWithSecurity(Role roleDefinition, Admin admins) {
        CruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("pipeline");
        SecurityConfig securityConfig = cruiseConfig.server().security();
        securityConfig.modifyPasswordFile(new PasswordFileConfig("foo.bar"));
        securityConfig.addRole(roleDefinition);
        securityConfig.adminsConfig().add(admins);
        return cruiseConfig;
    }

    private PipelineConfigs addUserAndRoleToDefaultGroup(CruiseConfig cruiseConfig, final String user, final String role) {
        PipelineConfigs group = cruiseConfig.findGroup(DEFAULT_GROUP);
        addUserAsOperatorToDefaultGroup(cruiseConfig, user);
        addRoleAsOperatorToDefaultGroup(cruiseConfig, role);
        return group;
    }

    private void addRoleAsOperatorToDefaultGroup(CruiseConfig goConfig, String role) {
        PipelineConfigs group = goConfig.findGroup(DEFAULT_GROUP);
        group.getAuthorization().getOperationConfig().add(new AdminRole(new CaseInsensitiveString(role)));
    }

    private PipelineConfigs addRoleAsAdminToDefaultGroup(CruiseConfig cruiseConfig, String role) {
        PipelineConfigs group = cruiseConfig.findGroup(DEFAULT_GROUP);
        group.getAuthorization().getAdminsConfig().add(new AdminRole(new CaseInsensitiveString(role)));
        return group;
    }

    private PipelineConfigs addUserAsOperatorToDefaultGroup(CruiseConfig cruiseConfig, String user) {
        PipelineConfigs group = cruiseConfig.findGroup(DEFAULT_GROUP);
        group.getAuthorization().getOperationConfig().add(new AdminUser(new CaseInsensitiveString(user)));
        return group;
    }

    private HashMap nameMap(final String name) {
        HashMap nameMap = new HashMap();
        nameMap.put("name", name);
        return nameMap;
    }

    private void assertNoErrors(Admin userOrRole) {
        assertThat(userOrRole.errors().getAll().toString(), userOrRole.errors().isEmpty(), is(true));
    }
}
