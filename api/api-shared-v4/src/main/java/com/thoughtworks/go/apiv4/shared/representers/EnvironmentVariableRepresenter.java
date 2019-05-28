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
package com.thoughtworks.go.apiv4.shared.representers;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.apiv4.shared.exceptions.InvalidGoCipherTextRuntimeException;
import com.thoughtworks.go.config.EnvironmentVariableConfig;
import com.thoughtworks.go.config.EnvironmentVariablesConfig;
import com.thoughtworks.go.security.CryptoException;

import java.util.HashMap;
import java.util.List;

public class EnvironmentVariableRepresenter {
    public static void toJSON(OutputListWriter jsonWriter, List<EnvironmentVariableConfig> environmentVariableConfigs) {
        environmentVariableConfigs.forEach(environmentVariableConfig -> {
            jsonWriter.addChild(envVarWriter -> toJSON(envVarWriter, environmentVariableConfig));
        });

    }

    public static void toJSON(OutputWriter jsonWriter, EnvironmentVariableConfig environmentVariableConfig) {
        if (!environmentVariableConfig.errors().isEmpty()) {
            jsonWriter.addChild("errors", errorWriter -> {
                HashMap<String, String> mapping = new HashMap<>();
                mapping.put("encryptedValue", "encrypted_value");
                new ErrorGetter(mapping).toJSON(jsonWriter, environmentVariableConfig);

            });
        }

        jsonWriter.add("secure", environmentVariableConfig.isSecure());
        jsonWriter.add("name", environmentVariableConfig.getName());
        if (environmentVariableConfig.isPlain()) {
            jsonWriter.addIfNotNull("value", environmentVariableConfig.getValueForDisplay());
        }
        if (environmentVariableConfig.isSecure()) {
            jsonWriter.addIfNotNull("encrypted_value", environmentVariableConfig.getValueForDisplay());
        }
    }


    public static EnvironmentVariablesConfig fromJSONArray(JsonReader jsonReader) {
        EnvironmentVariablesConfig variables = new EnvironmentVariablesConfig();
        jsonReader.readArrayIfPresent("environment_variables", environmentVariables -> {
            environmentVariables.forEach(variable -> variables.add(EnvironmentVariableRepresenter.fromJSON(new JsonReader(variable.getAsJsonObject()))));
        });
        return variables;
    }

    public static EnvironmentVariableConfig fromJSON(JsonReader jsonReader) {
        String name = jsonReader.getString("name");
        Boolean secure = jsonReader.optBoolean("secure").orElse(false);
        String value = secure ? jsonReader.optString("value").orElse(null) : jsonReader.getString("value");
        String encryptedValue = jsonReader.optString("encrypted_value").orElse(null);
        try {
            EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig();
            environmentVariableConfig.deserialize(name, value, secure, encryptedValue);
            return environmentVariableConfig;
        } catch (CryptoException e) {
            throw new InvalidGoCipherTextRuntimeException(e.getMessage(), e);
        }
    }

}
