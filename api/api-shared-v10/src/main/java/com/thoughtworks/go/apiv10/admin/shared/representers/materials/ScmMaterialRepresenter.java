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
package com.thoughtworks.go.apiv10.admin.shared.representers.materials;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.apiv10.admin.shared.representers.stages.ConfigHelperOptions;
import com.thoughtworks.go.config.materials.PasswordDeserializer;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;

public abstract class ScmMaterialRepresenter<T extends ScmMaterialConfig> implements MaterialRepresenter<T> {
    @Override
    public void toJSON(OutputWriter jsonWriter, T scmMaterialConfig) {
        if (!(scmMaterialConfig instanceof P4MaterialConfig)) {
            jsonWriter.add("url", scmMaterialConfig.getUrl());
        }
        jsonWriter.add("destination", scmMaterialConfig.getFolder());

        if (scmMaterialConfig.filter().isEmpty()) {
            jsonWriter.renderNull("filter");
        } else {
            jsonWriter.addChild("filter", filterWriter -> FilterRepresenter.toJSON(filterWriter, scmMaterialConfig.filter()));
        }
        jsonWriter.add("invert_filter", scmMaterialConfig.isInvertFilter());
        jsonWriter.add("name", scmMaterialConfig.getName());
        jsonWriter.add("auto_update", scmMaterialConfig.isAutoUpdate());
        jsonWriter.addIfNotNull("username", scmMaterialConfig.getUserName());
        jsonWriter.addIfNotNull("encrypted_password", scmMaterialConfig.getEncryptedPassword());
    }

    public void fromJSON(JsonReader jsonReader, ScmMaterialConfig scmMaterialConfig, ConfigHelperOptions options) {
        jsonReader.readStringIfPresent("destination", scmMaterialConfig::setFolder);
        jsonReader.optBoolean("invert_filter").ifPresent(scmMaterialConfig::setInvertFilter);
        jsonReader.optJsonObject("filter").ifPresent(filterReader -> {
            scmMaterialConfig.setFilter(FilterRepresenter.fromJSON(filterReader));
        });
        jsonReader.readCaseInsensitiveStringIfPresent("name", scmMaterialConfig::setName);
        jsonReader.optBoolean("auto_update").ifPresent(scmMaterialConfig::setAutoUpdate);

        jsonReader.readStringIfPresent("username", scmMaterialConfig::setUserName);
        String password = null, encryptedPassword = null;
        if (jsonReader.hasJsonObject("password")) {
            password = jsonReader.getString("password");
        }
        if (jsonReader.hasJsonObject("encrypted_password")) {
            encryptedPassword = jsonReader.getString("encrypted_password");
        }

        PasswordDeserializer passwordDeserializer = options.getPasswordDeserializer();
        String encryptedPasswordValue = passwordDeserializer.deserialize(password, encryptedPassword, scmMaterialConfig);
        scmMaterialConfig.setEncryptedPassword(encryptedPasswordValue);
    }
}
