/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.admin.security.representers;

import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.spark.RequestContext;
import com.thoughtworks.go.domain.config.ConfigurationProperty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseInvalidJSON;
import static org.apache.commons.lang.StringUtils.isBlank;

public interface ConfigurationPropertyRepresenter {

    public static List<Map<String, Object>> toJSON(List<ConfigurationProperty> configurationProperties, RequestContext requestContext) {
        return configurationProperties.stream()
                .map(configurationProperty -> ConfigurationPropertyRepresenter.toJSON(configurationProperty, requestContext))
                .collect(Collectors.toList());
    }

    public static Map<String, Object> toJSON(ConfigurationProperty configurationProperty, RequestContext requestContext) {
        if (configurationProperty == null) {
            return null;
        }
        Map<String, Object> jsonObject = new LinkedHashMap<>();
        jsonObject.put("key", configurationProperty.getKey().getName());
        if (!configurationProperty.isSecure() && !isBlank(configurationProperty.getConfigValue())) {
            jsonObject.put("value", configurationProperty.getConfigurationValue().getValue());
        }
        if (configurationProperty.isSecure() && !isBlank(configurationProperty.getEncryptedValue())) {
            jsonObject.put("encrypted_value", configurationProperty.getEncryptedValue());
        }
        if (configurationProperty.hasErrors()) {
            jsonObject.put("errors", new ErrorGetter(new LinkedHashMap<String, String>() {{
                put("encryptedValue", "encrypted_value");
                put("configurationValue", "configuration_value");
                put("configurationKey", "configuration_key");
            }}).apply(configurationProperty, requestContext));
        }
        return jsonObject;
    }

    public static ConfigurationProperty fromJSON(JsonReader jsonReader) {
        try {
            String key = jsonReader.getString("key");
            String value = jsonReader.optString("value").orElse(null);
            String encryptedValue = jsonReader.optString("encrypted_value").orElse(null);
            return ConfigurationProperty.deserialize(key, value, encryptedValue);
        } catch (Exception e) {
            throw haltBecauseInvalidJSON("Could not parse configuration property");
        }
    }

}
