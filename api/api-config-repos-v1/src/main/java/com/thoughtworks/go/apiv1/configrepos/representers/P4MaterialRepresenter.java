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
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.security.GoCipher;

class P4MaterialRepresenter {
    static void toJSON(OutputWriter json, P4MaterialConfig material) {
        json.add("name", material.getName());
        json.add("auto_update", material.getAutoUpdate());
        json.add("port", material.getUrl());
        json.add("use_tickets", material.getUseTickets());
        json.add("view", material.getView());
        json.add("username", material.getUserName());
        json.addIfNotNull("encrypted_password", material.getEncryptedPassword());
    }

    public static MaterialConfig fromJSON(JsonReader json, MaterialConfigHelper m) {
        P4MaterialConfig config = new P4MaterialConfig(
                m.serverAndPort(json),
                m.user(json),
                null,
                m.useTickets(json),
                m.view(json),
                new GoCipher(),
                m.name(json),
                m.autoUpdate(json),
                null,
                false,
                null
        );

        m.encryptedPassword(json, config);
        return config;
    }
}
