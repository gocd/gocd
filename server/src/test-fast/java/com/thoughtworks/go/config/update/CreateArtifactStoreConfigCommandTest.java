/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

public class CreateArtifactStoreConfigCommandTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private ArtifactExtension extension;


    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldAddArtifactStoreToConfig() throws Exception {
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        ArtifactStore artifactStore = new ArtifactStore("docker", "cd.go.artifact.docker");
        CreateArtifactStoreConfigCommand command = new CreateArtifactStoreConfigCommand(null, artifactStore, extension, null, null);
        command.update(cruiseConfig);

        assertThat(cruiseConfig.getArtifactStores().find("docker"), equalTo(artifactStore));
    }
}
