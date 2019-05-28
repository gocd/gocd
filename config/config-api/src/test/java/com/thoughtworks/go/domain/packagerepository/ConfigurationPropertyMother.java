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

import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.EncryptedConfigurationValue;
import com.thoughtworks.go.security.GoCipher;

public class ConfigurationPropertyMother {
    public static ConfigurationProperty create(String key, boolean isSecure, String value) {
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey(key), new ConfigurationValue(value), null, new GoCipher());
        property.handleSecureValueConfiguration(isSecure);
        return property;
    }

    public static ConfigurationProperty create(String key) {
        return new ConfigurationProperty(new ConfigurationKey(key), new ConfigurationValue());
    }

    public static ConfigurationProperty create(String key, String value, String encryptedValue) {
        return new ConfigurationProperty(new ConfigurationKey(key), new ConfigurationValue(value), new EncryptedConfigurationValue(encryptedValue), null);
    }

    public static ConfigurationProperty create(String key, String value) {
        return create(key, false, value);
    }

    public static ConfigurationProperty createKeyOnly(String key) {
        ConfigurationValue configurationValue = null;
        return new ConfigurationProperty(new ConfigurationKey(key), configurationValue);
    }
}
