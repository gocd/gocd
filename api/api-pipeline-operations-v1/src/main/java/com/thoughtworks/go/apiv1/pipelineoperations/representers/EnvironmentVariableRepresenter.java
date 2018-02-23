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

package com.thoughtworks.go.apiv1.pipelineoperations.representers;

import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.EnvironmentVariableConfig;
import com.thoughtworks.go.config.exceptions.InvalidCipherTextRuntimeException;
import org.bouncycastle.crypto.InvalidCipherTextException;

public class EnvironmentVariableRepresenter {
    public static EnvironmentVariableConfig fromJSON(JsonReader jsonReader) {
        String name = jsonReader.getString("name");
        Boolean secure = jsonReader.optBoolean("secure").orElse(false);
        String value = secure ? jsonReader.optString("value").orElse(null) : jsonReader.getString("value");
        String encryptedValue = jsonReader.optString("encrypted_value").orElse(null);
        try {
            EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig();
            environmentVariableConfig.deserialize(name, value, secure, encryptedValue);
            return environmentVariableConfig;
        } catch (InvalidCipherTextException e) {
            throw new InvalidCipherTextRuntimeException(e.getMessage(), e);
        }
    }
}
