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
package com.thoughtworks.go.config;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ArtifactConfigTest {

    @Test
    void shouldValidateArtifactDirectoryAndPurgeSetting() {
        ArtifactConfig artifactConfig = new ArtifactConfig();
        artifactConfig.setArtifactsDir(mock(ArtifactDirectory.class));
        artifactConfig.setPurgeSettings(mock(PurgeSettings.class));

        artifactConfig.validate(null);

        verify(artifactConfig.getArtifactsDir(), times(1)).validate(null);
        verify(artifactConfig.getPurgeSettings(), times(1)).validate(null);
    }
}