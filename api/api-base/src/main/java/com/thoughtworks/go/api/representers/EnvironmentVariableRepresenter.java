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
package com.thoughtworks.go.api.representers;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.util.HaltApiResponses;
import com.thoughtworks.go.config.EnvironmentVariableConfig;
import com.thoughtworks.go.config.EnvironmentVariablesConfig;
import com.thoughtworks.go.security.CryptoException;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static com.thoughtworks.go.api.util.HaltApiMessages.errorWhileEncryptingMessage;

public class EnvironmentVariableRepresenter {
    public static void toJSON(OutputListWriter jsonWriter, List<EnvironmentVariableConfig> environmentVariableConfigs) {
        environmentVariableConfigs.forEach(environmentVariableConfig -> {
            jsonWriter.addChild(envVarWriter -> toJSON(envVarWriter, environmentVariableConfig));
        });

    }

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

    public static void toJSONArray(OutputWriter outputWriter, String key, Collection<EnvironmentVariableConfig> environmentVariableConfig) {
        outputWriter.addChildList(key, outputListWriter -> {
            environmentVariableConfig.forEach(envVar -> {
                outputListWriter.addChild(childWriter -> toJSON(childWriter, envVar));
            });
        });
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
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig();
        try {
            environmentVariableConfig.deserialize(name, value, secure, encryptedValue);
        } catch (CryptoException e) {
            environmentVariableConfig.addError(name, errorWhileEncryptingMessage());
        }

        return environmentVariableConfig;
    }

    private static void addValue(OutputWriter outputWriter, EnvironmentVariableConfig environmentVariableConfig) {
        if (environmentVariableConfig.isSecure()) {
            outputWriter.add("encrypted_value", environmentVariableConfig.getEncryptedValue());
        } else {
            outputWriter.add("value", environmentVariableConfig.getValue());
        }
    }
}
