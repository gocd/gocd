/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv1.shared.representers.materials;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.materials.PasswordDeserializer;
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;

public class TfsMaterialRepresenter {

    public static void toJSON(OutputWriter jsonWriter, TfsMaterialConfig tfsMaterialConfig) {
        ScmMaterialRepresenter.toJSON(jsonWriter, tfsMaterialConfig);
        jsonWriter.add("url", tfsMaterialConfig.getUrl());
        jsonWriter.add("domain", tfsMaterialConfig.getDomain());
        jsonWriter.add("username", tfsMaterialConfig.getUserName());
        jsonWriter.addIfNotNull("encrypted_password", tfsMaterialConfig.getEncryptedPassword());
        jsonWriter.add("project_path", tfsMaterialConfig.getProjectPath());
    }

    public static TfsMaterialConfig fromJSON(JsonReader jsonReader, ConfigHelperOptions options) {
        TfsMaterialConfig tfsMaterialConfig = new TfsMaterialConfig();
        ScmMaterialRepresenter.fromJSON(jsonReader, tfsMaterialConfig);
        jsonReader.readStringIfPresent("url", tfsMaterialConfig::setUrl);
        jsonReader.readStringIfPresent("domain", tfsMaterialConfig::setDomain);
        jsonReader.readStringIfPresent("username", tfsMaterialConfig::setUserName);
        jsonReader.readStringIfPresent("project_path", tfsMaterialConfig::setProjectPath);
        String password = null, encryptedPassword = null;
        if (jsonReader.hasJsonObject("password")) {
            password = jsonReader.getString("password");
        }
        if (jsonReader.hasJsonObject("encrypted_password")) {
            encryptedPassword = jsonReader.getString("encrypted_password");
        }
        PasswordDeserializer passwordDeserializer = options.getPasswordDeserializer();
        String encryptedPasswordValue = passwordDeserializer.deserialize(password, encryptedPassword, tfsMaterialConfig);
        tfsMaterialConfig.setEncryptedPassword(encryptedPasswordValue);
        return tfsMaterialConfig;
    }
}
