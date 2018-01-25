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

import com.google.gson.JsonParseException;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.representers.JsonWriter;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.spark.RequestContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.lang.StringUtils.isBlank;

public class ConfigurationPropertyRepresenter {

    public static List<Map> toJSON(List<ConfigurationProperty> configurationProperties, RequestContext requestContext) {
        return configurationProperties.stream()
                .map(configurationProperty -> ConfigurationPropertyRepresenter.toJSON(configurationProperty, requestContext))
                .collect(Collectors.toList());
    }

    public static Map toJSON(ConfigurationProperty configurationProperty, RequestContext requestContext) {
        if (configurationProperty == null) {
            return null;
        }
        JsonWriter jsonWriter = new JsonWriter(requestContext);
        jsonWriter.add("key", configurationProperty.getKey().getName());
        if (!configurationProperty.isSecure() && !isBlank(configurationProperty.getConfigValue())) {
            jsonWriter.add("value", configurationProperty.getConfigurationValue().getValue());
        }
        if (configurationProperty.isSecure() && !isBlank(configurationProperty.getEncryptedValue())) {
            jsonWriter.add("encrypted_value", configurationProperty.getEncryptedValue());
        }
        if (configurationProperty.hasErrors()) {
            jsonWriter.add("errors", new ErrorGetter(new LinkedHashMap<String, String>() {{
                put("encryptedValue", "encrypted_value");
                put("configurationValue", "configuration_value");
                put("configurationKey", "configuration_key");
            }}).apply(configurationProperty, requestContext));
        }
        return jsonWriter.getAsMap();
    }

    public static ConfigurationProperty fromJSON(JsonReader jsonReader) {
        try {
            String key = jsonReader.getString("key");
            String value = jsonReader.optString("value").orElse(null);
            String encryptedValue = jsonReader.optString("encrypted_value").orElse(null);
            return ConfigurationProperty.deserialize(key, value, encryptedValue);
        } catch (Exception e) {
            throw new JsonParseException("Could not parse configuration property");
        }
    }

}
