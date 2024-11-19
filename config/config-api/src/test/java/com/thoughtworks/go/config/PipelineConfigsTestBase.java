/*
 * Copyright 2024 Thoughtworks, Inc.
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

import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.helper.PipelineConfigMother;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.config.Authorization.PrivilegeState.*;
import static com.thoughtworks.go.config.Authorization.UserType.ROLE;
import static com.thoughtworks.go.config.Authorization.UserType.USER;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class PipelineConfigsTestBase {

    protected abstract PipelineConfigs createWithPipeline(PipelineConfig pipelineConfig);

    protected abstract PipelineConfigs createEmpty();

    protected abstract PipelineConfigs createWithPipelines(PipelineConfig first, PipelineConfig second);

    @Test
    public void shouldReturnTrueIfPipelineExist() {
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1");
        PipelineConfigs configs = createWithPipeline(pipelineConfig);
        assertThat(configs.hasPipeline(new CaseInsensitiveString("pipeline1"))).isTrue();
    }

    @Test
    public void shouldReturnFalseIfPipelineNotExist() {
        PipelineConfigs configs = createWithPipeline(PipelineConfigMother.pipelineConfig("pipeline1"));
        assertThat(configs.hasPipeline(new CaseInsensitiveString("not-exist"))).isFalse();
    }

    @Test
    public void shouldUseDefaultViewPermissionsForViewersOfAGroupIfAuthorizationIsNotDefined() {
        assertThat(createEmpty().hasViewPermission(new CaseInsensitiveString("anyone"), null, true)).isTrue();
        assertThat(createEmpty().hasViewPermission(new CaseInsensitiveString("anyone"), null, false)).isFalse();
    }

    @Test
    public void shouldReturnFalseIfViewPermissionIsNotDefined() {
        PipelineConfigs group = createWithPipeline(PipelineConfigMother.pipelineConfig("pipeline1"));
        group.getAuthorization().getOperationConfig().add(new AdminUser(new CaseInsensitiveString("jez")));
        assertThat(group.hasViewPermission(new CaseInsensitiveString("jez"), null, true)).isFalse();
    }

    @Test
    public void shouldReturnFalseIfUserDoesNotHaveViewPermission() {
        PipelineConfigs group = createWithPipeline(PipelineConfigMother.pipelineConfig("pipeline1"));
        group.getAuthorization().getViewConfig().add(new AdminUser(new CaseInsensitiveString("jez")));
        assertThat(group.hasViewPermission(new CaseInsensitiveString("anyone"), null, true)).isFalse();
    }

    @Test
    public void shouldReturnTrueIfUserHasViewPermission() {
        PipelineConfigs group = createWithPipeline(PipelineConfigMother.pipelineConfig("pipeline1"));
        group.getAuthorization().getViewConfig().add(new AdminUser(new CaseInsensitiveString("jez")));
        assertThat(group.hasViewPermission(new CaseInsensitiveString("jez"), null, true)).isTrue();
    }

    @Test
    public void shouldReturnTrueForOperatePermissionIfAuthorizationIsNotDefined_AndDefaultPermissionForGroupsWithNoAuthIsToAllowAll() {
        assertThat(createEmpty().hasOperatePermission(new CaseInsensitiveString("anyone"), null, true)).isTrue();
    }

    @Test
    public void shouldReturnTrueForOperatePermissionIfAuthorizationIsNotDefined_AndDefaultPermissionForGroupsWithNoAuthIsToDeny() {
        assertThat(createEmpty().hasOperatePermission(new CaseInsensitiveString("anyone"), null, false)).isFalse();
    }

    @Test
    public void validate_shouldMakeSureTheNameIsAppropriate() {
        PipelineConfigs group = createEmpty();
        group.validate(null);
        assertThat(group.errors().on(BasicPipelineConfigs.GROUP)).isEqualTo("Invalid group name 'null'. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
    }

    @Test
    public void shouldErrorWhenAuthorizationIsDefinedInConfigRepo() {
        BasicPipelineConfigs group = new BasicPipelineConfigs(new RepoConfigOrigin());
        group.setGroup("gr");

        group.setConfigAttributes(Map.of(BasicPipelineConfigs.AUTHORIZATION, List.of(
            Map.of(Authorization.NAME, "loser", Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(ON, DISABLED, DISABLED)),
            Map.of(Authorization.NAME, "boozer", Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(OFF, ON, ON)),
            Map.of(Authorization.NAME, "geezer", Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(DISABLED, OFF, ON)),
            Map.of(Authorization.NAME, "gang_of_losers", Authorization.TYPE, ROLE.toString(), Authorization.PRIVILEGES, privileges(DISABLED, OFF, ON)),
            Map.of(Authorization.NAME, "blinds", Authorization.TYPE, ROLE.toString(), Authorization.PRIVILEGES, privileges(ON, ON, OFF)))));

        group.validate(null);

        assertThat(group.errors().on(BasicPipelineConfigs.NO_REMOTE_AUTHORIZATION)).isEqualTo("Authorization can be defined only in configuration file");
    }

    @Test
    public void shouldNotErrorWhenNoAuthorizationIsDefined_AndInConfigRepo() {
        BasicPipelineConfigs group = new BasicPipelineConfigs(new RepoConfigOrigin());
        group.setGroup("gr");

        group.validate(null);

        assertThat(group.errors().isEmpty()).isTrue();
    }

    @Test
    public void shouldNotErrorWhenAuthorizationIsDefinedLocally() {
        BasicPipelineConfigs group = new BasicPipelineConfigs(new FileConfigOrigin());
        group.setGroup("gr");
        group.setConfigAttributes(Map.of(BasicPipelineConfigs.AUTHORIZATION, List.of(
            Map.of(Authorization.NAME, "loser", Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(ON, DISABLED, DISABLED)),
            Map.of(Authorization.NAME, "boozer", Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(OFF, ON, ON)),
            Map.of(Authorization.NAME, "geezer", Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(DISABLED, OFF, ON)),
            Map.of(Authorization.NAME, "gang_of_losers", Authorization.TYPE, ROLE.toString(), Authorization.PRIVILEGES, privileges(DISABLED, OFF, ON)),
            Map.of(Authorization.NAME, "blinds", Authorization.TYPE, ROLE.toString(), Authorization.PRIVILEGES, privileges(ON, ON, OFF)))));

        group.validate(null);

        assertThat(group.errors().isEmpty()).isTrue();
    }

    @Test
    public void shouldValidateThatPipelineNameIsUnique() {
        PipelineConfig first = PipelineConfigMother.pipelineConfig("first");
        PipelineConfig second = PipelineConfigMother.pipelineConfig("second");
        PipelineConfigs group = createWithPipelines(first, second);
        PipelineConfig duplicate = PipelineConfigMother.pipelineConfig("first");
        group.addWithoutValidation(duplicate);

        group.validate(null);
        assertThat(duplicate.errors().on(PipelineConfig.NAME)).isEqualTo("You have defined multiple pipelines called 'first'. Pipeline names are case-insensitive and must be unique.");
        assertThat(first.errors().on(PipelineConfig.NAME)).isEqualTo("You have defined multiple pipelines called 'first'. Pipeline names are case-insensitive and must be unique.");

    }


    @Test
    public void shouldReturnFalseIfOperatePermissionIsNotDefined() {
        PipelineConfigs group = createWithPipeline(PipelineConfigMother.pipelineConfig("pipeline1"));
        group.getAuthorization().getViewConfig().add(new AdminUser(new CaseInsensitiveString("jez")));
        assertThat(group.hasOperatePermission(new CaseInsensitiveString("jez"), null, true)).isFalse();
    }

    @Test
    public void shouldReturnFalseIfUserDoesNotHaveOperatePermission() {
        PipelineConfigs group = createWithPipeline(PipelineConfigMother.pipelineConfig("pipeline1"));
        group.getAuthorization().getOperationConfig().add(new AdminUser(new CaseInsensitiveString("jez")));
        assertThat(group.hasOperatePermission(new CaseInsensitiveString("anyone"), null, true)).isFalse();
    }

    @Test
    public void shouldReturnTrueIfUserHasOperatePermission() {
        PipelineConfigs group = createWithPipeline(PipelineConfigMother.pipelineConfig("pipeline1"));
        group.getAuthorization().getOperationConfig().add(new AdminUser(new CaseInsensitiveString("jez")));
        assertThat(group.hasOperatePermission(new CaseInsensitiveString("jez"), null, true)).isTrue();
    }

    @Test
    public void hasViewPermissionDefinedShouldReturnTrueIfAuthorizationIsDefined() {
        PipelineConfigs group = createWithPipeline(PipelineConfigMother.pipelineConfig("pipeline1"));
        group.getAuthorization().getViewConfig().add(new AdminUser(new CaseInsensitiveString("jez")));
        assertThat(group.hasViewPermissionDefined()).isEqualTo(true);
    }

    @Test
    public void hasViewPermissionDefinedShouldReturnFalseIfAuthorizationIsNotDefined() {
        PipelineConfigs group = createWithPipeline(PipelineConfigMother.pipelineConfig("pipeline1"));
        assertThat(group.hasViewPermissionDefined()).isFalse();
    }

    @Test
    public void shouldReturnIndexOfPipeline() {
        PipelineConfigs group = createWithPipeline(PipelineConfigMother.pipelineConfig("pipeline1"));
        PipelineConfig pipelineConfig = (PipelineConfig) group.get(0).clone();
        pipelineConfig.setLabelTemplate("blah");
        group.update(group.getGroup(), pipelineConfig, "pipeline1");
        assertThat(group.get(0).getLabelTemplate()).isEqualTo("blah");
    }

    @Test
    public void shouldAddPipelineAtIndex() {
        PipelineConfigs group = createWithPipeline(PipelineConfigMother.pipelineConfig("pipeline0"));

        PipelineConfig p1 = PipelineConfigMother.pipelineConfig("pipeline1");
        group.add(1, p1);

        assertThat(group.get(1)).isEqualTo(p1);
    }

    @Test
    public void shouldRemovePipelineAtIndex() {
        PipelineConfigs group = createWithPipeline(PipelineConfigMother.pipelineConfig("pipeline0"));

        PipelineConfig p1 = PipelineConfigMother.pipelineConfig("pipeline1");
        group.add(1, p1);

        group.remove(0);

        assertThat(group.get(0)).isEqualTo(p1);
        assertThat(group.size()).isEqualTo(1);
    }

    @Test
    public void shouldRemovePipeline() {
        PipelineConfig p0 = PipelineConfigMother.pipelineConfig("pipeline0");
        PipelineConfigs group = createWithPipeline(p0);

        PipelineConfig p1 = PipelineConfigMother.pipelineConfig("pipeline1");
        group.add(1, p1);

        group.remove(p0);

        assertThat(group.get(0)).isEqualTo(p1);
        assertThat(group.size()).isEqualTo(1);
    }

    @Test
    public void shouldReturnIndexOfPipeline_When2Pipelines() {
        PipelineConfigs group = createWithPipelines(PipelineConfigMother.pipelineConfig("pipeline1"), PipelineConfigMother.pipelineConfig("pipeline2"));
        PipelineConfig pipelineConfig = group.findBy(new CaseInsensitiveString("pipeline2"));
        assertThat(group.indexOf(pipelineConfig)).isEqualTo(1);
    }


    @Test
    public void shouldUpdateAuthorization() {
        PipelineConfigs group = createWithPipeline(PipelineConfigMother.pipelineConfig("pipeline1"));
        group.setConfigAttributes(Map.of(BasicPipelineConfigs.AUTHORIZATION, List.of(
            Map.of(Authorization.NAME, "loser", Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(ON, DISABLED, DISABLED)),
            Map.of(Authorization.NAME, "boozer", Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(OFF, ON, ON)),
            Map.of(Authorization.NAME, "geezer", Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(DISABLED, OFF, ON)),
            Map.of(Authorization.NAME, "gang_of_losers", Authorization.TYPE, ROLE.toString(), Authorization.PRIVILEGES, privileges(DISABLED, OFF, ON)),
            Map.of(Authorization.NAME, "blinds", Authorization.TYPE, ROLE.toString(), Authorization.PRIVILEGES, privileges(ON, ON, OFF)))));
        Authorization authorization = group.getAuthorization();

        assertThat(authorization.getAdminsConfig().size()).isEqualTo(2);
        assertThat(authorization.getAdminsConfig()).contains(new AdminUser(new CaseInsensitiveString("loser")), new AdminRole(new CaseInsensitiveString("blinds")));

        assertThat(authorization.getOperationConfig().size()).isEqualTo(2);
        assertThat(authorization.getOperationConfig()).contains(new AdminUser(new CaseInsensitiveString("boozer")), new AdminRole(new CaseInsensitiveString("blinds")));

        assertThat(authorization.getViewConfig().size()).isEqualTo(3);
        assertThat(authorization.getViewConfig()).contains(new AdminUser(new CaseInsensitiveString("boozer")), new AdminUser(new CaseInsensitiveString("geezer")), new AdminRole(
            new CaseInsensitiveString("gang_of_losers")));
    }

    @Test
    public void shouldReInitializeAuthorizationIfWeClearAllPermissions() {
        PipelineConfigs group = createWithPipeline(PipelineConfigMother.pipelineConfig("pipeline1"));
        group.setConfigAttributes(Map.of(BasicPipelineConfigs.AUTHORIZATION, List.of(
            Map.of(Authorization.NAME, "loser", Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(ON, DISABLED, DISABLED)),
            Map.of(Authorization.NAME, "boozer", Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(OFF, ON, ON)),
            Map.of(Authorization.NAME, "geezer", Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(DISABLED, OFF, ON)),
            Map.of(Authorization.NAME, "gang_of_losers", Authorization.TYPE, ROLE.toString(), Authorization.PRIVILEGES, privileges(DISABLED, OFF, ON)),
            Map.of(Authorization.NAME, "blinds", Authorization.TYPE, ROLE.toString(), Authorization.PRIVILEGES, privileges(ON, ON, OFF)))));
        Authorization authorization = group.getAuthorization();

        assertThat(authorization.getAdminsConfig().size()).isEqualTo(2);
        assertThat(authorization.getOperationConfig().size()).isEqualTo(2);
        assertThat(authorization.getViewConfig().size()).isEqualTo(3);

        group.setConfigAttributes(Map.of());

        authorization = group.getAuthorization();

        assertThat(authorization.getAdminsConfig().size()).isEqualTo(0);
        assertThat(authorization.getOperationConfig().size()).isEqualTo(0);
        assertThat(authorization.getViewConfig().size()).isEqualTo(0);
    }

    @Test
    public void shouldIgnoreBlankUserOrRoleNames_whileSettingAttributes() {
        PipelineConfigs group = createWithPipeline(PipelineConfigMother.pipelineConfig("pipeline1"));
        group.setConfigAttributes(Map.of(BasicPipelineConfigs.AUTHORIZATION, List.of(
            Map.of(Authorization.NAME, "", Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(ON, DISABLED, DISABLED)),
            Map.of(Authorization.NAME, " ", Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(OFF, ON, ON)),
            Map.of(Authorization.NAME, "geezer", Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(DISABLED, OFF, ON)),
            Map.of(Authorization.NAME, "", Authorization.TYPE, ROLE.toString(), Authorization.PRIVILEGES, privileges(DISABLED, ON, ON)),
            Map.of(Authorization.NAME, " ", Authorization.TYPE, ROLE.toString(), Authorization.PRIVILEGES, privileges(ON, OFF, ON)),
            Map.of(Authorization.NAME, "blinds", Authorization.TYPE, ROLE.toString(), Authorization.PRIVILEGES, privileges(ON, ON, OFF)))));
        Authorization authorization = group.getAuthorization();

        assertThat(authorization.getAdminsConfig().size()).isEqualTo(1);
        assertThat(authorization.getAdminsConfig()).contains(new AdminRole(new CaseInsensitiveString("blinds")));

        assertThat(authorization.getOperationConfig().size()).isEqualTo(1);
        assertThat(authorization.getOperationConfig()).contains(new AdminRole(new CaseInsensitiveString("blinds")));

        assertThat(authorization.getViewConfig().size()).isEqualTo(1);
        assertThat(authorization.getViewConfig()).contains(new AdminUser(new CaseInsensitiveString("geezer")));
    }

    @Test
    public void shouldSetViewPermissionByDefaultIfNameIsPresentAndPermissionsAreOff_whileSettingAttributes() {
        PipelineConfigs group = createWithPipeline(PipelineConfigMother.pipelineConfig("pipeline1"));
        group.setConfigAttributes(Map.of(BasicPipelineConfigs.AUTHORIZATION, List.of(
            Map.of(Authorization.NAME, "user1", Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(OFF, OFF, OFF)),
            Map.of(Authorization.NAME, "role1", Authorization.TYPE, ROLE.toString(), Authorization.PRIVILEGES, privileges(OFF, OFF, OFF)))));
        Authorization authorization = group.getAuthorization();

        assertThat(authorization.getViewConfig().size()).isEqualTo(2);
        assertThat(authorization.getViewConfig()).contains(new AdminRole(new CaseInsensitiveString("role1")), new AdminUser(new CaseInsensitiveString("user1")));

        assertThat(authorization.getOperationConfig().size()).isEqualTo(0);
        assertThat(authorization.getAdminsConfig().size()).isEqualTo(0);
    }

    @Test
    public void shouldSetToDefaultGroupWithGroupNameIsEmptyString() {
        PipelineConfigs pipelineConfigs = createEmpty();
        pipelineConfigs.setGroup("");

        assertThat(pipelineConfigs.getGroup()).isEqualTo(BasicPipelineConfigs.DEFAULT_GROUP);
    }

    @Test
    public void shouldValidateGroupNameUniqueness() {
        Map<String, PipelineConfigs> nameToConfig = new HashMap<>();
        PipelineConfigs group1 = createEmpty();
        group1.setGroup("joe");
        PipelineConfigs group2 = createEmpty();
        group2.setGroup("joe");
        group1.validateNameUniqueness(nameToConfig);
        group2.validateNameUniqueness(nameToConfig);
        assertThat(group1.errors().on(PipelineConfigs.GROUP)).isEqualTo("Group with name 'joe' already exists");
    }

    @Test
    public void shouldGetAllPipelinesList() {
        PipelineConfig pipeline1 = PipelineConfigMother.pipelineConfig("pipeline1");
        PipelineConfig pipeline2 = PipelineConfigMother.pipelineConfig("pipeline2");
        PipelineConfigs group = createWithPipelines(pipeline1, pipeline2);
        assertThat(group.getPipelines()).contains(pipeline1);
        assertThat(group.getPipelines()).contains(pipeline2);
    }

    private List privileges(final Authorization.PrivilegeState admin, final Authorization.PrivilegeState operate, final Authorization.PrivilegeState view) {
        return List.of(Map.of(Authorization.PrivilegeType.ADMIN.toString(), admin.toString(),
            Authorization.PrivilegeType.OPERATE.toString(), operate.toString(),
            Authorization.PrivilegeType.VIEW.toString(), view.toString()));
    }

}
