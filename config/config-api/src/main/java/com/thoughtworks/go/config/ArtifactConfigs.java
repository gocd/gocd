/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.ArtifactType;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.config.ArtifactConfig.TEST_OUTPUT_FOLDER;

@ConfigTag("artifacts")
@ConfigCollection(ArtifactConfig.class)
public class ArtifactConfigs extends BaseCollection<ArtifactConfig> implements Validatable, ParamsAttributeAware {
    private final ConfigErrors configErrors = new ConfigErrors();

    public ArtifactConfigs() {
    }

    public ArtifactConfigs(List<ArtifactConfig> artifactConfigs) {
        super(artifactConfigs);
    }

    public ArtifactConfigs(ArtifactConfig... artifactConfigs) {
        super(artifactConfigs);
    }

    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        boolean isValid = errors().isEmpty();

        for (ArtifactConfig artifact : this) {
            isValid = artifact.validateTree(validationContext) && isValid;
        }
        return isValid;
    }

    public ArtifactConfig findByArtifactId(String artifactId) {
        for (ArtifactConfig artifact : getPluggableArtifactConfigs()) {
            if (artifact.getId().equals(artifactId)) {
                return artifact;
            }
        }
        return null;
    }

    public void validate(ValidationContext validationContext) {
        List<ArtifactConfig> artifactConfigs = new ArrayList<>();
        for (ArtifactConfig artifact : this) {
            artifact.validateUniqueness(artifactConfigs);
        }
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public void setConfigAttributes(Object attributes) {
        clear();
        if (attributes == null) {
            return;
        }
        List<Map> attrList = (List<Map>) attributes;
        for (Map attrMap : attrList) {
            String source = (String) attrMap.get(ArtifactConfig.SRC);
            String destination = (String) attrMap.get(ArtifactConfig.DEST);
            if (source.trim().isEmpty() && destination.trim().isEmpty()) {
                continue;
            }
            String type = (String) attrMap.get("artifactTypeValue");

            if ("Test Artifact".equals(type)) {
                this.add(new ArtifactConfig(ArtifactType.unit, source, destination));
            } else {
                this.add(new ArtifactConfig(ArtifactType.file, source, destination));
            }
        }
    }

    public List<ArtifactConfig> getArtifactConfigs() {
        final List<ArtifactConfig> artifactConfigs = new ArrayList<>();
        for (ArtifactConfig artifact : this) {
            if (artifact.getArtifactType() != ArtifactType.plugin) {
                artifactConfigs.add(artifact);
            }
        }
        return artifactConfigs;
    }

    public List<ArtifactConfig> getPluggableArtifactConfigs() {
        final List<ArtifactConfig> artifactConfigs = new ArrayList<>();
        for (ArtifactConfig artifact : this) {
            if (artifact.getArtifactType() == ArtifactType.plugin) {
                artifactConfigs.add(artifact);
            }
        }
        return artifactConfigs;
    }
}
