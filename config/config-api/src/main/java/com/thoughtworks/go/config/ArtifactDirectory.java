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

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.validation.ArtifactDirValidator;
import com.thoughtworks.go.domain.ConfigErrors;
import lombok.*;
import lombok.experimental.Accessors;

import javax.annotation.PostConstruct;

import static com.thoughtworks.go.config.ServerConfig.ARTIFACT_DIR;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Getter
@Setter
@EqualsAndHashCode
@Accessors(chain = true)
@ConfigTag("artifactsDir")
public class ArtifactDirectory implements Validatable {
    @ConfigValue
    private String artifactDir;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Exclude
    private ConfigErrors errors = new ConfigErrors();

    public ArtifactDirectory() {
    }

    public ArtifactDirectory(String artifactDir) {
        this.artifactDir = artifactDir;
    }

    @PostConstruct
    public void ensureThatArtifactDirectoryExists() {
        if (isBlank(artifactDir)) {
            artifactDir = "artifacts";
        }
    }

    @Override
    public void validate(ValidationContext validationContext) {
        ArtifactDirValidator artifactDirValidator = new ArtifactDirValidator();
        try {
            artifactDirValidator.validate(validationContext.getCruiseConfig());
        } catch (Exception e) {
            errors().add(ARTIFACT_DIR, e.getMessage());
        }
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }
}
