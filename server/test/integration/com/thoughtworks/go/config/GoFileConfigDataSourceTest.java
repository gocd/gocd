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

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.exceptions.ConfigFileHasChangedException;
import com.thoughtworks.go.config.exceptions.ConfigMergeException;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.domain.GoConfigRevision;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.helper.NoOpMetricsProbeService;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
import com.thoughtworks.go.server.util.ServerVersion;
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
import org.junit.Test;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.userdetails.User;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import static com.thoughtworks.go.helper.ConfigFileFixture.VALID_XML_3169;
import static com.thoughtworks.go.util.GoConfigFileHelper.loadAndMigrate;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GoFileConfigDataSourceTest {
    private GoFileConfigDataSource dataSource;

    private GoConfigFileHelper configHelper;
    private SystemEnvironment systemEnvironment;
    private ConfigRepository configRepository;
    private TimeProvider timeProvider;
    private ConfigCache configCache = new ConfigCache();
    private MetricsProbeService metricsProbeService = new NoOpMetricsProbeService();
    private GoConfigDao goConfigDao;

    @Before
    public void setup() throws Exception {
        systemEnvironment = new SystemEnvironment();
        configHelper = new GoConfigFileHelper();
        configHelper.onSetUp();
        configRepository = new ConfigRepository(systemEnvironment);
        configRepository.initialize();
        timeProvider = mock(TimeProvider.class);
        when(timeProvider.currentTime()).thenReturn(new Date());
        ServerVersion serverVersion = new ServerVersion();
        ConfigElementImplementationRegistry registry = ConfigElementImplementationRegistryMother.withNoPlugins();
        dataSource = new GoFileConfigDataSource(new GoConfigMigration(new GoConfigMigration.UpgradeFailedHandler() {
            public void handle(Exception e) {
                throw new RuntimeException(e);
            }
        }, configRepository, new TimeProvider(), configCache, registry, metricsProbeService),
                configRepository, systemEnvironment, timeProvider, configCache, serverVersion, registry, metricsProbeService, mock(ServerHealthService.class));
        dataSource.upgradeIfNecessary();
        CachedFileGoConfig fileService = new CachedFileGoConfig(dataSource, new ServerHealthService());
        fileService.loadConfigIfNull();
        goConfigDao = new GoConfigDao(fileService, metricsProbeService);
        configHelper.load();
        configHelper.usingCruiseConfigDao(goConfigDao);
    }

    @After
    public void teardown() throws Exception {
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
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(new User("loser_boozer", "pass", true, true, true, true, new GrantedAuthority[]{}), null));
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
        assertThat(firstRev.getGoVersion(), is("N/A"));
        assertThat(firstRev.getMd5(), is(expectedMd5));
        assertThat(firstRev.getTime(), is(loserChangedAt));
        assertThat(firstRev.getSchemaVersion(), is(GoConstants.CONFIG_SCHEMA_VERSION));
        assertThat(com.thoughtworks.go.config.ConfigMigrator.load(firstRev.getContent()), is(afterFirstSave.configForEdit));

        CruiseConfig config = afterSecondSave.config;
        assertThat(config.hasPipelineNamed(new CaseInsensitiveString("bar-pipeline")), is(true));
        expectedMd5 = config.getMd5();
        GoConfigRevision secondRev = configRepository.getRevision(expectedMd5);
        assertThat(secondRev.getUsername(), is("bigger_loser"));
        assertThat(secondRev.getGoVersion(), is("N/A"));
        assertThat(secondRev.getMd5(), is(expectedMd5));
        assertThat(secondRev.getTime(), is(biggerLoserChangedAt));
        assertThat(secondRev.getSchemaVersion(), is(GoConstants.CONFIG_SCHEMA_VERSION));
        assertThat(com.thoughtworks.go.config.ConfigMigrator.load(secondRev.getContent()), is(afterSecondSave.configForEdit));
    }

    @Test
    public void shouldSaveTheCruiseConfigXml() throws Exception {
        File file = dataSource.fileLocation();

        dataSource.write(ConfigMigrator.migrate(VALID_XML_3169), false);

        assertThat(FileUtils.readFileToString(file), containsString("http://hg-server/hg/connectfour"));
    }

    @Test
    public void shouldNotCorruptTheCruiseConfigXml() throws Exception {
        File file = dataSource.fileLocation();
        String originalCopy = FileUtils.readFileToString(file);

        try {
            dataSource.write("abc", false);
            fail("Should not allow us to write an invalid config");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Content is not allowed in prolog"));
        }

        assertThat(FileUtils.readFileToString(file), Is.is(originalCopy));
    }

    @Test
    public void shouldLoadAsUser_Filesystem_WithMd5Sum() throws Exception {
        GoConfigHolder configHolder = goConfigDao.loadConfigHolder();
        String md5 = DigestUtils.md5Hex(FileUtils.readFileToString(dataSource.fileLocation()));
        assertThat(configHolder.configForEdit.getMd5(), is(md5));
        assertThat(configHolder.config.getMd5(), is(md5));

        CruiseConfig forEdit = configHolder.configForEdit;
        forEdit.addPipeline("my-awesome-group", PipelineConfigMother.createPipelineConfig("pipeline-foo", "stage-bar", "job-baz"));
        FileOutputStream fos = new FileOutputStream(dataSource.fileLocation());
        new MagicalGoConfigXmlWriter(configCache, ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService).write(forEdit, fos, false);

        configHolder = dataSource.load();
        String xmlText = FileUtils.readFileToString(dataSource.fileLocation());
        String secondMd5 = DigestUtils.md5Hex(xmlText);
        assertThat(configHolder.configForEdit.getMd5(), is(secondMd5));
        assertThat(configHolder.config.getMd5(), is(secondMd5));
        assertThat(configHolder.configForEdit.getMd5(), is(not(md5)));
        GoConfigRevision commitedVersion = configRepository.getRevision(secondMd5);
        assertThat(commitedVersion.getContent(), is(xmlText));
        assertThat(commitedVersion.getUsername(), is(GoFileConfigDataSource.FILESYSTEM));
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
        FileUtils.writeStringToFile(dataSource.fileLocation(), configContent);

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
        FileUtils.writeStringToFile(dataSource.fileLocation(), configContent);

        GoConfigHolder configHolder = dataSource.load();

        PipelineConfig pipelineConfig = configHolder.config.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));
        TfsMaterialConfig tfsMaterial = (TfsMaterialConfig) pipelineConfig.materialConfigs().get(0);
        assertThat(tfsMaterial.getEncryptedPassword(), is(not(nullValue())));
    }

    @Test
    public void shouldNotReloadIfConfigDoesNotChange() throws Exception {
        LogFixture log = LogFixture.startListening();
        dataSource.reloadIfModified();
        GoConfigHolder loadedConfig = dataSource.load();
        assertThat(log.getLog(), containsString("Config file changed at"));
        assertThat(loadedConfig, not(nullValue()));
        log.clear();

        loadedConfig = dataSource.load();
        assertThat(log.getLog(), not(containsString("Config file changed at")));
        assertThat(loadedConfig, is(nullValue()));
    }

    @Test
    public void shouldUpdateFileAttributesIfFileContentsHaveNotChanged() throws Exception {//so that it doesn't have to do the file content checksum computation next time
        dataSource.reloadIfModified();
        assertThat(dataSource.load(), not(nullValue()));

        GoFileConfigDataSource.ReloadIfModified reloadStrategy = (GoFileConfigDataSource.ReloadIfModified) ReflectionUtil.getField(dataSource, "reloadStrategy");

        ReflectionUtil.setField(reloadStrategy, "lastModified", -1);
        ReflectionUtil.setField(reloadStrategy, "prevSize", -1);

        assertThat(dataSource.load(), is(nullValue()));

        assertThat((Long) ReflectionUtil.getField(reloadStrategy, "lastModified"), is(dataSource.fileLocation().lastModified()));
        assertThat((Long) ReflectionUtil.getField(reloadStrategy, "prevSize"), is(dataSource.fileLocation().length()));
    }

    @Test
    public void shouldBeAbleToConcurrentAccess() throws Exception {
        GoConfigFileHelper helper = new GoConfigFileHelper(loadAndMigrate(ConfigFileFixture.CONFIG_WITH_NANT_AND_EXEC_BUILDER));
        final String xml = FileUtil.readContentFromFile(helper.getConfigFile());

        final List<Exception> errors = new Vector<Exception>();
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
}
