/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.server.service.GoConfigService;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GoConfigWatchListTest {

    private CachedGoConfig cachedGoConfig;
    private GoConfigWatchList watchList;
    private CruiseConfig cruiseConfig;
    private GoConfigService goConfigService;
    private ConfigReposConfig configRepos;

    @Before
    public void setUp() throws Exception {
        cachedGoConfig = mock(CachedGoConfig.class);
        cruiseConfig = mock(CruiseConfig.class);
        goConfigService = mock(GoConfigService.class);
        configRepos = new ConfigReposConfig();

        when(cachedGoConfig.currentConfig()).thenReturn(cruiseConfig);
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(cruiseConfig.getConfigRepos()).thenReturn(configRepos);

        watchList = new GoConfigWatchList(cachedGoConfig, goConfigService);
    }

    @Test
    public void shouldListenForConfigFileChanges() {
        verify(cachedGoConfig, times(1)).registerListener(watchList);
    }

    @Test
    public void shouldNotifyConfigListenersWhenConfigChanges() throws Exception {
        final ChangedRepoConfigWatchListListener listener = mock(ChangedRepoConfigWatchListListener.class);

        watchList.registerListener(listener);
        watchList.onConfigChange(cruiseConfig);

        verify(listener, times(2)).onChangedRepoConfigWatchList(notNull(ConfigReposConfig.class));
    }

    @Test
    public void shouldNotifyConfigListenersWhenSingleConfigRepoHasChanged() throws Exception {
        final ChangedRepoConfigWatchListListener listener = mock(ChangedRepoConfigWatchListListener.class);
        watchList.registerListener(listener);
        watchList.onEntityConfigChange(new ConfigRepoConfig(new GitMaterialConfig("http://git1"), "myplugin", "id"));

        verify(listener, times(2)).onChangedRepoConfigWatchList(notNull(ConfigReposConfig.class));
    }

    @Test
    public void shouldReturnTrueWhenHasConfigRepoWithFingerprint() {
        GitMaterialConfig gitrepo = new GitMaterialConfig("http://configrepo.git");
        when(cruiseConfig.getConfigRepos()).thenReturn(new ConfigReposConfig(
                new ConfigRepoConfig(gitrepo,"myplugin")));

        watchList = new GoConfigWatchList(cachedGoConfig, goConfigService);

        assertTrue(watchList.hasConfigRepoWithFingerprint(gitrepo.getFingerprint()));
    }

    @Test
    public void shouldReturnFalseWhenDoesNotHaveConfigRepoWithFingerprint() {
        GitMaterialConfig gitrepo = new GitMaterialConfig("http://configrepo.git");
        when(cruiseConfig.getConfigRepos()).thenReturn(new ConfigReposConfig(
                new ConfigRepoConfig(gitrepo, "myplugin")));

        watchList = new GoConfigWatchList(cachedGoConfig, mock(GoConfigService.class));

        GitMaterialConfig gitrepo2 = new GitMaterialConfig("http://configrepo.git", "dev");
        assertFalse(watchList.hasConfigRepoWithFingerprint(gitrepo2.getFingerprint()));
    }


    @Test
    public void shouldReturnConfigRepoForMaterial() {
        GitMaterialConfig gitrepo = new GitMaterialConfig("http://configrepo.git");
        ConfigRepoConfig repoConfig = new ConfigRepoConfig(gitrepo, "myplugin");
        when(cruiseConfig.getConfigRepos()).thenReturn(new ConfigReposConfig(
                repoConfig));

        watchList = new GoConfigWatchList(cachedGoConfig, mock(GoConfigService.class));

        assertThat(watchList.getConfigRepoForMaterial(gitrepo), is(repoConfig));
    }

}
