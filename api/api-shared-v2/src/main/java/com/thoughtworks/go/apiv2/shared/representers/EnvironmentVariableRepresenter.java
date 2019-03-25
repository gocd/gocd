/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv2.shared.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.util.HaltApiResponses;
import com.thoughtworks.go.config.EnvironmentVariableConfig;
import com.thoughtworks.go.config.EnvironmentVariablesConfig;
import com.thoughtworks.go.config.exceptions.InvalidGoCipherTextException;
import com.thoughtworks.go.security.CryptoException;

import java.util.HashMap;
import java.util.Optional;


public class EnvironmentVariableRepresenter {
    public static void toJSON(OutputWriter outputWriter, EnvironmentVariableConfig environmentVariableConfig) {
        if (!environmentVariableConfig.errors().isEmpty()) {
            outputWriter.addChild("errors", errorWriter -> {
                HashMap<String, String> mapping = new HashMap<>();
                mapping.put("encryptedValue", "encrypted_value");
                new ErrorGetter(mapping).toJSON(outputWriter, environmentVariableConfig);
            });
        }

        outputWriter
                .add("secure", environmentVariableConfig.isSecure())
                .add("name", environmentVariableConfig.getName());
        addValue(outputWriter, environmentVariableConfig);
    }

    public static EnvironmentVariablesConfig fromJSONArray(JsonReader jsonReader) {
        EnvironmentVariablesConfig variables = new EnvironmentVariablesConfig();
        jsonReader.readArrayIfPresent("environment_variables", environmentVariables -> {
            environmentVariables.forEach(variable -> {
                variables.add(EnvironmentVariableRepresenter.fromJSON(new JsonReader(variable.getAsJsonObject())));
            });
        });
        return variables;
    }

    public static EnvironmentVariableConfig fromJSON(JsonReader jsonReader) {
        String name = jsonReader.getString("name");
        Boolean secure = jsonReader.optBoolean("secure").orElse(false);
        Optional<String> optValue = jsonReader.optString("value");
        Optional<String> optEncValue = jsonReader.optString("encrypted_value");

        if (!optValue.isPresent() && !optEncValue.isPresent()) {
            HaltApiResponses.haltBecauseOfReason("Environment variable must contain either 'value' or 'encrypted_value'");
        }

        String value = secure ? optValue.orElse(null) : jsonReader.getString("value");
        String encryptedValue = optEncValue.orElse(null);
        try {
            EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig();
            environmentVariableConfig.deserialize(name, value, secure, encryptedValue);
            return environmentVariableConfig;
        } catch (CryptoException e) {
            throw new InvalidGoCipherTextException(e.getMessage());
        }
    }

    private static void addValue(OutputWriter outputWriter, EnvironmentVariableConfig environmentVariableConfig) {
        if (environmentVariableConfig.isSecure()) {
            outputWriter.add("encrypted_value", environmentVariableConfig.getEncryptedValue());
        } else {
            outputWriter.add("value", environmentVariableConfig.getValue());
        }
    }
}
