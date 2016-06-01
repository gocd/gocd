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

package com.thoughtworks.go.plugin.access.common.settings;

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.access.common.handler.JSONResultMessageHandler;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isEmpty;

public class PluginSettingsJsonMessageHandler1_0 implements PluginSettingsJsonMessageHandler {
    private final JSONResultMessageHandler jsonResultMessageHandler;

    public PluginSettingsJsonMessageHandler1_0() {
        jsonResultMessageHandler = new JSONResultMessageHandler();
    }

    @Override
    public PluginSettingsConfiguration responseMessageForPluginSettingsConfiguration(String responseBody) {
        try {
            PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
            Map<String, Map> configurations;
            try {
                configurations = parseResponseToMap(responseBody);
            } catch (Exception e) {
                throw new RuntimeException("Plugin Settings Configuration should be returned as a map");
            }
            if (configurations == null || configurations.isEmpty()) {
                throw new RuntimeException("Empty response body");
            }
            for (String key : configurations.keySet()) {
                if (isEmpty(key)) {
                    throw new RuntimeException("Plugin Settings Configuration key cannot be empty");
                }
                if (!(configurations.get(key) instanceof Map)) {
                    throw new RuntimeException(format("Plugin Settings Configuration properties for key '%s' should be represented as a Map", key));
                }
                configuration.add(toPluginSettingsProperty(key, configurations.get(key)));
            }
            return configuration;
        } catch (Exception e) {
            throw new RuntimeException(format("Unable to de-serialize json response. %s", e.getMessage()));
        }
    }

    @Override
    public String responseMessageForPluginSettingsView(String responseBody) {
        try {
            final Map map = parseResponseToMap(responseBody);

            if (map.isEmpty()) {
                throw new RuntimeException("The JSON for Plugin Settings View cannot be empty");
            }

            final String template;
            try {
                template = (String) map.get("template");
            } catch (Exception e) {
                throw new RuntimeException("Plugin Settings View's 'template' should be of type string");
            }

            if (isEmpty(template)) {
                throw new RuntimeException("Plugin Settings View's 'template' is a required field");
            }

            return template;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Unable to de-serialize json response. Error: %s.", e.getMessage()));
        }
    }

    @Override
    public String requestMessageForPluginSettingsValidation(PluginSettingsConfiguration configuration) {
        Map configuredValues = new LinkedHashMap();
        configuredValues.put("plugin-settings", jsonResultMessageHandler.configurationToMap(configuration));
        return new GsonBuilder().create().toJson(configuredValues);
    }

    @Override
    public ValidationResult responseMessageForPluginSettingsValidation(String responseBody) {
        return jsonResultMessageHandler.toValidationResult(responseBody);
    }

    private Map parseResponseToMap(String responseBody) {
        return (Map) new GsonBuilder().create().fromJson(responseBody, Object.class);
    }

    private PluginSettingsProperty toPluginSettingsProperty(String key, Map configuration) {
        List<String> errors = new ArrayList<>();
        String defaultValue = null;
        try {
            defaultValue = (String) configuration.get("default-value");
        } catch (Exception e) {
            errors.add(format("'default-value' property for key '%s' should be of type string", key));
        }

        Boolean isSecure = null;
        try {
            isSecure = (Boolean) configuration.get("secure");
        } catch (Exception e) {
            errors.add(format("'secure' property for key '%s' should be of type boolean", key));
        }

        Boolean required = null;
        try {
            required = (Boolean) configuration.get("required");
        } catch (Exception e) {
            errors.add(format("'required' property for key '%s' should be of type boolean", key));
        }

        String displayName = null;
        try {
            displayName = (String) configuration.get("display-name");
        } catch (Exception e) {
            errors.add(format("'display-name' property for key '%s' should be of type string", key));
        }

        Integer displayOrder = null;
        try {
            displayOrder = configuration.get("display-order") == null ? null : Integer.parseInt((String) configuration.get("display-order"));
        } catch (Exception e) {
            errors.add(format("'display-order' property for key '%s' should be of type integer", key));
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException(StringUtils.join(errors, ", "));
        }

        PluginSettingsProperty property = new PluginSettingsProperty(key);
        if (!isEmpty(defaultValue)) {
            property.withDefault(defaultValue);
        }
        if (isSecure != null) {
            property.with(Property.SECURE, isSecure);
        }
        if (required != null) {
            property.with(Property.REQUIRED, required);
        }
        if (!isEmpty(displayName)) {
            property.with(Property.DISPLAY_NAME, displayName);
        }
        if (displayOrder != null) {
            property.with(Property.DISPLAY_ORDER, displayOrder);
        }
        return property;
    }
}
