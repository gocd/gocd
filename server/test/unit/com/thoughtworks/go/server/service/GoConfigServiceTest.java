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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.ConfigFileHasChangedException;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.exceptions.PipelineGroupNotFoundException;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.server.security.ldap.BaseConfig;
import com.thoughtworks.go.config.server.security.ldap.BasesConfig;
import com.thoughtworks.go.config.update.ConfigUpdateResponse;
import com.thoughtworks.go.config.update.UiBasedConfigUpdateCommand;
import com.thoughtworks.go.config.update.UpdateConfigFromUI;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.listener.BaseUrlChangeListener;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.UserDao;
import com.thoughtworks.go.server.domain.PipelineConfigDependencyGraph;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import com.thoughtworks.go.server.persistence.PipelineRepository;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.*;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsInstanceOf;
import org.jdom.input.JDOMParseException;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.*;

import static com.thoughtworks.go.config.PipelineConfigs.DEFAULT_GROUP;
import static com.thoughtworks.go.helper.AgentMother.*;
import static com.thoughtworks.go.helper.ConfigFileFixture.configWith;
import static com.thoughtworks.go.helper.PipelineConfigMother.createGroup;
import static com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig;
import static com.thoughtworks.go.helper.PipelineTemplateConfigMother.createTemplate;
import static java.lang.String.format;
import static org.apache.commons.httpclient.HttpStatus.SC_BAD_REQUEST;
import static org.apache.commons.httpclient.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class GoConfigServiceTest {

    private GoConfigDao goConfigDao;
    private GoConfigService goConfigService;
    private PipelineRepository pipelineRepository;
    private static final String PIPELINE = "pipeline1";
    private static final String STAGE = "stage1";
    private static final String JOB = "Job1";
    private CruiseConfig cruiseConfig;
    private Clock clock;
    private GoCache goCache;
    private ConfigRepository configRepo;
    private UserDao userDao;
    public PipelinePauseService pipelinePauseService;
    private MetricsProbeService metricsProbeService;
    private InstanceFactory instanceFactory;

    @Before
    public void setup() throws Exception {
        new SystemEnvironment().setProperty(SystemEnvironment.ENFORCE_SERVERID_MUTABILITY, "N");

        configRepo = mock(ConfigRepository.class);
        goConfigDao = mock(GoConfigDao.class);
        pipelineRepository = mock(PipelineRepository.class);
        pipelinePauseService = mock(PipelinePauseService.class);
        metricsProbeService = mock(MetricsProbeService.class);
        cruiseConfig = unchangedConfig();
        expectLoad(cruiseConfig);
        this.clock = mock(Clock.class);
        goCache = mock(GoCache.class);
        instanceFactory = mock(InstanceFactory.class);
        userDao = mock(UserDao.class);

        ConfigElementImplementationRegistry registry = ConfigElementImplementationRegistryMother.withNoPlugins();
        goConfigService = new GoConfigService(goConfigDao, pipelineRepository, this.clock, new GoConfigMigration(configRepo, new TimeProvider(), new ConfigCache(),
                registry, metricsProbeService), goCache, configRepo, userDao, registry, metricsProbeService,
                instanceFactory);
    }

    @Test
    public void shouldUnderstandIfAnEnvironmentVariableIsConfiguredForAPipeline() throws Exception {
        final PipelineConfigs newPipeline = new BasicPipelineConfigs();

        PipelineConfig otherPipeline = createPipelineConfig("pipeline_other", "stage_other", "plan_other");
        otherPipeline.setVariables(GoConfigFileHelper.env("OTHER_PIPELINE_LEVEL", "other pipeline"));
        otherPipeline.first().setVariables(GoConfigFileHelper.env("OTHER_STAGE_LEVEL", "other stage"));
        otherPipeline.first().jobConfigByConfigName(new CaseInsensitiveString("plan_other")).setVariables(GoConfigFileHelper.env("OTHER_JOB_LEVEL", "other job"));

        PipelineConfig pipelineConfig = createPipelineConfig("pipeline", "name", "plan");
        pipelineConfig.setVariables(GoConfigFileHelper.env("PIPELINE_LEVEL", "pipeline value"));
        StageConfig stageConfig = pipelineConfig.first();
        stageConfig.setVariables(GoConfigFileHelper.env("STAGE_LEVEL", "stage value"));
        stageConfig.jobConfigByConfigName(new CaseInsensitiveString("plan")).setVariables(GoConfigFileHelper.env("JOB_LEVEL", "job value"));

        newPipeline.add(pipelineConfig);
        newPipeline.add(otherPipeline);

        CruiseConfig cruiseConfig = new BasicCruiseConfig(newPipeline);
        EnvironmentConfig environmentConfig = cruiseConfig.addEnvironment("uat");
        environmentConfig.addPipeline(new CaseInsensitiveString("pipeline"));
        environmentConfig.addEnvironmentVariable("ENV_LEVEL", "env value");

        expectLoad(cruiseConfig);

        assertThat(goConfigService.hasVariableInScope("pipeline", "NOT_IN_SCOPE"), is(false));
        assertThat(goConfigService.hasVariableInScope("pipeline", "ENV_LEVEL"), is(true));
        assertThat(goConfigService.hasVariableInScope("pipeline", "PIPELINE_LEVEL"), is(true));
        assertThat(goConfigService.hasVariableInScope("pipeline", "STAGE_LEVEL"), is(true));
        assertThat(goConfigService.hasVariableInScope("pipeline", "JOB_LEVEL"), is(true));
        assertThat(goConfigService.hasVariableInScope("pipeline", "OTHER_PIPELINE_LEVEL"), is(false));
        assertThat(goConfigService.hasVariableInScope("pipeline", "OTHER_STAGE_LEVEL"), is(false));
        assertThat(goConfigService.hasVariableInScope("pipeline", "OTHER_JOB_LEVEL"), is(false));

        assertThat(goConfigService.hasVariableInScope("pipeline_other", "ENV_LEVEL"), is(false));
        assertThat(goConfigService.hasVariableInScope("pipeline_other", "OTHER_PIPELINE_LEVEL"), is(true));
        assertThat(goConfigService.hasVariableInScope("pipeline_other", "OTHER_STAGE_LEVEL"), is(true));
        assertThat(goConfigService.hasVariableInScope("pipeline_other", "OTHER_JOB_LEVEL"), is(true));
        assertThat(goConfigService.hasVariableInScope("pipeline_other", "NOT_IN_SCOPE"), is(false));
    }

    @Test
    public void shouldUnderstandIfAStageHasFetchMaterialsConfigured() throws Exception {
        PipelineConfig pipeline = createPipelineConfig("cruise", "dev", "test");
        StageConfig stage = pipeline.first();
        stage.setFetchMaterials(false);

        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(pipeline));
        expectLoad(cruiseConfig);

        assertThat(goConfigService.shouldFetchMaterials("cruise", "dev"), is(false));
    }

    private void expectLoad(final CruiseConfig result) throws Exception {
        when(goConfigDao.load()).thenReturn(result);
    }

    private void expectLoadForEditing(final CruiseConfig result) throws Exception {
        when(goConfigDao.loadForEditing()).thenReturn(result);
    }

    private CruiseConfig unchangedConfig() {
        return configWith(createPipelineConfig(PIPELINE, STAGE, JOB));
    }

    @Test
    public void shouldGetAllStagesWithOne() throws Exception {
        final PipelineConfigs newPipeline = new BasicPipelineConfigs();
        PipelineConfig pipelineConfig = createPipelineConfig("pipeline", "name", "plan");
        newPipeline.add(pipelineConfig);
        expectLoad(new BasicCruiseConfig(newPipeline));
        assertThat(goConfigService.stageConfigNamed("pipeline", "name"), is(pipelineConfig.findBy(new CaseInsensitiveString("name"))));
    }

    @Test
    public void shouldTellIfAnUSerIsGroupAdministrator() throws Exception {
        final PipelineConfigs newPipeline = new BasicPipelineConfigs();
        PipelineConfig pipelineConfig = createPipelineConfig("pipeline", "name", "plan");
        newPipeline.add(pipelineConfig);
        newPipeline.setAuthorization(new Authorization(new AdminsConfig(new AdminUser(new CaseInsensitiveString("dawg")))));
        expectLoad(new BasicCruiseConfig(newPipeline));
        final Username dawg = new Username(new CaseInsensitiveString("dawg"));
        assertThat(goConfigService.isGroupAdministrator(dawg.getUsername()), is(true));
    }

    @Test
    public void shouldTellIfAnEnvironmentExists() throws Exception {
        BasicEnvironmentConfig first = new BasicEnvironmentConfig(new CaseInsensitiveString("first"));
        BasicEnvironmentConfig second = new BasicEnvironmentConfig(new CaseInsensitiveString("second"));
        CruiseConfig config = new BasicCruiseConfig();
        config.addEnvironment(first);
        config.addEnvironment(second);
        expectLoad(config);

        assertThat(goConfigService.hasEnvironmentNamed(new CaseInsensitiveString("first")), is(true));
        assertThat(goConfigService.hasEnvironmentNamed(new CaseInsensitiveString("second")), is(true));
        assertThat(goConfigService.hasEnvironmentNamed(new CaseInsensitiveString("SECOND")), is(true));
        assertThat(goConfigService.hasEnvironmentNamed(new CaseInsensitiveString("fourth")), is(false));
    }

    @Test
    public void shouldTellIfOnlyKnownUsersAreAllowedToLogin() throws Exception {
        CruiseConfig config = new BasicCruiseConfig();
        config.server().security().setAllowOnlyKnownUsersToLogin(true);
        expectLoad(config);

        assertThat(goConfigService.isOnlyKnownUserAllowedToLogin(), is(true));
    }

    @Test
    public void shouldTellIfAnAgentExists() throws Exception {
        CruiseConfig config = new BasicCruiseConfig();
        config.agents().add(new AgentConfig("uuid"));
        expectLoad(config);

        assertThat(goConfigService.hasAgent("uuid"), is(true));
        assertThat(goConfigService.hasAgent("doesnt-exist"), is(false));
    }

    @Test
    public void shouldReturnTrueIfStageHasTestsAndFalseIfItDoesnt() throws Exception {
        PipelineConfigs newPipelines = new BasicPipelineConfigs();
        PipelineConfig pipelineConfig = createPipelineConfig("pipeline", "name", "plan");
        pipelineConfig.add(StageConfigMother.stageConfigWithArtifact("stage1", "job1", ArtifactType.unit));
        pipelineConfig.add(StageConfigMother.stageConfigWithArtifact("stage2", "job2", ArtifactType.file));
        newPipelines.add(pipelineConfig);
        expectLoad(new BasicCruiseConfig(newPipelines));
        assertThat(goConfigService.stageHasTests("pipeline", "stage1"), is(true));
        assertThat(goConfigService.stageHasTests("pipeline", "stage2"), is(false));
    }

    @Test
    public void shouldGetCommentRenderer() throws Exception {
        PipelineConfigs newPipeline = new BasicPipelineConfigs();
        PipelineConfig pipelineConfig = createPipelineConfig("pipeline", "name", "plan");
        pipelineConfig.setTrackingTool(new TrackingTool("link", "regex"));
        newPipeline.add(pipelineConfig);
        expectLoad(new BasicCruiseConfig(newPipeline));
        assertEquals(goConfigService.getCommentRendererFor("pipeline"), new TrackingTool("link", "regex"));

        pipelineConfig = createPipelineConfig("pipeline", "name", "plan");
        pipelineConfig.setMingleConfig(new MingleConfig("baseUrl", "projIdentifier", "mql"));
        newPipeline = new BasicPipelineConfigs();
        newPipeline.add(pipelineConfig);
        expectLoad(new BasicCruiseConfig(newPipeline));
        assertEquals(goConfigService.getCommentRendererFor("pipeline"), new MingleConfig("baseUrl", "projIdentifier", "mql"));

        pipelineConfig = createPipelineConfig("pipeline", "name", "plan");
        newPipeline = new BasicPipelineConfigs();
        newPipeline.add(pipelineConfig);
        expectLoad(new BasicCruiseConfig(newPipeline));
        assertEquals(goConfigService.getCommentRendererFor("pipeline"), new TrackingTool());
    }

    @Test
    public void shouldUnderstandIfAPipelineIsLockable() throws Exception {
        PipelineConfigs group = new BasicPipelineConfigs();
        PipelineConfig pipelineConfig = createPipelineConfig("pipeline", "name", "plan");
        group.add(pipelineConfig);
        expectLoad(new BasicCruiseConfig(group));
        assertThat(goConfigService.isLockable("pipeline"), is(false));

        pipelineConfig.lockExplicitly();
        expectLoad(new BasicCruiseConfig(group));
        assertThat(goConfigService.isLockable("pipeline"), is(true));
    }

    @Test
    public void shouldUnderstandIfLdapIsConfigured() throws Exception {
        CruiseConfig config = new BasicCruiseConfig();
        config.setServerConfig(new ServerConfig(null, new SecurityConfig(new LdapConfig("test", "test", "test", null, true, new BasesConfig(new BaseConfig("test")), "test"), null, true, null)));
        expectLoad(config);
        assertThat("Ldap is configured", goConfigService.isLdapConfigured(), is(true));
    }

    @Test
    public void shouldRememberValidityWhenCruiseConfigLoaderHasInvalidConfigFile() throws Exception {
        GoConfigService service = goConfigServiceWithInvalidStatus();
        assertThat(service.checkConfigFileValid().isValid(), is(false));
        assertThat(service.checkConfigFileValid().errorMessage(), is("JDom exception"));
    }

    @Test
    public void shouldNotHaveErrorMessageWhenConfigFileValid() throws Exception {
        when(goConfigDao.checkConfigFileValid()).thenReturn(GoConfigValidity.valid());
        GoConfigValidity configValidity = goConfigService.checkConfigFileValid();
        assertThat(configValidity.isValid(), is(true));
        assertThat(configValidity.errorMessage(), is(""));
    }

    @Test
    public void shouldThrowExceptionWhenBuildFileIsInvalid() throws Exception {
        goConfigDao = mock(GoConfigDao.class, "badCruiseConfigManager");
        when(goConfigDao.loadForEditing()).thenThrow(new RuntimeException("Invalid config file", new JDOMParseException("JDom exception", new RuntimeException())));

        GoConfigService service = new GoConfigService(goConfigDao, pipelineRepository, new SystemTimeClock(), mock(GoConfigMigration.class), goCache, null, userDao,
                ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService, instanceFactory);

        try {
            service.buildSaver("pipeline", "stage", 1).asXml();
            fail("Invalid config file.");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Invalid config file"));
        }
    }

    @Test
    public void shouldReturnInvalidWhenTemplatesPartialIsInvalid() throws Exception {
        CruiseConfig config = new BasicCruiseConfig();
        config.server().setArtifactsDir("/var/logs");
        config.addTemplate(new PipelineTemplateConfig(new CaseInsensitiveString("templateName"), StageConfigMother.custom("stage", "job")));

        when(goConfigDao.loadForEditing()).thenReturn(config);
        String templateContent = "<templates>"
                + "  <unknown/>"
                + "<pipeline name='pipeline'>\n"
                + "  <stage name='firstStage'>"
                + "     <jobs>"
                + "         <job name='jobName'/>"
                + "      </jobs>"
                + "  </stage>"
                + "</pipeline>"
                + "</templates>";
        GoConfigValidity validity = goConfigService.templatesSaver().saveXml(templateContent, "md5");
        assertThat(validity.errorMessage(), containsString("Invalid content was found starting with element 'unknown'"));
    }

    @Test
    public void shouldClearExistingTemplateDefinitionWhenAnEmptyStringIsPosted() throws Exception {
        CruiseConfig config = new BasicCruiseConfig();
        config.server().setArtifactsDir("/var/logs");
        config.addTemplate(new PipelineTemplateConfig(new CaseInsensitiveString("templateName"), StageConfigMother.custom("stage", "job")));
        when(goConfigDao.loadForEditing()).thenReturn(config);
        String templateContent = "";
        GoConfigValidity validity = goConfigService.templatesSaver().saveXml(templateContent, "md5");
        assertThat(validity.errorMessage(), is(""));
    }

    @Test
    public void shouldBombWithErrorMessageWhenNoPipelinesExistAndATemplateIsConfigured() throws Exception {
        CruiseConfig config = new BasicCruiseConfig();
        config.server().setArtifactsDir("/var/logs");
        when(goConfigDao.loadForEditing()).thenReturn(config);
        String templateContent = "<templates>"
                + "<pipeline name='pipeline'>\n"
                + "  <stage name='firstStage'>"
                + "     <jobs>"
                + "         <job name='jobName'/>"
                + "      </jobs>"
                + "  </stage>"
                + "</pipeline>"
                + "</templates>";
        GoConfigValidity validity = goConfigService.templatesSaver().saveXml(templateContent, "md5");
        assertThat(validity.errorMessage(), is("There are no pipelines configured. Please add at least one pipeline in order to use templates."));
    }

    @Test
    public void shouldPersistTheNewAndValidTemplateDefinition() throws Exception {
        CruiseConfig config = new BasicCruiseConfig();
        config.server().setArtifactsDir("/var/logs");
        config.addTemplate(new PipelineTemplateConfig(new CaseInsensitiveString("templateName"), StageConfigMother.custom("stage", "job")));

        when(goConfigDao.loadForEditing()).thenReturn(config);
        String templateContent = "<templates>"
                + "<pipeline name='pipeline'>\n"
                + "  <stage name='firstStage'>"
                + "     <jobs>"
                + "         <job name='jobName'/>"
                + "      </jobs>"
                + "  </stage>"
                + "</pipeline>"
                + "</templates>";
        GoConfigValidity validity = goConfigService.templatesSaver().saveXml(templateContent, "md5");
        assertThat(validity.errorMessage(), is(""));
    }

    @Test
    public void shouldReturnInvalidWhenIndividualTemplatePartialIsInvalid() throws Exception {
        CruiseConfig config = new BasicCruiseConfig();
        config.server().setArtifactsDir("/var/logs");
        config.addTemplate(new PipelineTemplateConfig(new CaseInsensitiveString("templateName"), StageConfigMother.custom("stage", "job")));

        when(goConfigDao.loadForEditing()).thenReturn(config);
        String templateContent = "<pipeline name='pipeline'>\n"
                + "  <unknown/>"
                + "  <stage name='firstStage'>"
                + "     <jobs>"
                + "         <job name='jobName'/>"
                + "      </jobs>"
                + "  </stage>"
                + "</pipeline>";
        GoConfigValidity validity = goConfigService.templateSaver(0).saveXml(templateContent, "md5");
        assertThat(validity.errorMessage(), containsString("Invalid content was found starting with element 'unknown'"));
    }

    private CruiseConfig configWithPipeline() {
        PipelineConfig pipelineConfig = createPipelineConfig("pipeline", "stage", "first");
        pipelineConfig.addMaterialConfig(MaterialConfigsMother.hgMaterialConfig());
        CruiseConfig config = configWith(pipelineConfig);
        config.server().setArtifactsDir("/var/logs");
        return config;
    }

    @Test
    public void shouldReturnInvalidWhenJobPartialIsInvalid() throws Exception {
        CruiseConfig config = configWithPipeline();
        when(goConfigDao.loadForEditing()).thenReturn(config);
        GoConfigValidity validity = goConfigService.buildSaver("pipeline", "stage", 0).saveXml("<job name='first'><unknown></unknown></job>", "md5");
        assertThat(validity.errorMessage(), containsString("Invalid content was found starting with element 'unknown'"));
    }

    @Test
    public void shouldReturnInvalidWhenPipelinePartialIsInvalid() throws Exception {
        CruiseConfig config = configWithPipeline();
        when(goConfigDao.loadForEditing()).thenReturn(config);
        String pipelineContent = "<pipeline name='pipeline'>\n"
                + "    <materials>\n"
                + "         <svn url ='svnurl' dest='a'/>\n"
                + "    </materials>\n"
                + "  <stage name='firstStage'>"
                + "     <jobs>"
                + "         <job name='jobName'/>"
                + "      </jobs>"
                + "  </stage>"
                + "  <unknown/>"
                + "</pipeline>";
        GoConfigValidity validity = goConfigService.pipelineSaver("defaultGroup", 0).saveXml(pipelineContent, "md5");
        assertThat(validity.errorMessage(), containsString("Invalid content was found starting with element 'unknown'"));
    }

    @Test
    public void shouldReturnInvalidWhenStagePartialIsInvalid() throws Exception {
        CruiseConfig config = configWithPipeline();
        when(goConfigDao.loadForEditing()).thenReturn(config);
        String stageContent = "<stage name='firstStage'>"
                + "  <unknown/>"
                + "     <jobs>"
                + "         <job name='jobName'/>"
                + "      </jobs>"
                + "  </stage>";
        GoConfigValidity validity = goConfigService.stageSaver("pipeline", 0).saveXml(stageContent, "md5");
        assertThat(validity.errorMessage(), containsString("Invalid content was found starting with element 'unknown'"));
    }

    @Test
    public void shouldReturnInvalidWhenWholeConfigIsInvalidAndShouldUpgrade() throws Exception {
        CruiseConfig config = configWithPipeline();
        when(goConfigDao.loadForEditing()).thenReturn(config);
        String configContent = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\"14\">\n"
                + "<server artifactsdir='artifactsDir'/><unknown/></cruise>";
        GoConfigValidity validity = goConfigService.fileSaver(true).saveXml(configContent, "md5");
        assertThat(validity.errorMessage(), is("Cruise config file with version 14 is invalid. Unable to upgrade."));
    }

    @Test
    public void shouldReturnInvalidWhenWholeConfigIsInvalid() throws Exception {
        CruiseConfig config = configWithPipeline();
        when(goConfigDao.loadForEditing()).thenReturn(config);
        String configContent = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\"" + GoConstants.CONFIG_SCHEMA_VERSION + "\">\n"
                + "<server artifactsdir='artifactsDir'/><unknown/></cruise>";
        GoConfigValidity validity = goConfigService.fileSaver(false).saveXml(configContent, "md5");
        assertThat(validity.errorMessage(), containsString("Invalid content was found starting with element 'unknown'"));
    }

    @Test
    @Ignore("Deprecated usage. Will be removed soon")
    public void shouldReturnValidWhenJobPartialIsValid() throws Exception {
        CruiseConfig config = configWithPipeline();
        when(goConfigDao.loadForEditing()).thenReturn(config);
        GoConfigValidity validity = goConfigService.buildSaver("pipeline", "stage", 0).saveXml("<job name='first'></job>", "md5");
        assertThat(validity.isValid(), is(true));
    }

    @Test
    public void shouldThrowExceptionWhenSavingJobToPipelineWithTemplate() throws Exception {
        PipelineConfig pipeline = pipelineWithTemplate();
        when(goConfigDao.loadForEditing()).thenReturn(configWith(pipeline));

        try {
            goConfigService.buildSaver("pipeline", "stage", 0).asXml();
            fail("shouldThrowExceptionWhenSavingJobToPipelineWithTemplate");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Pipeline 'pipeline' references template 'foo'. Cannot edit job."));
        }
    }

    @Test
    public void shouldReturnvariablesForAPipeline() {
        EnvironmentConfig env = cruiseConfig.addEnvironment("environment");
        env.addEnvironmentVariable("foo", "env-fooValue");
        env.addEnvironmentVariable("bar", "env-barValue");
        env.addPipeline(new CaseInsensitiveString(PIPELINE));
        PipelineConfig pipeline = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(PIPELINE));
        pipeline.addEnvironmentVariable("foo", "pipeline-fooValue");
        pipeline.addEnvironmentVariable("blah", "pipeline-blahValue");

        EnvironmentVariablesConfig variables = goConfigService.variablesFor(PIPELINE);
        assertThat(variables.size(), is(3));
        assertThat(variables, hasItems(
                new EnvironmentVariableConfig("foo", "pipeline-fooValue"),
                new EnvironmentVariableConfig("bar", "env-barValue"),
                new EnvironmentVariableConfig("blah", "pipeline-blahValue")));
    }

    @Test
    public void shouldReturnvariablesForAPipelineNotInAnEnvironment() {
        PipelineConfig pipeline = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(PIPELINE));
        pipeline.addEnvironmentVariable("foo", "pipeline-fooValue");
        pipeline.addEnvironmentVariable("blah", "pipeline-blahValue");
        EnvironmentVariablesConfig variables = goConfigService.variablesFor(PIPELINE);
        assertThat(variables.size(), is(2));
        assertThat(variables, hasItems(
                new EnvironmentVariableConfig("foo", "pipeline-fooValue"),
                new EnvironmentVariableConfig("blah", "pipeline-blahValue")));
    }


    @Test
    public void shouldThrowExceptionWhenSavingStageToPipelineWithTemplate() throws Exception {
        PipelineConfig pipeline = pipelineWithTemplate();
        expectLoadForEditing(configWith(pipeline));

        try {
            goConfigService.stageSaver("pipeline", 0).asXml();
            fail("shouldThrowExceptionWhenSavingStageToPipelineWithTemplate");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Pipeline 'pipeline' references template 'foo'. Cannot edit stage."));
        }
    }


    private PipelineConfig pipelineWithTemplate() {
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline");
        pipeline.clear();
        pipeline.setTemplateName(new CaseInsensitiveString("foo"));
        PipelineTemplateConfig template = new PipelineTemplateConfig(new CaseInsensitiveString("foo"), StageConfigMother.custom("stage", "job"));
        pipeline.usingTemplate(template);
        return pipeline;
    }

    @Test
    public void shouldThrowExceptionWhenUnknownStage() throws Exception {
        expectLoadForEditing(configWith(createPipelineConfig("pipeline", "stage", "build")));
        try {
            goConfigService.buildSaver("pipeline", "unknown", 1).asXml();
            fail("Unknown stage");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Stage 'unknown' not found in pipeline 'pipeline'"));
        }
    }

    @Test
    public void shouldThrowExceptionIfUnknownPipeline() throws Exception {
        expectLoadForEditing(configWith(createPipelineConfig("pipeline", "stage", "build")));
        try {
            goConfigService.stageSaver("unknown", 0).asXml();
            fail("Unknown pipeline");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Pipeline 'unknown' not found."));
        }
    }


    @Test
    public void shouldThrowExceptionIfPipelineDoesNotExist() throws Exception {
        expectLoadForEditing(configWith(createPipelineConfig("pipeline", "stage", "build")));
        try {
            goConfigService.pipelineSaver(DEFAULT_GROUP, 2).asXml();
            fail("Unknown pipeline");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Pipeline does not exist."));
        }
    }

    @Test
    public void shouldNotThrowExceptionWhenUpgradeFailsForConfigFileUpdate() throws Exception {
        expectLoadForEditing(configWith(createPipelineConfig("pipeline", "stage", "build")));
        GoConfigService.XmlPartialSaver saver = goConfigService.fileSaver(true);
        GoConfigValidity validity = saver.saveXml("some_junk", "junk_md5");
        assertThat(validity.isValid(), is(false));
        assertThat(validity.errorMessage(), is("Error on line 1: Content is not allowed in prolog."));
    }

    @Test
    public void shouldProvideDetailsWhenXmlConfigDomIsInvalid() throws Exception {
        expectLoadForEditing(configWith(createPipelineConfig("pipeline", "stage", "build")));
        GoConfigService.XmlPartialSaver saver = goConfigService.fileSaver(false);
        String configContent = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\"" + GoConstants.CONFIG_SCHEMA_VERSION + "\">\n"
                + "<server artifactsdir='artifactsDir></cruise>";
        GoConfigValidity validity = saver.saveXml(configContent, "junk_md5");
        assertThat(validity.isValid(), is(false));
        assertThat(validity.errorMessage(),
                is("Invalid Configuration - Error on line 3: The value of attribute \"artifactsdir\" associated with an element type \"server\" must not contain the '<' character."));
    }

    @Test
    public void xmlPartialSaverShouldReturnTheRightXMLThroughAsXml() throws Exception {
        expectLoadForEditing(new GoConfigMother().defaultCruiseConfig());
        GoConfigService.XmlPartialSaver saver = goConfigService.fileSaver(true);
        assertThat(saver.asXml(), containsString(String.format("schemaVersion=\"%s\"", GoConstants.CONFIG_SCHEMA_VERSION)));
        assertThat(saver.asXml(), containsString("xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\""));
    }


    @Test
    public void shouldRegisterListenerWithTheConfigDAO() throws Exception {
        final ConfigChangedListener listener = mock(ConfigChangedListener.class);
        goConfigService.register(listener);
        verify(goConfigDao).registerListener(listener);
    }

    @Test
    public void shouldReturnApprovedAgentsNumber() throws Exception {
        expectLoad(configWithAgents(approvedLocalAgent(), approvedAgent(), deniedAgent()));
        assertThat(goConfigService.getNumberOfApprovedRemoteAgents(), is(1));
    }

    private CruiseConfig configWithAgents(AgentConfig... agentConfigs) {
        CruiseConfig cruiseConfig = unchangedConfig();
        cruiseConfig.agents().addAll(Arrays.asList(agentConfigs));
        return cruiseConfig;
    }

    @Test
    public void shouldFixJobNameCase() throws Exception {
        expectLoad(unchangedConfig());
        JobConfigIdentifier translated = goConfigService.translateToActualCase(
                new JobConfigIdentifier(PIPELINE.toUpperCase(), STAGE.toUpperCase(), JOB.toUpperCase()));
        assertThat(translated, is(new JobConfigIdentifier(PIPELINE, STAGE, JOB)));
    }

    @Test
    public void shouldNotLoseUUIDWhenRunOnAllAgents() throws Exception {
        expectLoad(unchangedConfigWithRunOnAllAgents());
        JobConfigIdentifier translated = goConfigService.translateToActualCase(
                new JobConfigIdentifier(PIPELINE.toUpperCase(), STAGE.toUpperCase(), RunOnAllAgents.CounterBasedJobNameGenerator.appendMarker(JOB.toUpperCase(), 2)));
        assertThat(translated, is(new JobConfigIdentifier(PIPELINE, STAGE, RunOnAllAgents.CounterBasedJobNameGenerator.appendMarker(JOB, 2))));
    }

    @Test
    public void shouldNotBeInstanceOfWhenRunOnAllAgentsWithMissingAgent() throws Exception {
        expectLoad(unchangedConfigWithRunOnAllAgents());
        String missingJobName = JOB + "-missing";
        try {
            goConfigService.translateToActualCase(new JobConfigIdentifier(PIPELINE, STAGE, missingJobName));
            fail("Should not be able to find job with missing agent");
        } catch (JobNotFoundException expected) {
            assertThat(expected.getMessage(), is(format("Job '%s' not found in pipeline '%s' stage '%s'", missingJobName, PIPELINE, STAGE)));
        }
    }

    private CruiseConfig unchangedConfigWithRunOnAllAgents() {
        PipelineConfig pipelineConfig = createPipelineConfig(PIPELINE, STAGE, JOB);
        pipelineConfig.get(0).jobConfigByConfigName(new CaseInsensitiveString(JOB)).setRunOnAllAgents(true);
        return configWith(pipelineConfig);
    }

    @Test
    public void shouldThrowJobNotFoundExceptionWhenJobDoesNotExist() throws Exception {
        expectLoad(unchangedConfig());
        try {
            goConfigService.translateToActualCase(new JobConfigIdentifier(PIPELINE, STAGE, "invalid-job"));
            fail("should throw exception if job does not exist");
        } catch (Exception e) {
            assertThat(e, instanceOf(JobNotFoundException.class));
            assertThat(e.getMessage(), containsString("invalid-job"));
        }
    }

    @Test
    public void shouldThrowStageNotFoundExceptionWhenStageDoesNotExist() throws Exception {
        expectLoad(unchangedConfig());
        try {
            goConfigService.translateToActualCase(new JobConfigIdentifier(PIPELINE, "invalid-stage", JOB));
            fail("should throw exception if stage does not exist");
        } catch (Exception e) {
            assertThat(e, instanceOf(StageNotFoundException.class));
            assertThat(e.getMessage(), containsString("invalid-stage"));
        }
    }

    @Test
    public void shouldThrowPipelineNotFoundExceptionWhenStageDoesNotExist() throws Exception {
        expectLoad(unchangedConfig());
        try {
            goConfigService.translateToActualCase(new JobConfigIdentifier("invalid-pipeline", STAGE, JOB));
            fail("should throw exception if pipeline does not exist");
        } catch (Exception e) {
            assertThat(e, instanceOf(PipelineNotFoundException.class));
            assertThat(e.getMessage(), containsString("invalid-pipeline"));
        }
    }


    @Test
    public void shouldThrowIfCruiseHasNoReadPermissionOnArtifactsDir() throws Exception {
        if (SystemUtil.isWindows()) {
            return;
        }

        File artifactsDir = FileUtil.createTempFolder();
        artifactsDir.setReadable(false, false);
        cruiseConfig.setServerConfig(new ServerConfig(artifactsDir.getAbsolutePath(), new SecurityConfig()));
        expectLoad(cruiseConfig);

        try {
            goConfigService.initialize();
            fail("should throw when cruise has no read permission on artifacts dir " + artifactsDir.getAbsolutePath());
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Cruise does not have read permission on " + artifactsDir.getAbsolutePath()));
        } finally {
            FileUtil.deleteFolder(artifactsDir);
        }

    }

    @Test
    public void shouldThrowIfCruiseHasNoWritePermissionOnArtifactsDir() throws Exception {
        if (SystemUtil.isWindows()) {
            return;
        }
        File artifactsDir = FileUtil.createTempFolder();
        artifactsDir.setWritable(false, false);
        cruiseConfig.setServerConfig(new ServerConfig(artifactsDir.getAbsolutePath(), new SecurityConfig()));
        expectLoad(cruiseConfig);

        try {
            goConfigService.initialize();
            fail("should throw when cruise has no write permission on artifacts dir " + artifactsDir.getAbsolutePath());
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Cruise does not have write permission on " + artifactsDir.getAbsolutePath()));
        } finally {
            FileUtil.deleteFolder(artifactsDir);
        }

    }

    @Test
    public void shouldFindMaterialByPipelineUniqueFingerprint() throws Exception {
        SvnMaterialConfig svnMaterialConfig = new SvnMaterialConfig("repo", null, null, false);
        svnMaterialConfig.setName(new CaseInsensitiveString("foo"));
        cruiseConfig = configWith(GoConfigMother.createPipelineConfigWithMaterialConfig(svnMaterialConfig));
        when(goConfigDao.load()).thenReturn(cruiseConfig);

        assertThat((SvnMaterialConfig) goConfigService.findMaterial(new CaseInsensitiveString("pipeline"), svnMaterialConfig.getPipelineUniqueFingerprint()), is(svnMaterialConfig));
        assertThat((SvnMaterialConfig) goConfigService.findMaterial(new CaseInsensitiveString("piPelIne"), svnMaterialConfig.getPipelineUniqueFingerprint()), is(svnMaterialConfig));
    }

    @Test
    public void shouldReturnNullIfNoMaterialMatches() throws Exception {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString("upstream-pipeline"), new CaseInsensitiveString("upstream-stage"));
        cruiseConfig = configWith(GoConfigMother.createPipelineConfigWithMaterialConfig(dependencyMaterialConfig));
        when(goConfigDao.load()).thenReturn(cruiseConfig);

        assertThat(goConfigService.findMaterial(new CaseInsensitiveString("pipeline"), "missing"), is(nullValue()));
    }

    @Test
    public void shouldEnableAgentWhenPending() {
        String agentId = DatabaseAccessHelper.AGENT_UUID;
        AgentConfig agentConfig = new AgentConfig(agentId, "remote-host", "50.40.30.20");
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromAgent(new AgentIdentifier("remote-host", "50.40.30.20", agentId), "cookie", null);
        AgentInstance instance = AgentInstance.createFromLiveAgent(agentRuntimeInfo, new SystemEnvironment());
        goConfigService.disableAgents(false, instance);
        shouldPerformCommand(new GoConfigDao.CompositeConfigCommand(GoConfigDao.createAddAgentCommand(agentConfig)));
    }

    private void shouldPerformCommand(UpdateConfigCommand command) {
        verify(goConfigDao).updateConfig(command);
    }

    @Test
    public void shouldEnableMultipleAgents() {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromAgent(new AgentIdentifier("remote-host", "50.40.30.20", "abc"), "cookie", null);
        AgentInstance pending = AgentInstance.createFromLiveAgent(agentRuntimeInfo, new SystemEnvironment());

        AgentConfig agentConfig = new AgentConfig("UUID2", "remote-host", "50.40.30.20");
        agentConfig.disable();
        AgentInstance fromConfigFile = AgentInstance.createFromConfig(agentConfig, new SystemEnvironment());
        goConfigService.currentCruiseConfig().agents().add(agentConfig);

        goConfigService.disableAgents(false, pending, fromConfigFile);

        GoConfigDao.CompositeConfigCommand command = new GoConfigDao.CompositeConfigCommand(
                GoConfigDao.createAddAgentCommand(pending.agentConfig()),
                GoConfigDao.updateApprovalStatus("UUID2", false));
        verify(goConfigDao).updateConfig(command);
    }

    @Test
    public void shouldEnableAgentWhenAlreadyInTheConfig() {
        String agentId = DatabaseAccessHelper.AGENT_UUID;
        AgentConfig agentConfig = new AgentConfig(agentId, "remote-host", "50.40.30.20");
        agentConfig.disable();
        AgentInstance instance = AgentInstance.createFromConfig(agentConfig, new SystemEnvironment());
        goConfigService.currentCruiseConfig().agents().add(agentConfig);
        goConfigService.disableAgents(false, instance);
        shouldPerformCommand(new GoConfigDao.CompositeConfigCommand(GoConfigDao.updateApprovalStatus(agentId, false)));
    }

    @Test
    public void shouldFindMaterialConfigBasedOnFingerprint() throws Exception {
        SvnMaterialConfig expected = new SvnMaterialConfig("repo", null, null, false);
        cruiseConfig = configWith(GoConfigMother.createPipelineConfigWithMaterialConfig(expected));
        when(goConfigDao.load()).thenReturn(cruiseConfig);

        MaterialConfig actual = goConfigService.materialForPipelineWithFingerprint("pipeline", expected.getFingerprint());
        assertThat((SvnMaterialConfig) actual, is(expected));
    }

    @Test
    public void shouldThrowExceptionWhenUnableToFindMaterialBasedOnFingerprint() throws Exception {
        SvnMaterialConfig svnMaterialConfig = new SvnMaterialConfig("repo", null, null, false);
        cruiseConfig = configWith(GoConfigMother.createPipelineConfigWithMaterialConfig(svnMaterialConfig));
        when(goConfigDao.load()).thenReturn(cruiseConfig);

        try {
            goConfigService.materialForPipelineWithFingerprint("pipeline", "bad-fingerprint");
            fail("Shouldn't be able to find material with incorrect fingerprint");
        } catch (Exception expected) {
            assertThat(expected.getMessage(), is("Pipeline [pipeline] does not have a material with fingerprint [bad-fingerprint]"));
        }
    }

    @Test
    public void shouldReturnDependentPiplinesForAGivenPipeline() throws Exception {
        PipelineConfig up = createPipelineConfig("blahPipeline", "blahStage");
        up.addMaterialConfig(MaterialConfigsMother.hgMaterialConfig());
        PipelineConfig down1 = GoConfigMother.createPipelineConfigWithMaterialConfig("down1", new DependencyMaterialConfig(new CaseInsensitiveString("blahPipeline"), new CaseInsensitiveString("blahStage")));
        PipelineConfig down2 = GoConfigMother.createPipelineConfigWithMaterialConfig("down2", new DependencyMaterialConfig(new CaseInsensitiveString("blahPipeline"), new CaseInsensitiveString("blahStage")));
        when(goConfigDao.load()).thenReturn(configWith(
                up, down1, down2, GoConfigMother.createPipelineConfigWithMaterialConfig("otherPipeline", new DependencyMaterialConfig(new CaseInsensitiveString("someotherpipeline"),
                new CaseInsensitiveString("blahStage")))
        ));

        assertThat(goConfigService.downstreamPipelinesOf("blahPipeline"), is(Arrays.asList(down1, down2)));
    }

    @Test
    public void shouldReturnUpstreamDependencyGraphForAGivenPipeline() throws Exception {
        PipelineConfig current = GoConfigMother.createPipelineConfigWithMaterialConfig("current", new DependencyMaterialConfig(new CaseInsensitiveString("up1"), new CaseInsensitiveString("first")),
                new DependencyMaterialConfig(new CaseInsensitiveString("up2"), new CaseInsensitiveString("first")));
        PipelineConfig up1 = GoConfigMother.createPipelineConfigWithMaterialConfig("up1", new DependencyMaterialConfig(new CaseInsensitiveString("uppest"), new CaseInsensitiveString("first")));
        PipelineConfig up2 = GoConfigMother.createPipelineConfigWithMaterialConfig("up2", new DependencyMaterialConfig(new CaseInsensitiveString("uppest"), new CaseInsensitiveString("first")));
        PipelineConfig uppest = GoConfigMother.createPipelineConfigWithMaterialConfig("uppest", MaterialConfigsMother.hgMaterialConfig());
        when(goConfigDao.load()).thenReturn(configWith(current, up1, up2, uppest));
        assertThat(goConfigService.upstreamDependencyGraphOf("current"), is(
                new PipelineConfigDependencyGraph(current,
                        new PipelineConfigDependencyGraph(up1, new PipelineConfigDependencyGraph(uppest)),
                        new PipelineConfigDependencyGraph(up2, new PipelineConfigDependencyGraph(uppest))
                )));
        /*
                         uppest
                        /     \
                       up1   up2
                        \     /
                        current
        */
    }

    @Test
    public void shouldDetermineIfStageExistsInCurrentConfig() throws Exception {
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs();
        pipelineConfigs.add(createPipelineConfig("pipeline", "stage", "job"));
        expectLoad(new BasicCruiseConfig(pipelineConfigs));
        assertThat(goConfigService.stageExists("pipeline", "randomstage"), is(false));
        assertThat(goConfigService.stageExists("pipeline", "stage"), is(true));
    }

    @Test
    public void shouldPersistPipelineSelections_WhenSecurityIsDisabled() {
        Date date = new DateTime(2000, 1, 1, 1, 1, 1, 1).toDate();
        when(clock.currentTime()).thenReturn(date);
        mockConfig();
        Matcher<PipelineSelections> pipelineSelectionsMatcher = hasValues(Arrays.asList("pipelineX", "pipeline3"), Arrays.asList("pipeline1", "pipeline2"), date, null);
        when(pipelineRepository.saveSelectedPipelines(argThat(pipelineSelectionsMatcher))).thenReturn(2L);
        assertThat(goConfigService.persistSelectedPipelines(null, null, Arrays.asList("pipelineX", "pipeline3"), true), is(2l));
        verify(pipelineRepository).saveSelectedPipelines(argThat(pipelineSelectionsMatcher));
    }

    @Test
    public void shouldPersistPipelineSelectionsAgainstUser_AlreadyHavingSelections() {
        Date date = new DateTime(2000, 1, 1, 1, 1, 1, 1).toDate();
        when(clock.currentTime()).thenReturn(date);
        mockConfigWithSecurity();
        User user = getUser("badger", 10L);
        PipelineSelections pipelineSelections = new PipelineSelections(Arrays.asList("pipeline2"), new Date(), user.getId(), true);
        when(pipelineRepository.findPipelineSelectionsByUserId(user.getId())).thenReturn(pipelineSelections);
        when(pipelineRepository.saveSelectedPipelines(pipelineSelections)).thenReturn(2L);

        long pipelineSelectionId = goConfigService.persistSelectedPipelines("1", user.getId(), Arrays.asList("pipelineX", "pipeline3"), true);

        assertThat(pipelineSelections.getSelections(), is("pipeline1,pipeline2"));
        assertThat(pipelineSelectionId, is(2l));
        verify(pipelineRepository).saveSelectedPipelines(pipelineSelections);
        verify(pipelineRepository).findPipelineSelectionsByUserId(user.getId());
        verify(pipelineRepository, never()).findPipelineSelectionsById("1");
    }

    @Test
    public void shouldPersistPipelineSelectionsAgainstUser_WhenUserHasNoSelections() {
        Date date = new DateTime(2000, 1, 1, 1, 1, 1, 1).toDate();
        when(clock.currentTime()).thenReturn(date);
        mockConfigWithSecurity();
        User user = getUser("badger", 10L);
        Matcher<PipelineSelections> pipelineSelectionsMatcher = hasValues(Arrays.asList("pipelineX", "pipeline3"), Arrays.asList("pipeline1", "pipeline2"), date, user.getId());
        when(pipelineRepository.findPipelineSelectionsByUserId(user.getId())).thenReturn(null);
        when(pipelineRepository.saveSelectedPipelines(argThat(pipelineSelectionsMatcher))).thenReturn(2L);

        long pipelineSelectionsId = goConfigService.persistSelectedPipelines("1", user.getId(), Arrays.asList("pipelineX", "pipeline3"), true);

        assertThat(pipelineSelectionsId, is(2l));
        verify(pipelineRepository).saveSelectedPipelines(argThat(pipelineSelectionsMatcher));
        verify(pipelineRepository).findPipelineSelectionsByUserId(user.getId());
        verify(pipelineRepository, never()).findPipelineSelectionsById("1");
    }

    @Test
    public void shouldPersistPipelineSelectionsShouldRemovePipelinesFromSelectedGroups() {
        CruiseConfig config = configWith(
                createGroup("group1", pipelineConfig("pipeline1"), pipelineConfig("pipeline2")),
                createGroup("group2", pipelineConfig("pipelineX")),
                createGroup("group3", pipelineConfig("pipeline3"), pipelineConfig("pipeline4")));
        when(goConfigDao.load()).thenReturn(config);
        goConfigService.persistSelectedPipelines(null, null, Arrays.asList("pipeline1", "pipeline2", "pipeline3"), true);
        verify(pipelineRepository).saveSelectedPipelines(argThat(hasValues(Arrays.asList("pipeline1", "pipeline2", "pipeline3"), Arrays.asList("pipelineX", "pipeline4"), clock.currentTime(), null)));
    }

    @Test
    public void shouldPersistInvertedListOfPipelineSelections_WhenBlacklistIsSelected() {
        Date date = new DateTime(2000, 1, 1, 1, 1, 1, 1).toDate();
        when(clock.currentTime()).thenReturn(date);
        mockConfigWithSecurity();

        User user = getUser("badger", 10L);
        PipelineSelections blacklistPipelineSelections = new PipelineSelections(new ArrayList<String>(), date, user.getId(), false);
        when(pipelineRepository.findPipelineSelectionsByUserId(user.getId())).thenReturn(blacklistPipelineSelections);

        goConfigService.persistSelectedPipelines(null, user.getId(), Arrays.asList("pipelineX", "pipeline3"), true);

        verify(pipelineRepository).saveSelectedPipelines(argThat(isAPipelineSelectionsInstanceWith(true, "pipeline1", "pipeline2")));
    }

    @Test
    public void shouldPersistNonInvertedListOfPipelineSelections_WhenWhitelistIsSelected() {
        Date date = new DateTime(2000, 1, 1, 1, 1, 1, 1).toDate();
        when(clock.currentTime()).thenReturn(date);
        mockConfigWithSecurity();

        User user = getUser("badger", 10L);
        PipelineSelections whitelistPipelineSelections = new PipelineSelections(new ArrayList<String>(), date, user.getId(), true);
        when(pipelineRepository.findPipelineSelectionsByUserId(user.getId())).thenReturn(whitelistPipelineSelections);

        goConfigService.persistSelectedPipelines(null, user.getId(), Arrays.asList("pipelineX", "pipeline3"), false);

        verify(pipelineRepository).saveSelectedPipelines(argThat(isAPipelineSelectionsInstanceWith(false, "pipelineX", "pipeline3")));
    }

    @Test
    public void shouldUpdateAlreadyPersistedSelection_WhenSecurityIsDisabled() {
        Date date = new DateTime(2000, 1, 1, 1, 1, 1, 1).toDate();
        when(clock.currentTime()).thenReturn(date);
        mockConfig();
        PipelineSelections pipelineSelections = new PipelineSelections(Arrays.asList("pip1"));
        when(pipelineRepository.findPipelineSelectionsById("123")).thenReturn(pipelineSelections);
        List<String> newPipelines = Arrays.asList("pipeline1", "pipeline2");

        goConfigService.persistSelectedPipelines("123", null, newPipelines, true);

        assertHasSelected(pipelineSelections, newPipelines);
        assertThat(pipelineSelections.lastUpdated(), is(date));
        verify(pipelineRepository).findPipelineSelectionsById("123");
        verify(pipelineRepository).saveSelectedPipelines(argThat(hasValues(Arrays.asList("pipeline1", "pipeline2"), Arrays.asList("pipelineX", "pipeline3"), clock.currentTime(), null)));
    }

    @Test
    public void shouldReturnPersistedPipelineSelectionsAgainstCookieId_WhenSecurityisDisabled() {
        PipelineSelections pipelineSelections = new PipelineSelections(Arrays.asList("pip1"));
        when(pipelineRepository.findPipelineSelectionsById("123")).thenReturn(pipelineSelections);
        assertThat(goConfigService.getSelectedPipelines("123", null), is(pipelineSelections));
        assertThat(goConfigService.getSelectedPipelines("", null), is(PipelineSelections.ALL));
        assertThat(goConfigService.getSelectedPipelines("345", null), is(PipelineSelections.ALL));
    }

    @Test
    public void shouldReturnPersistedPipelineSelectionsAgainstUser_WhenSecurityIsEnabled() {
        User loser = getUser("loser", 10L);
        User newUser = getUser("new user", 20L);
        when(userDao.findUser("new user")).thenReturn(newUser);
        mockConfigWithSecurity();
        PipelineSelections pipelineSelections = new PipelineSelections(Arrays.asList("pip1"));

        when(pipelineRepository.findPipelineSelectionsByUserId(loser.getId())).thenReturn(pipelineSelections);

        assertThat(goConfigService.getSelectedPipelines("1", loser.getId()), is(pipelineSelections));
        assertThat(goConfigService.getSelectedPipelines("1", newUser.getId()), is(PipelineSelections.ALL));
    }

    @Test
    public void shouldReturnAllPipelineSelections_WhenSecurityIsEnabled_AndNoPersistedSelections() {
        User user = getUser("loser", 10L);
        User newUser = getUser("new user", 20L);
        when(userDao.findUser("new user")).thenReturn(newUser);
        mockConfigWithSecurity();

        when(pipelineRepository.findPipelineSelectionsByUserId(user.getId())).thenReturn(null);
        when(pipelineRepository.findPipelineSelectionsById("1")).thenReturn(null);

        assertThat(goConfigService.getSelectedPipelines("1", newUser.getId()), is(PipelineSelections.ALL));
    }

    @Test
    public void shouldReturnPersistedPipelineSelectionsAgainstCookieId_WhenSecurityIsEnabled_AndUserSelectionsDoesNotExist() {
        User user = getUser("loser", 10L);
        mockConfigWithSecurity();
        PipelineSelections pipelineSelections = new PipelineSelections(Arrays.asList("pip1"));

        when(pipelineRepository.findPipelineSelectionsByUserId(user.getId())).thenReturn(null);
        when(pipelineRepository.findPipelineSelectionsById("1")).thenReturn(pipelineSelections);

        assertThat(goConfigService.getSelectedPipelines("1", user.getId()), is(pipelineSelections));
    }

    @Test
    public void shouldRegisterBaseUrlChangeListener() throws Exception {
        CruiseConfig cruiseConfig = new GoConfigMother().cruiseConfigWithOnePipelineGroup();
        stub(goConfigDao.load()).toReturn(cruiseConfig);
        goConfigService.initialize();
        verify(goConfigDao).registerListener(any(BaseUrlChangeListener.class));
    }

    @Test
    public void getConfigAtVersion_shouldFetchRequiredVersion() throws Exception {
        GoConfigRevision revision = new GoConfigRevision("v1", "md5-1", "loser", "100.3.9.1", new TimeProvider());
        when(configRepo.getRevision("md5-1")).thenReturn(revision);
        assertThat(goConfigService.getConfigAtVersion("md5-1"), is(revision));
    }

    @Test
    public void getNotThrowUpWhenRevisionIsNotFound() throws Exception {
        when(configRepo.getRevision("md5-1")).thenThrow(new IllegalArgumentException("did not find the revision"));
        try {
            assertThat(goConfigService.getConfigAtVersion("md5-1"), is(nullValue()));
        } catch (Exception e) {
            fail("should not have thrown up");
        }

    }

    @Test
    public void shouldReturnListOfUpstreamPipelineConfigValidForFetchArtifact() {
        PipelineConfig unrelatedPipeline = PipelineConfigMother.pipelineConfig("some.random.pipeline");
        PipelineConfig upstream = PipelineConfigMother.createPipelineConfig("upstream", "upstream.stage", "upstream.job");
        upstream.add(StageConfigMother.stageConfig("upstream.stage.2"));
        upstream.add(StageConfigMother.stageConfig("upstream.stage.3"));
        PipelineConfig downstream = PipelineConfigMother.createPipelineConfig("pipeline", "stage.1", "jobs");
        downstream.add(StageConfigMother.stageConfig("stage.2"));
        downstream.add(StageConfigMother.stageConfig("current.stage"));

        downstream.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("upstream.stage.2")));

        CruiseConfig cruiseConfig = configWith(upstream, downstream, unrelatedPipeline);
        when(goConfigDao.load()).thenReturn(cruiseConfig);

        List<PipelineConfig> fetchablePipelines = goConfigService.pipelinesForFetchArtifacts("pipeline");

        assertThat(fetchablePipelines.size(), is(2));
        assertThat(fetchablePipelines, hasItem(upstream));
        assertThat(fetchablePipelines, hasItem(downstream));
    }

    @Test
    public void uiBasedUpdateCommandShouldReturnTheConfigPassedByUpdateOperation() {
        UiBasedConfigUpdateCommand command = new UiBasedConfigUpdateCommand("md5", null, null) {
            public boolean canContinue(CruiseConfig cruiseConfig) {
                return true;
            }
        };
        CruiseConfig after = new BasicCruiseConfig();
        command.afterUpdate(after);
        assertThat(command.configAfter(), sameInstance(after));
    }

    @Test
    public void shouldUseInstanceFactoryToCreateAStageInstanceForTheSpecifiedPipelineStageCombination() throws Exception {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("foo-pipeline", "foo-stage", "foo-job");
        DefaultSchedulingContext schedulingContext = new DefaultSchedulingContext("loser");
        String md5 = "foo-md5";

        CruiseConfig config = mock(BasicCruiseConfig.class);
        when(config.pipelineConfigByName(new CaseInsensitiveString("foo-pipeline"))).thenReturn(pipelineConfig);
        when(config.getMd5()).thenReturn(md5);
        when(goConfigDao.load()).thenReturn(config);

        goConfigService.scheduleStage("foo-pipeline", "foo-stage", schedulingContext);

        verify(instanceFactory).createStageInstance(pipelineConfig, new CaseInsensitiveString("foo-stage"), schedulingContext, md5, clock);
    }

    @Test
    public void shouldReturnFalseIfMD5DoesNotMatch() {
        String staleMd5 = "oldmd5";
        when(goConfigDao.md5OfConfigFile()).thenReturn("newmd5");
        assertThat(goConfigService.doesMd5Match(staleMd5), is(false));
    }

    @Test
    public void shouldReturnTrueifMd5Matches() {
        String staleMd5 = "md5";
        when(goConfigDao.md5OfConfigFile()).thenReturn("md5");
        assertThat(goConfigService.doesMd5Match(staleMd5), is(true));
    }

    @Test
    public void shouldThrowExceptionIfGroupDoesNotExist_WhenUserIsAdmin() {
        CaseInsensitiveString adminName = new CaseInsensitiveString("admin");
        GoConfigMother mother = new GoConfigMother();
        mother.enableSecurityWithPasswordFile(cruiseConfig);
        cruiseConfig.server().security().adminsConfig().add(new AdminUser(adminName));
        String groupName = String.format("group_%s", UUID.randomUUID());
        try {
            goConfigService.isUserAdminOfGroup(adminName, groupName);
            fail("Should fail since group does not exist");
        } catch (Exception e) {
            assertThat(e, is(instanceOf(PipelineGroupNotFoundException.class)));
        }
    }

    @Test
    public void shouldThrowExceptionIfGroupDoesNotExist_WhenUserIsNonAdmin() {
        CaseInsensitiveString adminName = new CaseInsensitiveString("admin");
        String groupName = String.format("group_%s", UUID.randomUUID());
        GoConfigMother mother = new GoConfigMother();
        mother.enableSecurityWithPasswordFile(cruiseConfig);
        cruiseConfig.server().security().adminsConfig().add(new AdminUser(adminName));
        try {
            goConfigService.isUserAdminOfGroup(new CaseInsensitiveString("foo"), groupName);
            fail("Should fail since group does not exist");
        } catch (Exception e) {
            assertThat(e, is(instanceOf(PipelineGroupNotFoundException.class)));
        }
    }

    @Test
    public void shouldReturnTrueIfUserIsTheAdminForGroup() {
        CaseInsensitiveString adminName = new CaseInsensitiveString("admin");
        String groupName = String.format("group_%s", UUID.randomUUID());
        GoConfigMother mother = new GoConfigMother();
        mother.enableSecurityWithPasswordFile(cruiseConfig);
        cruiseConfig.server().security().adminsConfig().add(new AdminUser(adminName));
        mother.addPipelineWithGroup(cruiseConfig, groupName, "pipeline", "stage");
        mother.addAdminUserForPipelineGroup(cruiseConfig, "user", groupName);
        assertThat(goConfigService.isUserAdminOfGroup(new CaseInsensitiveString("user"), groupName), is(true));
    }

    @Test
    public void shouldReturnValidOnUpdateXml() throws Exception {
        String groupName = "group_name";
        String md5 = "md5";
        cruiseConfig = new BasicCruiseConfig();
        expectLoad(cruiseConfig);
        new GoConfigMother().addPipelineWithGroup(cruiseConfig, groupName, "pipeline_name", "stage_name", "job_name");
        expectLoadForEditing(cruiseConfig);
        when(goConfigDao.md5OfConfigFile()).thenReturn(md5);

        GoConfigService.XmlPartialSaver partialSaver = goConfigService.groupSaver(groupName);
        String renamedGroupName = "renamed_group_name";
        GoConfigValidity validity = partialSaver.saveXml(groupXml(renamedGroupName), md5);
        assertThat(validity.isValid(), Matchers.is(true));
        assertThat(validity.errorMessage(), Matchers.is(""));
        verify(goConfigDao).updateConfig(argThat(cruiseConfigIsUpdatedWith(renamedGroupName, "new_name", "${COUNT}-#{foo}")));
    }

    @Test
    public void shouldReturnInvalidWhenPipelineGroupPartialIsInvalid() throws Exception {
        String groupName = "group_name";
        String md5 = "md5";
        cruiseConfig = new BasicCruiseConfig();
        expectLoad(cruiseConfig);
        new GoConfigMother().addPipelineWithGroup(cruiseConfig, groupName, "pipeline_name", "stage_name", "job_name");
        expectLoadForEditing(cruiseConfig);
        when(goConfigDao.md5OfConfigFile()).thenReturn(md5);

        String pipelineGroupContent = groupXmlWithInvalidElement(groupName);
        GoConfigValidity validity = goConfigService.groupSaver(groupName).saveXml(pipelineGroupContent, "md5");
        assertThat(validity.isValid(), Matchers.is(false));
        assertThat(validity.errorMessage(), containsString("Invalid content was found starting with element 'unknown'"));
        verify(goConfigDao, never()).updateConfig(any(UpdateConfigCommand.class));
    }

    @Test
    public void shouldReturnInvalidWhenPipelineGroupPartialHasInvalidAttributeValue() throws Exception {
        String groupName = "group_name";
        String md5 = "md5";
        cruiseConfig = new BasicCruiseConfig();
        expectLoad(cruiseConfig);
        new GoConfigMother().addPipelineWithGroup(cruiseConfig, groupName, "pipeline_name", "stage_name", "job_name");
        expectLoadForEditing(cruiseConfig);
        when(goConfigDao.md5OfConfigFile()).thenReturn(md5);

        String pipelineGroupContent = groupXmlWithInvalidAttributeValue(groupName);
        GoConfigValidity validity = goConfigService.groupSaver(groupName).saveXml(pipelineGroupContent, "md5");
        assertThat(validity.isValid(), Matchers.is(false));
        assertThat(validity.errorMessage(), containsString("Name is invalid. \"pipeline@$^\""));
        verify(goConfigDao, never()).updateConfig(any(UpdateConfigCommand.class));
    }

    @Test
    public void shouldReturnInvalidWhenPipelineGroupPartialXmlIsInvalid() throws Exception {
        String groupName = "group_name";
        String md5 = "md5";
        cruiseConfig = new BasicCruiseConfig();
        expectLoad(cruiseConfig);
        new GoConfigMother().addPipelineWithGroup(cruiseConfig, groupName, "pipeline_name", "stage_name", "job_name");
        expectLoadForEditing(cruiseConfig);
        when(goConfigDao.md5OfConfigFile()).thenReturn(md5);

        GoConfigValidity validity = goConfigService.groupSaver(groupName).saveXml("<foobar>", "md5");
        assertThat(validity.isValid(), Matchers.is(false));
        assertThat(validity.errorMessage(), containsString("XML document structures must start and end within the same entity"));
        verify(goConfigDao, never()).updateConfig(any(UpdateConfigCommand.class));
    }

    @Test
    public void shouldFindConfigChangesForGivenConfigMd5() throws Exception {
        goConfigService.configChangesFor("md5-5", "md5-4", new HttpLocalizedOperationResult());
        verify(configRepo).configChangesFor("md5-5", "md5-4");
    }

    @Test
    public void shouldUpdateResultAsConfigRevisionNotFoundWhenConfigChangeIsNotFound() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Localizer localizer = mock(Localizer.class);
        when(configRepo.configChangesFor("md5-5", "md5-4")).thenThrow(new IllegalArgumentException("something"));
        goConfigService.configChangesFor("md5-5", "md5-4", result);
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(SC_BAD_REQUEST));
        result.message(localizer);
        verify(localizer).localize("CONFIG_VERSION_NOT_FOUND", new Object[]{});
    }

    @Test
    public void shouldUpdateResultAsCouldNotRetrieveConfigDiffWhenGenericExceptionOccurs() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Localizer localizer = mock(Localizer.class);
        when(configRepo.configChangesFor("md5-5", "md5-4")).thenThrow(new RuntimeException("something"));
        goConfigService.configChangesFor("md5-5", "md5-4", result);
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(SC_INTERNAL_SERVER_ERROR));
        result.message(localizer);
        verify(localizer).localize("COULD_NOT_RETRIEVE_CONFIG_DIFF", new Object[]{});
    }

    @Test
    public void shouldReturnWasMergedInConfigUpdateResponse_WhenConfigIsMerged() {
        when(goConfigDao.updateConfig(org.mockito.Matchers.<UpdateConfigCommand>any())).thenReturn(ConfigSaveState.MERGED);
        ConfigUpdateResponse configUpdateResponse = goConfigService.updateConfigFromUI(mock(UpdateConfigFromUI.class), "md5", new Username(new CaseInsensitiveString("user")),
                new HttpLocalizedOperationResult());
        assertThat(configUpdateResponse.wasMerged(), is(true));
    }

    @Test
    public void shouldReturnNotMergedInConfigUpdateResponse_WhenConfigIsUpdated() {
        when(goConfigDao.updateConfig(org.mockito.Matchers.<UpdateConfigCommand>any())).thenReturn(ConfigSaveState.UPDATED);
        ConfigUpdateResponse configUpdateResponse = goConfigService.updateConfigFromUI(mock(UpdateConfigFromUI.class), "md5", new Username(new CaseInsensitiveString("user")),
                new HttpLocalizedOperationResult());
        assertThat(configUpdateResponse.wasMerged(), is(false));
    }

    @Test
    public void shouldReturnNotMergedInConfigUpdateResponse_WhenConfigUpdateFailed() throws Exception {
        when(goConfigDao.updateConfig(org.mockito.Matchers.<UpdateConfigCommand>any())).thenThrow(new ConfigFileHasChangedException());
        expectLoadForEditing(cruiseConfig);
        ConfigUpdateResponse configUpdateResponse = goConfigService.updateConfigFromUI(mock(UpdateConfigFromUI.class), "md5", new Username(new CaseInsensitiveString("user")),
                new HttpLocalizedOperationResult());
        assertThat(configUpdateResponse.wasMerged(), is(false));
    }

    @Test
    public void badConfigShouldContainOldMD5_WhenConfigUpdateFailed(){
        when(goConfigDao.updateConfig(org.mockito.Matchers.<UpdateConfigCommand>any())).thenThrow(new RuntimeException(getGoConfigInvalidException()));
        ConfigUpdateResponse configUpdateResponse = goConfigService.updateConfigFromUI(mock(UpdateConfigFromUI.class), "old-md5", new Username(new CaseInsensitiveString("user")),
                new HttpLocalizedOperationResult());
        assertThat(configUpdateResponse.wasMerged(), is(false));
        assertThat(configUpdateResponse.getCruiseConfig().getMd5(), is("old-md5"));
    }

    @Test
    public void configShouldContainOldMD5_WhenConfigMergeFailed(){
        when(goConfigDao.loadForEditing()).thenReturn(new BasicCruiseConfig());
        when(goConfigDao.updateConfig(org.mockito.Matchers.<UpdateConfigCommand>any())).thenThrow(new ConfigFileHasChangedException());
        ConfigUpdateResponse configUpdateResponse = goConfigService.updateConfigFromUI(mock(UpdateConfigFromUI.class), "old-md5", new Username(new CaseInsensitiveString("user")),
                new HttpLocalizedOperationResult());
        assertThat(configUpdateResponse.wasMerged(), is(false));
        assertThat(configUpdateResponse.getCruiseConfig().getMd5(), is("old-md5"));
    }

    @Test
    @Ignore("Deprecated usage. Will be removed soon")
    public void shouldReturnConfigValidityWithMergedStateWhenConfigIsMerged() throws Exception {
        CruiseConfig config = configWithPipeline();
        when(goConfigDao.loadForEditing()).thenReturn(config);
        when(goConfigDao.updateConfig(org.mockito.Matchers.<UpdateConfigCommand>any())).thenReturn(ConfigSaveState.MERGED);

        GoConfigValidity goConfigValidity = goConfigService.buildSaver("pipeline", "stage", 0).saveXml("<job name='first'></job>", "md5");

        assertThat(goConfigValidity.isValid(), is(true));
        assertThat(goConfigValidity.wasMerged(), is(true));
    }

    @Test
    public void shouldReturnConfigValidityWithUpdatedStateWhenConfigIsUpdated() throws Exception {
        CruiseConfig config = configWithPipeline();
        when(goConfigDao.loadForEditing()).thenReturn(config);
        when(goConfigDao.updateConfig(org.mockito.Matchers.<UpdateConfigCommand>any())).thenReturn(ConfigSaveState.UPDATED);

        GoConfigValidity goConfigValidity = goConfigService.buildSaver("pipeline", "stage", 0).saveXml("<job name='first'></job>", "md5");

        assertThat(goConfigValidity.isValid(), is(true));
        assertThat(goConfigValidity.wasMerged(), is(false));
    }

    @Test
    public void shouldReturnConfigStateFromDaoLayer_WhenUpdatingEnvironment() {
        ConfigSaveState expectedSaveState = ConfigSaveState.MERGED;
        when(goConfigDao.updateConfig(org.mockito.Matchers.<UpdateConfigCommand>any())).thenReturn(expectedSaveState);
        ConfigSaveState configSaveState = goConfigService.updateEnvironment("env", new BasicEnvironmentConfig(), "md5");
        assertThat(configSaveState, is(expectedSaveState));
    }

    @Test
    public void shouldReturnConfigStateFromDaoLayer_WhenUpdatingServerConfig() {
        ConfigSaveState expectedSaveState = ConfigSaveState.MERGED;
        when(goConfigDao.updateConfig(org.mockito.Matchers.<UpdateConfigCommand>any())).thenReturn(expectedSaveState);
        ConfigSaveState configSaveState = goConfigService.updateServerConfig(new MailHost(new GoCipher()), null, null, true, "md5", null, null, null, null, "http://site",
                "https://site", "location");
        assertThat(configSaveState, is(expectedSaveState));
    }

    @Test
    public void shouldSayThatAUserIsAuthorizedToEditTemplateWhenTheUserIsAnAdminOfThisTemplate() throws Exception {
        CruiseConfig config = new BasicCruiseConfig();
        String templateName = "template";
        CaseInsensitiveString templateAdminName = new CaseInsensitiveString("templateAdmin");

        GoConfigMother.enableSecurityWithPasswordFile(config);
        GoConfigMother.addUserAsSuperAdmin(config, "theSuperAdmin");
        config.addTemplate(createTemplate(templateName, new Authorization(new AdminsConfig(new AdminUser(templateAdminName)))));

        when(goConfigService.getCurrentConfig()).thenReturn(config);

        assertThat(goConfigService.isAuthorizedToEditTemplate(templateName, new Username(templateAdminName)), is(true));
        assertThat(goConfigService.isAuthorizedToEditTemplate(templateName, new Username(new CaseInsensitiveString("someOtherUserWhoIsNotAnAdmin"))), is(false));
    }

    @Test
    public void shouldSayThatAUserIsAuthorizedToViewAndEditTemplatesWhenTheUserHasPermissionsForAtLeastOneTemplate() throws Exception {
        CruiseConfig config = new BasicCruiseConfig();
        String theSuperAdmin = "theSuperAdmin";
        String templateName = "template";
        String secondTemplateName = "secondTemplate";
        CaseInsensitiveString templateAdminName = new CaseInsensitiveString("templateAdmin");
        CaseInsensitiveString secondTemplateAdminName = new CaseInsensitiveString("secondTemplateAdmin");

        GoConfigMother.enableSecurityWithPasswordFile(config);
        GoConfigMother.addUserAsSuperAdmin(config, theSuperAdmin);
        config.addTemplate(createTemplate(templateName, new Authorization(new AdminsConfig(new AdminUser(templateAdminName)))));
        config.addTemplate(createTemplate(secondTemplateName, new Authorization(new AdminsConfig(new AdminUser(secondTemplateAdminName)))));

        when(goConfigService.getCurrentConfig()).thenReturn(config);

        assertThat(goConfigService.isAuthorizedToViewAndEditTemplates(new Username(templateAdminName)), is(true));
        assertThat(goConfigService.isAuthorizedToViewAndEditTemplates(new Username(secondTemplateAdminName)), is(true));
        assertThat(goConfigService.isAuthorizedToViewAndEditTemplates(new Username(new CaseInsensitiveString(theSuperAdmin))), is(false));
        assertThat(goConfigService.isAuthorizedToViewAndEditTemplates(new Username(new CaseInsensitiveString("someOtherUserWhoIsNotAdminOfAnyTemplates"))), is(false));
    }

    @Test
    public void shouldSayThatAUserIsAuthorizedToEditTemplateWhenTheUserIsASuperAdmin() throws Exception {
        String adminName = "theSuperAdmin";
        String templateName = "template";

        GoConfigMother.enableSecurityWithPasswordFile(cruiseConfig);
        GoConfigMother.addUserAsSuperAdmin(cruiseConfig, adminName).addTemplate(createTemplate(templateName));

        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);

        assertThat(goConfigService.isAuthorizedToEditTemplate(templateName, new Username(new CaseInsensitiveString(adminName))), is(true));
    }

	@Test
	public void shouldDelegateToConfig_getAllPipelinesInGroup() throws Exception {
		CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);
		expectLoad(cruiseConfig);
		goConfigService.getAllPipelinesInGroup("group");
		verify(cruiseConfig).pipelines("group");
	}

    @Test
    public void shouldNotUpdatePipelineSelectionsWhenTheUserIsAnonymousAndHasNeverSelectedPipelines() {
        goConfigService.updateUserPipelineSelections(null, null, new CaseInsensitiveString("pipelineNew"));

        verify(pipelineRepository, times(0)).saveSelectedPipelines(argThat(Matchers.any(PipelineSelections.class)));
    }

    @Test
    public void shouldNotUpdatePipelineSelectionsWhenTheUserIsAnonymousAndHasSelectedPipelines_WithBlacklist() {
        when(pipelineRepository.findPipelineSelectionsById("1")).thenReturn(new PipelineSelections(Arrays.asList("pipeline1", "pipeline2"), null, null, true));

        goConfigService.updateUserPipelineSelections("1", null, new CaseInsensitiveString("pipelineNew"));

        verify(pipelineRepository).findPipelineSelectionsById("1");
        verify(pipelineRepository, times(0)).saveSelectedPipelines(argThat(Matchers.any(PipelineSelections.class)));
    }

    @Test
    public void shouldUpdatePipelineSelectionsWhenTheUserIsAnonymousAndHasSelectedPipelines_WithWhitelist() {
        when(pipelineRepository.findPipelineSelectionsById("1")).thenReturn(new PipelineSelections(Arrays.asList("pipeline1", "pipeline2"), null, null, false));

        goConfigService.updateUserPipelineSelections("1", null, new CaseInsensitiveString("pipelineNew"));

        verify(pipelineRepository).findPipelineSelectionsById("1");
        verify(pipelineRepository, times(1)).saveSelectedPipelines(argThat(isAPipelineSelectionsInstanceWith(false, "pipeline1", "pipeline2", "pipelineNew")));
    }

    @Test
    public void shouldNotUpdatePipelineSelectionsWhenTheUserIsLoggedIn_WithBlacklist() {
        mockConfigWithSecurity();

        when(pipelineRepository.findPipelineSelectionsByUserId(1L)).thenReturn(new PipelineSelections(Arrays.asList("pipeline1", "pipeline2"), null, null, true));

        goConfigService.updateUserPipelineSelections(null, 1L, new CaseInsensitiveString("pipelineNew"));

        verify(pipelineRepository).findPipelineSelectionsByUserId(1L);
        verify(pipelineRepository, times(0)).saveSelectedPipelines(argThat(Matchers.any(PipelineSelections.class)));
    }

    @Test
    public void shouldUpdatePipelineSelectionsWhenTheUserIsLoggedIn_WithWhitelist() {
        mockConfigWithSecurity();

        when(pipelineRepository.findPipelineSelectionsByUserId(1L)).thenReturn(new PipelineSelections(Arrays.asList("pipeline1", "pipeline2"), null, null, false));

        goConfigService.updateUserPipelineSelections(null, 1L, new CaseInsensitiveString("pipelineNew"));

        verify(pipelineRepository).findPipelineSelectionsByUserId(1L);
        verify(pipelineRepository, times(1)).saveSelectedPipelines(argThat(isAPipelineSelectionsInstanceWith(false, "pipeline1", "pipeline2", "pipelineNew")));
    }

    private PipelineConfig createPipelineConfig(String pipelineName, String stageName, String... buildNames) {
        PipelineConfig pipeline = new PipelineConfig(new CaseInsensitiveString(pipelineName), new MaterialConfigs());
        pipeline.add(new StageConfig(new CaseInsensitiveString(stageName), jobConfigs(buildNames)));
        return pipeline;
    }

    private JobConfigs jobConfigs(String... buildNames) {
        JobConfigs jobConfigs = new JobConfigs();
        for (String buildName : buildNames) {
            jobConfigs.add(new JobConfig(buildName));
        }
        return jobConfigs;
    }

    private GoConfigService goConfigServiceWithInvalidStatus() throws Exception {
        goConfigDao = mock(GoConfigDao.class, "badCruiseConfigManager");
        when(goConfigDao.checkConfigFileValid()).thenReturn(GoConfigValidity.invalid(new JDOMParseException("JDom exception", new RuntimeException())));
        return new GoConfigService(goConfigDao, pipelineRepository, new SystemTimeClock(), mock(GoConfigMigration.class), goCache, null, userDao,
                ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService, instanceFactory);
    }

    private CruiseConfig mockConfig() {
        CruiseConfig config = configWith(
                createGroup("group0", pipelineConfig("pipeline1"), pipelineConfig("pipeline2")),
                createGroup("group1", pipelineConfig("pipelineX")),
                createGroup("group2", pipelineConfig("pipeline3")));
        when(goConfigDao.load()).thenReturn(config);
        return config;
    }

    private CruiseConfig mockConfigWithSecurity() {
        CruiseConfig config = mockConfig();
        config.server().useSecurity(new SecurityConfig(null, new PasswordFileConfig("path"), true));
        return config;
    }

    private GoConfigInvalidException getGoConfigInvalidException() {
        ConfigErrors configErrors = new ConfigErrors();
        configErrors.add("command", "command cannot be empty");
        ArrayList<ConfigErrors> list = new ArrayList<ConfigErrors>();
        list.add(configErrors);
        return new GoConfigInvalidException(new BasicCruiseConfig(), list);
    }

    private Matcher<UpdateConfigCommand> cruiseConfigIsUpdatedWith(final String groupName, final String newPipelineName, final String labelTemplate) {
        return new Matcher<UpdateConfigCommand>() {
            @Override
            public boolean matches(Object item) {
                UpdateConfigCommand configCommand = (UpdateConfigCommand) item;
                CruiseConfig updatedConfig = null;
                try {
                    updatedConfig = configCommand.update(null);
                } catch (Exception e) {
                    Assert.fail(String.format("Updating config through exception : %s", e));
                }
                PipelineConfigs group = updatedConfig.findGroup(groupName);
                PipelineConfig pipeline = group.findBy(new CaseInsensitiveString(newPipelineName));
                assertThat(pipeline.name(), is(new CaseInsensitiveString(newPipelineName)));
                assertThat(pipeline.getLabelTemplate(), is(labelTemplate));
                assertThat(pipeline.materialConfigs().first(), is(IsInstanceOf.instanceOf(SvnMaterialConfig.class)));
                assertThat(pipeline.materialConfigs().first().getUriForDisplay(), is("file:///tmp/foo"));

                return true;
            }

            @Override
            public void describeMismatch(Object o, Description description) {
                description.appendText("There was a mismatch!");
            }

            @Override
            public void _dont_implement_Matcher___instead_extend_BaseMatcher_() {
            }

            @Override
            public void describeTo(Description description) {
            }
        };
    }

    private User getUser(final String userName, long id) {
        long userId = id;
        User user = new User(userName);
        user.setId(userId);
        when(userDao.findUser(userName)).thenReturn(user);
        return user;
    }

    private Matcher<PipelineSelections> hasValues(final List<String> isVisible, final List<String> isNotVisible, final Date today, final Long userId) {
        return new BaseMatcher<PipelineSelections>() {
            public boolean matches(Object o) {
                PipelineSelections pipelineSelections = (PipelineSelections) o;
                assertHasSelected(pipelineSelections, isVisible);
                assertHasSelected(pipelineSelections, isNotVisible, false);
                assertThat(pipelineSelections.lastUpdated(), is(today));
                assertThat(pipelineSelections.userId(), is(userId));
                return true;
            }

            public void describeTo(Description description) {
            }
        };
    }

    private Matcher<PipelineSelections> isAPipelineSelectionsInstanceWith(final boolean isBlacklist, final String... pipelineSelectionsInInstance) {
        return new BaseMatcher<PipelineSelections>() {
            public boolean matches(Object o) {
                PipelineSelections pipelineSelections = (PipelineSelections) o;
                assertThat(pipelineSelections.isBlacklist(), is(isBlacklist));

                List<String> expectedSelectionsAsList = Arrays.asList(pipelineSelectionsInInstance);
                assertEquals(pipelineSelections.getSelections(), ListUtil.join(expectedSelectionsAsList, ","));

                return true;
            }

            public void describeTo(Description description) {
            }
        };
    }

    private void assertHasSelected(PipelineSelections pipelineSelections, List<String> pipelines) {
        assertHasSelected(pipelineSelections, pipelines, true);
    }

    private void assertHasSelected(PipelineSelections pipelineSelections, List<String> pipelines, boolean has) {
        String message = "Expected: " + pipelines + " to include " + pipelineSelections + ": (" + has + ").";
        for (String pipeline : pipelines) {
            assertThat(message + ". Failed to find: " + pipeline, pipelineSelections.includesPipeline(pipelineConfig(pipeline)), is(has));
        }
    }

    private String groupXml(final String groupName) {
        return "<pipelines group=\"" + groupName + "\">\n"
                + "  <pipeline name=\"new_name\" labeltemplate=\"${COUNT}-#{foo}\">\n"
                + "     <params>\n"
                + "      <param name=\"foo\">test</param>\n"
                + "    </params>"
                + "    <materials>\n"
                + "      <svn url=\"file:///tmp/foo\" />\n"
                + "    </materials>\n"
                + "    <stage name=\"stage_name\">\n"
                + "      <jobs>\n"
                + "        <job name=\"job_name\" />\n"
                + "      </jobs>\n"
                + "    </stage>\n"
                + "  </pipeline>\n"
                + "</pipelines>";

    }

    private String groupXmlWithInvalidElement(final String groupName) {
        return "<pipelines group='" + groupName + "'>"
                + "  <unknown/>"
                + "<pipeline name='pipeline'>\n"
                + "    <materials>\n"
                + "         <svn url ='svnurl' dest='a'/>\n"
                + "    </materials>\n"
                + "  <stage name='firstStage'>"
                + "     <jobs>"
                + "         <job name='jobName'/>"
                + "      </jobs>"
                + "  </stage>"
                + "</pipeline>"
                + "</pipelines>";
    }

    private String groupXmlWithInvalidAttributeValue(final String groupName) {
        return "<pipelines group='" + groupName + "'>"
                + "<pipeline name='pipeline@$^'>\n"
                + "    <materials>\n"
                + "         <svn url ='svnurl' dest='a'/>\n"
                + "    </materials>\n"
                + "  <stage name='firstStage'>"
                + "     <jobs>"
                + "         <job name='jobName'/>"
                + "      </jobs>"
                + "  </stage>"
                + "</pipeline>"
                + "</pipelines>";
    }


}
