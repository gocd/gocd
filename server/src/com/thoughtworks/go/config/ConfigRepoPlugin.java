package com.thoughtworks.go.config;

import com.thoughtworks.go.config.ConfigConverter;
import com.thoughtworks.go.config.PartialConfigLoadContext;
import com.thoughtworks.go.config.PartialConfigProvider;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoExtension;
import com.thoughtworks.go.plugin.access.configrepo.InvalidPartialConfigException;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRConfigurationProperty;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRError;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRParseResult;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ConfigRepoPlugin implements PartialConfigProvider {
    private ConfigConverter configConverter ;
    private ConfigRepoExtension crExtension;
    private String pluginId;

    public ConfigRepoPlugin(ConfigConverter configConverter,ConfigRepoExtension crExtension, String pluginId) {
        this.configConverter = configConverter;
        this.crExtension = crExtension;
        this.pluginId = pluginId;
    }

    @Override
    public PartialConfig load(File configRepoCheckoutDirectory, PartialConfigLoadContext context) {
        Collection<CRConfigurationProperty> cRconfigurations = getCrConfigurations(context.configuration());
        CRParseResult crPartialConfig = parseDirectory(configRepoCheckoutDirectory, cRconfigurations);
        return configConverter.toPartialConfig(crPartialConfig);
    }

    @Override
    public String displayName() {
        return "Plugin " + this.pluginId;
    }

    public CRParseResult parseDirectory(File configRepoCheckoutDirectory, Collection<CRConfigurationProperty> cRconfigurations) {
        CRParseResult crParseResult = this.crExtension.parseDirectory(this.pluginId, configRepoCheckoutDirectory.getAbsolutePath(), cRconfigurations);
        if(crParseResult.hasErrors())
            throw new InvalidPartialConfigException(crParseResult,crParseResult.getErrors());
        return crParseResult;
    }

    public static List<CRConfigurationProperty> getCrConfigurations(Configuration configuration) {
        List<CRConfigurationProperty> config = new ArrayList<>();
        for(ConfigurationProperty prop : configuration)
        {
            String configKeyName = prop.getConfigKeyName();
            if(!prop.isSecure())
                config.add(new CRConfigurationProperty(configKeyName,prop.getValue(),null));
            else
            {
                CRConfigurationProperty crProp = new CRConfigurationProperty(configKeyName,null,prop.getEncryptedValue().getValue());
                config.add(crProp);
            }
        }
        return config;
    }
}
