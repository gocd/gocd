package com.thoughtworks.go.plugin.access.configrepo.contract.tasks;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRConfigurationProperty;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRPluginConfiguration;
import org.apache.commons.collections.CollectionUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public class CRPluggableTask extends CRTask {
    public static final java.lang.String TYPE_NAME = "pluggabletask";

    private CRPluginConfiguration pluginConfiguration ;
    private Collection<CRConfigurationProperty> configuration ;

    public CRPluggableTask()
    {
        super(TYPE_NAME);
    }
    public CRPluggableTask(CRPluginConfiguration pluginConfiguration,
                           CRConfigurationProperty... properties)
    {
        super(TYPE_NAME);
        this.pluginConfiguration = pluginConfiguration;
        this.configuration = Arrays.asList(properties);
    }
    public CRPluggableTask(String id,String version,
                           CRConfigurationProperty... properties)
    {
        super(TYPE_NAME);
        this.pluginConfiguration = new CRPluginConfiguration(id,version);
        this.configuration = Arrays.asList(properties);
    }
/*
    @Override
    public void getErrors(ErrorCollection errors) {
        if(this.pluginConfiguration != null)
            this.pluginConfiguration.getErrors(errors);
        else
            errors.add(this,"Pluggable task has no plugin configuration");
        if(this.configuration != null)
        {
            for(CRConfigurationProperty p : configuration)
            {
                p.getErrors(errors);
            }
        }
        validateType(errors);
        validateKeyUniqueness(errors);
        validateOnCancel(errors);
    }
    private void validateKeyUniqueness(ErrorCollection errors) {
        if(this.configuration == null)
            return;
        HashSet<String> keys = new HashSet<>();
        for(CRConfigurationProperty property1 : this.configuration)
        {
            String key = property1.getKey();
            if(keys.contains(key))
                errors.add(this,String.format(
                        "Configuration property %s is defined more than once",property1));
            else
                keys.add(key);
        }
    }*/

    public CRPluginConfiguration getPluginConfiguration() {
        return pluginConfiguration;
    }

    public void setPluginConfiguration(CRPluginConfiguration pluginConfiguration) {
        this.pluginConfiguration = pluginConfiguration;
    }

    public Collection<CRConfigurationProperty> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Collection<CRConfigurationProperty> configuration) {
        this.configuration = configuration;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        CRPluggableTask that = (CRPluggableTask) o;

        if (configuration != null && that.configuration == null)
            return false;
        if (configuration == null && that.configuration != null)
            return false;
        if (configuration != null ? !CollectionUtils.isEqualCollection(configuration,that.configuration) : that.configuration != null) {
            return false;
        }
        if (pluginConfiguration != null ? !pluginConfiguration.equals(that.pluginConfiguration) : that.pluginConfiguration != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (pluginConfiguration != null ? pluginConfiguration.hashCode() : 0);
        result = 31 * result + (configuration != null ? configuration.hashCode() : 0);
        return result;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {

    }

    @Override
    public String getLocation(String parent) {
        return null;
    }
}
