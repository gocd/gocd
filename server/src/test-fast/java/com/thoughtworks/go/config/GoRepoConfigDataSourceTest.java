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
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.PartialConfigMother;
import com.thoughtworks.go.server.service.ConfigRepoService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GoRepoConfigDataSourceTest {
    private GoConfigPluginService configPluginService;
    private GoConfigWatchList configWatchList;
    private PartialConfigProvider plugin;

    private GoRepoConfigDataSource repoConfigDataSource;

    private BasicCruiseConfig cruiseConfig;
    private ServerHealthService serverHealthService;
    private ConfigRepoService configRepoService;

    File folder = new File("dir");
    private GoConfigService goConfigService;

    @Before
    public void setUp() {
        serverHealthService = new ServerHealthService();
        configPluginService = mock(GoConfigPluginService.class);
        configRepoService = mock(ConfigRepoService.class);
        plugin = mock(PartialConfigProvider.class);
        goConfigService = mock(GoConfigService.class);
        when(configPluginService.partialConfigProviderFor(any(ConfigRepoConfig.class))).thenReturn(plugin);

        cruiseConfig = new BasicCruiseConfig();
        CachedGoConfig cachedGoConfig = mock(CachedGoConfig.class);
        when(cachedGoConfig.currentConfig()).thenReturn(cruiseConfig);

        configWatchList = new GoConfigWatchList(cachedGoConfig, mock(GoConfigService.class));
        repoConfigDataSource = new GoRepoConfigDataSource(configWatchList, configPluginService, serverHealthService, configRepoService, goConfigService);

        ScmMaterialConfig material = git("http://my.git");
        ConfigRepoConfig configRepoConfig = ConfigRepoConfig.createConfigRepoConfig(material, "myplugin", "id");
        when(configRepoService.findByFingerprint(anyString())).thenReturn(configRepoConfig);
    }

    @Test
    public void shouldCallPluginLoadOnCheckout_WhenMaterialInWatchList() {
        ScmMaterialConfig material = git("http://my.git");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(ConfigRepoConfig.createConfigRepoConfig(material, "myplugin", "id")));
        configWatchList.onConfigChange(cruiseConfig);

        repoConfigDataSource.onCheckoutComplete(material, folder, getModificationFor("7a8f"));

        verify(plugin, times(1)).load(eq(folder), any(PartialConfigLoadContext.class));
    }

    @Test
    public void shouldAssignConfigOrigin() throws Exception {
        ScmMaterialConfig material = git("http://my.git");
        ConfigRepoConfig configRepo = ConfigRepoConfig.createConfigRepoConfig(material, "myplugin", "id");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepo));
        configWatchList.onConfigChange(cruiseConfig);

        repoConfigDataSource.onCheckoutComplete(material, folder, getModificationFor("7a8f"));

        PartialConfig partialConfig = repoConfigDataSource.latestPartialConfigForMaterial(material);
        assertNotNull(partialConfig.getOrigin());
        RepoConfigOrigin repoConfigOrigin = new RepoConfigOrigin(configRepo, "7a8f");
        assertThat(partialConfig.getOrigin(), is(repoConfigOrigin));
    }

    @Test
    public void shouldAssignConfigOriginInPipelines() throws Exception {
        ScmMaterialConfig material = git("http://my.git");
        ConfigRepoConfig configRepo = ConfigRepoConfig.createConfigRepoConfig(material, "myplugin", "id");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepo));
        configWatchList.onConfigChange(cruiseConfig);

        when(plugin.load(any(File.class), any(PartialConfigLoadContext.class)))
                .thenReturn(PartialConfigMother.withPipeline("pipe1"));

        repoConfigDataSource.onCheckoutComplete(material, folder, getModificationFor("7a8f"));

        PartialConfig partialConfig = repoConfigDataSource.latestPartialConfigForMaterial(material);
        RepoConfigOrigin repoConfigOrigin = new RepoConfigOrigin(configRepo, "7a8f");

        assertNotNull(partialConfig.getOrigin());
        assertThat(partialConfig.getOrigin(), is(repoConfigOrigin));

        PipelineConfig pipe = partialConfig.getGroups().get(0).get(0);
        assertThat(pipe.getOrigin(), is(repoConfigOrigin));
    }

    @Test
    public void shouldAssignConfigOriginInEnvironments() throws Exception {
        ScmMaterialConfig material = git("http://my.git");
        ConfigRepoConfig configRepo = ConfigRepoConfig.createConfigRepoConfig(material, "myplugin", "id");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepo));
        configWatchList.onConfigChange(cruiseConfig);

        when(plugin.load(any(File.class), any(PartialConfigLoadContext.class)))
                .thenReturn(PartialConfigMother.withEnvironment("UAT"));

        repoConfigDataSource.onCheckoutComplete(material, folder, getModificationFor("7a8f"));

        PartialConfig partialConfig = repoConfigDataSource.latestPartialConfigForMaterial(material);
        RepoConfigOrigin repoConfigOrigin = new RepoConfigOrigin(configRepo, "7a8f");

        assertNotNull(partialConfig.getOrigin());
        assertThat(partialConfig.getOrigin(), is(repoConfigOrigin));

        EnvironmentConfig environmentConfig = partialConfig.getEnvironments().get(0);
        assertThat(environmentConfig.getOrigin(), is(repoConfigOrigin));
    }


    @Test
    public void shouldProvideParseContextWhenCallingPlugin() throws Exception {
        ScmMaterialConfig material = git("http://my.git");
        ConfigRepoConfig repoConfig = ConfigRepoConfig.createConfigRepoConfig(material, "myplugin", "id");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(repoConfig));
        configWatchList.onConfigChange(cruiseConfig);

        when(configPluginService.partialConfigProviderFor(any(ConfigRepoConfig.class))).thenReturn(plugin);

        repoConfigDataSource.onCheckoutComplete(material, folder, getModificationFor("7a8f"));

        verify(plugin, times(1)).load(eq(folder), notNull(PartialConfigLoadContext.class));
    }

    @Test
    public void shouldProvideConfigurationInParseContextWhenCallingPlugin() throws Exception {
        ScmMaterialConfig material = git("http://my.git");
        ConfigRepoConfig repoConfig = ConfigRepoConfig.createConfigRepoConfig(material, "myplugin", "id");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(repoConfig));
        configWatchList.onConfigChange(cruiseConfig);

        repoConfig.getConfiguration().addNewConfigurationWithValue("key", "value", false);

        plugin = new AssertingConfigPlugin(repoConfig.getConfiguration());
        when(configPluginService.partialConfigProviderFor(any(ConfigRepoConfig.class))).thenReturn(plugin);

        repoConfigDataSource.onCheckoutComplete(material, folder, getModificationFor("7a8f"));
    }

    private class AssertingConfigPlugin implements PartialConfigProvider {
        private Configuration configuration;

        public AssertingConfigPlugin(Configuration configuration) {

            this.configuration = configuration;
        }

        @Override
        public PartialConfig load(File configRepoCheckoutDirectory, PartialConfigLoadContext context) {
            Assert.assertThat(context.configuration(), is(configuration));
            Assert.assertThat(context.configuration().getProperty("key").getValue(), is("value"));
            return mock(PartialConfig.class);
        }

        @Override
        public String displayName() {
            return "Assert config provider";
        }
    }

    @Test
    public void shouldNotCallPluginLoadOnCheckout_WhenMaterialNotInWatchList() throws Exception {
        ScmMaterialConfig material = git("http://my.git");

        repoConfigDataSource.onCheckoutComplete(material, folder, getModificationFor("7a8f"));

        verify(plugin, times(0)).load(eq(folder), any(PartialConfigLoadContext.class));
    }

    @Test
    public void shouldReturnLatestPartialConfigForMaterial_WhenPartialExists() throws Exception {
        ScmMaterialConfig material = git("http://my.git");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(ConfigRepoConfig.createConfigRepoConfig(material, "myplugin", "id")));
        configWatchList.onConfigChange(cruiseConfig);

        repoConfigDataSource.onCheckoutComplete(material, folder, getModificationFor("7a8f"));

        assertNotNull(repoConfigDataSource.latestPartialConfigForMaterial(material));
    }

    @Test
    public void shouldThrowWhenGettingLatestPartialConfig_WhenPluginHasFailed() throws Exception {
        // use broken plugin now
        when(configPluginService.partialConfigProviderFor(any(ConfigRepoConfig.class)))
                .thenReturn(new BrokenConfigPlugin());

        ScmMaterialConfig material = git("http://my.git");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(ConfigRepoConfig.createConfigRepoConfig(material, "myplugin", "id")));
        configWatchList.onConfigChange(cruiseConfig);

        repoConfigDataSource.onCheckoutComplete(material, folder, getModificationFor("7a8f"));

        assertTrue(repoConfigDataSource.latestParseHasFailedForMaterial(material));

        try {
            repoConfigDataSource.latestPartialConfigForMaterial(material);
        } catch (BrokenConfigPluginException ex) {
            return;
        }
        fail("should have thrown BrokenConfigPluginException");
    }

    @Test
    public void shouldSetErrorHealthState_AtConfigRepoScope_WhenPluginHasThrown() {
        // use broken plugin now
        when(configPluginService.partialConfigProviderFor(any(ConfigRepoConfig.class)))
                .thenReturn(new BrokenConfigPlugin());

        ScmMaterialConfig material = git("http://my.git");
        ConfigRepoConfig configRepoConfig = ConfigRepoConfig.createConfigRepoConfig(material, "myplugin", "id");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepoConfig));
        configWatchList.onConfigChange(cruiseConfig);

        repoConfigDataSource.onCheckoutComplete(material, folder, getModificationFor("7a8f"));

        assertTrue(repoConfigDataSource.latestParseHasFailedForMaterial(material));

        assertFalse(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(configRepoConfig)).isEmpty());
    }

    @Test
    public void shouldSetOKHealthState_AtConfigRepoScope_WhenPluginHasParsed() {
        ScmMaterialConfig material = git("http://my.git");
        ConfigRepoConfig configRepoConfig = ConfigRepoConfig.createConfigRepoConfig(material, "myplugin", "id");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepoConfig));
        configWatchList.onConfigChange(cruiseConfig);

        // set error state to simulate previously failed parse
        HealthStateScope scope = HealthStateScope.forPartialConfigRepo(configRepoConfig);
        serverHealthService.update(ServerHealthState.error("Parse failed", "Bad config format", HealthStateType.general(scope)));
        // verify error health
        assertFalse(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(configRepoConfig)).isEmpty());

        // now this should fix health
        repoConfigDataSource.onCheckoutComplete(material, folder, getModificationFor("7a8f"));

        assertTrue(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(configRepoConfig)).isEmpty());
    }

    private class BrokenConfigPlugin implements PartialConfigProvider {
        @Override
        public PartialConfig load(File configRepoCheckoutDirectory, PartialConfigLoadContext context) {
            throw new BrokenConfigPluginException();
        }

        @Override
        public String displayName() {
            return "Broken Test Provider";
        }
    }

    private class BrokenConfigPluginException extends RuntimeException {
    }

    @Test
    public void shouldThrowWhenGettingLatestPartialConfig_WhenInitializingPluginHasFailed() throws Exception {
        when(configPluginService.partialConfigProviderFor(any(ConfigRepoConfig.class)))
                .thenThrow(new RuntimeException("Failed to initialize plugin"));

        ScmMaterialConfig material = git("http://my.git");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(ConfigRepoConfig.createConfigRepoConfig(material, "myplugin", "id")));
        configWatchList.onConfigChange(cruiseConfig);

        repoConfigDataSource.onCheckoutComplete(material, folder, getModificationFor("7a8f"));

        assertTrue(repoConfigDataSource.latestParseHasFailedForMaterial(material));

        try {
            repoConfigDataSource.latestPartialConfigForMaterial(material);
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage(), is("Failed to initialize plugin"));
        }
    }

    @Test
    public void shouldRemovePartialsWhenRemovedFromWatchList() throws Exception {
        ScmMaterialConfig material = git("http://my.git");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(ConfigRepoConfig.createConfigRepoConfig(material, "myplugin", "id")));
        configWatchList.onConfigChange(cruiseConfig);

        repoConfigDataSource.onCheckoutComplete(material, folder, getModificationFor("7a8f"));
        assertNotNull(repoConfigDataSource.latestPartialConfigForMaterial(material));

        // we change current configuration
        ScmMaterialConfig othermaterial = git("http://myother.git");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(ConfigRepoConfig.createConfigRepoConfig(othermaterial, "myplugin", "id")));
        configWatchList.onConfigChange(cruiseConfig);

        assertNull(repoConfigDataSource.latestPartialConfigForMaterial(material));
    }

    @Test
    public void shouldListenForConfigRepoListChanged() {
        assertTrue(configWatchList.hasListener(repoConfigDataSource));
    }

    @Test
    public void shouldMaintainAListOfConfigReposWhichHaveChangedSinceLastParse() {
        GitMaterialConfig material = git("http://my.git");
        ConfigRepoConfig configRepoConfig = ConfigRepoConfig.createConfigRepoConfig(material, "myplugin", "id");
        GoConfigWatchList goConfigWatchList = mock(GoConfigWatchList.class);
        repoConfigDataSource = new GoRepoConfigDataSource(goConfigWatchList, configPluginService, serverHealthService, configRepoService, goConfigService);

        when(goConfigWatchList.getConfigRepoForMaterial(material)).thenReturn(configRepoConfig);

        repoConfigDataSource.onConfigRepoConfigChange(configRepoConfig);

        assertTrue(repoConfigDataSource.hasConfigRepoConfigChangedSinceLastUpdate(material));
    }

    @Test
    public void onParseOfConfigRepo_shouldUpdateTheListOfConfigReposWhichHaveChanged() {
        GitMaterialConfig material = git("http://my.git");
        ConfigRepoConfig configRepoConfig = ConfigRepoConfig.createConfigRepoConfig(material, "myplugin", "id");
        GoConfigWatchList goConfigWatchList = mock(GoConfigWatchList.class);
        repoConfigDataSource = new GoRepoConfigDataSource(goConfigWatchList, configPluginService, serverHealthService, configRepoService, goConfigService);

        when(goConfigWatchList.getConfigRepoForMaterial(material)).thenReturn(configRepoConfig);
        when(goConfigWatchList.hasConfigRepoWithFingerprint(material.getFingerprint())).thenReturn(true);

        repoConfigDataSource.onConfigRepoConfigChange(configRepoConfig);

        assertTrue(repoConfigDataSource.hasConfigRepoConfigChangedSinceLastUpdate(material));

        repoConfigDataSource.onCheckoutComplete(material, folder, getModificationFor("7a8f"));

        assertFalse(repoConfigDataSource.hasConfigRepoConfigChangedSinceLastUpdate(material));
    }

    private Modification getModificationFor(String revision) {
        Modification modification = new Modification();
        modification.setRevision(revision);
        return modification;
    }
}
