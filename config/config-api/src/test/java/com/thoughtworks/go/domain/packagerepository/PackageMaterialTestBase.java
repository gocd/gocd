/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain.packagerepository;

import com.thoughtworks.go.config.helper.ConfigurationHolder;
import com.thoughtworks.go.domain.config.*;
import org.junit.After;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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


}
