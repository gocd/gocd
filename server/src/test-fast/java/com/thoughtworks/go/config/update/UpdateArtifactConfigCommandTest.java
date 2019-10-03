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

package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.service.EntityHashingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

public class UpdateArtifactConfigCommandTest {
    @Mock
    EntityHashingService entityHashingService;

    private BasicCruiseConfig cruiseConfig;

    @BeforeEach
    public void setup() {
        initMocks(this);
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
    }

    @Nested
    class IsValid {
        @Test
        void shouldValidateArtifactConfig() {
            ArtifactConfig artifactConfig = new ArtifactConfig();
            artifactConfig.setArtifactsDir(new ArtifactDirectory("artifacts"));
            cruiseConfig.server().setArtifactConfig(artifactConfig);
            UpdateArtifactConfigCommand updateArtifactConfigCommand = new UpdateArtifactConfigCommand(artifactConfig);

            assertTrue(updateArtifactConfigCommand.isValid(cruiseConfig));
        }

        @Test
        void shouldRaiseAnExceptionForBlankArtifactDirName() {
            ArtifactConfig artifactConfig = new ArtifactConfig();
            artifactConfig.setArtifactsDir(new ArtifactDirectory(""));
            cruiseConfig.server().setArtifactConfig(artifactConfig);
            UpdateArtifactConfigCommand updateArtifactConfigCommand = new UpdateArtifactConfigCommand(artifactConfig);

            assertThatCode(() -> updateArtifactConfigCommand.isValid(cruiseConfig)).isInstanceOf(GoConfigInvalidException.class).hasMessage("Please provide a not empty value for artifactsdir");
            assertThat(artifactConfig.errors().size()).isEqualTo(1);
            assertThat(artifactConfig.errors().firstError()).isEqualTo("Please provide a not empty value for artifactsdir");

        }

        @Test
        void shouldRaiseAnExceptionForInvalidPurgeStartValue() {
            ArtifactConfig artifactConfig = new ArtifactConfig();
            artifactConfig.setArtifactsDir(new ArtifactDirectory("artifactsDir"));
            PurgeSettings purgeSettings = new PurgeSettings();
            purgeSettings.setPurgeStart(new PurgeStart(0.0));
            purgeSettings.setPurgeUpto(new PurgeUpto(20.0));
            artifactConfig.setPurgeSettings(purgeSettings);
            cruiseConfig.server().setArtifactConfig(artifactConfig);
            UpdateArtifactConfigCommand updateArtifactConfigCommand = new UpdateArtifactConfigCommand(artifactConfig);

            assertThatCode(() -> updateArtifactConfigCommand.isValid(cruiseConfig)).isInstanceOf(GoConfigInvalidException.class).hasMessage("Error in artifact cleanup values. The trigger value is has to be specified when a goal is set");
            assertThat(artifactConfig.errors().size()).isEqualTo(1);
            assertThat(artifactConfig.errors().firstError()).isEqualTo("Error in artifact cleanup values. The trigger value is has to be specified when a goal is set");

        }
    }

    @Nested
    class Update {
        @Test
        void shouldUpdateArtifactConfig() throws Exception {
            ArtifactConfig artifactConfig = new ArtifactConfig();
            artifactConfig.setArtifactsDir(new ArtifactDirectory("artifactDir"));
            cruiseConfig.server().setArtifactConfig(artifactConfig);

            ArtifactConfig modifiedArtifactConfig = new ArtifactConfig();
            modifiedArtifactConfig.setArtifactsDir(new ArtifactDirectory("newArtifactDir"));

            UpdateArtifactConfigCommand updateArtifactConfigCommand = new UpdateArtifactConfigCommand(modifiedArtifactConfig);

            updateArtifactConfigCommand.update(cruiseConfig);

            assertThat(cruiseConfig.server().artifactsDir()).isEqualTo("newArtifactDir");
        }

    }
}
