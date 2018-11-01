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

import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.config.materials.PasswordDeserializer;
import com.thoughtworks.go.util.command.HgUrlArgument;
import com.thoughtworks.go.util.command.UrlArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MaterialConfigHelper {

    private PasswordDeserializer passwordDeserializer;

    @Autowired
    public MaterialConfigHelper(PasswordDeserializer passwordDeserializer) {
        this.passwordDeserializer = passwordDeserializer;
    }

    String password(JsonReader json) {
        return json.getStringOrDefault("password", null);
    }

    CaseInsensitiveString name(JsonReader json) {
        return json.optCaseInsensitiveString("name").orElse(null);
    }

    boolean autoUpdate(JsonReader json) {
        return json.optBoolean("auto_update").orElse(true);
    }

    UrlArgument url(JsonReader json) {
        return new UrlArgument(json.getString("url"));
    }

    String branch(JsonReader json) {
        return json.getStringOrDefault("branch", "master");
    }

    HgUrlArgument hgUrl(JsonReader json) {
        return new HgUrlArgument(json.getString("url"));
    }

    String serverAndPort(JsonReader json) {
        return json.getString("port");
    }

    String user(JsonReader json) {
        return json.getString("username");
    }

    String view(JsonReader json) {
        return json.getString("view");
    }

    boolean useTickets(JsonReader jsonReader) {
        return jsonReader.optBoolean("use_tickets").orElse(false);
    }

    void encryptedPassword(JsonReader json, AbstractMaterialConfig material) {
        String password = json.getStringOrDefault("password", null);
        String encryptedPassword = json.getStringOrDefault("encrypted_password", null);
        passwordDeserializer.deserialize(password, encryptedPassword, material);
    }

    boolean checkExternals(JsonReader json) {
        return json.optBoolean("check_externals").orElse(false);
    }

    String domain(JsonReader json) {
        return json.getStringOrDefault("domain", "");
    }

    String projectPath(JsonReader json) {
        return json.getString("project_path");
    }
}
