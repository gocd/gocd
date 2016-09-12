/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import java.util.Map;

import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineTemplateConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.util.DataStructureUtils;
import org.hamcrest.Matchers;
import org.junit.Test;

import static com.thoughtworks.go.util.DataStructureUtils.a;
import static com.thoughtworks.go.util.DataStructureUtils.m;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class PipelineTemplateConfigTest {
    @Test
    public void shouldFindByName() {
        PipelineTemplateConfig pipelineTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("pipeline"), StageConfigMother.manualStage("manual"),
                StageConfigMother.manualStage("manual2"));
        assertThat(pipelineTemplateConfig.findBy(new CaseInsensitiveString("manuaL2")).name(), is(new CaseInsensitiveString("manual2")));
    }

    @Test
    public void shouldGetStageByName() {
        PipelineTemplateConfig pipelineTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("pipeline"), StageConfigMother.manualStage("manual"),
                StageConfigMother.manualStage("manual2"));
        assertThat(pipelineTemplateConfig.findBy(new CaseInsensitiveString("manual")).name(), is(new CaseInsensitiveString("manual")));
        assertThat(pipelineTemplateConfig.findBy(new CaseInsensitiveString("Does-not-exist")), is(nullValue()));
    }


    @Test
    public void shouldIgnoreCaseWhileMatchingATemplateWithName() {
        assertThat(new PipelineTemplateConfig(new CaseInsensitiveString("FOO")).matches(new CaseInsensitiveString("foo")), is(true));
        assertThat(new PipelineTemplateConfig(new CaseInsensitiveString("FOO")).matches(new CaseInsensitiveString("FOO")), is(true));
        assertThat(new PipelineTemplateConfig(new CaseInsensitiveString("FOO")).matches(new CaseInsensitiveString("bar")), is(false));
    }

    @Test
    public void shouldSetPrimitiveAttributes() {
        PipelineTemplateConfig pipelineTemplateConfig = new PipelineTemplateConfig();
        Map map = m(PipelineTemplateConfig.NAME, "templateName");

        pipelineTemplateConfig.setConfigAttributes(map);

        assertThat(pipelineTemplateConfig.name(), is(new CaseInsensitiveString("templateName")));
    }

    @Test
    public void shouldUpdateAuthorization() {
        PipelineTemplateConfig templateConfig = PipelineTemplateConfigMother.createTemplate("template-1");
        templateConfig.setConfigAttributes(m(BasicPipelineConfigs.AUTHORIZATION, a(
                DataStructureUtils.m(Authorization.NAME, "loser", Authorization.TYPE, Authorization.UserType.USER.toString(), Authorization.PRIVILEGES, a(DataStructureUtils.m(Authorization.PrivilegeType.ADMIN.toString(), Authorization.PrivilegeState.ON.toString()))),
                DataStructureUtils.m(Authorization.NAME, "boozer", Authorization.TYPE, Authorization.UserType.USER.toString(), Authorization.PRIVILEGES, a(DataStructureUtils.m(Authorization.PrivilegeType.ADMIN.toString(), Authorization.PrivilegeState.ON.toString()))),
                DataStructureUtils.m(Authorization.NAME, "geezer", Authorization.TYPE, Authorization.UserType.USER.toString(), Authorization.PRIVILEGES, a(DataStructureUtils.m(Authorization.PrivilegeType.ADMIN.toString(), Authorization.PrivilegeState.ON.toString()))))));
        Authorization authorization = templateConfig.getAuthorization();

        assertThat(authorization.getAdminsConfig().size(), Matchers.is(3));
        assertThat(authorization.getAdminsConfig(), hasItem(new AdminUser(new CaseInsensitiveString("loser"))));
        assertThat(authorization.getAdminsConfig(), hasItem(new AdminUser(new CaseInsensitiveString("boozer"))));
        assertThat(authorization.getAdminsConfig(), hasItem(new AdminUser(new CaseInsensitiveString("geezer"))));

        assertThat(authorization.getOperationConfig().size(), Matchers.is(0));
        assertThat(authorization.getViewConfig().size(), Matchers.is(0));
    }

    @Test
    public void shouldReInitializeAuthorizationIfWeClearAllPermissions() {
        PipelineTemplateConfig templateConfig = PipelineTemplateConfigMother.createTemplate("template-1");
        templateConfig.setConfigAttributes(m(BasicPipelineConfigs.AUTHORIZATION, a(
                DataStructureUtils.m(Authorization.NAME, "loser", Authorization.TYPE, Authorization.UserType.USER.toString(), Authorization.PRIVILEGES, a(DataStructureUtils.m(Authorization.PrivilegeType.ADMIN.toString(), Authorization.PrivilegeState.ON.toString()))),
                DataStructureUtils.m(Authorization.NAME, "boozer", Authorization.TYPE, Authorization.UserType.USER.toString(), Authorization.PRIVILEGES, a(DataStructureUtils.m(Authorization.PrivilegeType.ADMIN.toString(), Authorization.PrivilegeState.ON.toString()))),
                DataStructureUtils.m(Authorization.NAME, "geezer", Authorization.TYPE, Authorization.UserType.USER.toString(), Authorization.PRIVILEGES, a(DataStructureUtils.m(Authorization.PrivilegeType.ADMIN.toString(), Authorization.PrivilegeState.ON.toString()))))));
        Authorization authorization = templateConfig.getAuthorization();

        assertThat(authorization.getAdminsConfig().size(), Matchers.is(3));

        templateConfig.setConfigAttributes(m());

        authorization = templateConfig.getAuthorization();
        assertThat(authorization.getAdminsConfig().size(), Matchers.is(0));
    }

    @Test
    public void shouldIgnoreBlankUserWhileSettingAttributes() {
        PipelineTemplateConfig templateConfig = PipelineTemplateConfigMother.createTemplate("template-1");
        templateConfig.setConfigAttributes(m(BasicPipelineConfigs.AUTHORIZATION, a(
                DataStructureUtils.m(Authorization.NAME, "", Authorization.TYPE, Authorization.UserType.USER.toString(), Authorization.PRIVILEGES, a(DataStructureUtils.m(Authorization.PrivilegeType.ADMIN.toString(), Authorization.PrivilegeState.ON.toString()))),
                DataStructureUtils.m(Authorization.NAME, null, Authorization.TYPE, Authorization.UserType.USER.toString(), Authorization.PRIVILEGES, a(DataStructureUtils.m(Authorization.PrivilegeType.ADMIN.toString(), Authorization.PrivilegeState.ON.toString()))),
                DataStructureUtils.m(Authorization.NAME, "geezer", Authorization.TYPE, Authorization.UserType.USER.toString(), Authorization.PRIVILEGES, a(DataStructureUtils.m(Authorization.PrivilegeType.ADMIN.toString(), Authorization.PrivilegeState.ON.toString()))))));
        Authorization authorization = templateConfig.getAuthorization();

        assertThat(authorization.getAdminsConfig().size(), Matchers.is(1));
        assertThat(authorization.getAdminsConfig(), hasItem(new AdminUser(new CaseInsensitiveString("geezer"))));
    }

    @Test
    public void validate_shouldEnsureThatTemplateFollowsTheNameType() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        PipelineTemplateConfig config = new PipelineTemplateConfig(new CaseInsensitiveString(".Abc"));
        config.validate(ConfigSaveValidationContext.forChain(cruiseConfig));
        assertThat(config.errors().isEmpty(), is(false));
        assertThat(config.errors().on(PipelineTemplateConfig.NAME),
                is("Invalid template name '.Abc'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void shouldErrorOutWhenTryingToAddTwoStagesWithSameName() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        PipelineTemplateConfig pipelineTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.manualStage("stage1"),
                StageConfigMother.manualStage("stage1"));
        pipelineTemplateConfig.validate(ConfigSaveValidationContext.forChain(cruiseConfig));
        assertThat(pipelineTemplateConfig.get(0).errors().isEmpty(), is(false));
        assertThat(pipelineTemplateConfig.get(1).errors().isEmpty(), is(false));
        assertThat(pipelineTemplateConfig.get(0).errors().on(StageConfig.NAME), is("You have defined multiple stages called 'stage1'. Stage names are case-insensitive and must be unique."));
        assertThat(pipelineTemplateConfig.get(1).errors().on(StageConfig.NAME), is("You have defined multiple stages called 'stage1'. Stage names are case-insensitive and must be unique."));
    }

    @Test
    public void shouldUnderstandUsedParams() {
        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplateWithParams("template-name", "foo", "bar", "baz");
        ParamsConfig params = template.referredParams();
        assertThat(params.size(), is(3));
        assertThat(params, hasItem(new ParamConfig("foo", null)));
        assertThat(params, hasItem(new ParamConfig("bar", null)));
        assertThat(params, hasItem(new ParamConfig("baz", null)));
        params = template.referredParams();//should not mutate
        assertThat(params.size(), is(3));
    }

    @Test
    public void copyStagesShouldNotThrowExceptionIfInputPipelineConfigIsNull() {
        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplateWithParams("template-name", "foo", "bar", "baz");
        int sizeBeforeCopy = template.size();
        template.copyStages(null);
        assertThat(template.size(), is(sizeBeforeCopy));
    }

}
