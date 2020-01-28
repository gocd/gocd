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

import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.config.update.FullConfigUpdateCommand;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.PartialConfigMother;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.File;
import java.util.*;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class GoFileConfigDataSourceTest {
    @Rule
    public final ClearSingleton clearSingleton = new ClearSingleton();
    private GoFileConfigDataSource dataSource;
    @Mock
    private SystemEnvironment systemEnvironment;
    @Mock
    private ConfigRepository configRepository;
    @Mock
    private TimeProvider timeProvider;
    @Mock
    private CachedGoPartials cachedGoPartials;
    @Mock
    private FullConfigSaveMergeFlow fullConfigSaveMergeFlow;
    @Mock
    private FullConfigSaveNormalFlow fullConfigSaveNormalFlow;
    @Mock
    private ServerHealthService serverHealthService;
    @Mock
    private GoConfigMigration goConfigMigration;
    @Mock
    private MagicalGoConfigXmlLoader xmlLoader;
    @Mock
    private MagicalGoConfigXmlWriter xmlWriter;
    @Mock
    private GoConfigFileWriter goConfigFileWriter;
    @Mock
    private GoConfigFileReader goConfigFileReader;
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() throws Exception {
        initMocks(this);
        dataSource = new GoFileConfigDataSource(goConfigMigration,
                configRepository, systemEnvironment, timeProvider, xmlLoader, xmlWriter, serverHealthService,
                cachedGoPartials, fullConfigSaveMergeFlow, fullConfigSaveNormalFlow, goConfigFileReader, goConfigFileWriter);


    }

    @Test
    public void shouldNotRetryConfigSaveWhenConfigRepoIsNotSetup() throws Exception {
        final String pipelineName = UUID.randomUUID().toString();
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines(pipelineName);
        ConfigErrors configErrors = new ConfigErrors();
        configErrors.add("key", "some error");
        when(xmlLoader.loadConfigHolder(any(String.class))).thenThrow(new GoConfigInvalidException(cruiseConfig, configErrors.firstError()));

        try {
            dataSource.writeWithLock(cruiseConfig1 -> {
                cruiseConfig1.getPipelineConfigByName(new CaseInsensitiveString(pipelineName)).clear();
                return cruiseConfig1;
            }, new GoConfigHolder(cruiseConfig, cruiseConfig));
            fail("expected the test to fail");
        } catch (Exception e) {
            verifyZeroInteractions(configRepository);
            verifyZeroInteractions(serverHealthService);
            verify(xmlLoader, times(1)).loadConfigHolder(any(String.class), any(MagicalGoConfigXmlLoader.Callback.class));
        }
    }

    @Test
    public void shouldReturnTrueWhenBothValidAndKnownPartialsListsAreEmpty() {
        assertThat(dataSource.areKnownPartialsSameAsValidPartials(new ArrayList<>(), new ArrayList<>()), is(true));
    }

    @Test
    public void shouldReturnTrueWhenValidPartialsListIsSameAsKnownPartialList() {
        ConfigRepoConfig repo1 = ConfigRepoConfig.createConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig(), "plugin");
        ConfigRepoConfig repo2 = ConfigRepoConfig.createConfigRepoConfig(MaterialConfigsMother.svnMaterialConfig(), "plugin");
        PartialConfig partialConfig1 = PartialConfigMother.withPipeline("p1", new RepoConfigOrigin(repo1, "git_r1"));
        PartialConfig partialConfig2 = PartialConfigMother.withPipeline("p2", new RepoConfigOrigin(repo2, "svn_r1"));
        List<PartialConfig> known = asList(partialConfig1, partialConfig2);
        List<PartialConfig> valid = asList(partialConfig1, partialConfig2);
        assertThat(dataSource.areKnownPartialsSameAsValidPartials(known, valid), is(true));
    }

    @Test
    public void shouldReturnFalseWhenValidPartialsListIsNotTheSameAsKnownPartialList() {
        PartialConfig partialConfig1 = PartialConfigMother.withPipeline("p1", new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig(), "plugin"), "git_r1"));
        PartialConfig partialConfig2 = PartialConfigMother.withPipeline("p2", new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(MaterialConfigsMother.svnMaterialConfig(), "plugin"), "svn_r1"));
        PartialConfig partialConfig3 = PartialConfigMother.withPipeline("p1", new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig(), "plugin"), "git_r2"));
        PartialConfig partialConfig4 = PartialConfigMother.withPipeline("p2", new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(MaterialConfigsMother.svnMaterialConfig(), "plugin"), "svn_r2"));
        List<PartialConfig> known = asList(partialConfig1, partialConfig2);
        List<PartialConfig> valid = asList(partialConfig3, partialConfig4);
        assertThat(dataSource.areKnownPartialsSameAsValidPartials(known, valid), is(false));
    }

    @Test
    public void shouldReturnFalseWhenValidPartialsListIsEmptyWhenKnownListIsNot() {
        ConfigRepoConfig repo1 = ConfigRepoConfig.createConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig(), "plugin");
        ConfigRepoConfig repo2 = ConfigRepoConfig.createConfigRepoConfig(MaterialConfigsMother.svnMaterialConfig(), "plugin");
        PartialConfig partialConfig1 = PartialConfigMother.withPipeline("p1", new RepoConfigOrigin(repo1, "git_r1"));
        PartialConfig partialConfig2 = PartialConfigMother.withPipeline("p2", new RepoConfigOrigin(repo2, "svn_r1"));
        List<PartialConfig> known = asList(partialConfig1, partialConfig2);
        List<PartialConfig> valid = new ArrayList<>();
        assertThat(dataSource.areKnownPartialsSameAsValidPartials(known, valid), is(false));
    }

    @Test
    public void shouldUpdateConfigWithLastKnownPartials_OnWriteFullConfigWithLock() throws Exception {
        com.thoughtworks.go.server.newsecurity.SessionUtilsHelper.loginAs("loser_boozer");
        BasicCruiseConfig configForEdit = new BasicCruiseConfig();
        MagicalGoConfigXmlLoader.setMd5(configForEdit, "md5");
        FullConfigUpdateCommand updatingCommand = new FullConfigUpdateCommand(new BasicCruiseConfig(), "md5");
        GoConfigHolder configHolder = new GoConfigHolder(new BasicCruiseConfig(), configForEdit);
        List<PartialConfig> lastKnownPartials = mock(List.class);

        when(cachedGoPartials.lastKnownPartials()).thenReturn(lastKnownPartials);
        when(fullConfigSaveNormalFlow.execute(any(FullConfigUpdateCommand.class), anyList(), any(String.class))).
                thenReturn(new GoConfigHolder(new BasicCruiseConfig(), new BasicCruiseConfig()));

        dataSource.writeFullConfigWithLock(updatingCommand, configHolder);

        verify(fullConfigSaveNormalFlow).execute(updatingCommand, lastKnownPartials, "loser_boozer");
    }

    @Test
    public void shouldEnsureMergeFlowWithLastKnownPartialsIfConfigHasChangedBetweenUpdates_OnWriteFullConfigWithLock() throws Exception {
        com.thoughtworks.go.server.newsecurity.SessionUtilsHelper.loginAs("loser_boozer");
        when(systemEnvironment.get(SystemEnvironment.ENABLE_CONFIG_MERGE_FEATURE)).thenReturn(true);
        BasicCruiseConfig configForEdit = new BasicCruiseConfig();
        MagicalGoConfigXmlLoader.setMd5(configForEdit, "new_md5");
        FullConfigUpdateCommand updatingCommand = new FullConfigUpdateCommand(new BasicCruiseConfig(), "old_md5");
        GoConfigHolder configHolder = new GoConfigHolder(new BasicCruiseConfig(), configForEdit);
        List<PartialConfig> lastKnownPartials = mock(List.class);
        when(cachedGoPartials.lastKnownPartials()).thenReturn(lastKnownPartials);
        when(fullConfigSaveMergeFlow.execute(any(FullConfigUpdateCommand.class), anyList(), any(String.class))).
                thenReturn(new GoConfigHolder(new BasicCruiseConfig(), new BasicCruiseConfig()));

        dataSource.writeFullConfigWithLock(updatingCommand, configHolder);

        verify(fullConfigSaveMergeFlow).execute(updatingCommand, lastKnownPartials, "loser_boozer");
    }

    @Test
    public void shouldFallbackOnLastValidPartialsIfUpdateWithLastKnownPartialsFails_OnWriteFullConfigWithLock() throws Exception {
        com.thoughtworks.go.server.newsecurity.SessionUtilsHelper.loginAs("loser_boozer");
        PartialConfig partialConfig1 = PartialConfigMother.withPipeline("p1", new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig(), "plugin"), "git_r1"));
        PartialConfig partialConfig2 = PartialConfigMother.withPipeline("p2", new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(MaterialConfigsMother.svnMaterialConfig(), "plugin"), "svn_r1"));
        List<PartialConfig> known = asList(partialConfig1);
        List<PartialConfig> valid = asList(partialConfig2);

        BasicCruiseConfig configForEdit = new BasicCruiseConfig();
        MagicalGoConfigXmlLoader.setMd5(configForEdit, "md5");
        FullConfigUpdateCommand updatingCommand = new FullConfigUpdateCommand(new BasicCruiseConfig(), "md5");
        GoConfigHolder configHolder = new GoConfigHolder(new BasicCruiseConfig(), configForEdit);
        when(cachedGoPartials.lastKnownPartials()).thenReturn(known);
        when(cachedGoPartials.lastValidPartials()).thenReturn(valid);
        when(fullConfigSaveNormalFlow.execute(updatingCommand, known, "loser_boozer")).
                thenThrow(new Exception());
        when(fullConfigSaveNormalFlow.execute(updatingCommand, valid, "loser_boozer")).
                thenReturn(new GoConfigHolder(new BasicCruiseConfig(), new BasicCruiseConfig()));

        dataSource.writeFullConfigWithLock(updatingCommand, configHolder);

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

        when(cachedGoPartials.lastKnownPartials()).thenReturn(known);
        when(fullConfigSaveNormalFlow.execute(updatingCommand, known, "loser_boozer")).
                thenThrow(new GoConfigInvalidException(configForEdit, "error"));

        dataSource.writeFullConfigWithLock(updatingCommand, configHolder);
    }

    @Test(expected = RuntimeException.class)
    public void shouldNotRetryConfigUpdateIfLastKnownAndValidPartialsAreSame_OnWriteFullConfigWithLock() throws Exception {
        PartialConfig partialConfig1 = PartialConfigMother.withPipeline("p1", new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig(), "plugin"), "git_r1"));
        List<PartialConfig> known = asList(partialConfig1);
        List<PartialConfig> valid = asList(partialConfig1);

        BasicCruiseConfig configForEdit = new BasicCruiseConfig();
        MagicalGoConfigXmlLoader.setMd5(configForEdit, "md5");
        FullConfigUpdateCommand updatingCommand = new FullConfigUpdateCommand(new BasicCruiseConfig(), "md5");
        GoConfigHolder configHolder = new GoConfigHolder(new BasicCruiseConfig(), configForEdit);

        when(cachedGoPartials.lastKnownPartials()).thenReturn(known);
        when(cachedGoPartials.lastValidPartials()).thenReturn(valid);
        when(fullConfigSaveNormalFlow.execute(updatingCommand, known, "loser_boozer")).
                thenThrow(new GoConfigInvalidException(configForEdit, "error"));

        dataSource.writeFullConfigWithLock(updatingCommand, configHolder);
    }

    @Test(expected = RuntimeException.class)
    public void shouldErrorOutOnTryingToMergeConfigsIfConfigMergeFeatureIsDisabled_OnWriteFullConfigWithLock() throws Exception {
        BasicCruiseConfig configForEdit = new BasicCruiseConfig();
        MagicalGoConfigXmlLoader.setMd5(configForEdit, "new_md5");
        FullConfigUpdateCommand updatingCommand = new FullConfigUpdateCommand(new BasicCruiseConfig(), "old_md5");
        GoConfigHolder configHolder = new GoConfigHolder(new BasicCruiseConfig(), configForEdit);
        List<PartialConfig> lastKnownPartials = new ArrayList<>();
        when(cachedGoPartials.lastKnownPartials()).thenReturn(lastKnownPartials);
        systemEnvironment.set(SystemEnvironment.ENABLE_CONFIG_MERGE_FEATURE, false);

        dataSource.writeFullConfigWithLock(updatingCommand, configHolder);

        verify(fullConfigSaveMergeFlow, never()).execute(any(FullConfigUpdateCommand.class), anyList(), any(String.class));
        verify(fullConfigSaveNormalFlow, never()).execute(any(FullConfigUpdateCommand.class), anyList(), any(String.class));
    }

    @Test
    public void shouldUpdateAndReloadConfigUsingFullSaveNormalFlowWithLastKnownPartials_onLoad() throws Exception {
        PartialConfig partialConfig1 = PartialConfigMother.withPipeline("p1", new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig(), "plugin"), "git_r1"));
        List<PartialConfig> lastKnownPartials = asList(partialConfig1);
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        MagicalGoConfigXmlLoader.setMd5(cruiseConfig, "md5");
        GoConfigHolder goConfigHolder = new GoConfigHolder(cruiseConfig, cruiseConfig);
        ArgumentCaptor<FullConfigUpdateCommand> commandArgumentCaptor = ArgumentCaptor.forClass(FullConfigUpdateCommand.class);
        ArgumentCaptor<List> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

        when(systemEnvironment.getCruiseConfigFile()).thenReturn("");
        when(goConfigFileReader.fileLocation()).thenReturn(new File(""));
        when(goConfigFileReader.configXml()).thenReturn("config_xml");
        when(xmlLoader.deserializeConfig("config_xml")).thenReturn(cruiseConfig);
        when(cachedGoPartials.lastKnownPartials()).thenReturn(lastKnownPartials);
        when(fullConfigSaveNormalFlow.execute(commandArgumentCaptor.capture(), listArgumentCaptor.capture(), stringArgumentCaptor.capture())).thenReturn(goConfigHolder);

        dataSource.reloadEveryTime();
        GoConfigHolder configHolder = dataSource.load();

        assertThat(configHolder, is(goConfigHolder));
        assertThat(commandArgumentCaptor.getValue().configForEdit(), is(cruiseConfig));
        assertThat(listArgumentCaptor.getValue(), is(lastKnownPartials));
        assertThat(stringArgumentCaptor.getValue(), is("Filesystem"));
    }

    @Test
    public void shouldReloadConfigUsingFullSaveNormalFlowWithLastValidPartialsIfUpdatingWithLastKnownPartialsFails_onLoad() throws Exception {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        PartialConfigMother.withPipeline("P1");
        List lastKnownPartials = Arrays.asList(PartialConfigMother.withPipeline("P1"));
        List lastValidPartials = Arrays.asList(PartialConfigMother.withPipeline("P2"), PartialConfigMother.withPipeline("P3"));
        GoConfigHolder goConfigHolder = new GoConfigHolder(cruiseConfig, cruiseConfig);

        ArgumentCaptor<FullConfigUpdateCommand> commandArgumentCaptor = ArgumentCaptor.forClass(FullConfigUpdateCommand.class);
        ArgumentCaptor<List> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

        when(systemEnvironment.getCruiseConfigFile()).thenReturn("");
        when(goConfigFileReader.fileLocation()).thenReturn(new File(""));
        when(goConfigFileReader.configXml()).thenReturn("config_xml");
        when(xmlLoader.deserializeConfig("config_xml")).thenReturn(cruiseConfig);
        when(cachedGoPartials.lastKnownPartials()).thenReturn(lastKnownPartials);
        when(cachedGoPartials.lastValidPartials()).thenReturn(lastValidPartials);
        when(fullConfigSaveNormalFlow.execute(commandArgumentCaptor.capture(), listArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenThrow(new GoConfigInvalidException(null, Collections.emptyList())).thenReturn(goConfigHolder);

        dataSource.reloadEveryTime();
        GoConfigHolder configHolder = dataSource.load();

        assertThat(configHolder, is(goConfigHolder));
        assertThat(commandArgumentCaptor.getValue().configForEdit(), is(cruiseConfig));
        assertThat(listArgumentCaptor.getValue(), is(lastValidPartials));
        assertThat(stringArgumentCaptor.getValue(), is("Filesystem"));
    }
}
