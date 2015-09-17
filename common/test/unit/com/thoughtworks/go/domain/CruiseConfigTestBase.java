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

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.merge.MergeConfigOrigin;
import com.thoughtworks.go.config.merge.MergePipelineConfigs;
import com.thoughtworks.go.config.remote.*;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.*;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMMother;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.ReflectionUtil;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

import static com.thoughtworks.go.helper.PipelineConfigMother.createGroup;
import static com.thoughtworks.go.helper.PipelineConfigMother.createPipelineConfig;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;

public abstract class CruiseConfigTestBase {
    public GoConfigMother goConfigMother;
    protected BasicPipelineConfigs pipelines;
    protected CruiseConfig cruiseConfig;

    protected abstract CruiseConfig createCruiseConfig(BasicPipelineConfigs pipelineConfigs);

    protected abstract BasicCruiseConfig createCruiseConfig();

    @Test
    public void shouldLoadPasswordForGivenMaterialFingerprint() {
        MaterialConfig svnConfig = new SvnMaterialConfig("url", "loser", "boozer", true);
        PipelineConfig one = PipelineConfigMother.pipelineConfig("one", svnConfig, new JobConfigs(new JobConfig("job")));
        cruiseConfig.addPipeline("group-1", one);

        P4MaterialConfig p4One = new P4MaterialConfig("server_and_port", "outside_the_window");
        p4One.setPassword("abcdef");
        PipelineConfig two = PipelineConfigMother.pipelineConfig("two", p4One, new JobConfigs(new JobConfig("job")));
        cruiseConfig.addPipeline("group-2", two);

        P4MaterialConfig p4Two = new P4MaterialConfig("port_and_server", "inside_yourself");
        p4Two.setPassword("fedcba");
        PipelineConfig three = PipelineConfigMother.pipelineConfig("three", p4Two, new JobConfigs(new JobConfig("job")));
        cruiseConfig.addPipeline("group-3", three);

        assertThat(cruiseConfig.materialConfigFor(svnConfig.getFingerprint()), is(svnConfig));
        assertThat(cruiseConfig.materialConfigFor(p4One.getFingerprint()), is((MaterialConfig) p4One));
        assertThat(cruiseConfig.materialConfigFor(p4Two.getFingerprint()), is((MaterialConfig) p4Two));
        assertThat(cruiseConfig.materialConfigFor("some_crazy_fingerprint"), is(nullValue()));
    }

    @Test
    public void shouldFindAllAgentResources() {
        cruiseConfig.agents().add(new AgentConfig("uuid", "host1", "127.0.0.1", new Resources("from-agent")));
        assertThat(cruiseConfig.getAllResources(), hasItem(new Resource("from-agent")));
    }


    @Test
    public void shouldFindBuildPlanWithStages() throws Exception {
        try {
            cruiseConfig.jobConfigByName("cetaceans", "whales", "right whale", true);
            fail("Expected not to find right whale in stage whales in pipeline cetaceans");
        } catch (RuntimeException ex) {
            // ignore
        }

        addPipeline("cetaceans", "whales", jobConfig("whale"));

        try {
            cruiseConfig.jobConfigByName("cetaceans", "whales", "dolphin", true);
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Job [dolphin] is not found in pipeline [cetaceans] stage [whales]."));
        }

        try {
            cruiseConfig.jobConfigByName("primates", "whales", "dolphin", true);
            fail("Expected not to find primates in stage whales in pipeline cetaceans");
        } catch (RuntimeException ex) {
            // ignore
        }
        JobConfig plan = jobConfig("baboon");
        addPipeline("primates", "apes", plan);
        assertThat(cruiseConfig.jobConfigByName("primates", "apes", "baboon", true), is(plan));
    }

    @Test
    public void shouldFindNextStage() {
        addPipelineWithStages("mingle", "dev", jobConfig("ut"), jobConfig("ft"));
        assertThat(cruiseConfig.hasNextStage(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev")), is(true));
        StageConfig nextStage = cruiseConfig.nextStage(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev"));
        assertThat(nextStage.name(), is(new CaseInsensitiveString("dev2")));
        assertThat(cruiseConfig.hasNextStage(new CaseInsensitiveString("mingle"), nextStage.name()), is(false));
    }

    @Test
    public void shouldFindPreviousStage() {
        addPipelineWithStages("mingle", "dev", jobConfig("ut"), jobConfig("ft"));
        assertThat(cruiseConfig.hasPreviousStage(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev2")), is(true));
        StageConfig previousStage = cruiseConfig.previousStage(new CaseInsensitiveString("mingle"), new CaseInsensitiveString("dev2"));
        assertThat(previousStage.name(), is(new CaseInsensitiveString("dev")));
        assertThat(cruiseConfig.hasPreviousStage(new CaseInsensitiveString("mingle"), previousStage.name()), is(false));
    }

    @Test
    public void shouldKnowWhenBuildPlanNotInConfigFile() {
        pipelines.add(createPipelineConfig("pipeline", "stage", "build1", "build2"));
        assertThat(cruiseConfig.hasBuildPlan(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("stage"), "build1", true), is(true));
        assertThat(cruiseConfig.hasBuildPlan(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("stage"), "build2", true), is(true));
        assertThat(cruiseConfig.hasBuildPlan(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("stage"), "build3", true), is(false));
    }

    @Test
    public void shouldTellIfSMTPIsEnabled() {
        assertThat(cruiseConfig.isSmtpEnabled(), is(false));

        MailHost mailHost = new MailHost("abc", 12, "admin", "p", true, true, "anc@mail.com", "anc@mail.com");
        cruiseConfig.setServerConfig(new ServerConfig(null, mailHost, null, null));

        cruiseConfig.server().updateMailHost(mailHost);
        assertThat(cruiseConfig.isSmtpEnabled(), is(true));
    }

    @Test
    public void shouldReturnAMapOfTemplateNamesToListOfAssociatedPipelinesCaseInsensitively() {
        PipelineTemplateConfig template = template("first_template");
        PipelineConfig pipelineConfig1 = PipelineConfigMother.pipelineConfig("first");
        pipelineConfig1.clear();
        pipelineConfig1.setTemplateName(new CaseInsensitiveString("first_template"));
        pipelineConfig1.usingTemplate(template);

        PipelineConfig pipelineConfig2 = PipelineConfigMother.pipelineConfig("second");
        pipelineConfig2.clear();
        pipelineConfig2.setTemplateName(new CaseInsensitiveString("FIRST_template"));
        pipelineConfig2.usingTemplate(template);

        PipelineConfig pipelineConfigWithoutTemplate = PipelineConfigMother.pipelineConfig("third");

        BasicPipelineConfigs pipelineConfigs = new BasicPipelineConfigs(pipelineConfig1, pipelineConfig2, pipelineConfigWithoutTemplate);
        CruiseConfig cruiseConfig = createCruiseConfig(pipelineConfigs);

        cruiseConfig.addTemplate(template);
        SecurityConfig securityConfig = new SecurityConfig(new LdapConfig(new GoCipher()), new PasswordFileConfig("foo"), false);
        securityConfig.adminsConfig().add(new AdminUser(new CaseInsensitiveString("root")));
        cruiseConfig.server().useSecurity(securityConfig);

        Map<CaseInsensitiveString, List<CaseInsensitiveString>> templateWithPipelines = cruiseConfig.templatesWithPipelinesForUser("root");

        assertThat(templateWithPipelines.size(), is(1));
        assertThat(templateWithPipelines.get(new CaseInsensitiveString("first_template")), is(Arrays.asList(new CaseInsensitiveString("first"), new CaseInsensitiveString("second"))));
    }

    @Test
    public void shouldReturnAMapOfTemplateNamesToListOfAssociatedPipelinesBasedOnUserPermissions() {

        PipelineTemplateConfig firstTemplate = PipelineTemplateConfigMother.createTemplate("first_template", new Authorization(new AdminsConfig(
                new AdminUser(new CaseInsensitiveString("firstTemplate-admin")))), StageConfigMother.manualStage("stage-one"));
        PipelineConfig pipelineConfig1 = PipelineConfigMother.pipelineConfig("first");
        pipelineConfig1.clear();
        pipelineConfig1.setTemplateName(new CaseInsensitiveString("first_template"));
        pipelineConfig1.usingTemplate(firstTemplate);

        PipelineTemplateConfig secondTemplate = PipelineTemplateConfigMother.createTemplate("second_template", new Authorization(new AdminsConfig(
                new AdminUser(new CaseInsensitiveString("secondTemplate-admin")))), StageConfigMother.stageConfig("one-more"));
        PipelineConfig pipelineConfig2 = PipelineConfigMother.pipelineConfig("second");
        pipelineConfig2.clear();
        pipelineConfig2.setTemplateName(new CaseInsensitiveString("second_template"));
        pipelineConfig2.usingTemplate(secondTemplate);

        PipelineConfig pipelineConfigWithoutTemplate = PipelineConfigMother.pipelineConfig("third");

        CruiseConfig cruiseConfig = createCruiseConfig(new BasicPipelineConfigs(pipelineConfig1, pipelineConfig2, pipelineConfigWithoutTemplate));

        cruiseConfig.addTemplate(firstTemplate);
        cruiseConfig.addTemplate(secondTemplate);

        SecurityConfig securityConfig = new SecurityConfig(new LdapConfig(new GoCipher()), new PasswordFileConfig("foo"), false);
        securityConfig.adminsConfig().add(new AdminUser(new CaseInsensitiveString("root")));
        cruiseConfig.server().useSecurity(securityConfig);

        Map<CaseInsensitiveString, List<CaseInsensitiveString>> templateWithPipelines = cruiseConfig.templatesWithPipelinesForUser("firstTemplate-admin");

        assertThat(templateWithPipelines.size(), is(1));
        assertThat(templateWithPipelines.get(new CaseInsensitiveString("first_template")), is(Arrays.asList(new CaseInsensitiveString("first"))));
    }

    @Test
    public void shouldReturnAMapOfAllTemplateNamesToListOfAssociatedPipelinesIfUserIsSuperAdmin() {

        PipelineTemplateConfig firstTemplate = PipelineTemplateConfigMother.createTemplate("first_template", new Authorization(new AdminsConfig(
                new AdminUser(new CaseInsensitiveString("firstTemplate-admin")))), StageConfigMother.manualStage("stage-one"));
        PipelineConfig pipelineConfig1 = PipelineConfigMother.pipelineConfig("first");
        pipelineConfig1.clear();
        pipelineConfig1.setTemplateName(new CaseInsensitiveString("first_template"));
        pipelineConfig1.usingTemplate(firstTemplate);

        PipelineTemplateConfig secondTemplate = PipelineTemplateConfigMother.createTemplate("second_template", new Authorization(new AdminsConfig(
                new AdminUser(new CaseInsensitiveString("secondTemplate-admin")))), StageConfigMother.stageConfig("one-more"));
        PipelineConfig pipelineConfig2 = PipelineConfigMother.pipelineConfig("second");
        pipelineConfig2.clear();
        pipelineConfig2.setTemplateName(new CaseInsensitiveString("second_template"));
        pipelineConfig2.usingTemplate(secondTemplate);

        PipelineConfig pipelineConfigWithoutTemplate = PipelineConfigMother.pipelineConfig("third");

        CruiseConfig cruiseConfig = createCruiseConfig(new BasicPipelineConfigs(pipelineConfig1, pipelineConfig2, pipelineConfigWithoutTemplate));

        SecurityConfig securityConfig = new SecurityConfig(new LdapConfig(new GoCipher()), new PasswordFileConfig("foo"), false);
        securityConfig.adminsConfig().add(new AdminUser(new CaseInsensitiveString("root")));
        cruiseConfig.server().useSecurity(securityConfig);

        cruiseConfig.addTemplate(firstTemplate);
        cruiseConfig.addTemplate(secondTemplate);

        Map<CaseInsensitiveString, List<CaseInsensitiveString>> templateWithPipelines = cruiseConfig.templatesWithPipelinesForUser("root");

        assertThat(templateWithPipelines.size(), is(2));
        assertThat(templateWithPipelines.get(new CaseInsensitiveString("first_template")), is(Arrays.asList(new CaseInsensitiveString("first"))));
        assertThat(templateWithPipelines.get(new CaseInsensitiveString("second_template")), is(Arrays.asList(new CaseInsensitiveString("second"))));
    }

    private PipelineTemplateConfig template(final String name) {
        return new PipelineTemplateConfig(new CaseInsensitiveString(name), StageConfigMother.stageConfig("some_stage"));
    }

    @Test
    public void shouldThrowExceptionWhenThereIsNoGroup() {
        CruiseConfig config = createCruiseConfig();
        try {
            config.isInFirstGroup(new CaseInsensitiveString("any-pipeline"));
            fail("should throw exception when there is no group");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("No pipeline group defined yet!"));
        }
    }

    @Test
    public void shouldOfferAllTasksToVisitors() throws Exception {
        CruiseConfig config = createCruiseConfig();
        Task task1 = new ExecTask("ls", "-a", "");
        Task task2 = new AntTask();
        setupJobWithTasks(config, task1, task2);

        final List<Task> tasksVisited = new ArrayList<Task>();
        config.accept(new TaskConfigVisitor() {

            public void visit(PipelineConfig pipelineConfig, StageConfig stageConfig, JobConfig jobConfig, Task task) {
                tasksVisited.add(task);
            }
        });

        assertThat(tasksVisited.size(), is(2));
        assertThat(tasksVisited.get(0), is(task1));
        assertThat(tasksVisited.get(1), is(task2));
    }

    @Test
    public void shouldOfferNothingToVisitorsIfThereAreNoTasks() throws Exception {
        CruiseConfig config = createCruiseConfig();
        setupJobWithTasks(config, new NullTask());

        final List<Task> tasksVisited = new ArrayList<Task>();
        config.accept(new TaskConfigVisitor() {

            public void visit(PipelineConfig pipelineConfig, StageConfig stageConfig, JobConfig jobConfig, Task task) {
                tasksVisited.add(task);
            }
        });

        assertThat(tasksVisited.size(), is(0));
    }

    @Test
    public void shouldReturnTrueIfThereAreTwoPipelineGroups() throws Exception {
        CruiseConfig config = goConfigMother.cruiseConfigWithTwoPipelineGroups();
        assertThat("shouldReturnTrueIfThereAreTwoPipelineGroups", config.hasMultiplePipelineGroups(), is(true));
    }

    @Test
    public void shouldReturnFalseIfThereIsOnePipelineGroup() throws Exception {
        CruiseConfig config = goConfigMother.cruiseConfigWithOnePipelineGroup();
        assertThat("shouldReturnFalseIfThereIsOnePipelineGroup", config.hasMultiplePipelineGroups(), is(false));
    }

    @Test
    public void shouldFindDownstreamPipelines() throws Exception {
        CruiseConfig config = goConfigMother.defaultCruiseConfig();
        goConfigMother.addPipeline(config, "pipeline-1", "stage-1", "job-1");
        PipelineConfig pipeline2 = goConfigMother.addPipeline(config, "pipeline-2", "stage-2", "job-2");
        PipelineConfig pipeline3 = goConfigMother.addPipeline(config, "pipeline-3", "stage-3", "job-3");
        goConfigMother.setDependencyOn(config, pipeline2, "pipeline-1", "stage-1");
        goConfigMother.setDependencyOn(config, pipeline3, "pipeline-1", "stage-1");
        Iterable<PipelineConfig> downstream = config.getDownstreamPipelines("pipeline-1");
        assertThat(downstream, hasItem(pipeline2));
        assertThat(downstream, hasItem(pipeline3));
    }

    @Test
    public void shouldReturnFalseForEmptyCruiseConfig() throws Exception {
        CruiseConfig config = createCruiseConfig();
        assertThat("shouldReturnFalseForEmptyCruiseConfig", config.hasMultiplePipelineGroups(), is(false));
    }

    @Test
    public void shouldReturnFalseIfNoMailHost() throws Exception {
        assertThat(createCruiseConfig().isMailHostConfigured(), is(false));
    }

    @Test
    public void shouldReturnTrueIfMailHostIsConfigured() throws Exception {
        MailHost mailHost = new MailHost("hostName", 1234, "user", "pass", true, true, "from", "admin@local.com");
        assertThat(GoConfigMother.cruiseConfigWithMailHost(mailHost).isMailHostConfigured(), is(true));
    }

    @Test
    public void shouldNotLockAPipelineWhenItIsAddedToAnEnvironment() throws Exception {
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline-1");
        EnvironmentConfig env = config.addEnvironment("environment");
        env.addPipeline(new CaseInsensitiveString("pipeline-1"));
        assertThat(config.isPipelineLocked("pipeline-1"), is(false));
    }

    @Test
    public void shouldBeAbleToExplicitlyLockAPipeline() throws Exception {
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline-1");
        PipelineConfig pipelineConfig = config.pipelineConfigByName(new CaseInsensitiveString("pipeline-1"));
        pipelineConfig.lockExplicitly();
        assertThat(config.isPipelineLocked("pipeline-1"), is(true));
    }

    @Test
    public void shouldCollectAllTheErrorsInTheChildren() {
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline-1");

        shouldCollectAllTheErrorsInTheChilderHelper(config);
    }

    protected void shouldCollectAllTheErrorsInTheChilderHelper(CruiseConfig config) {
        config.server().security().ldapConfig().errors().add("uri", "invalid ldap uri");
        config.server().security().ldapConfig().errors().add("searchBase", "invalid search base");

        PipelineConfig pipelineConfig = config.pipelineConfigByName(new CaseInsensitiveString("pipeline-1"));
        pipelineConfig.errors().add("base", "Some base errors");

        P4MaterialConfig p4MaterialConfig = new P4MaterialConfig("localhost:1999", "view");
        p4MaterialConfig.setConfigAttributes(Collections.singletonMap(ScmMaterialConfig.FOLDER, "p4_folder"));
        pipelineConfig.addMaterialConfig(p4MaterialConfig);
        p4MaterialConfig.errors().add("materialName", "material name does not follow pattern");

        StageConfig stage = pipelineConfig.first();
        stage.errors().add("role", "Roles must be proper");

        List<ConfigErrors> allErrors = config.validateAfterPreprocess();
        assertThat(allErrors.size(), is(5));
        assertThat(allErrors.get(0).on("uri"), is("invalid ldap uri"));
        assertThat(allErrors.get(0).on("searchBase"), is("invalid search base"));
        assertThat(allErrors.get(1).on("base"), is("Some base errors"));
        assertThat(allErrors.get(2).on("role"), is("Roles must be proper"));
        assertThat(allErrors.get(3).on(ScmMaterialConfig.FOLDER), is("Destination directory is required when specifying multiple scm materials"));
        assertThat(allErrors.get(4).on("materialName"), is("material name does not follow pattern"));
    }


    @Test
    public void getAllErrors_shouldCollectAllErrorsInTheChildren() {
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline-1");

        config.server().security().ldapConfig().errors().add("uri", "invalid ldap uri");
        config.server().security().ldapConfig().errors().add("searchBase", "invalid search base");

        PipelineConfig pipelineConfig = config.pipelineConfigByName(new CaseInsensitiveString("pipeline-1"));
        pipelineConfig.errors().add("base", "Some base errors");

        P4MaterialConfig p4MaterialConfig = new P4MaterialConfig("localhost:1999", "view");
        pipelineConfig.addMaterialConfig(p4MaterialConfig);
        p4MaterialConfig.errors().add("materialName", "material name does not follow pattern");

        StageConfig stage = pipelineConfig.first();
        stage.errors().add("role", "Roles must be proper");

        List<ConfigErrors> allErrors = config.getAllErrors();
        assertThat(allErrors.size(), is(4));
        assertThat(allErrors.get(0).on("uri"), is("invalid ldap uri"));
        assertThat(allErrors.get(0).on("searchBase"), is("invalid search base"));
        assertThat(allErrors.get(1).on("base"), is("Some base errors"));
        assertThat(allErrors.get(2).on("role"), is("Roles must be proper"));
        assertThat(allErrors.get(3).on("materialName"), is("material name does not follow pattern"));
    }

    @Test
    public void getAllErrors_shouldIgnoreErrorsOnElementToBeSkipped() {
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline-1");

        config.server().security().ldapConfig().errors().add("uri", "invalid ldap uri");
        config.server().security().ldapConfig().errors().add("searchBase", "invalid search base");

        PipelineConfig pipelineConfig = config.pipelineConfigByName(new CaseInsensitiveString("pipeline-1"));
        pipelineConfig.errors().add("base", "Some base errors");

        P4MaterialConfig p4MaterialConfig = new P4MaterialConfig("localhost:1999", "view");
        pipelineConfig.addMaterialConfig(p4MaterialConfig);
        p4MaterialConfig.errors().add("materialName", "material name does not follow pattern");

        StageConfig stage = pipelineConfig.first();
        stage.errors().add("role", "Roles must be proper");

        List<ConfigErrors> allErrors = config.getAllErrorsExceptFor(p4MaterialConfig);
        assertThat(allErrors.size(), is(3));
        assertThat(allErrors.get(0).on("uri"), is("invalid ldap uri"));
        assertThat(allErrors.get(0).on("searchBase"), is("invalid search base"));
        assertThat(allErrors.get(1).on("base"), is("Some base errors"));
        assertThat(allErrors.get(2).on("role"), is("Roles must be proper"));
    }

    @Test
    public void getAllErrors_shouldRetainAllErrorsWhenNoSubjectGiven() {
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline-1");

        config.server().security().ldapConfig().errors().add("uri", "invalid ldap uri");
        config.server().security().ldapConfig().errors().add("searchBase", "invalid search base");

        PipelineConfig pipelineConfig = config.pipelineConfigByName(new CaseInsensitiveString("pipeline-1"));
        pipelineConfig.errors().add("base", "Some base errors");

        P4MaterialConfig p4MaterialConfig = new P4MaterialConfig("localhost:1999", "view");
        pipelineConfig.addMaterialConfig(p4MaterialConfig);
        p4MaterialConfig.errors().add("materialName", "material name does not follow pattern");

        StageConfig stage = pipelineConfig.first();
        stage.errors().add("role", "Roles must be proper");

        List<ConfigErrors> allErrors = config.getAllErrorsExceptFor(null);
        assertThat(allErrors.size(), is(4));
    }

    @Test
    public void shouldBuildTheValidationContextForAnOnCancelTask() {
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline-1");
        PipelineConfig pipelineConfig = config.pipelineConfigByName(new CaseInsensitiveString("pipeline-1"));
        StageConfig stageConfig = pipelineConfig.get(0);
        JobConfig jobConfig = stageConfig.getJobs().get(0);
        ExecTask execTask = new ExecTask("ls", "-la", "dir");
        Task mockTask = mock(Task.class);
        when(mockTask.errors()).thenReturn(new ConfigErrors());
        execTask.setCancelTask(mockTask);
        jobConfig.addTask(execTask);

        config.validateAfterPreprocess();

        verify(mockTask).validate(ValidationContext.forChain(
                config,
                config.getGroups(),
                config.getGroups().findGroup("defaultGroup"),
                pipelineConfig,
                stageConfig,
                stageConfig.getJobs(),
                jobConfig,
                jobConfig.getTasks(),
                execTask,
                execTask.onCancelConfig()));
    }

    @Test
    @Ignore("ShilpaG & RajeshM removed in 12208:39962f06092d, because they could not find any place in the codebase where circular reference existed(so the circular ref check was undone) - Sachin & JJ")
    public void shouldNotFailOnCircularReference() {
        MyValidatable foo = new MyValidatable();
        MyValidatable bar = new MyValidatable();
        foo.innerValidatable = bar;
        bar.innerValidatable = foo;

        GoConfigGraphWalker.Handler handler = mock(GoConfigGraphWalker.Handler.class);

        new GoConfigGraphWalker(foo).walk(handler);

        verify(handler).handle(same(foo), any(ValidationContext.class));
        verify(handler).handle(same(bar), any(ValidationContext.class));
    }

    @Test
    public void shouldNotConsiderEqualObjectsAsSame() {
        MyValidatable foo = new AlwaysEqualMyValidatable();
        MyValidatable bar = new AlwaysEqualMyValidatable();
        foo.innerValidatable = bar;

        GoConfigGraphWalker.Handler handler = mock(GoConfigGraphWalker.Handler.class);

        new GoConfigGraphWalker(foo).walk(handler);

        verify(handler).handle(same(foo), any(ValidationContext.class));
        verify(handler).handle(same(bar), any(ValidationContext.class));
    }

    @Test
    public void shouldIgnoreConstantFieldsWhileCollectingErrorsToAvoidPotentialCycles() {
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline-1");
        List<ConfigErrors> allErrors = config.validateAfterPreprocess();
        assertThat(allErrors.size(), is(0));
    }

    @Test
    public void shouldErrorOutWhenDependsOnItself() throws Exception {
        CruiseConfig cruiseConfig = createCruiseConfig();
        PipelineConfig pipelineConfig = goConfigMother.addPipeline(cruiseConfig, "pipeline1", "stage", "build");
        goConfigMother.addStageToPipeline(cruiseConfig, "pipeline1", "ft", "build");
        goConfigMother.setDependencyOn(cruiseConfig, pipelineConfig, "pipeline1", "ft");
        cruiseConfig.validate(null);
        ConfigErrors errors = pipelineConfig.materialConfigs().errors();
        assertThat(errors.on("base"), is("Circular dependency: pipeline1 <- pipeline1"));
    }

    @Test
    public void shouldNotDuplicateErrorWhenPipelineDoesnotExist() throws Exception {
        CruiseConfig cruiseConfig = createCruiseConfig();
        PipelineConfig pipelineConfig = goConfigMother.addPipeline(cruiseConfig, "pipeline1", "stage", "build");
        PipelineConfig pipelineConfig2 = goConfigMother.addPipeline(cruiseConfig, "pipeline2", "stage", "build");
        goConfigMother.addStageToPipeline(cruiseConfig, "pipeline1", "ft", "build");
        goConfigMother.setDependencyOn(cruiseConfig, pipelineConfig2, "pipeline1", "ft");
        goConfigMother.setDependencyOn(cruiseConfig, pipelineConfig, "invalid", "invalid");
        cruiseConfig.validate(null);
        List<ConfigErrors> allErrors = cruiseConfig.getAllErrors();
        List<String> errors = new ArrayList<String>();
        for (ConfigErrors allError : allErrors) {
            errors.addAll(allError.getAllOn("base"));
        }
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0), is("Pipeline \"invalid\" does not exist. It is used from pipeline \"pipeline1\"."));
    }

    @Test
    public void shouldErrorOutWhenTwoPipelinesDependsOnEachOther() throws Exception {
        CruiseConfig cruiseConfig = createCruiseConfig();
        PipelineConfig pipeline1 = goConfigMother.addPipeline(cruiseConfig, "pipeline1", "stage", "build");
        PipelineConfig pipeline2 = goConfigMother.addPipeline(cruiseConfig, "pipeline2", "stage", "build");
        goConfigMother.setDependencyOn(cruiseConfig, pipeline2, "pipeline1", "stage");
        goConfigMother.setDependencyOn(cruiseConfig, pipeline1, "pipeline2", "stage");

        cruiseConfig.validate(null);

        assertThat(pipeline1.materialConfigs().errors().isEmpty(), is(false));
        assertThat(pipeline2.materialConfigs().errors().isEmpty(), is(false));
    }

    @Test
    public void shouldAddPipelineWithoutValidationInAnExistingGroup() {
        CruiseConfig cruiseConfig = createCruiseConfig();
        PipelineConfig pipeline1 = PipelineConfigMother.pipelineConfig("first");
        PipelineConfig pipeline2 = PipelineConfigMother.pipelineConfig("first");
        cruiseConfig.addPipelineWithoutValidation("first-group", pipeline1);

        assertThat(cruiseConfig.getGroups().size(), is(1));
        assertThat(cruiseConfig.findGroup("first-group").get(0), is(pipeline1));

        cruiseConfig.addPipelineWithoutValidation("first-group", pipeline2);
        assertThat(cruiseConfig.findGroup("first-group").get(0), is(pipeline1));
        assertThat(cruiseConfig.findGroup("first-group").get(1), is(pipeline2));
    }

    @Test
    public void shouldErrorOutWhenThreePipelinesFormACycle() throws Exception {
        CruiseConfig cruiseConfig = createCruiseConfig();
        PipelineConfig pipeline1 = goConfigMother.addPipeline(cruiseConfig, "pipeline1", "stage", "build");
        SvnMaterialConfig material = (SvnMaterialConfig) pipeline1.materialConfigs().get(0);
        material.setConfigAttributes(Collections.singletonMap(ScmMaterialConfig.FOLDER, "svn_dir"));
        P4MaterialConfig p4MaterialConfig = new P4MaterialConfig("localhost:1999", "view");
        p4MaterialConfig.setConfigAttributes(Collections.singletonMap(ScmMaterialConfig.FOLDER, "p4_folder"));
        pipeline1.addMaterialConfig(p4MaterialConfig);
        PipelineConfig pipeline2 = goConfigMother.addPipeline(cruiseConfig, "pipeline3", "stage", "build");
        PipelineConfig pipeline3 = goConfigMother.addPipeline(cruiseConfig, "pipeline2", "stage", "build");
        goConfigMother.setDependencyOn(cruiseConfig, pipeline3, "pipeline3", "stage");
        goConfigMother.setDependencyOn(cruiseConfig, pipeline2, "pipeline1", "stage");
        goConfigMother.setDependencyOn(cruiseConfig, pipeline1, "pipeline2", "stage");
        cruiseConfig.validate(null);
        assertThat(pipeline1.materialConfigs().errors().isEmpty(), is(false));
        assertThat(pipeline2.materialConfigs().errors().isEmpty(), is(false));
        assertThat(pipeline3.materialConfigs().errors().isEmpty(), is(false));
    }

    @Test
    public void shouldAllowCleanupOfNonExistentStages() {
        CruiseConfig cruiseConfig = createCruiseConfig();
        assertThat(cruiseConfig.isArtifactCleanupProhibited("foo", "bar"), is(false));

        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("foo-pipeline", "bar-stage", "baz-job");
        cruiseConfig.addPipeline("defaultGrp", pipelineConfig);
        assertThat(cruiseConfig.isArtifactCleanupProhibited("foo-pipeline", "baz-stage"), is(false));
        assertThat(cruiseConfig.isArtifactCleanupProhibited("foo-pipeline", "bar-stage"), is(false));

        ReflectionUtil.setField(pipelineConfig.getFirstStageConfig(), "artifactCleanupProhibited", true);
        assertThat(cruiseConfig.isArtifactCleanupProhibited("foo-pipeline", "bar-stage"), is(true));
        assertThat(cruiseConfig.isArtifactCleanupProhibited("fOO-pipeLINE", "BaR-StagE"), is(true));
    }

    @Test
    public void shouldReturnDefaultGroupNameIfNoGroupNameIsSpecified() {
        CruiseConfig cfg = createCruiseConfig();
        assertThat(cfg.sanitizedGroupName(null), is(BasicPipelineConfigs.DEFAULT_GROUP));
        cfg.addPipeline("grp1", PipelineConfigMother.pipelineConfig("foo"));
        assertThat(cfg.sanitizedGroupName(null), is(BasicPipelineConfigs.DEFAULT_GROUP));
    }

    @Test
    public void shouldReturnSelectedGroupNameIfGroupNameIsSpecified() {
        CruiseConfig cfg = createCruiseConfig();
        cfg.addPipeline("grp1", PipelineConfigMother.pipelineConfig("foo"));
        assertThat(cfg.sanitizedGroupName("gr1"), is("gr1"));
    }

    @Test
    public void shouldRemoveUserFromRoleWhenRoleIsDeleted() {
        CruiseConfig config = createCruiseConfig();
        SecurityConfig securityConfig = new SecurityConfig(new LdapConfig(new GoCipher()), new PasswordFileConfig("foo"), false);
        securityConfig.adminsConfig().add(new AdminUser(new CaseInsensitiveString("root")));
        final Role role = goConfigMother.createRole("ldap-users", "root");
        securityConfig.addRole(role);
        config.server().useSecurity(securityConfig);

        assertTrue(config.server().security().getRoles().isRoleExist(new CaseInsensitiveString("ldap-users")));
        config.removeRole(role);
        assertFalse(config.server().security().getRoles().isRoleExist(new CaseInsensitiveString("ldap-users")));
    }

    @Test
    public void shouldDeleteAdminRolesInAllPipelineGroupWhenARoleIsDeleted() throws Exception {
        final Role role = setupSecurityWithRole();

        goConfigMother.addPipelineWithGroup(cruiseConfig, "group1", "p1", "s1", "b1");
        goConfigMother.addPipelineWithGroup(cruiseConfig, "group2", "p2", "s2", "b2");
        goConfigMother.addRoleAsViewerOfPipelineGroup(cruiseConfig, "ldap-users", "group1");
        goConfigMother.addRoleAsViewerOfPipelineGroup(cruiseConfig, "ldap-users", "group2");

        assertEquals(new HashSet<String>(Arrays.asList("group1", "group2")), cruiseConfig.groupsAffectedByDeletionOfRole("ldap-users").keySet());
        cruiseConfig.removeRole(role);
        assertEquals(new HashSet<String>(), cruiseConfig.groupsAffectedByDeletionOfRole("ldap-users").keySet());
    }

    @Test
    public void shouldDeleteAdminRoleFromAllApprovalsWhenARoleIsDeleted() throws Exception {
        final Role role = setupSecurityWithRole();
        goConfigMother.addPipelineWithGroup(cruiseConfig, "group1", "p1", "s1", "b1");
        goConfigMother.addApprovalForStage(cruiseConfig, "p1", "s1", "ldap-users");
        assertEquals(1, cruiseConfig.stagesWithPermissionForRole(role.getName().toLower()).size());
        cruiseConfig.removeRole(role);
        assertEquals(0, cruiseConfig.stagesWithPermissionForRole(role.getName().toLower()).size());
    }

    @Test
    public void shouldRemoveSystemAdminPermissionsForARoleWhenARoleIsDeleted() throws Exception {
        String adminRoleName = "admin-role";
        Role adminRole = new Role(new CaseInsensitiveString(adminRoleName));
        adminRole.addUser(new RoleUser(new CaseInsensitiveString("admin")));
        goConfigMother.addRole(cruiseConfig, adminRole);
        goConfigMother.addRoleAsSuperAdmin(cruiseConfig, adminRoleName);
        assertThat(cruiseConfig.doesAdminConfigContainRole(adminRoleName), is(true));
        assertThat(cruiseConfig.server().security().getRoles().contains(adminRole), is(true));
        cruiseConfig.removeRole(adminRole);
        assertThat(cruiseConfig.doesAdminConfigContainRole(adminRoleName), is(false));
        assertThat(cruiseConfig.server().security().getRoles().contains(adminRole), is(false));
    }

    @Test
    public void shouldAddPackageRepository() throws Exception {
        PackageRepository packageRepository = new PackageRepository();
        cruiseConfig.savePackageRepository(packageRepository);
        assertThat(cruiseConfig.getPackageRepositories().size(), is(1));
        assertThat(cruiseConfig.getPackageRepositories().get(0), is(packageRepository));
        assertThat(cruiseConfig.getPackageRepositories().get(0).getId(), is(notNullValue()));
    }

    @Test
    public void shouldUpdatePackageRepository() throws Exception {
        PackageRepository packageRepository = new PackageRepository();
        packageRepository.setName("old");
        cruiseConfig.savePackageRepository(packageRepository);

        packageRepository.setName("new");
        cruiseConfig.savePackageRepository(packageRepository);

        assertThat(cruiseConfig.getPackageRepositories().size(), is(1));
        assertThat(cruiseConfig.getPackageRepositories().get(0), is(packageRepository));
        assertThat(cruiseConfig.getPackageRepositories().get(0).getId(), is(notNullValue()));
        assertThat(cruiseConfig.getPackageRepositories().get(0).getName(), is("new"));
    }

    @Test
    public void shouldAddPackageDefinitionToGivenRepository() throws Exception {
        String repoId = "repo-id";
        PackageRepository packageRepository = PackageRepositoryMother.create(repoId, "repo-name", "plugin-id", "1.0", new Configuration());
        PackageDefinition existing = PackageDefinitionMother.create("pkg-1", "pkg1-name", new Configuration(), packageRepository);

        packageRepository.setPackages(new Packages(existing));
        cruiseConfig.setPackageRepositories(new PackageRepositories(packageRepository));

        Configuration configuration = new Configuration();
        configuration.add(new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue("value")));
        configuration.add(new ConfigurationProperty(new ConfigurationKey("key-with-no-value"), new ConfigurationValue("")));
        PackageDefinition packageDefinition = PackageDefinitionMother.create(null, "pkg2-name", configuration, packageRepository);
        cruiseConfig.savePackageDefinition(packageDefinition);

        assertThat(cruiseConfig.getPackageRepositories().size(), is(1));
        assertThat(cruiseConfig.getPackageRepositories().get(0).getId(), is(repoId));

        assertThat(cruiseConfig.getPackageRepositories().get(0).getPackages().size(), is(2));
        assertThat(cruiseConfig.getPackageRepositories().get(0).getPackages().get(0).getId(), is(existing.getId()));
        PackageDefinition createdPkgDef = cruiseConfig.getPackageRepositories().get(0).getPackages().get(1);
        assertThat(createdPkgDef.getId(), is(notNullValue()));
        assertThat(createdPkgDef.getConfiguration().getProperty("key"), is(Matchers.notNullValue()));
        assertThat(createdPkgDef.getConfiguration().getProperty("key-with-no-value"), is(nullValue()));
    }

    @Test
    public void shouldClearPackageRepositoryConfigurationsWhichAreEmptyWithNoErrors() throws Exception {
        PackageRepository packageRepository = mock(PackageRepository.class);
        when(packageRepository.isNew()).thenReturn(true);
        cruiseConfig.savePackageRepository(packageRepository);
        verify(packageRepository).clearEmptyConfigurations();
    }

    @Test
    public void shouldRemovePackageRepositoryById() throws Exception {
        PackageRepository packageRepository = PackageRepositoryMother.create(null, "repo", "pid", "1.3", new Configuration());
        cruiseConfig.savePackageRepository(packageRepository);
        cruiseConfig.removePackageRepository(packageRepository.getId());
        assertThat(cruiseConfig.getPackageRepositories().find(packageRepository.getId()), is(Matchers.nullValue()));
    }

    @Test
    public void shouldDecideIfRepoCanBeDeleted_BasedOnPackageRepositoryBeingUsedByPipelines() throws Exception {
        PackageRepository repo1 = PackageRepositoryMother.create(null, "repo1", "plugin", "1.3", new Configuration());
        PackageRepository repo2 = PackageRepositoryMother.create(null, "repo2", "plugin", "1.3", new Configuration());
        PackageDefinition packageDefinition = PackageDefinitionMother.create("package", repo2);
        repo2.addPackage(packageDefinition);
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline");
        pipeline.addMaterialConfig(new PackageMaterialConfig(new CaseInsensitiveString("p1"), packageDefinition.getId(), packageDefinition));
        cruiseConfig.addPipeline("existing_group", pipeline);
        cruiseConfig.savePackageRepository(repo1);
        cruiseConfig.savePackageRepository(repo2);
        assertThat(cruiseConfig.canDeletePackageRepository(repo1), is(true));
        assertThat(cruiseConfig.canDeletePackageRepository(repo2), is(false));
    }

    @Test
    public void shouldDecideIfPluggableSCMMaterialCanBeDeleted_BasedOnPluggableSCMMaterialBeingUsedByPipelines() throws Exception {
        SCM scmConfigOne = SCMMother.create("scm-id-1");
        SCM scmConfigTwo = SCMMother.create("scm-id-2");
        cruiseConfig.getSCMs().addAll(Arrays.asList(scmConfigOne, scmConfigTwo));
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline");
        pipeline.addMaterialConfig(new PluggableSCMMaterialConfig(null, scmConfigOne, null, null));
        cruiseConfig.addPipeline("existing_group", pipeline);

        assertThat(cruiseConfig.canDeletePluggableSCMMaterial(scmConfigOne), is(false));
        assertThat(cruiseConfig.canDeletePluggableSCMMaterial(scmConfigTwo), is(true));
    }
    @Test
    public void shouldReturnConfigRepos(){
        assertNotNull(cruiseConfig.getConfigRepos());
    }

    @Test
    public void shouldReturnTrueWhenHasGroup()
    {
        assertThat(cruiseConfig.hasPipelineGroup("existing_group"), is(true));
    }
    @Test
    public void shouldReturnFalseWhenDoesNotHaveGroup()
    {
        assertThat(cruiseConfig.hasPipelineGroup("non_existing_group"),is(false));
    }

    @Test
    public  void shouldGetJobConfigByName()
    {
        goConfigMother.addPipeline(cruiseConfig, "cruise", "dev", "linux-firefox");
        JobConfig job = cruiseConfig.jobConfigByName("cruise", "dev", "linux-firefox", true);
        assertNotNull(job);
    }


    private Role setupSecurityWithRole() {
        SecurityConfig securityConfig = new SecurityConfig(new LdapConfig(new GoCipher()), new PasswordFileConfig("foo"), false);
        securityConfig.adminsConfig().add(new AdminUser(new CaseInsensitiveString("root")));
        final Role ldapUsersRole = goConfigMother.createRole("ldap-users", "root");
        securityConfig.addRole(ldapUsersRole);
        cruiseConfig.server().useSecurity(securityConfig);
        return ldapUsersRole;
    }

    private StageConfig stageConfig(String pipelineName, String stageName) {
        return cruiseConfig.stageConfigByName(new CaseInsensitiveString(pipelineName), new CaseInsensitiveString(stageName));
    }

    private void setupJobWithTasks(CruiseConfig config, Task... tasks) throws Exception {
        goConfigMother.addPipeline(config, "cruise", "dev", "linux-firefox");
        JobConfig job = config.jobConfigByName("cruise", "dev", "linux-firefox", true);

        for (Task task : tasks) {
            job.addTask(task);
        }
    }

    private JobConfig jobConfig(String jobConfigName) {
        return new JobConfig(new CaseInsensitiveString(jobConfigName), null, null);
    }

    private PipelineConfig addPipeline(String pipelineName, String stageName, JobConfig... jobConfigs) {
        PipelineConfig pipeline = new PipelineConfig(new CaseInsensitiveString(pipelineName), new MaterialConfigs());
        pipeline.add(new StageConfig(new CaseInsensitiveString(stageName), new JobConfigs(jobConfigs)));
        pipelines.add(pipeline);
        return pipeline;
    }

    private void addPipelineWithStages(String pipelineName, String stageName, JobConfig... jobConfigs) {
        PipelineConfig pipeline = new PipelineConfig(new CaseInsensitiveString(pipelineName), (MaterialConfigs) null);
        pipeline.add(new StageConfig(new CaseInsensitiveString(stageName), new JobConfigs(jobConfigs)));
        pipeline.add(new StageConfig(new CaseInsensitiveString(stageName + "2"), new JobConfigs(jobConfigs)));
        pipelines.add(pipeline);
    }

    private static class MyValidatable implements Validatable {
        public Validatable innerValidatable;

        public void validate(ValidationContext validationContext) {
        }

        public ConfigErrors errors() {
            return new ConfigErrors();
        }

        public void addError(String fieldName, String message) {
        }

    }

    private static class AlwaysEqualMyValidatable extends MyValidatable {
        @Override
        public final int hashCode() {
            return 42;
        }

        @Override
        public final boolean equals(Object obj) {
            return true;
        }
    }
}
