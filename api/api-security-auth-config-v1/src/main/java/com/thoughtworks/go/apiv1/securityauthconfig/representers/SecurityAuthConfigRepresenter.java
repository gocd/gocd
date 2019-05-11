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

package com.thoughtworks.go.apiv1.securityauthconfig.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ConfigurationPropertyRepresenter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.spark.Routes;

import java.util.Collections;
import java.util.Map;

public class SecurityAuthConfigRepresenter {

    public static void toJSON(OutputWriter outputWriter, SecurityAuthConfig securityAuthConfig) {
        outputWriter
                .addLinks(linksWriter -> linksWriter
                        .addLink("self", Routes.SecurityAuthConfigAPI.id(securityAuthConfig.getId()))
                        .addAbsoluteLink("doc", Routes.SecurityAuthConfigAPI.DOC)
                        .addLink("find", Routes.SecurityAuthConfigAPI.find()))
                .add("id", securityAuthConfig.getId())
                .add("plugin_id", securityAuthConfig.getPluginId())
                .addChildList("properties", listWriter ->
                        securityAuthConfig.forEach(property -> listWriter.addChild(propertyWriter -> ConfigurationPropertyRepresenter.toJSON(propertyWriter, property))));

        if (securityAuthConfig.hasErrors()) {
            Map<String, String> fieldMapping = Collections.singletonMap("pluginId", "plugin_id");
            outputWriter.addChild("errors", errorWriter -> new ErrorGetter(fieldMapping).toJSON(errorWriter, securityAuthConfig));
        }
    }

    public static SecurityAuthConfig fromJSON(JsonReader jsonReader) {
        SecurityAuthConfig securityAuthConfig = new SecurityAuthConfig(
                jsonReader.getString("id"),
                jsonReader.getString("plugin_id"));
        securityAuthConfig.addConfigurations(ConfigurationPropertyRepresenter.fromJSONArray(jsonReader, "properties"));

        return securityAuthConfig;
    }
}
