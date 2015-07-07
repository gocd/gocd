package com.thoughtworks.go.plugin.access.configrepo.contract.tasks;

import com.thoughtworks.go.plugin.access.configrepo.contract.CRConfigurationProperty;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRPluginConfiguration;

import java.util.Collection;

public class CRPluggableTask extends CRTask {
    private final CRPluginConfiguration pluginConfiguration ;
    private final Collection<CRConfigurationProperty> configuration ;

    public CRPluggableTask(CRRunIf runIf, CRTask onCancel,
                           CRPluginConfiguration pluginConfiguration,Collection<CRConfigurationProperty> configuration) {
        super(runIf, onCancel);
        this.pluginConfiguration = pluginConfiguration;
        this.configuration = configuration;
    }

    public CRPluginConfiguration getPluginConfiguration() {
        return pluginConfiguration;
    }

    public Collection<CRConfigurationProperty> getConfiguration() {
        return configuration;
    }

    public CRConfigurationProperty getPropertyByKey(String key)
    {
        for(CRConfigurationProperty property : configuration)
        {
            if(property.getKey().equals(key))
                return  property;
        }
        return null;
    }
}
