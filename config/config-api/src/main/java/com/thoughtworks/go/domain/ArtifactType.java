/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.domain;

import com.google.gson.Gson;
import com.thoughtworks.go.config.ArtifactConfig;

import java.util.HashMap;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public enum ArtifactType {
    unit {
        @Override
        public String toJSON(ArtifactConfig artifactConfig) {
            final HashMap<String, Object> artifactStoreAsHashMap = new HashMap<>();
            artifactStoreAsHashMap.put("type", artifactConfig.getArtifactType().name().toLowerCase());
            artifactStoreAsHashMap.put("src", artifactConfig.getSource());
            artifactStoreAsHashMap.put("dest", artifactConfig.getDestination());
            return new Gson().toJson(artifactStoreAsHashMap);
        }
    },
    file {
        @Override
        public String toJSON(ArtifactConfig artifactConfig) {
            return unit.toJSON(artifactConfig);
        }
    },
    plugin {
        @Override
        public String toJSON(ArtifactConfig artifactConfig) {
            final HashMap<String, Object> artifactStoreAsHashMap = new HashMap<>();
            artifactStoreAsHashMap.put("id", artifactConfig.getId());
            artifactStoreAsHashMap.put("storeId", artifactConfig.getStoreId());
            artifactStoreAsHashMap.put("configuration", artifactConfig.getConfigurationAsMap(true));
            return new Gson().toJson(artifactStoreAsHashMap);
        }
    };

    public static ArtifactType fromName(String artifactType) {
        try {
            return valueOf(artifactType);
        } catch (IllegalArgumentException e) {
            throw bomb("Illegal name in for the artifact type.[" + artifactType + "]", e);
        }
    }

    public abstract String toJSON(ArtifactConfig artifactConfig);

    public boolean isTest() {
        return this.equals(unit);
    }
}
