/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.remote.*;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.helper.PartialConfigMother;
import com.thoughtworks.go.serverhealth.*;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GoRepoConfigDataSourceTest {
    private GoConfigPluginService configPluginService;
    private GoConfigWatchList configWatchList;
    private PartialConfigProvider plugin;

    private GoRepoConfigDataSource repoConfigDataSource;

    private BasicCruiseConfig cruiseConfig ;
    private ServerHealthService serverHealthService;

    File folder = new File("dir");

    @Before
    public void setUp()
    {
        serverHealthService = new ServerHealthService();
        configPluginService = mock(GoConfigPluginService.class);
        plugin = mock(PartialConfigProvider.class);
        when(configPluginService.partialConfigProviderFor(any(ConfigRepoConfig.class))).thenReturn(plugin);

        cruiseConfig = new BasicCruiseConfig();
        CachedGoConfig cachedGoConfig = mock(CachedGoConfig.class);
        when(cachedGoConfig.currentConfig()).thenReturn(cruiseConfig);

        configWatchList = new GoConfigWatchList(cachedGoConfig);

        repoConfigDataSource = new GoRepoConfigDataSource(configWatchList,configPluginService,serverHealthService);
    }


    @Test
    public void shouldCallPluginLoadOnCheckout_WhenMaterialInWatchList()
    {
        ScmMaterialConfig material = new GitMaterialConfig("http://my.git");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(new ConfigRepoConfig(material,"myplugin")));
        configWatchList.onConfigChange(cruiseConfig);

        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");

        verify(plugin,times(1)).load(eq(folder), any(PartialConfigLoadContext.class));
    }

    @Test
    public void shouldAssignConfigOrigin() throws Exception
    {
        ScmMaterialConfig material = new GitMaterialConfig("http://my.git");
        ConfigRepoConfig configRepo = new ConfigRepoConfig(material, "myplugin");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepo));
        configWatchList.onConfigChange(cruiseConfig);

        repoConfigDataSource.onCheckoutComplete(material, folder, "7a8f");

        PartialConfig partialConfig = repoConfigDataSource.latestPartialConfigForMaterial(material);
        assertNotNull(partialConfig.getOrigin());
        RepoConfigOrigin repoConfigOrigin = new RepoConfigOrigin(configRepo,"7a8f");
        assertThat(partialConfig.getOrigin(), Is.<ConfigOrigin>is(repoConfigOrigin));
    }
    @Test
    public void shouldAssignConfigOriginInPipelines() throws Exception
    {
        ScmMaterialConfig material = new GitMaterialConfig("http://my.git");
        ConfigRepoConfig configRepo = new ConfigRepoConfig(material, "myplugin");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepo));
        configWatchList.onConfigChange(cruiseConfig);

        when(plugin.load(any(File.class), any(PartialConfigLoadContext.class)))
                .thenReturn(PartialConfigMother.withPipeline("pipe1"));

        repoConfigDataSource.onCheckoutComplete(material, folder, "7a8f");

        PartialConfig partialConfig = repoConfigDataSource.latestPartialConfigForMaterial(material);
        RepoConfigOrigin repoConfigOrigin = new RepoConfigOrigin(configRepo,"7a8f");

        assertNotNull(partialConfig.getOrigin());
        assertThat(partialConfig.getOrigin(), Is.<ConfigOrigin>is(repoConfigOrigin));

        PipelineConfig pipe = partialConfig.getGroups().get(0).get(0);
        assertThat(pipe.getOrigin(), Is.<ConfigOrigin>is(repoConfigOrigin));
    }

    @Test
    public void shouldAssignConfigOriginInEnvironments() throws Exception
    {
        ScmMaterialConfig material = new GitMaterialConfig("http://my.git");
        ConfigRepoConfig configRepo = new ConfigRepoConfig(material, "myplugin");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepo));
        configWatchList.onConfigChange(cruiseConfig);

        when(plugin.load(any(File.class), any(PartialConfigLoadContext.class)))
                .thenReturn(PartialConfigMother.withEnvironment("UAT"));

        repoConfigDataSource.onCheckoutComplete(material, folder, "7a8f");

        PartialConfig partialConfig = repoConfigDataSource.latestPartialConfigForMaterial(material);
        RepoConfigOrigin repoConfigOrigin = new RepoConfigOrigin(configRepo,"7a8f");

        assertNotNull(partialConfig.getOrigin());
        assertThat(partialConfig.getOrigin(), Is.<ConfigOrigin>is(repoConfigOrigin));

        EnvironmentConfig environmentConfig = partialConfig.getEnvironments().get(0);
        assertThat(environmentConfig.getOrigin(), Is.<ConfigOrigin>is(repoConfigOrigin));
    }


    @Test
    public void shouldProvideParseContextWhenCallingPlugin() throws Exception
    {
        ScmMaterialConfig material = new GitMaterialConfig("http://my.git");
        ConfigRepoConfig repoConfig = new ConfigRepoConfig(material, "myplugin");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(repoConfig));
        configWatchList.onConfigChange(cruiseConfig);

        when(configPluginService.partialConfigProviderFor(any(ConfigRepoConfig.class))).thenReturn(plugin);

        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");

        verify(plugin,times(1)).load(eq(folder), notNull(PartialConfigLoadContext.class));
    }

    @Test
    public void shouldProvideConfigurationInParseContextWhenCallingPlugin() throws Exception
    {
        ScmMaterialConfig material = new GitMaterialConfig("http://my.git");
        ConfigRepoConfig repoConfig = new ConfigRepoConfig(material, "myplugin");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(repoConfig));
        configWatchList.onConfigChange(cruiseConfig);

        repoConfig.getConfiguration().addNewConfigurationWithValue("key","value",false);

        plugin = new AssertingConfigPlugin(repoConfig.getConfiguration());
        when(configPluginService.partialConfigProviderFor(any(ConfigRepoConfig.class))).thenReturn(plugin);

        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");
    }

    private class AssertingConfigPlugin implements PartialConfigProvider
    {
        private Configuration configuration;

        public AssertingConfigPlugin(Configuration configuration) {

            this.configuration = configuration;
        }

        @Override
        public PartialConfig load(File configRepoCheckoutDirectory, PartialConfigLoadContext context) {
            Assert.assertThat(context.configuration(),is(configuration));
            Assert.assertThat(context.configuration().getProperty("key").getValue(),is("value"));
            return mock(PartialConfig.class);
        }

        @Override
        public String displayName() {
            return "Assert config provider";
        }
    }

    @Test
    public void shouldNotCallPluginLoadOnCheckout_WhenMaterialNotInWatchList() throws Exception
    {
        ScmMaterialConfig material = new GitMaterialConfig("http://my.git");

        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");

        verify(plugin,times(0)).load(eq(folder), any(PartialConfigLoadContext.class));
    }

    @Test
    public void shouldReturnLatestPartialConfigForMaterial_WhenPartialExists() throws  Exception
    {
        ScmMaterialConfig material = new GitMaterialConfig("http://my.git");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(new ConfigRepoConfig(material,"myplugin")));
        configWatchList.onConfigChange(cruiseConfig);

        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");

        assertNotNull(repoConfigDataSource.latestPartialConfigForMaterial(material));
    }

    @Test
    public void shouldThrowWhenGettingLatestPartialConfig_WhenPluginHasFailed() throws  Exception
    {
        // use broken plugin now
        when(configPluginService.partialConfigProviderFor(any(ConfigRepoConfig.class)))
                .thenReturn(new BrokenConfigPlugin());

        ScmMaterialConfig material = new GitMaterialConfig("http://my.git");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(new ConfigRepoConfig(material,"myplugin")));
        configWatchList.onConfigChange(cruiseConfig);

        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");

        assertTrue(repoConfigDataSource.latestParseHasFailedForMaterial(material));

        try
        {
            repoConfigDataSource.latestPartialConfigForMaterial(material);
        }
        catch (BrokenConfigPluginException ex)
        {
            return;
        }
        fail("should have thrown BrokenConfigPluginException");
    }

    @Test
    public void shouldSetErrorHealthState_AtConfigRepoScope_WhenPluginHasThrown()
    {
        // use broken plugin now
        when(configPluginService.partialConfigProviderFor(any(ConfigRepoConfig.class)))
                .thenReturn(new BrokenConfigPlugin());

        ScmMaterialConfig material = new GitMaterialConfig("http://my.git");
        ConfigRepoConfig configRepoConfig = new ConfigRepoConfig(material, "myplugin");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepoConfig));
        configWatchList.onConfigChange(cruiseConfig);

        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");

        assertTrue(repoConfigDataSource.latestParseHasFailedForMaterial(material));

        assertFalse(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(configRepoConfig)).isEmpty());
    }
    @Test
    public void shouldSetOKHealthState_AtConfigRepoScope_WhenPluginHasParsed()
    {
        ScmMaterialConfig material = new GitMaterialConfig("http://my.git");
        ConfigRepoConfig configRepoConfig = new ConfigRepoConfig(material, "myplugin");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepoConfig));
        configWatchList.onConfigChange(cruiseConfig);

        // set error state to simulate previously failed parse
        HealthStateScope scope = HealthStateScope.forPartialConfigRepo(configRepoConfig);
        serverHealthService.update(ServerHealthState.error("Parse failed", "Bad config format", HealthStateType.general(scope)));
        // verify error health
        assertFalse(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(configRepoConfig)).isEmpty());

        // now this should fix health
        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");

        assertTrue(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(configRepoConfig)).isEmpty());
    }

    private class BrokenConfigPlugin implements PartialConfigProvider
    {
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
    public void shouldThrowWhenGettingLatestPartialConfig_WhenInitializingPluginHasFailed() throws  Exception
    {
        when(configPluginService.partialConfigProviderFor(any(ConfigRepoConfig.class)))
                .thenThrow(new RuntimeException("Failed to initialize plugin"));

        ScmMaterialConfig material = new GitMaterialConfig("http://my.git");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(new ConfigRepoConfig(material,"myplugin")));
        configWatchList.onConfigChange(cruiseConfig);

        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");

        assertTrue(repoConfigDataSource.latestParseHasFailedForMaterial(material));

        try
        {
            repoConfigDataSource.latestPartialConfigForMaterial(material);
        }
        catch (RuntimeException ex)
        {
            assertThat(ex.getMessage(),is("Failed to initialize plugin"));
        }
    }

    @Test
    public void shouldRemovePartialsWhenRemovedFromWatchList() throws Exception
    {
        ScmMaterialConfig material = new GitMaterialConfig("http://my.git");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(new ConfigRepoConfig(material,"myplugin")));
        configWatchList.onConfigChange(cruiseConfig);

        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");
        assertNotNull(repoConfigDataSource.latestPartialConfigForMaterial(material));

        // we change current configuration
        ScmMaterialConfig othermaterial = new GitMaterialConfig("http://myother.git");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(new ConfigRepoConfig(othermaterial,"myplugin")));
        configWatchList.onConfigChange(cruiseConfig);

        assertNull(repoConfigDataSource.latestPartialConfigForMaterial(material));
    }

    @Test
    public void shouldListenForConfigRepoListChanged()
    {
        assertTrue(configWatchList.hasListener(repoConfigDataSource));
    }


}
