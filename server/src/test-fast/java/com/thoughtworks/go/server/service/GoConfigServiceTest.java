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
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.exceptions.StageNotFoundException;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.config.rules.Allow;
import com.thoughtworks.go.config.rules.Rules;
import com.thoughtworks.go.config.update.FullConfigUpdateCommand;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.listener.BaseUrlChangeListener;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.PipelineConfigDependencyGraph;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.PipelineRepository;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.thoughtworks.go.helper.ConfigFileFixture.configWith;
import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static com.thoughtworks.go.helper.MaterialConfigsMother.svn;
import static java.lang.String.format;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    private InstanceFactory instanceFactory;
    private SystemEnvironment systemEnvironment;

    @BeforeEach
    public void setup() throws Exception {
        new SystemEnvironment().setProperty(SystemEnvironment.ENFORCE_SERVER_IMMUTABILITY, "N");

        configRepo = mock(ConfigRepository.class);
        goConfigDao = mock(GoConfigDao.class);
        pipelineRepository = mock(PipelineRepository.class);
        systemEnvironment = mock(SystemEnvironment.class);

        cruiseConfig = unchangedConfig();
        expectLoad(cruiseConfig);
        this.clock = mock(Clock.class);
        goCache = mock(GoCache.class);
        instanceFactory = mock(InstanceFactory.class);

        ConfigElementImplementationRegistry registry = ConfigElementImplementationRegistryMother.withNoPlugins();
        goConfigService = new GoConfigService(goConfigDao, this.clock, new GoConfigMigration(new TimeProvider(),
                registry), goCache, configRepo, registry,
                instanceFactory, mock(CachedGoPartials.class), systemEnvironment);
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
    public void shouldReturnTrueIfStageHasTestsAndFalseIfItDoesnt() throws Exception {
        PipelineConfigs newPipelines = new BasicPipelineConfigs();
        PipelineConfig pipelineConfig = createPipelineConfig("pipeline", "name", "plan");
        pipelineConfig.add(StageConfigMother.stageConfigWithArtifact("stage1", "job1", ArtifactType.test));
        pipelineConfig.add(StageConfigMother.stageConfigWithArtifact("stage2", "job2", ArtifactType.build));
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
    public void shouldRememberValidityWhenCruiseConfigLoaderHasInvalidConfigFile() throws Exception {
        GoConfigService service = goConfigServiceWithInvalidStatus();
        assertThat(service.checkConfigFileValid().isValid(), is(false));
        assertThat(((GoConfigValidity.InvalidGoConfig) service.checkConfigFileValid()).errorMessage(), is("JDom exception"));
    }

    @Test
    public void shouldNotHaveErrorMessageWhenConfigFileValid() {
        when(goConfigDao.checkConfigFileValid()).thenReturn(GoConfigValidity.valid());
        GoConfigValidity configValidity = goConfigService.checkConfigFileValid();
        assertThat(configValidity.isValid(), is(true));
    }

    private CruiseConfig configWithPipeline() {
        PipelineConfig pipelineConfig = createPipelineConfig("pipeline", "stage", "first");
        pipelineConfig.addMaterialConfig(MaterialConfigsMother.hgMaterialConfig());
        CruiseConfig config = configWith(pipelineConfig);
        config.server().setArtifactsDir("/var/logs");
        return config;
    }

    @Test
    public void shouldReturnInvalidWhenWholeConfigIsInvalidAndShouldUpgrade() throws Exception {
        CruiseConfig config = configWithPipeline();
        when(goConfigDao.loadForEditing()).thenReturn(config);
        String configContent = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\"14\">\n"
                + "<server>"
                + "  <artifacts>"
                + "    <artifactsDir>artifacts</artifactsDir>"
                + "  </artifacts>"
                + "</server>"
                + "<unknown/></cruise>";
        GoConfigValidity validity = goConfigService.fileSaver(true).saveXml(configContent, "md5");
        assertThat(((GoConfigValidity.InvalidGoConfig) validity).errorMessage(), is("Cruise config file with version 14 is invalid. Unable to upgrade."));
    }

    @Test
    public void shouldReturnInvalidWhenWholeConfigIsInvalid() throws Exception {
        CruiseConfig config = configWithPipeline();
        when(goConfigDao.loadForEditing()).thenReturn(config);
        String configContent = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\"" + GoConstants.CONFIG_SCHEMA_VERSION + "\">\n"
                + "<server><artifacts>"
                + "<artifactsDir>artifacts</artifactsDir>"
                + "</artifacts></server><unknown/></cruise>";
        GoConfigValidity validity = goConfigService.fileSaver(false).saveXml(configContent, "md5");
        assertThat(((GoConfigValidity.InvalidGoConfig) validity).errorMessage(), containsString("Invalid content was found starting with element 'unknown'"));
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
    public void shouldNotThrowExceptionWhenUpgradeFailsForConfigFileUpdate() throws Exception {
        expectLoadForEditing(configWith(createPipelineConfig("pipeline", "stage", "build")));
        GoConfigService.XmlPartialSaver saver = goConfigService.fileSaver(true);
        GoConfigValidity validity = saver.saveXml("some_junk", "junk_md5");
        assertThat(validity.isValid(), is(false));
        assertThat(((GoConfigValidity.InvalidGoConfig) validity).errorMessage(), is("Error on line 1: Content is not allowed in prolog."));
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
        assertThat(((GoConfigValidity.InvalidGoConfig) validity).errorMessage(),
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
    public void shouldRegisterListenerWithTheConfigDAO() {
        final ConfigChangedListener listener = mock(ConfigChangedListener.class);
        goConfigService.register(listener);
        verify(goConfigDao).registerListener(listener);
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
        } catch (RecordNotFoundException expected) {
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
            assertThat(e, instanceOf(RecordNotFoundException.class));
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
    public void shouldThrowRecordNotFoundExceptionWhenStageDoesNotExist() throws Exception {
        expectLoad(unchangedConfig());
        try {
            goConfigService.translateToActualCase(new JobConfigIdentifier("invalid-pipeline", STAGE, JOB));
            fail("should throw exception if pipeline does not exist");
        } catch (Exception e) {
            assertThat(e, instanceOf(RecordNotFoundException.class));
            assertThat(e.getMessage(), containsString("invalid-pipeline"));
        }
    }


    @Test
    public void shouldThrowIfCruiseHasNoReadPermissionOnArtifactsDir() throws Exception {
        if (SystemUtils.IS_OS_WINDOWS) {
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
            FileUtils.deleteQuietly(artifactsDir);
        }

    }

    @Test
    public void shouldThrowIfCruiseHasNoWritePermissionOnArtifactsDir() throws Exception {
        if (SystemUtils.IS_OS_WINDOWS) {
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
            FileUtils.deleteQuietly(artifactsDir);
        }

    }

    @Test
    public void shouldFindMaterialByPipelineUniqueFingerprint() throws Exception {
        SvnMaterialConfig svnMaterialConfig = svn("repo", null, null, false);
        svnMaterialConfig.setName(new CaseInsensitiveString("foo"));
        cruiseConfig = configWith(GoConfigMother.createPipelineConfigWithMaterialConfig(svnMaterialConfig));
        when(goConfigDao.load()).thenReturn(cruiseConfig);

        assertThat(goConfigService.findMaterial(new CaseInsensitiveString("pipeline"), svnMaterialConfig.getPipelineUniqueFingerprint()), is(svnMaterialConfig));
        assertThat(goConfigService.findMaterial(new CaseInsensitiveString("piPelIne"), svnMaterialConfig.getPipelineUniqueFingerprint()), is(svnMaterialConfig));
    }

    @Test
    public void shouldReturnNullIfNoMaterialMatches() throws Exception {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString("upstream-pipeline"), new CaseInsensitiveString("upstream-stage"));
        cruiseConfig = configWith(GoConfigMother.createPipelineConfigWithMaterialConfig(dependencyMaterialConfig));
        when(goConfigDao.load()).thenReturn(cruiseConfig);

        assertThat(goConfigService.findMaterial(new CaseInsensitiveString("pipeline"), "missing"), is(nullValue()));
    }

    @Test
    public void shouldFindMaterialConfigBasedOnFingerprint() throws Exception {
        SvnMaterialConfig expected = svn("repo", null, null, false);
        cruiseConfig = configWith(GoConfigMother.createPipelineConfigWithMaterialConfig(expected));
        when(goConfigDao.load()).thenReturn(cruiseConfig);

        MaterialConfig actual = goConfigService.materialForPipelineWithFingerprint("pipeline", expected.getFingerprint());
        assertThat(actual, is(expected));
    }

    @Test
    public void shouldThrowExceptionWhenUnableToFindMaterialBasedOnFingerprint() throws Exception {
        SvnMaterialConfig svnMaterialConfig = svn("repo", null, null, false);
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
    public void shouldRegisterBaseUrlChangeListener() throws Exception {
        CruiseConfig cruiseConfig = new GoConfigMother().cruiseConfigWithOnePipelineGroup();
        when(goConfigDao.load()).thenReturn(cruiseConfig);
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
        mother.enableSecurityWithPasswordFilePlugin(cruiseConfig);
        cruiseConfig.server().security().adminsConfig().add(new AdminUser(adminName));
        String groupName = String.format("group_%s", UUID.randomUUID());
        try {
            goConfigService.isUserAdminOfGroup(adminName, groupName);
            fail("Should fail since group does not exist");
        } catch (Exception e) {
            assertThat(e, is(instanceOf(RecordNotFoundException.class)));
        }
    }

    @Test
    public void shouldThrowExceptionIfGroupDoesNotExist_WhenUserIsNonAdmin() {
        CaseInsensitiveString adminName = new CaseInsensitiveString("admin");
        String groupName = String.format("group_%s", UUID.randomUUID());
        GoConfigMother mother = new GoConfigMother();
        mother.enableSecurityWithPasswordFilePlugin(cruiseConfig);
        cruiseConfig.server().security().adminsConfig().add(new AdminUser(adminName));
        try {
            goConfigService.isUserAdminOfGroup(new CaseInsensitiveString("foo"), groupName);
            fail("Should fail since group does not exist");
        } catch (Exception e) {
            assertThat(e, is(instanceOf(RecordNotFoundException.class)));
        }
    }

    @Test
    public void shouldReturnTrueIfUserIsTheAdminForGroup() {
        CaseInsensitiveString adminName = new CaseInsensitiveString("admin");
        String groupName = String.format("group_%s", UUID.randomUUID());
        GoConfigMother mother = new GoConfigMother();
        mother.enableSecurityWithPasswordFilePlugin(cruiseConfig);
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

        ArgumentCaptor<FullConfigUpdateCommand> commandArgCaptor = ArgumentCaptor.forClass(FullConfigUpdateCommand.class);
        verify(goConfigDao).updateFullConfig(commandArgCaptor.capture());

        FullConfigUpdateCommand command = commandArgCaptor.getValue();
        CruiseConfig updatedConfig = command.configForEdit();
        PipelineConfigs group = updatedConfig.findGroup(renamedGroupName);
        PipelineConfig pipeline = group.findBy(new CaseInsensitiveString("new_name"));
        assertThat(pipeline.name(), is(new CaseInsensitiveString("new_name")));
        assertThat(pipeline.getLabelTemplate(), is("${COUNT}-#{foo}"));
        assertThat(pipeline.materialConfigs().first(), is(instanceOf(SvnMaterialConfig.class)));
        assertThat(pipeline.materialConfigs().first().getUriForDisplay(), is("file:///tmp/foo"));
    }

    @Test
    public void shouldIgnoreXmlEntitiesAndReplaceThemWithEmptyString_DuringPipelineGroupPartialSave() throws Exception {
        ArgumentCaptor<FullConfigUpdateCommand> commandArgumentCaptor = ArgumentCaptor.forClass(FullConfigUpdateCommand.class);
        File targetFile = TempFiles.createUniqueFile("somefile");
        FileUtils.writeStringToFile(targetFile, "CONTENTS_OF_FILE", StandardCharsets.UTF_8);

        cruiseConfig = new BasicCruiseConfig();
        expectLoad(cruiseConfig);
        new GoConfigMother().addPipelineWithGroup(cruiseConfig, "group_name", "pipeline1", "stage_name", "job_name");
        expectLoadForEditing(cruiseConfig);
        when(goConfigDao.md5OfConfigFile()).thenReturn("md5");
        when(goConfigDao.updateFullConfig(commandArgumentCaptor.capture())).thenReturn(null);

        GoConfigService.XmlPartialSaver partialSaver = goConfigService.groupSaver("group_name");
        GoConfigValidity validity = partialSaver.saveXml(groupXmlWithEntity(targetFile.getAbsolutePath()), "md5");

        PipelineConfigs group = commandArgumentCaptor.getValue().configForEdit().findGroup("group_name");
        PipelineConfig pipeline = group.findBy(new CaseInsensitiveString("pipeline1"));
        assertThat(validity.isValid(), Matchers.is(true));

        String entityValue = pipeline.getParams().getParamNamed("foo").getValue();
        assertThat(entityValue, not(containsString("CONTENTS_OF_FILE")));
        assertThat(entityValue, isEmptyString());
    }

    @Test
    public void shouldUpdateXmlUsingNewFlowIfEnabled() throws Exception {
        String groupName = "group_name";
        String md5 = "md5";
        cruiseConfig = new BasicCruiseConfig();
        ArgumentCaptor<FullConfigUpdateCommand> commandArgumentCaptor = ArgumentCaptor.forClass(FullConfigUpdateCommand.class);

        expectLoad(cruiseConfig);
        new GoConfigMother().addPipelineWithGroup(cruiseConfig, groupName, "pipeline_name", "stage_name", "job_name");
        expectLoadForEditing(cruiseConfig);
        when(goConfigDao.md5OfConfigFile()).thenReturn(md5);
        when(goConfigDao.updateFullConfig(commandArgumentCaptor.capture())).thenReturn(null);

        GoConfigService.XmlPartialSaver partialSaver = goConfigService.groupSaver(groupName);
        String renamedGroupName = "renamed_group_name";

        GoConfigValidity validity = partialSaver.saveXml(groupXml(renamedGroupName), md5);

        assertThat(validity.isValid(), Matchers.is(true));
        CruiseConfig updatedConfig = commandArgumentCaptor.getValue().configForEdit();

        PipelineConfigs group = updatedConfig.findGroup(renamedGroupName);
        PipelineConfig pipeline = group.findBy(new CaseInsensitiveString("new_name"));
        assertThat(pipeline.name(), is(new CaseInsensitiveString("new_name")));
        assertThat(pipeline.getLabelTemplate(), is("${COUNT}-#{foo}"));
        assertThat(pipeline.materialConfigs().first(), is(instanceOf(SvnMaterialConfig.class)));
        assertThat(pipeline.materialConfigs().first().getUriForDisplay(), is("file:///tmp/foo"));
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
        assertThat(((GoConfigValidity.InvalidGoConfig) validity).errorMessage(), containsString("Invalid content was found starting with element 'unknown'"));
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
        assertThat(((GoConfigValidity.InvalidGoConfig) validity).errorMessage(), containsString("Name is invalid. \"pipeline@$^\""));
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
        assertThat(((GoConfigValidity.InvalidGoConfig) validity).errorMessage(), containsString("XML document structures must start and end within the same entity"));
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
        when(configRepo.configChangesFor("md5-5", "md5-4")).thenThrow(new IllegalArgumentException("something"));
        goConfigService.configChangesFor("md5-5", "md5-4", result);
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(SC_BAD_REQUEST));
        assertThat(result.message(), is("Historical configuration is not available for this stage run."));
    }

    @Test
    public void shouldUpdateResultAsCouldNotRetrieveConfigDiffWhenGenericExceptionOccurs() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        when(configRepo.configChangesFor("md5-5", "md5-4")).thenThrow(new RuntimeException("something"));
        goConfigService.configChangesFor("md5-5", "md5-4", result);
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(SC_INTERNAL_SERVER_ERROR));
        assertThat(result.message(), is("Could not retrieve config changes for this revision."));
    }

    @Test
    public void shouldReturnConfigStateFromDaoLayer_WhenUpdatingServerConfig() {
        ConfigSaveState expectedSaveState = ConfigSaveState.MERGED;
        when(goConfigDao.updateConfig(org.mockito.ArgumentMatchers.<UpdateConfigCommand>any())).thenReturn(expectedSaveState);
        ConfigSaveState configSaveState = goConfigService.updateServerConfig(new MailHost(new GoCipher()), "md5", null, null, null, null, "http://site",
                "https://site", "location");
        assertThat(configSaveState, is(expectedSaveState));
    }

    @Test
    public void shouldDelegateToConfig_getAllPipelinesInGroup() throws Exception {
        CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);
        when(goConfigDao.loadForEditing()).thenReturn(cruiseConfig);
        goConfigService.getAllPipelinesForEditInGroup("group");
        verify(cruiseConfig).pipelines("group");
    }


    @Test
    public void pipelineEditableViaUI_shouldReturnFalseWhenPipelineIsRemote() throws Exception {
        PipelineConfigs group = new BasicPipelineConfigs();
        PipelineConfig pipelineConfig = createPipelineConfig("pipeline", "name", "plan");
        pipelineConfig.setOrigin(new RepoConfigOrigin());
        group.add(pipelineConfig);
        expectLoad(new BasicCruiseConfig(group));
        assertThat(goConfigService.isPipelineEditable("pipeline"), is(false));
    }

    @Test
    public void pipelineEditableViaUI_shouldReturnTrueWhenPipelineIsLocal() throws Exception {
        PipelineConfigs group = new BasicPipelineConfigs();
        PipelineConfig pipelineConfig = createPipelineConfig("pipeline", "name", "plan");
        group.add(pipelineConfig);
        expectLoad(new BasicCruiseConfig(group));
        assertThat(goConfigService.isPipelineEditable("pipeline"), is(true));
    }

    @Test
    public void shouldTellIfAnUserIsAdministrator() throws Exception {
        final Username user = new Username(new CaseInsensitiveString("user"));
        expectLoad(mock(BasicCruiseConfig.class));
        goConfigService.isAdministrator(user.getUsername());
        verify(goConfigDao.load()).isAdministrator(user.getUsername().toString());
    }

    @Test
    public void shouldBeAbleToListAllDependencyMaterialConfigs() {
        BasicCruiseConfig config = mock(BasicCruiseConfig.class);
        DependencyMaterialConfig dependencyMaterialConfig = MaterialConfigsMother.dependencyMaterialConfig();
        SvnMaterialConfig svnMaterialConfig = MaterialConfigsMother.svnMaterialConfig();
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = MaterialConfigsMother.pluggableSCMMaterialConfig();
        HashSet<MaterialConfig> materialConfigs = new HashSet<>(Arrays.asList(dependencyMaterialConfig, svnMaterialConfig, pluggableSCMMaterialConfig));

        when(goConfigService.getCurrentConfig()).thenReturn(config);
        when(config.getAllUniqueMaterialsBelongingToAutoPipelinesAndConfigRepos()).thenReturn(materialConfigs);

        Set<DependencyMaterialConfig> schedulableDependencyMaterials = goConfigService.getSchedulableDependencyMaterials();

        assertThat(schedulableDependencyMaterials.size(), is(1));
        assertTrue(schedulableDependencyMaterials.contains(dependencyMaterialConfig));
    }

    @Test
    public void shouldBeAbleToListAllSCMMaterialConfigs() {
        BasicCruiseConfig config = mock(BasicCruiseConfig.class);
        DependencyMaterialConfig dependencyMaterialConfig = MaterialConfigsMother.dependencyMaterialConfig();
        SvnMaterialConfig svnMaterialConfig = MaterialConfigsMother.svnMaterialConfig();
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = MaterialConfigsMother.pluggableSCMMaterialConfig();
        HashSet<MaterialConfig> materialConfigs = new HashSet<>(Arrays.asList(dependencyMaterialConfig, svnMaterialConfig, pluggableSCMMaterialConfig));

        when(goConfigService.getCurrentConfig()).thenReturn(config);
        when(config.getAllUniqueMaterialsBelongingToAutoPipelinesAndConfigRepos()).thenReturn(materialConfigs);

        Set<MaterialConfig> schedulableDependencyMaterials = goConfigService.getSchedulableSCMMaterials();

        assertThat(schedulableDependencyMaterials.size(), is(2));
        assertTrue(schedulableDependencyMaterials.contains(svnMaterialConfig));
        assertTrue(schedulableDependencyMaterials.contains(pluggableSCMMaterialConfig));
    }

    @Test
    public void shouldBeAbleToEditAnExistentLocalPipelineWithAdminPrivileges() throws Exception {
        CruiseConfig cruiseConfig = mock(CruiseConfig.class);
        PipelineConfig pipeline = new PipelineConfig();
        pipeline.setName("pipeline1");
        pipeline.setOrigin(null);

        when(goConfigDao.load()).thenReturn(cruiseConfig);
        when(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1"))).thenReturn(pipeline);
        when(cruiseConfig.getGroups()).thenReturn(new GoConfigMother().cruiseConfigWithOnePipelineGroup().getGroups());
        when(cruiseConfig.isAdministrator("admin_user")).thenReturn(true);

        assertTrue(goConfigService.canEditPipeline("pipeline1", new Username("admin_user")));
    }

    @Test
    public void shouldNotBeAbleToEditANonExistentPipeline() throws Exception {
        CruiseConfig cruiseConfig = mock(CruiseConfig.class);

        when(goConfigDao.load()).thenReturn(cruiseConfig);
        when(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("non_existing_pipeline"))).thenThrow(new RecordNotFoundException(EntityType.Pipeline, "non_existing_pipeline"));

        assertFalse(goConfigService.canEditPipeline("non_existing_pipeline", null));
    }

    @Test
    public void shouldNotBeAbleToEditPipelineIfUserDoesNotHaveSufficientPermissions() throws Exception {
        CruiseConfig cruiseConfig = mock(CruiseConfig.class);
        PipelineConfig pipeline = new PipelineConfig();
        pipeline.setName("pipeline1");

        when(goConfigDao.load()).thenReturn(cruiseConfig);
        when(cruiseConfig.isSecurityEnabled()).thenReturn(true);
        when(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1"))).thenReturn(pipeline);
        BasicCruiseConfig basicCruiseConfig = new GoConfigMother().cruiseConfigWithOnePipelineGroup();
        when(cruiseConfig.getGroups()).thenReturn(basicCruiseConfig.getGroups());
        when(cruiseConfig.findGroup("group1")).thenReturn(mock(PipelineConfigs.class));
        when(cruiseConfig.isAdministrator("view_user")).thenReturn(false);
        when(cruiseConfig.server()).thenReturn(new ServerConfig());

        assertFalse(goConfigService.canEditPipeline("pipeline1", new Username("view_user")));
    }

    @Test
    public void shouldNotAllowEditOfConfigRepoPipelines() throws Exception {
        CruiseConfig cruiseConfig = mock(CruiseConfig.class);

        when(goConfigDao.load()).thenReturn(cruiseConfig);
        PipelineConfig pipeline = new PipelineConfig();
        pipeline.setName("pipeline1");
        pipeline.setOrigin(new RepoConfigOrigin());
        when(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1"))).thenReturn(pipeline);
        when(cruiseConfig.getGroups()).thenReturn(new GoConfigMother().cruiseConfigWithOnePipelineGroup().getGroups());
        when(cruiseConfig.isAdministrator("admin_user")).thenReturn(true);

        assertFalse(goConfigService.canEditPipeline("pipeline1", new Username("admin_user")));
    }

    @Test
    public void shouldIncludeAllLocalPipelinesWithSpecificFingerprint() throws Exception {
        cruiseConfig = new BasicCruiseConfig();
        expectLoad(cruiseConfig);
        PipelineConfig pipelineConfig = new GoConfigMother().addPipelineWithGroup(cruiseConfig, "group", "pipeline_name", "stage_name", "job_name");

        GitMaterialConfig gitMaterialConfig = git("https://foo");
        MaterialConfigs materialConfigs = new MaterialConfigs(gitMaterialConfig);
        pipelineConfig.setMaterialConfigs(materialConfigs);

        List<CaseInsensitiveString> pipelineNames = goConfigService.pipelinesWithMaterial(gitMaterialConfig.getFingerprint());

        assertThat(pipelineNames, contains(new CaseInsensitiveString("pipeline_name")));
    }

    @Test
    public void shouldIncludeAllRemotePipelinesWithSpecificFingerprint() throws Exception {
        cruiseConfig = new BasicCruiseConfig();
        expectLoad(cruiseConfig);
        PipelineConfig pipelineConfig = new GoConfigMother().addPipelineWithGroup(cruiseConfig, "group", "pipeline_name", "stage_name", "job_name");

        GitMaterialConfig gitMaterialConfig = git("https://foo");
        MaterialConfigs materialConfigs = new MaterialConfigs(gitMaterialConfig);
        pipelineConfig.setMaterialConfigs(materialConfigs);
        pipelineConfig.setOrigin(new RepoConfigOrigin());

        List<CaseInsensitiveString> pipelineNames = goConfigService.pipelinesWithMaterial(gitMaterialConfig.getFingerprint());

        assertThat(pipelineNames, contains(new CaseInsensitiveString("pipeline_name")));
    }

    @Test
    public void shouldReturnEmptyWhenThereAreNoPipelinesWithGivenFingerprint() {
        List<CaseInsensitiveString> pipelineNames = goConfigService.pipelinesWithMaterial("fingerprint");

        assertThat(pipelineNames.isEmpty(), is(true));
    }

    @Test
    public void shouldFindGroupByPipelineName() throws Exception {
        GoConfigMother configMother = new GoConfigMother();
        BasicCruiseConfig config = GoConfigMother.defaultCruiseConfig();
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        configMother.addPipelineWithGroup(config, "group1", "pipeline2", "stage1", "job1");
        configMother.addPipelineWithGroup(config, "group2", "pipeline3", "stage1", "job1");

        expectLoad(config);
        assertThat(goConfigService.findGroupByPipeline(new CaseInsensitiveString("pipeline1")).getGroup(), is("group1"));
        assertThat(goConfigService.findGroupByPipeline(new CaseInsensitiveString("pipeline2")).getGroup(), is("group1"));
        assertThat(goConfigService.findGroupByPipeline(new CaseInsensitiveString("pipeline3")).getGroup(), is("group2"));
    }

    @Test
    public void shouldFindPipelineByPipelineName() throws Exception {
        GoConfigMother configMother = new GoConfigMother();
        BasicCruiseConfig config = GoConfigMother.defaultCruiseConfig();
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        configMother.addPipelineWithGroup(config, "group1", "pipeline2", "stage1", "job1");
        configMother.addPipelineWithGroup(config, "group2", "pipeline3", "stage1", "job1");

        expectLoad(config);
        assertThat(goConfigService.findPipelineByName(new CaseInsensitiveString("pipeline1")).name().toString(), is("pipeline1"));
        assertThat(goConfigService.findPipelineByName(new CaseInsensitiveString("pipeline2")).name().toString(), is("pipeline2"));
        assertThat(goConfigService.findPipelineByName(new CaseInsensitiveString("pipeline3")).name().toString(), is("pipeline3"));
    }

    @Test
    public void shouldReturnNullIfNoPipelineExistByPipelineName() throws Exception {
        GoConfigMother configMother = new GoConfigMother();
        BasicCruiseConfig config = GoConfigMother.defaultCruiseConfig();
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        configMother.addPipelineWithGroup(config, "group1", "pipeline2", "stage1", "job1");
        configMother.addPipelineWithGroup(config, "group2", "pipeline3", "stage1", "job1");

        expectLoad(config);
        assertThat(goConfigService.findPipelineByName(new CaseInsensitiveString("invalid")), is(nullValue()));
    }

    @Test
    public void shouldReturnSecretConfigBySecretConfigId() throws Exception {
        Rules rules = new Rules(new Allow("refer", "pipeline_group", "default"));
        SecretConfig secretConfig = new SecretConfig("secret_config_id", "plugin_id", rules);
        GoConfigMother configMother = new GoConfigMother();
        CruiseConfig config = GoConfigMother.configWithSecretConfig(secretConfig);
        configMother.addPipelineWithGroup(config, "default", "pipeline1", "stage1", "job1");

        expectLoad(config);
        assertThat(goConfigService.getSecretConfigById("secret_config_id"), is(secretConfig));
    }

    @Test
    public void shouldReturnNullIfNoSecretConfigExistBySecretConfigId() throws Exception {
        GoConfigMother configMother = new GoConfigMother();
        BasicCruiseConfig config = GoConfigMother.defaultCruiseConfig();
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");

        expectLoad(config);
        assertThat(goConfigService.getSecretConfigById("invalid"), is(nullValue()));
    }

    @Test
    public void shouldReturnAllPipelinesForASuperAdmin() throws Exception {
        GoConfigMother configMother = new GoConfigMother();
        BasicCruiseConfig config = GoConfigMother.defaultCruiseConfig();
        GoConfigMother.enableSecurityWithPasswordFilePlugin(config);
        GoConfigMother.addUserAsSuperAdmin(config, "superadmin");
        PipelineConfig pipelineConfig = configMother.addPipelineWithGroup(config, "group1", "p1", "s1", "j1");
        when(goConfigDao.loadForEditing()).thenReturn(config);
        expectLoad(config);

        List<PipelineConfig> pipelines = goConfigService.getAllPipelineConfigsForEditForUser(new Username("superadmin"));

        assertThat(pipelines, contains(pipelineConfig));
    }

    @Test
    public void shouldReturnSpecificPipelinesForAGroupAdmin() throws Exception {
        GoConfigMother configMother = new GoConfigMother();
        BasicCruiseConfig config = GoConfigMother.defaultCruiseConfig();
        GoConfigMother.enableSecurityWithPasswordFilePlugin(config);
        GoConfigMother.addUserAsSuperAdmin(config, "superadmin");

        PipelineConfig pipelineConfig1 = configMother.addPipelineWithGroup(config, "group1", "p1", "s1", "j1");
        PipelineConfig pipelineConfig2 = configMother.addPipelineWithGroup(config, "group2", "p2", "s1", "j1");

        configMother.addAdminUserForPipelineGroup(config, "groupAdmin", "group1");

        when(goConfigDao.loadForEditing()).thenReturn(config);
        expectLoad(config);

        List<PipelineConfig> pipelines = goConfigService.getAllPipelineConfigsForEditForUser(new Username("groupAdmin"));

        assertThat(pipelines, contains(pipelineConfig1));
        assertThat(pipelines, not(contains(pipelineConfig2)));
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

    private GoConfigService goConfigServiceWithInvalidStatus() {
        goConfigDao = mock(GoConfigDao.class, "badCruiseConfigManager");
        when(goConfigDao.checkConfigFileValid()).thenReturn(GoConfigValidity.invalid("JDom exception"));
        return new GoConfigService(goConfigDao, new SystemTimeClock(), mock(GoConfigMigration.class), goCache, null,
                ConfigElementImplementationRegistryMother.withNoPlugins(), instanceFactory, null, null);
    }

    private GoConfigInvalidException getGoConfigInvalidException() {
        ConfigErrors configErrors = new ConfigErrors();
        configErrors.add("command", "command cannot be empty");
        AllConfigErrors list = new AllConfigErrors();
        list.add(configErrors);
        return new GoConfigInvalidException(new BasicCruiseConfig(), list.asString());
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

    private String groupXmlWithEntity(String filePathToReferToInEntity) {
        return "<!DOCTYPE foo [  \n" +
                "<!ELEMENT param ANY >\n" +
                "<!ENTITY myentity SYSTEM \"file://" + filePathToReferToInEntity + "\" >]>" +
                "<pipelines group=\"group_name\">\n"
                + "  <pipeline name=\"pipeline1\" labeltemplate=\"${COUNT}-#{foo}\">\n"
                + "     <params>\n"
                + "      <param name=\"foo\">&myentity;</param>\n"
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
