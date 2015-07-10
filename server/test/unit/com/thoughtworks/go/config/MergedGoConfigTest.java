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

import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.helper.NoOpMetricsProbeService;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
import com.thoughtworks.go.server.materials.ScmMaterialCheckoutService;
import com.thoughtworks.go.server.util.ServerVersion;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.*;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static com.thoughtworks.go.helper.ConfigFileFixture.CONFIG;
import static com.thoughtworks.go.helper.ConfigFileFixture.CONFIG_WITH_1CONFIGREPO;
import static com.thoughtworks.go.helper.ConfigFileFixture.ONE_CONFIG_REPO;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class MergedGoConfigTest extends CachedGoConfigBaseTest {

    private CachedFileGoConfig cachedFileGoConfig;

    private GoConfigPluginService configPluginService;
    private GoConfigWatchList configWatchList;
    private ScmMaterialCheckoutService checkoutService;
    private PartialConfigProvider plugin;

    private GoRepoConfigDataSource repoConfigDataSource;

    private GoPartialConfig partials;

    private File folder = new File("workdir");

    @Before
    public void setUp() throws Exception {
        configHelper = new GoConfigFileHelper(CONFIG_WITH_1CONFIGREPO);
        SystemEnvironment env = new SystemEnvironment();
        ConfigRepository configRepository = new ConfigRepository(env);
        configRepository.initialize();
        dataSource = new GoFileConfigDataSource(new DoNotUpgrade(), configRepository, env, new TimeProvider(),
                new ConfigCache(), new ServerVersion(), ConfigElementImplementationRegistryMother.withNoPlugins(),
                metricsProbeService, serverHealthService);
        serverHealthService = new ServerHealthService();
        cachedFileGoConfig = new CachedFileGoConfig(dataSource, serverHealthService);
        cachedFileGoConfig.loadConfigIfNull();

        configPluginService = mock(GoConfigPluginService.class);
        plugin = mock(PartialConfigProvider.class);
        when(configPluginService.partialConfigProviderFor(any(ConfigRepoConfig.class))).thenReturn(plugin);

        configWatchList = new GoConfigWatchList(cachedFileGoConfig);
        checkoutService = mock(ScmMaterialCheckoutService.class);

        repoConfigDataSource = new GoRepoConfigDataSource(configWatchList,configPluginService,checkoutService);

        partials = new GoPartialConfig(repoConfigDataSource,configWatchList);

        cachedGoConfig = new MergedGoConfig(serverHealthService,cachedFileGoConfig, partials);
        configHelper.usingCruiseConfigDao(new GoConfigDao(cachedFileGoConfig, metricsProbeService));
    }

    @Test
    public void shouldNotifyListenersWhenFileChanged()
    {
        ConfigChangedListener listener = mock(ConfigChangedListener.class);
        cachedGoConfig.registerListener(listener);
        verify(listener, times(1)).onConfigChange(any(CruiseConfig.class));

        String content = "<cruise schemaVersion='" + GoConstants.CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifacts' />"
                + "<pipelines>\n"
                + "</pipelines>\n"
                + "</cruise>";

        configHelper.writeXmlToConfigFile(content);

        // once during registerListener call, second when reloaded
        verify(listener, times(2)).onConfigChange(any(CruiseConfig.class));
    }

    @Test
    public void shouldListenForNewPartialConfigs()
    {
        assertTrue(partials.hasListener((MergedGoConfig)cachedGoConfig));
    }
    @Test
    public void shouldListenForFileChanges()
    {
        assertTrue(cachedFileGoConfig.hasListener((MergedGoConfig)cachedGoConfig));
    }

    @Test
    public void shouldReturnMergedConfig_WhenThereIsValidPartialConfig() throws Exception
    {
        assertThat(configWatchList.getCurrentConfigRepos().size(),is(1));
        ConfigRepoConfig configRepo = configWatchList.getCurrentConfigRepos().get(0);
        PartialConfig part1 = new PartialConfig(new PipelineGroups(
                PipelineConfigMother.createGroup("part1", PipelineConfigMother.pipelineConfig("pipe1"))));
        when(plugin.Load(any(File.class),any(PartialConfigLoadContext.class))).thenReturn(
                part1
        );
        repoConfigDataSource.onCheckoutComplete(configRepo.getMaterialConfig(),folder,"321e");
        assertThat(repoConfigDataSource.latestPartialConfigForMaterial(configRepo.getMaterialConfig()),is(part1));
        assertThat(cachedGoConfig.currentConfig().hasPipelineNamed(new CaseInsensitiveString("pipe1")),is(true));
    }

    @Test
    public void shouldNotifyWithMergedConfig_WhenPartUpdated() throws Exception
    {
        ConfigRepoConfig configRepo = configWatchList.getCurrentConfigRepos().get(0);
        PartialConfig part1 = new PartialConfig(new PipelineGroups(
                PipelineConfigMother.createGroup("part1", PipelineConfigMother.pipelineConfig("pipe1"))));
        when(plugin.Load(any(File.class),any(PartialConfigLoadContext.class))).thenReturn(part1);

        ConfigChangedListener listener = mock(ConfigChangedListener.class);
        cachedGoConfig.registerListener(listener);

        repoConfigDataSource.onCheckoutComplete(configRepo.getMaterialConfig(),folder,"321e");

        assertThat("currentConfigShouldBeMerged",
                cachedGoConfig.currentConfig().hasPipelineNamed(new CaseInsensitiveString("pipe1")),is(true));
        verify(listener, times(1)).onConfigChange(cachedGoConfig.currentConfig());
    }

}
