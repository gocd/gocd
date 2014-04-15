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

package com.thoughtworks.go.domain;

import java.util.List;

import com.thoughtworks.go.config.AdminRole;
import com.thoughtworks.go.config.AdminUser;
import com.thoughtworks.go.config.Authorization;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.helper.PipelineConfigMother;
import org.junit.Test;

import static com.thoughtworks.go.config.Authorization.PrivilegeState.DISABLED;
import static com.thoughtworks.go.config.Authorization.PrivilegeState.OFF;
import static com.thoughtworks.go.config.Authorization.PrivilegeState.ON;
import static com.thoughtworks.go.config.Authorization.UserType.ROLE;
import static com.thoughtworks.go.config.Authorization.UserType.USER;
import static com.thoughtworks.go.util.DataStructureUtils.a;
import static com.thoughtworks.go.util.DataStructureUtils.m;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class PipelineConfigsTest {

    @Test
    public void shouldReturnTrueIfPipelineExist() {
        PipelineConfigs configs = new PipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        assertThat("shouldReturnTrueIfPipelineExist", configs.hasPipeline(new CaseInsensitiveString("pipeline1")), is(true));
    }

    @Test
    public void shouldReturnFalseIfPipelineNotExist() {
        PipelineConfigs configs = new PipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        assertThat("shouldReturnFalseIfPipelineNotExist", configs.hasPipeline(new CaseInsensitiveString("not-exist")), is(false));
    }

    @Test
    public void shouldReturnTrueIfAuthorizationIsNotDefined() {
        assertThat(new PipelineConfigs().hasViewPermission(new CaseInsensitiveString("anyone"), null), is(true));
    }

    @Test
    public void shouldReturnFalseIfViewPermissionIsNotDefined() {
        PipelineConfigs group = new PipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        group.getAuthorization().getOperationConfig().add(new AdminUser(new CaseInsensitiveString("jez")));
        assertThat(group.hasViewPermission(new CaseInsensitiveString("jez"), null), is(false));
    }

    @Test
    public void shouldReturnFalseIfUserDoesNotHaveViewPermission() {
        PipelineConfigs group = new PipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        group.getAuthorization().getViewConfig().add(new AdminUser(new CaseInsensitiveString("jez")));
        assertThat(group.hasViewPermission(new CaseInsensitiveString("anyone"), null), is(false));
    }

    @Test
    public void shouldReturnTrueIfUserHasViewPermission() {
        PipelineConfigs group = new PipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        group.getAuthorization().getViewConfig().add(new AdminUser(new CaseInsensitiveString("jez")));
        assertThat(group.hasViewPermission(new CaseInsensitiveString("jez"), null), is(true));
    }

    @Test
    public void shouldReturnTrueForOperatePermissionIfAuthorizationIsNotDefined() {
        assertThat(new PipelineConfigs().hasOperatePermission(new CaseInsensitiveString("anyone"), null), is(true));
    }

    @Test
    public void validate_shouldMakeSureTheNameIsAppropriate() {
        PipelineConfigs group = new PipelineConfigs();
        group.validate(null);
        assertThat(group.errors().on(PipelineConfigs.GROUP), is("Invalid group name 'null'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void shouldValidateThatPipelineNameIsUnique() {
        PipelineConfig first = PipelineConfigMother.pipelineConfig("first");
        PipelineConfigs group = new PipelineConfigs(first, PipelineConfigMother.pipelineConfig("second"));
        PipelineConfig duplicate = PipelineConfigMother.pipelineConfig("first");
        group.addWithoutValidation(duplicate);

        group.validate(null);
        assertThat(duplicate.errors().on(PipelineConfig.NAME), is("You have defined multiple pipelines called 'first'. Pipeline names are case-insensitive and must be unique."));
        assertThat(first.errors().on(PipelineConfig.NAME), is("You have defined multiple pipelines called 'first'. Pipeline names are case-insensitive and must be unique."));

    }
    @Test
    public void shouldReturnFalseIfOperatePermissionIsNotDefined() {
        PipelineConfigs group = new PipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        group.getAuthorization().getViewConfig().add(new AdminUser(new CaseInsensitiveString("jez")));
        assertThat(group.hasOperatePermission(new CaseInsensitiveString("jez"), null), is(false));
    }

    @Test
    public void shouldReturnFalseIfUserDoesNotHaveOperatePermission() {
        PipelineConfigs group = new PipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        group.getAuthorization().getOperationConfig().add(new AdminUser(new CaseInsensitiveString("jez")));
        assertThat(group.hasOperatePermission(new CaseInsensitiveString("anyone"), null), is(false));
    }

    @Test
    public void shouldReturnTrueIfUserHasOperatePermission() {
        PipelineConfigs group = new PipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        group.getAuthorization().getOperationConfig().add(new AdminUser(new CaseInsensitiveString("jez")));
        assertThat(group.hasOperatePermission(new CaseInsensitiveString("jez"), null), is(true));
    }

    @Test
    public void hasViewPermissionDefinedShouldReturnTrueIfAuthorizationIsDefined() {
        PipelineConfigs group = new PipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        group.getAuthorization().getViewConfig().add(new AdminUser(new CaseInsensitiveString("jez")));
        assertThat("hasViewPermissionDefinedShouldReturnTrueIfAuthorizationIsDefined", group.hasViewPermissionDefined(),
                is(true));
    }

    @Test
    public void hasViewPermissionDefinedShouldReturnFalseIfAuthorizationIsNotDefined() {
        PipelineConfigs group = new PipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        assertThat("hasViewPermissionDefinedShouldReturnFalseIfAuthorizationIsNotDefined",
                group.hasViewPermissionDefined(), is(false));
    }

    @Test
    public void shouldReturnIndexOfPipeline() {
        PipelineConfigs group = new PipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        PipelineConfig pipelineConfig = (PipelineConfig) group.first().clone();
        pipelineConfig.setLabelTemplate("blah");
        group.update(group.getGroup(), pipelineConfig, "pipeline1");
        assertThat(group.first().getLabelTemplate(), is("blah"));
    }

    @Test
    public void shouldUpdateName() {
        PipelineConfigs group = new PipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        group.setConfigAttributes(m(PipelineConfigs.GROUP, "my-new-group"));
        assertThat(group.getGroup(), is("my-new-group"));

        group.setConfigAttributes(m());
        assertThat(group.getGroup(), is("my-new-group"));

        group.setConfigAttributes(null);
        assertThat(group.getGroup(), is("my-new-group"));

        group.setConfigAttributes(m(PipelineConfigs.GROUP, null));
        assertThat(group.getGroup(), is(nullValue()));
    }

    @Test
    public void shouldUpdateAuthorization() {
        PipelineConfigs group = new PipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        group.setConfigAttributes(m(PipelineConfigs.AUTHORIZATION, a(
                m(Authorization.NAME, "loser",          Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(ON, DISABLED, DISABLED)),
                m(Authorization.NAME, "boozer",         Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(OFF, ON, ON)),
                m(Authorization.NAME, "geezer",         Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(DISABLED, OFF, ON)),
                m(Authorization.NAME, "gang_of_losers", Authorization.TYPE, ROLE.toString(), Authorization.PRIVILEGES, privileges(DISABLED, OFF, ON)),
                m(Authorization.NAME, "blinds",         Authorization.TYPE, ROLE.toString(), Authorization.PRIVILEGES, privileges(ON, ON, OFF)))));
        Authorization authorization = group.getAuthorization();

        assertThat(authorization.getAdminsConfig().size(), is(2));
        assertThat(authorization.getAdminsConfig(), hasItems(new AdminUser(new CaseInsensitiveString("loser")), new AdminRole(new CaseInsensitiveString("blinds"))));

        assertThat(authorization.getOperationConfig().size(), is(2));
        assertThat(authorization.getOperationConfig(), hasItems(new AdminUser(new CaseInsensitiveString("boozer")), new AdminRole(new CaseInsensitiveString("blinds"))));

        assertThat(authorization.getViewConfig().size(), is(3));
        assertThat(authorization.getViewConfig(), hasItems(new AdminUser(new CaseInsensitiveString("boozer")), new AdminUser(new CaseInsensitiveString("geezer")), new AdminRole(
                new CaseInsensitiveString("gang_of_losers"))));
    }

    @Test
    public void shouldReInitializeAuthorizationIfWeClearAllPermissions() {
        PipelineConfigs group = new PipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        group.setConfigAttributes(m(PipelineConfigs.AUTHORIZATION, a(
                m(Authorization.NAME, "loser",          Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(ON, DISABLED, DISABLED)),
                m(Authorization.NAME, "boozer",         Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(OFF, ON, ON)),
                m(Authorization.NAME, "geezer",         Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(DISABLED, OFF, ON)),
                m(Authorization.NAME, "gang_of_losers", Authorization.TYPE, ROLE.toString(), Authorization.PRIVILEGES, privileges(DISABLED, OFF, ON)),
                m(Authorization.NAME, "blinds",         Authorization.TYPE, ROLE.toString(), Authorization.PRIVILEGES, privileges(ON, ON, OFF)))));
        Authorization authorization = group.getAuthorization();

        assertThat(authorization.getAdminsConfig().size(), is(2));
        assertThat(authorization.getOperationConfig().size(), is(2));
        assertThat(authorization.getViewConfig().size(), is(3));

        group.setConfigAttributes(m());

        authorization = group.getAuthorization();

        assertThat(authorization.getAdminsConfig().size(), is(0));
        assertThat(authorization.getOperationConfig().size(), is(0));
        assertThat(authorization.getViewConfig().size(), is(0));
    }

    @Test
    public void shouldIgnoreBlankUserOrRoleNames_whileSettingAttributes() {
        PipelineConfigs group = new PipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        group.setConfigAttributes(m(PipelineConfigs.AUTHORIZATION, a(
                m(Authorization.NAME, "",          Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(ON, DISABLED, DISABLED)),
                m(Authorization.NAME, null,         Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(OFF, ON, ON)),
                m(Authorization.NAME, "geezer",         Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(DISABLED, OFF, ON)),
                m(Authorization.NAME, "", Authorization.TYPE, ROLE.toString(), Authorization.PRIVILEGES, privileges(DISABLED, ON, ON)),
                m(Authorization.NAME, null, Authorization.TYPE, ROLE.toString(), Authorization.PRIVILEGES, privileges(ON, OFF, ON)),
                m(Authorization.NAME, "blinds",         Authorization.TYPE, ROLE.toString(), Authorization.PRIVILEGES, privileges(ON, ON, OFF)))));
        Authorization authorization = group.getAuthorization();

        assertThat(authorization.getAdminsConfig().size(), is(1));
        assertThat(authorization.getAdminsConfig(), hasItem((Admin) new AdminRole(new CaseInsensitiveString("blinds"))));

        assertThat(authorization.getOperationConfig().size(), is(1));
        assertThat(authorization.getOperationConfig(), hasItem((Admin) new AdminRole(new CaseInsensitiveString("blinds"))));

        assertThat(authorization.getViewConfig().size(), is(1));
        assertThat(authorization.getViewConfig(), hasItem((Admin) new AdminUser(new CaseInsensitiveString("geezer"))));
    }

    @Test
    public void shouldSetViewPermissionByDefaultIfNameIsPresentAndPermissionsAreOff_whileSettingAttributes() {
        PipelineConfigs group = new PipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        group.setConfigAttributes(m(PipelineConfigs.AUTHORIZATION, a(
                m(Authorization.NAME, "user1", Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(OFF, OFF, OFF)),
                m(Authorization.NAME, "role1", Authorization.TYPE, ROLE.toString(), Authorization.PRIVILEGES, privileges(OFF, OFF, OFF)))));
        Authorization authorization = group.getAuthorization();

        assertThat(authorization.getViewConfig().size(), is(2));
        assertThat(authorization.getViewConfig(), hasItems((Admin) new AdminRole(new CaseInsensitiveString("role1")), (Admin) new AdminUser(new CaseInsensitiveString("user1"))));

        assertThat(authorization.getOperationConfig().size(), is(0));
        assertThat(authorization.getAdminsConfig().size(), is(0));
    }

    @Test
    public void shouldSetToDefaultGroupWithGroupNameIsEmptyString() {
        PipelineConfigs pipelineConfigs = new PipelineConfigs();
        pipelineConfigs.setGroup("");

        assertThat(pipelineConfigs.getGroup(), is(PipelineConfigs.DEFAULT_GROUP));
    }

    private List privileges(final Authorization.PrivilegeState admin, final Authorization.PrivilegeState operate, final Authorization.PrivilegeState view) {
        return a(m(Authorization.PrivilegeType.ADMIN.toString(), admin.toString(),
                Authorization.PrivilegeType.OPERATE.toString(), operate.toString(),
                Authorization.PrivilegeType.VIEW.toString(), view.toString()));
    }

}
