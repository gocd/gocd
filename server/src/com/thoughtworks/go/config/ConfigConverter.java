package com.thoughtworks.go.config;

import com.thoughtworks.go.config.BasicEnvironmentConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.plugin.access.configrepo.contract.CREnvironment;
import com.thoughtworks.go.plugin.access.configrepo.contract.CREnvironmentVariable;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRPartialConfig;
import com.thoughtworks.go.security.GoCipher;

/**
 * Helper to transform config repo classes to config-api classes
 */
public class ConfigConverter {

    private final GoCipher cipher;

    public ConfigConverter(GoCipher goCipher)
    {
        this.cipher = goCipher;
    }

    public PartialConfig toPartialConfig(CRPartialConfig crPartialConfig) {
        PartialConfig partialConfig = new PartialConfig();
        for(CREnvironment crEnvironment : crPartialConfig.getEnvironments())
        {
            EnvironmentConfig environment = toEnvironmentConfig(crEnvironment);
            partialConfig.getEnvironments().add(environment);
        }
        //TODO set other elements
        return partialConfig;
    }

    private EnvironmentConfig toEnvironmentConfig(CREnvironment crEnvironment) {
        BasicEnvironmentConfig basicEnvironmentConfig =
                new BasicEnvironmentConfig(new CaseInsensitiveString(crEnvironment.getName()));
        //TODO set other elements

        return basicEnvironmentConfig;
    }

    public EnvironmentVariableConfig toEnvironmentVariableConfig(CREnvironmentVariable crEnvironmentVariable) {
        if(crEnvironmentVariable.hasEncryptedValue())
        {
            return new EnvironmentVariableConfig(cipher,crEnvironmentVariable.getName(),crEnvironmentVariable.getEncryptedValue());
        }
        else
        {
            return new EnvironmentVariableConfig(crEnvironmentVariable.getName(),crEnvironmentVariable.getValue());
        }
    }

    //TODO #1133 convert each config element
}
