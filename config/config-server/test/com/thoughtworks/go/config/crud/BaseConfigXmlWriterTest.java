package com.thoughtworks.go.config.crud;

import com.thoughtworks.go.config.ConfigCache;
import com.thoughtworks.go.config.MagicalGoConfigXmlLoader;
import com.thoughtworks.go.config.MagicalGoConfigXmlWriter;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;

import java.io.ByteArrayOutputStream;

import static org.mockito.Mockito.mock;

public abstract class BaseConfigXmlWriterTest {
    ByteArrayOutputStream output;
    MagicalGoConfigXmlWriter xmlWriter;
    SystemEnvironment systemEnvironment;
    MagicalGoConfigXmlLoader xmlLoader;

    @Before
    public void setup() {
        MetricsProbeService metricsProbeService = mock(MetricsProbeService.class);
        output = new ByteArrayOutputStream();
        ConfigCache configCache = new ConfigCache();
        xmlWriter = new MagicalGoConfigXmlWriter(configCache, ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService);
        xmlLoader = new MagicalGoConfigXmlLoader(configCache, ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService);
    }
}
