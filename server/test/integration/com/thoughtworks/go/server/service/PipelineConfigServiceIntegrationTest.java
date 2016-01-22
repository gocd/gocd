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
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.domain.config.*;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
import com.thoughtworks.go.presentation.TriStateSelection;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemEnvironment;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.UUID;

import static com.thoughtworks.go.util.TestUtils.contains;
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
    private MetricsProbeService metricsProbeService;

    private GoConfigFileHelper configHelper;
    private PipelineConfig pipelineConfig;
    private Username user;
    private String headCommitBeforeUpdate;
    private HttpLocalizedOperationResult result;
    private String groupName = "jumbo";

    @Before
    public void setup() throws Exception {
        configHelper = new GoConfigFileHelper();
        dbHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();
        goConfigService.forceNotifyListeners();
        user = new Username(new CaseInsensitiveString("current"));
        pipelineConfig = GoConfigMother.createPipelineConfigWithMaterialConfig(UUID.randomUUID().toString(), new GitMaterialConfig("FOO"));
        goConfigService.addPipeline(pipelineConfig, groupName);
        GoCipher goCipher = new GoCipher();
        goConfigService.updateServerConfig(new MailHost(goCipher), new LdapConfig(goCipher), new PasswordFileConfig("path"), false, goConfigService.configFileMd5(), "artifacts", null, null, "0", null, null, "foo");
        UpdateConfigCommand command = goConfigService.modifyAdminPrivilegesCommand(Arrays.asList(user.getUsername().toString()), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.add));
        goConfigService.updateConfig(command);
        result = new HttpLocalizedOperationResult();
        headCommitBeforeUpdate = configRepository.getCurrentRevCommit().name();
    }

    @After
    public void tearDown() throws Exception {
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
    public void shouldCreatePipelineConfigWhenPipelineGroupDoesExist() throws GitAPIException {
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
    public void shouldNotCreatePipelineConfigWhenAPipelineBySameNameAlreadyExists() throws GitAPIException {
        GoConfigHolder goConfigHolderBeforeUpdate = goConfigDao.loadConfigHolder();
        PipelineConfig pipelineBeingCreated = GoConfigMother.createPipelineConfigWithMaterialConfig(pipelineConfig.name().toLower(), new DependencyMaterialConfig(pipelineConfig.name(), pipelineConfig.first().name()));
        pipelineConfigService.createPipelineConfig(user, pipelineBeingCreated, result, groupName);

        assertThat(result.toString(), result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(422));
        assertFalse(pipelineBeingCreated.errors().isEmpty());
        assertThat(pipelineBeingCreated.errors().on(PipelineConfig.NAME), is(String.format("You have defined multiple pipelines named '%s'. Pipeline names must be unique.", pipelineConfig.name())));
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
    public void shouldUpdatePipelineConfig() throws GitAPIException {
        GoConfigHolder goConfigHolderBeforeUpdate = goConfigDao.loadConfigHolder();

        pipelineConfig.add(new StageConfig(new CaseInsensitiveString("additional_stage"), new JobConfigs(new JobConfig(new CaseInsensitiveString("addtn_job")))));
        pipelineConfigService.updatePipelineConfig(user, pipelineConfig, result);

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
        pipelineConfig.setLabelTemplate("LABEL");
        pipelineConfigService.updatePipelineConfig(user, pipelineConfig, result);

        assertThat(result.toString(), result.isSuccessful(), is(false));
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
        pipelineConfig.clear();
        pipelineConfig.setTemplateName(templateName);
        pipelineConfigService.updatePipelineConfig(user, pipelineConfig, result);

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
        pipelineConfig.clear();
        pipelineConfig.setTemplateName(templateName);
        pipelineConfig.addStageWithoutValidityAssertion(StageConfigMother.stageConfig("local-stage"));
        pipelineConfigService.updatePipelineConfig(user, pipelineConfig, result);

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
        pipelineConfigService.updatePipelineConfig(new Username(new CaseInsensitiveString("unauthorized_user")), pipelineConfig, result);

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
        pipelineConfig.materialConfigs().add(scmMaterialConfig);
        pipelineConfigService.updatePipelineConfig(user, pipelineConfig, result);

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
        pipelineConfig.materialConfigs().add(packageMaterialConfig);
        pipelineConfigService.updatePipelineConfig(user, pipelineConfig, result);

        assertThat(result.toString(), result.isSuccessful(), is(false));
        assertThat(packageMaterialConfig.errors().on(PackageMaterialConfig.PACKAGE_ID), is("Could not find repository for given package id:[packageid]"));
        assertThat(configRepository.getCurrentRevCommit().name(), is(headCommitBeforeUpdate));
        assertThat(goConfigDao.loadConfigHolder().configForEdit, is(goConfigHolder.configForEdit));
        assertThat(goConfigDao.loadConfigHolder().config, is(goConfigHolder.config));
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
        new MagicalGoConfigXmlWriter(configCache, registry, metricsProbeService).write(cruiseConfig, buffer, false);
    }
}