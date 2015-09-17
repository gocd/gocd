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
package com.thoughtworks.go.config.merge;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.helper.PipelineConfigMother;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.util.DataStructureUtils.a;
import static com.thoughtworks.go.util.DataStructureUtils.m;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;

public class MergePipelineConfigsTest extends PipelineConfigsTestBase {

    @Override
    protected PipelineConfigs createWithPipeline(PipelineConfig pipelineConfig) {
        BasicPipelineConfigs pipelineConfigsLocal = new BasicPipelineConfigs(pipelineConfig);
        pipelineConfigsLocal.setOrigin(new FileConfigOrigin());
        BasicPipelineConfigs pipelineConfigsRemote = new BasicPipelineConfigs();
        pipelineConfigsRemote.setOrigin(new RepoConfigOrigin());
        return new MergePipelineConfigs(pipelineConfigsRemote,pipelineConfigsLocal);
    }

    @Override
    protected PipelineConfigs createEmpty() {
        BasicPipelineConfigs pipelineConfigs = new BasicPipelineConfigs();
        pipelineConfigs.setOrigin(new FileConfigOrigin());
        return new MergePipelineConfigs(pipelineConfigs);
    }

    @Override
    protected PipelineConfigs createWithPipelines(PipelineConfig first, PipelineConfig second) {
        BasicPipelineConfigs pipelineConfigsLocal = new BasicPipelineConfigs(first, second);
        pipelineConfigsLocal.setOrigin(new FileConfigOrigin());
        BasicPipelineConfigs pipelineConfigsRemote = new BasicPipelineConfigs();
        pipelineConfigsRemote.setOrigin(new RepoConfigOrigin());
        return new MergePipelineConfigs(pipelineConfigsLocal,pipelineConfigsRemote);
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
    public void shouldValidateNameUniqueness_When2ConfigParts()
    {
        PipelineConfig first = PipelineConfigMother.pipelineConfig("first");
        PipelineConfig duplicate = PipelineConfigMother.pipelineConfig("first");
        PipelineConfigs group = new MergePipelineConfigs(
                new BasicPipelineConfigs(first, PipelineConfigMother.pipelineConfig("second")),
                new BasicPipelineConfigs(duplicate, PipelineConfigMother.pipelineConfig("third")));

        Map<String, PipelineConfigs> nameToConfig = new HashMap<String, PipelineConfigs>();
        List<PipelineConfigs> visited = new ArrayList();

        group.validateNameUniqueness(nameToConfig);

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

    @Test
    public void shouldFailToAddPipelineAtIndex_WhenWouldLandInNonEditablePart() {
        PipelineConfig pipeline0 = PipelineConfigMother.pipelineConfig("pipeline0");
        PipelineConfig pipeline1 = PipelineConfigMother.pipelineConfig("pipeline1");
        PipelineConfig pipeline3 = PipelineConfigMother.pipelineConfig("pipeline3");
        PipelineConfig pipeline5 = PipelineConfigMother.pipelineConfig("pipeline5");
        PipelineConfig pipeline2 = PipelineConfigMother.pipelineConfig("pipeline2");
        PipelineConfig pipeline4 = PipelineConfigMother.pipelineConfig("pipeline4");

        BasicPipelineConfigs pipelineConfigsMiddle = new BasicPipelineConfigs(pipeline3);
        pipelineConfigsMiddle.setOrigin(new FileConfigOrigin());

        BasicPipelineConfigs bottom = new BasicPipelineConfigs(pipeline0, pipeline1, pipeline2);
        BasicPipelineConfigs top = new BasicPipelineConfigs(pipeline4, pipeline5);
        bottom.setOrigin(new RepoConfigOrigin());
        top.setOrigin(new RepoConfigOrigin());

        PipelineConfigs group = new MergePipelineConfigs(
                bottom,
                pipelineConfigsMiddle,
                top);

        PipelineConfig p1 = PipelineConfigMother.pipelineConfig("pipelineToInsert");

        tryAddAndAssertThatFailed(group, p1, 0);
        tryAddAndAssertThatFailed(group, p1, 1);
        tryAddAndAssertThatFailed(group, p1, 2);

        tryAddAndAssertThatFailed(group, p1, 5);
        tryAddAndAssertThatFailed(group, p1, 4);
    }

    private void tryAddAndAssertThatFailed(PipelineConfigs group, PipelineConfig p1, int index) {
        try {
            group.add(index,p1);
        }
        catch (Exception ex)
        {
            assertThat(ex.getMessage(),is("Cannot add pipeline to non-editable configuration part"));
            return;
        }
        fail(String.format("should have thrown when adding at %s",index));
    }

    @Test
    public void shouldAddPipelineAtIndex_WhenWouldLandInEditablePart() {
        PipelineConfig pipeline0 = PipelineConfigMother.pipelineConfig("pipeline0");
        PipelineConfig pipeline1 = PipelineConfigMother.pipelineConfig("pipeline1");
        PipelineConfig pipeline3 = PipelineConfigMother.pipelineConfig("pipeline3");
        PipelineConfig pipeline5 = PipelineConfigMother.pipelineConfig("pipeline5");
        PipelineConfig pipeline2 = PipelineConfigMother.pipelineConfig("pipeline2");
        PipelineConfig pipeline4 = PipelineConfigMother.pipelineConfig("pipeline4");

        BasicPipelineConfigs pipelineConfigsMiddle = new BasicPipelineConfigs(pipeline3);
        pipelineConfigsMiddle.setOrigin(new FileConfigOrigin());

        BasicPipelineConfigs bottom = new BasicPipelineConfigs(pipeline0, pipeline1, pipeline2);
        BasicPipelineConfigs top = new BasicPipelineConfigs(pipeline4, pipeline5);
        bottom.setOrigin(new RepoConfigOrigin());
        top.setOrigin(new RepoConfigOrigin());

        PipelineConfigs group = new MergePipelineConfigs(
                bottom,
                pipelineConfigsMiddle,
                top);

        PipelineConfig p1 = PipelineConfigMother.pipelineConfig("pipelineToInsert");

        group.add(3,p1);
        assertThat(group, hasItem(p1));
        assertThat(pipelineConfigsMiddle, hasItem(p1));
    }
}
