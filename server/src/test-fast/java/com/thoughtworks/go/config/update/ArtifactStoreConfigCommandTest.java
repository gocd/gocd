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
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ArtifactStoreConfigCommandTest {
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
    public void shouldNotContinueWithConfigSaveIfUserIsUnauthorized() {
        ArtifactStore artifactStore = new ArtifactStore("docker", "cd.go.artifact.docker");
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        StubCommand command = new StubCommand(goConfigService, artifactStore, extension, currentUser, result);

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.message(), is(EntityType.ArtifactStore.forbiddenToEdit(artifactStore.getId(), currentUser.getUsername())));
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsAuthorized() {
        ArtifactStore artifactStore = new ArtifactStore("docker", "cd.go.artifact.docker");
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        StubCommand command = new StubCommand(goConfigService, artifactStore, extension, currentUser, result);

        assertThat(command.canContinue(cruiseConfig), is(true));
        assertThat(result.httpCode(), is(200));
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfUserIsGroupAdmin() {
        ArtifactStore artifactStore = new ArtifactStore("docker", "cd.go.artifact.docker");
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        when(goConfigService.isGroupAdministrator(currentUser)).thenReturn(true);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        StubCommand command = new StubCommand(goConfigService, artifactStore, extension, currentUser, result);

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.message(), is(EntityType.ArtifactStore.forbiddenToEdit(artifactStore.getId(), currentUser.getUsername())));
    }

    @Test
    public void shouldValidateWithErrorIfIdIsNull() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArtifactStore artifactStore = new ArtifactStore(null, "cd.go.artifact.docker");
        cruiseConfig.getArtifactStores().add(artifactStore);

        StubCommand command = new StubCommand(goConfigService, artifactStore, extension, currentUser, result);
        thrown.expectMessage(EntityType.ArtifactStore.idCannotBeBlank());
        command.isValid(cruiseConfig);
    }

    @Test
    public void shouldPassValidationIfIdIsNotNull() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArtifactStore artifactStore = new ArtifactStore("docker", "cd.go.artifact.docker");
        cruiseConfig.getArtifactStores().add(artifactStore);
        when(extension.validateArtifactStoreConfig(eq("cd.go.artifact.docker"), anyMap())).thenReturn(new ValidationResult());

        StubCommand command = new StubCommand(goConfigService, artifactStore, extension, currentUser, result);
        boolean isValid = command.isValid(cruiseConfig);
        assertTrue(isValid);
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsAdmin() {
        ArtifactStore artifactStore = new ArtifactStore("docker", "cd.go.artifact.docker");
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        StubCommand command = new StubCommand(goConfigService, artifactStore, extension, currentUser, result);
        assertThat(cruiseConfig.getArtifactStores().find("docker"), nullValue());

        assertThat(command.canContinue(cruiseConfig), is(true));
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsGroupAdmin() {
        ArtifactStore artifactStore = new ArtifactStore("docker", "cd.go.artifact.docker");

        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        when(goConfigService.isGroupAdministrator(currentUser.getUsername())).thenReturn(true);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        StubCommand command = new StubCommand(goConfigService, artifactStore, extension, currentUser, result);
        assertThat(cruiseConfig.getArtifactStores().find("docker"), nullValue());

        assertThat(command.canContinue(cruiseConfig), is(false));
    }

    @Test
    public void shouldRaiseErrorForNonExistentArtifactStore() {
        cruiseConfig.getArtifactStores().clear();
        StubCommand command = new StubCommand(null, new ArtifactStore("foo", "cd.go.docker"), null, null, new HttpLocalizedOperationResult());

        thrown.expect(RecordNotFoundException.class);

        command.isValid(cruiseConfig);
    }

    private class StubCommand extends ArtifactStoreConfigCommand {

        public StubCommand(GoConfigService goConfigService, ArtifactStore newArtifactStore, ArtifactExtension extension, Username currentUser, LocalizedOperationResult result) {
            super(goConfigService, newArtifactStore, extension, currentUser, result);
        }

        @Override
        public void update(CruiseConfig modifiedConfig) {

        }

        @Override
        public boolean isValid(CruiseConfig preprocessedConfig) {
            return isValidForCreateOrUpdate(preprocessedConfig);
        }
    }

}
