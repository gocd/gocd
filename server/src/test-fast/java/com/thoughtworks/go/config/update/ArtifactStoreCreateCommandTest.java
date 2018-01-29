/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Matchers;
import org.mockito.Mock;

import java.util.Map;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ArtifactStoreCreateCommandTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Username currentUser;
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private ArtifactExtension extension;
    private BasicCruiseConfig cruiseConfig;


    @Before
    public void setUp() throws Exception {
        initMocks(this);
        currentUser = new Username("bob");
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
    }

    @Test
    public void shouldAddArtifactStoreToConfig() throws Exception {
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        ArtifactStore artifactStore = new ArtifactStore("docker", "cd.go.artifact.docker");
        ArtifactStoreCreateCommand command = new ArtifactStoreCreateCommand(null, artifactStore, extension, null, null);
        command.update(cruiseConfig);

        assertThat(cruiseConfig.getArtifactStores().find("docker"), equalTo(artifactStore));
    }

    @Test
    public void shouldInvokePluginValidationsBeforeSave() {
        ArtifactStore newArtifactStore = new ArtifactStore("docker", "cd.go.artifact.docker", create("key", false, "val"));
        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("key", "some-error"));
        cruiseConfig.getArtifactStores().add(newArtifactStore);

        when(extension.validateArtifactStoreConfig(eq("cd.go.artifact.docker"), Matchers.<Map<String, String>>any())).thenReturn(validationResult);


        PluginProfileCommand command = new ArtifactStoreCreateCommand(goConfigService, newArtifactStore, extension, null, new HttpLocalizedOperationResult());

        command.isValid(cruiseConfig);

        assertThat(newArtifactStore.first().errors().size(), is(1));
        assertThat(newArtifactStore.first().errors().on("key"), is("some-error"));
    }
}
