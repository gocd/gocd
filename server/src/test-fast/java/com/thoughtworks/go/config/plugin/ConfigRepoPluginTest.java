/*
 * Copyright 2020 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.config.plugin;

import com.thoughtworks.go.config.ConfigRepoPlugin;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.plugin.configrepo.contract.CRConfigurationProperty;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;

public class ConfigRepoPluginTest {
    @Test
    public void shouldGetCRConfigurationFromConfigurationWhenInsecureValue() {
        Configuration configuration = new Configuration();
        configuration.addNewConfigurationWithValue("key1", "value1", false);

        List<CRConfigurationProperty> crConfigurations = ConfigRepoPlugin.getCrConfigurations(configuration);
        assertThat(crConfigurations.size(), is(1));
        CRConfigurationProperty prop = crConfigurations.get(0);
        assertThat(prop.getKey(), is("key1"));
        assertThat(prop.getValue(), is("value1"));
    }

    @Test
    public void shouldGetCRConfigurationFromConfigurationWhenSecureValue() {
        Configuration configuration = new Configuration();
        configuration.addNewConfigurationWithValue("key1", "@$$%^1234", true);

        List<CRConfigurationProperty> crConfigurations = ConfigRepoPlugin.getCrConfigurations(configuration);
        assertThat(crConfigurations.size(), is(1));
        CRConfigurationProperty prop = crConfigurations.get(0);
        assertThat(prop.getKey(), is("key1"));
        assertThat(prop.getEncryptedValue(), is("@$$%^1234"));
    }
}
