package com.thoughtworks.go.plugin.access.configrepo.contract.tasks;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRConfigurationProperty;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRPluginConfiguration;
import org.apache.commons.collections.CollectionUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public class CRPluggableTask extends CRTask {
    public static final java.lang.String TYPE_NAME = "plugin";

    private CRPluginConfiguration plugin_configuration;
    private Collection<CRConfigurationProperty> configuration ;

    public CRPluggableTask()
    {
        super(TYPE_NAME);
    }
    public CRPluggableTask(CRPluginConfiguration pluginConfiguration,
                           CRConfigurationProperty... properties)
    {
        super(TYPE_NAME);
        this.plugin_configuration = pluginConfiguration;
        this.configuration = Arrays.asList(properties);
    }
    public CRPluggableTask(String id,String version,
                           CRConfigurationProperty... properties)
    {
        super(TYPE_NAME);
        this.plugin_configuration = new CRPluginConfiguration(id,version);
        this.configuration = Arrays.asList(properties);
    }
    public CRPluggableTask(CRRunIf runIf, CRTask onCancel,
                           CRPluginConfiguration pluginConfiguration,Collection<CRConfigurationProperty> configuration) {
        super(runIf, onCancel);
        this.plugin_configuration = pluginConfiguration;
        this.configuration = configuration;
    }

    public CRPluginConfiguration getPluginConfiguration() {
        return plugin_configuration;
    }

    public void setPluginConfiguration(CRPluginConfiguration pluginConfiguration) {
        this.plugin_configuration = pluginConfiguration;
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
        if (plugin_configuration != null ? !plugin_configuration.equals(that.plugin_configuration) : that.plugin_configuration != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (plugin_configuration != null ? plugin_configuration.hashCode() : 0);
        result = 31 * result + (configuration != null ? configuration.hashCode() : 0);
        return result;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        errors.checkMissing(location,"plugin_configuration",plugin_configuration);

        if(this.plugin_configuration != null)
            this.plugin_configuration.getErrors(errors,location);

        if(this.configuration != null)
        {
            for(CRConfigurationProperty p : configuration)
            {
                p.getErrors(errors,location);
            }
        }
        validateKeyUniqueness(errors,location);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        return String.format("%s; pluggable task",myLocation);
    }

    private void validateKeyUniqueness(ErrorCollection errors,String location) {
        if(this.configuration == null)
            return;
        HashSet<String> keys = new HashSet<>();
        for(CRConfigurationProperty property1 : this.configuration)
        {
            String key = property1.getKey();
            if(keys.contains(key))
                errors.addError(location,String.format(
                        "Configuration property %s is defined more than once",property1));
            else
                keys.add(key);
        }
    }
}
