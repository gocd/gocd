package com.thoughtworks.go.config.plugin;

import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRConfiguration;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ConfigRepoPluginTest {

    @Test
    public void shouldGetCRConfigurationFromConfigurationWhenInsecureValue(){
        Configuration configuration = new Configuration();
        configuration.addNewConfigurationWithValue("key1","value1",false);

        List<CRConfiguration> crConfigurations = ConfigRepoPlugin.getCrConfigurations(configuration);
        assertThat(crConfigurations.size(),is(1));
        CRConfiguration prop = crConfigurations.get(0);
        assertThat(prop.getKey(),is("key1"));
        assertThat(prop.getValue(),is("value1"));
    }


    @Test
    public void shouldGetCRConfigurationFromConfigurationWhenSecureValue(){
        Configuration configuration = new Configuration();
        configuration.addNewConfigurationWithValue("key1","@$$%^1234",true);

        List<CRConfiguration> crConfigurations = ConfigRepoPlugin.getCrConfigurations(configuration);
        assertThat(crConfigurations.size(),is(1));
        CRConfiguration prop = crConfigurations.get(0);
        assertThat(prop.getKey(),is("key1"));
        assertThat(prop.getEncryptedValue(),is("@$$%^1234"));
    }
}
