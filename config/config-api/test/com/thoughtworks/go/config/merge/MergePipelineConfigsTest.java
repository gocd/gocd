package com.thoughtworks.go.config.merge;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.helper.PipelineConfigMother;
import org.hamcrest.Matchers;
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
import static org.junit.Assert.*;

public class MergePipelineConfigsTest extends PipelineConfigsBaseTest  {

    @Override
    protected PipelineConfigs createWithPipeline(PipelineConfig pipelineConfig) {
        BasicPipelineConfigs pipelineConfigsLocal = new BasicPipelineConfigs(pipelineConfig);
        pipelineConfigsLocal.setOrigin(new FileConfigOrigin());
        BasicPipelineConfigs pipelineConfigsRemote = new BasicPipelineConfigs();
        pipelineConfigsRemote.setOrigin(new RepoConfigOrigin());
        return new MergePipelineConfigs(pipelineConfigsLocal,pipelineConfigsRemote);
    }

    @Override
    protected PipelineConfigs createEmpty() {
        BasicPipelineConfigs pipelineConfigs = new BasicPipelineConfigs();
        pipelineConfigs.setOrigin(new FileConfigOrigin());
        return new MergePipelineConfigs(pipelineConfigs);
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
    public void shouldAddToFirstEditableWhenAddToTop()
    {
        BasicPipelineConfigs filePart = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline2"));
        filePart.setOrigin(new FileConfigOrigin());

        PipelineConfigs group = new MergePipelineConfigs(new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1")),filePart);
        group.addToTop(PipelineConfigMother.pipelineConfig("pipeline3"));

        assertThat(filePart.hasPipeline(new CaseInsensitiveString("pipeline3")),is(true));
        assertThat(group.hasPipeline(new CaseInsensitiveString("pipeline3")),is(true));
    }

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
    public void shouldFailToUpdateName() {
        PipelineConfigs group = new MergePipelineConfigs(
                new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1")),
                new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline2")));
        group.setConfigAttributes(m(BasicPipelineConfigs.GROUP, "my-new-group"));
        assertThat(group.getGroup(), is("my-new-group"));
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
    public void shouldReturnTrueIfPipelineExist_When2ConfigParts() {
        PipelineConfigs part1 = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        PipelineConfigs part2 = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline2"));
        MergePipelineConfigs merge = new MergePipelineConfigs(part1,part2);
        assertThat("shouldReturnTrueIfPipelineExist", merge.hasPipeline(new CaseInsensitiveString("pipeline1")), is(true));
        assertThat("shouldReturnTrueIfPipelineExist", merge.hasPipeline(new CaseInsensitiveString("pipeline2")), is(true));
    }


    @Test
    public void shouldReturnFalseIfPipelineNotExist_When2ConfigParts() {
        PipelineConfigs part1 = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"));
        PipelineConfigs part2 = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline2"));
        MergePipelineConfigs merge = new MergePipelineConfigs(part2);
        assertThat("shouldReturnFalseIfPipelineNotExist", merge.hasPipeline(new CaseInsensitiveString("not-exist")), is(false));
    }

    @Test
    public void shouldReturnTrueIfAuthorizationIsNotDefined_When2ConfigParts() {
        BasicPipelineConfigs filePart = new BasicPipelineConfigs();
        filePart.setOrigin(new FileConfigOrigin());

        MergePipelineConfigs merge = new MergePipelineConfigs(new BasicPipelineConfigs(), filePart);
        assertThat(merge.hasViewPermission(new CaseInsensitiveString("anyone"), null), is(true));
    }
    @Test
    public void shouldReturnAuthorizationFromFileIfDefined_When2ConfigParts() {
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
    public void shouldReturnFalseIfViewPermissionIsNotDefined_When2ConfigParts() {
        BasicPipelineConfigs filePart = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline3"));
        filePart.setOrigin(new FileConfigOrigin());

        PipelineConfigs group = new MergePipelineConfigs(
                new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1")),
                new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline2")),filePart);
        group.getAuthorization().getOperationConfig().add(new AdminUser(new CaseInsensitiveString("jez")));
        assertThat(group.hasViewPermission(new CaseInsensitiveString("jez"), null), is(false));
    }

    @Test
    public void shouldReturnTrueForOperatePermissionIfAuthorizationIsNotDefined_When2ConfigParts() {
        BasicPipelineConfigs filePart = new BasicPipelineConfigs();
        filePart.setOrigin(new FileConfigOrigin());

        assertThat(new MergePipelineConfigs(filePart, new BasicPipelineConfigs())
                .hasOperatePermission(new CaseInsensitiveString("anyone"), null), is(true));
    }

    @Test
    public void validate_shouldMakeSureTheNameIsAppropriate_When2ConfigParts() {
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
    public void shouldValidateThatPipelineNameIsUnique_When2ConfigParts() {
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
    public void shouldReturnSizeSummedFrom2ConfigParts(){
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

        assertThat(group.getFirstEditablePartOrNull(), Matchers.<PipelineConfigs>is(part1));

    }

    @Test
    public  void  shouldReturnNullWhenFirstEditablePartNotExists(){
        PipelineConfig pipe1 = PipelineConfigMother.pipelineConfig("pipeline1");
        BasicPipelineConfigs part1 = new BasicPipelineConfigs(pipe1);
        MergePipelineConfigs group = new MergePipelineConfigs(
                part1, new BasicPipelineConfigs());

        assertNull(group.getFirstEditablePartOrNull());

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
