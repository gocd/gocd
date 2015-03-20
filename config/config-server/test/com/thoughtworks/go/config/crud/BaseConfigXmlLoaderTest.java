package com.thoughtworks.go.config.crud;

import com.thoughtworks.go.config.ConfigCache;
import com.thoughtworks.go.config.MagicalGoConfigXmlLoader;
import com.thoughtworks.go.domain.config.RepositoryMetadataStoreHelper;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
import com.thoughtworks.go.plugin.access.scm.SCMMetadataStore;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import org.junit.After;
import org.junit.Before;

import static org.mockito.Mockito.mock;

public abstract class BaseConfigXmlLoaderTest {
    MagicalGoConfigXmlLoader xmlLoader;
    ConfigCache configCache = new ConfigCache();
    MetricsProbeService metricsProbeService;

    @Before
    public void setup() throws Exception {
        SCMMetadataStore.getInstance().clear();

        metricsProbeService = mock(MetricsProbeService.class);
        xmlLoader = new MagicalGoConfigXmlLoader(configCache, ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService);
    }

    @After
    public void tearDown() throws Exception {
        SCMMetadataStore.getInstance().clear();
    }
}
