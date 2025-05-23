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
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.PipelineTemplateConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PipelineTemplateConfigTest {
    @Test
    public void shouldFindByName() {
        PipelineTemplateConfig pipelineTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("pipeline"), StageConfigMother.manualStage("manual"),
                StageConfigMother.manualStage("manual2"));
        assertThat(pipelineTemplateConfig.findBy(new CaseInsensitiveString("manuaL2")).name()).isEqualTo(new CaseInsensitiveString("manual2"));
    }

    @Test
    public void shouldGetStageByName() {
        PipelineTemplateConfig pipelineTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("pipeline"), StageConfigMother.manualStage("manual"),
                StageConfigMother.manualStage("manual2"));
        assertThat(pipelineTemplateConfig.findBy(new CaseInsensitiveString("manual")).name()).isEqualTo(new CaseInsensitiveString("manual"));
        assertThat(pipelineTemplateConfig.findBy(new CaseInsensitiveString("Does-not-exist"))).isNull();
    }


    @Test
    public void shouldIgnoreCaseWhileMatchingATemplateWithName() {
        assertThat(new PipelineTemplateConfig(new CaseInsensitiveString("FOO")).matches(new CaseInsensitiveString("foo"))).isTrue();
        assertThat(new PipelineTemplateConfig(new CaseInsensitiveString("FOO")).matches(new CaseInsensitiveString("FOO"))).isTrue();
        assertThat(new PipelineTemplateConfig(new CaseInsensitiveString("FOO")).matches(new CaseInsensitiveString("bar"))).isFalse();
    }

    @Test
    public void shouldSetPrimitiveAttributes() {
        PipelineTemplateConfig pipelineTemplateConfig = new PipelineTemplateConfig();
        Map<String, String> map = Map.of(PipelineTemplateConfig.NAME, "templateName");

        pipelineTemplateConfig.setConfigAttributes(map);

        assertThat(pipelineTemplateConfig.name()).isEqualTo(new CaseInsensitiveString("templateName"));
    }

    @Test
    public void shouldUpdateAuthorization() {
        PipelineTemplateConfig templateConfig = PipelineTemplateConfigMother.createTemplate("template-1");
        templateConfig.setConfigAttributes(Map.of(BasicPipelineConfigs.AUTHORIZATION, List.of(
                Map.of(Authorization.NAME, "loser", Authorization.TYPE, Authorization.UserType.USER.toString(), Authorization.PRIVILEGES, List.of(Map.of(Authorization.PrivilegeType.ADMIN.toString(), Authorization.PrivilegeState.ON.toString()))),
                Map.of(Authorization.NAME, "boozer", Authorization.TYPE, Authorization.UserType.USER.toString(), Authorization.PRIVILEGES, List.of(Map.of(Authorization.PrivilegeType.ADMIN.toString(), Authorization.PrivilegeState.ON.toString()))),
                Map.of(Authorization.NAME, "geezer", Authorization.TYPE, Authorization.UserType.USER.toString(), Authorization.PRIVILEGES, List.of(Map.of(Authorization.PrivilegeType.ADMIN.toString(), Authorization.PrivilegeState.ON.toString()))))));
        Authorization authorization = templateConfig.getAuthorization();

        assertThat(authorization.getAdminsConfig().size()).isEqualTo((3));
        assertThat(authorization.getAdminsConfig()).contains(new AdminUser(new CaseInsensitiveString("loser")));
        assertThat(authorization.getAdminsConfig()).contains(new AdminUser(new CaseInsensitiveString("boozer")));
        assertThat(authorization.getAdminsConfig()).contains(new AdminUser(new CaseInsensitiveString("geezer")));

        assertThat(authorization.getOperationConfig().size()).isEqualTo(0);
        assertThat(authorization.getViewConfig().size()).isEqualTo(0);
    }

    @Test
    public void shouldReInitializeAuthorizationIfWeClearAllPermissions() {
        PipelineTemplateConfig templateConfig = PipelineTemplateConfigMother.createTemplate("template-1");
        templateConfig.setConfigAttributes(Map.of(BasicPipelineConfigs.AUTHORIZATION, List.of(
                Map.of(Authorization.NAME, "loser", Authorization.TYPE, Authorization.UserType.USER.toString(), Authorization.PRIVILEGES, List.of(Map.of(Authorization.PrivilegeType.ADMIN.toString(), Authorization.PrivilegeState.ON.toString()))),
                Map.of(Authorization.NAME, "boozer", Authorization.TYPE, Authorization.UserType.USER.toString(), Authorization.PRIVILEGES, List.of(Map.of(Authorization.PrivilegeType.ADMIN.toString(), Authorization.PrivilegeState.ON.toString()))),
                Map.of(Authorization.NAME, "geezer", Authorization.TYPE, Authorization.UserType.USER.toString(), Authorization.PRIVILEGES, List.of(Map.of(Authorization.PrivilegeType.ADMIN.toString(), Authorization.PrivilegeState.ON.toString()))))));
        Authorization authorization = templateConfig.getAuthorization();

        assertThat(authorization.getAdminsConfig().size()).isEqualTo((3));

        templateConfig.setConfigAttributes(Map.of());

        authorization = templateConfig.getAuthorization();
        assertThat(authorization.getAdminsConfig().size()).isEqualTo(0);
    }

    @Test
    public void shouldIgnoreBlankUserWhileSettingAttributes() {
        PipelineTemplateConfig templateConfig = PipelineTemplateConfigMother.createTemplate("template-1");
        templateConfig.setConfigAttributes(Map.of(BasicPipelineConfigs.AUTHORIZATION, List.of(
                Map.of(Authorization.NAME, "", Authorization.TYPE, Authorization.UserType.USER.toString(), Authorization.PRIVILEGES, List.of(Map.of(Authorization.PrivilegeType.ADMIN.toString(), Authorization.PrivilegeState.ON.toString()))),
                Map.of(Authorization.NAME, " ", Authorization.TYPE, Authorization.UserType.USER.toString(), Authorization.PRIVILEGES, List.of(Map.of(Authorization.PrivilegeType.ADMIN.toString(), Authorization.PrivilegeState.ON.toString()))),
                Map.of(Authorization.NAME, "geezer", Authorization.TYPE, Authorization.UserType.USER.toString(), Authorization.PRIVILEGES, List.of(Map.of(Authorization.PrivilegeType.ADMIN.toString(), Authorization.PrivilegeState.ON.toString()))))));
        Authorization authorization = templateConfig.getAuthorization();

        assertThat(authorization.getAdminsConfig().size()).isEqualTo((1));
        assertThat(authorization.getAdminsConfig()).contains(new AdminUser(new CaseInsensitiveString("geezer")));
    }

    @Test
    public void validate_shouldEnsureThatTemplateFollowsTheNameType() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        PipelineTemplateConfig config = new PipelineTemplateConfig(new CaseInsensitiveString(".Abc"));
        config.validate(ConfigSaveValidationContext.forChain(cruiseConfig));
        assertThat(config.errors().isEmpty()).isFalse();
        assertThat(config.errors().on(PipelineTemplateConfig.NAME)).isEqualTo("Invalid template name '.Abc'. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
    }

    @Test
    public void shouldErrorOutWhenTryingToAddTwoStagesWithSameName() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        PipelineTemplateConfig pipelineTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.manualStage("stage1"),
                StageConfigMother.manualStage("stage1"));
        pipelineTemplateConfig.validate(ConfigSaveValidationContext.forChain(cruiseConfig));
        assertThat(pipelineTemplateConfig.get(0).errors().isEmpty()).isFalse();
        assertThat(pipelineTemplateConfig.get(1).errors().isEmpty()).isFalse();
        assertThat(pipelineTemplateConfig.get(0).errors().on(StageConfig.NAME)).isEqualTo("You have defined multiple stages called 'stage1'. Stage names are case-insensitive and must be unique.");
        assertThat(pipelineTemplateConfig.get(1).errors().on(StageConfig.NAME)).isEqualTo("You have defined multiple stages called 'stage1'. Stage names are case-insensitive and must be unique.");
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

        assertThat(template.getAllErrors().get(0).getAllOn("name")).isEqualTo(List.of("Role \"non-existent-role\" does not exist."));
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

        assertThat(template.getAllErrors().get(0).getAllOn("name")).isEqualTo(List.of("Role \"non-existent-role\" does not exist."));
    }

    @Test
    public void shouldUnderstandUsedParams() {
        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplateWithParams("template-name", "foo", "bar", "baz");
        ParamsConfig params = template.referredParams();
        assertThat(params.size()).isEqualTo(3);
        assertThat(params).contains(new ParamConfig("foo", null));
        assertThat(params).contains(new ParamConfig("bar", null));
        assertThat(params).contains(new ParamConfig("baz", null));
        params = template.referredParams();//should not mutate
        assertThat(params.size()).isEqualTo(3);
    }

    @Test
    public void shouldValidateWhetherTheReferredParamsAreDefinedInPipelinesUsingTheTemplate() {
        PipelineTemplateConfig templateWithParams = PipelineTemplateConfigMother.createTemplateWithParams("template", "param1", "param2");
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfigWithTemplate("pipeline", "template");
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.addTemplate(templateWithParams);
        cruiseConfig.addPipelineWithoutValidation("group", pipelineConfig);

        templateWithParams.validateTree(ConfigSaveValidationContext.forChain(cruiseConfig), cruiseConfig, false);

        assertThat(templateWithParams.errors().getAllOn("params")).isEqualTo(List.of("The param 'param1' is not defined in pipeline 'pipeline'", "The param 'param2' is not defined in pipeline 'pipeline'"));
    }

    @Test
    public void shouldValidateFetchTasksOfATemplateInTheContextOfPipelinesUsingTheTemplate() {
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

        assertThat(template.errors().getAllOn("pipeline")).isEqualTo(List.of("\"pipeline :: stage :: defaultJob\" tries to fetch artifact from pipeline \"non-existent-pipeline\" which does not exist."));
    }


    @Test
    public void shouldValidateFetchPluggableTasksOfATemplateInTheContextOfPipelinesUsingTheTemplate() {
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("defaultJob"));
        jobConfig.addTask(new FetchPluggableArtifactTask(new CaseInsensitiveString("non-existent-pipeline"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "artifactId"));
        JobConfigs jobConfigs = new JobConfigs(jobConfig);
        StageConfig stageConfig = StageConfigMother.custom("stage", jobConfigs);
        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplate("template", stageConfig);
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfigWithTemplate("pipeline", "template");
        pipelineConfig.usingTemplate(template);
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.addTemplate(template);
        cruiseConfig.addPipelineWithoutValidation("group", pipelineConfig);

        template.validateTree(ConfigSaveValidationContext.forChain(cruiseConfig), cruiseConfig, false);

        assertThat(template.errors().getAllOn("pipeline")).isEqualTo(List.of("\"pipeline :: stage :: defaultJob\" tries to fetch artifact from pipeline \"non-existent-pipeline\" which does not exist."));
    }

    @Test
    public void shouldValidatePublishExternalArtifactOfATemplateInTheContextOfPipelinesUsingTheTemplate() {
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("defaultJob"));
        jobConfig.artifactTypeConfigs().add(new PluggableArtifactConfig("some-id", "non-existent-store-id"));
        JobConfigs jobConfigs = new JobConfigs(jobConfig);
        StageConfig stageConfig = StageConfigMother.custom("stage", jobConfigs);
        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplate("template", stageConfig);
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfigWithTemplate("pipeline", "template");
        pipelineConfig.usingTemplate(template);
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.addTemplate(template);
        cruiseConfig.addPipelineWithoutValidation("group", pipelineConfig);

        template.validateTree(ConfigSaveValidationContext.forChain(cruiseConfig), cruiseConfig, false);

        assertThat(template.errors().getAllOn("storeId")).isEqualTo(List.of("Artifact store with id `non-existent-store-id` does not exist. Please correct the `storeId` attribute on pipeline `pipeline`."));
    }

    @Test
    public void shouldValidateStageApprovalAuthorizationOfATemplateInTheContextOfPipelinesUsingTheTemplate() {
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("defaultJob"));
        JobConfigs jobConfigs = new JobConfigs(jobConfig);
        StageConfig stageConfig = StageConfigMother.custom("stage", jobConfigs);
        stageConfig.setApproval(new Approval(new AuthConfig(new AdminRole(new CaseInsensitiveString("non-existent-role")))));
        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplate("template", stageConfig);
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfigWithTemplate("pipeline", "template");
        pipelineConfig.usingTemplate(template);
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.addTemplate(template);
        cruiseConfig.addPipelineWithoutValidation("group", pipelineConfig);

        template.validateTree(ConfigSaveValidationContext.forChain(cruiseConfig), cruiseConfig, false);

        assertThat(template.errors().getAllOn("name")).isEqualTo(List.of("Role \"non-existent-role\" does not exist."));
    }

    @Test
    public void shouldValidateStagePermissionsOfATemplateStageInTheContextOfPipelineUsingTheTemplate() {
        StageConfig stageConfig = StageConfigMother.custom("stage", new JobConfigs(new JobConfig(new CaseInsensitiveString("defaultJob"))));
        stageConfig.setApproval(new Approval(new AuthConfig(new AdminUser(new CaseInsensitiveString("non-admin-non-operate")))));
        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplate("template", stageConfig);
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfigWithTemplate("pipeline", "template");
        pipelineConfig.usingTemplate(template);
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.addTemplate(template);
        cruiseConfig.addPipelineWithoutValidation("group", pipelineConfig);
        PipelineConfigs group = cruiseConfig.findGroup("group");
        group.setAuthorization(new Authorization(new ViewConfig(), new OperationConfig(new AdminUser(new CaseInsensitiveString("foo"))), new AdminsConfig()));
        cruiseConfig.server().security().securityAuthConfigs().add(new SecurityAuthConfig());
        cruiseConfig.server().security().adminsConfig().add(new AdminUser(new CaseInsensitiveString("super-admin")));

        template.validateTree(ConfigSaveValidationContext.forChain(cruiseConfig), cruiseConfig, false);

        assertThat(template.errors().getAllOn("name")).isEqualTo(List.of("User \"non-admin-non-operate\" who is not authorized to operate pipeline group `group` can not be authorized to approve stage"));
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

        assertThat(template.errors().getAllOn("base")).isEqualTo(List.of("Stage with name 'non-existent-stage' does not exist on pipeline 'pipeline', it is being referred to from pipeline 'downstreamPipeline' (cruise-config.xml)"));
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

        assertThat(template.errors().getAllOn("base")).isEqualTo(List.of("\"downstreamPipeline :: mingle :: fetchJob\" tries to fetch artifact from job \"pipeline :: stage :: non-existent-job\" which does not exist."));
    }

    @Test
    public void shouldAllowEditingOfStageNameWhenItIsNotUsedAsDependencyMaterial() {
        PipelineTemplateConfig template = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage2"));
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.addTemplate(template);
        template.getStages().get(0).setName(new CaseInsensitiveString("updatedStageName"));

        template.validateTree(ConfigSaveValidationContext.forChain(cruiseConfig), cruiseConfig, false);

        assertThat(template.errors().isEmpty()).isTrue();
    }

    @Test
    public void shouldAllowEditingOfJobNameWhenItIsNotUsedAsFetchArtifact() {
        PipelineTemplateConfig template = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job2"));
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.addTemplate(template);
        template.getStages().get(0).getJobs().get(0).setName(new CaseInsensitiveString("updatedJobName"));

        template.validateTree(ConfigSaveValidationContext.forChain(cruiseConfig), cruiseConfig, false);

        assertThat(template.errors().isEmpty()).isTrue();
    }

    @Test
    public void copyStagesShouldNotThrowExceptionIfInputPipelineConfigIsNull() {
        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplateWithParams("template-name", "foo", "bar", "baz");
        int sizeBeforeCopy = template.size();
        template.copyStages(null);
        assertThat(template.size()).isEqualTo(sizeBeforeCopy);
    }

}
