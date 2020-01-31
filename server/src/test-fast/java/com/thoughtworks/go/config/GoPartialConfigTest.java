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

import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.update.PartialConfigUpdateCommand;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.PartialConfigMother;
import com.thoughtworks.go.server.service.ConfigRepoService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class GoPartialConfigTest {

    File folder = new File("dir");
    private GoConfigPluginService configPluginService;
    private GoConfigWatchList configWatchList;
    private PartialConfigProvider plugin;
    private GoRepoConfigDataSource repoConfigDataSource;
    private BasicCruiseConfig cruiseConfig;
    private GoPartialConfig partialConfig;
    private CachedGoConfig cachedGoConfig;
    private ConfigRepoConfig configRepoConfig;
    private CachedGoPartials cachedGoPartials;
    private GoConfigService goConfigService;
    private ServerHealthService serverHealthService;
    private PartialConfigUpdateCommand updateCommand;
    private ConfigRepoService configRepoService;

    @Before
    public void setUp() {
        configRepoService = mock(ConfigRepoService.class);
        serverHealthService = mock(ServerHealthService.class);
        configPluginService = mock(GoConfigPluginService.class);
        plugin = mock(PartialConfigProvider.class);
        when(configPluginService.partialConfigProviderFor(any(ConfigRepoConfig.class))).thenReturn(plugin);

        cruiseConfig = new BasicCruiseConfig();
        configRepoConfig = ConfigRepoConfig.createConfigRepoConfig(git("url"), "plugin");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepoConfig));
        cachedGoConfig = mock(CachedGoConfig.class);
        when(cachedGoConfig.currentConfig()).thenReturn(cruiseConfig);

        configWatchList = new GoConfigWatchList(cachedGoConfig, mock(GoConfigService.class));
        goConfigService = mock(GoConfigService.class);
        repoConfigDataSource = new GoRepoConfigDataSource(configWatchList, configPluginService, serverHealthService, configRepoService, goConfigService);
        cachedGoPartials = new CachedGoPartials(serverHealthService);
        serverHealthService = mock(ServerHealthService.class);

        updateCommand = null;
        partialConfig = new GoPartialConfig(repoConfigDataSource, configWatchList, goConfigService, cachedGoPartials, serverHealthService) {
            @Override
            public PartialConfigUpdateCommand buildUpdateCommand(PartialConfig partial, String fingerprint) {
                if (null == updateCommand) {
                    return super.buildUpdateCommand(partial, fingerprint);
                }
                return updateCommand;
            }
        };

        when(configRepoService.findByFingerprint(anyString())).thenReturn(configRepoConfig);
    }

    @Test
    public void mergeAppliesUpdateToConfig() {
        updateCommand = mock(PartialConfigUpdateCommand.class);
        PartialConfig partial = new PartialConfig();

        partialConfig.merge(partial, "finger", cruiseConfig);
        verify(updateCommand, times(1)).update(cruiseConfig);
    }

    @Test
    public void shouldReturnEmptyListWhenWatchListEmpty() {
        assertThat(partialConfig.lastPartials().isEmpty(), is(true));
    }

    @Test
    public void shouldReturnEmptyListWhenNothingParsedYet_AndWatchListNotEmpty() {
        setOneConfigRepo();

        assertThat(partialConfig.lastPartials().isEmpty(), is(true));
    }

    private ScmMaterialConfig setOneConfigRepo() {
        ScmMaterialConfig material = git("http://my.git");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(ConfigRepoConfig.createConfigRepoConfig(material, "myplugin")));
        configWatchList.onConfigChange(cruiseConfig);
        return material;
    }

    @Test
    public void shouldReturnLatestPartialAfterCheckout_AndWatchListNotEmpty() throws Exception {
        ScmMaterialConfig material = setOneConfigRepo();

        PartialConfig part = new PartialConfig();
        when(plugin.load(any(File.class), any(PartialConfigLoadContext.class))).thenReturn(part);

        repoConfigDataSource.onCheckoutComplete(material, folder, getModificationFor("7a8f"));

        assertThat(partialConfig.lastPartials().size(), is(1));
        assertThat(partialConfig.lastPartials().get(0), is(part));
    }

    @Test
    public void shouldReturnEmptyList_WhenFirstParsingFailed() {
        ScmMaterialConfig material = setOneConfigRepo();

        PartialConfig part = new PartialConfig();
        when(plugin.load(any(File.class), any(PartialConfigLoadContext.class)))
                .thenThrow(new RuntimeException("Failed parsing"));

        repoConfigDataSource.onCheckoutComplete(material, folder, getModificationFor("7a8f"));

        assertThat(repoConfigDataSource.latestParseHasFailedForMaterial(material), is(true));

        assertThat(partialConfig.lastPartials().size(), is(0));
    }

    @Test
    public void shouldReturnFirstPartial_WhenFirstParsedSucceed_ButSecondFailed() throws Exception {
        ScmMaterialConfig material = setOneConfigRepo();

        PartialConfig part = new PartialConfig();
        when(plugin.load(any(File.class), any(PartialConfigLoadContext.class))).thenReturn(part);

        repoConfigDataSource.onCheckoutComplete(material, folder, getModificationFor("7a8f"));

        when(plugin.load(any(File.class), any(PartialConfigLoadContext.class)))
                .thenThrow(new RuntimeException("Failed parsing"));
        repoConfigDataSource.onCheckoutComplete(material, folder, getModificationFor("6354"));

        assertThat(repoConfigDataSource.latestParseHasFailedForMaterial(material), is(true));

        assertThat(partialConfig.lastPartials().size(), is(1));
        assertThat(partialConfig.lastPartials().get(0), is(part));
    }

    @Test
    public void shouldRemovePartialWhenNoLongerInWatchList() throws Exception {
        ScmMaterialConfig material = setOneConfigRepo();

        PartialConfig part = new PartialConfig();
        when(plugin.load(any(File.class), any(PartialConfigLoadContext.class))).thenReturn(part);

        repoConfigDataSource.onCheckoutComplete(material, folder, getModificationFor("7a8f"));

        assertThat(partialConfig.lastPartials().size(), is(1));
        assertThat(partialConfig.lastPartials().get(0), is(part));

        // we change current configuration
        ScmMaterialConfig othermaterial = git("http://myother.git");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(ConfigRepoConfig.createConfigRepoConfig(othermaterial, "myplugin")));
        configWatchList.onConfigChange(cruiseConfig);

        assertThat(partialConfig.lastPartials().size(), is(0));
    }

    @Test
    public void shouldListenForConfigRepoListChanges() {
        assertTrue(repoConfigDataSource.hasListener(partialConfig));
    }

    @Test
    public void shouldListenForCompletedParsing() {
        assertTrue(configWatchList.hasListener(partialConfig));
    }

    @Test
    public void shouldNotifyListenersAfterUpdatingMapOfLatestValidConfig() {
        ScmMaterialConfig material = setOneConfigRepo();

        PartialConfig part = new PartialConfig();
        when(plugin.load(any(File.class), any(PartialConfigLoadContext.class))).thenReturn(part);

        PartialConfigUpdateCompletedListener listener = mock(PartialConfigUpdateCompletedListener.class);
        repoConfigDataSource.registerListener(listener);

        repoConfigDataSource.onCheckoutComplete(material, folder, getModificationFor("7a8f"));
        verify(listener, times(1)).onSuccessPartialConfig(any(ConfigRepoConfig.class), any(PartialConfig.class));
    }

    @Test
    public void shouldMergeRemoteGroupToMain() {
        when(goConfigService.updateConfig(any(UpdateConfigCommand.class))).thenAnswer(invocationOnMock -> {
            UpdateConfigCommand command = (UpdateConfigCommand) invocationOnMock.getArguments()[0];
            command.update(cruiseConfig);
            return cruiseConfig;
        });
        partialConfig.onSuccessPartialConfig(configRepoConfig, PartialConfigMother.withPipeline("p1"));
        assertThat(cruiseConfig.getPartials().size(), is(1));
        assertThat(cruiseConfig.getPartials().get(0).getGroups().first().getGroup(), is("group"));
    }

    @Test
    public void shouldMergeRemoteEnvironmentToMain() {
        when(goConfigService.updateConfig(any(UpdateConfigCommand.class))).thenAnswer(invocationOnMock -> {
            UpdateConfigCommand command = (UpdateConfigCommand) invocationOnMock.getArguments()[0];
            command.update(cruiseConfig);
            return cruiseConfig;
        });
        partialConfig.onSuccessPartialConfig(configRepoConfig, PartialConfigMother.withEnvironment("env1"));
        assertThat(cruiseConfig.getPartials().size(), is(1));
        assertThat(cruiseConfig.getPartials().get(0).getEnvironments().first().name(), is(new CaseInsensitiveString("env1")));
    }

    private Modification getModificationFor(String revision) {
        Modification modification = new Modification();
        modification.setRevision(revision);
        return modification;
    }
}
