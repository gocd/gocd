package com.thoughtworks.go.config.remote;

import com.thoughtworks.go.config.materials.ScmMaterialConfig;

/**
 * Defines single source of remote configuration and name of plugin to interpet it.
 * This goes to standard static xml configuration.
 */
public class ConfigRepoConfig {
    // defines source of configuration. Any will fit
    private ScmMaterialConfig repo;


    // TODO something must instantiate this name into proper implementation of ConfigProvider
    // which can be a plugin or embedded class
    private String configProviderPluginName;
    // plugin-name which will process the repository tree to return configuration.
    // as in https://github.com/gocd/gocd/issues/1133#issuecomment-109014208
    // then pattern-based plugin is just one option

    public ScmMaterialConfig getMaterialConfig() {
        return repo;
    }

    public String getConfigProviderPluginName() {
        return configProviderPluginName;
    }

    public void setConfigProviderPluginName(String configProviderPluginName) {
        this.configProviderPluginName = configProviderPluginName;
    }
}
