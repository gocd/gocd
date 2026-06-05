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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.config.Approval.TYPE_MANUAL;
import static com.thoughtworks.go.config.Approval.TYPE_SUCCESS;
import static com.thoughtworks.go.config.CaseInsensitiveString.cis;
import static org.assertj.core.api.Assertions.assertThat;

public class ApprovalTest {

    public static final String DEFAULT_GROUP = "defaultGroup";

    @Test
    void shouldSetDefaultValues() {
        Approval approval = new Approval();
        assertThat(approval.getType()).isEqualTo(TYPE_MANUAL);
        assertThat(approval.getAuthConfig()).isEmpty();
        assertThat(approval.getDisplayName()).isEqualTo("Manual");
    }

    @Test
    public void shouldNotAssignType() {
        Approval approval = new Approval();
        approval.setConfigAttributes(Map.of(Approval.TYPE, TYPE_SUCCESS));
        assertThat(approval.getType()).isEqualTo(TYPE_SUCCESS);
        approval.setConfigAttributes(new HashMap<>());
        assertThat(approval.getType()).isEqualTo(TYPE_SUCCESS);

        approval.setConfigAttributes(Map.of(Approval.TYPE, TYPE_MANUAL));
        assertThat(approval.getType()).isEqualTo(TYPE_MANUAL);
        approval.setConfigAttributes(new HashMap<>());
        assertThat(approval.getType()).isEqualTo(TYPE_MANUAL);
    }

    @Test
    void shouldValidateApprovalType() {
        Approval approval = new Approval();
        approval.setConfigAttributes(Map.of(Approval.TYPE, "not-manual-or-success"));
        assertThat(approval.getType()).isEqualTo("not-manual-or-success");
        approval.validate(ConfigSaveValidationContext.forChain(new BasicCruiseConfig(), new BasicPipelineConfigs()));
        assertThat(approval.errors().firstError()).isEqualTo("You have defined approval type as 'not-manual-or-success'. Approval can only be of the type 'manual' or 'success'.");
    }

    @Test
    void shouldFailValidateWhenUsersWithoutOperatePermissionOnGroupAreAuthorizedToApproveStage_WithPipelineConfigSaveValidationContext() {
        CruiseConfig cruiseConfig = cruiseConfigWithSecurity(
                new RoleConfig(cis("role"), new RoleUser(cis("first")), new RoleUser(cis("second"))), new AdminUser(
                        cis("admin")));

        addUserAndRoleToDefaultGroup(cruiseConfig, "user", "role");

        PipelineConfig pipeline = cruiseConfig.findGroup(DEFAULT_GROUP).getFirst();
        StageConfig stage = pipeline.getFirst();
        StageConfigMother.addApprovalWithUsers(stage, "not-present");
        Approval approval = stage.getApproval();

        approval.validate(PipelineConfigSaveValidationContext.forChain(true, DEFAULT_GROUP, cruiseConfig, pipeline, stage));

        AdminUser user = approval.getAuthConfig().getUsers().getFirst();
        assertThat(user.errors().isEmpty()).isFalse();
        assertThat(user.errors().firstErrorOn("name")).isEqualTo("User \"not-present\" who is not authorized to operate pipeline group `defaultGroup` can not be authorized to approve stage");
    }

    @Test
    void shouldPassValidateWhenNoPermissionAreSetupOnGroupAndUserIsAuthorizedToApproveStage_WithPipelineConfigSaveValidationContext() {
        CruiseConfig cruiseConfig = cruiseConfigWithSecurity(
                new RoleConfig(cis("role"),
                        new RoleUser(cis("first")),
                        new RoleUser(cis("second"))),
                new AdminUser(
                        cis("admin")));

        PipelineConfig pipeline = cruiseConfig.findGroup(DEFAULT_GROUP).getFirst();
        StageConfig stage = pipeline.getFirst();
        StageConfigMother.addApprovalWithUsers(stage, "not-present");
        Approval approval = stage.getApproval();

        approval.validate(PipelineConfigSaveValidationContext.forChain(true, DEFAULT_GROUP, cruiseConfig, pipeline, stage));

        assertNoErrors(approval.getAuthConfig().getUsers().getFirst());
    }

    @Test
    void shouldPassValidateWhenARoleIsAdminOnGroupAndThatRoleIsAuthorizedToApproveStage_WithPipelineConfigSaveValidationContext() {
        CruiseConfig cruiseConfig = cruiseConfigWithSecurity(
                new RoleConfig(cis("role"),
                        new RoleUser(cis("first")),
                        new RoleUser(cis("second"))),
                new AdminUser(cis("admin")));

        addUserAsOperatorToDefaultGroup(cruiseConfig, "user");
        addRoleAsAdminToDefaultGroup(cruiseConfig, "role");

        PipelineConfig pipeline = cruiseConfig.findGroup(DEFAULT_GROUP).getFirst();
        StageConfig stage = pipeline.getFirst();
        StageConfigMother.addApprovalWithRoles(stage, "role");
        Approval approval = stage.getApproval();

        approval.validate(PipelineConfigSaveValidationContext.forChain(true, DEFAULT_GROUP, cruiseConfig, pipeline, stage));

        assertNoErrors(approval.getAuthConfig().getRoles().getFirst());
    }

    @Test
    void shouldReturnDisplayNameForApprovalType() {
        assertThat(Approval.automaticApproval().getDisplayName()).isEqualTo("On Success");
        assertThat(Approval.manualApproval().getDisplayName()).isEqualTo("Manual");
    }

    @Test
    void shouldOverwriteExistingUsersWhileSettingNewUsers() {
        Approval approval = Approval.automaticApproval();
        approval.getAuthConfig().add(new AdminUser(cis("sachin")));
        approval.getAuthConfig().add(new AdminRole(cis("admin")));

        List<Map<String, String>> names = new ArrayList<>();
        names.add(nameMap("awesome_shilpa"));
        names.add(nameMap("youth"));
        names.add(nameMap(""));

        List<Map<String, String>> roles = new ArrayList<>();
        roles.add(nameMap("role1"));
        roles.add(nameMap("role2"));
        roles.add(nameMap(""));


        approval.setOperatePermissions(names, roles);

        assertThat(approval.getAuthConfig().size()).isEqualTo(4);
        assertThat(approval.getAuthConfig()).contains(new AdminUser(cis("awesome_shilpa")));
        assertThat(approval.getAuthConfig()).contains(new AdminUser(cis("youth")));
        assertThat(approval.getAuthConfig()).contains(new AdminRole(cis("role1")));
        assertThat(approval.getAuthConfig()).contains(new AdminRole(cis("role2")));
    }

    @Test
    void shouldClearAllPermissions() {
        Approval approval = Approval.automaticApproval();
        approval.getAuthConfig().add(new AdminUser(cis("sachin")));
        approval.getAuthConfig().add(new AdminRole(cis("admin")));

        approval.removeOperatePermissions();

        assertThat(approval.getAuthConfig().isEmpty()).isTrue();
    }

    @Test
    void shouldClearAllPermissionsWhenTheAttributesAreNull() {
        Approval approval = Approval.automaticApproval();
        approval.getAuthConfig().add(new AdminUser(cis("sachin")));
        approval.getAuthConfig().add(new AdminRole(cis("admin")));

        approval.setOperatePermissions(null, null);

        assertThat(approval.getAuthConfig().isEmpty()).isTrue();
    }

    @Test
    void validate_shouldNotAllow_UserInApprovalListButNotInOperationList() {
        CruiseConfig cruiseConfig = cruiseConfigWithSecurity(
                new RoleConfig(cis("role"), new RoleUser(cis("first")), new RoleUser(cis("second"))), new AdminUser(
                        cis("admin")));

        PipelineConfigs group = addUserAndRoleToDefaultGroup(cruiseConfig, "user", "role");
        PipelineConfig pipeline = cruiseConfig.findGroup(DEFAULT_GROUP).getFirst();
        StageConfig stage = pipeline.getFirst();
        StageConfigMother.addApprovalWithUsers(stage, "not-present");
        Approval approval = stage.getApproval();

        approval.validate(ConfigSaveValidationContext.forChain(cruiseConfig, group, pipeline, stage));

        AdminUser user = approval.getAuthConfig().getUsers().getFirst();
        assertThat(user.errors().isEmpty()).isFalse();
        assertThat(user.errors().firstErrorOn("name")).isEqualTo("User \"not-present\" who is not authorized to operate pipeline group `defaultGroup` can not be authorized to approve stage");
    }

    @Test
    void validate_shouldNotAllowRoleInApprovalListButNotInOperationList() {
        CruiseConfig cruiseConfig = cruiseConfigWithSecurity(
                new RoleConfig(cis("role"), new RoleUser(cis("first")), new RoleUser(cis("second"))), new AdminUser(
                        cis("admin")));

        PipelineConfigs group = addUserAndRoleToDefaultGroup(cruiseConfig, "user", "role");
        PipelineConfig pipeline = cruiseConfig.findGroup(DEFAULT_GROUP).getFirst();
        StageConfig stage = pipeline.getFirst();
        StageConfigMother.addApprovalWithRoles(stage, "not-present");
        Approval approval = stage.getApproval();

        approval.validate(ConfigSaveValidationContext.forChain(cruiseConfig, group, pipeline, stage));

        AdminRole user = approval.getAuthConfig().getRoles().getFirst();
        assertThat(user.errors().isEmpty()).isFalse();
        assertThat(user.errors().firstErrorOn("name")).isEqualTo("Role \"not-present\" who is not authorized to operate pipeline group `defaultGroup` can not be authorized to approve stage");
    }

    @Test
    void validate_shouldAllowUserWhoseRoleHasOperatePermission() {
        CruiseConfig cruiseConfig = cruiseConfigWithSecurity(
                new RoleConfig(cis("role"), new RoleUser(cis("first")), new RoleUser(cis("second"))), new AdminUser(
                        cis("admin")));

        PipelineConfigs group = addUserAndRoleToDefaultGroup(cruiseConfig, "user", "role");
        PipelineConfig pipeline = cruiseConfig.findGroup(DEFAULT_GROUP).getFirst();
        StageConfig stage = pipeline.getFirst();
        StageConfigMother.addApprovalWithUsers(stage, "first");
        Approval approval = stage.getApproval();

        approval.validate(ConfigSaveValidationContext.forChain(cruiseConfig, group, pipeline, stage));

        assertNoErrors(approval.getAuthConfig().getUsers().getFirst());
    }

    @Test
    void validate_shouldAllowUserWhoIsDefinedInGroup() {
        CruiseConfig cruiseConfig = cruiseConfigWithSecurity(
                new RoleConfig(cis("role"), new RoleUser(cis("first")), new RoleUser(cis("second"))), new AdminUser(
                        cis("admin")));

        PipelineConfigs group = addUserAndRoleToDefaultGroup(cruiseConfig, "user", "role");
        PipelineConfig pipeline = cruiseConfig.findGroup(DEFAULT_GROUP).getFirst();
        StageConfig stage = pipeline.getFirst();
        StageConfigMother.addApprovalWithUsers(stage, "user");
        Approval approval = stage.getApproval();

        approval.validate(ConfigSaveValidationContext.forChain(cruiseConfig, group, pipeline, stage));

        assertNoErrors(approval.getAuthConfig().getUsers().getFirst());
    }

    @Test
    void validate_shouldAllowUserWhenSecurityIsNotDefinedInGroup() {
        CruiseConfig cruiseConfig = cruiseConfigWithSecurity(
                new RoleConfig(cis("role"), new RoleUser(cis("first")), new RoleUser(cis("second"))), new AdminUser(
                        cis("admin")));

        PipelineConfigs group = cruiseConfig.findGroup(DEFAULT_GROUP);
        PipelineConfig pipeline = cruiseConfig.findGroup(DEFAULT_GROUP).getFirst();
        StageConfig stage = pipeline.getFirst();
        StageConfigMother.addApprovalWithUsers(stage, "user");
        Approval approval = stage.getApproval();

        approval.validate(ConfigSaveValidationContext.forChain(cruiseConfig, group, pipeline, stage));

        assertNoErrors(approval.getAuthConfig().getUsers().getFirst());
    }

    @Test
    void validate_shouldAllowAdminToOperateOnAStage() {
        CruiseConfig cruiseConfig = cruiseConfigWithSecurity(
                new RoleConfig(cis("role"), new RoleUser(cis("first")), new RoleUser(cis("second"))), new AdminUser(
                        cis("admin")));

        PipelineConfigs group = addUserAndRoleToDefaultGroup(cruiseConfig, "user", "role");
        PipelineConfig pipeline = cruiseConfig.findGroup(DEFAULT_GROUP).getFirst();
        StageConfig stage = pipeline.getFirst();
        StageConfigMother.addApprovalWithUsers(stage, "admin");
        Approval approval = stage.getApproval();

        approval.validate(ConfigSaveValidationContext.forChain(cruiseConfig, group, pipeline, stage));

        assertNoErrors(approval.getAuthConfig().getUsers().getFirst());
    }

    @Test
    void shouldShowBugWhichAllowsAUserWithoutOperatePermissionToOperateAStage() {
        CruiseConfig cruiseConfig = cruiseConfigWithSecurity(
                new RoleConfig(cis("role"),
                        new RoleUser(cis("first")),
                        new RoleUser(cis("second"))),
                new AdminUser(cis("admin")));

        addRoleAsAdminToDefaultGroup(cruiseConfig, "role");

        PipelineConfig pipeline = cruiseConfig.findGroup(DEFAULT_GROUP).getFirst();
        StageConfig stage = pipeline.getFirst();
        StageConfigMother.addApprovalWithUsers(stage, "first", "some-other-user-who-is-not-operate-authorized");
        Approval approval = stage.getApproval();


        approval.validate(PipelineConfigSaveValidationContext.forChain(true, DEFAULT_GROUP, cruiseConfig, pipeline, stage));

        assertNoErrors(approval.getAuthConfig().getUsers().getFirst());
        /* https://github.com/gocd/gocd/pull/1779#issuecomment-170161521 */
        assertNoErrors(approval.getAuthConfig().getUsers().get(1));
    }

    @Test
    void validate_shouldNotTryAndValidateWhenWithinTemplate() {
        CruiseConfig cruiseConfig = cruiseConfigWithSecurity(
                new RoleConfig(cis("role"), new RoleUser(cis("first")), new RoleUser(cis("second"))), new AdminUser(
                        cis("admin")));

        addUserAndRoleToDefaultGroup(cruiseConfig, "user", "role");
        PipelineConfig pipeline = cruiseConfig.findGroup(DEFAULT_GROUP).getFirst();
        StageConfig stage = pipeline.getFirst();
        StageConfigMother.addApprovalWithUsers(stage, "not-present");
        Approval approval = stage.getApproval();

        approval.validate(ConfigSaveValidationContext.forChain(cruiseConfig, new TemplatesConfig(), stage));
        assertNoErrors(approval.getAuthConfig().getUsers().getFirst());
    }

    @Test
    void shouldValidateTree() {
        Approval approval = new Approval(new AuthConfig(new AdminRole(cis("role"))));
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.server().security().adminsConfig().addRole(new AdminRole(cis("super-admin")));
        PipelineConfig pipelineConfig = new PipelineConfig(cis("p1"), new MaterialConfigs());
        cruiseConfig.addPipeline("g1", pipelineConfig);

        assertThat(approval.validateTree(PipelineConfigSaveValidationContext.forChain(true, "g1", cruiseConfig, pipelineConfig))).isFalse();
        assertThat(approval.getAuthConfig().errors().isEmpty()).isFalse();
    }

    @Test
    void shouldSetAllowOnSuccessOnlyOnManualApproval() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("allowOnlyOnSuccess", "true");
        attributes.put(Approval.TYPE, TYPE_MANUAL);
        Approval approval = new Approval();
        approval.setConfigAttributes(attributes);

        assertThat(approval.getType()).isEqualTo(TYPE_MANUAL);
        assertThat(approval.isAllowOnlyOnSuccess()).isTrue();
    }

    @Test
    void shouldSetAllowOnSuccessOnlyOnSuccessApproval() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("allowOnlyOnSuccess", "true");
        attributes.put(Approval.TYPE, TYPE_SUCCESS);
        Approval approval = new Approval();
        approval.setConfigAttributes(attributes);

        assertThat(approval.getType()).isEqualTo(TYPE_SUCCESS);
        assertThat(approval.isAllowOnlyOnSuccess()).isTrue();
    }

    private CruiseConfig cruiseConfigWithSecurity(Role roleDefinition, Admin admins) {
        CruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("pipeline");
        SecurityConfig securityConfig = cruiseConfig.server().security();
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("file", "cd.go.authentication.passwordfile"));
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
        group.getAuthorization().getOperationConfig().add(new AdminRole(cis(role)));
    }

    private void addRoleAsAdminToDefaultGroup(CruiseConfig cruiseConfig, String role) {
        PipelineConfigs group = cruiseConfig.findGroup(DEFAULT_GROUP);
        group.getAuthorization().getAdminsConfig().add(new AdminRole(cis(role)));
    }

    private void addUserAsOperatorToDefaultGroup(CruiseConfig cruiseConfig, String user) {
        PipelineConfigs group = cruiseConfig.findGroup(DEFAULT_GROUP);
        group.getAuthorization().getOperationConfig().add(new AdminUser(cis(user)));
    }

    private Map<String, String> nameMap(final String name) {
        return Map.of("name", name);
    }

    private void assertNoErrors(Admin userOrRole) {
        assertThat(userOrRole.errors().isEmpty()).as(userOrRole.errors().getAll().toString()).isTrue();
    }
}
