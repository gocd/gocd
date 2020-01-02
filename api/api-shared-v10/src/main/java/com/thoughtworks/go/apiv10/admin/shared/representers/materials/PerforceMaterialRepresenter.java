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
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;

public class PerforceMaterialRepresenter extends ScmMaterialRepresenter<P4MaterialConfig> {

    @Override
    public void toJSON(OutputWriter jsonWriter, P4MaterialConfig p4MaterialConfig) {
        // The ScmMaterialRepresenter tries to do getUrl, but p4 material doesn't have a url.
        super.toJSON(jsonWriter, p4MaterialConfig);
        jsonWriter.add("port", p4MaterialConfig.getServerAndPort());
        jsonWriter.add("use_tickets", p4MaterialConfig.getUseTickets());
        jsonWriter.add("view", p4MaterialConfig.getView());
    }

    @Override
    public P4MaterialConfig fromJSON(JsonReader jsonReader, ConfigHelperOptions options) {
        P4MaterialConfig p4MaterialConfig = new P4MaterialConfig();
        super.fromJSON(jsonReader, p4MaterialConfig, options);
        jsonReader.readStringIfPresent("port", p4MaterialConfig::setServerAndPort);
        jsonReader.optBoolean("use_tickets").ifPresent(p4MaterialConfig::setUseTickets);
        jsonReader.readStringIfPresent("view", p4MaterialConfig::setView);
        return p4MaterialConfig;
    }
}
