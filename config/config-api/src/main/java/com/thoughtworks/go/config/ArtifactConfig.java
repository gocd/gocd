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

import com.thoughtworks.go.domain.ConfigErrors;
import lombok.*;
import lombok.experimental.Accessors;

import javax.annotation.PostConstruct;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
@Accessors(chain = true)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.NONE)
@ConfigTag("artifacts")
public class ArtifactConfig implements Validatable {
    @ConfigSubtag
    private ArtifactDirectory artifactsDir = new ArtifactDirectory();
    @ConfigSubtag
    private PurgeSettings purgeSettings = new PurgeSettings();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Exclude
    private ConfigErrors errors = new ConfigErrors();

    @Override
    public void validate(ValidationContext validationContext) {
        artifactsDir.validate(validationContext);
        purgeSettings.validate(validationContext);
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }

    @PostConstruct
    public void ensureThatArtifactDirectoryExists() {
        artifactsDir.ensureThatArtifactDirectoryExists();
    }

    public boolean hasErrors() {
        return !errors().isEmpty();
    }
}
