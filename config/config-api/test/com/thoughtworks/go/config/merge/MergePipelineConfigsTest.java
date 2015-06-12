package com.thoughtworks.go.config.merge;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.helper.PipelineConfigMother;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.List;

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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class MergePipelineConfigsTest {

    // 1 part cases, basically tests backward compatibility

    @Test
    public void shouldReturnTrueIfPipelineExist() {
        PipelineConfigs part = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        MergePipelineConfigs merge = new MergePipelineConfigs(part);
        assertThat("shouldReturnTrueIfPipelineExist", merge.hasPipeline(new CaseInsensitiveString("pipeline1")), is(true));
    }

    @Test
    public void shouldReturnFalseIfPipelineNotExist() {
        PipelineConfigs part1 = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        MergePipelineConfigs merge = new MergePipelineConfigs(part1);
        assertThat("shouldReturnFalseIfPipelineNotExist", merge.hasPipeline(new CaseInsensitiveString("not-exist")), is(false));
    }
    @Test
    public void shouldSetAuthorizationInFile() {
        BasicPipelineConfigs filePart = new BasicPipelineConfigs();
        filePart.setOrigin(new FileConfigOrigin());

        MergePipelineConfigs merge = new MergePipelineConfigs(filePart,new BasicPipelineConfigs());

        Authorization auth = new Authorization(new AdminsConfig(new AdminUser(new CaseInsensitiveString("buddy"))));
        merge.setAuthorization(auth);
        assertThat(filePart.getAuthorization(),is(auth));
    }

    @Test
    public void shouldReturnTrueIfAuthorizationIsNotDefined() {
        BasicPipelineConfigs filePart = new BasicPipelineConfigs();
        filePart.setOrigin(new FileConfigOrigin());

        MergePipelineConfigs merge = new MergePipelineConfigs(filePart,new BasicPipelineConfigs());
        assertThat(merge.hasViewPermission(new CaseInsensitiveString("anyone"), null), is(true));
    }

    @Test
    public void shouldReturnFalseIfViewPermissionIsNotDefined() {
        BasicPipelineConfigs filePart = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        filePart.setOrigin(new FileConfigOrigin());

        PipelineConfigs group = new MergePipelineConfigs(filePart);
        group.getAuthorization().getOperationConfig().add(new AdminUser(new CaseInsensitiveString("jez")));
        assertThat(group.hasViewPermission(new CaseInsensitiveString("jez"), null), is(false));
    }

    @Test
    public void shouldReturnFalseIfUserDoesNotHaveViewPermission() {
        BasicPipelineConfigs filePart = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        filePart.setOrigin(new FileConfigOrigin());

        PipelineConfigs group = new MergePipelineConfigs(filePart);
        group.getAuthorization().getViewConfig().add(new AdminUser(new CaseInsensitiveString("jez")));
        assertThat(group.hasViewPermission(new CaseInsensitiveString("anyone"), null), is(false));
    }

    @Test
    public void shouldReturnTrueIfUserHasViewPermission() {
        BasicPipelineConfigs filePart = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        filePart.setOrigin(new FileConfigOrigin());

        PipelineConfigs group = new MergePipelineConfigs(filePart);
        group.getAuthorization().getViewConfig().add(new AdminUser(new CaseInsensitiveString("jez")));
        assertThat(group.hasViewPermission(new CaseInsensitiveString("jez"), null), is(true));
    }

    @Test
    public void shouldReturnTrueForOperatePermissionIfAuthorizationIsNotDefined() {
        BasicPipelineConfigs filePart = new BasicPipelineConfigs();
        filePart.setOrigin(new FileConfigOrigin());

        assertThat(new MergePipelineConfigs(filePart).hasOperatePermission(new CaseInsensitiveString("anyone"), null), is(true));
    }

    @Test
    public void validate_shouldMakeSureTheNameIsAppropriate() {
        PipelineConfigs group = new MergePipelineConfigs(new BasicPipelineConfigs());
        group.validate(null);
        assertThat(group.errors().on(BasicPipelineConfigs.GROUP),
                is("Invalid group name 'null'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void shouldValidateThatPipelineNameIsUnique() {
        PipelineConfig first = PipelineConfigMother.pipelineConfig("first");
        BasicPipelineConfigs part = new BasicPipelineConfigs(first, PipelineConfigMother.pipelineConfig("second"));
        PipelineConfigs group = new MergePipelineConfigs(part);
        PipelineConfig duplicate = PipelineConfigMother.pipelineConfig("first");
        part.addWithoutValidation(duplicate);

        group.validate(null);
        assertThat(duplicate.errors().on(PipelineConfig.NAME), is("You have defined multiple pipelines called 'first'. Pipeline names are case-insensitive and must be unique."));
        assertThat(first.errors().on(PipelineConfig.NAME), is("You have defined multiple pipelines called 'first'. Pipeline names are case-insensitive and must be unique."));

    }
    @Test
    public void shouldReturnFalseIfOperatePermissionIsNotDefined() {
        BasicPipelineConfigs filePart = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        filePart.setOrigin(new FileConfigOrigin());
        PipelineConfigs group = new MergePipelineConfigs(filePart);
        group.getAuthorization().getViewConfig().add(new AdminUser(new CaseInsensitiveString("jez")));
        assertThat(group.hasOperatePermission(new CaseInsensitiveString("jez"), null), is(false));
    }

    @Test
    public void shouldReturnFalseIfUserDoesNotHaveOperatePermission() {
        BasicPipelineConfigs filePart = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        filePart.setOrigin(new FileConfigOrigin());
        PipelineConfigs group = new MergePipelineConfigs(filePart);
        group.getAuthorization().getOperationConfig().add(new AdminUser(new CaseInsensitiveString("jez")));
        assertThat(group.hasOperatePermission(new CaseInsensitiveString("anyone"), null), is(false));
    }

    @Test
    public void shouldReturnTrueIfUserHasOperatePermission() {
        BasicPipelineConfigs filePart = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        filePart.setOrigin(new FileConfigOrigin());

        PipelineConfigs group = new MergePipelineConfigs(filePart);
        group.getAuthorization().getOperationConfig().add(new AdminUser(new CaseInsensitiveString("jez")));
        assertThat(group.hasOperatePermission(new CaseInsensitiveString("jez"), null), is(true));
    }

    @Test
    public void hasViewPermissionDefinedShouldReturnTrueIfAuthorizationIsDefined() {
        BasicPipelineConfigs filePart = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        filePart.setOrigin(new FileConfigOrigin());
        PipelineConfigs group = new MergePipelineConfigs(filePart);
        group.getAuthorization().getViewConfig().add(new AdminUser(new CaseInsensitiveString("jez")));
        assertThat("hasViewPermissionDefinedShouldReturnTrueIfAuthorizationIsDefined", group.hasViewPermissionDefined(),
                is(true));
    }

    @Test
    public void hasViewPermissionDefinedShouldReturnFalseIfAuthorizationIsNotDefined() {
        BasicPipelineConfigs filePart = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline2"));
        filePart.setOrigin(new FileConfigOrigin());

        PipelineConfigs group = new MergePipelineConfigs(new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1")),filePart);
        assertThat("hasViewPermissionDefinedShouldReturnFalseIfAuthorizationIsNotDefined",
                group.hasViewPermissionDefined(), is(false));
    }

    //TODO updates

    @Test
    public void shouldReturnIndexOfPipeline() {
        PipelineConfigs group = new MergePipelineConfigs(new BasicPipelineConfigs(
                PipelineConfigMother.pipelineConfig("pipeline1"),PipelineConfigMother.pipelineConfig("pipeline2")));
        PipelineConfig pipelineConfig = group.findBy(new CaseInsensitiveString("pipeline2"));
        assertThat(group.indexOf(pipelineConfig),is(1));
    }
    @Test
    public void shouldApplyChangesToPipelineWhenPartEditable() {
        BasicPipelineConfigs filePart = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        filePart.setOrigin(new FileConfigOrigin());
        PipelineConfigs group = new MergePipelineConfigs(filePart);
        PipelineConfig pipelineConfig = (PipelineConfig) group.get(0).clone();
        pipelineConfig.setLabelTemplate("blah");
        group.update(group.getGroup(), pipelineConfig, "pipeline1");
        assertThat(group.get(0).getLabelTemplate(), is("blah"));
    }

    @Test(expected = RuntimeException.class)
    public void shouldUpdateName() {
        PipelineConfigs group = new MergePipelineConfigs(new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1")));
        group.setConfigAttributes(m(BasicPipelineConfigs.GROUP, "my-new-group"));
        assertThat(group.getGroup(), is("my-new-group"));

        group.setConfigAttributes(m());
        assertThat(group.getGroup(), is("my-new-group"));

        group.setConfigAttributes(null);
        assertThat(group.getGroup(), is("my-new-group"));

        group.setConfigAttributes(m(BasicPipelineConfigs.GROUP, null));
        assertThat(group.getGroup(), is(nullValue()));
    }

    //TODO this must always work. At least in xml authorization can be defined.
    @Test
    public void shouldUpdateAuthorization() {
        BasicPipelineConfigs filePart = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        filePart.setOrigin(new FileConfigOrigin());

        PipelineConfigs group = new MergePipelineConfigs(filePart,new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline2")));
        group.setConfigAttributes(m(BasicPipelineConfigs.AUTHORIZATION, a(
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
        BasicPipelineConfigs filePart = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        filePart.setOrigin(new FileConfigOrigin());

        PipelineConfigs group = new MergePipelineConfigs(filePart,new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline2")));
        group.setConfigAttributes(m(BasicPipelineConfigs.AUTHORIZATION, a(
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
        BasicPipelineConfigs filePart = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        filePart.setOrigin(new FileConfigOrigin());

        PipelineConfigs group = new MergePipelineConfigs(filePart);
        group.setConfigAttributes(m(BasicPipelineConfigs.AUTHORIZATION, a(
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
        BasicPipelineConfigs filePart = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline2"));
        filePart.setOrigin(new FileConfigOrigin());

        PipelineConfigs group = new MergePipelineConfigs(new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1")),filePart);
        group.setConfigAttributes(m(BasicPipelineConfigs.AUTHORIZATION, a(
                m(Authorization.NAME, "user1", Authorization.TYPE, USER.toString(), Authorization.PRIVILEGES, privileges(OFF, OFF, OFF)),
                m(Authorization.NAME, "role1", Authorization.TYPE, ROLE.toString(), Authorization.PRIVILEGES, privileges(OFF, OFF, OFF)))));
        Authorization authorization = group.getAuthorization();

        assertThat(authorization.getViewConfig().size(), is(2));
        assertThat(authorization.getViewConfig(), hasItems((Admin) new AdminRole(new CaseInsensitiveString("role1")), (Admin) new AdminUser(new CaseInsensitiveString("user1"))));

        assertThat(authorization.getOperationConfig().size(), is(0));
        assertThat(authorization.getAdminsConfig().size(), is(0));
    }

    @Test(expected = RuntimeException.class)
    public void shouldSetToDefaultGroupWithGroupNameIsEmptyString() {
        PipelineConfigs pipelineConfigs = new MergePipelineConfigs(new BasicPipelineConfigs());
        pipelineConfigs.setGroup("");

        assertThat(pipelineConfigs.getGroup(), is(BasicPipelineConfigs.DEFAULT_GROUP));
    }

    private List privileges(final Authorization.PrivilegeState admin, final Authorization.PrivilegeState operate, final Authorization.PrivilegeState view) {
        return a(m(Authorization.PrivilegeType.ADMIN.toString(), admin.toString(),
                Authorization.PrivilegeType.OPERATE.toString(), operate.toString(),
                Authorization.PrivilegeType.VIEW.toString(), view.toString()));
    }

    // 2 parts and more cases

    @Test
    public void shouldReturnTrueIfPipelineExistIn2Parts() {
        PipelineConfigs part1 = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        PipelineConfigs part2 = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline2"));
        MergePipelineConfigs merge = new MergePipelineConfigs(part1,part2);
        assertThat("shouldReturnTrueIfPipelineExist", merge.hasPipeline(new CaseInsensitiveString("pipeline1")), is(true));
        assertThat("shouldReturnTrueIfPipelineExist", merge.hasPipeline(new CaseInsensitiveString("pipeline2")), is(true));
    }


    @Test
    public void shouldReturnFalseIfPipelineNotExistIn2Parts() {
        PipelineConfigs part1 = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        PipelineConfigs part2 = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline2"));
        MergePipelineConfigs merge = new MergePipelineConfigs(part2);
        assertThat("shouldReturnFalseIfPipelineNotExist", merge.hasPipeline(new CaseInsensitiveString("not-exist")), is(false));
    }

    @Test
    public void shouldReturnTrueIfAuthorizationIsNotDefinedIn2Parts() {
        BasicPipelineConfigs filePart = new BasicPipelineConfigs();
        filePart.setOrigin(new FileConfigOrigin());

        MergePipelineConfigs merge = new MergePipelineConfigs(new BasicPipelineConfigs(), filePart);
        assertThat(merge.hasViewPermission(new CaseInsensitiveString("anyone"), null), is(true));
    }
    @Test
    public void shouldReturnAuthorizationFromFileIfDefinedIn2Parts() {
        BasicPipelineConfigs part1 = new BasicPipelineConfigs();
        Authorization fileAuth = new Authorization();
        part1.setAuthorization(fileAuth);
        part1.setOrigin(new FileConfigOrigin());

        BasicPipelineConfigs part2 = new BasicPipelineConfigs();
        part2.setAuthorization(new Authorization());
        MergePipelineConfigs merge = new MergePipelineConfigs(part1,part2);

        assertThat(merge.getAuthorization(),is(fileAuth));
    }

    @Test
    public void shouldReturnFalseIfViewPermissionIsNotDefinedIn2Parts() {
        BasicPipelineConfigs filePart = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline3"));
        filePart.setOrigin(new FileConfigOrigin());

        PipelineConfigs group = new MergePipelineConfigs(
                new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1")),
                new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline2")),filePart);
        group.getAuthorization().getOperationConfig().add(new AdminUser(new CaseInsensitiveString("jez")));
        assertThat(group.hasViewPermission(new CaseInsensitiveString("jez"), null), is(false));
    }

    @Test
    public void shouldReturnTrueForOperatePermissionIfAuthorizationIsNotDefinedIn2Parts() {
        BasicPipelineConfigs filePart = new BasicPipelineConfigs();
        filePart.setOrigin(new FileConfigOrigin());

        assertThat(new MergePipelineConfigs(filePart, new BasicPipelineConfigs())
                .hasOperatePermission(new CaseInsensitiveString("anyone"), null), is(true));
    }

    @Test
    public void validate_shouldMakeSureTheNameIsAppropriateIn2Parts() {
        PipelineConfigs group = new MergePipelineConfigs(new BasicPipelineConfigs(),new BasicPipelineConfigs());
        group.validate(null);
        assertThat(group.errors().on(BasicPipelineConfigs.GROUP),
                is("Invalid group name 'null'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnPartsWithDifferentGroupNames(){
        PipelineConfigs group = new MergePipelineConfigs(new BasicPipelineConfigs("one",null),new BasicPipelineConfigs("two",null));
    }

    @Test
    public void shouldValidateThatPipelineNameIsUniqueIn2Parts() {
        PipelineConfig first = PipelineConfigMother.pipelineConfig("first");
        PipelineConfig duplicate = PipelineConfigMother.pipelineConfig("first");
        PipelineConfigs group = new MergePipelineConfigs(
                new BasicPipelineConfigs(first, PipelineConfigMother.pipelineConfig("second")),
                new BasicPipelineConfigs(duplicate, PipelineConfigMother.pipelineConfig("third")));

        group.validate(null);
        assertThat(duplicate.errors().on(PipelineConfig.NAME), is("You have defined multiple pipelines called 'first'. Pipeline names are case-insensitive and must be unique."));
        assertThat(first.errors().on(PipelineConfig.NAME), is("You have defined multiple pipelines called 'first'. Pipeline names are case-insensitive and must be unique."));

    }

    @Test
    public void shouldReturnSizeSummedFrom2Parts(){
        PipelineConfigs group = new MergePipelineConfigs(
                new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1")),
                new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline2")));
        assertThat(group.size(),is(2));
    }
    @Test
    public void shouldReturnTrueWhenAllPartsEmpty(){
        PipelineConfigs group = new MergePipelineConfigs(
                new BasicPipelineConfigs(),
                new BasicPipelineConfigs());
        assertThat(group.isEmpty(),is(true));
    }
    @Test
    public void shouldReturnFalseSomePartIsNotEmpty(){
        PipelineConfigs group = new MergePipelineConfigs(
                new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1")),
                new BasicPipelineConfigs());
        assertThat(group.isEmpty(),is(false));
    }
    @Test
    public  void  shouldReturnTrueWhenContainsPipeline() {
        PipelineConfig pipe1 = PipelineConfigMother.pipelineConfig("pipeline1");
        PipelineConfigs group = new MergePipelineConfigs(
                new BasicPipelineConfigs(pipe1),
                new BasicPipelineConfigs());
        assertThat(group.contains(pipe1),is(true));
    }
    @Test
    public  void  shouldReturnFalseWhenDoesNotContainPipeline() {
        PipelineConfig pipe1 = PipelineConfigMother.pipelineConfig("pipeline1");
        PipelineConfigs group = new MergePipelineConfigs(
                new BasicPipelineConfigs(pipe1),
                new BasicPipelineConfigs());
        assertThat(group.contains(PipelineConfigMother.pipelineConfig("pipeline2")),is(false));
    }

    @Test
    public void shouldReturnPipelinesInOrder(){
        PipelineConfig pipeline1 = PipelineConfigMother.pipelineConfig("pipeline1");
        PipelineConfig pipeline3 = PipelineConfigMother.pipelineConfig("pipeline3");
        PipelineConfig pipeline5 = PipelineConfigMother.pipelineConfig("pipeline5");
        PipelineConfig pipeline2 = PipelineConfigMother.pipelineConfig("pipeline2");
        PipelineConfig pipeline4 = PipelineConfigMother.pipelineConfig("pipeline4");
        PipelineConfigs group = new MergePipelineConfigs(
                new BasicPipelineConfigs(pipeline1, pipeline2),
                new BasicPipelineConfigs(pipeline3),
                new BasicPipelineConfigs(pipeline4, pipeline5));

        assertThat(group.get(0),is(pipeline1));
        assertThat(group.get(1),is(pipeline2));
        assertThat(group.get(2),is(pipeline3));
        assertThat(group.get(3),is(pipeline4));
        assertThat(group.get(4),is(pipeline5));
    }

    @Test
    public  void  shouldReturnFirstEditablePartWhenExists(){
        PipelineConfig pipe1 = PipelineConfigMother.pipelineConfig("pipeline1");
        BasicPipelineConfigs part1 = new BasicPipelineConfigs(pipe1);
        part1.setOrigin(new FileConfigOrigin());

        MergePipelineConfigs group = new MergePipelineConfigs(
                part1, new BasicPipelineConfigs());

        assertThat(group.getFirstEditablePart(), Matchers.<PipelineConfigs>is(part1));

    }

    @Test
    public  void  shouldReturnNullWhenFirstEditablePartNotExists(){
        PipelineConfig pipe1 = PipelineConfigMother.pipelineConfig("pipeline1");
        BasicPipelineConfigs part1 = new BasicPipelineConfigs(pipe1);
        MergePipelineConfigs group = new MergePipelineConfigs(
                part1, new BasicPipelineConfigs());

        assertNull(group.getFirstEditablePart());

    }

    @Test
    public  void  shouldReturnPartWithPipelineWhenExists(){
        PipelineConfig pipe1 = PipelineConfigMother.pipelineConfig("pipeline1");
        BasicPipelineConfigs part1 = new BasicPipelineConfigs(pipe1);
        part1.setOrigin(new FileConfigOrigin());

        MergePipelineConfigs group = new MergePipelineConfigs(
                part1, new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline2")));

        assertThat(group.getPartWithPipeline(new CaseInsensitiveString("pipeline1")), Matchers.<PipelineConfigs>is(part1));

    }

    @Test
    public  void  shouldReturnNullPartWithPipelineNotExists(){
        PipelineConfig pipe1 = PipelineConfigMother.pipelineConfig("pipeline1");
        BasicPipelineConfigs part1 = new BasicPipelineConfigs(pipe1);
        MergePipelineConfigs group = new MergePipelineConfigs(
                part1, new BasicPipelineConfigs());

        assertNull(group.getPartWithPipeline(new CaseInsensitiveString("pipelineX")));

    }


    @Test
    public void shouldAddPipelineToFirstEditablePartWhenExists(){
        PipelineConfig pipe1 = PipelineConfigMother.pipelineConfig("pipeline1");
        BasicPipelineConfigs part1 = new BasicPipelineConfigs(pipe1);
        part1.setOrigin(new FileConfigOrigin());

        MergePipelineConfigs group = new MergePipelineConfigs(
                part1, new BasicPipelineConfigs());

        PipelineConfig pipeline2 = PipelineConfigMother.pipelineConfig("pipeline2");
        group.add(pipeline2);

        assertThat(group.contains(pipeline2),is(true));
    }

    @Test
    public void shouldBombWhenAddPipelineAndNoEditablePartExists(){
        PipelineConfig pipe1 = PipelineConfigMother.pipelineConfig("pipeline1");
        BasicPipelineConfigs part1 = new BasicPipelineConfigs(pipe1);

        MergePipelineConfigs group = new MergePipelineConfigs(
                part1, new BasicPipelineConfigs());

        PipelineConfig pipeline2 = PipelineConfigMother.pipelineConfig("pipeline2");
        try {
            group.add(pipeline2);
        }
        catch (Exception ex)
        {
            assertThat(ex.getMessage(),is("No editable configuration sources"));
            return;
        }

        fail("exception not thrown");
    }
}
