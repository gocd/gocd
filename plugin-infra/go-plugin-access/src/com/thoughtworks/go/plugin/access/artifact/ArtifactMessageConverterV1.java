/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.artifact;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.config.ArtifactStores;
import com.thoughtworks.go.domain.ArtifactPlan;
import com.thoughtworks.go.plugin.access.common.models.PluginProfileMetadataKeys;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArtifactMessageConverterV1 implements ArtifactMessageConverter {
    public static final String VERSION = "1.0";
    private static final Gson GSON = new Gson();

    @Override
    public String publishArtifactMessage(ArtifactStores artifactStores, List<ArtifactPlan> artifactPlans) {
        final HashMap<String, Object> messageObject = new HashMap<>();
        messageObject.put("artifactStores", getArtifactStore(artifactStores));
        messageObject.put("artifactPlans", getArtifactPlans(artifactPlans));
        return GSON.toJson(messageObject);
    }

    private List<Map<String, Object>> getArtifactPlans(List<ArtifactPlan> artifactPlans) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ArtifactPlan artifactPlan : artifactPlans) {
            list.add(artifactPlan.getPluggableArtifactConfiguration());
        }
        return list;
    }

    private List<Map<String, Object>> getArtifactStore(ArtifactStores artifactStores) {
        final List<Map<String, Object>> list = new ArrayList<>();
        for (ArtifactStore artifactStore : artifactStores) {
            final HashMap<String, Object> artifactStoreAsHashMap = new HashMap<>();
            artifactStoreAsHashMap.put("id", artifactStore.getId());
            artifactStoreAsHashMap.put("configuration", artifactStore.getConfigurationAsMap(true));
            list.add(artifactStoreAsHashMap);
        }
        return list;
    }

    @Override
    public Map<String, Object> publishArtifactResponse(String responseBody) {
        final Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        return GSON.fromJson(responseBody, type);
    }

    @Override
    public List<PluginConfiguration> getMetadataResponseFromBody(String responseBody) {
        return PluginProfileMetadataKeys.fromJSON(responseBody).toPluginConfigurations();
    }

    @Override
    public String getViewFromResponseBody(String responseBody, final String viewLabel) {
        return getTemplateFromResponse(responseBody, String.format("%s `template` was blank!", viewLabel));
    }

    private String getTemplateFromResponse(String responseBody, String message) {
        String template = (String) new Gson().fromJson(responseBody, Map.class).get("template");
        if (StringUtils.isBlank(template)) {
            throw new RuntimeException(message);
        }
        return template;
    }
}
