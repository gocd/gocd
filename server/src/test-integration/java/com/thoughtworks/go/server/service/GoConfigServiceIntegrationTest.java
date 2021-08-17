/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.plugin.access.artifact.ArtifactMetadataStore;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.artifact.ArtifactPluginInfo;
import com.thoughtworks.go.plugin.domain.artifact.Capabilities;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.presentation.ConfigForEdit;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class GoConfigServiceIntegrationTest {
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private CachedGoConfig cachedGoConfig;

    private GoConfigFileHelper configHelper;
    @Autowired
    private GoConfigMigration goConfigMigration;

    @BeforeEach
    public void setup() throws Exception {
        configHelper = new GoConfigFileHelper();
        dbHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();
        goConfigService.forceNotifyListeners();
        setupMetadataForPlugin();
    }

    @AfterEach
    public void tearDown() throws Exception {
        configHelper.onTearDown();
        dbHelper.onTearDown();
        ArtifactMetadataStore.instance().clear();
    }

    @Test
    public void shouldUnderstandGettingPipelineConfigForEdit() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("my-pipeline", "my-stage", "my-build");
        pipelineConfig.addParam(new ParamConfig("label-param", "param-value"));
        pipelineConfig.setLabelTemplate("${COUNT}-#{label-param}");
        CruiseConfig config = configHelper.currentConfig();
        config.addPipeline("defaultGroup", pipelineConfig);
        configHelper.writeConfigFile(config);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        ConfigForEdit<PipelineConfig> configForEdit = goConfigService.loadForEdit("my-pipeline", new Username(new CaseInsensitiveString("root")), result);
        assertThat(configForEdit.getProcessedConfig(), is(goConfigService.getCurrentConfig()));
        assertThat(configForEdit.getConfig().getLabelTemplate(), is("${COUNT}-#{label-param}"));
        assertThat(configForEdit.getCruiseConfig().getMd5(), is(goConfigService.configFileMd5()));
        configHelper.addPipeline("pipeline-foo", "stage-foo");
        assertThat(configForEdit.getCruiseConfig().getMd5(), not(goConfigService.configFileMd5()));
    }

    @Test
    public void shouldOnlyAllowAdminsToGetPipelineConfig() throws IOException {
        setupSecurity();

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ConfigForEdit configForEdit = goConfigService.loadForEdit("my-pipeline", new Username(new CaseInsensitiveString("loser")), result);
        assertThat(configForEdit, is(nullValue()));
        assertThat(result.httpCode(), is(403));
        assertThat(result.message(), is("Unauthorized to edit 'my-pipeline' pipeline."));

        result = new HttpLocalizedOperationResult();
        configForEdit = goConfigService.loadForEdit("my-pipeline", new Username(new CaseInsensitiveString("pipeline_admin")), result);
        assertThat(configForEdit, not(nullValue()));
        assertThat(result.isSuccessful(), is(true));

        result = new HttpLocalizedOperationResult();
        configForEdit = goConfigService.loadForEdit("my-pipeline", new Username(new CaseInsensitiveString("root")), result);
        assertThat(configForEdit, not(nullValue()));
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldReturn404WhenUserIsNotAnAdminAndTriesToLoadANonExistentPipeline() throws IOException {
        setupSecurity();
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ConfigForEdit configForEdit = goConfigService.loadForEdit("non-existent-pipeline", new Username(new CaseInsensitiveString("loser")), result);
        assertThat(configForEdit, is(nullValue()));
        assertThat(result.httpCode(), is(404));
        assertThat(result.message(), is(EntityType.Pipeline.notFoundMessage("non-existent-pipeline")));
    }

    private void setupSecurity() throws IOException {
        configHelper.enableSecurity();
        configHelper.addAdmins("root");
        configHelper.addPipeline("my-pipeline", "my-stage");
        configHelper.setAdminPermissionForGroup(BasicPipelineConfigs.DEFAULT_GROUP, "pipeline_admin");
    }

    @Test
    public void shouldReturnWithA404WhenPipelineNotFound() {
        CruiseConfig config = configHelper.currentConfig();
        configHelper.writeConfigFile(config);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        assertThat(goConfigService.loadForEdit("my-invalid-pipeline", new Username(new CaseInsensitiveString("root")), result), is(nullValue()));
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(404));
        assertThat(result.message(), is(EntityType.Pipeline.notFoundMessage("my-invalid-pipeline")));
    }

    @Test
    public void shouldAlwaysReturnCloneOfCruiseConfigSoThatCachedCopyIsNotCorrupted() {
        configHelper.addPipeline("my-pipeline", "my-stage");
        CruiseConfig cruiseConfig = configHelper.load();
        cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("my-pipeline")).setLabelTemplate("foo-${COUNT}-bar");
        configHelper.writeConfigFile(cruiseConfig);

        ConfigForEdit<PipelineConfig> config = goConfigService.loadForEdit("my-pipeline", new Username(new CaseInsensitiveString("root")), new HttpLocalizedOperationResult());

        config.getConfig().setLabelTemplate("Foo-bar");
        config.getCruiseConfig().pipelineConfigByName(new CaseInsensitiveString("my-pipeline")).setLabelTemplate("Foo-bar");
        config.getProcessedConfig().pipelineConfigByName(new CaseInsensitiveString("my-pipeline")).setLabelTemplate("Foo-bar");

        assertThat(goConfigService.getConfigForEditing().pipelineConfigByName(new CaseInsensitiveString("my-pipeline")).getLabelTemplate(), is("foo-${COUNT}-bar"));
        assertThat(goConfigService.currentCruiseConfig().pipelineConfigByName(new CaseInsensitiveString("my-pipeline")).getLabelTemplate(), is("foo-${COUNT}-bar"));
    }

    @Test
    public void shouldReturn403WhenAUserIsNotAnAdmin() throws IOException {
        configHelper.enableSecurity();
        configHelper.addAdmins("hero");

        configHelper.addTemplate("pipeline", "stage");

        cachedGoConfig.forceReload();

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        goConfigService.loadCruiseConfigForEdit(new Username(new CaseInsensitiveString("loser")), result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(403));
        assertThat(result.message(), is("Unauthorized to edit."));
    }

    @Test
    public void shouldReturnANewCopyOfConfigForEditWhenAUserIsATemplateAdmin() throws IOException {
        configHelper.enableSecurity();
        configHelper.addAdmins("hero");

        configHelper.addTemplate("pipeline", new Authorization(new AdminsConfig(new AdminUser(new CaseInsensitiveString("template-admin")))), "stage");

        cachedGoConfig.forceReload();

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        CruiseConfig config = goConfigService.loadCruiseConfigForEdit(new Username(new CaseInsensitiveString("template-admin")), result);

        assertThat(result.isSuccessful(), is(true));
        assertThat(goConfigService.getConfigForEditing(), is(config));
        assertThat(goConfigService.getConfigForEditing(), not(sameInstance(config)));
    }

    @Test
    public void shouldReturnANewCopyOfConfigForEditWhenLoadingForEdit() throws IOException {
        configHelper.enableSecurity();
        configHelper.addAdmins("hero");

        configHelper.addTemplate("pipeline", "stage");

        cachedGoConfig.forceReload();

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        CruiseConfig config = goConfigService.loadCruiseConfigForEdit(new Username(new CaseInsensitiveString("hero")), result);

        assertThat(result.isSuccessful(), is(true));
        assertThat(goConfigService.getConfigForEditing(), is(config));
        assertThat(goConfigService.getConfigForEditing(), not(sameInstance(config)));
    }

    @Test
    public void shouldLoadPipelineGroupForEdit() throws IOException {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("my-pipeline", "my-stage", "my-build");
        pipelineConfig.addParam(new ParamConfig("label-param", "param-value"));
        pipelineConfig.setLabelTemplate("${COUNT}-#{label-param}");
        CruiseConfig config = configHelper.currentConfig();
        config.addPipeline("group_one", pipelineConfig);
        configHelper.writeConfigFile(config);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        ConfigForEdit<PipelineConfigs> configForEdit = goConfigService.loadGroupForEditing("group_one", new Username(new CaseInsensitiveString("root")), result);
        assertThat(configForEdit.getConfig().findBy(new CaseInsensitiveString("my-pipeline")).getLabelTemplate(), is("${COUNT}-#{label-param}"));
        assertThat(configForEdit.getCruiseConfig().getMd5(), is(goConfigService.configFileMd5()));
        configHelper.addPipeline("pipeline-foo", "stage-foo");
        assertThat(configForEdit.getCruiseConfig().getMd5(), not(goConfigService.configFileMd5()));
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldCloneTheConfigObjectBeforeHandingOffForEdit() throws IOException {
        configHelper.addAdmins("hero");
        configHelper.addPipelineWithGroup("group_one", "pipeline", "stage", "my_job");

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ConfigForEdit<PipelineConfigs> groupForEdit = goConfigService.loadGroupForEditing("group_one", new Username(new CaseInsensitiveString("hero")), result);

        PipelineConfigs group = groupForEdit.getConfig();
        group.getAuthorization().getAdminsConfig().add(new AdminUser(new CaseInsensitiveString("loser")));

        PipelineConfigs groupFromProcessedConfigCopy = groupForEdit.getProcessedConfig().getGroups().findGroup("group_one");
        groupFromProcessedConfigCopy.getAuthorization().getAdminsConfig().add(new AdminUser(new CaseInsensitiveString("loser")));

        PipelineConfigs groupFromEditableConfigCopy = groupForEdit.getCruiseConfig().getGroups().findGroup("group_one");
        groupFromEditableConfigCopy.getAuthorization().getAdminsConfig().add(new AdminUser(new CaseInsensitiveString("loser")));

        AdminsConfig adminsConfig = goConfigService.getConfigForEditing().findGroup("group_one").getAuthorization().getAdminsConfig();
        assertThat(adminsConfig.size(), is(0));//should not affect the global copy
        adminsConfig = goConfigService.currentCruiseConfig().findGroup("group_one").getAuthorization().getAdminsConfig();
        assertThat(adminsConfig.size(), is(0));

        group.setGroup("new-name");
        assertThat(groupForEdit.getCruiseConfig().hasPipelineGroup("group_one"), is(true));
        assertThat(groupForEdit.getProcessedConfig().hasPipelineGroup("group_one"), is(true));
        assertThat(groupForEdit.getCruiseConfig().hasPipelineGroup("new-name"), is(false));
        assertThat(groupForEdit.getProcessedConfig().hasPipelineGroup("new-name"), is(false));
    }

    @Test
    public void shouldErrorOutWhenUserIsNotAuthorizedToLoadGroupForEdit() throws IOException {
        configHelper.enableSecurity();
        configHelper.addAdmins("hero");
        configHelper.addPipelineWithGroup("group_one", "pipeline", "stage", "my_job");

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ConfigForEdit<PipelineConfigs> configForEdit = goConfigService.loadGroupForEditing("group_one", new Username(new CaseInsensitiveString("loser")), result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(), is(EntityType.PipelineGroup.forbiddenToEdit("group_one", "loser")));
        assertThat(configForEdit, is(nullValue()));
    }

    @Test
    public void shouldFailLoadingWhenGivenInvalidGroupName() throws IOException {
        configHelper.addPipelineWithGroup("group_one", "pipeline", "stage", "my_job");

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ConfigForEdit<PipelineConfigs> configForEdit = goConfigService.loadGroupForEditing("group_foo", new Username(new CaseInsensitiveString("loser")), result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(), is(EntityType.PipelineGroup.notFoundMessage("group_foo")));
        assertThat(configForEdit, is(nullValue()));
    }

    @Test
    public void shouldLoadConfigFileOnlyWhenModifiedOnDisk() throws InterruptedException {
        cachedGoConfig.forceReload();
        Thread.sleep(1000);
        goConfigService.updateConfig(new UpdateConfigCommand() {
            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                cruiseConfig.server().setArtifactsDir("foo");
                return cruiseConfig;
            }
        });
        CruiseConfig cruiseConfig = cachedGoConfig.loadForEditing();
        cachedGoConfig.forceReload();
        assertThat(cruiseConfig, sameInstance(cachedGoConfig.loadForEditing()));
    }

    @Test
    public void shouldReturnTheServerLevelJobTimeoutIfTheJobDoesNotHaveItConfigured() {
        configHelper.addPipeline("pipeline", "stage");
        setJobTimeoutTo("30");
        assertThat(goConfigService.getUnresponsiveJobTerminationThreshold(new JobIdentifier("pipeline", -1, "label", "stage", "-1", "unit")), is(30 * 60 * 1000L));
    }

    @Test
    public void shouldReturnTrueIfTheJobDoesNotHaveTimeoutConfigured() {
        configHelper.addPipeline("pipeline", "stage");
        assertThat(goConfigService.canCancelJobIfHung(new JobIdentifier("pipeline", -1, "label", "stage", "-1", "unit")), is(false));
    }

    @Test
    public void shouldReturnFalseIfTheJobDoesNotHaveTimeoutConfiguredAndServerHasItSetToZero() {
        configHelper.addPipeline("pipeline", "stage");
        setJobTimeoutTo("0");

        assertThat(goConfigService.canCancelJobIfHung(new JobIdentifier("pipeline", -1, "label", "stage", "-1", "unit")), is(false));
    }

    @Test
    public void shouldReturnFalseIfPipelineIsNotFoundForTheJob() {
        assertThat(goConfigService.canCancelJobIfHung(new JobIdentifier("recently_deleted_pipeline", -1, "label", "stage", "-1", "unit")), is(false));
    }

    @Test
    public void shouldReturnTrueIfTheJobHasNonZeroTimeoutConfigured() {
        configHelper.addPipeline("pipeline", "stage");
        CruiseConfig config = configHelper.currentConfig();
        config.findJob("pipeline", "stage", "unit").setTimeout("10");
        configHelper.writeConfigFile(config);
        assertThat(goConfigService.canCancelJobIfHung(new JobIdentifier("pipeline", -1, "label", "stage", "-1", "unit")), is(true));
    }

    @Test
    public void shouldReturnFalseIfTheJobHasZeroTimeoutConfigured() {
        configHelper.addPipeline("pipeline", "stage");
        CruiseConfig config = configHelper.currentConfig();
        config.findJob("pipeline", "stage", "unit").setTimeout("0");
        configHelper.writeConfigFile(config);
        assertThat(goConfigService.canCancelJobIfHung(new JobIdentifier("pipeline", -1, "label", "stage", "-1", "unit")), is(false));
    }

    @Test
    public void shouldReturnTheJobLevelTimeoutIfTheJobHasItConfigured() {
        configHelper.addPipeline("pipeline", "stage");
        CruiseConfig config = configHelper.currentConfig();
        config.findJob("pipeline", "stage", "unit").setTimeout("10");
        configHelper.writeConfigFile(config);
        setJobTimeoutTo("30");
        assertThat(goConfigService.getUnresponsiveJobTerminationThreshold(new JobIdentifier("pipeline", -1, "label", "stage", "-1", "unit")), is(10 * 60 * 1000L));
    }

    @Test
    public void shouldReturnTheDefaultTimeoutIfThePipelineIsNotRecentlyDeleted() {
        assertThat(goConfigService.getUnresponsiveJobTerminationThreshold(new JobIdentifier("recently_deleted_pipeline", -1, "label", "stage", "-1", "unit")), is(0L));
    }

    @Test
    public void shouldThrowUpOnConfigSaveMergeConflict_ViaMergeFlow() throws Exception {
        // User 1 loads page
        CruiseConfig user1SeeingConfig = goConfigDao.loadForEditing();
        String user1SeeingMd5 = user1SeeingConfig.getMd5();

        // User 2 edits config
        configHelper.addPipelineWithGroup("defaultGroup", "user2_pipeline", "user2_stage", "user2_job");
        CruiseConfig user2SeeingConfig = configHelper.load();

        // User 1 edits old config
        new GoConfigMother().addPipelineWithGroup(user1SeeingConfig, "defaultGroup", "user1_pipeline", "user1_stage", "user1_job");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        configHelper.getXml(user1SeeingConfig, os);

        // User 1 saves edited config
        GoConfigService.XmlPartialSaver saver = goConfigService.fileSaver(false);
        GoConfigValidity validity = saver.saveXml(os.toString(), user1SeeingMd5);

        assertThat(validity.isValid(), is(false));
        GoConfigValidity.InvalidGoConfig invalidGoConfig = (GoConfigValidity.InvalidGoConfig) validity;
        assertThat(invalidGoConfig.isType(GoConfigValidity.VT_MERGE_OPERATION_ERROR), is(true));
        assertThat(invalidGoConfig.errorMessage(), is("Configuration file has been modified by someone else."));
    }

    @Test
    public void shouldThrowUpOnConfigSavePreValidationError_ViaMergeFlow() throws Exception {
        // User 1 loads page
        CruiseConfig user1SeeingConfig = goConfigDao.loadForEditing();
        String user1SeeingMd5 = user1SeeingConfig.getMd5();

        // User 2 edits config
        configHelper.addPipelineWithGroup("defaultGroup", "user2_pipeline", "user2_stage", "user2_job");
        CruiseConfig user2SeeingConfig = configHelper.load();

        // User 1 edits old config
        new GoConfigMother().addPipelineWithGroup(user1SeeingConfig, "defaultGroup", "user1_pipeline", "user1_stage", "user1_job");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        configHelper.getXml(user1SeeingConfig, os);

        // Introduce validation error on xml
        String xml = os.toString();
        xml = xml.replace("user1_pipeline", "user1 pipeline");

        // User 1 saves edited config
        GoConfigService.XmlPartialSaver saver = goConfigService.fileSaver(false);
        GoConfigValidity validity = saver.saveXml(xml, user1SeeingMd5);

        assertThat(validity.isValid(), is(false));
        GoConfigValidity.InvalidGoConfig invalidGoConfig = (GoConfigValidity.InvalidGoConfig) validity;
        // Pre throws VT_CONFLICT as user submitted xml is validated before attempting to save
        assertThat(invalidGoConfig.isType(GoConfigValidity.VT_CONFLICT), is(true));
        assertThat(invalidGoConfig.errorMessage(), containsString("Name is invalid. \"user1 pipeline\""));
    }

    @Test
    public void shouldThrowUpOnConfigSavePostValidationError_ViaMergeFlow() throws Exception {
        // User 1 adds a pipeline
        configHelper.addPipelineWithGroup("defaultGroup", "up_pipeline", "up_stage", "up_job");
        configHelper.addPipelineWithGroup("anotherGroup", "random_pipeline", "random_stage", "random_job");
        CruiseConfig user1SeeingConfig = configHelper.load();
        String user1SeeingMd5 = user1SeeingConfig.getMd5();

        // User 2 edits config
        configHelper.removePipeline("up_pipeline");

        // User 1 edits old config
        MaterialConfigs materialConfigs = new MaterialConfigs();
        materialConfigs.add(MaterialConfigsMother.dependencyMaterialConfig("up_pipeline", "up_stage"));
        new GoConfigMother().addPipelineWithGroup(user1SeeingConfig, "anotherGroup", "down_pipeline", materialConfigs, "down_stage", "down_job");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        configHelper.getXml(user1SeeingConfig, os);

        // User 1 saves edited config
        String xml = os.toString();
        GoConfigService.XmlPartialSaver saver = goConfigService.fileSaver(false);
        GoConfigValidity validity = saver.saveXml(xml, user1SeeingMd5);

        assertThat(validity.isValid(), is(false));
        GoConfigValidity.InvalidGoConfig invalidGoConfig = (GoConfigValidity.InvalidGoConfig) validity;
        assertThat(validity.toString(), invalidGoConfig.isType(GoConfigValidity.VT_MERGE_POST_VALIDATION_ERROR), is(true));
        assertThat(invalidGoConfig.errorMessage(), is("Pipeline 'up_pipeline' does not exist. It is used from pipeline 'down_pipeline'."));
    }

    @Test
    public void shouldThrowUpOnConfigSaveValidationError_ViaNormalFlow() throws Exception {
        // User 1 loads page
        CruiseConfig user1SeeingConfig = goConfigDao.loadForEditing();
        String user1SeeingMd5 = user1SeeingConfig.getMd5();

        // User 1 edits old config
        new GoConfigMother().addPipelineWithGroup(user1SeeingConfig, "defaultGroup", "user1_pipeline", "user1_stage", "user1_job");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        configHelper.getXml(user1SeeingConfig, os);

        // Introduce validation error on xml
        String xml = os.toString();
        xml = xml.replace("user1_pipeline", "user1 pipeline");

        // User 1 saves edited config
        GoConfigService.XmlPartialSaver saver = goConfigService.fileSaver(false);
        GoConfigValidity validity = saver.saveXml(xml, user1SeeingMd5);

        assertThat(validity.isValid(), is(false));
        GoConfigValidity.InvalidGoConfig invalidGoConfig = (GoConfigValidity.InvalidGoConfig) validity;
        assertThat(invalidGoConfig.isType(GoConfigValidity.VT_CONFLICT), is(true));
        assertThat(invalidGoConfig.errorMessage(), containsString("Name is invalid. \"user1 pipeline\""));
    }

    @Test
    public void shouldNotThrowUpOnConfigSaveWhenIndependentChangesAreMade_ViaMergeFlow() throws Exception {
        // Priming current configuration to add lines simulating the license section before removal
        for (int i = 0; i < 10; i++) {
            configHelper.addRole(new RoleConfig(new CaseInsensitiveString("admin_role_" + i), new RoleUser(new CaseInsensitiveString("admin_user_" + i))));
        }

        // User 1 loads page
        CruiseConfig user1SeeingConfig = goConfigDao.loadForEditing();
        String user1SeeingMd5 = user1SeeingConfig.getMd5();

        // User 2 edits config
        configHelper.addPipelineWithGroup("defaultGroup", "user2_pipeline", "user2_stage", "user2_job");
        CruiseConfig user2SeeingConfig = configHelper.load();

        // User 1 edits old config to make an independent change
        new GoConfigMother().addRole(user1SeeingConfig, new RoleConfig(new CaseInsensitiveString("admin_role"), new RoleUser(new CaseInsensitiveString("admin_user"))));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        configHelper.getXml(user1SeeingConfig, os);

        // User 1 saves edited config
        GoConfigService.XmlPartialSaver saver = goConfigService.fileSaver(false);
        GoConfigValidity validity = saver.saveXml(os.toString(), user1SeeingMd5);

        assertThat(validity.isValid(), is(true));
    }

    @Test
    public void shouldNotThrowUpOnConfigSave_ViaNormalFlow() throws Exception {
        // User 1 loads page
        CruiseConfig user1SeeingConfig = goConfigDao.loadForEditing();

        // User 2 edits config
        configHelper.addPipelineWithGroup("defaultGroup", "user2_pipeline", "user2_stage", "user2_job");
        CruiseConfig user2SeeingConfig = configHelper.load();
        String user2SeeingMd5 = user2SeeingConfig.getMd5();

        // User 1 edits new config
        new GoConfigMother().addPipelineWithGroup(user2SeeingConfig, "defaultGroup", "user1_pipeline", "user1_stage", "user1_job");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        configHelper.getXml(user2SeeingConfig, os);

        // User 1 saves edited config
        GoConfigService.XmlPartialSaver saver = goConfigService.fileSaver(false);
        GoConfigValidity validity = saver.saveXml(os.toString(), user2SeeingMd5);

        assertThat(validity.isValid(), is(true));
    }

    private void setupMetadataForPlugin() {
        PluginDescriptor pluginDescriptor = GoPluginDescriptor.builder().id("cd.go.artifact.docker.registry").build();
        PluginConfiguration buildFile = new PluginConfiguration("BuildFile", new Metadata(false, false));
        PluginConfiguration image = new PluginConfiguration("Image", new Metadata(false, true));
        PluginConfiguration tag = new PluginConfiguration("Tag", new Metadata(false, false));
        PluginConfiguration fetchProperty = new PluginConfiguration("FetchProperty", new Metadata(false, true));
        PluginConfiguration fetchTag = new PluginConfiguration("Tag", new Metadata(false, false));
        PluginConfiguration registryUrl = new PluginConfiguration("RegistryURL", new Metadata(true, false));
        PluginConfiguration username = new PluginConfiguration("Username", new Metadata(false, false));
        PluginConfiguration password = new PluginConfiguration("Password", new Metadata(false, true));
        PluggableInstanceSettings storeConfigSettings = new PluggableInstanceSettings(asList(registryUrl, username, password));
        PluggableInstanceSettings publishArtifactSettings = new PluggableInstanceSettings(asList(buildFile, image, tag));
        PluggableInstanceSettings fetchArtifactSettings = new PluggableInstanceSettings(asList(fetchProperty, fetchTag));
        ArtifactPluginInfo artifactPluginInfo = new ArtifactPluginInfo(pluginDescriptor, storeConfigSettings, publishArtifactSettings, fetchArtifactSettings, null, new Capabilities());
        ArtifactMetadataStore.instance().setPluginInfo(artifactPluginInfo);
    }

    private void setJobTimeoutTo(final String jobTimeout) {
        CruiseConfig config = configHelper.currentConfig();
        config.server().setJobTimeout(jobTimeout);
        configHelper.writeConfigFile(config);
    }

    private void assertFailedResult(HttpLocalizedOperationResult result, final String message) {
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(), is(message));
    }

    private void assertStageError(StageConfig duplicatedStage, final String message, final String field) {
        assertThat(duplicatedStage.errors().isEmpty(), is(false));
        assertThat(duplicatedStage.errors().on(field), is(message));
    }

}
