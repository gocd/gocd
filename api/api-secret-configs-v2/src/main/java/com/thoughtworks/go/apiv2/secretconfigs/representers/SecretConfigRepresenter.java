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
package com.thoughtworks.go.apiv2.secretconfigs.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ConfigurationPropertyRepresenter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.SecretConfig;
import com.thoughtworks.go.spark.Routes;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.Map;

public class SecretConfigRepresenter {
    public static void toJSON(OutputWriter jsonWriter, SecretConfig secretConfig) {
        if (secretConfig == null)
            return;
        jsonWriter.addLinks(linksWriter -> linksWriter
                .addLink("self", Routes.SecretConfigsAPI.id(secretConfig.getId()))
                .addAbsoluteLink("doc", Routes.SecretConfigsAPI.DOC)
                .addLink("find", Routes.SecretConfigsAPI.find()))
                .add("id", secretConfig.getId())
                .add("plugin_id", secretConfig.getPluginId())
                .addIfNotNull("description", secretConfig.getDescription());

        if (secretConfig.hasErrors()) {
            Map<String, String> fieldMapping = Collections.singletonMap("pluginId", "plugin_id");
            jsonWriter.addChild("errors", errorWriter -> new ErrorGetter(fieldMapping).toJSON(errorWriter, secretConfig));
        }

        jsonWriter.addChildList("properties", listWriter -> {
            ConfigurationPropertyRepresenter.toJSON(listWriter, secretConfig.getConfiguration());
        });

        if (!CollectionUtils.isEmpty(secretConfig.getRules())) {
            jsonWriter.addChildList("rules", rulesWriter -> RulesRepresenter.toJSON(rulesWriter, secretConfig.getRules()));
        }
    }

    public static SecretConfig fromJSON(JsonReader jsonReader) {
        SecretConfig secretConfig = new SecretConfig(jsonReader.getString("id"), jsonReader.getString("plugin_id"));
        jsonReader.optString("description").ifPresent(description -> secretConfig.setDescription(description));
        secretConfig.addConfigurations(ConfigurationPropertyRepresenter.fromJSONArray(jsonReader, "properties"));

        jsonReader.readArrayIfPresent("rules", array -> {
            secretConfig.setRules(RulesRepresenter.fromJSON(array));
        });

        return secretConfig;
    }
}
