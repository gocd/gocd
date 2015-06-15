package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.helper.NoOpMetricsProbeService;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
import com.thoughtworks.go.server.util.ServerVersion;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.helper.ConfigFileFixture.CONFIG;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by tomzo on 6/15/15.
 */
public class GoConfigWatchListTest {

    private CachedFileGoConfig cachedGoConfig;
    private GoConfigWatchList watchList;

    @Before
    public void setUp() throws Exception {
        cachedGoConfig = mock(CachedFileGoConfig.class);
        watchList = new GoConfigWatchList(cachedGoConfig);
    }

    @Test
    public void shouldNotifyConfigListenersWhenConfigChanges() throws Exception {
        final ChangedRepoConfigWatchListListener listener = mock(ChangedRepoConfigWatchListListener.class);

        watchList.registerListener(listener);

        CruiseConfig cruiseConfig = mock(CruiseConfig.class);
        watchList.onConfigChange(cruiseConfig);

        verify(listener, times(1)).onChangedRepoConfigWatchList(notNull(ConfigReposConfig.class));
    }
}
