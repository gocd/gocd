package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.helper.NoOpMetricsProbeService;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
import com.thoughtworks.go.server.materials.ScmMaterialCheckoutService;
import com.thoughtworks.go.server.util.ServerVersion;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.*;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.helper.ConfigFileFixture.CONFIG;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by tomzo on 6/19/15.
 */
public class CachedGoConfigTest {

    private CachedFileGoConfig cachedFileGoConfig;
    private CachedGoConfig cachedGoConfig;
    private GoConfigFileHelper configHelper;
    private GoFileConfigDataSource dataSource;
    private ServerHealthService serverHealthService;
    private MetricsProbeService metricsProbeService = new NoOpMetricsProbeService();

    private GoConfigPluginService configPluginService;
    private GoConfigWatchList configWatchList;
    private ScmMaterialCheckoutService checkoutService;
    private PartialConfigProvider plugin;

    private GoRepoConfigDataSource repoConfigDataSource;

    private GoPartialConfig partials;

    @Before
    public void setUp() throws Exception {
        configHelper = new GoConfigFileHelper(CONFIG);
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

        cachedGoConfig = new CachedGoConfig(serverHealthService,cachedFileGoConfig, partials);
    }

    @Test
    public void shouldNotifyListenersWhenFileChanged()
    {
        ConfigChangedListener listener = mock(ConfigChangedListener.class);
        cachedGoConfig.registerListener(listener);

        String content = "<cruise schemaVersion='" + GoConstants.CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifacts' />"
                + "<pipelines>\n"
                + "</pipelines>\n"
                + "</cruise>";

        configHelper.writeXmlToConfigFile(content);

        verify(listener, times(1)).onConfigChange(any(CruiseConfig.class));
    }

}
