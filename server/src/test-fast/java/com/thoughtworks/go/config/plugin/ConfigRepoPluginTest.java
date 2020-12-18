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
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.helper.ReversingEncrypter;
import com.thoughtworks.go.plugin.configrepo.contract.CRConfigurationProperty;
import com.thoughtworks.go.security.GoCipher;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigRepoPluginTest {

    private final GoCipher cipher = new GoCipher(new ReversingEncrypter());

    @Test
    void shouldGetCRConfigurationFromConfigurationWhenInsecureValue() {
        Configuration configuration = new Configuration();
        configuration.add(new ConfigurationProperty(cipher).withKey("key1").withValue("value1"));

        List<CRConfigurationProperty> crConfigurations = ConfigRepoPlugin.getCrConfigurations(configuration);
        assertEquals(1, crConfigurations.size());
        CRConfigurationProperty prop = crConfigurations.get(0);
        assertEquals("key1", prop.getKey());
        assertEquals("value1", prop.getValue());
    }

    @Test
    void shouldGetCRConfigurationFromConfigurationWhenSecureValue() {
        Configuration configuration = new Configuration();
        configuration.add(new ConfigurationProperty(cipher).withKey("key1").withEncryptedValue("terces"));

        List<CRConfigurationProperty> crConfigurations = ConfigRepoPlugin.getCrConfigurations(configuration);
        assertEquals(1, crConfigurations.size());
        CRConfigurationProperty prop = crConfigurations.get(0);
        assertEquals("key1", prop.getKey());
        assertEquals("secret", prop.getValue());
    }
}
