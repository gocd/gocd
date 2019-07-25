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
package com.thoughtworks.go.apiv1.configrepos.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;

class HgMaterialRepresenter {
    static void toJSON(OutputWriter json, HgMaterialConfig material) {
        json.add("name", material.getName());
        json.add("auto_update", material.getAutoUpdate());
        json.add("url", material.getUrl());
    }

    public static MaterialConfig fromJSON(JsonReader json) {
        HgMaterialConfig materialConfig = new HgMaterialConfig();
        json.readStringIfPresent("url", materialConfig::setUrl);
        json.readStringIfPresent("name", materialConfig::setName);
        json.readBooleanIfPresent("auto_update", materialConfig::setAutoUpdate);
        return materialConfig;
    }
}
