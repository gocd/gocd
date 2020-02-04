/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import ch.qos.logback.classic.Level;
import com.rits.cloning.Cloner;
import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.config.exceptions.ConfigFileHasChangedException;
import com.thoughtworks.go.config.exceptions.ConfigMergeException;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;
import com.thoughtworks.go.config.parts.XmlPartialConfigProvider;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.config.rules.Allow;
import com.thoughtworks.go.domain.GoConfigRevision;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import static com.thoughtworks.go.helper.ConfigFileFixture.DEFAULT_XML_WITH_2_AGENTS;
import static com.thoughtworks.go.helper.ConfigFileFixture.VALID_XML_3169;
import static com.thoughtworks.go.util.LogFixture.logFixtureFor;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class GoFileConfigDataSourceIntegrationTest {

    private final String DEFAULT_CHARSET = "defaultCharset";
    private final SystemEnvironment systemEnvironment = new SystemEnvironment();
    @Autowired
    private GoFileConfigDataSource dataSource;
    @Autowired
    private GoPartialConfig goPartialConfig;
    @Autowired
    private GoConfigDao goConfigDao;
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    private GoConfigFileHelper configHelper;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private GoConfigWatchList configWatchList;
    private ConfigRepoConfig configRepo;
    @Autowired
    private CachedGoPartials cachedGoPartials;
    @Autowired
    private ConfigCache configCache;
    @Autowired
    private ConfigRepository configRepository;
    @Autowired
    private ConfigElementImplementationRegistry configElementImplementationRegistry;
    private final String remoteDownstream = "remote_downstream";
    private PartialConfig partialConfig;
    private PipelineConfig upstreamPipeline;
    private ConfigRepoConfig repoConfig;

    @Before
    public void setUp() throws Exception {
        File configDir = temporaryFolder.newFolder();
        String absolutePath = new File(configDir, "cruise-config.xml").getAbsolutePath();
        systemEnvironment.setProperty(SystemEnvironment.CONFIG_FILE_PROPERTY, absolutePath);
        configHelper = new GoConfigFileHelper(DEFAULT_XML_WITH_2_AGENTS);
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();
        ConfigRepoConfig config = ConfigRepoConfig.createConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig("url"), XmlPartialConfigProvider.providerName, "git-id");
        config.getRules().add(new Allow("refer", "*", "*"));
        repoConfig = config;
        configHelper.addConfigRepo(repoConfig);
        configHelper.addPipeline("upstream", "upstream_stage_original");
        goConfigService.forceNotifyListeners();
        cachedGoPartials.clear();
        configRepo = configWatchList.getCurrentConfigRepos().get(0);
        upstreamPipeline = goConfigService.pipelineConfigNamed(new CaseInsensitiveString("upstream"));
        partialConfig = PartialConfigMother.pipelineWithDependencyMaterial(remoteDownstream, upstreamPipeline, new RepoConfigOrigin(configRepo, "r1"));
        goPartialConfig.onSuccessPartialConfig(configRepo, partialConfig);
        systemEnvironment.set(SystemEnvironment.ENABLE_CONFIG_MERGE_FEATURE, true);
    }

    @After
    public void tearDown() throws Exception {
        cachedGoPartials.clear();
        dataSource.reloadIfModified();
        configHelper.onTearDown();
        ReflectionUtil.setStaticField(Charset.class, DEFAULT_CHARSET, null);
        systemEnvironment.clearProperty(SystemEnvironment.CONFIG_FILE_PROPERTY);
        systemEnvironment.set(SystemEnvironment.ENABLE_CONFIG_MERGE_FEATURE, true);
    }

    @Test
    public void shouldConvertToUTF8BeforeSavingConfigToFileSystem() throws IOException {
        ReflectionUtil.setStaticField(Charset.class, DEFAULT_CHARSET, Charset.forName("windows-1252"));
        GoFileConfigDataSource.GoConfigSaveResult result = dataSource.writeWithLock(new UpdateConfigCommand() {
            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig(UUID.randomUUID().toString());
                JobConfig job = new JobConfig("job");
                ExecTask task = new ExecTask();
                task.setCommand("powershell");
                task.setArgs("Get-ChildItem -Path . â€“Recurse");
                job.addTask(task);
                pipelineConfig.first().getJobs().add(job);
                cruiseConfig.addPipeline(UUID.randomUUID().toString(), pipelineConfig);
                return cruiseConfig;
            }
        }, new GoConfigHolder(goConfigService.currentCruiseConfig(), goConfigService.getConfigForEditing()));
        assertThat(result.getConfigSaveState(), is(ConfigSaveState.UPDATED));
        FileInputStream inputStream = new FileInputStream(dataSource.fileLocation());
        String newMd5 = CachedDigestUtils.md5Hex(inputStream);
        assertThat(newMd5, is(result.getConfigHolder().config.getMd5()));
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldValidateMergedConfigForConfigChangesThroughFileSystem() throws Exception {
        assertThat(goConfigService.getCurrentConfig().getAllPipelineNames().contains(new CaseInsensitiveString(remoteDownstream)), is(true));
        updateConfigOnFileSystem(new UpdateConfig() {
            @Override
            public void update(CruiseConfig cruiseConfig) {
                PipelineConfig updatedUpstream = cruiseConfig.getPipelineConfigByName(upstreamPipeline.name());
                updatedUpstream.getFirstStageConfig().setName(new CaseInsensitiveString("upstream_stage_renamed"));
            }
        });

        thrown.expect(org.hamcrest.Matchers.any(GoConfigInvalidException.class));
        thrown.expectMessage("Stage with name 'upstream_stage_original' does not exist on pipeline 'upstream', it is being referred to from pipeline 'remote_downstream' (url at r1)");
        dataSource.forceLoad(new File(systemEnvironment.getCruiseConfigFile()));
    }

    @Test
    public void shouldFallbackToValidPartialsForConfigChangesThroughFileSystem() throws Exception {
        assertThat(goConfigService.getCurrentConfig().getAllPipelineNames().contains(new CaseInsensitiveString(remoteDownstream)), is(true));

        String remoteInvalidPipeline = "remote_invalid_pipeline";
        PartialConfig invalidPartial = PartialConfigMother.invalidPartial(remoteInvalidPipeline, new RepoConfigOrigin(configRepo, "r2"));
        goPartialConfig.onSuccessPartialConfig(configRepo, invalidPartial);
        assertThat(goConfigService.getCurrentConfig().getAllPipelineNames().contains(new CaseInsensitiveString(remoteInvalidPipeline)), is(false));

        final String newArtifactLocation = "some_random_change_to_config";
        updateConfigOnFileSystem(new UpdateConfig() {
            @Override
            public void update(CruiseConfig cruiseConfig) {
                cruiseConfig.server().setArtifactsDir(newArtifactLocation);
            }
        });

        GoConfigHolder goConfigHolder = dataSource.forceLoad(new File(systemEnvironment.getCruiseConfigFile()));
        assertThat(goConfigHolder.config.server().artifactsDir(), is(newArtifactLocation));
        assertThat(goConfigHolder.config.getAllPipelineNames().contains(new CaseInsensitiveString(remoteDownstream)), is(true));
        assertThat(goConfigHolder.config.getAllPipelineNames().contains(new CaseInsensitiveString(remoteInvalidPipeline)), is(false));
    }

    @Test
    public void shouldSaveWithKnownPartialsWhenValidationPassesForConfigChangesThroughFileSystem() throws Exception {
        assertThat(goConfigService.getCurrentConfig().getAllPipelineNames().contains(new CaseInsensitiveString(remoteDownstream)), is(true));

        //Introducing a change to make the latest version of remote pipeline invalid
        PipelineConfig remoteDownstreamPipeline = partialConfig.getGroups().first().getPipelines().get(0);
        DependencyMaterialConfig dependencyMaterial = remoteDownstreamPipeline.materialConfigs().findDependencyMaterial(upstreamPipeline.name());
        dependencyMaterial.setStageName(new CaseInsensitiveString("upstream_stage_renamed"));
        goPartialConfig.onSuccessPartialConfig(configRepo, partialConfig);
        DependencyMaterialConfig dependencyMaterialForRemotePipelineInConfigCache = goConfigService.getCurrentConfig().getPipelineConfigByName(new CaseInsensitiveString(remoteDownstream)).materialConfigs().findDependencyMaterial(upstreamPipeline.name());
        assertThat(dependencyMaterialForRemotePipelineInConfigCache.getStageName(), is(new CaseInsensitiveString("upstream_stage_original")));

        final CaseInsensitiveString upstreamStageRenamed = new CaseInsensitiveString("upstream_stage_renamed");
        updateConfigOnFileSystem(new UpdateConfig() {
            @Override
            public void update(CruiseConfig cruiseConfig) {
                cruiseConfig.getPipelineConfigByName(upstreamPipeline.name()).first().setName(upstreamStageRenamed);
            }
        });

        GoConfigHolder goConfigHolder = dataSource.forceLoad(new File(systemEnvironment.getCruiseConfigFile()));
        assertThat(goConfigHolder.config.getAllPipelineNames().contains(new CaseInsensitiveString(remoteDownstream)), is(true));
        assertThat(goConfigHolder.config.getPipelineConfigByName(remoteDownstreamPipeline.name()).materialConfigs().findDependencyMaterial(upstreamPipeline.name()).getStageName(), is(upstreamStageRenamed));
        assertThat(goConfigHolder.config.getPipelineConfigByName(upstreamPipeline.name()).getFirstStageConfig().name(), is(upstreamStageRenamed));
    }

    @Test
    public void shouldNotSaveConfigIfValidationOfLastKnownValidPartialsMergedWithMainConfigFails() {
        final PipelineConfig upstream = PipelineConfigMother.createPipelineConfig(UUID.randomUUID().toString(), "s1", "j1");
        configHelper.addPipeline(upstream);
        String remotePipeline = "remote_pipeline";
        RepoConfigOrigin repoConfigOrigin = new RepoConfigOrigin(this.repoConfig, "1");
        PartialConfig partialConfig = PartialConfigMother.pipelineWithDependencyMaterial(remotePipeline, upstream, repoConfigOrigin);
        cachedGoPartials.addOrUpdate(this.repoConfig.getRepo().getFingerprint(), partialConfig);
        cachedGoPartials.markAllKnownAsValid();
        thrown.expect(RuntimeException.class);
        thrown.expectCause(Matchers.any(GoConfigInvalidException.class));
        thrown.expectMessage(String.format("Stage with name 's1' does not exist on pipeline '%s', it is being referred to from pipeline '%s' (%s)", upstream.name(), remotePipeline, repoConfigOrigin.displayName()));
        dataSource.writeWithLock(cruiseConfig -> {
            PipelineConfig pipelineConfig = cruiseConfig.getPipelineConfigByName(upstream.name());
            pipelineConfig.clear();
            pipelineConfig.add(new StageConfig(new CaseInsensitiveString("new_stage"), new JobConfigs(new JobConfig("job"))));
            return cruiseConfig;
        }, new GoConfigHolder(configHelper.currentConfig(), configHelper.currentConfig()));
    }

    @Test
    public void shouldUse_UserFromSession_asConfigModifyingUserWhenNoneGiven() throws GitAPIException, IOException {
        com.thoughtworks.go.server.newsecurity.SessionUtilsHelper.loginAs("loser_boozer");
        goConfigDao.updateMailHost(getMailHost("mailhost.local"));
        CruiseConfig cruiseConfig = goConfigDao.load();
        GoConfigRevision revision = configRepository.getRevision(cruiseConfig.getMd5());
        assertThat(revision.getUsername(), is("loser_boozer"));
    }

    @Test
    public void shouldSaveTheCruiseConfigXml() throws Exception {
        File file = dataSource.fileLocation();

        dataSource.write(ConfigMigrator.migrate(VALID_XML_3169), false);

        assertThat(FileUtils.readFileToString(file, UTF_8), containsString("http://hg-server/hg/connectfour"));
    }

    @Test
    public void shouldVersionTheCruiseConfigXmlWhenSaved() throws Exception {
        CachedGoConfig cachedGoConfig = configHelper.getCachedGoConfig();
        CruiseConfig configForEdit = cachedGoConfig.loadForEditing();
        GoConfigHolder configHolder = new GoConfigHolder(cachedGoConfig.currentConfig(), configForEdit);

        GoConfigHolder afterFirstSave = dataSource.writeWithLock(new UserAwarePipelineAddingCommand("foo-pipeline", "loser"), configHolder).getConfigHolder();

        GoConfigHolder afterSecondSave = dataSource.writeWithLock(new UserAwarePipelineAddingCommand("bar-pipeline", "bigger_loser"), afterFirstSave).getConfigHolder();

        String expectedMd5 = afterFirstSave.config.getMd5();
        GoConfigRevision firstRev = configRepository.getRevision(expectedMd5);
        assertThat(firstRev.getUsername(), is("loser"));
        assertThat(firstRev.getGoVersion(), is(CurrentGoCDVersion.getInstance().formatted()));
        assertThat(firstRev.getMd5(), is(expectedMd5));
        assertThat(firstRev.getSchemaVersion(), is(GoConstants.CONFIG_SCHEMA_VERSION));
        assertThat(ConfigMigrator.load(firstRev.getContent()), is(afterFirstSave.configForEdit));

        CruiseConfig config = afterSecondSave.config;
        assertThat(config.hasPipelineNamed(new CaseInsensitiveString("bar-pipeline")), is(true));
        expectedMd5 = config.getMd5();
        GoConfigRevision secondRev = configRepository.getRevision(expectedMd5);
        assertThat(secondRev.getUsername(), is("bigger_loser"));
        assertThat(secondRev.getGoVersion(), is(CurrentGoCDVersion.getInstance().formatted()));
        assertThat(secondRev.getMd5(), is(expectedMd5));
        assertTrue(secondRev.getTime().after(firstRev.getTime()));
        assertThat(secondRev.getSchemaVersion(), is(GoConstants.CONFIG_SCHEMA_VERSION));
        assertThat(ConfigMigrator.load(secondRev.getContent()), is(afterSecondSave.configForEdit));
    }

    @Test
    public void shouldLoadAsUser_Filesystem_WithMd5Sum() throws Exception {
        GoConfigHolder configHolder = goConfigDao.loadConfigHolder();
        String md5 = DigestUtils.md5Hex(FileUtils.readFileToString(dataSource.fileLocation(), UTF_8));
        assertThat(configHolder.configForEdit.getMd5(), is(md5));
        assertThat(configHolder.config.getMd5(), is(md5));

        CruiseConfig forEdit = configHolder.configForEdit;
        forEdit.addPipeline("my-awesome-group", PipelineConfigMother.createPipelineConfig("pipeline-foo", "stage-bar", "job-baz"));
        FileOutputStream fos = new FileOutputStream(dataSource.fileLocation());
        new MagicalGoConfigXmlWriter(configCache, ConfigElementImplementationRegistryMother.withNoPlugins()).write(forEdit, fos, false);

        configHolder = dataSource.load();
        String xmlText = FileUtils.readFileToString(dataSource.fileLocation(), UTF_8);
        String secondMd5 = DigestUtils.md5Hex(xmlText);
        assertThat(configHolder.configForEdit.getMd5(), is(secondMd5));
        assertThat(configHolder.config.getMd5(), is(secondMd5));
        assertThat(configHolder.configForEdit.getMd5(), is(not(md5)));
        GoConfigRevision commitedVersion = configRepository.getRevision(secondMd5);
        assertThat(commitedVersion.getContent(), is(xmlText));
        assertThat(commitedVersion.getUsername(), is(GoFileConfigDataSource.FILESYSTEM));
    }

    @Test
    public void shouldNotCorruptTheCruiseConfigXml() throws Exception {
        File file = dataSource.fileLocation();
        String originalCopy = FileUtils.readFileToString(file, UTF_8);

        try {
            dataSource.write("abc", false);
            fail("Should not allow us to write an invalid config");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Content is not allowed in prolog"));
        }

        assertThat(readFileToString(file, UTF_8), is(originalCopy));
    }

    @Test
    public void shouldEncryptSvnPasswordWhenConfigIsChangedViaFileSystem() throws Exception {
        String configContent = ConfigFileFixture.configWithPipeline(String.format(
                "<pipeline name='pipeline1'>"
                        + "    <materials>"
                        + "      <svn url='svnurl' username='admin' password='%s'/>"
                        + "    </materials>"
                        + "  <stage name='mingle'>"
                        + "    <jobs>"
                        + "      <job name='do-something'>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>", "hello"), GoConstants.CONFIG_SCHEMA_VERSION);
        FileUtils.writeStringToFile(dataSource.fileLocation(), configContent, UTF_8);

        GoConfigHolder configHolder = dataSource.load();

        PipelineConfig pipelineConfig = configHolder.config.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));
        SvnMaterialConfig svnMaterialConfig = (SvnMaterialConfig) pipelineConfig.materialConfigs().get(0);
        assertThat(svnMaterialConfig.getEncryptedPassword(), is(not(nullValue())));
    }

    @Test
    public void shouldEncryptTfsPasswordWhenConfigIsChangedViaFileSystem() throws Exception {
        String configContent = ConfigFileFixture.configWithPipeline(String.format(
                "<pipeline name='pipeline1'>"
                        + "    <materials>"
                        + "      <tfs url='http://some.repo.local' username='username@domain' password='password' projectPath='$/project_path' />"
                        + "    </materials>"
                        + "  <stage name='mingle'>"
                        + "    <jobs>"
                        + "      <job name='do-something'>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>", "hello"), GoConstants.CONFIG_SCHEMA_VERSION);
        FileUtils.writeStringToFile(dataSource.fileLocation(), configContent, UTF_8);

        GoConfigHolder configHolder = dataSource.load();

        PipelineConfig pipelineConfig = configHolder.config.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));
        TfsMaterialConfig tfsMaterial = (TfsMaterialConfig) pipelineConfig.materialConfigs().get(0);
        assertThat(tfsMaterial.getEncryptedPassword(), is(not(nullValue())));
    }

    @Test
    public void shouldUpdateFileAttributesIfFileContentsHaveNotChanged() throws Exception {//so that it doesn't have to do the file content checksum computation next time
        dataSource.reloadIfModified();
        assertThat(dataSource.load(), not(nullValue()));

        GoFileConfigDataSource.ReloadIfModified reloadStrategy = (GoFileConfigDataSource.ReloadIfModified) ReflectionUtil.getField(dataSource, "reloadStrategy");

        ReflectionUtil.setField(reloadStrategy, "lastModified", -1);
        ReflectionUtil.setField(reloadStrategy, "prevSize", -1);

        assertThat(dataSource.load(), is(nullValue()));

        assertThat(ReflectionUtil.getField(reloadStrategy, "lastModified"), is(dataSource.fileLocation().lastModified()));
        assertThat(ReflectionUtil.getField(reloadStrategy, "prevSize"), is(dataSource.fileLocation().length()));
    }

    @Test
    public void shouldGetMergedConfig() throws Exception {
        configHelper.addMailHost(getMailHost("mailhost.local.old"));
        GoConfigHolder goConfigHolder = dataSource.forceLoad(dataSource.fileLocation());
        CruiseConfig oldConfigForEdit = goConfigHolder.configForEdit;
        final String oldMD5 = oldConfigForEdit.getMd5();
        MailHost oldMailHost = oldConfigForEdit.server().mailHost();

        assertThat(oldMailHost.getHostName(), is("mailhost.local.old"));
        assertThat(oldMailHost.getHostName(), is(not("mailhost.local")));

        goConfigDao.updateMailHost(getMailHost("mailhost.local"));

        goConfigHolder = dataSource.forceLoad(dataSource.fileLocation());

        GoFileConfigDataSource.GoConfigSaveResult result = dataSource.writeWithLock(new NoOverwriteUpdateConfigCommand() {
            @Override
            public String unmodifiedMd5() {
                return oldMD5;
            }

            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                cruiseConfig.addPipeline("g", PipelineConfigMother.pipelineConfig("p1", StageConfigMother.custom("s", "b")));
                return cruiseConfig;
            }
        }, goConfigHolder);

        assertThat(result.getConfigHolder().config.server().mailHost().getHostName(), is("mailhost.local"));
        assertThat(result.getConfigHolder().config.hasPipelineNamed(new CaseInsensitiveString("p1")), is(true));
    }

    @Test
    public void shouldPropagateConfigHasChangedException() throws Exception {
        String originalMd5 = dataSource.forceLoad(dataSource.fileLocation()).configForEdit.getMd5();
        goConfigDao.updateConfig(configHelper.addPipelineCommand(originalMd5, "p1", "s1", "b1"));
        GoConfigHolder goConfigHolder = dataSource.forceLoad(dataSource.fileLocation());

        try {
            dataSource.writeWithLock(configHelper.addPipelineCommand(originalMd5, "p2", "s", "b"), goConfigHolder);
            fail("Should throw ConfigFileHasChanged exception");
        } catch (Exception e) {
            assertThat(e.getCause().getClass().getName(), e.getCause() instanceof ConfigMergeException, is(true));
        }
    }

    @Test
    public void shouldThrowConfigMergeExceptionWhenConfigMergeFeatureIsTurnedOff() throws Exception {
        String firstMd5 = dataSource.forceLoad(dataSource.fileLocation()).configForEdit.getMd5();
        goConfigDao.updateConfig(configHelper.addPipelineCommand(firstMd5, "p0", "s0", "b0"));
        String originalMd5 = dataSource.forceLoad(dataSource.fileLocation()).configForEdit.getMd5();
        goConfigDao.updateConfig(configHelper.addPipelineCommand(originalMd5, "p1", "s1", "j1"));
        GoConfigHolder goConfigHolder = dataSource.forceLoad(dataSource.fileLocation());

        systemEnvironment.set(SystemEnvironment.ENABLE_CONFIG_MERGE_FEATURE, Boolean.FALSE);

        try {
            dataSource.writeWithLock(configHelper.changeJobNameCommand(originalMd5, "p0", "s0", "b0", "j0"), goConfigHolder);
            fail("Should throw ConfigMergeException");
        } catch (RuntimeException e) {
            ConfigMergeException cme = (ConfigMergeException) e.getCause();
            assertThat(cme.getMessage(), is(ConfigFileHasChangedException.CONFIG_CHANGED_PLEASE_REFRESH));
        }
    }

    @Test
    public void shouldGetConfigMergedStateWhenAMergerOccurs() throws Exception {
        System.out.println("systemEnvironment.get(SystemEnvironment.ENABLE_CONFIG_MERGE_FEATURE) = " + systemEnvironment.get(SystemEnvironment.ENABLE_CONFIG_MERGE_FEATURE));
        configHelper.addMailHost(getMailHost("mailhost.local.old"));
        String originalMd5 = dataSource.forceLoad(dataSource.fileLocation()).configForEdit.getMd5();
        configHelper.addMailHost(getMailHost("mailhost.local"));
        GoConfigHolder goConfigHolder = dataSource.forceLoad(dataSource.fileLocation());

        GoFileConfigDataSource.GoConfigSaveResult goConfigSaveResult = dataSource.writeWithLock(configHelper.addPipelineCommand(originalMd5, "p1", "s", "b"), goConfigHolder);
        assertThat(goConfigSaveResult.getConfigSaveState(), is(ConfigSaveState.MERGED));
    }

    @Test
    public void shouldGetConfigUpdateStateWhenAnUpdateOccurs() throws Exception {
        String originalMd5 = dataSource.forceLoad(dataSource.fileLocation()).configForEdit.getMd5();
        GoConfigHolder goConfigHolder = dataSource.forceLoad(dataSource.fileLocation());

        GoFileConfigDataSource.GoConfigSaveResult goConfigSaveResult = dataSource.writeWithLock(configHelper.addPipelineCommand(originalMd5, "p1", "s", "b"), goConfigHolder);
        assertThat(goConfigSaveResult.getConfigSaveState(), is(ConfigSaveState.UPDATED));
    }

    @Test
    public void shouldFallbackToLastKnownValidPartialsForValidationWhenConfigSaveWithLastKnownPartialsWithMainConfigFails() {
        String pipelineOneFromConfigRepo = "pipeline_one_from_config_repo";
        String invalidPartial = "invalidPartial";
        final String pipelineInMain = "pipeline_in_main";
        PartialConfig validPartialConfig = PartialConfigMother.withPipeline(pipelineOneFromConfigRepo, new RepoConfigOrigin(repoConfig, "1"));
        PartialConfig invalidPartialConfig = PartialConfigMother.invalidPartial(invalidPartial, new RepoConfigOrigin(repoConfig, "2"));
        cachedGoPartials.addOrUpdate(repoConfig.getRepo().getFingerprint(), validPartialConfig);
        cachedGoPartials.markAllKnownAsValid();
        cachedGoPartials.addOrUpdate(repoConfig.getRepo().getFingerprint(), invalidPartialConfig);

        GoFileConfigDataSource.GoConfigSaveResult result = dataSource.writeWithLock(cruiseConfig -> {
            cruiseConfig.addPipeline("default", PipelineConfigMother.createPipelineConfig(pipelineInMain, "stage", "job"));
            return cruiseConfig;
        }, new GoConfigHolder(configHelper.currentConfig(), configHelper.currentConfig()));
        assertThat(result.getConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString(invalidPartial)), is(false));
        assertThat(result.getConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString(pipelineOneFromConfigRepo)), is(true));
        assertThat(result.getConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString(pipelineInMain)), is(true));
        assertThat(cachedGoPartials.lastValidPartials().size(), is(1));
        PartialConfig partialConfig = cachedGoPartials.lastValidPartials().get(0);
        assertThat(partialConfig.getGroups(), is(validPartialConfig.getGroups()));
        assertThat(partialConfig.getEnvironments(), is(validPartialConfig.getEnvironments()));
        assertThat(partialConfig.getOrigin(), is(validPartialConfig.getOrigin()));
    }

    @Test
    public void shouldValidateConfigRepoLastKnownPartialsWithMainConfigAndUpdateConfigToIncludePipelinesFromPartials() {
        String pipelineFromConfigRepo = "pipeline_from_config_repo";
        final String pipelineInMain = "pipeline_in_main";
        PartialConfig partialConfig = PartialConfigMother.withPipeline(pipelineFromConfigRepo, new RepoConfigOrigin(repoConfig, "r2"));
        cachedGoPartials.addOrUpdate(repoConfig.getRepo().getFingerprint(), partialConfig);
        assertThat(cachedGoPartials.lastValidPartials().size(), is(1));
        assertThat(cachedGoPartials.lastValidPartials().get(0).getGroups().findGroup("group").hasPipeline(new CaseInsensitiveString(pipelineFromConfigRepo)), is(false));

        GoFileConfigDataSource.GoConfigSaveResult result = dataSource.writeWithLock(cruiseConfig -> {
            cruiseConfig.addPipeline("default", PipelineConfigMother.createPipelineConfig(pipelineInMain, "stage", "job"));
            return cruiseConfig;
        }, new GoConfigHolder(configHelper.currentConfig(), configHelper.currentConfig()));
        assertThat(result.getConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString(pipelineFromConfigRepo)), is(true));
        assertThat(result.getConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString(pipelineInMain)), is(true));
        assertThat(cachedGoPartials.lastValidPartials().size(), is(1));
        PartialConfig actualPartial = cachedGoPartials.lastValidPartials().get(0);
        assertThat(actualPartial.getGroups().findGroup("group").hasPipeline(new CaseInsensitiveString(pipelineFromConfigRepo)), is(true));
        assertThat(actualPartial.getGroups(), is(partialConfig.getGroups()));
        assertThat(actualPartial.getEnvironments(), is(partialConfig.getEnvironments()));
        assertThat(actualPartial.getOrigin(), is(partialConfig.getOrigin()));
    }

    @Test
    public void shouldNotReloadIfConfigDoesNotChange() throws Exception {
        try (LogFixture log = logFixtureFor(GoFileConfigDataSource.class, Level.DEBUG)) {
            dataSource.reloadIfModified();
            GoConfigHolder loadedConfig = dataSource.load();
            assertThat(log.getLog(), containsString("Config file changed at"));
            assertThat(loadedConfig, not(nullValue()));
            log.clear();

            loadedConfig = dataSource.load();
            assertThat(log.getLog(), not(containsString("Config file changed at")));
            assertThat(loadedConfig, is(nullValue()));
        }
    }

    @Test
    public void shouldBeAbleToConcurrentAccess() throws Exception {
        final String xml = ConfigMigrator.migrate(ConfigFileFixture.CONFIG_WITH_NANT_AND_EXEC_BUILDER);

        final List<Exception> errors = new Vector<>();
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                try {
                    goConfigDao.updateMailHost(new MailHost("hostname", 9999, "user", "password", false, false, "from@local", "admin@local"));
                } catch (Exception e) {
                    e.printStackTrace();
                    errors.add(e);
                }
            }
        }, "Update-license");

        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                try {
                    dataSource.write(xml, false);
                } catch (Exception e) {
                    e.printStackTrace();
                    errors.add(e);
                }
            }
        }, "Modify-config");

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();
        assertThat(errors.size(), is(0));
    }


    private static class UserAwarePipelineAddingCommand implements UpdateConfigCommand, UserAware {
        private final String pipelineName;
        private final String username;

        UserAwarePipelineAddingCommand(String pipelineName, String username) {
            this.pipelineName = pipelineName;
            this.username = username;
        }

        @Override
        public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
            cruiseConfig.addPipeline("my-grp", PipelineConfigMother.createPipelineConfig(pipelineName, "stage-other", "job-yet-another"));
            return cruiseConfig;
        }

        @Override
        public ConfigModifyingUser user() {
            return new ConfigModifyingUser(username);
        }
    }


    private MailHost getMailHost(String hostName) {
        return new MailHost(hostName, 9999, "user", "password", true, false, "from@local", "admin@local");
    }

    private interface UpdateConfig {
        void update(CruiseConfig cruiseConfig);
    }

    private void updateConfigOnFileSystem(UpdateConfig updateConfig) throws Exception {
        String cruiseConfigFile = systemEnvironment.getCruiseConfigFile();
        CruiseConfig updatedConfig = new Cloner().deepClone(goConfigService.getConfigForEditing());
        updateConfig.update(updatedConfig);
        File configFile = new File(cruiseConfigFile);
        FileOutputStream outputStream = new FileOutputStream(configFile);
        new MagicalGoConfigXmlWriter(configCache, configElementImplementationRegistry).write(updatedConfig, outputStream, true);
    }

}
