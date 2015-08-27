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
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.server.materials.ScmMaterialCheckoutService;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class GoPartialConfigTest {

    private GoConfigPluginService configPluginService;
    private GoConfigWatchList configWatchList;
    private ScmMaterialCheckoutService checkoutService;
    private PartialConfigProvider plugin;

    private GoRepoConfigDataSource repoConfigDataSource;

    private BasicCruiseConfig cruiseConfig ;

    private GoPartialConfig partialConfig;

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

        partialConfig = new GoPartialConfig(repoConfigDataSource,configWatchList);
    }

    @Test
    public void shouldReturnEmptyListWhenWatchListEmpty()
    {
        assertThat(partialConfig.lastPartials().isEmpty(),is(true));
    }
    @Test
    public void shouldReturnEmptyListWhenNothingParsedYet_AndWatchListNotEmpty()
    {
        setOneConfigRepo();

        assertThat(partialConfig.lastPartials().isEmpty(),is(true));
    }

    private ScmMaterialConfig setOneConfigRepo() {
        ScmMaterialConfig material = new GitMaterialConfig("http://my.git");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(new ConfigRepoConfig(material,"myplugin")));
        configWatchList.onConfigChange(cruiseConfig);
        return material;
    }

    @Test
    public void shouldReturnLatestPartialAfterCheckout_AndWatchListNotEmpty()
    {
        ScmMaterialConfig material = setOneConfigRepo();

        PartialConfig part = new PartialConfig();
        when(plugin.load(any(File.class), any(PartialConfigLoadContext.class))).thenReturn(part);

        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");

        assertThat(partialConfig.lastPartials().size(), is(1));
        assertThat(partialConfig.lastPartials().get(0), is(part));
    }
    @Test
    public void shouldReturnEmptyList_WhenFirstParsingFailed()
    {
        ScmMaterialConfig material = setOneConfigRepo();

        PartialConfig part = new PartialConfig();
        when(plugin.load(any(File.class), any(PartialConfigLoadContext.class)))
                .thenThrow(new RuntimeException("Failed parsing"));

        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");

        assertThat(repoConfigDataSource.latestParseHasFailedForMaterial(material),is(true));

        assertThat(partialConfig.lastPartials().size(),is(0));
    }
    @Test
    public void shouldReturnFirstPartial_WhenFirstParsedSucceed_ButSecondFailed()
    {
        ScmMaterialConfig material = setOneConfigRepo();

        PartialConfig part = new PartialConfig();
        when(plugin.load(any(File.class), any(PartialConfigLoadContext.class))).thenReturn(part);

        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");

        when(plugin.load(any(File.class), any(PartialConfigLoadContext.class)))
                .thenThrow(new RuntimeException("Failed parsing"));
        repoConfigDataSource.onCheckoutComplete(material,folder,"6354");

        assertThat(repoConfigDataSource.latestParseHasFailedForMaterial(material),is(true));

        assertThat(partialConfig.lastPartials().size(), is(1));
        assertThat(partialConfig.lastPartials().get(0), is(part));
    }

    @Test
    public void shouldRemovePartialWhenNoLongerInWatchList()
    {
        ScmMaterialConfig material = setOneConfigRepo();

        PartialConfig part = new PartialConfig();
        when(plugin.load(any(File.class), any(PartialConfigLoadContext.class))).thenReturn(part);

        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");

        assertThat(partialConfig.lastPartials().size(), is(1));
        assertThat(partialConfig.lastPartials().get(0), is(part));

        // we change current configuration
        ScmMaterialConfig othermaterial = new GitMaterialConfig("http://myother.git");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(new ConfigRepoConfig(othermaterial,"myplugin")));
        configWatchList.onConfigChange(cruiseConfig);

        assertThat(partialConfig.lastPartials().size(),is(0));
    }

    @Test
    public void shouldListenForConfigRepoListChanges()
    {
        assertTrue(repoConfigDataSource.hasListener(partialConfig));
    }
    @Test
    public void shouldListenForCompletedParsing()
    {
        assertTrue(configWatchList.hasListener(partialConfig));
    }

    @Test
    public void shouldNotifyListenersAfterUpdatingMapOfLatestValidConfig()
    {
        ScmMaterialConfig material = setOneConfigRepo();

        PartialConfig part = new PartialConfig();
        when(plugin.load(any(File.class), any(PartialConfigLoadContext.class))).thenReturn(part);

        PartialConfigUpdateCompletedListener listener = mock(PartialConfigUpdateCompletedListener.class);
        repoConfigDataSource.registerListener(listener);

        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");
        verify(listener,times(1)).onSuccessPartialConfig(any(ConfigRepoConfig.class), any(PartialConfig.class));
    }
}
