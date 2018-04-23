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

package com.thoughtworks.go.apiv6.shared.representers;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.EnvironmentVariableConfig;
import org.bouncycastle.crypto.InvalidCipherTextException;

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

    public static EnvironmentVariableConfig fromJSON(JsonReader jsonReader) throws InvalidCipherTextException {
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig();
        String name = jsonReader.getString("name");
        String value = null, encryptedValue = null;
        Boolean secure = false;
        if (jsonReader.hasJsonObject("value")) {
            value = jsonReader.getString("value");
        }
        if (jsonReader.hasJsonObject("encrypted_value")) {
            encryptedValue = jsonReader.getString("encrypted_value");
        }
        if (jsonReader.hasJsonObject("secure")) {
            secure = jsonReader.optBoolean("secure").get();
        }
        environmentVariableConfig.deserialize(name, value, secure, encryptedValue);
        return environmentVariableConfig;
    }
}
