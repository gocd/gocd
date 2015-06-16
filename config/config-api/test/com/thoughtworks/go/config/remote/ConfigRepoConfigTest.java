package com.thoughtworks.go.config.remote;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class ConfigRepoConfigTest {
    @Test
    public void shouldReturnPluginNameWhenSpecified() {
        ConfigRepoConfig config = new ConfigRepoConfig();
        config.setConfigProviderPluginName("myplugin");
        assertThat(config.getConfigProviderPluginName(),is("myplugin"));
    }
    @Test
    public void shouldReturnNullPluginNameWhenEmpty() {
        ConfigRepoConfig config = new ConfigRepoConfig();
        config.setConfigProviderPluginName("");
        assertNull(config.getConfigProviderPluginName());
    }
}
