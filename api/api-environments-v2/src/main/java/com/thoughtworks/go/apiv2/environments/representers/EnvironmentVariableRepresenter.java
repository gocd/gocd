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

package com.thoughtworks.go.apiv2.environments.representers;

import com.google.gson.JsonObject;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.config.EnvironmentVariableConfig;


public class EnvironmentVariableRepresenter {
    public static void toJSON(OutputWriter outputWriter, EnvironmentVariableConfig environmentVariableConfig) {
        outputWriter
                .add("secure", environmentVariableConfig.isSecure())
                .add("name", environmentVariableConfig.getName());
        addValue(outputWriter, environmentVariableConfig);
    }

    public static EnvironmentVariableConfig fromJSON(JsonObject jsonReader) {
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig();
        environmentVariableConfig.setName(jsonReader.get("name").getAsString());
        environmentVariableConfig.setIsSecure(jsonReader.has("secure") && jsonReader.get("secure").getAsBoolean());
        if (jsonReader.has("encrypted_value")) {
            environmentVariableConfig.setEncryptedValue(jsonReader.get("encrypted_value").getAsString());
        }
        if (jsonReader.has("value")) {
            environmentVariableConfig.setValue(jsonReader.get("value").getAsString());
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
