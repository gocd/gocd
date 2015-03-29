package com.thoughtworks.go.domain.config;

import com.thoughtworks.go.config.ConfigSubtag;
import com.thoughtworks.go.config.ConfigTag;

@ConfigTag("artifactCleanupStrategy")
public class ArtifactCleanupStrategy {

    @ConfigSubtag
    private PluginConfiguration pluginConfiguration = new PluginConfiguration();

    public ArtifactCleanupStrategy() {
    }

    public ArtifactCleanupStrategy(PluginConfiguration pluginConfiguration) {
        this.pluginConfiguration = pluginConfiguration;
    }

    public PluginConfiguration getPluginConfiguration() {
        return pluginConfiguration;
    }
}
