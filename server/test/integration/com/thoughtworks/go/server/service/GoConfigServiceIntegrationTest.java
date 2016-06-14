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

package com.thoughtworks.go.server.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.CachedGoConfig;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.update.ConfigUpdateResponse;
import com.thoughtworks.go.config.update.UpdateConfigFromUI;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.presentation.ConfigForEdit;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.ReflectionUtil;
import org.apache.commons.httpclient.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.sameInstance;
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
public class GoConfigServiceIntegrationTest {
    @Autowired private SecurityService securityService;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private GoConfigService goConfigService;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private Localizer localizer;
    @Autowired private ConfigRepository configRepo;
    @Autowired private CachedGoConfig cachedGoConfig;
    @Autowired private ServerConfigService serverConfigService;

    private GoConfigFileHelper configHelper;

    @Before
    public void setup() throws Exception {
        configHelper = new GoConfigFileHelper();
        dbHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();
        goConfigService.forceNotifyListeners();
    }

    @After
    public void tearDown() throws Exception {
        configHelper.onTearDown();
        dbHelper.onTearDown();
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
    public void shouldOnlyAllowAdminsToGetPipelineConfig() {
        setupSecurity();

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ConfigForEdit configForEdit = goConfigService.loadForEdit("my-pipeline", new Username(new CaseInsensitiveString("loser")), result);
        assertThat(configForEdit, is(nullValue()));
        assertThat(result.httpCode(), is(401));
        assertThat(result.message(localizer), is("Unauthorized to edit my-pipeline pipeline."));

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
    public void shouldReturn404WhenUserIsNotAnAdminAndTriesToLoadANonExistentPipeline() {
        setupSecurity();
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ConfigForEdit configForEdit = goConfigService.loadForEdit("non-existent-pipeline", new Username(new CaseInsensitiveString("loser")), result);
        assertThat(configForEdit, is(nullValue()));
        assertThat(result.httpCode(), is(404));
        assertThat(result.message(localizer), is("Pipeline 'non-existent-pipeline' not found."));
    }

    private void setupSecurity() {
        SecurityConfig securityConfig = new SecurityConfig(new LdapConfig(new GoCipher()), new PasswordFileConfig("foo"), false);
        securityConfig.adminsConfig().add(new AdminUser(new CaseInsensitiveString("root")));
        configHelper.addSecurity(securityConfig);
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
        assertThat(result.message(localizer), is("Pipeline 'my-invalid-pipeline' not found."));
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
    public void shouldReturn401WhenAUserIsNotAnAdmin() throws IOException {
        configHelper.turnOnSecurity();
        configHelper.addAdmins("hero");

        configHelper.addTemplate("pipeline", "stage");

        cachedGoConfig.forceReload();

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        goConfigService.loadCruiseConfigForEdit(new Username(new CaseInsensitiveString("loser")), result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(401));
        assertThat(result.message(localizer), is("Unauthorized to edit configuration"));

    }

    @Test
    public void shouldReturnANewCopyOfConfigForEditWhenAUserIsATemplateAdmin() throws IOException {
        configHelper.turnOnSecurity();
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
        configHelper.turnOnSecurity();
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
        configHelper.turnOnSecurity();
        configHelper.addAdmins("hero");
        configHelper.addPipelineWithGroup("group_one", "pipeline", "stage", "my_job");

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ConfigForEdit<PipelineConfigs> configForEdit = goConfigService.loadGroupForEditing("group_one", new Username(new CaseInsensitiveString("loser")), result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is("Unauthorized to edit 'group_one' group."));
        assertThat(configForEdit, is(nullValue()));
    }

    @Test
    public void shouldFailLoadingWhenGivenInvalidGroupName() throws IOException {
        configHelper.addPipelineWithGroup("group_one", "pipeline", "stage", "my_job");

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ConfigForEdit<PipelineConfigs> configForEdit = goConfigService.loadGroupForEditing("group_foo", new Username(new CaseInsensitiveString("loser")), result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is("Pipeline group named 'group_foo' does not exist."));
        assertThat(configForEdit, is(nullValue()));
    }

    @Test
    public void shouldUpdateConfigFromUI() {
        configHelper.addPipeline("pipeline", "stage");
        String md5 = goConfigService.getConfigForEditing().getMd5();
        ConfigUpdateResponse response = goConfigService.updateConfigFromUI(new AddStageToPipelineCommand("secondStage"), md5, Username.ANONYMOUS, new HttpLocalizedOperationResult());

        PipelineConfig config = goConfigService.getConfigForEditing().pipelineConfigByName(new CaseInsensitiveString("pipeline"));

        assertThat(config.size(), is(2));
        assertThat(config.get(0).name(), is(new CaseInsensitiveString("stage")));
        assertThat(config.get(1).name(), is(new CaseInsensitiveString("secondStage")));

        assertThat(response.configAfterUpdate().hasStageConfigNamed(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("secondStage"), true), is(true));
    }

    @Test
    public void shouldRespondWithNewConfigWhenSavedSuccessfully() {
        configHelper.addPipeline("pipeline", "stage");
        String md5 = goConfigService.getConfigForEditing().getMd5();
        UpdateConfigFromUI pipelineAndStageRename = new PipelineStageRenamingCommand();
        ConfigUpdateResponse response = goConfigService.updateConfigFromUI(pipelineAndStageRename, md5, Username.ANONYMOUS, new HttpLocalizedOperationResult());

        PipelineConfig pipeline = goConfigService.getConfigForEditing().pipelineConfigByName(new CaseInsensitiveString("new-pipeline"));
        StageConfig stage = pipeline.getStage(new CaseInsensitiveString("new-stage"));
        assertThat(pipeline, not(nullValue()));
        assertThat(stage, not(nullValue()));

        assertThat(((PipelineConfig) response.getNode()).name(), is(new CaseInsensitiveString("new-pipeline")));
        assertThat(((StageConfig) response.getSubject()).name(), is(new CaseInsensitiveString("new-stage")));

        assertThat(response.configAfterUpdate().hasStageConfigNamed(new CaseInsensitiveString("new-pipeline"), new CaseInsensitiveString("new-stage"), false), is(true));
    }

    @Test
    public void shouldRespondWithLatestUnmodifiedConfigInCaseOfUnexpectedFailures() {
        configHelper.addPipeline("pipeline", "stage");
        String md5 = goConfigService.getConfigForEditing().getMd5();
        UpdateConfigFromUI pipelineAndStageRename = new PipelineStageRenamingCommand() {
            @Override public void update(Validatable pipelineNode) {
                throw new RuntimeException("Oh no!");
            }
        };
        ConfigUpdateResponse response = goConfigService.updateConfigFromUI(pipelineAndStageRename, md5, Username.ANONYMOUS, new HttpLocalizedOperationResult());

        assertThat(goConfigService.getConfigForEditing().hasPipelineNamed(new CaseInsensitiveString("new-pipeline")), is(false));
        assertThat(goConfigService.getConfigForEditing().hasPipelineNamed(new CaseInsensitiveString("pipeline")), is(true));

        assertThat(((PipelineConfig) response.getNode()).name(), is(new CaseInsensitiveString("pipeline")));
        assertThat(((StageConfig) response.getSubject()).name(), is(new CaseInsensitiveString("stage")));

        assertThat(response.configAfterUpdate().hasStageConfigNamed(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("new-stage"), false), is(false));
        assertThat(response.configAfterUpdate().hasStageConfigNamed(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("stage"), false), is(true));
    }

    @Test
    public void shouldRespondWithModifiedConfigWhenSaveFailsBecauseOfValidationErrors() {
        configHelper.addPipeline("pipeline", "stage");
        String md5 = goConfigService.getConfigForEditing().getMd5();
        UpdateConfigFromUI pipelineAndStageRename = new PipelineStageRenamingCommand() {{
            newPipelineName = "pipeline!@foo - bar";
        }};
        ConfigUpdateResponse response = goConfigService.updateConfigFromUI(pipelineAndStageRename, md5, Username.ANONYMOUS, new HttpLocalizedOperationResult());

        assertThat(goConfigService.getConfigForEditing().hasPipelineNamed(new CaseInsensitiveString("pipeline!@foo - bar")), is(false));
        assertThat(goConfigService.getConfigForEditing().hasPipelineNamed(new CaseInsensitiveString("pipeline")), is(true));

        assertThat(((PipelineConfig) response.getNode()).name(), is(new CaseInsensitiveString("pipeline!@foo - bar")));
        assertThat(((StageConfig) response.getSubject()).name(), is(new CaseInsensitiveString("new-stage")));

        assertThat(response.configAfterUpdate().hasStageConfigNamed(new CaseInsensitiveString("pipeline!@foo - bar"), new CaseInsensitiveString("new-stage"), false), is(false));
    }

    @Test
    public void shouldReturnAllErrorsAppliedOverEditedCopy() {
        configHelper.addPipeline("pipeline", "stage");
        configHelper.addParamToPipeline("pipeline", "mingle_url", "http://foo.bar");
        String md5 = goConfigService.getConfigForEditing().getMd5();
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ConfigUpdateResponse response = goConfigService.updateConfigFromUI(new UpdateConfigFromUI() {
            public void checkPermission(CruiseConfig cruiseConfig, LocalizedOperationResult result) {

            }

            public Validatable node(CruiseConfig cruiseConfig) {
                return cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline"));
            }

            public Validatable updatedNode(CruiseConfig cruiseConfig) {
                return node(cruiseConfig);
            }

            public void update(Validatable pipeline) {
                PipelineConfig pipelineConfig = (PipelineConfig) pipeline;
                pipelineConfig.setMingleConfig(new MingleConfig("#{mingle_url}", "go"));
            }

            public Validatable subject(Validatable node) {
                return node;
            }

            public Validatable updatedSubject(Validatable updatedNode) {
                return subject(updatedNode);
            }
        }, md5, new Username(new CaseInsensitiveString("admin")), result);

        MingleConfig mingleConfig = ((PipelineConfig) response.getNode()).getMingleConfig();
        assertThat(mingleConfig.errors().on(MingleConfig.BASE_URL), is("Should be a URL starting with https://"));
        assertThat(mingleConfig.getBaseUrl(), is("#{mingle_url}"));
    }

    @Test
    public void shouldReturnTheLatestConfigAsResultWhenThereIsAnMd5Conflict() {
        configHelper.addPipeline("pipeline", "stage");
        String md5 = goConfigService.getConfigForEditing().getMd5();
        goConfigService.updateConfigFromUI(new AddStageToPipelineCommand("secondStage"), md5, Username.ANONYMOUS, new HttpLocalizedOperationResult());

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ConfigUpdateResponse response = goConfigService.updateConfigFromUI(new AddStageToPipelineCommand("thirdStage"), md5, Username.ANONYMOUS, result);

        assertFailedResult(result, "Save failed. Configuration file has been modified by someone else.");
        CruiseConfig expectedConfig = goConfigService.getConfigForEditing();
        CruiseConfig modifiedConfig = new Cloner().deepClone(expectedConfig);
        ReflectionUtil.setField(modifiedConfig, "md5", expectedConfig.getMd5());
        PipelineConfig expected = modifiedConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline"));
        expected.addStageWithoutValidityAssertion(StageConfigMother.custom("thirdStage", "job"));

        PipelineConfig actual = (PipelineConfig) response.getNode();

        assertThat(response.configAfterUpdate(), is(expectedConfig));
        assertThat(response.getCruiseConfig(), is(modifiedConfig));
        assertThat(actual, is(expected));
        assertFailedResult(result, "Save failed. Configuration file has been modified by someone else.");
    }

    @Test
    public void shouldReturnAResponseWithTheValidatedCruiseConfig() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        configHelper.addPipeline("pipeline", "stage");
        String md5 = goConfigService.getConfigForEditing().getMd5();
        ConfigUpdateResponse response = goConfigService.updateConfigFromUI(new AddStageToPipelineCommand("stage"), md5, Username.ANONYMOUS, result);

        CruiseConfig invalidConfig = response.getCruiseConfig();
        assertThat(invalidConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline")).size(), is(2));//Make sure that the config returned is the duplicate on.

        PipelineConfig config = (PipelineConfig) response.getNode();
        assertThat(config.size(), is(2));

        assertStageError(config.get(1), "You have defined multiple stages called 'stage'. Stage names are case-insensitive and must be unique.", StageConfig.NAME);
        assertFailedResult(result, "Save failed, see errors below");

        assertThat(response.configAfterUpdate().hasStageConfigNamed(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("secondStage"), true), is(false));
    }

    @Test
    public void shouldNotUpdateConfigFromUI_whenUpdateMethodBombs() {
        final PipelineConfig pipelineConfig = configHelper.addPipeline("pipeline", "stage");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String md5 = goConfigService.getConfigForEditing().getMd5();
        ConfigUpdateResponse response = goConfigService.updateConfigFromUI(new AddStageToPipelineCommand("secondStage") {
            public void update(Validatable node) {
                super.update(node);
                throw new RuntimeException("oops, foo bared!");
            }
        }, md5, Username.ANONYMOUS, result);

        PipelineConfig config = goConfigService.getConfigForEditing().pipelineConfigByName(new CaseInsensitiveString("pipeline"));

        assertThat(config.size(), is(1));
        assertThat(config.get(0).name(), is(new CaseInsensitiveString("stage")));

        assertThat(response.getCruiseConfig(), is(goConfigService.getConfigForEditing()));
        assertThat((PipelineConfig) response.getNode(), is(pipelineConfig));
        assertFailedResult(result, "Save failed. oops, foo bared!");
    }

    @Test
    public void shouldNotUpdateConfigFromUIWhentheUserDoesNotHavePermissions() {
        final PipelineConfig pipelineConfig = configHelper.addPipeline("pipeline", "stage");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String md5 = goConfigService.getConfigForEditing().getMd5();
        final CruiseConfig[] configObtainedInCheckPermissions = new CruiseConfig[1];
        ConfigUpdateResponse response = goConfigService.updateConfigFromUI(new AddStageToPipelineCommand("secondStage") {
            public void checkPermission(CruiseConfig cruiseConfig, LocalizedOperationResult result) {
                result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_GROUP", "groupName"), null);
                configObtainedInCheckPermissions[0] = cruiseConfig;
            }
        }, md5, Username.ANONYMOUS, result);

        assertThat(configObtainedInCheckPermissions[0], is(goConfigService.getCurrentConfig()));

        PipelineConfig config = goConfigService.getConfigForEditing().pipelineConfigByName(new CaseInsensitiveString("pipeline"));

        assertThat(config.size(), is(1));
        assertThat(config.get(0).name(), is(new CaseInsensitiveString("stage")));

        assertThat(response.getCruiseConfig(), is(goConfigService.getConfigForEditing()));
        assertThat((PipelineConfig) response.getNode(), is(pipelineConfig));
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(401));
        assertThat(result.message(localizer), is("Unauthorized to edit 'groupName' group."));
    }

    @Test
    public void shouldLoadConfigFileOnlyWhenModifiedOnDisk() throws InterruptedException {
        cachedGoConfig.forceReload();
        Thread.sleep(1000);
        goConfigService.updateConfig(new UpdateConfigCommand() {
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
        assertThat(validity.isType(GoConfigValidity.VT_MERGE_OPERATION_ERROR), is(true));
        assertThat(validity.errorMessage(), is("Configuration file has been modified by someone else."));
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
        // Pre throws VT_CONFLICT as user submitted xml is validated before attempting to save
        assertThat(validity.isType(GoConfigValidity.VT_CONFLICT), is(true));
        assertThat(validity.errorMessage(), containsString("Name is invalid. \"user1 pipeline\""));
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
        assertThat(validity.toString(), validity.isType(GoConfigValidity.VT_MERGE_POST_VALIDATION_ERROR), is(true));
        assertThat(validity.errorMessage(), is("Pipeline \"up_pipeline\" does not exist. It is used from pipeline \"down_pipeline\"."));
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
        assertThat(validity.isType(GoConfigValidity.VT_CONFLICT), is(true));
        assertThat(validity.errorMessage(), containsString("Name is invalid. \"user1 pipeline\""));
    }

    @Test
    public void shouldInternallyGetGoConfigInvalidExceptionOnValidationErrorAndFailWithATopLevelConfigError() throws Exception {
        String oldMd5 = goConfigService.getConfigForEditing().getMd5();
        CruiseConfig user1SeeingConfig = configHelper.load();

        // Setup a pipeline group in the config
        new GoConfigMother().addPipelineWithGroup(user1SeeingConfig, "defaultGroup", "user1_pipeline", "user1_stage", "user1_job");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        configHelper.getXml(user1SeeingConfig, os);

        GoConfigService.XmlPartialSaver saver = goConfigService.fileSaver(false);
        saver.saveXml(os.toString(), oldMd5);

        CruiseConfig configBeforePipelineGroupWasAddedAtBeginning = configHelper.load();
        String md5BeforeAddingGroupAtBeginning = configBeforePipelineGroupWasAddedAtBeginning.getMd5();

        // User 1 edits config XML and adds a pipeline group before the first group in config
        String configXMLWithGroupAddedAtBeginning = os.toString().replace("</pipelines>", "</pipelines><pipelines group=\"first_group\"/>");
        saver.saveXml(configXMLWithGroupAddedAtBeginning, md5BeforeAddingGroupAtBeginning);

        // User 2 adds another pipeline group, with the same name, through UI, but using the older MD5.
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ConfigUpdateResponse response = goConfigService.updateConfigFromUI(new UpdateConfigFromUI() {
            public void checkPermission(CruiseConfig cruiseConfig, LocalizedOperationResult result) {
            }

            public Validatable node(CruiseConfig cruiseConfig) {
                return cruiseConfig;
            }

            public Validatable updatedNode(CruiseConfig cruiseConfig) {
                return node(cruiseConfig);
            }

            public void update(Validatable config) {
                CruiseConfig cruiseConfig = (CruiseConfig) config;
                MaterialConfigs materials = new MaterialConfigs(MaterialConfigsMother.mockMaterialConfigs("file:///tmp/foo"));
                new GoConfigMother().addPipelineWithGroup(cruiseConfig, "first_group", "up_pipeline", materials, "down_stage", "down_job");
            }

            public Validatable subject(Validatable node) {
                return node;
            }

            public Validatable updatedSubject(Validatable updatedNode) {
                return subject(updatedNode);
            }
        }, md5BeforeAddingGroupAtBeginning, new Username(new CaseInsensitiveString("admin")), result);

        CruiseConfig config = response.getCruiseConfig();
        assertThat(config.getMd5(), is(md5BeforeAddingGroupAtBeginning));
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(HttpStatus.SC_CONFLICT));
        assertThat(result.message(localizer), is("Save failed. Duplicate unique value [first_group] declared for identity constraint \"uniquePipelines\" of element \"cruise\"."));
    }

    @Test
    public void shouldNotThrowUpOnConfigSaveWhenIndependentChangesAreMade_ViaMergeFlow() throws Exception {
        // Priming current configuration to add lines simulating the license section before removal
        for (int i = 0; i < 10; i++) {
            configHelper.addRole(new Role(new CaseInsensitiveString("admin_role_" + i), new RoleUser(new CaseInsensitiveString("admin_user_" + i))));
        }

        // User 1 loads page
        CruiseConfig user1SeeingConfig = goConfigDao.loadForEditing();
        String user1SeeingMd5 = user1SeeingConfig.getMd5();

        // User 2 edits config
        configHelper.addPipelineWithGroup("defaultGroup", "user2_pipeline", "user2_stage", "user2_job");
        CruiseConfig user2SeeingConfig = configHelper.load();

        // User 1 edits old config to make an independent change
        new GoConfigMother().addRole(user1SeeingConfig, new Role(new CaseInsensitiveString("admin_role"), new RoleUser(new CaseInsensitiveString("admin_user"))));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        configHelper.getXml(user1SeeingConfig, os);

        // User 1 saves edited config
        GoConfigService.XmlPartialSaver saver = goConfigService.fileSaver(false);
        GoConfigValidity validity = saver.saveXml(os.toString(), user1SeeingMd5);

        assertThat(validity.errorMessage(), validity.isValid(), is(true));
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

    private void setJobTimeoutTo(final String jobTimeout) {
        CruiseConfig config = configHelper.currentConfig();
        config.server().setJobTimeout(jobTimeout);
        configHelper.writeConfigFile(config);
    }

    private void assertFailedResult(HttpLocalizedOperationResult result, final String message) {
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is(message));
    }

    private void assertStageError(StageConfig duplicatedStage, final String message, final String field) {
        assertThat(duplicatedStage.errors().isEmpty(), is(false));
        assertThat(duplicatedStage.errors().on(field), is(message));
    }

    private static class AddStageToPipelineCommand implements UpdateConfigFromUI {
        public String stageName;

        public AddStageToPipelineCommand(String stageName) {
            this.stageName = stageName;
        }

        public void checkPermission(CruiseConfig cruiseConfig, LocalizedOperationResult result) {
        }

        public Validatable node(CruiseConfig cruiseConfig) {
            return cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline"));
        }

        public Validatable updatedNode(CruiseConfig cruiseConfig) {
            return node(cruiseConfig);
        }

        public void update(Validatable node) {
            PipelineConfig pipeline = (PipelineConfig) node;
            pipeline.addStageWithoutValidityAssertion(StageConfigMother.custom(stageName, "job"));
        }

        public Validatable subject(Validatable node) {
            return node;
        }

        public Validatable updatedSubject(Validatable updatedNode) {
            return subject(updatedNode);
        }
    }

    private static class PipelineStageRenamingCommand implements UpdateConfigFromUI {

        protected String newPipelineName = "new-pipeline";

        public void checkPermission(CruiseConfig cruiseConfig, LocalizedOperationResult result) {

        }

        public Validatable node(CruiseConfig cruiseConfig) {
            return cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline"));
        }

        public Validatable updatedNode(CruiseConfig cruiseConfig) {
            return cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(newPipelineName));
        }

        public void update(Validatable pipelineNode) {
            PipelineConfig pipeline = (PipelineConfig) pipelineNode;
            ReflectionUtil.setField(pipeline, "name", new CaseInsensitiveString(newPipelineName));
            ReflectionUtil.setField(pipeline.getStage(new CaseInsensitiveString("stage")), "name", new CaseInsensitiveString("new-stage"));
        }

        private StageConfig getStage(Validatable pipelineNode, String stageName) {
            PipelineConfig pipeline = (PipelineConfig) pipelineNode;
            return pipeline.getStage(new CaseInsensitiveString(stageName));
        }

        public Validatable subject(Validatable pipelineNode) {
            return getStage(pipelineNode, "stage");
        }

        public Validatable updatedSubject(Validatable pipelineNode) {
            return getStage(pipelineNode, "new-stage");
        }
    }
}
