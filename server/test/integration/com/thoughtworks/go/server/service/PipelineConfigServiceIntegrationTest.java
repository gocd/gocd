/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */


package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.parts.XmlPartialConfigProvider;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.config.*;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.presentation.TriStateSelection;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.DefaultLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.CachedDigestUtils;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static com.thoughtworks.go.util.TestUtils.contains;
import static java.util.Arrays.asList;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertFalse;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})

public class PipelineConfigServiceIntegrationTest {
    static {
        new SystemEnvironment().setProperty(GoConstants.USE_COMPRESSED_JAVASCRIPT, "false");
    }

    @Autowired
    private PipelineConfigService pipelineConfigService;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private ConfigRepository configRepository;
    @Autowired
    private ConfigCache configCache;
    @Autowired
    private ConfigElementImplementationRegistry registry;
    @Autowired
    private GoPartialConfig goPartialConfig;
    @Autowired
    private CachedGoPartials cachedGoPartials;
    @Autowired
    private Localizer localizer;
    @Autowired
    private EntityHashingService entityHashingService;
    @Autowired
    private ServerHealthService serverHealthService;

    private GoConfigFileHelper configHelper;
    private PipelineConfig pipelineConfig;
    private Username user;
    private String headCommitBeforeUpdate;
    private HttpLocalizedOperationResult result;
    private String groupName = "jumbo";
    private ConfigRepoConfig repoConfig1;
    private PartialConfig partialConfig;
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private String remoteDownstreamPipelineName;
    private ConfigRepoConfig repoConfig2;

    @Before
    public void setup() throws Exception {
        cachedGoPartials.clear();
        configHelper = new GoConfigFileHelper();
        dbHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();
        goConfigService.forceNotifyListeners();
        user = new Username(new CaseInsensitiveString("current"));
        pipelineConfig = GoConfigMother.createPipelineConfigWithMaterialConfig(UUID.randomUUID().toString(), new GitMaterialConfig("FOO"));
        goConfigService.addPipeline(pipelineConfig, groupName);
        repoConfig1 = new ConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig("url"), XmlPartialConfigProvider.providerName);
        repoConfig2 = new ConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig("url2"), XmlPartialConfigProvider.providerName);
        goConfigService.updateConfig(new UpdateConfigCommand() {
            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                cruiseConfig.getConfigRepos().add(repoConfig1);
                cruiseConfig.getConfigRepos().add(repoConfig2);
                return cruiseConfig;
            }
        });
        GoCipher goCipher = new GoCipher();
        goConfigService.updateServerConfig(new MailHost(goCipher), new LdapConfig(goCipher), new PasswordFileConfig("path"), false, goConfigService.configFileMd5(), "artifacts", null, null, "0", null, null, "foo");
        UpdateConfigCommand command = goConfigService.modifyAdminPrivilegesCommand(asList(user.getUsername().toString()), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.add));
        goConfigService.updateConfig(command);
        remoteDownstreamPipelineName = "remote-downstream";
        partialConfig = PartialConfigMother.pipelineWithDependencyMaterial(remoteDownstreamPipelineName, pipelineConfig, new RepoConfigOrigin(repoConfig1, "repo1_r1"));
        goPartialConfig.onSuccessPartialConfig(repoConfig1, partialConfig);
        PartialConfig partialConfigFromRepo2 = PartialConfigMother.withPipeline("independent-pipeline", new RepoConfigOrigin(repoConfig2, "repo2_r1"));
        goPartialConfig.onSuccessPartialConfig(repoConfig2, partialConfigFromRepo2);
        result = new HttpLocalizedOperationResult();
        headCommitBeforeUpdate = configRepository.getCurrentRevCommit().name();

    }

    @After
    public void tearDown() throws Exception {
        for (PartialConfig partial : cachedGoPartials.lastValidPartials()) {
            assertThat(ErrorCollector.getAllErrors(partial).isEmpty(), is(true));
        }
        for (PartialConfig partial : cachedGoPartials.lastKnownPartials()) {
            assertThat(ErrorCollector.getAllErrors(partial).isEmpty(), is(true));
        }
        cachedGoPartials.clear();
        configHelper.onTearDown();
        dbHelper.onTearDown();
    }

    @Test
    public void shouldCreatePipelineConfigWhenPipelineGroupExists() throws GitAPIException {
        GoConfigHolder goConfigHolderBeforeUpdate = goConfigDao.loadConfigHolder();
        pipelineConfig = GoConfigMother.createPipelineConfigWithMaterialConfig(UUID.randomUUID().toString(), new GitMaterialConfig("FOO"));
        pipelineConfigService.createPipelineConfig(user, pipelineConfig, result, groupName);

        assertThat(result.toString(), result.isSuccessful(), is(true));
        assertThat(goConfigDao.loadConfigHolder(), is(not(goConfigHolderBeforeUpdate)));
        PipelineConfig savedPipelineConfig = goConfigDao.loadForEditing().getPipelineConfigByName(pipelineConfig.name());
        assertThat(savedPipelineConfig, is(pipelineConfig));
        assertThat(configRepository.getCurrentRevCommit().name(), is(not(headCommitBeforeUpdate)));
        assertThat(configRepository.getCurrentRevision().getUsername(), is(user.getDisplayName()));
        assertThat(configRepository.getCurrentRevision().getMd5(), is(not(goConfigHolderBeforeUpdate.config.getMd5())));
        assertThat(configRepository.getCurrentRevision().getMd5(), is(goConfigDao.loadConfigHolder().config.getMd5()));
        assertThat(configRepository.getCurrentRevision().getMd5(), is(goConfigDao.loadConfigHolder().configForEdit.getMd5()));
    }

    @Test
    public void shouldCreatePipelineConfigWhenPipelineGroupDoesNotExist() throws GitAPIException {
        GoConfigHolder goConfigHolderBeforeUpdate = goConfigDao.loadConfigHolder();
        PipelineConfig downstream = GoConfigMother.createPipelineConfigWithMaterialConfig(UUID.randomUUID().toString(), new DependencyMaterialConfig(pipelineConfig.name(), pipelineConfig.first().name()));
        pipelineConfigService.createPipelineConfig(user, downstream, result, "does-not-exist");

        assertThat(result.toString(), result.isSuccessful(), is(true));
        assertThat(goConfigDao.loadConfigHolder(), is(not(goConfigHolderBeforeUpdate)));
        PipelineConfig savedPipelineConfig = goConfigDao.loadForEditing().getPipelineConfigByName(downstream.name());
        assertThat(savedPipelineConfig, is(downstream));
        assertThat(configRepository.getCurrentRevCommit().name(), is(not(headCommitBeforeUpdate)));
        assertThat(configRepository.getCurrentRevision().getUsername(), is(user.getDisplayName()));
    }

    @Test
    public void shouldUpdatePipelineConfigWhenDependencyMaterialHasTemplateDefined() throws Exception {
        CaseInsensitiveString templateName = new CaseInsensitiveString("template_with_param");
        saveTemplateWithParamToConfig(templateName);

        pipelineConfig.clear();
        pipelineConfig.setTemplateName(templateName);
        pipelineConfig.addParam(new ParamConfig("SOME_PARAM", "SOME_VALUE"));

        CruiseConfig cruiseConfig = goConfigDao.loadConfigHolder().configForEdit;
        cruiseConfig.update(groupName, pipelineConfig.name().toString(), pipelineConfig);
        saveConfig(cruiseConfig);

        PipelineConfig downstream = GoConfigMother.createPipelineConfigWithMaterialConfig("downstream", new DependencyMaterialConfig(pipelineConfig.name(), new CaseInsensitiveString("stage")));
        pipelineConfigService.createPipelineConfig(user, downstream, result, groupName);

        assertThat(result.toString(), result.isSuccessful(), is(true));
        assertTrue(downstream.materialConfigs().first().errors().isEmpty());
    }

    @Test
    public void shouldUpdatePipelineConfigWithDependencyMaterialWhenUpstreamPipelineHasTemplateDefinedANDUpstreamPipelineIsCreatedUsingCreatePipelineFlow() throws Exception {
        CaseInsensitiveString templateName = new CaseInsensitiveString("template_with_param");
        saveTemplateWithParamToConfig(templateName);

        MaterialConfigs materialConfigs = new MaterialConfigs();
        materialConfigs.add(new DependencyMaterialConfig(pipelineConfig.name(), new CaseInsensitiveString("stage")));
        PipelineConfig upstream = new PipelineConfig(new CaseInsensitiveString("upstream"), materialConfigs);
        upstream.setTemplateName(templateName);
        upstream.addParam(new ParamConfig("SOME_PARAM", "SOME_VALUE"));
        pipelineConfigService.createPipelineConfig(user, upstream, result, groupName);

        PipelineConfig downstream = GoConfigMother.createPipelineConfigWithMaterialConfig("downstream", new DependencyMaterialConfig(upstream.name(), new CaseInsensitiveString("stage")));

        pipelineConfigService.createPipelineConfig(user, downstream, result, groupName);

        assertThat(result.toString(), result.isSuccessful(), is(true));
        assertTrue(downstream.materialConfigs().first().errors().isEmpty());
    }

    @Test
    public void shouldUpdatePipelineConfigWhenFetchTaskFromUpstreamHasPipelineWithTemplateDefined() throws Exception {
        CaseInsensitiveString templateName = new CaseInsensitiveString("template_with_param");
        saveTemplateWithParamToConfig(templateName);

        pipelineConfig.clear();
        pipelineConfig.setTemplateName(templateName);
        pipelineConfig.addParam(new ParamConfig("SOME_PARAM", "SOME_VALUE"));

        CaseInsensitiveString stage = new CaseInsensitiveString("stage");
        CaseInsensitiveString job = new CaseInsensitiveString("job");
        CruiseConfig cruiseConfig = goConfigDao.loadConfigHolder().configForEdit;
        cruiseConfig.update(groupName, pipelineConfig.name().toString(), pipelineConfig);
        saveConfig(cruiseConfig);

        PipelineConfig downstream = GoConfigMother.createPipelineConfigWithMaterialConfig("downstream", new DependencyMaterialConfig(pipelineConfig.name(), stage));
        downstream.getStage(stage).getJobs().first().addTask(new FetchTask(pipelineConfig.name(), stage, job, "src", "dest"));
        pipelineConfigService.createPipelineConfig(user, downstream, result, groupName);

        assertThat(result.toString(), result.isSuccessful(), is(true));
        assertTrue(downstream.materialConfigs().first().errors().isEmpty());
    }

    @Test
    public void shouldNotCreatePipelineConfigWhenAPipelineBySameNameAlreadyExists() throws GitAPIException {
        GoConfigHolder goConfigHolderBeforeUpdate = goConfigDao.loadConfigHolder();
        PipelineConfig pipelineBeingCreated = GoConfigMother.createPipelineConfigWithMaterialConfig(pipelineConfig.name().toLower(), new DependencyMaterialConfig(pipelineConfig.name(), pipelineConfig.first().name()));
        pipelineConfigService.createPipelineConfig(user, pipelineBeingCreated, result, groupName);

        assertThat(result.toString(), result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(422));
        assertFalse(pipelineBeingCreated.errors().isEmpty());
        assertThat(pipelineBeingCreated.errors().on(PipelineConfig.NAME), is(String.format("You have defined multiple pipelines named '%s'. Pipeline names must be unique. Source(s): [cruise-config.xml]", pipelineConfig.name())));
        assertThat(goConfigDao.loadConfigHolder(), is(goConfigHolderBeforeUpdate));
        assertThat(configRepository.getCurrentRevCommit().name(), is(headCommitBeforeUpdate));
    }

    @Test
    public void shouldNotCreatePipelineConfigWhenInvalidGroupNameIsPassed() throws GitAPIException {
        GoConfigHolder goConfigHolderBeforeUpdate = goConfigDao.loadConfigHolder();
        PipelineConfig pipelineBeingCreated = GoConfigMother.createPipelineConfigWithMaterialConfig(pipelineConfig.name().toLower(), new DependencyMaterialConfig(pipelineConfig.name(), pipelineConfig.first().name()));
        pipelineConfigService.createPipelineConfig(user, pipelineBeingCreated, result, "%$-with-invalid-characters");

        assertThat(result.toString(), result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(422));
        assertFalse(pipelineBeingCreated.errors().isEmpty());
        assertThat(pipelineBeingCreated.errors().on(PipelineConfigs.GROUP), contains("Invalid group name '%$-with-invalid-characters'"));
        assertThat(goConfigDao.loadConfigHolder(), is(goConfigHolderBeforeUpdate));
        assertThat(configRepository.getCurrentRevCommit().name(), is(headCommitBeforeUpdate));
    }

    @Test
    public void shouldShowThePipelineConfigErrorMessageWhenPipelineBeingCreatedHasErrors() throws GitAPIException {
        ExecTask execTask = new ExecTask("ls", "-al", "#{foo}");
        FetchTask fetchTask = new FetchTask(pipelineConfig.name(), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "srcfile", "/usr/dest");

        JobConfig job = new JobConfig("default-job");
        job.addTask(execTask);
        job.addTask(fetchTask);

        StageConfig stage = new StageConfig(new CaseInsensitiveString("default-stage"), new JobConfigs(job));

        PipelineConfig pipeline = GoConfigMother.createPipelineConfigWithMaterialConfig(UUID.randomUUID().toString(), new DependencyMaterialConfig(pipelineConfig.name(), pipelineConfig.first().name()));
        pipeline.addParam(new ParamConfig("foo", "."));
        pipeline.addStageWithoutValidityAssertion(stage);

        pipelineConfigService.createPipelineConfig(user, pipeline, result, groupName);

        assertThat(result.toString(), result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(422));
        String expectedError = String.format("Task of job 'default-job' in stage 'default-stage' of pipeline '%s' has dest path '/usr/dest' which is outside the working directory.", pipeline.name());
        assertThat(fetchTask.errors().on("dest"), is(expectedError));
    }

    @Test
    public void shouldShowThePipelineConfigErrorMessageWhenPipelineBeingCreatedHasErrorsOnEnvironmentVariables() throws GitAPIException {
        PipelineConfig pipeline = GoConfigMother.createPipelineConfigWithMaterialConfig(UUID.randomUUID().toString(), new DependencyMaterialConfig(pipelineConfig.name(), pipelineConfig.first().name()));
        pipeline.addEnvironmentVariable("", "PipelineEnvVar");

        EnvironmentVariablesConfig stageVariables = new EnvironmentVariablesConfig();
        EnvironmentVariableConfig stageVar = new EnvironmentVariableConfig("", "StageEnvVar");
        stageVariables.add(stageVar);


        EnvironmentVariablesConfig jobVariables = new EnvironmentVariablesConfig();
        EnvironmentVariableConfig jobVar = new EnvironmentVariableConfig("", "JobEnvVar");
        jobVariables.add(jobVar);

        StageConfig stageConfig = pipeline.get(0);
        stageConfig.setVariables(stageVariables);

        JobConfig jobConfig = stageConfig.getJobs().get(0);
        jobConfig.setVariables(jobVariables);

        pipelineConfigService.createPipelineConfig(user, pipeline, result, groupName);

        assertThat(result.toString(), result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(422));
        assertThat(pipeline.getVariables().get(0).errors().firstError(), is(String.format("Environment Variable cannot have an empty name for pipeline '" + pipeline.name() + "'.", pipeline.name())));
        assertThat(stageVar.errors().firstError(), is(String.format("Environment Variable cannot have an empty name for stage 'stage'.", pipeline.name())));
        assertThat(jobVar.errors().firstError(), is(String.format("Environment Variable cannot have an empty name for job 'job'.", pipeline.name())));
    }

    @Test
    public void shouldShowThePipelineConfigErrorMessageWhenPipelineBeingCreatedHasErrorsOnParameters() throws GitAPIException {
        PipelineConfig pipeline = GoConfigMother.createPipelineConfigWithMaterialConfig(UUID.randomUUID().toString(), new DependencyMaterialConfig(pipelineConfig.name(), pipelineConfig.first().name()));
        ParamConfig param = new ParamConfig("", "Foo");
        pipeline.addParam(param);

        pipelineConfigService.createPipelineConfig(user, pipeline, result, groupName);

        assertThat(result.toString(), result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(422));
        assertThat(param.errors().firstError(), is(String.format("Parameter cannot have an empty name for pipeline '" + pipeline.name() + "'.", pipeline.name())));
    }

    @Test
    public void shouldShowThePipelineConfigErrorMessageWhenPipelineBeingCreatedHasErrorsOnTrackingTool() throws GitAPIException {
        PipelineConfig pipeline = GoConfigMother.createPipelineConfigWithMaterialConfig(UUID.randomUUID().toString(), new DependencyMaterialConfig(pipelineConfig.name(), pipelineConfig.first().name()));
        TrackingTool trackingTool = new TrackingTool();
        pipeline.setTrackingTool(trackingTool);

        pipelineConfigService.createPipelineConfig(user, pipeline, result, groupName);

        assertThat(result.toString(), result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(422));
        assertThat(trackingTool.errors().firstError(), is(String.format("Regex should be populated", pipeline.name())));
    }

    @Test
    public void shouldShowThePipelineConfigErrorMessageWhenPipelineBeingCreatedHasErrorsOnArtifactPlans() throws GitAPIException {
        PipelineConfig pipeline = GoConfigMother.createPipelineConfigWithMaterialConfig(UUID.randomUUID().toString(), new DependencyMaterialConfig(pipelineConfig.name(), pipelineConfig.first().name()));
        JobConfig jobConfig = pipeline.get(0).getJobs().get(0);
        ArtifactPlans artifactPlans = new ArtifactPlans();
        ArtifactPlan artifactPlan = new ArtifactPlan("", "/foo");
        artifactPlans.add(artifactPlan);
        jobConfig.setArtifactPlans(artifactPlans);

        pipelineConfigService.createPipelineConfig(user, pipeline, result, groupName);

        assertThat(result.toString(), result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(422));
        assertThat(artifactPlan.errors().firstError(), is(String.format("Job 'job' has an artifact with an empty source", pipeline.name())));
    }

    @Test
    public void shouldShowThePipelineConfigErrorMessageWhenPipelineBeingCreatedHasErrorsOnTimer() throws GitAPIException {
        PipelineConfig pipeline = GoConfigMother.createPipelineConfigWithMaterialConfig(UUID.randomUUID().toString(), new DependencyMaterialConfig(pipelineConfig.name(), pipelineConfig.first().name()));
        TimerConfig timer = new TimerConfig(null, true);
        pipeline.setTimer(timer);

        pipelineConfigService.createPipelineConfig(user, pipeline, result, groupName);

        assertThat(result.toString(), result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(422));
        assertThat(timer.errors().firstError(), is("Timer Spec can not be null."));
    }

    @Test
    public void shouldShowThePipelineConfigErrorMessageWhenPipelineBeingCreatedHasErrorsOnProperties() throws GitAPIException {
        PipelineConfig pipeline = GoConfigMother.createPipelineConfigWithMaterialConfig(UUID.randomUUID().toString(), new DependencyMaterialConfig(pipelineConfig.name(), pipelineConfig.first().name()));
        JobConfig jobConfig = pipeline.get(0).getJobs().get(0);
        ArtifactPropertiesGenerators properties = new ArtifactPropertiesGenerators();
        ArtifactPropertiesGenerator artifactPropertiesGenerator = new ArtifactPropertiesGenerator();
        properties.add(artifactPropertiesGenerator);
        jobConfig.setProperties(properties);

        pipelineConfigService.createPipelineConfig(user, pipeline, result, groupName);

        assertThat(result.toString(), result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(422));
        assertThat(artifactPropertiesGenerator.errors().firstError(), is(String.format("Invalid property name 'null'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.", pipeline.name())));
    }

    @Test
    public void shouldShowThePipelineConfigErrorMessageWhenPipelineBeingCreatedHasErrorsOnTabs() throws GitAPIException {
        PipelineConfig pipeline = GoConfigMother.createPipelineConfigWithMaterialConfig(UUID.randomUUID().toString(), new DependencyMaterialConfig(pipelineConfig.name(), pipelineConfig.first().name()));
        JobConfig jobConfig = pipeline.get(0).getJobs().get(0);
        jobConfig.addTab("", "/foo");

        pipelineConfigService.createPipelineConfig(user, pipeline, result, groupName);

        assertThat(result.toString(), result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(422));
        assertThat(jobConfig.getTabs().first().errors().firstError(), is(String.format("Tab name '' is invalid. This must be alphanumeric and can contain underscores and periods.", pipeline.name())));
    }

    @Test
    public void shouldShowThePipelineConfigErrorMessageWhenPipelineBeingCreatedFromTemplateHasErrors() throws GitAPIException {
        JobConfigs jobConfigs = new JobConfigs();
        jobConfigs.add(new JobConfig(new CaseInsensitiveString("Job")));
        StageConfig stage = new StageConfig(new CaseInsensitiveString("Stage-1"), jobConfigs);
        final PipelineTemplateConfig templateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("foo"), stage);
        goConfigDao.updateConfig(new UpdateConfigCommand() {
            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                cruiseConfig.addTemplate(templateConfig);
                return cruiseConfig;
            }
        });

        PipelineConfig pipeline = GoConfigMother.createPipelineConfigWithMaterialConfig();
        pipeline.templatize(templateConfig.name());
        DependencyMaterialConfig material = new DependencyMaterialConfig(new CaseInsensitiveString("Invalid-pipeline"), new CaseInsensitiveString("Stage"));
        pipeline.addMaterialConfig(material);
        pipelineConfigService.createPipelineConfig(user, pipeline, result, groupName);

        assertThat(result.toString(), result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(422));
        assertThat(material.errors().firstError(), is(String.format("Pipeline with name 'Invalid-pipeline' does not exist, it is defined as a dependency for pipeline 'pipeline' (cruise-config.xml)", pipeline.name())));
    }

    @Test
    public void shouldShowThePipelineConfigErrorMessageWhenPipelineBeingUpdatedHasErrors() throws GitAPIException {
        ExecTask execTask = new ExecTask("ls", "-al", "#{foo}");
        FetchTask fetchTask = new FetchTask(pipelineConfig.name(), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "srcfile", "/usr/dest");
        JobConfig job = new JobConfig("default-job");
        job.addTask(execTask);
        job.addTask(fetchTask);
        StageConfig stage = new StageConfig(new CaseInsensitiveString("default-stage"), new JobConfigs(job));

        String xml = new MagicalGoConfigXmlWriter(configCache, registry).toXmlPartial(pipelineConfig);
        String md5 = CachedDigestUtils.md5Hex(xml);

        pipelineConfig.add(stage);
        pipelineConfig.addParam(new ParamConfig("foo", "."));

        pipelineConfigService.updatePipelineConfig(user, pipelineConfig, md5, result);

        assertThat(result.toString(), result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(422));
        String expectedError = String.format("Task of job 'default-job' in stage 'default-stage' of pipeline '%s' has dest path '/usr/dest' which is outside the working directory.", pipelineConfig.name());
        assertThat(fetchTask.errors().on("dest"), is(expectedError));
    }

    @Test
    public void shouldUpdatePipelineConfig() throws GitAPIException {
        GoConfigHolder goConfigHolderBeforeUpdate = goConfigDao.loadConfigHolder();

        String xml = new MagicalGoConfigXmlWriter(configCache, registry).toXmlPartial(pipelineConfig);
        String md5 = CachedDigestUtils.md5Hex(xml);
        pipelineConfig.add(new StageConfig(new CaseInsensitiveString("additional_stage"), new JobConfigs(new JobConfig(new CaseInsensitiveString("addtn_job")))));

        pipelineConfigService.updatePipelineConfig(user, pipelineConfig, md5, result);

        assertThat(result.toString(), result.isSuccessful(), is(true));
        assertThat(goConfigDao.loadConfigHolder(), is(not(goConfigHolderBeforeUpdate)));
        StageConfig newlyAddedStage = goConfigDao.loadForEditing().getPipelineConfigByName(pipelineConfig.name()).getStage(new CaseInsensitiveString("additional_stage"));
        assertThat(newlyAddedStage, is(not(nullValue())));
        assertThat(newlyAddedStage.getJobs().isEmpty(), is(false));
        assertThat(newlyAddedStage.getJobs().first().name().toString(), is("addtn_job"));
        assertThat(configRepository.getCurrentRevCommit().name(), is(not(headCommitBeforeUpdate)));
        assertThat(configRepository.getCurrentRevision().getUsername(), is(user.getDisplayName()));
    }

    @Test
    public void shouldNotUpdatePipelineConfigInCaseOfValidationErrors() throws GitAPIException {
        GoConfigHolder goConfigHolder = goConfigDao.loadConfigHolder();
        String xml = new MagicalGoConfigXmlWriter(configCache, registry).toXmlPartial(pipelineConfig);
        String md5 = CachedDigestUtils.md5Hex(xml);

        pipelineConfig.setLabelTemplate("LABEL");
        pipelineConfigService.updatePipelineConfig(user, pipelineConfig, md5, result);

        assertThat(result.toString(), result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(422));
        assertThat(pipelineConfig.errors().on(PipelineConfig.LABEL_TEMPLATE), contains("Invalid label"));
        assertThat(configRepository.getCurrentRevCommit().name(), is(headCommitBeforeUpdate));
        assertThat(goConfigDao.loadConfigHolder().configForEdit, is(goConfigHolder.configForEdit));
        assertThat(goConfigDao.loadConfigHolder().config, is(goConfigHolder.config));
    }

    @Test
    public void shouldNotUpdatePipelineWhenPreprocessingFails() throws Exception {
        CaseInsensitiveString templateName = new CaseInsensitiveString("template_with_param");
        saveTemplateWithParamToConfig(templateName);

        GoConfigHolder goConfigHolder = goConfigDao.loadConfigHolder();
        String xml = new MagicalGoConfigXmlWriter(configCache, registry).toXmlPartial(pipelineConfig);
        String md5 = CachedDigestUtils.md5Hex(xml);
        pipelineConfig.clear();
        pipelineConfig.setTemplateName(templateName);
        pipelineConfigService.updatePipelineConfig(user, pipelineConfig, md5, result);

        assertThat(result.toString(), result.isSuccessful(), is(false));
        assertThat(result.toString(), result.toString().contains("Parameter 'SOME_PARAM' is not defined"), is(true));
        assertThat(configRepository.getCurrentRevCommit().name(), is(headCommitBeforeUpdate));
        assertThat(goConfigDao.loadConfigHolder().configForEdit, is(goConfigHolder.configForEdit));
        assertThat(goConfigDao.loadConfigHolder().config, is(goConfigHolder.config));
    }

    @Test
    public void shouldNotUpdatePipelineWhenPipelineIsAssociatedWithTemplateAsWellAsHasStagesDefinedLocally() throws Exception {
        CaseInsensitiveString templateName = new CaseInsensitiveString("template_with_param");
        saveTemplateWithParamToConfig(templateName);

        GoConfigHolder goConfigHolder = goConfigDao.loadConfigHolder();
        String xml = new MagicalGoConfigXmlWriter(configCache, registry).toXmlPartial(pipelineConfig);
        String md5 = CachedDigestUtils.md5Hex(xml);
        pipelineConfig.clear();
        pipelineConfig.setTemplateName(templateName);
        pipelineConfig.addStageWithoutValidityAssertion(StageConfigMother.stageConfig("local-stage"));
        pipelineConfigService.updatePipelineConfig(user, pipelineConfig, md5, result);

        assertThat(result.toString(), result.isSuccessful(), is(false));
        assertThat(pipelineConfig.errors().on("stages"), is(String.format("Cannot add stages to pipeline '%s' which already references template '%s'", pipelineConfig.name(), templateName)));
        assertThat(pipelineConfig.errors().on("template"), is(String.format("Cannot set template '%s' on pipeline '%s' because it already has stages defined", templateName, pipelineConfig.name())));
        assertThat(configRepository.getCurrentRevCommit().name(), is(headCommitBeforeUpdate));
        assertThat(goConfigDao.loadConfigHolder().configForEdit, is(goConfigHolder.configForEdit));
        assertThat(goConfigDao.loadConfigHolder().config, is(goConfigHolder.config));
    }

    @Test
    public void shouldCheckForUserPermissionBeforeUpdatingPipelineConfig() throws Exception {
        CaseInsensitiveString templateName = new CaseInsensitiveString("template_with_param");
        saveTemplateWithParamToConfig(templateName);

        GoConfigHolder goConfigHolderBeforeUpdate = goConfigDao.loadConfigHolder();
        pipelineConfigService.updatePipelineConfig(new Username(new CaseInsensitiveString("unauthorized_user")), pipelineConfig, null, result);

        assertThat(result.toString(), result.isSuccessful(), is(false));
        assertThat(result.toString(), result.httpCode(), is(401));
        assertThat(result.toString(), result.toString().contains("UNAUTHORIZED_TO_EDIT_PIPELINE"), is(true));
        assertThat(configRepository.getCurrentRevCommit().name(), is(headCommitBeforeUpdate));
        assertThat(goConfigDao.loadConfigHolder().configForEdit, is(goConfigHolderBeforeUpdate.configForEdit));
        assertThat(goConfigDao.loadConfigHolder().config, is(goConfigHolderBeforeUpdate.config));
    }

    @Test
    public void shouldMapErrorsBackToScmMaterials() throws Exception {
        GoConfigHolder goConfigHolder = goConfigDao.loadConfigHolder();
        String scmid = "scmid";
        saveScmMaterialToConfig(scmid);
        PluggableSCMMaterialConfig scmMaterialConfig = new PluggableSCMMaterialConfig(scmid);
        String xml = new MagicalGoConfigXmlWriter(configCache, registry).toXmlPartial(pipelineConfig);
        String md5 = CachedDigestUtils.md5Hex(xml);

        pipelineConfig.materialConfigs().add(scmMaterialConfig);
        pipelineConfigService.updatePipelineConfig(user, pipelineConfig, md5, result);

        assertThat(result.toString(), result.isSuccessful(), is(false));
        assertThat(scmMaterialConfig.errors().on(PluggableSCMMaterialConfig.FOLDER), is("Destination directory is required when specifying multiple scm materials"));
        assertThat(scmMaterialConfig.errors().on(PluggableSCMMaterialConfig.SCM_ID), is("Could not find plugin for scm-id: [scmid]."));
        assertThat(configRepository.getCurrentRevCommit().name(), is(headCommitBeforeUpdate));
        assertThat(goConfigDao.loadConfigHolder().configForEdit, is(goConfigHolder.configForEdit));
        assertThat(goConfigDao.loadConfigHolder().config, is(goConfigHolder.config));
    }

    @Test
    public void shouldMapErrorsBackToPackageMaterials() throws Exception {
        GoConfigHolder goConfigHolder = goConfigDao.loadConfigHolder();
        String packageid = "packageid";
        saveScmMaterialToConfig(packageid);
        PackageMaterialConfig packageMaterialConfig = new PackageMaterialConfig(packageid);
        String xml = new MagicalGoConfigXmlWriter(configCache, registry).toXmlPartial(pipelineConfig);
        String md5 = CachedDigestUtils.md5Hex(xml);
        pipelineConfig.materialConfigs().add(packageMaterialConfig);
        pipelineConfigService.updatePipelineConfig(user, pipelineConfig, md5, result);

        assertThat(result.toString(), result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is(String.format("Validations failed for pipeline '%s'. Error(s): [Validation failed.]. Please correct and resubmit.", pipelineConfig.name())));

        assertThat(packageMaterialConfig.errors().on(PackageMaterialConfig.PACKAGE_ID), is("Could not find repository for given package id:[packageid]"));
        assertThat(configRepository.getCurrentRevCommit().name(), is(headCommitBeforeUpdate));
        assertThat(goConfigDao.loadConfigHolder().configForEdit, is(goConfigHolder.configForEdit));
        assertThat(goConfigDao.loadConfigHolder().config, is(goConfigHolder.config));
    }

    @Test
    public void shouldDeletePipelineConfig() throws Exception {
        PipelineConfig pipeline = PipelineConfigMother.createPipelineConfigWithStages(UUID.randomUUID().toString(), "stage");
        goConfigService.addPipeline(pipeline, "default");
        assertTrue(goConfigService.hasPipelineNamed(pipeline.name()));

        int pipelineCountBefore = goConfigService.getAllPipelineConfigs().size();
        pipelineConfigService.deletePipelineConfig(user, pipeline, result);

        assertTrue(result.isSuccessful());
        int pipelineCountAfter = goConfigService.getAllPipelineConfigs().size();
        assertThat(pipelineCountBefore - pipelineCountAfter, is(1));
        assertFalse(goConfigService.hasPipelineNamed(pipeline.name()));
    }

    @Test
    public void shouldNotDeleteThePipelineForUnauthorizedUsers() throws Exception {
        int pipelineCountBefore = goConfigService.getAllPipelineConfigs().size();
        assertTrue(goConfigService.hasPipelineNamed(pipelineConfig.name()));

        pipelineConfigService.deletePipelineConfig(new Username(new CaseInsensitiveString("unauthorized-user")), pipelineConfig, result);

        assertFalse(result.isSuccessful());
        assertThat(result.toString(), result.toString().contains("UNAUTHORIZED_TO_DELETE_PIPELINE"), is(true));
        assertThat(result.httpCode(), is(401));
        int pipelineCountAfter = goConfigService.getAllPipelineConfigs().size();
        assertThat(pipelineCountAfter, is(pipelineCountBefore));
        assertTrue(goConfigService.hasPipelineNamed(pipelineConfig.name()));
    }

    @Test
    public void shouldNotDeletePipelineConfigWhenItIsUsedInAnEnvironment() throws Exception {
        BasicEnvironmentConfig env = new BasicEnvironmentConfig(new CaseInsensitiveString("Dev"));
        PipelineConfig pipeline = PipelineConfigMother.createPipelineConfigWithStages(UUID.randomUUID().toString(), "stage");
        goConfigService.addPipeline(pipeline, "default");
        env.addPipeline(pipeline.name());
        goConfigService.addEnvironment(env);

        int pipelineCountBefore = goConfigService.getAllPipelineConfigs().size();
        assertTrue(goConfigService.hasPipelineNamed(pipeline.name()));

        pipelineConfigService.deletePipelineConfig(user, pipeline, result);

        assertFalse(result.isSuccessful());
        assertThat(result.toString(), result.toString().contains("CANNOT_DELETE_PIPELINE_IN_ENVIRONMENT"), is(true));
        assertThat(result.httpCode(), is(422));
        int pipelineCountAfter = goConfigService.getAllPipelineConfigs().size();
        assertThat(pipelineCountAfter, is(pipelineCountBefore));
        assertTrue(goConfigService.hasPipelineNamed(pipeline.name()));
    }

    @Test
    public void shouldNotDeletePipelineConfigWhenItHasDownstreamDependencies() throws Exception {
        PipelineConfig dependency = GoConfigMother.createPipelineConfigWithMaterialConfig(new DependencyMaterialConfig(pipelineConfig.name(), pipelineConfig.first().name()));
        goConfigService.addPipeline(dependency, groupName);

        int pipelineCountBefore = goConfigService.getAllPipelineConfigs().size();
        assertTrue(goConfigService.hasPipelineNamed(pipelineConfig.name()));

        pipelineConfigService.deletePipelineConfig(user, pipelineConfig, result);

        assertFalse(result.isSuccessful());
        assertThat(result.toString(), result.toString().contains("CANNOT_DELETE_PIPELINE_USED_AS_MATERIALS"), is(true));
        assertThat(result.httpCode(), is(422));
        int pipelineCountAfter = goConfigService.getAllPipelineConfigs().size();
        assertThat(pipelineCountAfter, is(pipelineCountBefore));
        assertTrue(goConfigService.hasPipelineNamed(pipelineConfig.name()));
    }

    @Test
    public void shouldNotifyListenersWithPreprocessedConfigUponSuccessfulUpdate() {
        final String pipelineName = UUID.randomUUID().toString();
        final String templateName = UUID.randomUUID().toString();
        final boolean[] listenerInvoked = {false};
        setupPipelineWithTemplate(pipelineName, templateName);
        PipelineConfig pipelineConfig1 = goConfigService.pipelineConfigNamed(new CaseInsensitiveString(pipelineName));
        String xml = new MagicalGoConfigXmlWriter(configCache, registry).toXmlPartial(pipelineConfig1);
        String md5 = CachedDigestUtils.md5Hex(xml);
        EntityConfigChangedListener<PipelineConfig> pipelineConfigChangedListener = new EntityConfigChangedListener<PipelineConfig>() {
            @Override
            public void onConfigChange(CruiseConfig newCruiseConfig) {
            }

            @Override
            public void onEntityConfigChange(PipelineConfig pipelineConfig) {
                listenerInvoked[0] = true;
                assertThat(pipelineConfig.first(), is(goConfigService.cruiseConfig().getTemplateByName(new CaseInsensitiveString(templateName)).first()));
            }
        };
        goConfigService.register(pipelineConfigChangedListener);
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfigWithTemplate(pipelineName, templateName);
        pipeline.setVariables(new EnvironmentVariablesConfig());
        pipelineConfigService.updatePipelineConfig(user, pipeline, md5, new DefaultLocalizedOperationResult());
        assertThat(listenerInvoked[0], is(true));
    }

    @Test
    public void shouldNotifyListenersWithPreprocessedConfigUponSuccessfulCreate() {
        final String pipelineName = UUID.randomUUID().toString();
        final String templateName = UUID.randomUUID().toString();
        final boolean[] listenerInvoked = {false};
        setupPipelineWithTemplate(pipelineName, templateName);

        EntityConfigChangedListener<PipelineConfig> pipelineConfigChangedListener = new EntityConfigChangedListener<PipelineConfig>() {
            @Override
            public void onConfigChange(CruiseConfig newCruiseConfig) {
            }

            @Override
            public void onEntityConfigChange(PipelineConfig pipelineConfig) {
                listenerInvoked[0] = true;
                assertThat(pipelineConfig.first(), is(goConfigService.cruiseConfig().getTemplateByName(new CaseInsensitiveString(templateName)).first()));
            }
        };
        goConfigService.register(pipelineConfigChangedListener);
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfigWithTemplate(UUID.randomUUID().toString(), templateName);
        pipeline.setVariables(new EnvironmentVariablesConfig());
        pipelineConfigService.createPipelineConfig(user, pipeline, new DefaultLocalizedOperationResult(), "group1");
        assertThat(listenerInvoked[0], is(true));
    }

    private void setupPipelineWithTemplate(final String pipelineName, final String templateName) {
        goConfigService.updateConfig(new UpdateConfigCommand() {
            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplate(templateName);
                PipelineConfig pipeline = PipelineConfigMother.pipelineConfigWithTemplate(pipelineName, template.name().toString());
                cruiseConfig.addTemplate(template);
                cruiseConfig.addPipeline("group", pipeline);
                return cruiseConfig;
            }
        });
    }

    @Test
    public void shouldValidateMergedConfigForConfigChanges() throws Exception {
        assertThat(goConfigService.getCurrentConfig().getAllPipelineNames().contains(new CaseInsensitiveString(remoteDownstreamPipelineName)), is(true));
        String xml = new MagicalGoConfigXmlWriter(configCache, registry).toXmlPartial(pipelineConfig);
        String md5 = CachedDigestUtils.md5Hex(xml);

        pipelineConfig.getFirstStageConfig().setName(new CaseInsensitiveString("upstream_stage_renamed"));

        pipelineConfigService.updatePipelineConfig(user, pipelineConfig, md5, result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(pipelineConfig.errors().on("base"), is(String.format("Stage with name 'stage' does not exist on pipeline '%s', it is being referred to from pipeline 'remote-downstream' (url at repo1_r1)", pipelineConfig.name())));
        assertThat(result.message(localizer), is(String.format("Validations failed for pipeline '%s'. Error(s): [Validation failed.]. Please correct and resubmit.", pipelineConfig.name())));
    }

    @Test
    public void shouldFallbackToValidPartialsForConfigChanges() throws Exception {
        assertThat(goConfigService.getCurrentConfig().getAllPipelineNames().contains(new CaseInsensitiveString(remoteDownstreamPipelineName)), is(true));

        String remoteInvalidPipeline = "remote_invalid_pipeline";
        PartialConfig invalidPartial = PartialConfigMother.invalidPartial(remoteInvalidPipeline, new RepoConfigOrigin(repoConfig1, "repo1_r2"));
        goPartialConfig.onSuccessPartialConfig(repoConfig1, invalidPartial);
        assertThat(goConfigService.getCurrentConfig().getAllPipelineNames().contains(new CaseInsensitiveString(remoteInvalidPipeline)), is(false));
        assertThat(goConfigService.getCurrentConfig().getAllPipelineNames().contains(new CaseInsensitiveString(remoteDownstreamPipelineName)), is(true));

        String xml = new MagicalGoConfigXmlWriter(configCache, registry).toXmlPartial(pipelineConfig);
        String md5 = CachedDigestUtils.md5Hex(xml);

        pipelineConfig.getFirstStageConfig().getJobs().first().addTask(new ExecTask("executable", new Arguments(new Argument("foo")), "working"));
        pipelineConfigService.updatePipelineConfig(user, pipelineConfig, md5, result);

        assertThat(result.isSuccessful(), is(true));
        CruiseConfig currentConfig = goConfigService.getCurrentConfig();
        assertThat(currentConfig.getAllPipelineNames().contains(new CaseInsensitiveString(remoteDownstreamPipelineName)), is(true));
        assertThat(currentConfig.getAllPipelineNames().contains(new CaseInsensitiveString(remoteInvalidPipeline)), is(false));
    }

    @Test
    public void shouldSaveWhenKnownPartialListIsTheSameAsValidPartialsAndValidationPassesForConfigChanges() throws Exception {
        PipelineConfig remoteDownstreamPipeline = partialConfig.getGroups().first().getPipelines().get(0);
        assertThat(goConfigService.getCurrentConfig().getAllPipelineNames().contains(remoteDownstreamPipeline.name()), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).isEmpty(), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).isEmpty(), is(true));

        DependencyMaterialConfig dependencyMaterialForRemotePipelineInConfigCache = goConfigService.getCurrentConfig().getPipelineConfigByName(remoteDownstreamPipeline.name()).materialConfigs().findDependencyMaterial(pipelineConfig.name());
        assertThat(dependencyMaterialForRemotePipelineInConfigCache.getStageName(), is(new CaseInsensitiveString("stage")));

        String xml = new MagicalGoConfigXmlWriter(configCache, registry).toXmlPartial(pipelineConfig);
        String md5 = CachedDigestUtils.md5Hex(xml);

        pipelineConfig.setVariables(new EnvironmentVariablesConfig(asList(new EnvironmentVariableConfig("key", "value"))));
        pipelineConfigService.updatePipelineConfig(user, pipelineConfig, md5, result);

        assertThat(result.isSuccessful(), is(true));
        CruiseConfig currentConfig = goConfigService.getCurrentConfig();
        assertThat(currentConfig.getAllPipelineNames().contains(remoteDownstreamPipeline.name()), is(true));
        assertThat(currentConfig.getPipelineConfigByName(pipelineConfig.name()).getVariables().contains(new EnvironmentVariableConfig("key", "value")), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).isEmpty(), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).isEmpty(), is(true));
    }

    @Test
    public void shouldNotSaveWhenKnownPartialsListIsTheSameAsValidPartialsAndPipelineValidationFailsForConfigChanges() throws Exception {
        PipelineConfig remoteDownstreamPipeline = partialConfig.getGroups().first().getPipelines().get(0);
        assertThat(goConfigService.getCurrentConfig().getAllPipelineNames().contains(remoteDownstreamPipeline.name()), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).isEmpty(), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).isEmpty(), is(true));

        DependencyMaterialConfig dependencyMaterialForRemotePipelineInConfigCache = goConfigService.getCurrentConfig().getPipelineConfigByName(remoteDownstreamPipeline.name()).materialConfigs().findDependencyMaterial(pipelineConfig.name());
        assertThat(dependencyMaterialForRemotePipelineInConfigCache.getStageName(), is(new CaseInsensitiveString("stage")));

        String xml = new MagicalGoConfigXmlWriter(configCache, registry).toXmlPartial(pipelineConfig);
        String md5 = CachedDigestUtils.md5Hex(xml);

        pipelineConfig.getFirstStageConfig().setName(new CaseInsensitiveString("new_name"));
        pipelineConfigService.updatePipelineConfig(user, pipelineConfig, md5, result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is(String.format("Validations failed for pipeline '%s'. Error(s): [Validation failed.]. Please correct and resubmit.", pipelineConfig.name())));
        CruiseConfig currentConfig = goConfigService.getCurrentConfig();
        assertThat(currentConfig.getAllPipelineNames().contains(remoteDownstreamPipeline.name()), is(true));
        assertThat(cachedGoPartials.lastValidPartials().contains(partialConfig), is(true));
        assertThat(cachedGoPartials.lastKnownPartials().contains(partialConfig), is(true));
        assertThat(cachedGoPartials.lastKnownPartials().equals(cachedGoPartials.lastValidPartials()), is(true));
        assertThat(currentConfig.getPipelineConfigByName(remoteDownstreamPipeline.name()).materialConfigs().findDependencyMaterial(pipelineConfig.name()).getStageName(), is(new CaseInsensitiveString("stage")));
        assertThat(currentConfig.getPipelineConfigByName(pipelineConfig.name()).getFirstStageConfig().name(), is(new CaseInsensitiveString("stage")));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).isEmpty(), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).isEmpty(), is(true));
    }

    @Test
    public void shouldSaveWhenKnownNotEqualsValidPartialsAndPipelineValidationPassesWhenValidPartialsAreMergedToMain() throws Exception {
        PipelineConfig remoteDownstreamPipeline = partialConfig.getGroups().first().getPipelines().get(0);
        assertThat(goConfigService.getCurrentConfig().getAllPipelineNames().contains(remoteDownstreamPipeline.name()), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).isEmpty(), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).isEmpty(), is(true));

        partialConfig = PartialConfigMother.invalidPartial(remoteDownstreamPipeline.name().toString(), new RepoConfigOrigin(repoConfig1, "repo1_r2"));
        goPartialConfig.onSuccessPartialConfig(repoConfig1, partialConfig);
        CruiseConfig currentConfig = goConfigService.getCurrentConfig();
        assertThat(currentConfig.getAllPipelineNames().contains(remoteDownstreamPipeline.name()), is(true));
        assertThat(((RepoConfigOrigin) currentConfig.getPipelineConfigByName(remoteDownstreamPipeline.name()).getOrigin()).getRevision(), is("repo1_r1"));
        assertThat(((RepoConfigOrigin) cachedGoPartials.getValid(repoConfig1.getMaterialConfig().getFingerprint()).getOrigin()).getRevision(), is("repo1_r1"));
        assertThat(((RepoConfigOrigin) cachedGoPartials.getKnown(repoConfig1.getMaterialConfig().getFingerprint()).getOrigin()).getRevision(), is("repo1_r2"));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).size(), is(1));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).get(0).getMessage(), is("Invalid Merged Configuration"));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).get(0).getDescription(), is("1+ errors :: Invalid stage name ''. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.;;  -  Config-Repo: url at repo1_r2"));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).isEmpty(), is(true));

        String xml = new MagicalGoConfigXmlWriter(configCache, registry).toXmlPartial(pipelineConfig);
        String md5 = CachedDigestUtils.md5Hex(xml);
        pipelineConfig.setVariables(new EnvironmentVariablesConfig(asList(new EnvironmentVariableConfig("key", "value"))));
        pipelineConfigService.updatePipelineConfig(user, pipelineConfig, md5, result);

        assertThat(result.isSuccessful(), is(true));
        currentConfig = goConfigService.getCurrentConfig();
        assertThat(currentConfig.getAllPipelineNames().contains(remoteDownstreamPipeline.name()), is(true));
        assertThat(currentConfig.getPipelineConfigByName(pipelineConfig.name()).getVariables().contains(new EnvironmentVariableConfig("key", "value")), is(true));
        assertThat(((RepoConfigOrigin) currentConfig.getPipelineConfigByName(remoteDownstreamPipeline.name()).getOrigin()).getRevision(), is("repo1_r1"));
        assertThat(((RepoConfigOrigin) cachedGoPartials.getValid(repoConfig1.getMaterialConfig().getFingerprint()).getOrigin()).getRevision(), is("repo1_r1"));
        assertThat(((RepoConfigOrigin) cachedGoPartials.getKnown(repoConfig1.getMaterialConfig().getFingerprint()).getOrigin()).getRevision(), is("repo1_r2"));

        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).size(), is(1));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).get(0).getMessage(), is("Invalid Merged Configuration"));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).get(0).getDescription(), is("1+ errors :: Invalid stage name ''. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.;;  -  Config-Repo: url at repo1_r2"));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).isEmpty(), is(true));
    }

    @Test
    public void shouldSaveWhenKnownNotEqualsValidPartialsAndPipelineValidationFailsWithValidPartialsButPassesWhenKnownPartialsAreMergedToMain() throws Exception {
        PipelineConfig remoteDownstreamPipeline = partialConfig.getGroups().first().getPipelines().get(0);
        assertThat(goConfigService.getCurrentConfig().getAllPipelineNames().contains(remoteDownstreamPipeline.name()), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).isEmpty(), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).isEmpty(), is(true));

        final CaseInsensitiveString upstreamStageRenamed = new CaseInsensitiveString("upstream_stage_renamed");
        partialConfig = PartialConfigMother.pipelineWithDependencyMaterial("remote-downstream", new PipelineConfig(pipelineConfig.name(), pipelineConfig.materialConfigs(), new StageConfig(upstreamStageRenamed, new JobConfigs())), new RepoConfigOrigin(repoConfig1, "repo1_r2"));
        goPartialConfig.onSuccessPartialConfig(repoConfig1, partialConfig);
        CruiseConfig currentConfig = goConfigService.getCurrentConfig();
        DependencyMaterialConfig dependencyMaterialForRemotePipelineInConfigCache = currentConfig.getPipelineConfigByName(remoteDownstreamPipeline.name()).materialConfigs().findDependencyMaterial(pipelineConfig.name());
        assertThat(dependencyMaterialForRemotePipelineInConfigCache.getStageName(), is(new CaseInsensitiveString("stage")));
        assertThat(((RepoConfigOrigin) currentConfig.getPipelineConfigByName(remoteDownstreamPipeline.name()).getOrigin()).getRevision(), is("repo1_r1"));
        assertThat(((RepoConfigOrigin) cachedGoPartials.getValid(repoConfig1.getMaterialConfig().getFingerprint()).getOrigin()).getRevision(), is("repo1_r1"));
        assertThat(((RepoConfigOrigin) cachedGoPartials.getKnown(repoConfig1.getMaterialConfig().getFingerprint()).getOrigin()).getRevision(), is("repo1_r2"));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).size(), is(1));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).get(0).getMessage(), is("Invalid Merged Configuration"));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).get(0).getDescription(), is(String.format("1+ errors :: Stage with name 'upstream_stage_renamed' does not exist on pipeline '%s', it is being referred to from pipeline 'remote-downstream' (url at repo1_r2);;  -  Config-Repo: url at repo1_r2", pipelineConfig.name())));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).isEmpty(), is(true));

        String xml = new MagicalGoConfigXmlWriter(configCache, registry).toXmlPartial(pipelineConfig);
        String md5 = CachedDigestUtils.md5Hex(xml);
        pipelineConfig.getFirstStageConfig().setName(new CaseInsensitiveString("upstream_stage_renamed"));
        pipelineConfigService.updatePipelineConfig(user, pipelineConfig, md5, result);

        assertThat(result.isSuccessful(), is(true));
        currentConfig = goConfigService.getCurrentConfig();
        assertThat(currentConfig.getAllPipelineNames().contains(remoteDownstreamPipeline.name()), is(true));
        assertThat(cachedGoPartials.lastValidPartials().contains(partialConfig), is(true));
        assertThat(cachedGoPartials.lastKnownPartials().contains(partialConfig), is(true));
        assertThat(cachedGoPartials.lastKnownPartials().equals(cachedGoPartials.lastValidPartials()), is(true));
        assertThat(currentConfig.getPipelineConfigByName(remoteDownstreamPipeline.name()).materialConfigs().findDependencyMaterial(pipelineConfig.name()).getStageName(), is(upstreamStageRenamed));
        assertThat(currentConfig.getPipelineConfigByName(pipelineConfig.name()).getFirstStageConfig().name(), is(upstreamStageRenamed));
        assertThat(((RepoConfigOrigin) currentConfig.getPipelineConfigByName(remoteDownstreamPipeline.name()).getOrigin()).getRevision(), is("repo1_r2"));
        assertThat(((RepoConfigOrigin) cachedGoPartials.getValid(repoConfig1.getMaterialConfig().getFingerprint()).getOrigin()).getRevision(), is("repo1_r2"));
        assertThat(((RepoConfigOrigin) cachedGoPartials.getKnown(repoConfig1.getMaterialConfig().getFingerprint()).getOrigin()).getRevision(), is("repo1_r2"));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).isEmpty(), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).isEmpty(), is(true));
    }

    @Test
    public void shouldPerformFullValidationNotJustEntitySpecificIfMergingKnownPartialsAsOtherAspectsOfAKnownPartialMightBeInvalid() throws Exception {
        PipelineConfig remoteDownstreamPipeline = partialConfig.getGroups().first().getPipelines().get(0);
        assertThat(goConfigService.getCurrentConfig().getAllPipelineNames().contains(remoteDownstreamPipeline.name()), is(true));
        String independentRemotePipeline = "independent-pipeline";
        assertThat(goConfigService.getCurrentConfig().getAllPipelineNames().contains(new CaseInsensitiveString(independentRemotePipeline)), is(true));

        //introduce an invalid change in the independent partial
        PartialConfig invalidIndependentPartial = PartialConfigMother.invalidPartial(independentRemotePipeline, new RepoConfigOrigin(repoConfig2, "repo2_r2"));
        goPartialConfig.onSuccessPartialConfig(repoConfig2, invalidIndependentPartial);
        assertThat(((RepoConfigOrigin) cachedGoPartials.getValid(repoConfig2.getMaterialConfig().getFingerprint()).getOrigin()).getRevision(), is("repo2_r1"));
        assertThat(((RepoConfigOrigin) cachedGoPartials.getKnown(repoConfig2.getMaterialConfig().getFingerprint()).getOrigin()).getRevision(), is("repo2_r2"));
        assertThat(((RepoConfigOrigin) goConfigService.getCurrentConfig().getPipelineConfigByName(new CaseInsensitiveString(independentRemotePipeline)).getOrigin()).getRevision(), is("repo2_r1"));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).isEmpty(), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).size(), is(1));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).get(0).getMessage(), is("Invalid Merged Configuration"));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).get(0).getDescription(), is("1+ errors :: Invalid stage name ''. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.;;  -  Config-Repo: url2 at repo2_r2"));

        final CaseInsensitiveString upstreamStageRenamed = new CaseInsensitiveString("upstream_stage_renamed");
        partialConfig = PartialConfigMother.pipelineWithDependencyMaterial("remote-downstream", new PipelineConfig(pipelineConfig.name(), pipelineConfig.materialConfigs(), new StageConfig(upstreamStageRenamed, new JobConfigs())), new RepoConfigOrigin(repoConfig1, "repo1_r2"));
        goPartialConfig.onSuccessPartialConfig(repoConfig1, partialConfig);
        CruiseConfig currentConfig = goConfigService.getCurrentConfig();
        DependencyMaterialConfig dependencyMaterialForRemotePipelineInConfigCache = currentConfig.getPipelineConfigByName(remoteDownstreamPipeline.name()).materialConfigs().findDependencyMaterial(pipelineConfig.name());
        assertThat(dependencyMaterialForRemotePipelineInConfigCache.getStageName(), is(new CaseInsensitiveString("stage")));
        assertThat(((RepoConfigOrigin) currentConfig.getPipelineConfigByName(remoteDownstreamPipeline.name()).getOrigin()).getRevision(), is("repo1_r1"));
        assertThat(((RepoConfigOrigin) cachedGoPartials.getValid(repoConfig1.getMaterialConfig().getFingerprint()).getOrigin()).getRevision(), is("repo1_r1"));
        assertThat(((RepoConfigOrigin) cachedGoPartials.getKnown(repoConfig1.getMaterialConfig().getFingerprint()).getOrigin()).getRevision(), is("repo1_r2"));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).size(), is(1));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).get(0).getMessage(), is("Invalid Merged Configuration"));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).get(0).getDescription(), is(String.format("1+ errors :: Stage with name 'upstream_stage_renamed' does not exist on pipeline '%s', it is being referred to from pipeline 'remote-downstream' (url at repo1_r2);;  -  Config-Repo: url at repo1_r2", pipelineConfig.name())));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).size(), is(1));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).get(0).getMessage(), is("Invalid Merged Configuration"));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).get(0).getDescription(), is("1+ errors :: Invalid stage name ''. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.;;  -  Config-Repo: url2 at repo2_r2"));

        String xml = new MagicalGoConfigXmlWriter(configCache, registry).toXmlPartial(pipelineConfig);
        String md5 = CachedDigestUtils.md5Hex(xml);

        pipelineConfig.getFirstStageConfig().setName(new CaseInsensitiveString("upstream_stage_renamed"));
        pipelineConfigService.updatePipelineConfig(user, pipelineConfig, md5, result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is(String.format("Validations failed for pipeline '%s'. " +
                "Error(s): [Merged update operation failed on VALID 2 partials. Falling back to using LAST KNOWN 2 partials. " +
                "Exception message was: [Validation failed. Stage with name 'stage' does not exist on pipeline '%s', " +
                "it is being referred to from pipeline 'remote-downstream' (url at repo1_r1)]" +
                System.lineSeparator() +
                "Merged config update operation failed using fallback LAST KNOWN 2 partials. " +
                "Exception message was: 1+ errors :: Invalid stage name ''. This must be alphanumeric and can contain underscores and periods " +
                "(however, it cannot start with a period). The maximum allowed length is 255 characters.;; ]. Please correct and resubmit.", pipelineConfig.name(), pipelineConfig.name())));
        assertThat(ErrorCollector.getAllErrors(pipelineConfig).isEmpty(), is(true));
        currentConfig = goConfigService.getCurrentConfig();
        assertThat(currentConfig.getAllPipelineNames().contains(remoteDownstreamPipeline.name()), is(true));
        assertThat(cachedGoPartials.lastKnownPartials().equals(cachedGoPartials.lastValidPartials()), is(false));
        assertThat(currentConfig.getPipelineConfigByName(remoteDownstreamPipeline.name()).materialConfigs().findDependencyMaterial(pipelineConfig.name()).getStageName(), is(new CaseInsensitiveString("stage")));
        assertThat(currentConfig.getPipelineConfigByName(pipelineConfig.name()).getFirstStageConfig().name(), is(new CaseInsensitiveString("stage")));
        assertThat(((RepoConfigOrigin) currentConfig.getPipelineConfigByName(remoteDownstreamPipeline.name()).getOrigin()).getRevision(), is("repo1_r1"));
        assertThat(((RepoConfigOrigin) cachedGoPartials.getValid(repoConfig1.getMaterialConfig().getFingerprint()).getOrigin()).getRevision(), is("repo1_r1"));
        assertThat(((RepoConfigOrigin) cachedGoPartials.getKnown(repoConfig1.getMaterialConfig().getFingerprint()).getOrigin()).getRevision(), is("repo1_r2"));
        assertThat(((RepoConfigOrigin) currentConfig.getPipelineConfigByName(new CaseInsensitiveString(independentRemotePipeline)).getOrigin()).getRevision(), is("repo2_r1"));
        assertThat(((RepoConfigOrigin) cachedGoPartials.getValid(repoConfig2.getMaterialConfig().getFingerprint()).getOrigin()).getRevision(), is("repo2_r1"));
        assertThat(((RepoConfigOrigin) cachedGoPartials.getKnown(repoConfig2.getMaterialConfig().getFingerprint()).getOrigin()).getRevision(), is("repo2_r2"));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).size(), is(1));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).get(0).getMessage(), is("Invalid Merged Configuration"));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).get(0).getDescription(), is(String.format("1+ errors :: Stage with name 'upstream_stage_renamed' does not exist on pipeline '%s', it is being referred to from pipeline 'remote-downstream' (url at repo1_r2);;  -  Config-Repo: url at repo1_r2", pipelineConfig.name())));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).size(), is(1));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).get(0).getMessage(), is("Invalid Merged Configuration"));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).get(0).getDescription(), is("1+ errors :: Invalid stage name ''. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.;;  -  Config-Repo: url2 at repo2_r2"));
    }

    @Test
    public void shouldNotSaveWhenKnownNotEqualsValidPartialsAndPipelineValidationFailsWithValidPartialsAsWellAsKnownPartialsMergedToMain() throws Exception {
        PipelineConfig remoteDownstreamPipeline = partialConfig.getGroups().first().getPipelines().get(0);
        assertThat(goConfigService.getCurrentConfig().getAllPipelineNames().contains(remoteDownstreamPipeline.name()), is(true));

        final CaseInsensitiveString upstreamStageRenamed = new CaseInsensitiveString("upstream_stage_renamed");
        partialConfig = PartialConfigMother.pipelineWithDependencyMaterial("remote-downstream", new PipelineConfig(pipelineConfig.name(), pipelineConfig.materialConfigs(), new StageConfig(upstreamStageRenamed, new JobConfigs())), new RepoConfigOrigin(repoConfig1, "repo1_r2"));
        goPartialConfig.onSuccessPartialConfig(repoConfig1, partialConfig);
        CruiseConfig currentConfig = goConfigService.getCurrentConfig();
        DependencyMaterialConfig dependencyMaterialForRemotePipelineInConfigCache = currentConfig.getPipelineConfigByName(remoteDownstreamPipeline.name()).materialConfigs().findDependencyMaterial(pipelineConfig.name());
        assertThat(dependencyMaterialForRemotePipelineInConfigCache.getStageName(), is(new CaseInsensitiveString("stage")));
        assertThat(((RepoConfigOrigin) currentConfig.getPipelineConfigByName(remoteDownstreamPipeline.name()).getOrigin()).getRevision(), is("repo1_r1"));
        assertThat(((RepoConfigOrigin) cachedGoPartials.getValid(repoConfig1.getMaterialConfig().getFingerprint()).getOrigin()).getRevision(), is("repo1_r1"));
        assertThat(((RepoConfigOrigin) cachedGoPartials.getKnown(repoConfig1.getMaterialConfig().getFingerprint()).getOrigin()).getRevision(), is("repo1_r2"));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).size(), is(1));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).get(0).getMessage(), is("Invalid Merged Configuration"));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).get(0).getDescription(), is(String.format("1+ errors :: Stage with name 'upstream_stage_renamed' does not exist on pipeline '%s', it is being referred to from pipeline 'remote-downstream' (url at repo1_r2);;  -  Config-Repo: url at repo1_r2", pipelineConfig.name())));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).isEmpty(), is(true));

        String xml = new MagicalGoConfigXmlWriter(configCache, registry).toXmlPartial(pipelineConfig);
        String md5 = CachedDigestUtils.md5Hex(xml);

        pipelineConfig.getFirstStageConfig().setName(new CaseInsensitiveString("new_name"));
        pipelineConfigService.updatePipelineConfig(user, pipelineConfig, md5, result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is(String.format("Validations failed for pipeline '%s'. " +
                "Error(s): [Merged update operation failed on VALID 2 partials. Falling back to using LAST KNOWN 2 partials. " +
                "Exception message was: [Validation failed. Stage with name 'stage' does not exist on pipeline '%s', " +
                "it is being referred to from pipeline 'remote-downstream' (url at repo1_r1)]" +
                System.lineSeparator() +
                "Merged config update operation failed using fallback LAST KNOWN 2 partials. " +
                "Exception message was: 1+ errors :: Stage with name 'upstream_stage_renamed' does not exist on pipeline " +
                "'%s', it is being referred to from pipeline 'remote-downstream' (url at repo1_r2);; ]. " +
                "Please correct and resubmit.", pipelineConfig.name(), pipelineConfig.name(), pipelineConfig.name())));
        currentConfig = goConfigService.getCurrentConfig();
        assertThat(currentConfig.getAllPipelineNames().contains(remoteDownstreamPipeline.name()), is(true));
        assertThat(((RepoConfigOrigin) currentConfig.getPipelineConfigByName(remoteDownstreamPipeline.name()).getOrigin()).getRevision(), is("repo1_r1"));
        assertThat(((RepoConfigOrigin) cachedGoPartials.getValid(repoConfig1.getMaterialConfig().getFingerprint()).getOrigin()).getRevision(), is("repo1_r1"));
        assertThat(((RepoConfigOrigin) cachedGoPartials.getKnown(repoConfig1.getMaterialConfig().getFingerprint()).getOrigin()).getRevision(), is("repo1_r2"));
        assertThat(currentConfig.getPipelineConfigByName(remoteDownstreamPipeline.name()).materialConfigs().findDependencyMaterial(pipelineConfig.name()).getStageName(), is(new CaseInsensitiveString("stage")));
        assertThat(currentConfig.getPipelineConfigByName(pipelineConfig.name()).getFirstStageConfig().name(), is(new CaseInsensitiveString("stage")));
        assertThat(((RepoConfigOrigin) currentConfig.getPipelineConfigByName(remoteDownstreamPipeline.name()).getOrigin()).getRevision(), is("repo1_r1"));
        assertThat(((RepoConfigOrigin) cachedGoPartials.lastValidPartials().get(0).getOrigin()).getRevision(), is("repo1_r1"));
        assertThat(((RepoConfigOrigin) cachedGoPartials.lastKnownPartials().get(0).getOrigin()).getRevision(), is("repo1_r2"));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).size(), is(1));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).get(0).getMessage(), is("Invalid Merged Configuration"));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).get(0).getDescription(), is(String.format("1+ errors :: Stage with name 'upstream_stage_renamed' does not exist on pipeline '%s', it is being referred to from pipeline 'remote-downstream' (url at repo1_r2);;  -  Config-Repo: url at repo1_r2", pipelineConfig.name())));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).isEmpty(), is(true));
    }

    private void saveTemplateWithParamToConfig(CaseInsensitiveString templateName) throws Exception {
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("job"));
        ExecTask task = new ExecTask();
        task.setCommand("ls");
        jobConfig.addTask(task);
        jobConfig.addVariable("ENV_VAR", "#{SOME_PARAM}");
        final PipelineTemplateConfig template = new PipelineTemplateConfig(templateName, new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs(jobConfig)));
        CruiseConfig cruiseConfig = goConfigDao.loadConfigHolder().configForEdit;
        cruiseConfig.addTemplate(template);
        saveConfig(cruiseConfig);
    }

    private void saveScmMaterialToConfig(String id) throws Exception {
        SCM scm = new SCM(id, new PluginConfiguration(id, "1.0"), new Configuration(new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue("value"))));
        scm.setName(id);
        CruiseConfig cruiseConfig = goConfigDao.loadConfigHolder().configForEdit;
        cruiseConfig.getSCMs().add(scm);
        saveConfig(cruiseConfig);
    }

    private void saveConfig(CruiseConfig cruiseConfig) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        new MagicalGoConfigXmlWriter(configCache, registry).write(cruiseConfig, buffer, false);
    }
}
