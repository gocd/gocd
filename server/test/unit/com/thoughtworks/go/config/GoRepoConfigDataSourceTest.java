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

import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.remote.*;
import com.thoughtworks.go.helper.PartialConfigMother;
import com.thoughtworks.go.server.materials.ScmMaterialCheckoutService;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class GoRepoConfigDataSourceTest {
    private GoConfigPluginService configPluginService;
    private GoConfigWatchList configWatchList;
    private ScmMaterialCheckoutService checkoutService;
    private PartialConfigProvider plugin;

    private GoRepoConfigDataSource repoConfigDataSource;

    private BasicCruiseConfig cruiseConfig ;

    File folder = new File("dir");

    @Before
    public void setUp()
    {
        configPluginService = mock(GoConfigPluginService.class);
        plugin = mock(PartialConfigProvider.class);
        when(configPluginService.partialConfigProviderFor(any(ConfigRepoConfig.class))).thenReturn(plugin);

        cruiseConfig = new BasicCruiseConfig();
        CachedFileGoConfig fileMock = mock(CachedFileGoConfig.class);
        when(fileMock.currentConfig()).thenReturn(cruiseConfig);

        configWatchList = new GoConfigWatchList(fileMock);
        checkoutService = mock(ScmMaterialCheckoutService.class);

        repoConfigDataSource = new GoRepoConfigDataSource(configWatchList,configPluginService,checkoutService);
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
    public void shouldProvideParseContextWhenCallingPluginParse()
    {
        ScmMaterialConfig material = new GitMaterialConfig("http://my.git");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(new ConfigRepoConfig(material,"myplugin")));
        configWatchList.onConfigChange(cruiseConfig);

        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");

        verify(plugin,times(1)).load(eq(folder), notNull(PartialConfigLoadContext.class));
    }

    @Test
    public void shouldNotCallPluginLoadOnCheckout_WhenMaterialNotInWatchList()
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

    private class BrokenConfigPlugin implements PartialConfigProvider
    {
        @Override
        public PartialConfig load(File configRepoCheckoutDirectory, PartialConfigLoadContext context) {
            throw new BrokenConfigPluginException();
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
