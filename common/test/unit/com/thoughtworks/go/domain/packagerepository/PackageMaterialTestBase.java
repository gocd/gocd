/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.domain.packagerepository;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.EncryptedConfigurationValue;
import com.thoughtworks.go.domain.config.RepositoryMetadataStoreHelper;
import org.junit.After;

public abstract class PackageMaterialTestBase {
    @After
    public void tearDown() {
        RepositoryMetadataStoreHelper.clear();
        teardown();
    }

    protected void teardown(){
    }

    protected void createPackageConfigurationsFor(Map attributes, ConfigurationHolder[] configurations) {
        Map configurationMap = new LinkedHashMap();
        for (int i = 0; i < configurations.length; i++) {
            ConfigurationHolder currentConfiguration = configurations[i];

            HashMap config = new HashMap();
            HashMap firstConfigKey = new HashMap();
            firstConfigKey.put(ConfigurationKey.NAME, currentConfiguration.name);
            config.put(ConfigurationProperty.CONFIGURATION_KEY, firstConfigKey);

            HashMap firstConfigValue = new HashMap();
            firstConfigValue.put(ConfigurationValue.VALUE, currentConfiguration.value);
            config.put(ConfigurationProperty.CONFIGURATION_VALUE, firstConfigValue);

            if (currentConfiguration.isChanged()) {
                config.put(ConfigurationProperty.IS_CHANGED, "1");
            }
            if (currentConfiguration.isSecure) {
                HashMap encryptedValue = new HashMap();
                encryptedValue.put(EncryptedConfigurationValue.VALUE, currentConfiguration.encryptedValue);
                config.put(ConfigurationProperty.ENCRYPTED_VALUE, encryptedValue);
            }
            configurationMap.put(String.valueOf(i), config);
        }
        attributes.put(Configuration.CONFIGURATION, configurationMap);
    }

    protected Map createPackageDefinitionConfiguration(String name, String pluginId, ConfigurationHolder... configurations) {
        Map attributes = new HashMap();
        attributes.put(PackageDefinition.NAME, name);
        attributes.put("pluginId", pluginId);

        createPackageConfigurationsFor(attributes, configurations);
        return attributes;
    }

    class ConfigurationHolder {
        String name;
        String value;
        String encryptedValue;
        boolean isSecure;
        String isChanged;

        ConfigurationHolder(String name, String value) {
            this.name = name;
            this.value = value;
        }

        ConfigurationHolder(String name, String value, String encryptedValue, boolean isSecure, String isChanged) {
            this.name = name;
            this.value = value;
            this.encryptedValue = encryptedValue;
            this.isSecure = isSecure;
            this.isChanged = isChanged;
        }

        public boolean isChanged() {
            return "1".equals(isChanged);
        }
    }
}
