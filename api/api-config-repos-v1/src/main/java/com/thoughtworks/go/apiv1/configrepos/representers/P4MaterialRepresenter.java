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

package com.thoughtworks.go.apiv1.configrepos.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.materials.PasswordDeserializer;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;

class P4MaterialRepresenter {
    private static final PasswordDeserializer PASSWORD_DESERIALIZER = new PasswordDeserializer();

    static void toJSON(OutputWriter json, P4MaterialConfig material) {
        json.add("name", material.getName());
        json.add("auto_update", material.getAutoUpdate());
        json.add("port", material.getUrl());
        json.add("use_tickets", material.getUseTickets());
        json.add("view", material.getView());
        json.add("username", material.getUserName());
        json.addIfNotNull("encrypted_password", material.getEncryptedPassword());
    }

    public static P4MaterialConfig fromJSON(JsonReader json) {
        P4MaterialConfig materialConfig = new P4MaterialConfig();
        json.readStringIfPresent("name", materialConfig::setName);
        json.readBooleanIfPresent("auto_update", materialConfig::setAutoUpdate);
        json.readStringIfPresent("port", materialConfig::setServerAndPort);
        json.readBooleanIfPresent("use_tickets", materialConfig::setUseTickets);
        json.readStringIfPresent("view", materialConfig::setView);
        json.readStringIfPresent("username", materialConfig::setUserName);

        String password = json.getStringOrDefault("password", null);
        String encryptedPassword = json.getStringOrDefault("encrypted_password", null);
        materialConfig.setEncryptedPassword(PASSWORD_DESERIALIZER.deserialize(password, encryptedPassword, materialConfig));
        return materialConfig;
    }
}
