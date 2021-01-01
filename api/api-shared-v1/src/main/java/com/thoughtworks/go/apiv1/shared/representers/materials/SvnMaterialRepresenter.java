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
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;

public class SvnMaterialRepresenter {

    public static void toJSON(OutputWriter jsonWriter, SvnMaterialConfig svnMaterialConfig) {
        ScmMaterialRepresenter.toJSON(jsonWriter, svnMaterialConfig);
        jsonWriter.add("url", svnMaterialConfig.getUrl());
        jsonWriter.add("check_externals", svnMaterialConfig.isCheckExternals());
        jsonWriter.add("username", svnMaterialConfig.getUserName());
        jsonWriter.addIfNotNull("encrypted_password", svnMaterialConfig.getEncryptedPassword());
    }

    public static SvnMaterialConfig fromJSON(JsonReader jsonReader, ConfigHelperOptions options) {
        SvnMaterialConfig svnMaterialConfig = new SvnMaterialConfig();
        ScmMaterialRepresenter.fromJSON(jsonReader, svnMaterialConfig);
        jsonReader.readStringIfPresent("url", svnMaterialConfig::setUrl);
        jsonReader.optBoolean("check_externals").ifPresent(svnMaterialConfig::setCheckExternals);
        jsonReader.readStringIfPresent("username", svnMaterialConfig::setUserName);
        String password = null, encryptedPassword = null;
        if (jsonReader.hasJsonObject("password")) {
            password = jsonReader.getString("password");
        }
        if (jsonReader.hasJsonObject("encrypted_password")) {
            encryptedPassword = jsonReader.getString("encrypted_password");
        }
        PasswordDeserializer passwordDeserializer = options.getPasswordDeserializer();
        String encryptedPasswordValue = passwordDeserializer.deserialize(password, encryptedPassword, svnMaterialConfig);
        svnMaterialConfig.setEncryptedPassword(encryptedPasswordValue);
        return svnMaterialConfig;
    }
}
