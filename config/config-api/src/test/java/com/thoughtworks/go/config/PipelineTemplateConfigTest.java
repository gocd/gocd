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

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.PipelineTemplateConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.util.DataStructureUtils;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

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
    public void shouldValidateRoleNamesInTemplateAdminAuthorization() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        ServerConfig serverConfig = new ServerConfig(new SecurityConfig(new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")))), null);
        cruiseConfig.setServerConfig(serverConfig);
        GoConfigMother.enableSecurityWithPasswordFilePlugin(cruiseConfig);
        RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("non-existent-role"), new RoleUser("non-existent-user"));
        PipelineTemplateConfig template = new PipelineTemplateConfig(new CaseInsensitiveString("template"), new Authorization(new AdminsConfig(new AdminRole(roleConfig))), StageConfigMother.manualStage("stage2"),
                StageConfigMother.manualStage("stage"));

        template.validate(ConfigSaveValidationContext.forChain(cruiseConfig));

        assertThat(template.getAllErrors().get(0).getAllOn("name"), is(Arrays.asList("Role \"non-existent-role\" does not exist.")));
    }

    @Test
    public void shouldValidateRoleNamesInTemplateViewAuthorization() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        ServerConfig serverConfig = new ServerConfig(new SecurityConfig(new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")))), null);
        cruiseConfig.setServerConfig(serverConfig);
        GoConfigMother.enableSecurityWithPasswordFilePlugin(cruiseConfig);
        RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("non-existent-role"), new RoleUser("non-existent-user"));
        PipelineTemplateConfig template = new PipelineTemplateConfig(new CaseInsensitiveString("template"), new Authorization(new ViewConfig(new AdminRole(roleConfig))), StageConfigMother.manualStage("stage2"),
                StageConfigMother.manualStage("stage"));

        template.validate(ConfigSaveValidationContext.forChain(cruiseConfig));

        assertThat(template.getAllErrors().get(0).getAllOn("name"), is(Arrays.asList("Role \"non-existent-role\" does not exist.")));
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
    public void shouldValidateWhetherTheReferredParamsAreDefinedInPipelinesUsingTheTemplate() {
        PipelineTemplateConfig templateWithParams = PipelineTemplateConfigMother.createTemplateWithParams("template", "param1", "param2");
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfigWithTemplate("pipeline", "template");
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.addTemplate(templateWithParams);
        cruiseConfig.addPipelineWithoutValidation("group", pipelineConfig);

        templateWithParams.validateTree(ConfigSaveValidationContext.forChain(cruiseConfig), cruiseConfig, false);

        assertThat(templateWithParams.errors().getAllOn("params"), is(Arrays.asList("The param 'param1' is not defined in pipeline 'pipeline'", "The param 'param2' is not defined in pipeline 'pipeline'")));
    }

    @Test
    public void shouldValidateFetchTasksOfATemplateInTheContextOfPipelinesUsingTheTemplate() throws Exception {
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("defaultJob"));
        jobConfig.addTask(new FetchTask(new CaseInsensitiveString("non-existent-pipeline"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "src", "dest"));
        JobConfigs jobConfigs = new JobConfigs(jobConfig);
        StageConfig stageConfig = StageConfigMother.custom("stage", jobConfigs);
        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplate("template", stageConfig);
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfigWithTemplate("pipeline", "template");
        pipelineConfig.usingTemplate(template);
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.addTemplate(template);
        cruiseConfig.addPipelineWithoutValidation("group", pipelineConfig);

        template.validateTree(ConfigSaveValidationContext.forChain(cruiseConfig), cruiseConfig, false);

        assertThat(template.errors().getAllOn("pipeline"), is(Arrays.asList("\"pipeline :: stage :: defaultJob\" tries to fetch artifact from pipeline \"non-existent-pipeline\" which does not exist.")));
    }

    @Test
    public void shouldValidateTemplateStageUsedInDownstreamPipelines() {
        JobConfig jobConfigWithExecTask = new JobConfig(new CaseInsensitiveString("defaultJob"));
        jobConfigWithExecTask.addTask(new ExecTask("ls", "l", "server/config"));
        JobConfigs jobConfigs = new JobConfigs(jobConfigWithExecTask);

        StageConfig stageConfig = StageConfigMother.custom("stage", jobConfigs);
        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplate("template", stageConfig);

        PipelineConfig upstreamPipelineUsingTemplate = PipelineConfigMother.pipelineConfigWithTemplate("pipeline", "template");
        upstreamPipelineUsingTemplate.usingTemplate(template);

        //Pipeline and stage of upstreamPipelineUsingTemplate
        MaterialConfig dependency = new DependencyMaterialConfig(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("non-existent-stage"));
        PipelineConfig downStreamPipeline = PipelineConfigMother.pipelineConfig("downstreamPipeline", new MaterialConfigs(dependency));

        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.addTemplate(template);
        cruiseConfig.addPipelineWithoutValidation("group", upstreamPipelineUsingTemplate);
        cruiseConfig.addPipelineWithoutValidation("group", downStreamPipeline);

        template.validateTree(ConfigSaveValidationContext.forChain(cruiseConfig), cruiseConfig, false);

        assertThat(template.errors().getAllOn("base"), is(Arrays.asList("Stage with name 'non-existent-stage' does not exist on pipeline 'pipeline', it is being referred to from pipeline 'downstreamPipeline' (cruise-config.xml)")));
    }

    @Test
    public void shouldValidateTemplateJobsUsedInDownstreamPipelines() {
        JobConfig jobConfigWithExecTask = new JobConfig(new CaseInsensitiveString("defaultJob"));
        jobConfigWithExecTask.addTask(new ExecTask("ls", "l", "server/config"));
        JobConfigs jobConfigs = new JobConfigs(jobConfigWithExecTask);

        JobConfig jobConfigWithFetchTask = new JobConfig(new CaseInsensitiveString("fetchJob"));
        jobConfigWithFetchTask.addTask(new FetchTask(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("non-existent-job"), "src", "dest"));
        JobConfigs jobConfigsForDownstream = new JobConfigs(jobConfigWithFetchTask);

        StageConfig stageConfig = StageConfigMother.custom("stage", jobConfigs);
        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplate("template", stageConfig);

        PipelineConfig upstreamPipelineUsingTemplate = PipelineConfigMother.pipelineConfigWithTemplate("pipeline", "template");
        upstreamPipelineUsingTemplate.usingTemplate(template);

        //Pipeline and stage of upstreamPipelineUsingTemplate
        MaterialConfig dependency = new DependencyMaterialConfig(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("stage"));
        PipelineConfig downStreamPipeline = PipelineConfigMother.pipelineConfig("downstreamPipeline", new MaterialConfigs(dependency), jobConfigsForDownstream);

        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.addTemplate(template);
        cruiseConfig.addPipelineWithoutValidation("group", upstreamPipelineUsingTemplate);
        cruiseConfig.addPipelineWithoutValidation("group", downStreamPipeline);

        template.validateTree(ConfigSaveValidationContext.forChain(cruiseConfig), cruiseConfig, false);

        assertThat(template.errors().getAllOn("base"), is(Arrays.asList("\"downstreamPipeline :: mingle :: fetchJob\" tries to fetch artifact from job \"pipeline :: stage :: non-existent-job\" which does not exist.")));
    }

    @Test
    public void shouldAllowEditingOfStageNameWhenItIsNotUsedAsDependencyMaterial() throws Exception {
        PipelineTemplateConfig template = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage2"));
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.addTemplate(template);
        template.getStages().get(0).setName(new CaseInsensitiveString("updatedStageName"));

        template.validateTree(ConfigSaveValidationContext.forChain(cruiseConfig), cruiseConfig, false);

        assertThat(template.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldAllowEditingOfJobNameWhenItIsNotUsedAsFetchArtifact() throws Exception {
        PipelineTemplateConfig template = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job2"));
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.addTemplate(template);
        template.getStages().get(0).getJobs().get(0).setName(new CaseInsensitiveString("updatedJobName"));

        template.validateTree(ConfigSaveValidationContext.forChain(cruiseConfig), cruiseConfig, false);

        assertThat(template.errors().isEmpty(), is(true));
    }

    @Test
    public void copyStagesShouldNotThrowExceptionIfInputPipelineConfigIsNull() {
        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplateWithParams("template-name", "foo", "bar", "baz");
        int sizeBeforeCopy = template.size();
        template.copyStages(null);
        assertThat(template.size(), is(sizeBeforeCopy));
    }

}
