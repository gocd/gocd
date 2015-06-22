package com.thoughtworks.go.config;

import com.thoughtworks.go.config.parts.XmlPartialConfigProvider;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Provides config plugins implementations
 */
@Component
public class GoConfigPluginService {

    private final XmlPartialConfigProvider embeddedXmlPlugin;

    @Autowired public GoConfigPluginService(ConfigCache configCache,ConfigElementImplementationRegistry configElementImplementationRegistry,
                                 MetricsProbeService metricsProbeService)
    {
        MagicalGoConfigXmlLoader loader = new MagicalGoConfigXmlLoader(configCache, configElementImplementationRegistry, metricsProbeService);
        embeddedXmlPlugin = new XmlPartialConfigProvider(loader);
    }

    public PartialConfigProvider partialConfigProviderFor(ConfigRepoConfig repoConfig)
    {
        return embeddedXmlPlugin;
    }
}
