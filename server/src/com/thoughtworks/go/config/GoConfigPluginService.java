package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import org.springframework.stereotype.Component;

/**
 * Provides config plugins implementations
 */
@Component
public class GoConfigPluginService {
    public PartialConfigProvider partialConfigProviderFor(ConfigRepoConfig repoConfig)
    {
        return  null;
    }
}
