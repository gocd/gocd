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

import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;

import java.util.List;

@ConfigTag("properties")
@ConfigCollection(ArtifactPropertyConfig.class)
public class ArtifactPropertiesConfig extends BaseCollection<ArtifactPropertyConfig> implements Validatable {
    private final ConfigErrors configErrors = new ConfigErrors();

    public ArtifactPropertiesConfig() {
    }

    public ArtifactPropertiesConfig(ArtifactPropertyConfig... artifactPropertiesGenerators) {
        super(artifactPropertiesGenerators);
    }

    public ArtifactPropertiesConfig(List<ArtifactPropertyConfig> generators) {
        super(generators);
    }

    public boolean validateTree(ValidationContext validationContext) {
        boolean isValid = errors().isEmpty();

        for (ArtifactPropertyConfig artifactPropertiesGenerator : this) {
            isValid = artifactPropertiesGenerator.validateTree(validationContext) && isValid;
        }
        return isValid;
    }

    public void validate(ValidationContext validationContext) {
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

}
