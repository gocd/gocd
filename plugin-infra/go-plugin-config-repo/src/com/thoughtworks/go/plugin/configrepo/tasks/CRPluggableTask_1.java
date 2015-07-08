package com.thoughtworks.go.plugin.configrepo.tasks;

import com.thoughtworks.go.plugin.configrepo.CRConfigurationProperty_1;
import com.thoughtworks.go.plugin.configrepo.CRPluginConfiguration_1;
import com.thoughtworks.go.plugin.configrepo.ErrorCollection;
import org.apache.commons.collections.CollectionUtils;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public class CRPluggableTask_1 extends CRTask_1 {
    public static final java.lang.String TYPE_NAME = "pluggabletask";

    private CRPluginConfiguration_1 pluginConfiguration ;
    private Collection<CRConfigurationProperty_1> configuration ;

    public CRPluggableTask_1()
    {
        super(TYPE_NAME);
    }
    public CRPluggableTask_1(CRPluginConfiguration_1 pluginConfiguration,
                             CRConfigurationProperty_1... properties)
    {
        super(TYPE_NAME);
        this.pluginConfiguration = pluginConfiguration;
        this.configuration = Arrays.asList(properties);
    }
    public CRPluggableTask_1(String id,String version,
                             CRConfigurationProperty_1... properties)
    {
        super(TYPE_NAME);
        this.pluginConfiguration = new CRPluginConfiguration_1(id,version);
        this.configuration = Arrays.asList(properties);
    }

    @Override
    public void getErrors(ErrorCollection errors) {
        if(this.pluginConfiguration != null)
            this.pluginConfiguration.getErrors(errors);
        else
            errors.add(this,"Pluggable task has no plugin configuration");
        if(this.configuration != null)
        {
            for(CRConfigurationProperty_1 p : configuration)
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
        for(CRConfigurationProperty_1 property1 : this.configuration)
        {
            String key = property1.getKey();
            if(keys.contains(key))
                errors.add(this,String.format(
                        "Configuration property %s is defined more than once",property1));
            else
                keys.add(key);
        }
    }

    public CRPluginConfiguration_1 getPluginConfiguration() {
        return pluginConfiguration;
    }

    public void setPluginConfiguration(CRPluginConfiguration_1 pluginConfiguration) {
        this.pluginConfiguration = pluginConfiguration;
    }

    public Collection<CRConfigurationProperty_1> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Collection<CRConfigurationProperty_1> configuration) {
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

        CRPluggableTask_1 that = (CRPluggableTask_1) o;

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
}
