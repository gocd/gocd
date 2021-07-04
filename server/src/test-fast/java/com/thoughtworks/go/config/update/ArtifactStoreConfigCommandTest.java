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
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ArtifactStoreConfigCommandTest {

    private Username currentUser;
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private ArtifactExtension extension;
    private BasicCruiseConfig cruiseConfig;


    @BeforeEach
    public void setUp() throws Exception {
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
        lenient().when(goConfigService.isGroupAdministrator(currentUser)).thenReturn(true);

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

        assertThatThrownBy(() -> command.isValid(cruiseConfig))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(EntityType.ArtifactStore.idCannotBeBlank());
    }

    @Test
    public void shouldPassValidationIfIdIsNotNull() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArtifactStore artifactStore = new ArtifactStore("docker", "cd.go.artifact.docker");
        cruiseConfig.getArtifactStores().add(artifactStore);

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
        lenient().when(goConfigService.isGroupAdministrator(currentUser.getUsername())).thenReturn(true);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        StubCommand command = new StubCommand(goConfigService, artifactStore, extension, currentUser, result);
        assertThat(cruiseConfig.getArtifactStores().find("docker"), nullValue());

        assertThat(command.canContinue(cruiseConfig), is(false));
    }

    @Test
    public void shouldRaiseErrorForNonExistentArtifactStore() {
        cruiseConfig.getArtifactStores().clear();
        StubCommand command = new StubCommand(null, new ArtifactStore("foo", "cd.go.docker"), null, null, new HttpLocalizedOperationResult());

        assertThatThrownBy(() -> command.isValid(cruiseConfig))
                .isInstanceOf(RecordNotFoundException.class);
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
