/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.config.exceptions.ConfigFileHasChangedException;
import com.thoughtworks.go.config.exceptions.ConfigMergeException;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.registry.NoPluginsInstalled;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.config.update.FullConfigUpdateCommand;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.GoConfigRevision;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoExtension;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.hamcrest.core.Is;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static com.thoughtworks.go.helper.ConfigFileFixture.VALID_XML_3169;
import static com.thoughtworks.go.server.newsecurity.SessionUtilsHelper.loginAs;
import static com.thoughtworks.go.util.GoConfigFileHelper.loadAndMigrate;
import static com.thoughtworks.go.util.LogFixture.logFixtureFor;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class GoFileConfigDataSourceTest {
    @Rule
    public final ClearSingleton clearSingleton = new ClearSingleton();

    private GoFileConfigDataSource dataSource;

    private GoConfigFileHelper configHelper;
    private SystemEnvironment systemEnvironment;
    private ConfigRepository configRepository;
    private TimeProvider timeProvider;
    private ConfigCache configCache = new ConfigCache();
    private GoConfigDao goConfigDao;
    private CachedGoPartials cachedGoPartials;
    private ConfigRepoConfig repoConfig;
    private FullConfigSaveMergeFlow fullConfigSaveMergeFlow;
    private FullConfigSaveNormalFlow fullConfigSaveNormalFlow;

    @Before
    public void setup() throws Exception {
        systemEnvironment = new SystemEnvironment();
        configHelper = new GoConfigFileHelper();
        configHelper.onSetUp();
        configRepository = new ConfigRepository(systemEnvironment);
        configRepository.initialize();
        timeProvider = mock(TimeProvider.class);
        fullConfigSaveMergeFlow = mock(FullConfigSaveMergeFlow.class);
        fullConfigSaveNormalFlow = mock(FullConfigSaveNormalFlow.class);
        when(fullConfigSaveNormalFlow.execute(Matchers.any(FullConfigUpdateCommand.class), Matchers.any(List.class), Matchers.any(String.class))).
                thenReturn(new GoConfigHolder(new BasicCruiseConfig(), new BasicCruiseConfig()));
        when(timeProvider.currentTime()).thenReturn(new Date());
        ConfigElementImplementationRegistry registry = ConfigElementImplementationRegistryMother.withNoPlugins();
        ServerHealthService serverHealthService = new ServerHealthService();
        cachedGoPartials = new CachedGoPartials(serverHealthService);
        dataSource = new GoFileConfigDataSource(new GoConfigMigration(e -> {
            throw new RuntimeException(e);
        }, configRepository, new TimeProvider(), configCache, registry),
                configRepository, systemEnvironment, timeProvider, configCache, registry, mock(ServerHealthService.class),
                cachedGoPartials, fullConfigSaveMergeFlow, fullConfigSaveNormalFlow);

        CachedGoConfig cachedGoConfig = new CachedGoConfig(serverHealthService, dataSource, mock(CachedGoPartials.class), null);
        cachedGoConfig.loadConfigIfNull();
        goConfigDao = new GoConfigDao(cachedGoConfig);
        configHelper.load();
        configHelper.usingCruiseConfigDao(goConfigDao);
        GoConfigWatchList configWatchList = new GoConfigWatchList(cachedGoConfig, mock(GoConfigService.class));
        ConfigElementImplementationRegistry configElementImplementationRegistry = new ConfigElementImplementationRegistry(new NoPluginsInstalled());
        GoConfigPluginService configPluginService = new GoConfigPluginService(mock(ConfigRepoExtension.class), new ConfigCache(), configElementImplementationRegistry, cachedGoConfig);
        repoConfig = new ConfigRepoConfig(new GitMaterialConfig("url"), "plugin");
        configHelper.addConfigRepo(repoConfig);
        loginAs("loser_boozer");
    }

    @After
    public void teardown() throws Exception {
        cachedGoPartials.clear();
        configHelper.onTearDown();
        systemEnvironment.reset(SystemEnvironment.ENABLE_CONFIG_MERGE_FEATURE);
    }

    private static class UserAwarePipelineAddingCommand implements UpdateConfigCommand, UserAware {
        private final String pipelineName;
        private final String username;

        UserAwarePipelineAddingCommand(String pipelineName, String username) {
            this.pipelineName = pipelineName;
            this.username = username;
        }

        public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
            cruiseConfig.addPipeline("my-grp", PipelineConfigMother.createPipelineConfig(pipelineName, "stage-other", "job-yet-another"));
            return cruiseConfig;
        }

        public ConfigModifyingUser user() {
            return new ConfigModifyingUser(username);
        }
    }

    @Test
    public void shouldUse_UserFromSession_asConfigModifyingUserWhenNoneGiven() throws GitAPIException, IOException {
        goConfigDao.updateMailHost(getMailHost("mailhost.local"));

        CruiseConfig cruiseConfig = goConfigDao.load();
        GoConfigRevision revision = configRepository.getRevision(cruiseConfig.getMd5());
        assertThat(revision.getUsername(), is("loser_boozer"));
    }

    @Test
    public void shouldVersionTheCruiseConfigXmlWhenSaved() throws Exception {
        CachedGoConfig cachedGoConfig = configHelper.getCachedGoConfig();
        CruiseConfig configForEdit = cachedGoConfig.loadForEditing();
        GoConfigHolder configHolder = new GoConfigHolder(cachedGoConfig.currentConfig(), configForEdit);

        Date loserChangedAt = new DateTime().plusDays(2).toDate();
        when(timeProvider.currentTime()).thenReturn(loserChangedAt);

        GoConfigHolder afterFirstSave = dataSource.writeWithLock(new UserAwarePipelineAddingCommand("foo-pipeline", "loser"), configHolder).getConfigHolder();

        Date biggerLoserChangedAt = new DateTime().plusDays(4).toDate();
        when(timeProvider.currentTime()).thenReturn(biggerLoserChangedAt);

        GoConfigHolder afterSecondSave = dataSource.writeWithLock(new UserAwarePipelineAddingCommand("bar-pipeline", "bigger_loser"), afterFirstSave).getConfigHolder();

        String expectedMd5 = afterFirstSave.config.getMd5();
        GoConfigRevision firstRev = configRepository.getRevision(expectedMd5);
        assertThat(firstRev.getUsername(), is("loser"));
        assertThat(firstRev.getGoVersion(), is(CurrentGoCDVersion.getInstance().formatted()));
        assertThat(firstRev.getMd5(), is(expectedMd5));
        assertThat(firstRev.getTime(), is(loserChangedAt));
        assertThat(firstRev.getSchemaVersion(), is(GoConstants.CONFIG_SCHEMA_VERSION));
        assertThat(ConfigMigrator.load(firstRev.getContent()), is(afterFirstSave.configForEdit));

        CruiseConfig config = afterSecondSave.config;
        assertThat(config.hasPipelineNamed(new CaseInsensitiveString("bar-pipeline")), is(true));
        expectedMd5 = config.getMd5();
        GoConfigRevision secondRev = configRepository.getRevision(expectedMd5);
        assertThat(secondRev.getUsername(), is("bigger_loser"));
        assertThat(secondRev.getGoVersion(), is(CurrentGoCDVersion.getInstance().formatted()));
        assertThat(secondRev.getMd5(), is(expectedMd5));
        assertThat(secondRev.getTime(), is(biggerLoserChangedAt));
        assertThat(secondRev.getSchemaVersion(), is(GoConstants.CONFIG_SCHEMA_VERSION));
        assertThat(ConfigMigrator.load(secondRev.getContent()), is(afterSecondSave.configForEdit));
    }

    @Test
    public void shouldSaveTheCruiseConfigXml() throws Exception {
        File file = dataSource.fileLocation();

        dataSource.write(ConfigMigrator.migrate(VALID_XML_3169), false);

        assertThat(FileUtils.readFileToString(file, UTF_8), containsString("http://hg-server/hg/connectfour"));
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

        assertThat(FileUtils.readFileToString(file, UTF_8), Is.is(originalCopy));
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
    public void shouldBeAbleToConcurrentAccess() throws Exception {
        GoConfigFileHelper helper = new GoConfigFileHelper(loadAndMigrate(ConfigFileFixture.CONFIG_WITH_NANT_AND_EXEC_BUILDER));
        final String xml = FileUtils.readFileToString(helper.getConfigFile(), UTF_8);

        final List<Exception> errors = new Vector<>();
        Thread thread1 = new Thread(new Runnable() {
            public void run() {
                for (int i = 0; i < 5; i++) {
                    try {
                        goConfigDao.updateMailHost(new MailHost("hostname", 9999, "user", "password", false, false, "from@local", "admin@local"));
                    } catch (Exception e) {
                        e.printStackTrace();
                        errors.add(e);
                    }
                }
            }
        }, "Update-license");

        Thread thread2 = new Thread(new Runnable() {
            public void run() {
                for (int i = 0; i < 5; i++) {
                    try {
                        dataSource.write(xml, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                        errors.add(e);
                    }
                }
            }
        }, "Modify-config");

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();
        assertThat(errors.size(), is(0));
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
        configHelper.addMailHost(getMailHost("mailhost.local.old"));
        String originalMd5 = dataSource.forceLoad(dataSource.fileLocation()).configForEdit.getMd5();
        configHelper.addMailHost(getMailHost("mailhost.local"));
        GoConfigHolder goConfigHolder = dataSource.forceLoad(dataSource.fileLocation());

        GoFileConfigDataSource.GoConfigSaveResult goConfigSaveResult = dataSource.writeWithLock(configHelper.addPipelineCommand(originalMd5, "p1", "s", "b"), goConfigHolder);
        assertThat(goConfigSaveResult.getConfigSaveState(), is(ConfigSaveState.MERGED));
    }

    private MailHost getMailHost(String hostName) {
        return new MailHost(hostName, 9999, "user", "password", true, false, "from@local", "admin@local");
    }

    @Test
    public void shouldGetConfigUpdateStateWhenAnUpdateOccurs() throws Exception {
        String originalMd5 = dataSource.forceLoad(dataSource.fileLocation()).configForEdit.getMd5();
        GoConfigHolder goConfigHolder = dataSource.forceLoad(dataSource.fileLocation());

        GoFileConfigDataSource.GoConfigSaveResult goConfigSaveResult = dataSource.writeWithLock(configHelper.addPipelineCommand(originalMd5, "p1", "s", "b"), goConfigHolder);
        assertThat(goConfigSaveResult.getConfigSaveState(), is(ConfigSaveState.UPDATED));
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldNotRetryConfigSaveWhenConfigRepoIsNotSetup() throws Exception {
        MagicalGoConfigXmlLoader loader = mock(MagicalGoConfigXmlLoader.class);
        MagicalGoConfigXmlWriter writer = mock(MagicalGoConfigXmlWriter.class);
        GoConfigMigration migration = mock(GoConfigMigration.class);
        ServerHealthService serverHealthService = mock(ServerHealthService.class);
        CachedGoPartials cachedGoPartials = mock(CachedGoPartials.class);
        ConfigRepository configRepository = mock(ConfigRepository.class);
        dataSource = new GoFileConfigDataSource(migration, configRepository, systemEnvironment, timeProvider, loader, writer, serverHealthService, cachedGoPartials, null, null, null, null);

        final String pipelineName = UUID.randomUUID().toString();
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines(pipelineName);
        ConfigErrors configErrors = new ConfigErrors();
        configErrors.add("key", "some error");
        when(loader.loadConfigHolder(Matchers.any(String.class))).thenThrow(new GoConfigInvalidException(cruiseConfig, configErrors.firstError()));

        try {
            dataSource.writeWithLock(new UpdateConfigCommand() {
                @Override
                public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                    cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString(pipelineName)).clear();
                    return cruiseConfig;
                }
            }, new GoConfigHolder(cruiseConfig, cruiseConfig));
            fail("expected the test to fail");
        } catch (Exception e) {
            verifyZeroInteractions(configRepository);
            verifyZeroInteractions(serverHealthService);
            verify(loader, times(1)).loadConfigHolder(Matchers.any(String.class), Matchers.any(MagicalGoConfigXmlLoader.Callback.class));
        }
    }

    @Test
    public void shouldReturnTrueWhenBothValidAndKnownPartialsListsAreEmpty() {
        assertThat(dataSource.areKnownPartialsSameAsValidPartials(new ArrayList<>(), new ArrayList<>()), is(true));
    }

    @Test
    public void shouldReturnTrueWhenValidPartialsListIsSameAsKnownPartialList() {
        ConfigRepoConfig repo1 = new ConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig(), "plugin");
        ConfigRepoConfig repo2 = new ConfigRepoConfig(MaterialConfigsMother.svnMaterialConfig(), "plugin");
        PartialConfig partialConfig1 = PartialConfigMother.withPipeline("p1", new RepoConfigOrigin(repo1, "git_r1"));
        PartialConfig partialConfig2 = PartialConfigMother.withPipeline("p2", new RepoConfigOrigin(repo2, "svn_r1"));
        List<PartialConfig> known = asList(partialConfig1, partialConfig2);
        List<PartialConfig> valid = asList(partialConfig1, partialConfig2);
        assertThat(dataSource.areKnownPartialsSameAsValidPartials(known, valid), is(true));
    }

    @Test
    public void shouldReturnFalseWhenValidPartialsListIsNotTheSameAsKnownPartialList() {
        PartialConfig partialConfig1 = PartialConfigMother.withPipeline("p1", new RepoConfigOrigin(new ConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig(), "plugin"), "git_r1"));
        PartialConfig partialConfig2 = PartialConfigMother.withPipeline("p2", new RepoConfigOrigin(new ConfigRepoConfig(MaterialConfigsMother.svnMaterialConfig(), "plugin"), "svn_r1"));
        PartialConfig partialConfig3 = PartialConfigMother.withPipeline("p1", new RepoConfigOrigin(new ConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig(), "plugin"), "git_r2"));
        PartialConfig partialConfig4 = PartialConfigMother.withPipeline("p2", new RepoConfigOrigin(new ConfigRepoConfig(MaterialConfigsMother.svnMaterialConfig(), "plugin"), "svn_r2"));
        List<PartialConfig> known = asList(partialConfig1, partialConfig2);
        List<PartialConfig> valid = asList(partialConfig3, partialConfig4);
        assertThat(dataSource.areKnownPartialsSameAsValidPartials(known, valid), is(false));
    }

    @Test
    public void shouldReturnFalseWhenValidPartialsListIsEmptyWhenKnownListIsNot() {
        ConfigRepoConfig repo1 = new ConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig(), "plugin");
        ConfigRepoConfig repo2 = new ConfigRepoConfig(MaterialConfigsMother.svnMaterialConfig(), "plugin");
        PartialConfig partialConfig1 = PartialConfigMother.withPipeline("p1", new RepoConfigOrigin(repo1, "git_r1"));
        PartialConfig partialConfig2 = PartialConfigMother.withPipeline("p2", new RepoConfigOrigin(repo2, "svn_r1"));
        List<PartialConfig> known = asList(partialConfig1, partialConfig2);
        List<PartialConfig> valid = new ArrayList<>();
        assertThat(dataSource.areKnownPartialsSameAsValidPartials(known, valid), is(false));
    }

    @Test
    public void shouldUpdateConfigWithLastKnownPartials_OnWriteFullConfigWithLock() throws Exception {
        BasicCruiseConfig configForEdit = new BasicCruiseConfig();
        MagicalGoConfigXmlLoader.setMd5(configForEdit, "md5");
        FullConfigUpdateCommand updatingCommand = new FullConfigUpdateCommand(new BasicCruiseConfig(), "md5");
        GoConfigHolder configHolder = new GoConfigHolder(new BasicCruiseConfig(), configForEdit);
        List<PartialConfig> lastKnownPartials = mock(List.class);
        CachedGoPartials cachedGoPartials = mock(CachedGoPartials.class);

        GoFileConfigDataSource source = new GoFileConfigDataSource(null, null, null, null, null, null, null, cachedGoPartials,
                fullConfigSaveMergeFlow, fullConfigSaveNormalFlow);

        when(cachedGoPartials.lastKnownPartials()).thenReturn(lastKnownPartials);
        when(fullConfigSaveNormalFlow.execute(Matchers.any(FullConfigUpdateCommand.class), Matchers.any(List.class), Matchers.any(String.class))).
                thenReturn(new GoConfigHolder(new BasicCruiseConfig(), new BasicCruiseConfig()));

        source.writeFullConfigWithLock(updatingCommand, configHolder);

        verify(fullConfigSaveNormalFlow).execute(updatingCommand, lastKnownPartials, "loser_boozer");
    }

    @Test
    public void shouldEnsureMergeFlowWithLastKnownPartialsIfConfigHasChangedBetweenUpdates_OnWriteFullConfigWithLock() throws Exception {
        BasicCruiseConfig configForEdit = new BasicCruiseConfig();
        MagicalGoConfigXmlLoader.setMd5(configForEdit, "new_md5");
        FullConfigUpdateCommand updatingCommand = new FullConfigUpdateCommand(new BasicCruiseConfig(), "old_md5");
        GoConfigHolder configHolder = new GoConfigHolder(new BasicCruiseConfig(), configForEdit);
        List<PartialConfig> lastKnownPartials = mock(List.class);
        CachedGoPartials cachedGoPartials = mock(CachedGoPartials.class);

        GoFileConfigDataSource source = new GoFileConfigDataSource(null, null, systemEnvironment, null, null, null, null, cachedGoPartials,
                fullConfigSaveMergeFlow, fullConfigSaveNormalFlow);

        when(cachedGoPartials.lastKnownPartials()).thenReturn(lastKnownPartials);
        when(fullConfigSaveMergeFlow.execute(Matchers.any(FullConfigUpdateCommand.class), Matchers.any(List.class), Matchers.any(String.class))).
                thenReturn(new GoConfigHolder(new BasicCruiseConfig(), new BasicCruiseConfig()));

        source.writeFullConfigWithLock(updatingCommand, configHolder);

        verify(fullConfigSaveMergeFlow).execute(updatingCommand, lastKnownPartials, "loser_boozer");
    }

    @Test
    public void shouldFallbackOnLastValidPartialsIfUpdateWithLastKnownPartialsFails_OnWriteFullConfigWithLock() throws Exception {
        PartialConfig partialConfig1 = PartialConfigMother.withPipeline("p1", new RepoConfigOrigin(new ConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig(), "plugin"), "git_r1"));
        PartialConfig partialConfig2 = PartialConfigMother.withPipeline("p2", new RepoConfigOrigin(new ConfigRepoConfig(MaterialConfigsMother.svnMaterialConfig(), "plugin"), "svn_r1"));
        List<PartialConfig> known = asList(partialConfig1);
        List<PartialConfig> valid = asList(partialConfig2);

        BasicCruiseConfig configForEdit = new BasicCruiseConfig();
        MagicalGoConfigXmlLoader.setMd5(configForEdit, "md5");
        FullConfigUpdateCommand updatingCommand = new FullConfigUpdateCommand(new BasicCruiseConfig(), "md5");
        GoConfigHolder configHolder = new GoConfigHolder(new BasicCruiseConfig(), configForEdit);
        CachedGoPartials cachedGoPartials = mock(CachedGoPartials.class);
        GoFileConfigDataSource source = new GoFileConfigDataSource(null, null, systemEnvironment, null, null, null, null, cachedGoPartials,
                fullConfigSaveMergeFlow, fullConfigSaveNormalFlow);

        when(cachedGoPartials.lastKnownPartials()).thenReturn(known);
        when(cachedGoPartials.lastValidPartials()).thenReturn(valid);
        when(fullConfigSaveNormalFlow.execute(updatingCommand, known, "loser_boozer")).
                thenThrow(new Exception());
        when(fullConfigSaveNormalFlow.execute(updatingCommand, valid, "loser_boozer")).
                thenReturn(new GoConfigHolder(new BasicCruiseConfig(), new BasicCruiseConfig()));

        source.writeFullConfigWithLock(updatingCommand, configHolder);

        verify(fullConfigSaveNormalFlow).execute(updatingCommand, known, "loser_boozer");
        verify(fullConfigSaveNormalFlow).execute(updatingCommand, valid, "loser_boozer");
    }

    @Test(expected = RuntimeException.class)
    public void shouldNotRetryConfigUpdateIfLastKnownPartialsAreEmpty_OnWriteFullConfigWithLock() throws Exception {
        List<PartialConfig> known = new ArrayList<>();

        BasicCruiseConfig configForEdit = new BasicCruiseConfig();
        MagicalGoConfigXmlLoader.setMd5(configForEdit, "md5");
        FullConfigUpdateCommand updatingCommand = new FullConfigUpdateCommand(new BasicCruiseConfig(), "md5");
        GoConfigHolder configHolder = new GoConfigHolder(new BasicCruiseConfig(), configForEdit);
        CachedGoPartials cachedGoPartials = mock(CachedGoPartials.class);
        GoFileConfigDataSource source = new GoFileConfigDataSource(null, null, systemEnvironment, null, null, null, null, cachedGoPartials,
                fullConfigSaveMergeFlow, fullConfigSaveNormalFlow);

        when(cachedGoPartials.lastKnownPartials()).thenReturn(known);
        when(fullConfigSaveNormalFlow.execute(updatingCommand, known, "loser_boozer")).
                thenThrow(new GoConfigInvalidException(configForEdit, "error"));

        source.writeFullConfigWithLock(updatingCommand, configHolder);
    }

    @Test(expected = RuntimeException.class)
    public void shouldNotRetryConfigUpdateIfLastKnownAndValidPartialsAreSame_OnWriteFullConfigWithLock() throws Exception {
        PartialConfig partialConfig1 = PartialConfigMother.withPipeline("p1", new RepoConfigOrigin(new ConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig(), "plugin"), "git_r1"));
        List<PartialConfig> known = asList(partialConfig1);
        List<PartialConfig> valid = asList(partialConfig1);

        BasicCruiseConfig configForEdit = new BasicCruiseConfig();
        MagicalGoConfigXmlLoader.setMd5(configForEdit, "md5");
        FullConfigUpdateCommand updatingCommand = new FullConfigUpdateCommand(new BasicCruiseConfig(), "md5");
        GoConfigHolder configHolder = new GoConfigHolder(new BasicCruiseConfig(), configForEdit);
        CachedGoPartials cachedGoPartials = mock(CachedGoPartials.class);
        GoFileConfigDataSource source = new GoFileConfigDataSource(null, null, systemEnvironment, null, null, null, null, cachedGoPartials,
                fullConfigSaveMergeFlow, fullConfigSaveNormalFlow);

        when(cachedGoPartials.lastKnownPartials()).thenReturn(known);
        when(cachedGoPartials.lastValidPartials()).thenReturn(valid);
        when(fullConfigSaveNormalFlow.execute(updatingCommand, known, "loser_boozer")).
                thenThrow(new GoConfigInvalidException(configForEdit, "error"));

        source.writeFullConfigWithLock(updatingCommand, configHolder);
    }

    @Test(expected = RuntimeException.class)
    public void shouldErrorOutOnTryingToMergeConfigsIfConfigMergeFeatureIsDisabled_OnWriteFullConfigWithLock() throws Exception {
        BasicCruiseConfig configForEdit = new BasicCruiseConfig();
        MagicalGoConfigXmlLoader.setMd5(configForEdit, "new_md5");
        FullConfigUpdateCommand updatingCommand = new FullConfigUpdateCommand(new BasicCruiseConfig(), "old_md5");
        GoConfigHolder configHolder = new GoConfigHolder(new BasicCruiseConfig(), configForEdit);
        List<PartialConfig> lastKnownPartials = new ArrayList<>();
        CachedGoPartials cachedGoPartials = mock(CachedGoPartials.class);

        GoFileConfigDataSource source = new GoFileConfigDataSource(null, null, systemEnvironment, null, null, null, null, cachedGoPartials,
                fullConfigSaveMergeFlow, fullConfigSaveNormalFlow);

        when(cachedGoPartials.lastKnownPartials()).thenReturn(lastKnownPartials);
        systemEnvironment.set(SystemEnvironment.ENABLE_CONFIG_MERGE_FEATURE, false);

        source.writeFullConfigWithLock(updatingCommand, configHolder);

        verify(fullConfigSaveMergeFlow, never()).execute(Matchers.any(FullConfigUpdateCommand.class), Matchers.any(List.class), Matchers.any(String.class));
        verify(fullConfigSaveNormalFlow, never()).execute(Matchers.any(FullConfigUpdateCommand.class), Matchers.any(List.class), Matchers.any(String.class));
    }

    @Test
    public void shouldUpdateAndReloadConfigUsingFullSaveNormalFlowWithLastKnownPartials_onLoad() throws Exception {
        GoConfigFileReader goConfigFileReader = mock(GoConfigFileReader.class);
        MagicalGoConfigXmlLoader loader = mock(MagicalGoConfigXmlLoader.class);
        CruiseConfig cruiseConfig = mock(CruiseConfig.class);
        List lastKnownPartials = mock(List.class);
        CachedGoPartials cachedGoPartials = mock(CachedGoPartials.class);
        GoConfigHolder goConfigHolder = new GoConfigHolder(new BasicCruiseConfig(), new BasicCruiseConfig());

        ArgumentCaptor<FullConfigUpdateCommand> commandArgumentCaptor = ArgumentCaptor.forClass(FullConfigUpdateCommand.class);
        ArgumentCaptor<List> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

        when(goConfigFileReader.fileLocation()).thenReturn(new File(""));
        when(goConfigFileReader.configXml()).thenReturn("config_xml");
        when(loader.deserializeConfig("config_xml")).thenReturn(cruiseConfig);
        when(cachedGoPartials.lastKnownPartials()).thenReturn(lastKnownPartials);
        when(fullConfigSaveNormalFlow.execute(commandArgumentCaptor.capture(), listArgumentCaptor.capture(), stringArgumentCaptor.capture())).thenReturn(goConfigHolder);

        GoFileConfigDataSource source = new GoFileConfigDataSource(null, null, systemEnvironment, null, loader, null, null, cachedGoPartials,
                fullConfigSaveMergeFlow, fullConfigSaveNormalFlow, goConfigFileReader, null);

        GoConfigHolder configHolder = source.load();

        assertThat(configHolder, is(goConfigHolder));
        assertThat(commandArgumentCaptor.getValue().configForEdit(), is(cruiseConfig));
        assertThat(listArgumentCaptor.getValue(), is(lastKnownPartials));
        assertThat(stringArgumentCaptor.getValue(), is("Filesystem"));
    }

    @Test
    public void shouldReloadConfigUsingFullSaveNormalFlowWithLastValidPartialsIfUpdatingWithLastKnownPartialsFails_onLoad() throws Exception {
        GoConfigFileReader goConfigFileReader = mock(GoConfigFileReader.class);
        MagicalGoConfigXmlLoader loader = mock(MagicalGoConfigXmlLoader.class);
        CruiseConfig cruiseConfig = mock(CruiseConfig.class);
        PartialConfigMother.withPipeline("P1");
        List lastKnownPartials = Arrays.asList(PartialConfigMother.withPipeline("P1"));
        List lastValidPartials = Arrays.asList(PartialConfigMother.withPipeline("P2"), PartialConfigMother.withPipeline("P3"));
        CachedGoPartials cachedGoPartials = mock(CachedGoPartials.class);
        GoConfigHolder goConfigHolder = new GoConfigHolder(new BasicCruiseConfig(), new BasicCruiseConfig());

        ArgumentCaptor<FullConfigUpdateCommand> commandArgumentCaptor = ArgumentCaptor.forClass(FullConfigUpdateCommand.class);
        ArgumentCaptor<List> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

        when(goConfigFileReader.fileLocation()).thenReturn(new File(""));
        when(goConfigFileReader.configXml()).thenReturn("config_xml");
        when(loader.deserializeConfig("config_xml")).thenReturn(cruiseConfig);
        when(cachedGoPartials.lastKnownPartials()).thenReturn(lastKnownPartials);
        when(cachedGoPartials.lastValidPartials()).thenReturn(lastValidPartials);
        when(fullConfigSaveNormalFlow.execute(commandArgumentCaptor.capture(), listArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenThrow(new GoConfigInvalidException(null, null)).thenReturn(goConfigHolder);

        GoFileConfigDataSource source = new GoFileConfigDataSource(null, null, systemEnvironment, null, loader, null, null, cachedGoPartials,
                fullConfigSaveMergeFlow, fullConfigSaveNormalFlow, goConfigFileReader, null);

        GoConfigHolder configHolder = source.load();

        assertThat(configHolder, is(goConfigHolder));
        assertThat(commandArgumentCaptor.getValue().configForEdit(), is(cruiseConfig));
        assertThat(listArgumentCaptor.getValue(), is(lastValidPartials));
        assertThat(stringArgumentCaptor.getValue(), is("Filesystem"));
    }
}
