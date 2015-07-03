package com.thoughtworks.go.config.plugin;

import com.thoughtworks.go.config.PartialConfigLoadContext;
import com.thoughtworks.go.config.PartialConfigProvider;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoExtension;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRConfiguration;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRPartialConfig;

import java.io.File;
import java.util.Collection;

public class ConfigRepoPlugin implements PartialConfigProvider {
    private ConfigRepoExtension crExtension;
    private String pluginId;

    public ConfigRepoPlugin(ConfigRepoExtension crExtension, String pluginId) {

        this.crExtension = crExtension;
        this.pluginId = pluginId;
    }

    @Override
    public PartialConfig load(File configRepoCheckoutDirectory, PartialConfigLoadContext context) throws Exception {
        Collection<CRConfiguration> cRconfigurations = getCrConfigurations(context.configuration());
        CRPartialConfig crPartialConfig = parseDirectory(configRepoCheckoutDirectory, cRconfigurations);
        return ConfigConverter.toPartialConfig(crPartialConfig);
    }

    public CRPartialConfig parseDirectory(File configRepoCheckoutDirectory, Collection<CRConfiguration> cRconfigurations) {
        return this.crExtension.ParseCheckout(this.pluginId, configRepoCheckoutDirectory.getAbsolutePath(), cRconfigurations);
    }

    private Collection<CRConfiguration> getCrConfigurations(Configuration configuration) {
        throw  new RuntimeException("not implemented");
    }
}
