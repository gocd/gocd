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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


class ArtifactDirectoryTest {


    @Test
    void shouldAddErrorsIfArtifactsDirIsEmpty() throws Exception {
        String message = "Please provide a not empty value for artifactsdir";
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.server().setArtifactsDir("");
        ArtifactDirectory artifactDir = new ArtifactDirectory("");
        ArtifactDirValidator artifactDirValidator = mock(ArtifactDirValidator.class);
        ValidationContext validationContext = new ConfigSaveValidationContext(cruiseConfig);

        doThrow(new Exception(message)).when(artifactDirValidator).validate(cruiseConfig);

        artifactDir.validate(validationContext);

        assertFalse(artifactDir.errors().isEmpty());
        assertThat(artifactDir.errors().firstError()).isEqualTo(message);
    }

    @Test
    void shouldValidateArtifactDirectory() {
        ArtifactDirectory artifactDir = new ArtifactDirectory("artifacts");
        BasicCruiseConfig basicCruiseConfig = new BasicCruiseConfig();
        basicCruiseConfig.server().setArtifactsDir("artifacts");

        ValidationContext validationContext = new ConfigSaveValidationContext(basicCruiseConfig);
        artifactDir.validate(validationContext);

        assertTrue(artifactDir.errors().isEmpty());
    }

}